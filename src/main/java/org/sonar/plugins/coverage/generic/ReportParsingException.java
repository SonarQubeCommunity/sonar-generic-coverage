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

import org.codehaus.staxmate.in.SMInputCursor;

import javax.xml.stream.XMLStreamException;

public class ReportParsingException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final int lineNumber;

  public ReportParsingException(String message, int lineNumber) {
    super(message);
    this.lineNumber = lineNumber;
  }

  public ReportParsingException(String message, SMInputCursor cursor) throws XMLStreamException {
    this(message, cursor.getCursorLocation().getLineNumber());
  }

  public ReportParsingException(String message, Throwable cause, int lineNumber) {
    super(message, cause);
    this.lineNumber = lineNumber;
  }

  public int lineNumber() {
    return lineNumber;
  }

}
