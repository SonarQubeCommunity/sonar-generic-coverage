/*
 * SonarQube Generic Coverage Plugin
 * Copyright (C) 2014 SonarSource
 * dev@sonar.codehaus.org
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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.codehaus.staxmate.in.SMHierarchicCursor;
import org.codehaus.staxmate.in.SMInputCursor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.measures.CoverageMeasuresBuilder;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.File;
import org.sonar.api.utils.SonarException;
import org.sonar.api.utils.StaxParser;

import javax.xml.stream.XMLStreamException;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;

public class ReportParser {

  private static final String LINE_NUMBER_ATTR = "lineNumber";
  private static final String COVERED_ATTR = "covered";
  private static final String BRANCHES_TO_COVER_ATTR = "branchesToCover";
  private static final String COVERED_BRANCHES_ATTR = "coveredBranches";

  private static final int MAX_STORED_UNKNOWN_FILE_PATHS = 5;

  private final InputStream inputStream;
  private final ResourceLocator resourceLocator;
  private final SensorContext context;

  private int numberOfMatchedFiles;
  private int numberOfUnknownFiles;
  private List<String> firstUnknownFiles = Lists.newArrayList();
  private Set<String> matchedFileKeys = Sets.newHashSet();

  public ReportParser(InputStream inputStream, ResourceLocator resourceLocator, SensorContext context) throws XMLStreamException {
    this.inputStream = inputStream;
    this.resourceLocator = resourceLocator;
    this.context = context;
    parse();
  }

  public static ReportParser parse(InputStream inputStream, ResourceLocator resourceLocator, SensorContext context)
    throws XMLStreamException {
    return new ReportParser(inputStream, resourceLocator, context);
  }

  public static ReportParser parse(java.io.File reportFile, ResourceLocator resourceLocator, SensorContext context)
    throws XMLStreamException {
    InputStream inputStream;
    try {
      inputStream = new FileInputStream(reportFile);
    } catch (FileNotFoundException e) {
      throw new SonarException(e);
    }
    return parse(inputStream, resourceLocator, context);
  }

  private void parse() throws XMLStreamException {
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
    checkElementName(rootCursor, "coverage");
    String version = rootCursor.getAttrValue("version");
    if (!"1".equals(version)) {
      String message = "Unknown coverage version: " + version + ". This parser only handles version 1.";
      throw new ReportParsingException(message, rootCursor);
    }
    parseFileMeasures(rootCursor.childElementCursor());
  }

  private void parseFileMeasures(SMInputCursor fileCursor) throws XMLStreamException {
    while (fileCursor.getNext() != null) {
      checkElementName(fileCursor, "file");
      String filePath = mandatoryAttribute(fileCursor, "path");
      File resource = resourceLocator.getResource(filePath);
      if (context.getResource(resource) == null) {
        numberOfUnknownFiles++;
        if (numberOfUnknownFiles <= MAX_STORED_UNKNOWN_FILE_PATHS) {
          firstUnknownFiles.add(filePath);
        }
        continue;
      }
      addMatchedFile(resource, filePath, fileCursor);

      CoverageMeasuresBuilder measureBuilder = CoverageMeasuresBuilder.create();

      SMInputCursor lineToCoverCursor = fileCursor.childElementCursor();
      Set<Integer> parsedLineNumbers = Sets.newHashSet();
      while (lineToCoverCursor.getNext() != null) {
        parseLineToCover(measureBuilder, lineToCoverCursor, parsedLineNumbers);
      }

      for (Measure measure : measureBuilder.createMeasures()) {
        context.saveMeasure(resource, measure);
      }
      numberOfMatchedFiles++;
    }
  }

  private void addMatchedFile(File resource, String filePath, SMInputCursor cursor) throws XMLStreamException {
    boolean added = matchedFileKeys.add(resource.getKey());
    if (!added) {
      throw new ReportParsingException("Coverage data cannot be added multiples times for the same file: " + filePath, cursor);
    }
  }

  private void parseLineToCover(CoverageMeasuresBuilder measureBuilder, SMInputCursor cursor, Set<Integer> parsedLineNumbers)
    throws XMLStreamException {

    checkElementName(cursor, "lineToCover");
    String lineNumberAsString = mandatoryAttribute(cursor, LINE_NUMBER_ATTR);
    int lineNumber = intValue(lineNumberAsString, cursor, LINE_NUMBER_ATTR, 1);
    addParsedLineNumber(parsedLineNumbers, lineNumber, cursor);

    String coveredAsString = mandatoryAttribute(cursor, COVERED_ATTR);
    if (!"true".equalsIgnoreCase(coveredAsString) && !"false".equalsIgnoreCase(coveredAsString)) {
      throw new ReportParsingException(expectedMessage("boolean value", COVERED_ATTR, coveredAsString), cursor);
    }
    boolean covered = Boolean.parseBoolean(coveredAsString);
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
      measureBuilder.setConditions(lineNumber, branchesToCover, coveredBranches);
    }
  }

  private void addParsedLineNumber(Set<Integer> parsedLineNumbers, int lineNumber, SMInputCursor cursor) throws XMLStreamException {
    boolean added = parsedLineNumbers.add(lineNumber);
    if (!added) {
      String message = "Coverage data cannot be added multiple times for the same line number (" + lineNumber + ") of the same file";
      throw new ReportParsingException(message, cursor);
    }
  }

  private void checkElementName(SMInputCursor cursor, String expectedName) throws XMLStreamException {
    String elementName = cursor.getLocalName();
    if (!expectedName.equals(elementName)) {
      String message = "Unknown XML node, expected \"" + expectedName + "\" but got \"" + elementName + "\"";
      throw new ReportParsingException(message, cursor);
    }
  }

  private String mandatoryAttribute(SMInputCursor cursor, String attributeName) throws XMLStreamException {
    String attributeValue = cursor.getAttrValue(attributeName);
    if (attributeValue == null) {
      String message = "Missing attribute \"" + attributeName + "\" in element \"" + cursor.getLocalName() + "\"";
      throw new ReportParsingException(message, cursor);
    }
    return attributeValue;
  }

  private int intValue(String stringValue, SMInputCursor cursor, String attributeName, int minimum) throws XMLStreamException {
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

  private String expectedMessage(String expected, String attributeName, String stringValue) {
    return "Expected " + expected + " for attribute \"" + attributeName + "\" but got \"" + stringValue + "\"";
  }

  public int numberOfMatchedFiles() {
    return numberOfMatchedFiles;
  }

  public int numberOfUnknownFiles() {
    return numberOfUnknownFiles;
  }

  public List<String> firstUnknownFiles() {
    return firstUnknownFiles;
  }

}
