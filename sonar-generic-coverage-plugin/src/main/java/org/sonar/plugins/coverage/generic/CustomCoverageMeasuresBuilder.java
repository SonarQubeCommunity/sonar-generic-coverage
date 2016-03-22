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

import com.google.common.collect.ImmutableMap;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.PersistenceMode;
import org.sonar.api.utils.KeyValueFormat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public final class CustomCoverageMeasuresBuilder {

  private enum METRIC {
    LINES_TO_COVER, UNCOVERED_LINES, COVERAGE_LINE_HITS_DATA, CONDITIONS_TO_COVER, UNCOVERED_CONDITIONS, COVERED_CONDITIONS_BY_LINE, CONDITIONS_BY_LINE
  }

  private static final Map<METRIC, Metric> DEFAULT_KEYS = ImmutableMap.<METRIC, Metric>builder()
    .put(METRIC.LINES_TO_COVER, CoreMetrics.LINES_TO_COVER)
    .put(METRIC.UNCOVERED_LINES, CoreMetrics.UNCOVERED_LINES)
    .put(METRIC.COVERAGE_LINE_HITS_DATA, CoreMetrics.COVERAGE_LINE_HITS_DATA)
    .put(METRIC.CONDITIONS_TO_COVER, CoreMetrics.CONDITIONS_TO_COVER)
    .put(METRIC.UNCOVERED_CONDITIONS, CoreMetrics.UNCOVERED_CONDITIONS)
    .put(METRIC.COVERED_CONDITIONS_BY_LINE, CoreMetrics.COVERED_CONDITIONS_BY_LINE)
    .put(METRIC.CONDITIONS_BY_LINE, CoreMetrics.CONDITIONS_BY_LINE).build();

  private static final Map<METRIC, Metric> IT_KEYS = ImmutableMap.<METRIC, Metric>builder()
    .put(METRIC.LINES_TO_COVER, CoreMetrics.IT_LINES_TO_COVER)
    .put(METRIC.UNCOVERED_LINES, CoreMetrics.IT_UNCOVERED_LINES)
    .put(METRIC.COVERAGE_LINE_HITS_DATA, CoreMetrics.IT_COVERAGE_LINE_HITS_DATA)
    .put(METRIC.CONDITIONS_TO_COVER, CoreMetrics.IT_CONDITIONS_TO_COVER)
    .put(METRIC.UNCOVERED_CONDITIONS, CoreMetrics.IT_UNCOVERED_CONDITIONS)
    .put(METRIC.COVERED_CONDITIONS_BY_LINE, CoreMetrics.IT_COVERED_CONDITIONS_BY_LINE)
    .put(METRIC.CONDITIONS_BY_LINE, CoreMetrics.IT_CONDITIONS_BY_LINE).build();

  private static final Map<METRIC, Metric> OVERALL_KEYS = ImmutableMap.<METRIC, Metric>builder()
    .put(METRIC.LINES_TO_COVER, CoreMetrics.OVERALL_LINES_TO_COVER)
    .put(METRIC.UNCOVERED_LINES, CoreMetrics.OVERALL_UNCOVERED_LINES)
    .put(METRIC.COVERAGE_LINE_HITS_DATA, CoreMetrics.OVERALL_COVERAGE_LINE_HITS_DATA)
    .put(METRIC.CONDITIONS_TO_COVER, CoreMetrics.OVERALL_CONDITIONS_TO_COVER)
    .put(METRIC.UNCOVERED_CONDITIONS, CoreMetrics.OVERALL_UNCOVERED_CONDITIONS)
    .put(METRIC.COVERED_CONDITIONS_BY_LINE, CoreMetrics.OVERALL_COVERED_CONDITIONS_BY_LINE)
    .put(METRIC.CONDITIONS_BY_LINE, CoreMetrics.OVERALL_CONDITIONS_BY_LINE).build();

  private int totalCoveredLines = 0;
  private int totalConditions = 0;
  private int totalCoveredConditions = 0;
  private final SortedMap<Integer, Integer> hitsByLine = new TreeMap<>();
  private final SortedMap<Integer, Integer> conditionsByLine = new TreeMap<>();
  private final SortedMap<Integer, Integer> coveredConditionsByLine = new TreeMap<>();
  private Map<METRIC, Metric> metrics = DEFAULT_KEYS;

  private CustomCoverageMeasuresBuilder() {
    // use the factory
  }

  public CustomCoverageMeasuresBuilder setHits(int lineId, int hits) {
    if (hitsByLine.containsKey(lineId)) {
      int oldValue = hitsByLine.get(lineId);
      hitsByLine.put(lineId, Math.max(oldValue, hits));
      if (oldValue == 0 && hits > 0) {
        totalCoveredLines += 1;
      }
    } else {
      hitsByLine.put(lineId, hits);
      if (hits > 0) {
        totalCoveredLines += 1;
      }
    }
    return this;
  }

  public CustomCoverageMeasuresBuilder setConditions(int lineId, int conditions, int coveredConditions) {
    if (conditions > 0) {
      int coveredNewValue;
      int totalCoveredDiff;
      if (conditionsByLine.containsKey(lineId)) {
        if (conditions != conditionsByLine.get(lineId)) {
          return null;
        }
        int oldValue = coveredConditionsByLine.get(lineId);
        coveredNewValue = Math.max(oldValue, coveredConditions);
        totalCoveredDiff = Math.abs(oldValue - coveredNewValue);
      } else {
        totalConditions += conditions;
        totalCoveredDiff = coveredConditions;
        conditionsByLine.put(lineId, conditions);
        coveredNewValue = coveredConditions;
      }
      coveredConditionsByLine.put(lineId, coveredNewValue);
      totalCoveredConditions += totalCoveredDiff;
    }
    return this;
  }

  public int getCoveredConditions() {
    return totalCoveredConditions;
  }

  public int getConditions() {
    return totalConditions;
  }

  public int getLinesToCover() {
    return hitsByLine.size();
  }

  public int getCoveredLines() {
    return totalCoveredLines;
  }

  public SortedMap<Integer, Integer> getCoveredConditionsByLine() {
    return Collections.unmodifiableSortedMap(coveredConditionsByLine);
  }

  public SortedMap<Integer, Integer> getConditionsByLine() {
    return Collections.unmodifiableSortedMap(conditionsByLine);
  }

  public SortedMap<Integer, Integer> getHitsByLine() {
    return Collections.unmodifiableSortedMap(hitsByLine);
  }

  public Collection<Measure> createMeasures() {
    Collection<Measure> measures = new ArrayList<>();
    if (getLinesToCover() > 0) {
      measures.add(new Measure(metrics.get(METRIC.LINES_TO_COVER), (double) getLinesToCover()));
      measures.add(new Measure(metrics.get(METRIC.UNCOVERED_LINES), (double) (getLinesToCover() - getCoveredLines())));
      measures.add(new Measure(metrics.get(METRIC.COVERAGE_LINE_HITS_DATA)).setData(KeyValueFormat.format(hitsByLine)).setPersistenceMode(PersistenceMode.DATABASE));
    }
    if (getConditions() > 0) {
      measures.add(new Measure(metrics.get(METRIC.CONDITIONS_TO_COVER), (double) getConditions()));
      measures.add(new Measure(metrics.get(METRIC.UNCOVERED_CONDITIONS), (double) (getConditions() - getCoveredConditions())));
      measures.add(createMeasureByLine(conditionsByLine, METRIC.CONDITIONS_BY_LINE));
      measures.add(createMeasureByLine(coveredConditionsByLine, METRIC.COVERED_CONDITIONS_BY_LINE));
    }
    return measures;
  }

  private Measure createMeasureByLine(SortedMap<Integer, Integer> lines, METRIC metric) {
    return new Measure(metrics.get(metric))
      .setData(KeyValueFormat.format(lines))
      .setPersistenceMode(PersistenceMode.DATABASE);
  }

  public static CustomCoverageMeasuresBuilder create() {
    return new CustomCoverageMeasuresBuilder();
  }

  public CustomCoverageMeasuresBuilder enableITMode() {
    metrics = IT_KEYS;
    return this;
  }

  public CustomCoverageMeasuresBuilder enableOverallMode() {
    metrics = OVERALL_KEYS;
    return this;
  }

}
