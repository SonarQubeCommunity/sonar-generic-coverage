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

public class CustomCoverageMeasuresBuilderTest {

  @Test
  public void shouldNotCreateIfNoValues() {
    CustomCoverageMeasuresBuilder builder = CustomCoverageMeasuresBuilder.create();
    assertThat(builder.createMeasures().size()).isEqualTo(0);
  }

  @Test
  public void shouldCreateHitsByLineData() {
    CustomCoverageMeasuresBuilder builder = CustomCoverageMeasuresBuilder.create();
    builder.setHits(1, 0);
    builder.setHits(1, 0); // equal set
    builder.setHits(2, 3);
    builder.setHits(2, 0); // ignore
    builder.setHits(4, 2);
    assertThat(find(builder.createMeasures(), CoreMetrics.COVERAGE_LINE_HITS_DATA_KEY).getData()).isEqualTo("1=0;2=3;4=2");
    assertThat(builder.getCoveredLines()).isEqualTo(2);
  }

  @Test
  public void shouldCreateUncoveredLines() {
    CustomCoverageMeasuresBuilder builder = CustomCoverageMeasuresBuilder.create();
    builder.setHits(1, 0);
    builder.setHits(2, 3);
    builder.setHits(3, 0);
    assertThat(find(builder.createMeasures(), CoreMetrics.UNCOVERED_LINES_KEY).getIntValue()).isEqualTo(2);
    assertThat(builder.getCoveredLines()).isEqualTo(1);
  }

  @Test
  public void shouldCreateITLinesMetrics() {
    CustomCoverageMeasuresBuilder builder = CustomCoverageMeasuresBuilder.create().enableITMode();
    builder.setHits(1, 0);
    builder.setHits(2, 3);
    builder.setHits(3, 0);
    assertThat(find(builder.createMeasures(), CoreMetrics.IT_COVERAGE_LINE_HITS_DATA_KEY).getData()).isEqualTo("1=0;2=3;3=0");
    assertThat(find(builder.createMeasures(), CoreMetrics.IT_UNCOVERED_LINES_KEY).getIntValue()).isEqualTo(2);
    assertThat(find(builder.createMeasures(), CoreMetrics.IT_LINES_TO_COVER_KEY).getIntValue()).isEqualTo(3);
    assertThat(builder.getCoveredLines()).isEqualTo(1);
  }

  @Test
  public void shouldCreateConditionsByLineData() {
    CustomCoverageMeasuresBuilder builder = CustomCoverageMeasuresBuilder.create();
    builder.setConditions(1, 2, 2);
    builder.setConditions(2, 1, 0);
    assertThat(find(builder.createMeasures(), CoreMetrics.CONDITIONS_BY_LINE_KEY).getData()).isEqualTo("1=2;2=1");
    assertThat(find(builder.createMeasures(), CoreMetrics.COVERED_CONDITIONS_BY_LINE_KEY).getData()).isEqualTo("1=2;2=0");
  }

  @Test
  public void shouldCreateNumberOfConditionsToCover() {
    CustomCoverageMeasuresBuilder builder = CustomCoverageMeasuresBuilder.create();
    builder.setConditions(1, 2, 2);
    builder.setConditions(2, 1, 0);
    assertThat(find(builder.createMeasures(), CoreMetrics.CONDITIONS_TO_COVER_KEY).getIntValue()).isEqualTo(3);
  }

  @Test
  public void shouldCreateNumberOfUncoveredConditions() {
    CustomCoverageMeasuresBuilder builder = CustomCoverageMeasuresBuilder.create();
    builder.setConditions(1, 2, 2);
    builder.setConditions(2, 1, 0);
    builder.setConditions(3, 3, 1);
    assertThat(find(builder.createMeasures(), CoreMetrics.UNCOVERED_CONDITIONS_KEY).getIntValue()).isEqualTo(3);
  }

  @Test
  public void shouldSetOnlyPositiveConditions() {
    CustomCoverageMeasuresBuilder builder = CustomCoverageMeasuresBuilder.create();
    builder.setConditions(1, 0, 0);
    builder.setConditions(2, 1, 0);
    assertThat(find(builder.createMeasures(), CoreMetrics.CONDITIONS_BY_LINE_KEY).getData()).isEqualTo("2=1");
    assertThat(find(builder.createMeasures(), CoreMetrics.COVERED_CONDITIONS_BY_LINE_KEY).getData()).isEqualTo("2=0");
  }

  @Test
  public void shouldCreateNumberITMetrics() {
    CustomCoverageMeasuresBuilder builder = CustomCoverageMeasuresBuilder.create().enableITMode();
    builder.setConditions(1, 2, 2);
    builder.setConditions(2, 1, 0);
    builder.setConditions(3, 3, 1);
    assertThat(find(builder.createMeasures(), CoreMetrics.IT_CONDITIONS_BY_LINE_KEY).getData()).isEqualTo("1=2;2=1;3=3");
    assertThat(find(builder.createMeasures(), CoreMetrics.IT_COVERED_CONDITIONS_BY_LINE_KEY).getData()).isEqualTo("1=2;2=0;3=1");
    assertThat(find(builder.createMeasures(), CoreMetrics.IT_CONDITIONS_TO_COVER_KEY).getIntValue()).isEqualTo(6);
    assertThat(find(builder.createMeasures(), CoreMetrics.IT_UNCOVERED_CONDITIONS_KEY).getIntValue()).isEqualTo(3);
  }

  @Test
  public void shouldMergeDuplicatedSetHits() {
    CustomCoverageMeasuresBuilder builder = CustomCoverageMeasuresBuilder.create();
    builder.setHits(2, 3);
    builder.setHits(2, 5); // to merge
    assertThat(builder.getLinesToCover()).isEqualTo(1);
    assertThat(builder.getCoveredLines()).isEqualTo(1);
    assertThat(builder.getHitsByLine().get(2)).isEqualTo(5);
    builder.setHits(3, 0);
    assertThat(builder.getLinesToCover()).isEqualTo(2);
    assertThat(builder.getCoveredLines()).isEqualTo(1);
    assertThat(builder.getHitsByLine().get(3)).isEqualTo(0);
    builder.setHits(3, 1);
    assertThat(builder.getLinesToCover()).isEqualTo(2);
    assertThat(builder.getCoveredLines()).isEqualTo(2);
    assertThat(builder.getHitsByLine().get(3)).isEqualTo(1);

  }

  @Test
  public void shouldReturnNulBadSetConditions() {
    CustomCoverageMeasuresBuilder builder = CustomCoverageMeasuresBuilder.create();
    builder.setConditions(1, 3, 2);
    assertThat(builder.setConditions(1, 1, 2)).isEqualTo(null); // should return null
  }

  @Test
  public void shouldMergeMultipleSetConditions() {
    CustomCoverageMeasuresBuilder builder = CustomCoverageMeasuresBuilder.create();
    builder.setConditions(1, 3, 1);
    builder.setConditions(1, 3, 2);
    assertThat(builder.getConditions()).isEqualTo(3);
    assertThat(builder.getCoveredConditions()).isEqualTo(2);
    assertThat(builder.getConditionsByLine().get(1)).isEqualTo(3);
    assertThat(builder.getCoveredConditionsByLine().get(1)).isEqualTo(2);
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
