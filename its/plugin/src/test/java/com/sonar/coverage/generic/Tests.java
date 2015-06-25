/*
 * Copyright (C) 2014-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.coverage.generic;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarRunner;
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
    .setMainPluginKey(PLUGIN_KEY)
    .addPlugin(PLUGIN_KEY)
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
