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

import com.google.common.flogger.FluentLogger;
import com.google.gerrit.extensions.events.LifecycleListener;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.extensions.registration.RegistrationHandle;
import com.google.gerrit.httpd.WebLoginListener;
import com.google.gerrit.lifecycle.LifecycleModule;
import com.google.gerrit.server.plugins.PluginGuiceEnvironment;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;

/**
 * BSL awareness for components loaded as Gerrit lib modules ({@code gerrit.installModule}).
 *
 * <p>Lib modules cannot contribute to Gerrit's web injector, where the {@link WebLoginListener}
 * extension point lives. This module must be installed from the lib module's sys module; at startup
 * it registers {@link RedirectAdminsToBslUrl} into the web injector's {@code
 * DynamicSet<WebLoginListener>} under {@code componentName}.
 */
public class BslLibModule extends LifecycleModule {
  private final String componentName;

  public BslLibModule(String componentName) {
    this.componentName = componentName;
  }

  @Override
  protected void configure() {
    listener().toInstance(new WebLoginListenerRegistration(componentName));
  }

  private static class WebLoginListenerRegistration implements LifecycleListener {
    private static final FluentLogger logger = FluentLogger.forEnclosingClass();

    private final String componentName;
    private Injector sysInjector;
    private PluginGuiceEnvironment pluginGuiceEnvironment;
    private RegistrationHandle registration;

    WebLoginListenerRegistration(String componentName) {
      this.componentName = componentName;
    }

    @Inject
    void init(Injector sysInjector, PluginGuiceEnvironment pluginGuiceEnvironment) {
      this.sysInjector = sysInjector;
      this.pluginGuiceEnvironment = pluginGuiceEnvironment;
    }

    @Override
    public void start() {
      Module httpModule = pluginGuiceEnvironment.getHttpModule();
      if (httpModule == null) {
        // Without HTTP, admins are never redirected to the BSL awareness page at login, so this
        // log is the only place they learn about the license.
        logger.atWarning().log(
            "[BSL License] %s is licensed under the Business Source License 1.1 (BSL 1.1)."
                + " Review the license terms at %s and, if your use exceeds the Additional Use"
                + " Grant, contact GerritForge at https://gerritforge.com/contact for a"
                + " commercial license.",
            componentName, RedirectAdminsToBslUrl.BSL_BASE_URL);
        return;
      }

      Injector webInjector = sysInjector.createChildInjector(httpModule);
      registration =
          webInjector
              .getInstance(Key.get(new TypeLiteral<DynamicSet<WebLoginListener>>() {}))
              .add(componentName, webInjector.getInstance(RedirectAdminsToBslUrl.class));
    }

    @Override
    public void stop() {
      if (registration != null) {
        registration.remove();
        registration = null;
      }
    }
  }
}
