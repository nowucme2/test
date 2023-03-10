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
package org.sonarsource.sonarlint.core.analyzer.issue;

import org.junit.Test;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.scan.issue.filter.FilterableIssue;
import org.sonarsource.sonarlint.core.container.analysis.filesystem.DefaultTextPointer;
import org.sonarsource.sonarlint.core.container.analysis.filesystem.DefaultTextRange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class DefaultFilterableIssueTest {

  @Test
  public void delegate_textRange_to_rawIssue() {
    TextRange textRange = new DefaultTextRange(new DefaultTextPointer(0, 1), new DefaultTextPointer(2, 3));
    DefaultClientIssue rawIssue = new DefaultClientIssue(null, null, null, null, null, textRange, null, null, null);
    FilterableIssue underTest = new DefaultFilterableIssue(rawIssue, mock(InputComponent.class));
    assertThat(underTest.textRange()).usingRecursiveComparison().isEqualTo(textRange);
  }
}
