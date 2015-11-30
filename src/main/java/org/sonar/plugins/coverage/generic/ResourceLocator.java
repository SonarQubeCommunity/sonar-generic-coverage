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

import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.resources.File;
import org.sonar.api.resources.Project;

public class ResourceLocator {

  private final Project project;
  private final FileSystem fs;

  public ResourceLocator(Project project, FileSystem fs) {
    this.project = project;
    this.fs = fs;
  }

  public File getResource(String path) {
    java.io.File javaFile = new java.io.File(path);
    if (!javaFile.isAbsolute()) {
      javaFile = new java.io.File(fs.baseDir(), path);
    }
    return File.fromIOFile(javaFile, project);
  }

}
