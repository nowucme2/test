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
package org.sonarsource.sonarlint.core.analyzer.sensor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.BatchSide;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.events.SensorsPhaseHandler;
import org.sonar.api.batch.events.SensorsPhaseHandler.SensorsPhaseEvent;
import org.sonar.api.resources.Project;

@BatchSide
public class SensorsExecutor {

  private static final Logger LOG = LoggerFactory.getLogger(SensorsExecutor.class);

  private Project module;
  private BatchExtensionDictionnary selector;
  private SensorsPhaseHandler[] handlers;

  public SensorsExecutor(BatchExtensionDictionnary selector, Project project) {
    this(selector, project, new SensorsPhaseHandler[0]);
  }

  public SensorsExecutor(BatchExtensionDictionnary selector, Project project, SensorsPhaseHandler[] handlers) {
    this.selector = selector;
    this.module = project;
    this.handlers = handlers;
  }

  public void execute(SensorContext context) {
    Collection<Sensor> sensors = selector.select(Sensor.class, module, true, null);

    for (SensorsPhaseHandler h : handlers) {
      h.onSensorsPhase(new DefaultSensorsPhaseEvent(sensors, true));
    }

    for (Sensor sensor : sensors) {
      executeSensor(context, sensor);
    }

    for (SensorsPhaseHandler h : handlers) {
      h.onSensorsPhase(new DefaultSensorsPhaseEvent(sensors, false));
    }
  }

  private void executeSensor(SensorContext context, Sensor sensor) {
    LOG.debug("Execute Sensor: " + sensor.toString());
    sensor.analyse(module, context);
  }

  private static class DefaultSensorsPhaseEvent implements SensorsPhaseEvent {
    private List<Sensor> sensors;
    private boolean start;

    DefaultSensorsPhaseEvent(Collection<Sensor> sensors, boolean start) {
      this.sensors = new ArrayList<>(sensors);
      this.start = start;
    }

    @Override
    public List<Sensor> getSensors() {
      return sensors;
    }

    @Override
    public boolean isStart() {
      return start;
    }

    @Override
    public boolean isEnd() {
      return !start;
    }
  }
}
