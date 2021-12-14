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
package org.sonarsource.sonarlint.core.container.storage.partialupdate;

import org.sonarsource.sonarlint.core.commons.http.HttpClient;
import org.sonarsource.sonarlint.core.container.connected.IssueStoreFactory;
import org.sonarsource.sonarlint.core.container.connected.update.IssueDownloader;
import org.sonarsource.sonarlint.core.container.connected.update.IssueStorePaths;
import org.sonarsource.sonarlint.core.container.storage.ProjectStoragePaths;
import org.sonarsource.sonarlint.core.serverapi.EndpointParams;
import org.sonarsource.sonarlint.core.serverapi.ServerApiHelper;
import org.sonarsource.sonarlint.core.serverapi.issue.IssueApi;
import org.sonarsource.sonarlint.core.serverapi.source.SourceApi;

public class PartialUpdaterFactory {
  private final ProjectStoragePaths projectStoragePaths;
  private final IssueStorePaths issueStorePaths;

  public PartialUpdaterFactory(ProjectStoragePaths projectStoragePaths, IssueStorePaths issueStorePaths) {
    this.projectStoragePaths = projectStoragePaths;
    this.issueStorePaths = issueStorePaths;
  }

  public PartialUpdater create(EndpointParams endpoint, HttpClient client) {
    ServerApiHelper serverApiHelper = new ServerApiHelper(endpoint, client);
    IssueStoreFactory issueStoreFactory = new IssueStoreFactory();
    IssueDownloader downloader = new IssueDownloader(new IssueApi(serverApiHelper), new SourceApi(serverApiHelper), issueStorePaths);
    return new PartialUpdater(issueStoreFactory, downloader, projectStoragePaths, issueStorePaths);
  }
}
