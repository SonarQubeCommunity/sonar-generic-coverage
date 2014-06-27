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
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.ProjectFileSystem;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ResourceLocatorTest {

  @Mock
  private Project project;
  @Mock
  private FileSystem fs;

  private ResourceLocator locator;

  private final File baseDir = new File("src/test/resources/project1").getAbsoluteFile();

  @Before
  public void before() {
    MockitoAnnotations.initMocks(this);
    when(fs.baseDir()).thenReturn(baseDir);
    ProjectFileSystem projectFs = mock(ProjectFileSystem.class);
    when(project.getFileSystem()).thenReturn(projectFs);
    when(projectFs.getSourceDirs()).thenReturn(ImmutableList.of(new File(baseDir, "src")));
    when(projectFs.getBasedir()).thenReturn(baseDir);
    locator = new ResourceLocator(project, fs);
  }

  @Test
  public void relative_path() throws Exception {
    assertThat(locator.getResource("src/resources/MyFile.txt").getKey()).isEqualTo("src/resources/MyFile.txt");
  }

  @Test
  public void absolute_path() throws Exception {
    String path = baseDir.getAbsolutePath() + "/src/resources/MyFile.txt";
    assertThat(locator.getResource(path).getKey()).isEqualTo("src/resources/MyFile.txt");
  }

}
