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

public final class TestCase {

  public static final String OK = "ok";
  public static final String ERROR = "error";
  public static final String FAILURE = "failure";
  public static final String SKIPPED = "skipped";

  private String name;
  private String status;
  private String stackTrace;
  private String message;
  private long duration = 0L;

  public String getName() {
    return name;
  }

  public TestCase setName(String name) {
    this.name = name;
    return this;
  }

  public String getStatus() {
    return status;
  }

  public TestCase setStatus(String status) {
    this.status = status;
    return this;
  }

  public String getStackTrace() {
    return stackTrace;
  }

  public TestCase setStackTrace(String stackTrace) {
    this.stackTrace = stackTrace;
    return this;
  }

  public String getMessage() {
    return message;
  }

  public TestCase setMessage(String message) {
    this.message = message;
    return this;
  }

  public long getDuration() {
    return duration;
  }

  public TestCase setDuration(long duration) {
    this.duration = duration;
    return this;
  }

}
