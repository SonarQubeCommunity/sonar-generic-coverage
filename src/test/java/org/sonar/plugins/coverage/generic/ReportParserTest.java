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

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.File;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.api.utils.SonarException;

import java.io.ByteArrayInputStream;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.refEq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ReportParserTest {

  @Mock
  private ResourceLocator resourceLocator;
  @Mock
  private SensorContext context;
  private File fileWithBranches;
  private File fileWithoutBranch;
  private File emptyFile;

  @Before
  public void before() {
    MockitoAnnotations.initMocks(this);
    fileWithBranches = setupFile("src/main/java/com/example/ClassWithBranches.java");
    fileWithoutBranch = setupFile("src/main/java/com/example/ClassWithoutBranch.java");
    emptyFile = setupFile("src/main/java/com/example/EmptyClass.java");
  }

  @Test
  public void empty_file() throws Exception {
    File file = emptyFile;
    addFileToContext(file);
    ReportParser parser = parseReportFile("src/test/resources/coverage.xml");
    assertThat(parser.numberOfMatchedFiles()).isEqualTo(1);
    assertThat(parser.numberOfUnknownFiles()).isEqualTo(2);
    assertThat(parser.firstUnknownFiles()).hasSize(2);
    verify(context, never()).saveMeasure(any(File.class), any(Measure.class));
  }

  @Test
  public void file_without_branch() throws Exception {
    File file = fileWithoutBranch;
    addFileToContext(file);
    ReportParser parser = parseReportFile("src/test/resources/coverage.xml");
    parser.saveMeasures();
    assertThat(parser.numberOfMatchedFiles()).isEqualTo(1);
    verify(context).saveMeasure(eq(file), refEq(new Measure(CoreMetrics.LINES_TO_COVER, 4.)));
    verify(context).saveMeasure(eq(file), refEq(new Measure(CoreMetrics.UNCOVERED_LINES, 2.)));
    verify(context).saveMeasure(eq(file), dataMeasure(CoreMetrics.COVERAGE_LINE_HITS_DATA, ImmutableMap.of(2, 0, 3, 1, 5, 1, 6, 0)));
  }

  @Test
  public void file_with_branches() throws Exception {
    File file = fileWithBranches;
    addFileToContext(file);
    ReportParser parser = parseReportFile("src/test/resources/coverage.xml");
    assertThat(parser.numberOfMatchedFiles()).isEqualTo(1);
    parser.saveMeasures();
    verify(context).saveMeasure(eq(file), refEq(new Measure(CoreMetrics.LINES_TO_COVER, 2.)));
    verify(context).saveMeasure(eq(file), refEq(new Measure(CoreMetrics.UNCOVERED_LINES, 0.)));
    verify(context).saveMeasure(eq(file), dataMeasure(CoreMetrics.COVERAGE_LINE_HITS_DATA, ImmutableMap.of(3, 1, 4, 1)));
    verify(context).saveMeasure(eq(file), refEq(new Measure(CoreMetrics.CONDITIONS_TO_COVER, 10.)));
    verify(context).saveMeasure(eq(file), refEq(new Measure(CoreMetrics.UNCOVERED_CONDITIONS, 5.)));
    verify(context).saveMeasure(eq(file), dataMeasure(CoreMetrics.CONDITIONS_BY_LINE, ImmutableMap.of(3, 8, 4, 2)));
    verify(context).saveMeasure(eq(file), dataMeasure(CoreMetrics.COVERED_CONDITIONS_BY_LINE, ImmutableMap.of(3, 5, 4, 0)));
  }

  @Test
  public void files_with_branches_merge() throws Exception {
    File file = fileWithBranches;
    addFileToContext(file);
    ReportParser parser = parseReportFile("src/test/resources/coverage.xml");
    parser.parse(new java.io.File("src/test/resources/coverage2.xml"));
    parser.saveMeasures();
    assertThat(parser.numberOfMatchedFiles()).isEqualTo(1);
    verify(context).saveMeasure(eq(file), refEq(new Measure(CoreMetrics.LINES_TO_COVER, 2.)));
    verify(context).saveMeasure(eq(file), refEq(new Measure(CoreMetrics.UNCOVERED_LINES, 0.)));
    verify(context).saveMeasure(eq(file), dataMeasure(CoreMetrics.COVERAGE_LINE_HITS_DATA, ImmutableMap.of(3, 1, 4, 1)));
    verify(context).saveMeasure(eq(file), refEq(new Measure(CoreMetrics.CONDITIONS_TO_COVER, 10.)));
    verify(context).saveMeasure(eq(file), refEq(new Measure(CoreMetrics.UNCOVERED_CONDITIONS, 3.)));
    verify(context).saveMeasure(eq(file), dataMeasure(CoreMetrics.CONDITIONS_BY_LINE, ImmutableMap.of(3, 8, 4, 2)));
    verify(context).saveMeasure(eq(file), dataMeasure(CoreMetrics.COVERED_CONDITIONS_BY_LINE, ImmutableMap.of(3, 7, 4, 0)));
  }

  @Test(expected = ReportParsingException.class)
  public void invalid_root_node_name() throws Exception {
    parseReportString("<mycoverage version=\"1\"></mycoverage>");
  }

  @Test(expected = ReportParsingException.class)
  public void invalid_report_version() throws Exception {
    parseReportString("<coverage version=\"2\"></coverage>");
  }

  @Test(expected = ReportParsingException.class)
  public void no_report_version() throws Exception {
    parseReportString("<coverage></coverage>");
  }

  @Test(expected = ReportParsingException.class)
  public void invalid_file_node_name() throws Exception {
    parseReportString("<coverage version=\"1\"><xx></xx></coverage>");
  }

  @Test(expected = ReportParsingException.class)
  public void missing_path_attribute() throws Exception {
    parseReportString("<coverage version=\"1\"><file></file></coverage>");
  }

  @Test(expected = ReportParsingException.class)
  public void invalid_lineToCover_node_name() throws Exception {
    addFileToContext(setupFile("file1"));
    parseReportString("<coverage version=\"1\"><file path=\"file1\"><xx/></file></coverage>");
  }

  @Test(expected = ReportParsingException.class)
  public void missing_lineNumber_in_lineToCover() throws Exception {
    addFileToContext(setupFile("file1"));
    parseReportString("<coverage version=\"1\"><file path=\"file1\"><lineToCover covered=\"true\"/></file></coverage>");
  }

  @Test(expected = ReportParsingException.class)
  public void lineNumber_in_lineToCover_should_be_a_number() throws Exception {
    addFileToContext(setupFile("file1"));
    parseReportString("<coverage version=\"1\"><file path=\"file1\"><lineToCover lineNumber=\"x\" covered=\"true\"/></file></coverage>");
  }

  @Test(expected = ReportParsingException.class)
  public void lineNumber_in_lineToCover_should_be_positive() throws Exception {
    addFileToContext(setupFile("file1"));
    parseReportString("<coverage version=\"1\"><file path=\"file1\"><lineToCover lineNumber=\"0\" covered=\"true\"/></file></coverage>");
  }

  @Test
  public void lineNumber_in_lineToCover_can_appear_several_times_for_same_file() throws Exception {
    addFileToContext(setupFile("file1"));
    parseReportString("<coverage version=\"1\"><file path=\"file1\">"
      + "<lineToCover lineNumber=\"1\" covered=\"true\"/>"
      + "<lineToCover lineNumber=\"1\" covered=\"true\"/></file></coverage>");
  }

  @Test(expected = ReportParsingException.class)
  public void missing_covered_in_lineToCover() throws Exception {
    addFileToContext(setupFile("file1"));
    parseReportString("<coverage version=\"1\"><file path=\"file1\"><lineToCover lineNumber=\"3\"/></file></coverage>");
  }

  @Test(expected = ReportParsingException.class)
  public void covered_in_lineToCover_should_be_a_boolean() throws Exception {
    addFileToContext(setupFile("file1"));
    parseReportString("<coverage version=\"1\"><file path=\"file1\"><lineToCover lineNumber=\"3\" covered=\"x\"/></file></coverage>");
  }

  @Test(expected = ReportParsingException.class)
  public void branchesToCover_in_lineToCover_should_be_a_number() throws Exception {
    addFileToContext(setupFile("file1"));
    parseReportString("<coverage version=\"1\"><file path=\"file1\">"
      + "<lineToCover lineNumber=\"1\" covered=\"true\" branchesToCover=\"x\"/></file></coverage>");
  }

  @Test(expected = ReportParsingException.class)
  public void branchesToCover_in_lineToCover_should_not_be_negative() throws Exception {
    addFileToContext(setupFile("file1"));
    parseReportString("<coverage version=\"1\"><file path=\"file1\">"
      + "<lineToCover lineNumber=\"1\" covered=\"true\" branchesToCover=\"-1\"/></file></coverage>");
  }

  @Test(expected = ReportParsingException.class)
  public void coveredBranches_in_lineToCover_should_be_a_number() throws Exception {
    addFileToContext(setupFile("file1"));
    parseReportString("<coverage version=\"1\"><file path=\"file1\">"
      + "<lineToCover lineNumber=\"1\" covered=\"true\" branchesToCover=\"2\" coveredBranches=\"x\"/></file></coverage>");
  }

  @Test(expected = ReportParsingException.class)
  public void coveredBranches_in_lineToCover_should_not_be_negative() throws Exception {
    addFileToContext(setupFile("file1"));
    parseReportString("<coverage version=\"1\"><file path=\"file1\">"
      + "<lineToCover lineNumber=\"1\" covered=\"true\" branchesToCover=\"2\" coveredBranches=\"-1\"/></file></coverage>");
  }

  @Test(expected = ReportParsingException.class)
  public void coveredBranches_should_not_be_greater_than_branchesToCover() throws Exception {
    addFileToContext(setupFile("file1"));
    parseReportString("<coverage version=\"1\"><file path=\"file1\">"
      + "<lineToCover lineNumber=\"1\" covered=\"true\" branchesToCover=\"2\" coveredBranches=\"3\"/></file></coverage>");
  }

  @Test(expected = ReportParsingException.class)
  public void coveredBranches_should_not_mismatch_on_merged_reports() throws Exception {
    addFileToContext(setupFile("file1"));
    parseReportString("<coverage version=\"1\"><file path=\"file1\">"
      + "<lineToCover lineNumber=\"1\" covered=\"true\" branchesToCover=\"2\" coveredBranches=\"1\"/>" +
      "<lineToCover lineNumber=\"1\" covered=\"true\" branchesToCover=\"3\" coveredBranches=\"1\"/></file></coverage>");
  }

  @Test(expected = SonarException.class)
  public void testUnknownFile() throws Exception {
    parseReportFile("xxx.xml");
  }

  private void addFileToContext(File file1) {
    when(context.getResource(file1)).thenReturn(file1);
  }

  private ReportParser parseReportString(String string) throws Exception {
    ByteArrayInputStream inputStream = new ByteArrayInputStream(string.getBytes());
    ReportParser reportParser = new ReportParser(resourceLocator, context);
    reportParser.parse(inputStream);
    return reportParser;
  }

  private ReportParser parseReportFile(String reportLocation) throws Exception {
    ReportParser reportParser = new ReportParser(resourceLocator, context);
    reportParser.parse(new java.io.File(reportLocation));
    return reportParser;
  }

  private Measure dataMeasure(Metric metric, Map<Integer, Integer> data) {
    return refEq(new Measure(metric).setData(KeyValueFormat.format(data)), "persistenceMode");
  }

  private org.sonar.api.resources.File setupFile(String path) {
    File sonarFile = mock(File.class, "File[" + path + "]");
    when(resourceLocator.getResource(path)).thenReturn(sonarFile);
    return sonarFile;
  }

}
