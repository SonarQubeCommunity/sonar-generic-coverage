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
  private GenericCoverageSensor sensor;

  @Mock
  private Appender<ILoggingEvent> mockAppender;
  private final List<ILoggingEvent> loggingEvents = Lists.newArrayList();

  private final File baseDir = new File("src/test/resources/project1").getAbsoluteFile();

  @Before
  public void before() {
    MockitoAnnotations.initMocks(this);
    setupMockLogAppender();
    sensor = new GenericCoverageSensor(settings, fs);
    when(fs.baseDir()).thenReturn(baseDir);
    ProjectFileSystem projectFs = mock(ProjectFileSystem.class);
    when(project.getFileSystem()).thenReturn(projectFs);
    when(projectFs.getSourceDirs()).thenReturn(ImmutableList.of(new File(baseDir, "src")));
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
    configureReportPath("my-report");
    assertThat(sensor.shouldExecuteOnProject(project)).isTrue();
  }

  @Test
  public void should_not_execute_when_report_path_is_empty() throws Exception {
    configureReportPath("");
    assertThat(sensor.shouldExecuteOnProject(project)).isFalse();
  }

  @Test
  public void analyse_report_with_relative_path() throws Exception {
    configureReportPath("coverage.xml");
    org.sonar.api.resources.File resource = addFileToContext("src/test/resources/project1/src/foobar.js");
    sensor.analyse(project, context);
    verify(context, times(3)).saveMeasure(eq(resource), any(Measure.class));

    assertThat(loggingEvents.get(0).getMessage()).contains("Parsing").contains("coverage.xml");
    assertThat(loggingEvents.get(1).getMessage()).contains("Imported coverage data for 1 file");
  }

  @Test
  public void analyse_report_with_multiple_relative_path() throws Exception {
    configureReportPath("coverage.xml,coverage2.xml");
    org.sonar.api.resources.File resource1 = addFileToContext("src/test/resources/project1/src/foobar.js");
    org.sonar.api.resources.File resource2 = addFileToContext("src/test/resources/project1/src/helloworld.js");
    sensor.analyse(project, context);
    verify(context, times(3)).saveMeasure(eq(resource1), any(Measure.class));
    verify(context, times(3)).saveMeasure(eq(resource2), any(Measure.class));

    assertThat(loggingEvents.get(0).getMessage()).contains("Parsing").contains("coverage.xml");
    assertThat(loggingEvents.get(1).getMessage()).contains("Parsing").contains("coverage2.xml");
    assertThat(loggingEvents.get(2).getMessage()).contains("Imported coverage data for 2 file");
  }

  @Test
  public void analyse_report_with_absolute_path() throws Exception {
    File reportFile = new File(baseDir, "coverage.xml");
    configureReportPath(reportFile.getAbsolutePath());
    org.sonar.api.resources.File resource = addFileToContext("src/test/resources/project1/src/foobar.js");
    sensor.analyse(project, context);
    verify(context, times(3)).saveMeasure(eq(resource), any(Measure.class));
  }

  @Test
  public void analyse_report_with_multiple_absolute_path() throws Exception {
    configureReportPath(new File(baseDir, "coverage.xml").getAbsolutePath() + "," +  new File(baseDir, "coverage2.xml").getAbsolutePath());
    org.sonar.api.resources.File resource = addFileToContext("src/test/resources/project1/src/foobar.js");
    org.sonar.api.resources.File resource2 = addFileToContext("src/test/resources/project1/src/helloworld.js");
    sensor.analyse(project, context);
    verify(context, times(3)).saveMeasure(eq(resource), any(Measure.class));
    verify(context, times(3)).saveMeasure(eq(resource2), any(Measure.class));

    assertThat(loggingEvents.get(0).getMessage()).contains("Parsing").contains("coverage.xml");
    assertThat(loggingEvents.get(1).getMessage()).contains("Parsing").contains("coverage2.xml");
    assertThat(loggingEvents.get(2).getMessage()).contains("Imported coverage data for 2 file");
  }

  @Test
  public void analyse_report_with_unknown_files() throws Exception {
    configureReportPath("coverage_with_2_unknown_files.xml");
    sensor.analyse(project, context);
    assertThat(loggingEvents.get(2).getMessage()).contains("Coverage data ignored for 2 unknown files");
  }

  @Test
  public void analyse_report_with_7_unknown_files() throws Exception {
    configureReportPath("coverage_with_7_unknown_files.xml");
    sensor.analyse(project, context);
    String message = loggingEvents.get(2).getMessage();
    assertThat(message).contains("Coverage data ignored for 7 unknown files");
    assertThat(message).contains("unknown1.js");
    assertThat(Splitter.on("\n").split(message)).hasSize(6);
  }

  @Test
  public void analyse_report_not_found() throws Exception {
    configureReportPath("xxx");
    sensor.analyse(project, context);
    verifyZeroInteractions(context);
    assertThat(loggingEvents.get(1).getLevel()).isEqualTo(Level.WARN);
    assertThat(loggingEvents.get(1).getMessage()).contains("Cannot find");
  }

  @Test(expected = SonarException.class)
  public void analyse_txt_report() throws Exception {
    configureReportPath("not-xml.txt");
    sensor.analyse(project, context);
  }

  @Test(expected = SonarException.class)
  public void analyse_invalid_report() throws Exception {
    configureReportPath("invalid-coverage.xml");
    sensor.analyse(project, context);
  }

  @Test
  public void to_string() throws Exception {
    assertThat(sensor.toString()).isEqualTo("GenericCoverageSensor");
  }

  private void configureReportPath(String reportPath) {
    when(settings.getString(GenericCoveragePlugin.REPORT_PATH_PROPERTY_KEY)).thenReturn(reportPath);
  }

  private org.sonar.api.resources.File addFileToContext(String filePath) {
    org.sonar.api.resources.File sonarFile = org.sonar.api.resources.File.fromIOFile(new File(filePath), project);
    when(context.getResource(sonarFile)).thenReturn(sonarFile);
    return sonarFile;
  }

}
