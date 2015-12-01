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

import com.google.common.base.Splitter;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.config.Settings;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;

import java.io.File;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class GenericCoverageSensorTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Mock
  private Project project;
  @Mock
  private SensorContext context;
  @Mock
  private ResourcePerspectives perspectives;
  private GenericCoverageSensor sensor;
  private DefaultFileSystem fs;
  private Settings settings;

  private final File baseDir = new File("src/test/resources/project1").getAbsoluteFile();
  private StubLogger logger;

  @Before
  public void before() {
    MockitoAnnotations.initMocks(this);
    settings = new Settings();
    fs = new DefaultFileSystem().setBaseDir(baseDir);
    logger = new StubLogger();
    sensor = new GenericCoverageSensor(settings, fs, perspectives);
  }

  private List<StubLogger.LoggingEvent> getLoggingEvents() {
    return logger.getLoggingEvents();
  }

  @Test
  public void should_execute_when_report_path_is_provided() throws Exception {
    configureReportPaths("my-report");
    assertThat(sensor.shouldExecuteOnProject(project)).isTrue();
    configureReportPaths("");
    configureITReportPaths("my-report");
    assertThat(sensor.shouldExecuteOnProject(project)).isTrue();
    configureReportPaths("");
    configureITReportPaths("");
    configureOverallReportPaths("my-report");
    assertThat(sensor.shouldExecuteOnProject(project)).isTrue();
    configureReportPaths("");
    configureITReportPaths("");
    configureOverallReportPaths("");
    configureUTReportPaths("my-report");
    assertThat(sensor.shouldExecuteOnProject(project)).isTrue();
  }

  @Test
  public void should_not_execute_when_report_path_is_empty() throws Exception {
    configureReportPaths("");
    configureITReportPaths("");
    configureUTReportPaths("");
    configureOverallReportPaths("");
    assertThat(sensor.shouldExecuteOnProject(project)).isFalse();
  }

  @Test
  public void analyse_report_with_deprecated_key_and_no_logger() throws Exception {
    configureOldReportPath("coverage.xml");
    assertThat(sensor.shouldExecuteOnProject(project)).isTrue();
  }

  @Test
  public void analyse_report_with_deprecated_key() throws Exception {
    configureOldReportPath("coverage.xml");
    InputFile resource = addFileToContext("src/foobar.js");
    sensor.analyseWithLogger(context, logger);
    verify(context, times(3)).saveMeasure(eq(resource), any(Measure.class));

    assertThat(getLoggingEvents().get(0).getMessage())
      .isEqualTo("Use the new property \"sonar.genericcoverage.reportPaths\" instead of the deprecated \"sonar.genericcoverage.reportPath\"");
    assertThat(getLoggingEvents().get(1).getMessage()).contains("Parsing").contains("coverage.xml");
    assertThat(getLoggingEvents().get(2).getMessage()).contains("Imported coverage data for 1 file");
    assertThat(getLoggingEvents().get(4).getMessage()).contains("Imported IT coverage data for 0 files");
    assertThat(getLoggingEvents().get(5).getMessage()).contains("Imported Overall coverage data for 0 files");
    assertThat(getLoggingEvents().get(6).getMessage()).contains("Imported unit test data for 0 files");
  }

  @Test
  public void analyse_report_with_relative_path() throws Exception {
    configureReportPaths("coverage.xml");
    configureITReportPaths("coverage.xml");
    configureOverallReportPaths("coverage.xml");
    configureUTReportPaths("unittest.xml");
    InputFile resource = addFileToContext("src/foobar.js");
    InputFile testResource = addFileToContext("test/foobar_test.js");
    sensor.analyseWithLogger(context, logger);
    verify(context, times(9)).saveMeasure(eq(resource), any(Measure.class));
    verify(context, times(6)).saveMeasure(eq(testResource), any(Measure.class));

    assertThat(getLoggingEvents().get(0).getMessage()).contains("Parsing").contains("coverage.xml");
    assertThat(getLoggingEvents().get(1).getMessage()).contains("Imported coverage data for 1 file");
    assertThat(getLoggingEvents().get(3).getMessage()).contains("Parsing ").contains("coverage.xml");
    assertThat(getLoggingEvents().get(4).getMessage()).contains("Imported IT coverage data for 1 file");
    assertThat(getLoggingEvents().get(6).getMessage()).contains("Parsing ").contains("coverage.xml");
    assertThat(getLoggingEvents().get(7).getMessage()).contains("Imported Overall coverage data for 1 file");
    assertThat(getLoggingEvents().get(9).getMessage()).contains("Parsing ").contains("unittest.xml");
    assertThat(getLoggingEvents().get(10).getMessage()).contains("Imported unit test data for 1 file");
  }

  @Test
  public void analyse_report_with_multiple_relative_path() throws Exception {
    configureReportPaths("coverage.xml,coverage2.xml");
    configureITReportPaths("coverage.xml,coverage2.xml");
    configureOverallReportPaths("coverage.xml,coverage2.xml");
    configureUTReportPaths("unittest.xml,unittest2.xml");
    InputFile resource1 = addFileToContext("src/foobar.js");
    InputFile resource2 = addFileToContext("src/helloworld.js");
    InputFile resource3 = addFileToContext("src/third.js");
    InputFile testResource1 = addFileToContext("test/foobar_test.js");
    InputFile testResource2 = addFileToContext("test/helloworld_test.js");
    sensor.analyseWithLogger(context, logger);
    verify(context, times(9)).saveMeasure(eq(resource1), any(Measure.class));
    verify(context, times(9)).saveMeasure(eq(resource2), any(Measure.class));
    verify(context, times(9)).saveMeasure(eq(resource3), any(Measure.class));
    verify(context, times(6)).saveMeasure(eq(testResource1), any(Measure.class));
    verify(context, times(6)).saveMeasure(eq(testResource2), any(Measure.class));

    assertThat(getLoggingEvents().get(0).getMessage()).contains("Parsing").contains("coverage.xml");
    assertThat(getLoggingEvents().get(1).getMessage()).contains("Parsing").contains("coverage2.xml");
    assertThat(getLoggingEvents().get(2).getMessage()).isEqualTo("Imported coverage data for 3 files");
    assertThat(getLoggingEvents().get(3).getMessage()).contains("Parsing").contains("coverage.xml");
    assertThat(getLoggingEvents().get(4).getMessage()).contains("Parsing").contains("coverage2.xml");
    assertThat(getLoggingEvents().get(5).getMessage()).isEqualTo("Imported IT coverage data for 3 files");
    assertThat(getLoggingEvents().get(6).getMessage()).contains("Parsing").contains("coverage.xml");
    assertThat(getLoggingEvents().get(7).getMessage()).contains("Parsing").contains("coverage2.xml");
    assertThat(getLoggingEvents().get(8).getMessage()).isEqualTo("Imported Overall coverage data for 3 files");
    assertThat(getLoggingEvents().get(9).getMessage()).contains("Parsing").contains("unittest.xml");
    assertThat(getLoggingEvents().get(10).getMessage()).contains("Parsing").contains("unittest2.xml");
    assertThat(getLoggingEvents().get(11).getMessage()).isEqualTo("Imported unit test data for 2 files");
  }

  @Test
  public void analyse_report_with_absolute_path() throws Exception {
    File reportFile = new File(baseDir, "coverage.xml");
    configureReportPaths(reportFile.getAbsolutePath());
    InputFile resource = addFileToContext("src/foobar.js");
    sensor.analyse(project, context);
    verify(context, times(3)).saveMeasure(eq(resource), any(Measure.class));
  }

  @Test
  public void analyse_report_with_multiple_absolute_path() throws Exception {
    configureReportPaths(new File(baseDir, "coverage.xml").getAbsolutePath() + "," + new File(baseDir, "coverage2.xml").getAbsolutePath());
    configureITReportPaths(new File(baseDir, "coverage.xml").getAbsolutePath() + "," + new File(baseDir, "coverage2.xml").getAbsolutePath());
    configureOverallReportPaths(new File(baseDir, "coverage.xml").getAbsolutePath() + "," + new File(baseDir, "coverage2.xml").getAbsolutePath());
    configureUTReportPaths(new File(baseDir, "unittest.xml").getAbsolutePath() + "," + new File(baseDir, "unittest2.xml").getAbsolutePath());
    InputFile resource = addFileToContext("src/foobar.js");
    InputFile resource2 = addFileToContext("src/helloworld.js");
    InputFile testResource = addFileToContext("test/foobar_test.js");
    InputFile testResource2 = addFileToContext("test/helloworld_test.js");
    sensor.analyseWithLogger(context, logger);
    verify(context, times(9)).saveMeasure(eq(resource), any(Measure.class));
    verify(context, times(9)).saveMeasure(eq(resource2), any(Measure.class));
    verify(context, times(6)).saveMeasure(eq(testResource), any(Measure.class));
    verify(context, times(6)).saveMeasure(eq(testResource2), any(Measure.class));

    assertThat(getLoggingEvents().get(0).getMessage()).contains("Parsing").contains("coverage.xml");
    assertThat(getLoggingEvents().get(1).getMessage()).contains("Parsing").contains("coverage2.xml");
    assertThat(getLoggingEvents().get(2).getMessage()).contains("Imported coverage data for 2 file");
    assertThat(getLoggingEvents().get(4).getMessage()).contains("Parsing").contains("coverage.xml");
    assertThat(getLoggingEvents().get(5).getMessage()).contains("Parsing").contains("coverage2.xml");
    assertThat(getLoggingEvents().get(6).getMessage()).contains("Imported IT coverage data for 2 file");
    assertThat(getLoggingEvents().get(8).getMessage()).contains("Parsing").contains("coverage.xml");
    assertThat(getLoggingEvents().get(9).getMessage()).contains("Parsing").contains("coverage2.xml");
    assertThat(getLoggingEvents().get(10).getMessage()).contains("Imported Overall coverage data for 2 file");
    assertThat(getLoggingEvents().get(12).getMessage()).contains("Parsing").contains("unittest.xml");
    assertThat(getLoggingEvents().get(13).getMessage()).contains("Parsing").contains("unittest2.xml");
    assertThat(getLoggingEvents().get(14).getMessage()).contains("Imported unit test data for 2 file");
  }

  @Test
  public void analyse_report_with_unknown_files() throws Exception {
    configureReportPaths("coverage_with_2_unknown_files.xml");
    configureITReportPaths("coverage_with_2_unknown_files.xml");
    configureOverallReportPaths("coverage_with_2_unknown_files.xml");
    configureUTReportPaths("unittest_with_2_unknown_files.xml");
    sensor.analyseWithLogger(context, logger);
    assertThat(getLoggingEvents().get(2).getMessage()).contains("coverage data ignored for 2 unknown files");
    assertThat(getLoggingEvents().get(5).getMessage()).contains("IT coverage data ignored for 2 unknown files");
    assertThat(getLoggingEvents().get(8).getMessage()).contains("Overall coverage data ignored for 2 unknown files");
    assertThat(getLoggingEvents().get(11).getMessage()).contains("unit test data ignored for 2 unknown files");
  }

  @Test
  public void analyse_report_with_7_unknown_files() throws Exception {
    configureReportPaths("coverage_with_7_unknown_files.xml");
    configureITReportPaths("coverage_with_7_unknown_files.xml");
    configureOverallReportPaths("coverage_with_7_unknown_files.xml");
    configureUTReportPaths("unittest_with_7_unknown_files.xml");
    sensor.analyseWithLogger(context, logger);
    String message = getLoggingEvents().get(2).getMessage();
    assertThat(message).contains("coverage data ignored for 7 unknown files");
    assertThat(message).contains("unknown1.js");
    assertThat(Splitter.on("\n").split(message)).hasSize(6);
    message = getLoggingEvents().get(5).getMessage();
    assertThat(message).contains("IT coverage data ignored for 7 unknown files");
    assertThat(message).contains("unknown1.js");
    assertThat(Splitter.on("\n").split(message)).hasSize(6);
    message = getLoggingEvents().get(8).getMessage();
    assertThat(message).contains("Overall coverage data ignored for 7 unknown files");
    assertThat(message).contains("unknown1.js");
    assertThat(Splitter.on("\n").split(message)).hasSize(6);
    message = getLoggingEvents().get(11).getMessage();
    assertThat(message).contains("unit test data ignored for 7 unknown files");
    assertThat(message).contains("unknown1.js");
    assertThat(Splitter.on("\n").split(message)).hasSize(6);
  }

  @Test
  public void analyse_report_not_found() throws Exception {
    configureReportPaths("xxx");
    configureITReportPaths("");
    configureOverallReportPaths("");
    sensor.analyseWithLogger(context, logger);
    verifyZeroInteractions(context);
    assertThat(getLoggingEvents().get(1).getLevel()).isEqualTo("warn");
    assertThat(getLoggingEvents().get(1).getMessage()).contains("Cannot find coverage");
    configureReportPaths("");
    configureITReportPaths("xxx");
    configureOverallReportPaths("");
    sensor.analyseWithLogger(context, logger);
    verifyZeroInteractions(context);
    assertThat(getLoggingEvents().get(4).getLevel()).isEqualTo("warn");
    assertThat(getLoggingEvents().get(4).getMessage()).contains("Cannot find IT coverage");
    configureReportPaths("");
    configureITReportPaths("");
    configureUTReportPaths("xxx");
    configureOverallReportPaths("xxx");
    sensor.analyseWithLogger(context, logger);
    verifyZeroInteractions(context);
    assertThat(getLoggingEvents().get(8).getLevel()).isEqualTo("warn");
    assertThat(getLoggingEvents().get(8).getMessage()).contains("Cannot find Overall coverage");
    configureReportPaths("");
    configureOverallReportPaths("");
    configureUTReportPaths("xxx");
    sensor.analyseWithLogger(context, logger);
    verifyZeroInteractions(context);
    assertThat(getLoggingEvents().get(13).getMessage()).contains("Cannot find unit test");
    assertThat(getLoggingEvents().get(13).getLevel()).isEqualTo("warn");
  }

  @Test
  public void analyse_report_with_language_unknown_files() throws Exception {
    configureReportPaths("coverage.xml");
    String path = "src/foobar.js";
    DefaultInputFile inputFile = new DefaultInputFile(path).setAbsolutePath(path);
    fs.add(inputFile);
    when(context.getResource(inputFile)).thenReturn(mock(Resource.class));

    thrown.expectMessage("Line 2 of report coverage.xml refers to a file with an unknown language: src/foobar.js");
    thrown.expect(IllegalStateException.class);

    sensor.analyseWithLogger(context, logger);
  }

  @Test
  public void analyse_report_with_unittest_reports_on_main_files() throws Exception {
    configureUTReportPaths("unittest.xml");
    String path = "test/foobar_test.js";
    DefaultInputFile inputFile = new DefaultInputFile(path).setAbsolutePath(path).setType(InputFile.Type.MAIN).setLanguage("bla");
    fs.add(inputFile);
    when(context.getResource(inputFile)).thenReturn(mock(Resource.class));

    thrown.expectMessage("Line 2 of report unittest.xml refers to a file which is not configured as a test file: test/foobar_test.js");
    thrown.expect(IllegalStateException.class);

    sensor.analyseWithLogger(context, logger);
  }

  @Test(expected = IllegalStateException.class)
  public void analyse_txt_report() throws Exception {
    configureReportPaths("not-xml.txt");
    sensor.analyse(project, context);
  }

  @Test(expected = IllegalStateException.class)
  public void it_analyse_txt_report() throws Exception {
    configureITReportPaths("not-xml.txt");
    sensor.analyse(project, context);
  }

  @Test(expected = IllegalStateException.class)
  public void overall_analyse_txt_report() throws Exception {
    configureOverallReportPaths("not-xml.txt");
    sensor.analyse(project, context);
  }

  @Test(expected = IllegalStateException.class)
  public void ut_analyse_txt_report() throws Exception {
    configureUTReportPaths("not-xml.txt");
    sensor.analyse(project, context);
  }

  @Test(expected = IllegalStateException.class)
  public void analyse_invalid_report() throws Exception {
    configureReportPaths("invalid-coverage.xml");
    sensor.analyse(project, context);
  }

  @Test(expected = IllegalStateException.class)
  public void it_analyse_invalid_report() throws Exception {
    configureITReportPaths("invalid-coverage.xml");
    sensor.analyse(project, context);
  }

  @Test(expected = IllegalStateException.class)
  public void overall_analyse_invalid_report() throws Exception {
    configureOverallReportPaths("invalid-coverage.xml");
    sensor.analyse(project, context);
  }

  @Test(expected = IllegalStateException.class)
  public void ut_analyse_invalid_report() throws Exception {
    configureUTReportPaths("invalid-unittest.xml");
    sensor.analyse(project, context);
  }

  @Test
  public void to_string() throws Exception {
    assertThat(sensor.toString()).isEqualTo("GenericCoverageSensor");
  }

  private void configureOldReportPath(String reportPath) {
    settings.setProperty(GenericCoveragePlugin.OLD_REPORT_PATH_PROPERTY_KEY, reportPath);
  }

  private void configureReportPaths(String reportPaths) {
    settings.setProperty(GenericCoveragePlugin.COVERAGE_REPORT_PATHS_PROPERTY_KEY, reportPaths);
  }

  private void configureITReportPaths(String itReportPaths) {
    settings.setProperty(GenericCoveragePlugin.IT_COVERAGE_REPORT_PATHS_PROPERTY_KEY, itReportPaths);
  }

  private void configureOverallReportPaths(String overallReportPaths) {
    settings.setProperty(GenericCoveragePlugin.OVERALL_COVERAGE_REPORT_PATHS_PROPERTY_KEY, overallReportPaths);
  }

  private void configureUTReportPaths(String utReportPaths) {
    settings.setProperty(GenericCoveragePlugin.UNIT_TEST_REPORT_PATHS_PROPERTY_KEY, utReportPaths);
  }

  private InputFile addFileToContext(String filePath) {
    DefaultInputFile inputFile = new DefaultInputFile(filePath).setAbsolutePath(filePath).setLanguage("bla").setType(InputFile.Type.TEST);
    fs.add(inputFile);
    when(context.getResource(inputFile)).thenReturn(mock(Resource.class));
    return inputFile;
  }

}
