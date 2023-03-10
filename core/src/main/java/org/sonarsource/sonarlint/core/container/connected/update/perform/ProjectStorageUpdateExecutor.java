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
package org.sonarsource.sonarlint.core.container.connected.update.perform;

import java.nio.file.Path;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.utils.TempFolder;
import org.sonarsource.sonarlint.core.client.api.util.FileUtils;
import org.sonarsource.sonarlint.core.container.connected.update.ProjectConfigurationDownloader;
import org.sonarsource.sonarlint.core.container.connected.update.ProjectFileListDownloader;
import org.sonarsource.sonarlint.core.container.storage.ProjectStoragePaths;
import org.sonarsource.sonarlint.core.container.storage.ProtobufUtil;
import org.sonarsource.sonarlint.core.container.storage.QualityProfileStore;
import org.sonarsource.sonarlint.core.proto.Sonarlint;
import org.sonarsource.sonarlint.core.proto.Sonarlint.ProjectConfiguration;
import org.sonarsource.sonarlint.core.proto.Sonarlint.StorageStatus;
import org.sonarsource.sonarlint.core.util.ProgressWrapper;
import org.sonarsource.sonarlint.core.util.VersionUtils;

public class ProjectStorageUpdateExecutor {
  private final TempFolder tempFolder;
  private final ProjectConfigurationDownloader projectConfigurationDownloader;
  private final ProjectFileListDownloader projectFileListDownloader;
  private final ServerIssueUpdater serverIssueUpdater;
  private final ProjectStoragePaths projectStoragePaths;
  private final QualityProfileStore qualityProfileStore;

  public ProjectStorageUpdateExecutor(ProjectStoragePaths projectStoragePaths, TempFolder tempFolder, ProjectConfigurationDownloader projectConfigurationDownloader,
    ProjectFileListDownloader projectFileListDownloader, ServerIssueUpdater serverIssueUpdater, QualityProfileStore qualityProfileStore) {
    this.projectStoragePaths = projectStoragePaths;
    this.tempFolder = tempFolder;
    this.projectConfigurationDownloader = projectConfigurationDownloader;
    this.projectFileListDownloader = projectFileListDownloader;
    this.serverIssueUpdater = serverIssueUpdater;
    this.qualityProfileStore = qualityProfileStore;
  }

  public void update(String projectKey, boolean fetchTaintVulnerabilities, ProgressWrapper progress) {
    FileUtils.replaceDir(temp -> {
      ProjectConfiguration projectConfiguration = updateConfiguration(projectKey, qualityProfileStore, temp, progress);
      updateServerIssues(projectKey, temp, projectConfiguration, fetchTaintVulnerabilities, progress);
      updateComponents(projectKey, temp, projectConfiguration, progress);
      updateStatus(temp);
    }, projectStoragePaths.getProjectStorageRoot(projectKey), tempFolder.newDir().toPath());
  }

  private ProjectConfiguration updateConfiguration(String projectKey, QualityProfileStore qualityProfileStore, Path temp, ProgressWrapper progress) {
    ProjectConfiguration projectConfiguration = projectConfigurationDownloader.fetch(projectKey, progress);
    final Set<String> qProfileKeys = qualityProfileStore.getAll().getQprofilesByKeyMap().keySet();
    for (String qpKey : projectConfiguration.getQprofilePerLanguageMap().values()) {
      if (!qProfileKeys.contains(qpKey)) {
        throw new IllegalStateException(
          "Project '" + projectKey + "' is associated to quality profile '" + qpKey + "' that is not in the storage. "
            + "The SonarQube server binding is probably outdated,  please update it.");
      }
    }
    ProtobufUtil.writeToFile(projectConfiguration, temp.resolve(ProjectStoragePaths.PROJECT_CONFIGURATION_PB));
    return projectConfiguration;
  }

  void updateComponents(String projectKey, Path temp, ProjectConfiguration projectConfiguration, ProgressWrapper progress) {
    List<String> sqFiles = projectFileListDownloader.get(projectKey, progress);
    Sonarlint.ProjectComponents.Builder componentsBuilder = Sonarlint.ProjectComponents.newBuilder();

    Map<String, String> modulePathByKey = projectConfiguration.getModulePathByKeyMap();
    for (String fileKey : sqFiles) {
      int idx = StringUtils.lastIndexOf(fileKey, ":");
      String moduleKey = fileKey.substring(0, idx);
      String relativePath = fileKey.substring(idx + 1);
      String prefix = modulePathByKey.getOrDefault(moduleKey, "");
      if (!prefix.isEmpty()) {
        prefix = prefix + "/";
      }
      componentsBuilder.addComponent(prefix + relativePath);
    }
    ProtobufUtil.writeToFile(componentsBuilder.build(), temp.resolve(ProjectStoragePaths.COMPONENT_LIST_PB));
  }

  private void updateServerIssues(String projectKey, Path temp, ProjectConfiguration projectConfiguration, boolean fetchTaintVulnerabilities, ProgressWrapper progress) {
    Path basedir = temp.resolve(ProjectStoragePaths.SERVER_ISSUES_DIR);
    serverIssueUpdater.updateServerIssues(projectKey, projectConfiguration, basedir, fetchTaintVulnerabilities, progress);
  }

  private void updateStatus(Path temp) {
    StorageStatus storageStatus = StorageStatus.newBuilder()
      .setStorageVersion(ProjectStoragePaths.STORAGE_VERSION)
      .setSonarlintCoreVersion(VersionUtils.getLibraryVersion())
      .setUpdateTimestamp(new Date().getTime())
      .build();
    ProtobufUtil.writeToFile(storageStatus, temp.resolve(ProjectStoragePaths.STORAGE_STATUS_PB));
  }

}
