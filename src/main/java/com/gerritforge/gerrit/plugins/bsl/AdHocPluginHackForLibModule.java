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

import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.httpd.WebLoginListener;
import com.google.gerrit.server.plugins.PluginGuiceEnvironment;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Named;

/**
 * Provide access to DynamicSet<WebloginListener> without having direct access to the WebInjector
 * because it isn't exposed from Gerrit's Daemon.
 *
 * <p>Ideally, Gerrit Daemon should have a simple getWebInjector(), but that method doesn't exist in
 * Gerrit v3.13.
 *
 * <p>Hack into the Daemon's web injector by simulating the creation of an ad-hoc plugin that
 * contains the dynamic items of Gerrit's web injector and is named pluginName.
 *
 * <p>The synthetic plugin is then used to access the DynamicSet<WebLoginListener> and add the
 * RedirectAdminsToBslUrl as it was instantiated by a plugin's web injector.
 *
 * <p>TODO: To be sanitised in master by introducing Daemon.getWebInjector() method, which would
 * make this whole hack redundant.
 */
@Singleton
public class AdHocPluginHackForLibModule {
  public static final String PLUGIN_NAME = "plugin_name";

  public final String pluginName;
  public DynamicSet<WebLoginListener> webLoginListeners;
  public RedirectAdminsToBslUrl redirectAdminsToBslUrl;

  private final Injector injector;
  private final PluginGuiceEnvironment pluginGuiceEnvironment;

  @Inject
  AdHocPluginHackForLibModule(
      Injector injector,
      PluginGuiceEnvironment pluginGuiceEnvironment,
      @Named(PLUGIN_NAME) String pluginName) {
    this.injector = injector;
    this.pluginGuiceEnvironment = pluginGuiceEnvironment;
    this.pluginName = pluginName;
  }

  public void injectMembers() {
    Module httpModule = pluginGuiceEnvironment.getHttpModule();
    if (httpModule != null) {
      Injector webInjector = injector.createChildInjector(httpModule);
      webLoginListeners = webInjector.getInstance(Key.get(new TypeLiteral<>() {}));
      redirectAdminsToBslUrl = webInjector.getInstance(RedirectAdminsToBslUrl.class);
    }
  }

  public boolean hasWebModule() {
    return pluginGuiceEnvironment.getHttpModule() != null;
  }
}
