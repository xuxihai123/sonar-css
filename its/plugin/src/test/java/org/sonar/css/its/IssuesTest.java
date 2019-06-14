/*
 * SonarCSS
 * Copyright (C) 2018-2019 SonarSource SA
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
package org.sonar.css.its;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import com.sonar.orchestrator.locator.FileLocation;
import java.io.File;
import java.util.Collections;
import java.util.List;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.css.plugin.CssRules;
import org.sonarqube.ws.Issues.Issue;
import org.sonarqube.ws.client.issues.SearchRequest;
import org.sonarsource.analyzer.commons.ProfileGenerator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.css.its.Tests.newWsClient;

public class IssuesTest {

  private static String PROJECT_KEY = "issues-project";

  @ClassRule
  public static Orchestrator orchestrator = Tests.ORCHESTRATOR;

  @BeforeClass
  public static void prepare() {
    File profile = ProfileGenerator.generateProfile(orchestrator.getServer().getUrl(), "css", "css", new ProfileGenerator.RulesConfiguration(), Collections.emptySet());
    orchestrator.getServer().restoreProfile(FileLocation.of(profile));

    orchestrator.getServer().provisionProject(PROJECT_KEY, PROJECT_KEY);
    orchestrator.getServer().associateProjectToQualityProfile(PROJECT_KEY, "css", "rules");

    SonarScanner scanner = Tests.createScanner(PROJECT_KEY);
    scanner.setProperty("sonar.html.file.suffixes", ".htm");
    orchestrator.executeBuild(scanner);
  }

  @Test
  public void test() {
    SearchRequest request = new SearchRequest();
    request.setComponentKeys(Collections.singletonList(PROJECT_KEY));
    List<Issue> issuesList = newWsClient().issues().search(request).getIssuesList();

    assertThat(issuesList).extracting("rule").hasSize(
      CssRules.getRuleClasses().size() * 3 /* issues are raised against .css, .less and .scss */
      + 2 /* for html and vue files */
      + 1 /* issue S4654 is raised for comment */
      + 1 /* issue S4662 is raised for cssModules.css */
      + 6 /* S5362 raises at the same place as S4650 */
      - 2 /* issue S4668 not raised on .less nor .scss */);
  }

}
