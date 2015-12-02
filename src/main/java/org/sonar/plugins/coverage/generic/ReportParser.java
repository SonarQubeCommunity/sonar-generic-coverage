/*
 * SonarQube Generic Coverage Plugin
 * Copyright (C) 2014 SonarSource
 * sonarqube@googlegroups.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.coverage.generic;

import org.codehaus.staxmate.in.SMHierarchicCursor;
import org.codehaus.staxmate.in.SMInputCursor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.measures.Measure;
import org.sonar.api.test.MutableTestPlan;
import org.sonar.api.utils.StaxParser;

import javax.xml.stream.XMLStreamException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ReportParser {

  public enum Mode {
    COVERAGE, IT_COVERAGE, OVERALL_COVERAGE, UNITTEST
  }

  private static final String LINE_NUMBER_ATTR = "lineNumber";
  private static final String COVERED_ATTR = "covered";
  private static final String BRANCHES_TO_COVER_ATTR = "branchesToCover";
  private static final String COVERED_BRANCHES_ATTR = "coveredBranches";
  private static final String NAME_ATTR = "name";
  private static final String DURATION_ATTR = "duration";
  private static final String MESSAGE_ATTR = "message";

  private static final int MAX_STORED_UNKNOWN_FILE_PATHS = 5;

  private final FileSystem fileSystem;
  private final SensorContext context;
  private final ResourcePerspectives perspectives;
  private final Mode mode;

  private int numberOfUnknownFiles;
  private final List<String> firstUnknownFiles = new ArrayList<>();
  private final Set<String> matchedFileKeys = new HashSet<>();
  private final Map<InputFile, CustomCoverageMeasuresBuilder> coverageMeasures = new HashMap<>();
  private final Map<InputFile, UnitTestMeasuresBuilder> unitTestMeasures = new HashMap<>();

  public ReportParser(FileSystem fileSystem, SensorContext context, ResourcePerspectives perspectives, Mode mode) {
    this.fileSystem = fileSystem;
    this.context = context;
    this.perspectives = perspectives;
    this.mode = mode;
  }

  public void parse(java.io.File reportFile)
    throws XMLStreamException {
    InputStream inputStream;
    try {
      inputStream = new FileInputStream(reportFile);
    } catch (FileNotFoundException e) {
      throw new IllegalStateException(e);
    }
    parse(inputStream);
  }

  public void parse(InputStream inputStream) throws XMLStreamException {
    StaxParser parser = new StaxParser(new StaxParser.XmlStreamHandler() {
      @Override
      public void stream(SMHierarchicCursor rootCursor) throws XMLStreamException {
        rootCursor.advance();
        parseRootNode(rootCursor);
      }
    });
    parser.parse(inputStream);
  }

  private void parseRootNode(SMHierarchicCursor rootCursor) throws XMLStreamException {
    checkElementName(rootCursor, mode == Mode.UNITTEST ? "unitTest" : "coverage");
    String version = rootCursor.getAttrValue("version");
    if (!"1".equals(version)) {
      String message = "Unknown coverage version: " + version + ". This parser only handles version 1.";
      throw new ReportParsingException(message, rootCursor);
    }
    parseFiles(rootCursor.childElementCursor());
  }

  private void parseFiles(SMInputCursor fileCursor) throws XMLStreamException {
    while (fileCursor.getNext() != null) {
      checkElementName(fileCursor, "file");
      String filePath = mandatoryAttribute(fileCursor, "path");
      InputFile resource = fileSystem.inputFile(fileSystem.predicates().hasPath(filePath));
      if (resource == null || context.getResource(resource) == null) {
        numberOfUnknownFiles++;
        if (numberOfUnknownFiles <= MAX_STORED_UNKNOWN_FILE_PATHS) {
          firstUnknownFiles.add(filePath);
        }
        continue;
      }
      matchedFileKeys.add(resource.absolutePath());

      SMInputCursor testCaseCursor = fileCursor.childElementCursor();
      while (testCaseCursor.getNext() != null) {
        if (Mode.UNITTEST == mode) {
          parseTestCase(resource, testCaseCursor);
        } else {
          parseLineToCover(resource, testCaseCursor);
        }
      }
    }
  }

  private UnitTestMeasuresBuilder getUnitTestMeasuresBuilder(InputFile resource) {
    UnitTestMeasuresBuilder measuresBuilder = unitTestMeasures.get(resource);
    if (measuresBuilder == null) {
      measuresBuilder = UnitTestMeasuresBuilder.create();
      unitTestMeasures.put(resource, measuresBuilder);
    }
    return measuresBuilder;
  }

  private CustomCoverageMeasuresBuilder getCoverageMeasuresBuilder(InputFile resource) {
    CustomCoverageMeasuresBuilder measuresBuilder = coverageMeasures.get(resource);
    if (measuresBuilder == null) {
      measuresBuilder = CustomCoverageMeasuresBuilder.create();
      switch (mode) {
        case IT_COVERAGE:
          measuresBuilder.enableITMode();
          break;
        case OVERALL_COVERAGE:
          measuresBuilder.enableOverallMode();
          break;
        default:
          break;
      }
      coverageMeasures.put(resource, measuresBuilder);
    }
    return measuresBuilder;
  }

  private void parseLineToCover(InputFile resource, SMInputCursor cursor)
    throws XMLStreamException {
    CustomCoverageMeasuresBuilder measureBuilder = getCoverageMeasuresBuilder(resource);
    checkElementName(cursor, "lineToCover");
    String lineNumberAsString = mandatoryAttribute(cursor, LINE_NUMBER_ATTR);
    int lineNumber = intValue(lineNumberAsString, cursor, LINE_NUMBER_ATTR, 1);

    boolean covered = getCoveredValue(cursor);
    measureBuilder.setHits(lineNumber, covered ? 1 : 0);

    String branchesToCoverAsString = cursor.getAttrValue(BRANCHES_TO_COVER_ATTR);
    if (branchesToCoverAsString != null) {
      int branchesToCover = intValue(branchesToCoverAsString, cursor, BRANCHES_TO_COVER_ATTR, 0);
      String coveredBranchesAsString = cursor.getAttrValue(COVERED_BRANCHES_ATTR);
      int coveredBranches = 0;
      if (coveredBranchesAsString != null) {
        coveredBranches = intValue(coveredBranchesAsString, cursor, COVERED_BRANCHES_ATTR, 0);
        if (coveredBranches > branchesToCover) {
          throw new ReportParsingException("\"coveredBranches\" should not be greater than \"branchesToCover\"", cursor);
        }
      }
      if (measureBuilder.setConditions(lineNumber, branchesToCover, coveredBranches) == null) {
        throw new ReportParsingException("\"branchesToCover\" mismatch between two different reports", cursor);
      }
    }
  }

  private static boolean getCoveredValue(SMInputCursor cursor) throws XMLStreamException {
    String coveredAsString = mandatoryAttribute(cursor, COVERED_ATTR);
    if (!"true".equalsIgnoreCase(coveredAsString) && !"false".equalsIgnoreCase(coveredAsString)) {
      throw new ReportParsingException(expectedMessage("boolean value", COVERED_ATTR, coveredAsString), cursor);
    }
    return Boolean.parseBoolean(coveredAsString);
  }

  private void parseTestCase(InputFile resource, SMInputCursor cursor) throws XMLStreamException {
    UnitTestMeasuresBuilder measures = getUnitTestMeasuresBuilder(resource);
    checkElementName(cursor, "testCase");
    String name = mandatoryAttribute(cursor, NAME_ATTR);
    String status = TestCase.OK;
    String durationAsString = mandatoryAttribute(cursor, DURATION_ATTR);
    long duration = longValue(durationAsString, cursor, DURATION_ATTR, 0);

    String message = null;
    String stacktrace = null;
    int cursorLine = cursor.getCursorLocation().getLineNumber();
    SMInputCursor child = cursor.descendantElementCursor();
    if (child.getNext() != null) {
      String elementName = child.getLocalName();
      if (TestCase.SKIPPED.equals(elementName)) {
        status = TestCase.SKIPPED;
      } else if (TestCase.FAILURE.equals(elementName)) {
        status = TestCase.FAILURE;
      } else if (TestCase.ERROR.equals(elementName)) {
        status = TestCase.ERROR;
      }
      if (!TestCase.OK.equals(status)) {
        message = mandatoryAttribute(child, MESSAGE_ATTR);
        stacktrace = child.collectDescendantText();
      }
    }

    if (!measures.setTestCase(name, status, duration, message, stacktrace)) {
      throw new ReportParsingException("\"testCase\" with name " + name + " reported twice", cursorLine);
    }
  }

  private static void checkElementName(SMInputCursor cursor, String expectedName) throws XMLStreamException {
    String elementName = cursor.getLocalName();
    if (!expectedName.equals(elementName)) {
      String message = "Unknown XML node, expected \"" + expectedName + "\" but got \"" + elementName + "\"";
      throw new ReportParsingException(message, cursor);
    }
  }

  private static String mandatoryAttribute(SMInputCursor cursor, String attributeName) throws XMLStreamException {
    String attributeValue = cursor.getAttrValue(attributeName);
    if (attributeValue == null) {
      String message = "Missing attribute \"" + attributeName + "\" in element \"" + cursor.getLocalName() + "\"";
      throw new ReportParsingException(message, cursor);
    }
    return attributeValue;
  }

  private static int intValue(String stringValue, SMInputCursor cursor, String attributeName, int minimum) throws XMLStreamException {
    int intValue;
    try {
      intValue = Integer.valueOf(stringValue);
    } catch (NumberFormatException e) {
      String message = expectedMessage("integer value", attributeName, stringValue);
      throw new ReportParsingException(message, e, cursor.getCursorLocation().getLineNumber());
    }
    if (intValue < minimum) {
      String message =
        "Value of attribute \"" + attributeName + "\" is \"" + intValue + "\" but it should be greater than or equal to " + minimum;
      throw new ReportParsingException(message, cursor);
    }
    return intValue;
  }

  private static long longValue(String stringValue, SMInputCursor cursor, String attributeName, long minimum) throws XMLStreamException {
    long longValue;
    try {
      longValue = Long.valueOf(stringValue);
    } catch (NumberFormatException e) {
      String message = expectedMessage("long value", attributeName, stringValue);
      throw new ReportParsingException(message, e, cursor.getCursorLocation().getLineNumber());
    }
    if (longValue < minimum) {
      String message =
        "Value of attribute \"" + attributeName + "\" is \"" + longValue + "\" but it should be greater than or equal to " + minimum;
      throw new ReportParsingException(message, cursor);
    }
    return longValue;
  }

  private static String expectedMessage(String expected, String attributeName, String stringValue) {
    return "Expected " + expected + " for attribute \"" + attributeName + "\" but got \"" + stringValue + "\"";
  }

  public int numberOfMatchedFiles() {
    return matchedFileKeys.size();
  }

  public int numberOfUnknownFiles() {
    return numberOfUnknownFiles;
  }

  public List<String> firstUnknownFiles() {
    return firstUnknownFiles;
  }

  public void saveMeasures() {
    if (mode == Mode.UNITTEST) {
      saveUnitTestMeasures();
    } else {
      saveCoverageMeasure();
    }
  }

  private void saveCoverageMeasure() {
    for (Map.Entry<InputFile, CustomCoverageMeasuresBuilder> entry : coverageMeasures.entrySet()) {
      for (Measure measure : entry.getValue().createMeasures()) {
        context.saveMeasure(entry.getKey(), measure);
      }
    }
  }

  private void saveUnitTestMeasures() {
    for (Map.Entry<InputFile, UnitTestMeasuresBuilder> entry : unitTestMeasures.entrySet()) {
      InputFile inputFile = entry.getKey();
      UnitTestMeasuresBuilder measuresBuilder = entry.getValue();
      for (Measure measure : measuresBuilder.createMeasures()) {
        context.saveMeasure(inputFile, measure);
      }
      for (TestCase testCase : measuresBuilder.getTestCases()) {
        MutableTestPlan testPlan = perspectives.as(MutableTestPlan.class, inputFile);
        if (testPlan != null) {
          testPlan.addTestCase(testCase.getName())
            .setDurationInMs(testCase.getDuration())
            .setStatus(org.sonar.api.test.TestCase.Status.of(testCase.getStatus()))
            .setMessage(testCase.getMessage())
            .setType(org.sonar.api.test.TestCase.TYPE_UNIT)
            .setStackTrace(testCase.getStackTrace());
        }
      }
    }
  }
}
