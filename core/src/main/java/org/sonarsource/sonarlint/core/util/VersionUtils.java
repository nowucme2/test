/*
 * SonarLint Core - Implementation
 * Copyright (C) 2009-2016 SonarSource SA
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
package org.sonarsource.sonarlint.core.util;

import javax.annotation.CheckForNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VersionUtils {
  private static final String VERSION_REGEX = ".*?(\\d+\\.\\d+(?:\\.\\d+)*).*";
  private static final Pattern JAR_VERSION_PATTERN = Pattern.compile(VERSION_REGEX);
  
  private VersionUtils() {
  }

  public static String getLibraryVersion() {
    String version = "unknown";
    Package packageInfo = VersionUtils.class.getPackage();
    if (packageInfo != null && packageInfo.getImplementationVersion() != null) {
      version = packageInfo.getImplementationVersion();
    }
    return version;
  }
  
  @CheckForNull
  public static String getJarVersion(String jarName) {
    Matcher matcher = JAR_VERSION_PATTERN.matcher(jarName);
    if(matcher.find()) {
      return matcher.group(1);
    }
    
    return null;
  }
}
