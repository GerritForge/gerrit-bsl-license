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
 * This should be used when a libModule only registers a sys module.
 */
public class WebLoginListenerLibModule implements LifecycleListener {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  /**
   * Returns the module to install from the lib module's sys module.
   *
   * @param componentName name of the component to show on the licensing page
   */
  public static Module sysModule(String componentName) {
    // Hack: lib modules can only contribute to the sys injector, while the WebLoginListener
    // extension point lives in the web injector. Gerrit creates the web injector after the sys
    // one, so it does not exist yet when this module is configured. Wrapping the registration
    // in a LifecycleListener defers it to start(), which Gerrit calls once every injector has
    // been created, when PluginGuiceEnvironment.getHttpModule() becomes available.
    return new LifecycleModule() {
      @Override
      protected void configure() {
        listener().toInstance(new WebLoginListenerLibModule(componentName));
      }
    };
  }

  private final String componentName;
  private Injector sysInjector;
  private PluginGuiceEnvironment pluginGuiceEnvironment;

  private WebLoginListenerLibModule(String componentName) {
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

    // Hack: lib modules have no web injector, so build one from getHttpModule(), as Gerrit does
    // for plugins, and add the redirect to Gerrit's WebLoginListener set under componentName.
    Injector webInjector = sysInjector.createChildInjector(httpModule);
    webInjector
        .getInstance(Key.get(new TypeLiteral<DynamicSet<WebLoginListener>>() {}))
        .add(componentName, webInjector.getInstance(RedirectAdminsToBslUrl.class));
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
    // Lib modules cannot be unloaded; stop() only runs at server shutdown, when the
    // DynamicSet is discarded anyway.
  }
}
