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
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.locator.FileLocation;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.sonar.wsclient.Sonar;
import org.sonar.wsclient.services.Measure;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

@RunWith(Suite.class)
@SuiteClasses({
  SingleLanguageTest.class,
  MultiLanguageTest.class
})
public class Tests {

  private static final String PLUGIN_KEY = "genericcoverage";

  @ClassRule
  public static final Orchestrator ORCHESTRATOR = Orchestrator.builderEnv()
    .addPlugin(FileLocation.of("../../target/sonar-generic-coverage-plugin.jar"))
    .addPlugin("java")
    .addPlugin("javascript")
    .build();

  public static boolean is_after_sonar_4_2() {
    return ORCHESTRATOR.getConfiguration().getSonarVersion().isGreaterThanOrEquals("4.2");
  }

  public static boolean is_after_plugin_1_1() {
    return is_after_plugin("1.1");
  }

  public static boolean is_after_plugin(String version) {
    return ORCHESTRATOR.getConfiguration().getPluginVersion(PLUGIN_KEY).isGreaterThanOrEquals(version);
  }

  public static SonarRunner createSonarRunnerBuild() {
    return SonarRunner.create();
  }

  public static Measure getMeasure(String resourceKey, String metricKey) {
    Sonar wsClient = ORCHESTRATOR.getServer().getWsClient();
    Resource resource = wsClient.find(ResourceQuery.createForMetrics(resourceKey, metricKey));
    return resource != null ? resource.getMeasure(metricKey) : null;
  }

}
