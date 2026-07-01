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

import com.google.gerrit.acceptance.LightweightPluginDaemonTest;
import com.google.gerrit.acceptance.RestResponse;
import com.google.gerrit.acceptance.TestPlugin;
import com.google.gerrit.acceptance.config.GerritConfig;
import java.net.URI;
import org.junit.Test;

@TestPlugin(
    name = "gerrit-bsl-license",
    httpModule = "com.gerritforge.gerrit.plugins.bsl.HttpModule")
public class GerritBslLicenseIT extends LightweightPluginDaemonTest {

  @Test
  @GerritConfig(name = "auth.type", value = "DEVELOPMENT_BECOME_ANY_ACCOUNT")
  public void anonymousCallsAreNotRedirected() throws Exception {
    anonymousRestSession.get("/login").assertOK();
  }

  @Test
  @GerritConfig(name = "auth.type", value = "DEVELOPMENT_BECOME_ANY_ACCOUNT")
  public void onlyAdminLoginCallsAreRedirected() throws Exception {
    RestResponse response = anonymousRestSession.get("/login?account_id=" + admin.id().id());

    assertThat(response.getStatusCode()).isEqualTo(SC_MOVED_TEMPORARILY);
    String location = response.getHeader(LOCATION);
    assertThat(location).startsWith(RedirectAdminsToBslUrl.BSL_BASE_URL);

    URI locationUri = URI.create(location);
    String locationQuery = locationUri.getQuery();
    assertThat(locationQuery).contains("plugin=gerrit-bsl-license");
    assertThat(locationQuery).contains("returnURL=");

    RestResponse userResponse = anonymousRestSession.get("/login?account_id=" + user.id().id());
    assertThat(userResponse.getStatusCode()).isEqualTo(SC_MOVED_TEMPORARILY);
    String userLocation = userResponse.getHeader(LOCATION);
    assertThat(userLocation).doesNotContain(RedirectAdminsToBslUrl.BSL_BASE_URL);
  }

  @Test
  @GerritConfig(name = "auth.type", value = "DEVELOPMENT_BECOME_ANY_ACCOUNT")
  public void adminAlreadyLoggedInCallsAreNotRedirected() throws Exception {
    assertThat(adminRestSession.get("/login?account_id=" + admin.id().id()).getStatusCode())
        .isNotEqualTo(SC_MOVED_TEMPORARILY);
  }
}
