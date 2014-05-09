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

import com.google.common.base.Joiner;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Project;
import org.sonar.api.scan.filesystem.ModuleFileSystem;
import org.sonar.api.utils.SonarException;

import javax.xml.stream.XMLStreamException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class GenericCoverageSensor implements Sensor {

  private static final Logger LOG = LoggerFactory.getLogger(GenericCoverageSensor.class);

  private final Settings settings;
  private final ModuleFileSystem fs;

  public GenericCoverageSensor(Settings settings, ModuleFileSystem fs) {
    this.settings = settings;
    this.fs = fs;
  }

  @Override
  public boolean shouldExecuteOnProject(Project project) {
    return StringUtils.isNotEmpty(reportPath());
  }

  private String reportPath() {
    return settings.getString(GenericCoveragePlugin.REPORT_PATH_PROPERTY_KEY);
  }

  @Override
  public void analyse(Project project, SensorContext context) {
    File reportFile = new File(reportPath());
    if (!reportFile.isAbsolute()) {
      reportFile = new File(fs.baseDir(), reportPath());
    }
    String reportAbsolutePath = reportFile.getAbsolutePath();
    LOG.info("Parsing " + reportAbsolutePath);

    InputStream inputStream;
    try {
      inputStream = new FileInputStream(reportFile);
    } catch (FileNotFoundException e) {
      LOG.warn("Cannot find coverage report to parse: " + reportAbsolutePath);
      return;
    }

    ReportParser parser;
    try {
      parser = ReportParser.parse(inputStream, new ResourceLocator(project, fs), context);
    } catch (XMLStreamException e) {
      throw new SonarException("Cannot parse generic coverage report " + reportAbsolutePath, e);
    } catch (ReportParsingException e) {
      throw new SonarException("Error at line " + e.lineNumber() + " of generic coverage report " + reportAbsolutePath, e);
    }

    LOG.info("Imported coverage data for " + parser.numberOfMatchedFiles() + " files");
    int numberOfUnknownFiles = parser.numberOfUnknownFiles();
    if (numberOfUnknownFiles > 0) {
      String fileList = Joiner.on("\n").join(parser.firstUnknownFiles());
      LOG.info("Coverage data ignored for " + numberOfUnknownFiles + " unknown files, including:\n" + fileList);
    }
  }

  @Override
  public String toString() {
    return "GenericCoverageSensor";
  }

}
