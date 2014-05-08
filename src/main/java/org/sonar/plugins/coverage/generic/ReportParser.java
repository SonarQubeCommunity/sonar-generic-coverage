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
import org.codehaus.staxmate.in.SMHierarchicCursor;
import org.codehaus.staxmate.in.SMInputCursor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.measures.CoverageMeasuresBuilder;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.File;
import org.sonar.api.utils.StaxParser;

import javax.xml.stream.XMLStreamException;

import java.io.InputStream;
import java.util.List;

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

      CoverageMeasuresBuilder measureBuilder = CoverageMeasuresBuilder.create();

      SMInputCursor lineToCoverCursor = fileCursor.childElementCursor();
      while (lineToCoverCursor.getNext() != null) {
        parseLineToCover(measureBuilder, lineToCoverCursor);
      }

      for (Measure measure : measureBuilder.createMeasures()) {
        context.saveMeasure(resource, measure);
      }
      numberOfMatchedFiles++;
    }
  }

  private void parseLineToCover(CoverageMeasuresBuilder measureBuilder, SMInputCursor cursor) throws XMLStreamException {
    checkElementName(cursor, "lineToCover");
    String lineNumberAsString = mandatoryAttribute(cursor, LINE_NUMBER_ATTR);
    int lineNumber = intValue(lineNumberAsString, cursor, LINE_NUMBER_ATTR);
    String coveredAsString = mandatoryAttribute(cursor, COVERED_ATTR);
    boolean covered = Boolean.parseBoolean(coveredAsString);
    measureBuilder.setHits(lineNumber, covered ? 1 : 0);

    String branchesToCover = cursor.getAttrValue(BRANCHES_TO_COVER_ATTR);
    if (branchesToCover != null) {
      String coveredBranchesAsString = cursor.getAttrValue(COVERED_BRANCHES_ATTR);
      int coveredBranches = coveredBranchesAsString == null ? 0 : intValue(coveredBranchesAsString, cursor, COVERED_BRANCHES_ATTR);
      measureBuilder.setConditions(lineNumber, intValue(branchesToCover, cursor, BRANCHES_TO_COVER_ATTR), coveredBranches);
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

  private int intValue(String stringValue, SMInputCursor cursor, String attributeName) throws XMLStreamException {
    try {
      return Integer.valueOf(stringValue);
    } catch (NumberFormatException e) {
      String message = "Expected integer value for attribute \"" + attributeName + "\" but got \"" + stringValue + "\"";
      throw new ReportParsingException(message, e, cursor.getCursorLocation().getLineNumber());
    }
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
