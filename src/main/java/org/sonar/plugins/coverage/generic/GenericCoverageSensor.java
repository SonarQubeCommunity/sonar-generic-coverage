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
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Project;
import org.sonar.api.scan.filesystem.ModuleFileSystem;
import org.sonar.api.utils.SonarException;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.util.List;

public class GenericCoverageSensor implements Sensor {

  private static final Logger LOG = LoggerFactory.getLogger(GenericCoverageSensor.class);

  private final Settings settings;
  private final ModuleFileSystem fs;
  private final ResourcePerspectives perspectives;

  public GenericCoverageSensor(Settings settings, ModuleFileSystem fs, ResourcePerspectives perspectives) {
    this.settings = settings;
    this.fs = fs;
    this.perspectives = perspectives;
  }

  @Override
  public boolean shouldExecuteOnProject(Project project) {
    return StringUtils.isNotEmpty(reportPath()) || StringUtils.isNotEmpty(itReportPath()) || StringUtils.isNotEmpty(unitTestReportPath());
  }

  private String reportPath() {
    return settings.getString(GenericCoveragePlugin.REPORT_PATH_PROPERTY_KEY);
  }

  private String itReportPath() {
    return settings.getString(GenericCoveragePlugin.IT_REPORT_PATH_PROPERTY_KEY);
  }

  private String unitTestReportPath() {
    return settings.getString(GenericCoveragePlugin.UNIT_TEST_REPORT_PATH_PROPERTY_KEY);
  }

  private List<String> getList(String string) {
    return string == null ? ImmutableList.<String>of() : Lists.newArrayList(Splitter.on(",").trimResults().omitEmptyStrings().split(string));
  }

  @Override
  public void analyse(Project project, SensorContext context) {
    boolean ok = loadReport(project, context, ReportParser.Mode.COVERAGE, reportPath());
    if (ok) {
      ok = loadReport(project, context, ReportParser.Mode.IT_COVERAGE, itReportPath());
    }
    if (ok) {
      loadReport(project, context, ReportParser.Mode.UNITTEST, unitTestReportPath());
    }
  }

  public boolean loadReport(Project project, SensorContext context, ReportParser.Mode mode, String reportPath) {
    String modeString = getModeString(mode);
    ReportParser parser = new ReportParser(new ResourceLocator(project, fs), context, perspectives, mode);
    List<String> strings = getList(reportPath);
    while (!strings.isEmpty()) {
      String path = strings.remove(0);
      File reportFile = new File(path);
      if (!reportFile.isAbsolute()) {
        reportFile = new File(fs.baseDir(), path);
      }
      String reportAbsolutePath = reportFile.getAbsolutePath();
      LOG.info("Parsing " + reportAbsolutePath);

      if (!reportFile.exists()) {
        LOG.warn("Cannot find " + modeString + " report to parse: " + reportAbsolutePath);
        return false;
      }

      try {
        parser.parse(reportFile);
      } catch (XMLStreamException e) {
        throw new SonarException("Cannot parse " + modeString + " report " + reportAbsolutePath, e);
      } catch (ReportParsingException e) {
        throw new SonarException("Error at line " + e.lineNumber() + " of " + modeString + " report " + reportAbsolutePath, e);
      }
    }
    parser.saveMeasures();

    LOG.info("Imported " + modeString + " data for " + parser.numberOfMatchedFiles() + " files");
    int numberOfUnknownFiles = parser.numberOfUnknownFiles();
    if (numberOfUnknownFiles > 0) {
      String fileList = Joiner.on("\n").join(parser.firstUnknownFiles());
      LOG.info(modeString + " data ignored for " + numberOfUnknownFiles + " unknown files, including:\n" + fileList);
    }
    return true;
  }

  private String getModeString(ReportParser.Mode mode) {
    if (ReportParser.Mode.COVERAGE == mode) {
      return "coverage";
    } else if (ReportParser.Mode.IT_COVERAGE == mode) {
      return "IT coverage";
    } else {
      return "unit test";
    }
  }

  @Override
  public String toString() {
    return "GenericCoverageSensor";
  }

}
