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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.config.Settings;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.ProjectFileSystem;
import org.sonar.api.scan.filesystem.ModuleFileSystem;
import org.sonar.api.utils.SonarException;

import java.io.File;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class GenericCoverageSensorTest {

  @Mock
  private Settings settings;
  @Mock
  private Project project;
  @Mock
  private ModuleFileSystem fs;
  @Mock
  private SensorContext context;
  @Mock
  private ResourcePerspectives perspectives;
  private GenericCoverageSensor sensor;

  @Mock
  private Appender<ILoggingEvent> mockAppender;
  private final List<ILoggingEvent> loggingEvents = Lists.newArrayList();

  private final File baseDir = new File("src/test/resources/project1").getAbsoluteFile();

  @Before
  public void before() {
    MockitoAnnotations.initMocks(this);
    setupMockLogAppender();
    sensor = new GenericCoverageSensor(settings, fs, perspectives);
    when(fs.baseDir()).thenReturn(baseDir);
    ProjectFileSystem projectFs = mock(ProjectFileSystem.class);
    when(project.getFileSystem()).thenReturn(projectFs);
    when(projectFs.getSourceDirs()).thenReturn(ImmutableList.of(new File(baseDir, "src")));
    when(projectFs.getTestDirs()).thenReturn(ImmutableList.of(new File(baseDir, "test")));
  }

  public void setupMockLogAppender() {
    ch.qos.logback.classic.Logger root =
      (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
    when(mockAppender.getName()).thenReturn("MOCK");
    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        loggingEvents.add((ILoggingEvent) invocation.getArguments()[0]);
        return null;
      }
    }).when(mockAppender).doAppend(any(ILoggingEvent.class));
    root.addAppender(mockAppender);
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
    configureUTReportPaths("my-report");
    assertThat(sensor.shouldExecuteOnProject(project)).isTrue();
  }

  @Test
  public void should_not_execute_when_report_path_is_empty() throws Exception {
    configureReportPaths("");
    configureITReportPaths("");
    configureUTReportPaths("");
    assertThat(sensor.shouldExecuteOnProject(project)).isFalse();
  }

  @Test
  public void analyse_report_with_deprecated_key() throws Exception {
    configureOldReportPath("coverage.xml");
    org.sonar.api.resources.File resource = addFileToContext("src/test/resources/project1/src/foobar.js");
    sensor.analyse(project, context);
    verify(context, times(3)).saveMeasure(eq(resource), any(Measure.class));

    assertThat(loggingEvents.get(0).getMessage())
      .isEqualTo("Use the new property \"sonar.genericcoverage.reportPaths\" instead of the deprecated \"sonar.genericcoverage.reportPath\"");
    assertThat(loggingEvents.get(1).getMessage()).contains("Parsing").contains("coverage.xml");
    assertThat(loggingEvents.get(2).getMessage()).contains("Imported coverage data for 1 file");
    assertThat(loggingEvents.get(4).getMessage()).contains("Imported IT coverage data for 0 files");
    assertThat(loggingEvents.get(5).getMessage()).contains("Imported unit test data for 0 files");
  }

  @Test
  public void analyse_report_with_relative_path() throws Exception {
    configureReportPaths("coverage.xml");
    configureITReportPaths("coverage.xml");
    configureUTReportPaths("unittest.xml");
    org.sonar.api.resources.File resource = addFileToContext("src/test/resources/project1/src/foobar.js");
    org.sonar.api.resources.File testResource = addTestFileToContext("src/test/resources/project1/test/foobar_test.js");
    sensor.analyse(project, context);
    verify(context, times(6)).saveMeasure(eq(resource), any(Measure.class));
    verify(context, times(6)).saveMeasure(eq(testResource), any(Measure.class));

    assertThat(loggingEvents.get(0).getMessage()).contains("Parsing").contains("coverage.xml");
    assertThat(loggingEvents.get(1).getMessage()).contains("Imported coverage data for 1 file");
    assertThat(loggingEvents.get(3).getMessage()).contains("Parsing ").contains("coverage.xml");
    assertThat(loggingEvents.get(4).getMessage()).contains("Imported IT coverage data for 1 file");
    assertThat(loggingEvents.get(6).getMessage()).contains("Parsing ").contains("unittest.xml");
    assertThat(loggingEvents.get(7).getMessage()).contains("Imported unit test data for 1 file");
  }

  @Test
  public void analyse_report_with_multiple_relative_path() throws Exception {
    configureReportPaths("coverage.xml,coverage2.xml");
    configureITReportPaths("coverage.xml,coverage2.xml");
    configureUTReportPaths("unittest.xml,unittest2.xml");
    org.sonar.api.resources.File resource1 = addFileToContext("src/test/resources/project1/src/foobar.js");
    org.sonar.api.resources.File resource2 = addFileToContext("src/test/resources/project1/src/helloworld.js");
    org.sonar.api.resources.File resource3 = addFileToContext("src/test/resources/project1/src/third.js");
    org.sonar.api.resources.File testResource1 = addTestFileToContext("src/test/resources/project1/src/foobar_test.js");
    org.sonar.api.resources.File testResource2 = addTestFileToContext("src/test/resources/project1/src/helloworld_test.js");
    sensor.analyse(project, context);
    verify(context, times(6)).saveMeasure(eq(resource1), any(Measure.class));
    verify(context, times(6)).saveMeasure(eq(resource2), any(Measure.class));
    verify(context, times(6)).saveMeasure(eq(resource3), any(Measure.class));
    verify(context, times(6)).saveMeasure(eq(testResource1), any(Measure.class));
    verify(context, times(6)).saveMeasure(eq(testResource2), any(Measure.class));

    assertThat(loggingEvents.get(0).getMessage()).contains("Parsing").contains("coverage.xml");
    assertThat(loggingEvents.get(1).getMessage()).contains("Parsing").contains("coverage2.xml");
    assertThat(loggingEvents.get(2).getMessage()).isEqualTo("Imported coverage data for 3 files");
    assertThat(loggingEvents.get(3).getMessage()).contains("Parsing").contains("coverage.xml");
    assertThat(loggingEvents.get(4).getMessage()).contains("Parsing").contains("coverage2.xml");
    assertThat(loggingEvents.get(5).getMessage()).isEqualTo("Imported IT coverage data for 3 files");
    assertThat(loggingEvents.get(6).getMessage()).contains("Parsing").contains("unittest.xml");
    assertThat(loggingEvents.get(7).getMessage()).contains("Parsing").contains("unittest2.xml");
    assertThat(loggingEvents.get(8).getMessage()).isEqualTo("Imported unit test data for 2 files");
  }

  @Test
  public void analyse_report_with_absolute_path() throws Exception {
    File reportFile = new File(baseDir, "coverage.xml");
    configureReportPaths(reportFile.getAbsolutePath());
    org.sonar.api.resources.File resource = addFileToContext("src/test/resources/project1/src/foobar.js");
    sensor.analyse(project, context);
    verify(context, times(3)).saveMeasure(eq(resource), any(Measure.class));
  }

  @Test
  public void analyse_report_with_multiple_absolute_path() throws Exception {
    configureReportPaths(new File(baseDir, "coverage.xml").getAbsolutePath() + "," + new File(baseDir, "coverage2.xml").getAbsolutePath());
    configureITReportPaths(new File(baseDir, "coverage.xml").getAbsolutePath() + "," + new File(baseDir, "coverage2.xml").getAbsolutePath());
    configureUTReportPaths(new File(baseDir, "unittest.xml").getAbsolutePath() + "," + new File(baseDir, "unittest2.xml").getAbsolutePath());
    org.sonar.api.resources.File resource = addFileToContext("src/test/resources/project1/src/foobar.js");
    org.sonar.api.resources.File resource2 = addFileToContext("src/test/resources/project1/src/helloworld.js");
    org.sonar.api.resources.File testResource = addFileToContext("src/test/resources/project1/src/foobar_test.js");
    org.sonar.api.resources.File testResource2 = addFileToContext("src/test/resources/project1/src/helloworld_test.js");
    sensor.analyse(project, context);
    verify(context, times(6)).saveMeasure(eq(resource), any(Measure.class));
    verify(context, times(6)).saveMeasure(eq(resource2), any(Measure.class));
    verify(context, times(6)).saveMeasure(eq(testResource), any(Measure.class));
    verify(context, times(6)).saveMeasure(eq(testResource2), any(Measure.class));

    assertThat(loggingEvents.get(0).getMessage()).contains("Parsing").contains("coverage.xml");
    assertThat(loggingEvents.get(1).getMessage()).contains("Parsing").contains("coverage2.xml");
    assertThat(loggingEvents.get(2).getMessage()).contains("Imported coverage data for 2 file");
    assertThat(loggingEvents.get(4).getMessage()).contains("Parsing").contains("coverage.xml");
    assertThat(loggingEvents.get(5).getMessage()).contains("Parsing").contains("coverage2.xml");
    assertThat(loggingEvents.get(6).getMessage()).contains("Imported IT coverage data for 2 file");
    assertThat(loggingEvents.get(8).getMessage()).contains("Parsing").contains("unittest.xml");
    assertThat(loggingEvents.get(9).getMessage()).contains("Parsing").contains("unittest2.xml");
    assertThat(loggingEvents.get(10).getMessage()).contains("Imported unit test data for 2 file");
  }

  @Test
  public void analyse_report_with_unknown_files() throws Exception {
    configureReportPaths("coverage_with_2_unknown_files.xml");
    configureITReportPaths("coverage_with_2_unknown_files.xml");
    configureUTReportPaths("unittest_with_2_unknown_files.xml");
    sensor.analyse(project, context);
    assertThat(loggingEvents.get(2).getMessage()).contains("coverage data ignored for 2 unknown files");
    assertThat(loggingEvents.get(5).getMessage()).contains("IT coverage data ignored for 2 unknown files");
    assertThat(loggingEvents.get(8).getMessage()).contains("unit test data ignored for 2 unknown files");
  }

  @Test
  public void analyse_report_with_7_unknown_files() throws Exception {
    configureReportPaths("coverage_with_7_unknown_files.xml");
    configureITReportPaths("coverage_with_7_unknown_files.xml");
    configureUTReportPaths("unittest_with_7_unknown_files.xml");
    sensor.analyse(project, context);
    String message = loggingEvents.get(2).getMessage();
    assertThat(message).contains("coverage data ignored for 7 unknown files");
    assertThat(message).contains("unknown1.js");
    assertThat(Splitter.on("\n").split(message)).hasSize(6);
    message = loggingEvents.get(5).getMessage();
    assertThat(message).contains("IT coverage data ignored for 7 unknown files");
    assertThat(message).contains("unknown1.js");
    assertThat(Splitter.on("\n").split(message)).hasSize(6);
    message = loggingEvents.get(8).getMessage();
    assertThat(message).contains("unit test data ignored for 7 unknown files");
    assertThat(message).contains("unknown1.js");
    assertThat(Splitter.on("\n").split(message)).hasSize(6);
  }

  @Test
  public void analyse_report_not_found() throws Exception {
    configureReportPaths("xxx");
    sensor.analyse(project, context);
    verifyZeroInteractions(context);
    assertThat(loggingEvents.get(1).getLevel()).isEqualTo(Level.WARN);
    assertThat(loggingEvents.get(1).getMessage()).contains("Cannot find coverage");
    configureReportPaths("");
    configureITReportPaths("xxx");
    sensor.analyse(project, context);
    verifyZeroInteractions(context);
    assertThat(loggingEvents.get(4).getLevel()).isEqualTo(Level.WARN);
    assertThat(loggingEvents.get(4).getMessage()).contains("Cannot find IT coverage");
    configureReportPaths("");
    configureITReportPaths("");
    configureUTReportPaths("xxx");
    sensor.analyse(project, context);
    verifyZeroInteractions(context);
    assertThat(loggingEvents.get(8).getLevel()).isEqualTo(Level.WARN);
    assertThat(loggingEvents.get(8).getMessage()).contains("Cannot find unit test");
  }

  @Test(expected = SonarException.class)
  public void analyse_txt_report() throws Exception {
    configureReportPaths("not-xml.txt");
    sensor.analyse(project, context);
  }

  @Test(expected = SonarException.class)
  public void it_analyse_txt_report() throws Exception {
    configureITReportPaths("not-xml.txt");
    sensor.analyse(project, context);
  }

  @Test(expected = SonarException.class)
  public void ut_analyse_txt_report() throws Exception {
    configureUTReportPaths("not-xml.txt");
    sensor.analyse(project, context);
  }

  @Test(expected = SonarException.class)
  public void analyse_invalid_report() throws Exception {
    configureReportPaths("invalid-coverage.xml");
    sensor.analyse(project, context);
  }

  @Test(expected = SonarException.class)
  public void it_analyse_invalid_report() throws Exception {
    configureITReportPaths("invalid-coverage.xml");
    sensor.analyse(project, context);
  }

  @Test(expected = SonarException.class)
  public void ut_analyse_invalid_report() throws Exception {
    configureUTReportPaths("invalid-unittest.xml");
    sensor.analyse(project, context);
  }

  @Test
  public void to_string() throws Exception {
    assertThat(sensor.toString()).isEqualTo("GenericCoverageSensor");
  }

  private void configureOldReportPath(String reportPath) {
    when(settings.getString(GenericCoveragePlugin.OLD_REPORT_PATH_PROPERTY_KEY)).thenReturn(reportPath);
  }

  private void configureReportPaths(String reportPaths) {
    when(settings.getString(GenericCoveragePlugin.REPORT_PATHS_PROPERTY_KEY)).thenReturn(reportPaths);
  }

  private void configureITReportPaths(String itReportPaths) {
    when(settings.getString(GenericCoveragePlugin.IT_REPORT_PATHS_PROPERTY_KEY)).thenReturn(itReportPaths);
  }

  private void configureUTReportPaths(String utReportPaths) {
    when(settings.getString(GenericCoveragePlugin.UNIT_TEST_REPORT_PATHS_PROPERTY_KEY)).thenReturn(utReportPaths);
  }

  private org.sonar.api.resources.File addFileToContext(String filePath) {
    org.sonar.api.resources.File sonarFile = org.sonar.api.resources.File.fromIOFile(new File(filePath), project);
    when(context.getResource(sonarFile)).thenReturn(sonarFile);
    return sonarFile;
  }

  private org.sonar.api.resources.File addTestFileToContext(String filePath) {
    org.sonar.api.resources.File sonarFile = org.sonar.api.resources.File.fromIOFile(new File(filePath), project);
    if (sonarFile == null) {
      sonarFile = org.sonar.api.resources.File.fromIOFile(new File(filePath), project.getFileSystem().getTestDirs());
    }
    when(context.getResource(sonarFile)).thenReturn(sonarFile);
    return sonarFile;
  }

}
