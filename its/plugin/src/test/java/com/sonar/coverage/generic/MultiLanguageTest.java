/*
 * SonarQube Generic Coverage plugin :: IT
 * Copyright (C) 2014-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.sonar.coverage.generic;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;

public class MultiLanguageTest {

  private static final String PROJECT = "com.sonar.coverage.generic:it-multi-language";

  @ClassRule
  public static Orchestrator orchestrator = Tests.ORCHESTRATOR;

  @BeforeClass
  public static void inspectProject() {
    SonarScanner sonarRunner = Tests.createSonarScannerBuild()
      .setProjectDir(new File("projects/multi-language"))
      .setProjectKey(PROJECT)
      .setProjectName("SonarSource::GenericCoverage::IT-MultiLanguage")
      .setProjectVersion("1.0-SNAPSHOT")
      .setProperty("sonar.genericcoverage.reportPaths", "report/coverage.xml")
      .setProperty("sonar.genericcoverage.itReportPaths", "report/itcoverage.xml")
      .setProperty("sonar.genericcoverage.overallReportPaths", "report/overallcoverage.xml")
      .setProperty("sonar.genericcoverage.unitTestReportPaths", "report/unittest.xml")
      .setSourceDirs("src/main/java,src/main/js")
      .setTestDirs("src/test/java,src/test/js");
    orchestrator.executeBuild(sonarRunner);
  }

  @Test
  public void file_coverage_measures() throws Exception {
    String javaFileKey = PROJECT + ":src/main/java/FooBar.java";
    assertThat(Tests.getMeasure(javaFileKey, "lines_to_cover").getIntValue()).isEqualTo(2);
    assertThat(Tests.getMeasure(javaFileKey, "uncovered_lines").getIntValue()).isEqualTo(1);
    assertThat(Tests.getMeasure(javaFileKey, "conditions_to_cover").getIntValue()).isEqualTo(4);
    assertThat(Tests.getMeasure(javaFileKey, "uncovered_conditions").getIntValue()).isEqualTo(1);
    String jsFileKey = PROJECT + ":src/main/js/foo.js";
    assertThat(Tests.getMeasure(jsFileKey, "lines_to_cover").getIntValue()).isEqualTo(3);
    assertThat(Tests.getMeasure(jsFileKey, "uncovered_lines").getIntValue()).isEqualTo(1);
    assertThat(Tests.getMeasure(jsFileKey, "conditions_to_cover").getIntValue()).isEqualTo(2);
    assertThat(Tests.getMeasure(jsFileKey, "uncovered_conditions").getIntValue()).isEqualTo(1);
  }

  @Test
  public void file_it_coverage_measures() throws Exception {
    String javaFileKey = PROJECT + ":src/main/java/FooBar.java";
    assertThat(Tests.getMeasure(javaFileKey, "it_lines_to_cover").getIntValue()).isEqualTo(2);
    assertThat(Tests.getMeasure(javaFileKey, "it_uncovered_lines").getIntValue()).isEqualTo(1);
    assertThat(Tests.getMeasure(javaFileKey, "it_conditions_to_cover").getIntValue()).isEqualTo(4);
    assertThat(Tests.getMeasure(javaFileKey, "it_uncovered_conditions").getIntValue()).isEqualTo(1);
    String jsFileKey = PROJECT + ":src/main/js/foo.js";
    assertThat(Tests.getMeasure(jsFileKey, "it_lines_to_cover").getIntValue()).isEqualTo(3);
    assertThat(Tests.getMeasure(jsFileKey, "it_uncovered_lines").getIntValue()).isEqualTo(1);
    assertThat(Tests.getMeasure(jsFileKey, "it_conditions_to_cover").getIntValue()).isEqualTo(2);
    assertThat(Tests.getMeasure(jsFileKey, "it_uncovered_conditions").getIntValue()).isEqualTo(1);
  }

  @Test
  public void file_overall_coverage_measures() throws Exception {
    String javaFileKey = PROJECT + ":src/main/java/FooBar.java";
    assertThat(Tests.getMeasure(javaFileKey, "overall_lines_to_cover").getIntValue()).isEqualTo(2);
    assertThat(Tests.getMeasure(javaFileKey, "overall_uncovered_lines").getIntValue()).isEqualTo(1);
    assertThat(Tests.getMeasure(javaFileKey, "overall_conditions_to_cover").getIntValue()).isEqualTo(4);
    assertThat(Tests.getMeasure(javaFileKey, "overall_uncovered_conditions").getIntValue()).isEqualTo(1);
    String jsFileKey = PROJECT + ":src/main/js/foo.js";
    assertThat(Tests.getMeasure(jsFileKey, "overall_lines_to_cover").getIntValue()).isEqualTo(3);
    assertThat(Tests.getMeasure(jsFileKey, "overall_uncovered_lines").getIntValue()).isEqualTo(1);
    assertThat(Tests.getMeasure(jsFileKey, "overall_conditions_to_cover").getIntValue()).isEqualTo(2);
    assertThat(Tests.getMeasure(jsFileKey, "overall_uncovered_conditions").getIntValue()).isEqualTo(1);
  }

  @Test
  public void file_unittest_measures() throws Exception {
    String javaTestFileKey = PROJECT + ":src/test/java/FooBarTest.java";
    assertThat(Tests.getMeasure(javaTestFileKey, "skipped_tests").getIntValue()).isEqualTo(1);
    assertThat(Tests.getMeasure(javaTestFileKey, "tests").getIntValue()).isEqualTo(3);
    assertThat(Tests.getMeasure(javaTestFileKey, "test_execution_time").getIntValue()).isEqualTo(1350);
    String jsTestFileKey = PROJECT + ":src/test/js/test_foo.js";
    assertThat(Tests.getMeasure(jsTestFileKey, "tests").getIntValue()).isEqualTo(3);
    assertThat(Tests.getMeasure(jsTestFileKey, "test_failures").getIntValue()).isEqualTo(1);
    assertThat(Tests.getMeasure(jsTestFileKey, "test_execution_time").getIntValue()).isEqualTo(1550);
    assertThat(Tests.getMeasure(jsTestFileKey, "test_success_density").getIntValue()).isEqualTo(66);
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
    assertThat(Tests.getMeasure(PROJECT, "it_lines_to_cover").getIntValue()).isEqualTo(5);
    assertThat(Tests.getMeasure(PROJECT, "it_uncovered_lines").getIntValue()).isEqualTo(2);
    assertThat(Tests.getMeasure(PROJECT, "it_conditions_to_cover").getIntValue()).isEqualTo(6);
    assertThat(Tests.getMeasure(PROJECT, "it_uncovered_conditions").getIntValue()).isEqualTo(2);
  }

  @Test
  public void project_overall_coverage_measures() throws Exception {
    assertThat(Tests.getMeasure(PROJECT, "overall_lines_to_cover").getIntValue()).isEqualTo(5);
    assertThat(Tests.getMeasure(PROJECT, "overall_uncovered_lines").getIntValue()).isEqualTo(2);
    assertThat(Tests.getMeasure(PROJECT, "overall_conditions_to_cover").getIntValue()).isEqualTo(6);
    assertThat(Tests.getMeasure(PROJECT, "overall_uncovered_conditions").getIntValue()).isEqualTo(2);
  }

  @Test
  public void project_unittest_measures() throws Exception {
    assertThat(Tests.getMeasure(PROJECT, "tests").getIntValue()).isEqualTo(6);
    assertThat(Tests.getMeasure(PROJECT, "test_errors").getIntValue()).isEqualTo(0);
    assertThat(Tests.getMeasure(PROJECT, "test_failures").getIntValue()).isEqualTo(1);
    assertThat(Tests.getMeasure(PROJECT, "skipped_tests").getIntValue()).isEqualTo(1);
    assertThat(Tests.getMeasure(PROJECT, "test_execution_time").getIntValue()).isEqualTo(2900);
    assertThat(Tests.getMeasure(PROJECT, "test_success_density").getIntValue()).isEqualTo(83);
  }

}
