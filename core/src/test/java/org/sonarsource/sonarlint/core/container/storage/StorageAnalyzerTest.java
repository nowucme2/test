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
package org.sonarsource.sonarlint.core.container.storage;

import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sonarsource.sonarlint.core.client.api.common.analysis.IssueListener;
import org.sonarsource.sonarlint.core.client.api.connected.ConnectedAnalysisConfiguration;
import org.sonarsource.sonarlint.core.client.api.connected.GlobalStorageStatus;
import org.sonarsource.sonarlint.core.client.api.connected.ProjectStorageStatus;
import org.sonarsource.sonarlint.core.client.api.exceptions.StorageException;
import org.sonarsource.sonarlint.core.proto.Sonarlint;
import org.sonarsource.sonarlint.core.util.ProgressWrapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StorageAnalyzerTest {
  @Mock
  private GlobalUpdateStatusReader globalReader;
  @Mock
  private ProjectStorageStatusReader moduleReader;
  @Mock
  private ConnectedAnalysisConfiguration config;
  @TempDir
  Path tempDir;

  private GlobalSettingsStore globalSettingsStore;
  private StorageAnalyzer analyzer;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    when(config.projectKey()).thenReturn("module1");
    globalSettingsStore = new GlobalSettingsStore(new StorageFolder.Default(tempDir));
    globalSettingsStore.store(Sonarlint.GlobalProperties.newBuilder().build());
    analyzer = new StorageAnalyzer(globalReader, moduleReader);
  }

  @Test
  void testNoGlobalStorage() {
    when(globalReader.read()).thenReturn(null);

    Throwable exception = catchThrowable(() -> analyzer.analyze(mock(StorageContainer.class), config, mock(IssueListener.class), globalSettingsStore, new ProgressWrapper(null)));

    assertThat(exception)
      .isInstanceOf(StorageException.class)
      .hasMessage("Missing global data. Please update server.");
  }

  @Test
  void testNoModuleStorage() {
    when(globalReader.read()).thenReturn(mock(GlobalStorageStatus.class));
    when(moduleReader.apply("module1")).thenReturn(null);

    Throwable exception = catchThrowable(() -> analyzer.analyze(mock(StorageContainer.class), config, mock(IssueListener.class), globalSettingsStore, new ProgressWrapper(null)));

    assertThat(exception)
      .isInstanceOf(StorageException.class)
      .hasMessage("No data stored for project 'module1'. Please update the binding.");
  }

  @Test
  void testStaleModuleStorage() {
    when(globalReader.read()).thenReturn(mock(GlobalStorageStatus.class));
    ProjectStorageStatus moduleStatus = mock(ProjectStorageStatus.class);
    when(moduleStatus.isStale()).thenReturn(true);
    when(moduleReader.apply("module1")).thenReturn(moduleStatus);

    Throwable exception = catchThrowable(() -> analyzer.analyze(mock(StorageContainer.class), config, mock(IssueListener.class), globalSettingsStore, new ProgressWrapper(null)));

    assertThat(exception)
      .isInstanceOf(StorageException.class)
      .hasMessage("Stored data for project 'module1' is stale because it was created with a different version of SonarLint. Please update the binding.");
  }

}
