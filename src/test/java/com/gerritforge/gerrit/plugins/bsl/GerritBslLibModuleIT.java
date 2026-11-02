// Copyright (C) 2026 GerritForge, Inc.
//
// Licensed under the BSL 1.1 (the "License");
// you may not use this file except in compliance with the License.
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.gerritforge.gerrit.plugins.bsl;

import static com.google.common.net.HttpHeaders.LOCATION;
import static com.google.common.truth.Truth.assertThat;
import static org.apache.http.HttpStatus.SC_MOVED_TEMPORARILY;

import com.google.gerrit.acceptance.AbstractDaemonTest;
import com.google.gerrit.acceptance.RestResponse;
import com.google.gerrit.acceptance.config.GerritConfig;
import com.google.inject.AbstractModule;
import org.junit.Test;

/** BSL redirect for a component deployed only as a lib module, with no BSL plugin installed. */
public class GerritBslLibModuleIT extends AbstractDaemonTest {
  static final String LIB_MODULE_CLASS_NAME =
      "com.gerritforge.gerrit.plugins.bsl.GerritBslLibModuleIT$TestLibModule";
  static final String LIB_MODULE_COMPONENT_NAME = "bsl-test-lib-module";

  /** Simulates a BSL component deployed via {@code gerrit.installModule}. */
  public static class TestLibModule extends AbstractModule {
    @Override
    protected void configure() {
      install(new LibModule(LIB_MODULE_COMPONENT_NAME));
    }
  }

  @Test
  @GerritConfig(name = "gerrit.installModule", value = LIB_MODULE_CLASS_NAME)
  @GerritConfig(name = "auth.type", value = "DEVELOPMENT_BECOME_ANY_ACCOUNT")
  public void adminLoginIsRedirectedToBslUrl() throws Exception {
    RestResponse response = anonymousRestSession.get("/login?account_id=" + admin.id().id());

    assertThat(response.getStatusCode()).isEqualTo(SC_MOVED_TEMPORARILY);
    String location = response.getHeader(LOCATION);
    assertThat(location).startsWith(RedirectAdminsToBslUrl.BSL_BASE_URL);
    assertThat(location).contains("plugin=" + LIB_MODULE_COMPONENT_NAME + "&");
  }
}
