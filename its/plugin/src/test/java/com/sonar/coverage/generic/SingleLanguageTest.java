/*
 * SonarQube Generic Coverage plugin :: IT
 * Copyright (C) 2014 ${owner}
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
package com.sonar.coverage.generic;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assume.assumeTrue;

public class SingleLanguageTest {

  private static final String PROJECT = "com.sonar.coverage.generic:it-single-language";

  @ClassRule
  public static Orchestrator orchestrator = Tests.ORCHESTRATOR;

  @BeforeClass
  public static void inspectProject() {
    SonarScanner sonarRunner = Tests.createSonarScannerBuild()
      .setProjectDir(new File("projects/single-language"))
      .setProjectKey(PROJECT)
      .setProjectName("SonarSource::GenericCoverage::IT-SingleLanguage")
      .setProjectVersion("1.0-SNAPSHOT")
      .setProperty("sonar.genericcoverage.reportPath" + (Tests.is_after_plugin_1_1() ? "s" : ""), "report/coverage.xml")
      .setProperty("sonar.genericcoverage.itReportPaths", "report/itcoverage.xml")
      .setProperty("sonar.genericcoverage.unitTestReportPaths", "report/unittest.xml")
      .setSourceDirs("src")
      .setTestDirs("test")
      .setLanguage("js");
    orchestrator.executeBuild(sonarRunner);
  }

  @Test
  public void file_coverage_measures() throws Exception {
    String fileKey = PROJECT + ":" + (Tests.is_after_sonar_4_2() ? "src/" : "") + "foo.js";
    assertThat(Tests.getMeasure(fileKey, "lines_to_cover").getIntValue()).isEqualTo(3);
    assertThat(Tests.getMeasure(fileKey, "uncovered_lines").getIntValue()).isEqualTo(1);
    assertThat(Tests.getMeasure(fileKey, "conditions_to_cover").getIntValue()).isEqualTo(2);
    assertThat(Tests.getMeasure(fileKey, "uncovered_conditions").getIntValue()).isEqualTo(1);
  }

  @Test
  public void file_it_coverage_measures() throws Exception {
    assumeTrue(Tests.is_after_plugin_1_1());
    String fileKey = PROJECT + ":" + (Tests.is_after_sonar_4_2() ? "src/" : "") + "foo.js";
    assertThat(Tests.getMeasure(fileKey, "it_lines_to_cover").getIntValue()).isEqualTo(3);
    assertThat(Tests.getMeasure(fileKey, "it_uncovered_lines").getIntValue()).isEqualTo(1);
    assertThat(Tests.getMeasure(fileKey, "it_conditions_to_cover").getIntValue()).isEqualTo(2);
    assertThat(Tests.getMeasure(fileKey, "it_uncovered_conditions").getIntValue()).isEqualTo(1);
  }

  @Test
  public void file_unittest_measures() throws Exception {
    assumeTrue(Tests.is_after_plugin_1_1() && Tests.is_after_sonar_4_2());
    String fileKey = PROJECT + ":" + (Tests.is_after_sonar_4_2() ? "test/" : "") + "test_foo.js";
    assertThat(Tests.getMeasure(fileKey, "tests").getIntValue()).isEqualTo(3);
    assertThat(Tests.getMeasure(fileKey, "test_errors").getIntValue()).isEqualTo(1);
    assertThat(Tests.getMeasure(fileKey, "test_execution_time").getIntValue()).isEqualTo(1650);
    assertThat(Tests.getMeasure(fileKey, "test_success_density").getIntValue()).isEqualTo(66);
  }

  @Test
  public void project_coverage_measures() throws Exception {
    assertThat(Tests.getMeasure(PROJECT, "lines_to_cover").getIntValue()).isEqualTo(5);
    assertThat(Tests.getMeasure(PROJECT, "uncovered_lines").getIntValue()).isEqualTo(2);
    assertThat(Tests.getMeasure(PROJECT, "conditions_to_cover").getIntValue()).isEqualTo(6);
    assertThat(Tests.getMeasure(PROJECT, "uncovered_conditions").getIntValue()).isEqualTo(2);
  }

  @Test
  public void project_it_coverage_measures() throws Exception {
    assumeTrue(Tests.is_after_plugin_1_1());
    assertThat(Tests.getMeasure(PROJECT, "it_lines_to_cover").getIntValue()).isEqualTo(5);
    assertThat(Tests.getMeasure(PROJECT, "it_uncovered_lines").getIntValue()).isEqualTo(2);
    assertThat(Tests.getMeasure(PROJECT, "it_conditions_to_cover").getIntValue()).isEqualTo(6);
    assertThat(Tests.getMeasure(PROJECT, "it_uncovered_conditions").getIntValue()).isEqualTo(2);
  }

  @Test
  public void project_unittest_measures() throws Exception {
    assumeTrue(Tests.is_after_plugin_1_1() && Tests.is_after_sonar_4_2());
    assertThat(Tests.getMeasure(PROJECT, "tests").getIntValue()).isEqualTo(5);
    assertThat(Tests.getMeasure(PROJECT, "test_errors").getIntValue()).isEqualTo(1);
    assertThat(Tests.getMeasure(PROJECT, "test_failures").getIntValue()).isEqualTo(0);
    assertThat(Tests.getMeasure(PROJECT, "skipped_tests").getIntValue()).isEqualTo(0);
    assertThat(Tests.getMeasure(PROJECT, "test_execution_time").getIntValue()).isEqualTo(2500);
    assertThat(Tests.getMeasure(PROJECT, "test_success_density").getIntValue()).isEqualTo(80);
  }

}
