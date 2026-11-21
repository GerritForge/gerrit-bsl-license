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

import static com.gerritforge.gerrit.plugins.bsl.AdHocPluginHackForLibModule.PLUGIN_NAME;

import com.google.gerrit.extensions.events.LifecycleListener;
import com.google.gerrit.lifecycle.LifecycleModule;
import com.google.inject.Inject;
import com.google.inject.Module;
import com.google.inject.Singleton;
import com.google.inject.name.Names;

/**
 * BSL awareness for components loaded as Gerrit lib modules ({@code gerrit.installModule}). This
 * should be used when a libModule only registers a sys module.
 */
@Singleton
public class RedirectAdminRegistrationLifecycleListener implements LifecycleListener {
  private final AdHocPluginHackForLibModule adHocPluginHack;

  /**
   * Returns the module to install from the lib module's sys module.
   *
   * @param libModuleName name of the component to show on the licensing page
   */
  public static Module sysModule(String libModuleName) {
    return new LifecycleModule() {
      @Override
      protected void configure() {
        // Hack: lib modules do not have a plugin name, therefore could not be registered in the
        // DynamicSet<WebLoginListener>.
        // Add an artificial binding of the lib module name as a plugin name, so that the addition
        // to the DynamicSet<WebLoginListener> can succeed.
        bind(String.class).annotatedWith(Names.named(PLUGIN_NAME)).toInstance(libModuleName);
        listener().to(RedirectAdminRegistrationLifecycleListener.class);
      }
    };
  }

  @Inject
  public RedirectAdminRegistrationLifecycleListener(AdHocPluginHackForLibModule adHocPluginHack) {
    this.adHocPluginHack = adHocPluginHack;
  }

  @Override
  public void start() {
    // Hack: Gerrit creates the web injector after the sys injector, therefore the injection is
    // available only when the startup process of the plugins is initiated.
    // Deferring the extraction of the DynamicSet<WebLoginListener> in a LifecycleListener's start()
    // method
    // ensure that the web injector has been already created and is available to use.
    adHocPluginHack.injectMembers();

    // Gerrit HTTP listen could be disabled with container.daemonOpt = --disable-httpd
    // therefore ensure that the HTTP server is available for being able to add a new
    // web login listener to it.
    if (adHocPluginHack.hasWebModule()) {
      adHocPluginHack.webLoginListeners.add(
          adHocPluginHack.pluginName, adHocPluginHack.redirectAdminsToBslUrl);
    }
  }

  @Override
  public void stop() {
    // Lib modules cannot be unloaded; stop() only runs at server shutdown, when the
    // DynamicSet is discarded anyway.
  }
}
