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
import static com.google.gerrit.server.permissions.GlobalPermission.ADMINISTRATE_SERVER;
import static javax.servlet.http.HttpServletResponse.SC_MOVED_PERMANENTLY;
import static javax.servlet.http.HttpServletResponse.SC_MOVED_TEMPORARILY;

import com.google.common.base.Strings;
import com.google.common.flogger.FluentLogger;
import com.google.gerrit.httpd.WebLoginListener;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.config.CanonicalWebUrl;
import com.google.gerrit.server.permissions.PermissionBackend;
import com.google.gerrit.server.plugincontext.PluginSetContext;
import com.google.gerrit.server.plugincontext.PluginSetEntryContext;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Singleton
class RedirectAdminsToBslUrl implements WebLoginListener {
  public static final String BSL_BASE_URL = "https://bsl.gerritforge.com";
  public static final String BSL_REDIRECTION_DONE =
      RedirectAdminsToBslUrl.class.getCanonicalName() + ".done";

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();
  public static final String REDIRECT_ADMINS_TO_BSL_CLASS_NAME =
      RedirectAdminsToBslUrl.class.getName();

  private final PermissionBackend permissionBackend;
  private final String canonicalWebUrl;
  private final PluginSetContext<WebLoginListener> pluginsWebLoginListeners;

  @Inject
  RedirectAdminsToBslUrl(
      PermissionBackend permissionBackend,
      @CanonicalWebUrl String canonicalWebUrl,
      PluginSetContext<WebLoginListener> pluginsWebLoginListeners) {
    this.permissionBackend = permissionBackend;
    this.canonicalWebUrl = canonicalWebUrl;
    this.pluginsWebLoginListeners = pluginsWebLoginListeners;
  }

  @Override
  public void onLogin(IdentifiedUser user, HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    if (isLoginRedirect(response)
        && permissionBackend.user(user).testOrFalse(ADMINISTRATE_SERVER)
        && !hasValidLicence()
        && needsBslRedirection(request)) {
      String callbackLocation = Strings.nullToEmpty(response.getHeader(LOCATION));
      String redirectUrl = buildRedirectUrl(callbackLocation);
      response.setHeader(LOCATION, redirectUrl);

      logger.atWarning().log(
          "[BSL License] Redirecting admin %s to BSL awareness page '%s' instead of location"
              + " '%s'",
          user, redirectUrl, callbackLocation);

      setBslRedirectionDone(request);
    }
  }

  private static void setBslRedirectionDone(HttpServletRequest request) {
    request.setAttribute(BSL_REDIRECTION_DONE, Boolean.TRUE);
  }

  private static boolean needsBslRedirection(HttpServletRequest request) {
    return request.getAttribute(BSL_REDIRECTION_DONE) != Boolean.TRUE;
  }

  private Set<String> allPluginsBslRedirects() {
    return StreamSupport.stream(
            Spliterators.spliteratorUnknownSize(
                pluginsWebLoginListeners.iterator(), Spliterator.ORDERED),
            false)
        .filter(RedirectAdminsToBslUrl::isRedirectAdminsToBslUrlFilter)
        .map(PluginSetEntryContext::getPluginName)
        .collect(Collectors.toSet());
  }

  private static boolean isRedirectAdminsToBslUrlFilter(
      PluginSetEntryContext<WebLoginListener> pluginContext) {
    return pluginContext.get().getClass().getName().equals(REDIRECT_ADMINS_TO_BSL_CLASS_NAME);
  }

  private static boolean isLoginRedirect(HttpServletResponse response) {
    int responseStatus = response.getStatus();
    return responseStatus == SC_MOVED_TEMPORARILY || responseStatus == SC_MOVED_PERMANENTLY;
  }

  private String buildRedirectUrl(String responseLocation) {
    StringBuilder url = new StringBuilder(BSL_BASE_URL);
    Set<String> pluginsWithWebLoginFilters = allPluginsBslRedirects();
    String bslPluginNames = String.join(",", pluginsWithWebLoginFilters);
    url.append("?plugin=").append(URLEncoder.encode(bslPluginNames, StandardCharsets.UTF_8));

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
