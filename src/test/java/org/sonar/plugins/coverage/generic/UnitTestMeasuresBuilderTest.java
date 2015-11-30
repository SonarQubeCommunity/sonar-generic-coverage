/*
 * SonarQube Generic Coverage Plugin
 * Copyright (C) 2014 SonarSource
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
package org.sonar.plugins.coverage.generic;

import org.junit.Test;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;

import java.util.Collection;

import static org.fest.assertions.Assertions.assertThat;

public class UnitTestMeasuresBuilderTest {

  @Test
  public void shouldNotCreateIfNoValues() {
    UnitTestMeasuresBuilder builder = UnitTestMeasuresBuilder.create();
    assertThat(builder.createMeasures().size()).isEqualTo(0);
  }

  @Test
  public void shouldCreateMetrics() {
    UnitTestMeasuresBuilder builder = UnitTestMeasuresBuilder.create();
    builder.setTestCase("foo", "ok", 10L, null, null);
    builder.setTestCase("foo", "ok", 10L, null, null);
    builder.setTestCase("foo1", "skipped", 100L, "skipped", "skipped");
    builder.setTestCase("foo2", "error", 200L, "error", "error");
    builder.setTestCase("foo3", "failure", 300L, "failure", "failure");
    assertThat(find(builder.createMeasures(), CoreMetrics.TESTS_KEY).getIntValue()).isEqualTo(4);
    assertThat(find(builder.createMeasures(), CoreMetrics.SKIPPED_TESTS_KEY).getIntValue()).isEqualTo(1);
    assertThat(find(builder.createMeasures(), CoreMetrics.TEST_ERRORS_KEY).getIntValue()).isEqualTo(1);
    assertThat(find(builder.createMeasures(), CoreMetrics.TEST_FAILURES_KEY).getIntValue()).isEqualTo(1);
    assertThat(find(builder.createMeasures(), CoreMetrics.TEST_EXECUTION_TIME_KEY).getIntValue()).isEqualTo(610);
  }

  private Measure find(Collection<Measure> measures, String metricKey) {
    for (Measure measure : measures) {
      if (metricKey.equals(measure.getMetricKey())) {
        return measure;
      }
    }
    return null;
  }
}
