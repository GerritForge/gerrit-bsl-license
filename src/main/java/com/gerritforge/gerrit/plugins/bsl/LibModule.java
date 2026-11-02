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

/** BSL awareness for components loaded as Gerrit lib modules ({@code gerrit.installModule}). */
public class LibModule extends LifecycleModule {
  private final String componentName;

  public LibModule(String componentName) {
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
        logBslNotice();
        return;
      }

      try {
        Injector webInjector = sysInjector.createChildInjector(httpModule);
        registration =
            webInjector
                .getInstance(Key.get(new TypeLiteral<DynamicSet<WebLoginListener>>() {}))
                .add(componentName, webInjector.getInstance(RedirectAdminsToBslUrl.class));
      } catch (RuntimeException e) {
        logger.atWarning().withCause(e).log(
            "[BSL License] Unable to register the BSL login redirect for %s", componentName);
        logBslNotice();
      }
    }

    private void logBslNotice() {
      // Without the login redirect, admins are never sent to the BSL awareness page, so this
      // log is the only place they learn about the license.
      logger.atWarning().log(
          "[BSL License] %s is licensed under the Business Source License 1.1 (BSL 1.1)."
              + " Review the license terms at %s and, if your use exceeds the Additional Use"
              + " Grant, contact GerritForge at https://gerritforge.com/contact for a"
              + " commercial license.",
          componentName, RedirectAdminsToBslUrl.BSL_BASE_URL);
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
