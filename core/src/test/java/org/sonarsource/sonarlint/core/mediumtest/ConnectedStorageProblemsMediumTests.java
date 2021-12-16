/*
 * SonarLint Core - Implementation
 * Copyright (C) 2016-2021 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
package org.sonarsource.sonarlint.core.mediumtest;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sonarsource.sonarlint.core.ConnectedSonarLintEngineImpl;
import org.sonarsource.sonarlint.core.analysis.api.ClientInputFile;
import org.sonarsource.sonarlint.core.client.api.common.analysis.Issue;
import org.sonarsource.sonarlint.core.client.api.connected.ConnectedAnalysisConfiguration;
import org.sonarsource.sonarlint.core.client.api.connected.ConnectedGlobalConfiguration;
import org.sonarsource.sonarlint.core.client.api.connected.ConnectedSonarLintEngine;
import org.sonarsource.sonarlint.core.client.api.exceptions.StorageException;
import org.sonarsource.sonarlint.core.commons.Language;
import testutils.TestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.sonarsource.sonarlint.core.mediumtest.fixtures.StorageFixture.newStorage;

class ConnectedStorageProblemsMediumTests {

  private ConnectedSonarLintEngine sonarlint;

  @AfterEach
  public void stop() {
    sonarlint.stop(true);
  }

  @Test
  void test_no_storage(@TempDir Path slHome, @TempDir Path baseDir) {

    ConnectedGlobalConfiguration config = ConnectedGlobalConfiguration.builder()
      .setConnectionId("localhost")
      .setSonarLintUserHome(slHome)
      .setLogOutput((msg, level) -> {
      })
      .build();
    sonarlint = new ConnectedSonarLintEngineImpl(config);

    assertThat(sonarlint.getGlobalStorageStatus()).isNull();
    assertThat(sonarlint.getProjectStorageStatus("foo")).isNull();

    assertThat(sonarlint.allProjectsByKey()).isEmpty();

    IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> sonarlint.getRuleDetails("rule"));
    assertThat(thrown).hasMessage("Unable to find rule description for 'rule'");

    var analysisConfig = ConnectedAnalysisConfiguration.builder()
      .setBaseDir(baseDir)
      .build();

    StorageException thrown2 = assertThrows(StorageException.class, () -> sonarlint.analyze(analysisConfig, i -> {
    }, null, null));
    assertThat(thrown2).hasMessage("Missing storage for connection");
  }

  @Test
  void test_stale_storage(@TempDir Path slHome, @TempDir Path baseDir) {
    String storageId = "localhost";
    newStorage(storageId)
      .stale()
      .create(slHome);

    ConnectedGlobalConfiguration config = ConnectedGlobalConfiguration.builder()
      .setConnectionId(storageId)
      .setSonarLintUserHome(slHome)
      .setLogOutput((msg, level) -> {
      })
      .build();
    sonarlint = new ConnectedSonarLintEngineImpl(config);

    assertThat(sonarlint.getGlobalStorageStatus().isStale()).isTrue();
    assertThat(sonarlint.getProjectStorageStatus("foo")).isNull();

    assertThat(sonarlint.allProjectsByKey()).isEmpty();

    IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> sonarlint.getRuleDetails("rule"));
    assertThat(thrown).hasMessage("Unable to find rule description for 'rule'");

    var analysisConfig = ConnectedAnalysisConfiguration.builder()
      .setBaseDir(baseDir)
      .build();

    StorageException thrown2 = assertThrows(StorageException.class, () -> sonarlint.analyze(analysisConfig, i -> {
    }, null, null));
    assertThat(thrown2).hasMessage("Outdated storage for connection");
  }

  @Test
  void corrupted_plugin_should_not_prevent_startup(@TempDir Path slHome, @TempDir Path baseDir) throws Exception {
    String storageId = "localhost";
    var storage = newStorage(storageId)
      .withJSPlugin()
      .withJavaPlugin()
      .create(slHome);

    Path cachedJSPlugin = storage.getPluginPaths().get(0);
    FileUtils.write(cachedJSPlugin.toFile(), "corrupted jar", StandardCharsets.UTF_8);

    List<String> logs = new CopyOnWriteArrayList<>();

    ConnectedGlobalConfiguration config = ConnectedGlobalConfiguration.builder()
      .setConnectionId(storageId)
      .setSonarLintUserHome(slHome)
      .setStorageRoot(storage.getPath())
      .setLogOutput((m, l) -> logs.add(m))
      .addEnabledLanguage(Language.JAVA)
      .addEnabledLanguage(Language.JS)
      .build();
    sonarlint = new ConnectedSonarLintEngineImpl(config);

    assertThat(logs).contains("Unable to load plugin " + cachedJSPlugin.toString());

    ClientInputFile inputFile = prepareJavaInputFile(baseDir);

    final List<Issue> issues = new ArrayList<>();
    sonarlint.analyze(ConnectedAnalysisConfiguration.builder()
      .setProjectKey(null)
      .setBaseDir(baseDir)
      .addInputFile(inputFile).build(),
      issues::add, null, null);

    assertThat(logs).contains("Execute Sensor: JavaSquidSensor");
  }

  private ClientInputFile prepareJavaInputFile(Path baseDir) throws IOException {
    return prepareInputFile(baseDir, "Foo.java",
      "public class Foo {\n"
        + "  public void foo() {\n"
        + "    int x;\n"
        + "    System.out.println(\"Foo\");\n"
        + "    System.out.println(\"Foo\"); //NOSONAR\n"
        + "  }\n"
        + "}",
      false);
  }

  private ClientInputFile prepareInputFile(Path baseDir, String relativePath, String content, final boolean isTest) throws IOException {
    final File file = new File(baseDir.toFile(), relativePath);
    FileUtils.write(file, content, StandardCharsets.UTF_8);
    ClientInputFile inputFile = TestUtils.createInputFile(file.toPath(), relativePath, isTest);
    return inputFile;
  }

}