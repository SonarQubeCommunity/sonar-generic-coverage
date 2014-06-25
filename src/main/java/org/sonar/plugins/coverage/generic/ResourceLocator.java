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

import org.sonar.api.resources.File;
import org.sonar.api.resources.Project;
import org.sonar.api.scan.filesystem.ModuleFileSystem;

public class ResourceLocator {

  private final Project project;
  private final ModuleFileSystem fs;

  public ResourceLocator(Project project, ModuleFileSystem fs) {
    this.project = project;
    this.fs = fs;
  }

  public File getResource(String path) {
    java.io.File javaFile = new java.io.File(path);
    if (!javaFile.isAbsolute()) {
      javaFile = new java.io.File(fs.baseDir(), path);
    }
    org.sonar.api.resources.File sonarFile = File.fromIOFile(javaFile, project);
    if (sonarFile == null) {
      // support SQ<4.2
      sonarFile = File.fromIOFile(javaFile, project.getFileSystem().getTestDirs());
    }
    return sonarFile;
  }

}
