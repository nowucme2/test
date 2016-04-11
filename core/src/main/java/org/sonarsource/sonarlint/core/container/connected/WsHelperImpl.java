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
package org.sonarsource.sonarlint.core.container.connected;

import com.google.gson.Gson;
import org.sonarsource.sonarlint.core.client.api.common.SonarLintWrappedException;
import org.sonarsource.sonarlint.core.client.api.connected.ServerConfiguration;
import org.sonarsource.sonarlint.core.client.api.connected.UnsupportedServerException;
import org.sonarsource.sonarlint.core.client.api.connected.ValidationResult;
import org.sonarsource.sonarlint.core.client.api.connected.WsHelper;
import org.sonarsource.sonarlint.core.container.connected.validate.AuthenticationChecker;
import org.sonarsource.sonarlint.core.container.connected.validate.DefaultValidationResult;
import org.sonarsource.sonarlint.core.container.connected.validate.PluginVersionChecker;
import org.sonarsource.sonarlint.core.container.connected.validate.ServerVersionAndStatusChecker;
import org.sonarsource.sonarlint.core.proto.Sonarlint.ServerInfos;

import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class WsHelperImpl implements WsHelper {

  @Override
  public ValidationResult validateConnection(ServerConfiguration serverConfig) {
    checkNotNull(serverConfig);
    try {
      SonarLintWsClient client = new SonarLintWsClient(serverConfig);
      ServerVersionAndStatusChecker serverChecker = new ServerVersionAndStatusChecker(client);
      PluginVersionChecker pluginsChecker = new PluginVersionChecker(client);
      ServerInfos serverInfo = serverChecker.checkVersionAndStatus();
      pluginsChecker.checkPlugins(serverInfo.getVersion());
      return new AuthenticationChecker(client).validateCredentials();
    } catch (UnsupportedServerException e) {
      return new DefaultValidationResult(false, e.getMessage());
    } catch (RuntimeException e) {
      throw SonarLintWrappedException.wrap(e);
    }
  }

  @Override
  public String generateAuthenticationToken(ServerConfiguration serverConfig, String name, boolean force) {
    checkNotNull(serverConfig);
    try {
      SonarLintWsClient client = new SonarLintWsClient(serverConfig);

      ServerVersionAndStatusChecker versionChecker = new ServerVersionAndStatusChecker(client);
      // in 5.3 login is mandatory and user needs admin privileges
      versionChecker.checkVersionAndStatus("5.4");

      if (force) {
        // revoke
        client.post("api/user_tokens/revoke?name=" + name);
      }

      // create
      CloseableWsResponse response = client.post("api/user_tokens/generate?name=" + name);
      Map<?, ?> javaRootMapObject = new Gson().fromJson(response.content(), Map.class);
      return (String) javaRootMapObject.get("token");
    } catch (RuntimeException e) {
      throw SonarLintWrappedException.wrap(e);
    }
  }
}
