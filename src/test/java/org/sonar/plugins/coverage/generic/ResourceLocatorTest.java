/*
 * Copyright (C) 2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package org.sonar.plugins.coverage.generic;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.ProjectFileSystem;
import org.sonar.api.scan.filesystem.ModuleFileSystem;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ResourceLocatorTest {

  @Mock
  private Project project;
  @Mock
  private ModuleFileSystem fs;

  private ResourceLocator locator;

  private File baseDir = new File("src/test/resources/project1").getAbsoluteFile();

  @Before
  public void before() {
    MockitoAnnotations.initMocks(this);
    when(fs.baseDir()).thenReturn(baseDir);
    ProjectFileSystem projectFs = mock(ProjectFileSystem.class);
    when(project.getFileSystem()).thenReturn(projectFs);
    when(projectFs.getSourceDirs()).thenReturn(ImmutableList.of(new File(baseDir, "src")));
    locator = new ResourceLocator(project, fs);
  }

  @Test
  public void relative_path() throws Exception {
    assertThat(locator.getResource("src/resources/MyFile.txt").getKey()).isEqualTo("resources/MyFile.txt");
  }

  @Test
  public void absolute_path() throws Exception {
    String path = baseDir.getAbsolutePath() + "/src/resources/MyFile.txt";
    assertThat(locator.getResource(path).getKey()).isEqualTo("resources/MyFile.txt");
  }

}
