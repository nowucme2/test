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

import javax.annotation.Nullable;
import org.sonarsource.sonarlint.core.analyzer.sensor.SensorsExecutor;
import org.sonarsource.sonarlint.core.client.api.common.analysis.AnalysisResults;
import org.sonarsource.sonarlint.core.client.api.common.analysis.IssueListener;
import org.sonarsource.sonarlint.core.client.api.connected.ConnectedAnalysisConfiguration;
import org.sonarsource.sonarlint.core.client.api.connected.GlobalStorageStatus;
import org.sonarsource.sonarlint.core.client.api.connected.ProjectStorageStatus;
import org.sonarsource.sonarlint.core.client.api.exceptions.StorageException;
import org.sonarsource.sonarlint.core.container.ComponentContainer;
import org.sonarsource.sonarlint.core.container.analysis.AnalysisContainer;
import org.sonarsource.sonarlint.core.container.connected.DefaultServer;
import org.sonarsource.sonarlint.core.container.model.DefaultAnalysisResult;
import org.sonarsource.sonarlint.core.util.ProgressWrapper;

public class StorageAnalyzer {
  private final ProjectStorageStatusReader moduleUpdateStatusReader;
  private final GlobalUpdateStatusReader globalUpdateStatusReader;

  public StorageAnalyzer(GlobalUpdateStatusReader globalUpdateStatusReader, ProjectStorageStatusReader moduleUpdateStatusReader) {
    this.globalUpdateStatusReader = globalUpdateStatusReader;
    this.moduleUpdateStatusReader = moduleUpdateStatusReader;
  }

  private void checkStatus(@Nullable String projectKey) {
    GlobalStorageStatus updateStatus = globalUpdateStatusReader.read();
    if (updateStatus == null) {
      throw new StorageException("Missing global data. Please update server.", false);
    }
    if (projectKey != null) {
      ProjectStorageStatus moduleUpdateStatus = moduleUpdateStatusReader.apply(projectKey);
      if (moduleUpdateStatus == null) {
        throw new StorageException(String.format("No data stored for project '%s'. Please update the binding.", projectKey), false);
      } else if (moduleUpdateStatus.isStale()) {
        throw new StorageException(String.format("Stored data for project '%s' is stale because "
          + "it was created with a different version of SonarLint. Please update the binding.", projectKey), false);
      }
    }
  }

  public AnalysisResults analyze(ComponentContainer parent, ConnectedAnalysisConfiguration configuration, IssueListener issueListener, GlobalSettingsStore globalSettingsStore,
    ProgressWrapper progress) {
    checkStatus(configuration.projectKey());

    AnalysisContainer analysisContainer = new AnalysisContainer(parent, progress);
    DefaultAnalysisResult defaultAnalysisResult = new DefaultAnalysisResult();

    analysisContainer.add(
      globalSettingsStore,
      configuration,
      issueListener,
      new SonarQubeActiveRulesProvider(),
      DefaultServer.class,
      defaultAnalysisResult,
      SensorsExecutor.class);

    analysisContainer.execute();
    return defaultAnalysisResult;
  }
}
