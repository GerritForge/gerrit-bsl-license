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

package com.gerritforge.bsl.licence.manager;

import static com.google.common.net.HttpHeaders.LOCATION;
import static com.google.gerrit.server.permissions.GlobalPermission.ADMINISTRATE_SERVER;
import static javax.servlet.http.HttpServletResponse.SC_MOVED_PERMANENTLY;
import static javax.servlet.http.HttpServletResponse.SC_MOVED_TEMPORARILY;

import com.google.common.base.Strings;
import com.google.common.flogger.FluentLogger;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.httpd.WebLoginListener;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.config.CanonicalWebUrl;
import com.google.gerrit.server.permissions.PermissionBackend;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Singleton
class RedirectAdminsToBslUrl implements WebLoginListener {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();
  private static final String BSL_BASE_URL = "https://bsl.gerritforge.com";

  private final PermissionBackend permissionBackend;
  private final String pluginName;
  private final String canonicalWebUrl;

  @Inject
  RedirectAdminsToBslUrl(
      PermissionBackend permissionBackend,
      @PluginName String pluginName,
      @CanonicalWebUrl String canonicalWebUrl) {
    this.permissionBackend = permissionBackend;
    this.pluginName = pluginName;
    this.canonicalWebUrl = canonicalWebUrl;
  }

  @Override
  public void onLogin(IdentifiedUser user, HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    if (isLoginRedirect(response)
        && permissionBackend.user(user).testOrFalse(ADMINISTRATE_SERVER)
        && !hasValidLicence()) {
      String callbackLocation = Strings.nullToEmpty(response.getHeader(LOCATION));
      String redirectUrl = buildRedirectUrl(callbackLocation);
      response.setHeader(LOCATION, redirectUrl);

      logger.atWarning().log(
          "[BSL License] Redirecting admin %s to BSL awareness page '%s' instead of location"
              + " '%s'",
          user, redirectUrl, callbackLocation);
    }
  }

  private static boolean isLoginRedirect(HttpServletResponse response) {
    int responseStatus = response.getStatus();
    return responseStatus == SC_MOVED_TEMPORARILY || responseStatus == SC_MOVED_PERMANENTLY;
  }

  private String buildRedirectUrl(String responseLocation) {
    StringBuilder url = new StringBuilder(BSL_BASE_URL);
    url.append("?plugin=").append(URLEncoder.encode(pluginName, StandardCharsets.UTF_8));

    URI requestUri;
    if (responseLocation.toLowerCase(Locale.ROOT).startsWith("http")) {
      requestUri = URI.create(responseLocation);
    } else {
      requestUri = URI.create(canonicalWebUrl).resolve(responseLocation);
    }

    url.append("&returnURL=")
        .append(URLEncoder.encode(requestUri.toString(), StandardCharsets.UTF_8));
    return url.toString();
  }

  private boolean hasValidLicence() {
    return false;
  }

  @Override
  public void onLogout(
      IdentifiedUser user, HttpServletRequest request, HttpServletResponse response) {}
}
