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
import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.api.SonarPlugin;

import java.util.List;

@Properties({
  @Property(
    key = GenericCoveragePlugin.REPORT_PATH_PROPERTY_KEY,
    category = "Generic Coverage",
    name = "Coverage report path",
    description = "List of comma-separated paths (absolute or relative) containing coverage report.",
    project = true, global = false),
  @Property(
    key = GenericCoveragePlugin.IT_REPORT_PATH_PROPERTY_KEY,
    category = "Generic Coverage",
    name = "Integration tests coverage report path",
    description = "List of comma-separated paths (absolute or relative) containing integration tests coverage report.",
    project = true, global = false)
})
public class GenericCoveragePlugin extends SonarPlugin {

  public static final String REPORT_PATH_PROPERTY_KEY = "sonar.genericcoverage.reportPath";
  public static final String IT_REPORT_PATH_PROPERTY_KEY = "sonar.genericcoverage.itReportPath";

  @Override
  public List getExtensions() {
    return ImmutableList.of(
      GenericCoverageSensor.class);
  }

}
