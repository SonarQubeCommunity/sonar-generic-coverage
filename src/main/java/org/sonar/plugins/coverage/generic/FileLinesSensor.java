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

import com.google.common.base.Strings;
import com.google.common.io.Files;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.FileLinesContext;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonar.api.resources.Project;
import org.sonar.api.utils.SonarException;

import java.io.IOException;
import java.util.List;

public class FileLinesSensor implements Sensor {

 private final FileLinesContextFactory fileLinesContextFactory;
 private final FileSystem fs;

 public FileLinesSensor(FileLinesContextFactory fileLinesContextFactory, FileSystem fileSystem) {
   this.fileLinesContextFactory = fileLinesContextFactory;
   this.fs = fileSystem;
 }

 @Override
 public boolean shouldExecuteOnProject(Project project) {
   return fs.hasFiles(fs.predicates().hasLanguage(OtherLanguage.KEY));
 }

 @Override
 public void analyse(Project module, SensorContext context) {
   for (InputFile inputFile : fs.inputFiles(fs.predicates().hasLanguage(OtherLanguage.KEY))) {
     FileLinesContext fileLinesContext = fileLinesContextFactory.createFor(inputFile);
     List<String> lines;
     try {
       lines = Files.readLines(inputFile.file(), fs.encoding());
     } catch (IOException e) {
       throw new SonarException(e);
     }
     double total = 0;
     for (int line = 0; line < lines.size(); line++) {
       int ncloc = Strings.isNullOrEmpty(lines.get(line)) ? 0 : 1;
       fileLinesContext.setIntValue(CoreMetrics.NCLOC_DATA_KEY, line + 1, ncloc);
       total += ncloc;
     }
     fileLinesContext.save();
     context.saveMeasure(inputFile, CoreMetrics.NCLOC, total);
   }
 }
}
