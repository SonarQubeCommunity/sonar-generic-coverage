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

import org.slf4j.Logger;
import org.slf4j.Marker;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.ArrayList;
import java.util.List;

public class StubLogger implements Logger {

  private List<LoggingEvent> loggingEvents = new ArrayList<>();

  class LoggingEvent {
    private String level;
    private String message;

    LoggingEvent(String level, String message) {
      this.level = level;
      this.message = message;
    }

    public String getLevel() {
      return level;
    }

    public String getMessage() {
      return message;
    }
  }

  public List<LoggingEvent> getLoggingEvents() {
    return loggingEvents;
  }

  @Override
  public String getName() {
    return null;
  }

  @Override
  public boolean isTraceEnabled() {
    return false;
  }

  @Override
  public void trace(String s) {
    throw new NotImplementedException();
  }

  @Override
  public void trace(String s, Object o) {
    throw new NotImplementedException();
  }

  @Override
  public void trace(String s, Object o, Object o2) {
    throw new NotImplementedException();
  }

  @Override
  public void trace(String s, Object[] objects) {
    throw new NotImplementedException();
  }

  @Override
  public void trace(String s, Throwable throwable) {
    throw new NotImplementedException();
  }

  @Override
  public boolean isTraceEnabled(Marker marker) {
    return false;
  }

  @Override
  public void trace(Marker marker, String s) {
    throw new NotImplementedException();
  }

  @Override
  public void trace(Marker marker, String s, Object o) {
    throw new NotImplementedException();
  }

  @Override
  public void trace(Marker marker, String s, Object o, Object o2) {
    throw new NotImplementedException();
  }

  @Override
  public void trace(Marker marker, String s, Object[] objects) {
    throw new NotImplementedException();
  }

  @Override
  public void trace(Marker marker, String s, Throwable throwable) {
    throw new NotImplementedException();
  }

  @Override
  public boolean isDebugEnabled() {
    return false;
  }

  @Override
  public void debug(String s) {
    throw new NotImplementedException();
  }

  @Override
  public void debug(String s, Object o) {
    throw new NotImplementedException();
  }

  @Override
  public void debug(String s, Object o, Object o2) {
    throw new NotImplementedException();
  }

  @Override
  public void debug(String s, Object[] objects) {
    throw new NotImplementedException();
  }

  @Override
  public void debug(String s, Throwable throwable) {
    throw new NotImplementedException();
  }

  @Override
  public boolean isDebugEnabled(Marker marker) {
    return false;
  }

  @Override
  public void debug(Marker marker, String s) {
    throw new NotImplementedException();
  }

  @Override
  public void debug(Marker marker, String s, Object o) {
    throw new NotImplementedException();
  }

  @Override
  public void debug(Marker marker, String s, Object o, Object o2) {
    throw new NotImplementedException();
  }

  @Override
  public void debug(Marker marker, String s, Object[] objects) {
    throw new NotImplementedException();
  }

  @Override
  public void debug(Marker marker, String s, Throwable throwable) {
    throw new NotImplementedException();
  }

  @Override
  public boolean isInfoEnabled() {
    return false;
  }

  @Override
  public void info(String s) {
    loggingEvents.add(new LoggingEvent("info", s));
  }

  @Override
  public void info(String s, Object o) {
    throw new NotImplementedException();
  }

  @Override
  public void info(String s, Object o, Object o2) {
    throw new NotImplementedException();
  }

  @Override
  public void info(String s, Object[] objects) {
    throw new NotImplementedException();
  }

  @Override
  public void info(String s, Throwable throwable) {
    throw new NotImplementedException();
  }

  @Override
  public boolean isInfoEnabled(Marker marker) {
    return false;
  }

  @Override
  public void info(Marker marker, String s) {
    throw new NotImplementedException();
  }

  @Override
  public void info(Marker marker, String s, Object o) {
    throw new NotImplementedException();
  }

  @Override
  public void info(Marker marker, String s, Object o, Object o2) {
    throw new NotImplementedException();
  }

  @Override
  public void info(Marker marker, String s, Object[] objects) {
    throw new NotImplementedException();
  }

  @Override
  public void info(Marker marker, String s, Throwable throwable) {
    throw new NotImplementedException();
  }

  @Override
  public boolean isWarnEnabled() {
    return false;
  }

  @Override
  public void warn(String s) {
    loggingEvents.add(new LoggingEvent("warn", s));
  }

  @Override
  public void warn(String s, Object o) {
    throw new NotImplementedException();
  }

  @Override
  public void warn(String s, Object[] objects) {
    throw new NotImplementedException();
  }

  @Override
  public void warn(String s, Object o, Object o2) {
    throw new NotImplementedException();
  }

  @Override
  public void warn(String s, Throwable throwable) {
    throw new NotImplementedException();
  }

  @Override
  public boolean isWarnEnabled(Marker marker) {
    return false;
  }

  @Override
  public void warn(Marker marker, String s) {
    throw new NotImplementedException();
  }

  @Override
  public void warn(Marker marker, String s, Object o) {
    throw new NotImplementedException();
  }

  @Override
  public void warn(Marker marker, String s, Object o, Object o2) {
    throw new NotImplementedException();
  }

  @Override
  public void warn(Marker marker, String s, Object[] objects) {
    throw new NotImplementedException();
  }

  @Override
  public void warn(Marker marker, String s, Throwable throwable) {
    throw new NotImplementedException();
  }

  @Override
  public boolean isErrorEnabled() {
    return false;
  }

  @Override
  public void error(String s) {
    throw new NotImplementedException();
  }

  @Override
  public void error(String s, Object o) {
    throw new NotImplementedException();
  }

  @Override
  public void error(String s, Object o, Object o2) {
    throw new NotImplementedException();
  }

  @Override
  public void error(String s, Object[] objects) {
    throw new NotImplementedException();
  }

  @Override
  public void error(String s, Throwable throwable) {
    throw new NotImplementedException();
  }

  @Override
  public boolean isErrorEnabled(Marker marker) {
    return false;
  }

  @Override
  public void error(Marker marker, String s) {
    throw new NotImplementedException();
  }

  @Override
  public void error(Marker marker, String s, Object o) {
    throw new NotImplementedException();
  }

  @Override
  public void error(Marker marker, String s, Object o, Object o2) {
    throw new NotImplementedException();
  }

  @Override
  public void error(Marker marker, String s, Object[] objects) {
    throw new NotImplementedException();
  }

  @Override
  public void error(Marker marker, String s, Throwable throwable) {
    throw new NotImplementedException();
  }
}
