/*
 * SonarQube Generic Coverage Plugin
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
package org.sonar.plugins.coverage.generic;

import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.utils.ParsingUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class UnitTestMeasuresBuilder {

  private final Map<String, TestCase> index = new HashMap<>();
  private int test = 0;
  private int failure = 0;
  private int error = 0;
  private int skipped = 0;
  private long duration = 0L;

  private UnitTestMeasuresBuilder() {
    // Use the factory
  }

  public boolean setTestCase(String name, String status, long duration, String message, String stacktrace) {
    if (index.containsKey(name)) {
      return false;
    } else {
      TestCase testCase = new TestCase();
      testCase
        .setName(name)
        .setStatus(status)
        .setDuration(duration)
        .setMessage(message)
        .setStackTrace(stacktrace);
      index.put(name, testCase);

      setCounter(status);
      this.duration += duration;
      return true;
    }
  }

  private void setCounter(String status) {
    if (TestCase.ERROR.equals(status)) {
      error++;
    } else if (TestCase.FAILURE.equals(status)) {
      failure++;
    } else if (TestCase.SKIPPED.equals(status)) {
      skipped++;
    }
    test++;
  }

  public static UnitTestMeasuresBuilder create() {
    return new UnitTestMeasuresBuilder();
  }

  public java.util.Collection<Measure> createMeasures() {
    Collection<Measure> measures = new ArrayList<>();
    if (test > 0) {
      measures.add(new Measure(CoreMetrics.SKIPPED_TESTS, (double) skipped));
      measures.add(new Measure(CoreMetrics.TESTS, (double) test));
      measures.add(new Measure(CoreMetrics.TEST_ERRORS, (double) error));
      measures.add(new Measure(CoreMetrics.TEST_FAILURES, (double) failure));
      measures.add(new Measure(CoreMetrics.TEST_EXECUTION_TIME, (double) duration));
      double passedTests = (double) test - error - failure;
      double percentage = passedTests * 100d / test;
      measures.add(new Measure(CoreMetrics.TEST_SUCCESS_DENSITY, ParsingUtils.scaleValue(percentage)));
    }
    return measures;
  }

  public Collection<TestCase> getTestCases() {
    return Collections.unmodifiableCollection(index.values());
  }
}
