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

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.sonar.api.SonarPlugin;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;

public class GenericCoveragePlugin extends SonarPlugin {

  private static final String CATEGORY = "Generic Coverage";
  public static final String OLD_REPORT_PATH_PROPERTY_KEY = "sonar.genericcoverage.reportPath";
  public static final String COVERAGE_REPORT_PATHS_PROPERTY_KEY = "sonar.genericcoverage.reportPaths";
  public static final String IT_COVERAGE_REPORT_PATHS_PROPERTY_KEY = "sonar.genericcoverage.itReportPaths";
  public static final String UNIT_TEST_REPORT_PATHS_PROPERTY_KEY = "sonar.genericcoverage.unitTestReportPaths";
  public static final String OVERALL_COVERAGE_TEST_REPORT_PATHS_PROPERTY_KEY = "sonar.genericcoverage.overallTestReportPaths";

  @Override
  public List getExtensions() {
    ImmutableList.Builder builder = ImmutableList.builder();
    builder.add(GenericCoverageSensor.class);
    builder.addAll(pluginProperties());
    return builder.build();
  }

  private static ImmutableList<PropertyDefinition> pluginProperties() {
    return ImmutableList.of(

      PropertyDefinition.builder(COVERAGE_REPORT_PATHS_PROPERTY_KEY)
        .name("Coverage report paths")
        .description("List of comma-separated paths (absolute or relative) containing coverage report.")
        .category(CATEGORY)
        .onQualifiers(Qualifiers.PROJECT)
        .build(),

      PropertyDefinition.builder(IT_COVERAGE_REPORT_PATHS_PROPERTY_KEY)
        .name("Integration tests coverage report paths")
        .description("List of comma-separated paths (absolute or relative) containing integration tests coverage report.")
        .category(CATEGORY)
        .onQualifiers(Qualifiers.PROJECT)
        .build(),

      PropertyDefinition.builder(OVERALL_COVERAGE_TEST_REPORT_PATHS_PROPERTY_KEY)
        .name("Overall tests coverage report paths")
        .description("List of comma-separated paths (absolute or relative) containing overall tests coverage report.")
        .category(CATEGORY)
        .onQualifiers(Qualifiers.PROJECT)
        .build(),

      PropertyDefinition.builder(UNIT_TEST_REPORT_PATHS_PROPERTY_KEY)
        .name("Unit tests results report paths")
        .description("List of comma-separated paths (absolute or relative) containing unit tests results report.")
        .category(CATEGORY)
        .onQualifiers(Qualifiers.PROJECT)
        .build(),

      deprecatedPropertyDefinition(OLD_REPORT_PATH_PROPERTY_KEY)
      );
  }

  private static PropertyDefinition deprecatedPropertyDefinition(String oldKey) {
    return PropertyDefinition
      .builder(oldKey)
      .name(oldKey)
      .description("This property is deprecated and will be removed in a future version.<br />"
        + "You should stop using it as soon as possible.<br />"
        + "Consult the migration guide for guidance.")
      .category("Generic Coverage")
      .subCategory("Deprecated")
      .onQualifiers(Qualifiers.PROJECT)
      .build();
  }

}
