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

import static com.gerritforge.gerrit.plugins.bsl.BslLicensePathProvider.BSL_LICENSE_FILE;
import static com.gerritforge.gerrit.plugins.bsl.X509TestCertificatesFixture.INSTANT_AFTER_X509_VALIDITY_PERIOD;
import static com.gerritforge.gerrit.plugins.bsl.X509TestCertificatesFixture.INSTANT_WITHIN_X509_VALIDITY_PERIOD;
import static com.gerritforge.gerrit.plugins.bsl.X509TestCertificatesFixture.X509_LICENSE_FILE_CERTIFICATE_FOR_ANOTHER_HOST;
import static com.gerritforge.gerrit.plugins.bsl.X509TestCertificatesFixture.X509_LICENSE_FILE_CERTIFICATE_FOR_ANOTHER_PLUGIN;
import static com.gerritforge.gerrit.plugins.bsl.X509TestCertificatesFixture.X509_LICENSE_FILE_CERTIFICATE_FOR_GERRIT_BSL_LICENSE;
import static com.gerritforge.gerrit.plugins.bsl.X509TestCertificatesFixture.X509_LICENSE_FILE_CERTIFICATE_SIGNED_BY_ANOTHER_CA;
import static com.google.common.net.HttpHeaders.LOCATION;
import static com.google.common.truth.Truth.assertThat;
import static org.apache.http.HttpStatus.SC_MOVED_TEMPORARILY;

import com.google.gerrit.acceptance.LightweightPluginDaemonTest;
import com.google.gerrit.acceptance.NoHttpd;
import com.google.gerrit.acceptance.RestResponse;
import com.google.gerrit.acceptance.TestPlugin;
import com.google.gerrit.acceptance.config.GerritConfig;
import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.junit.Before;
import org.junit.Test;

@TestPlugin(
    name = X509TestCertificatesFixture.BSL_PLUGIN_NAME,
    httpModule = "com.gerritforge.gerrit.plugins.bsl.GerritBslLicenseIT$TestHttpModule")
public class GerritBslLicenseIT extends LightweightPluginDaemonTest {
  private static final String LIB_MODULE_CLASS_NAME =
      "com.gerritforge.gerrit.plugins.bsl.GerritBslLicenseIT$TestLibModule";
  private static final String LIB_MODULE_COMPONENT_NAME = "bsl-test-lib-module";

  static class TestHttpModule extends HttpModule {
    private static final AtomicReference<Instant> instantNow = new AtomicReference<>();
    private static final List<String> licenseError = new ArrayList<>();

    static void setNow(Instant now) {
      instantNow.set(now);
    }

    static List<String> getLicenseErrors() {
      return licenseError;
    }

    static void clearLicenseError() {
      licenseError.clear();
    }

    @Override
    protected void configure() {
      configureRedirectOnLogin();
      bind(Instant.class).annotatedWith(InstantNow.class).toProvider(instantNow::get);
      bind(Key.get(new TypeLiteral<Consumer<String>>() {}).withAnnotation(ErrorConsumer.class))
          .toInstance(licenseError::add);
      bind(Path.class).annotatedWith(BslLicensePath.class).toProvider(BslLicensePathProvider.class);
    }
  }

  /** Simulates a BSL component deployed via {@code gerrit.installModule}. */
  public static class TestLibModule extends AbstractModule {
    @Override
    protected void configure() {
      install(RedirectAdminRegistrationLifecycleListener.sysModule(LIB_MODULE_COMPONENT_NAME));
    }
  }

  @Before
  public void setUp() throws IOException {
    Files.deleteIfExists(sitePaths.etc_dir.resolve(BSL_LICENSE_FILE));
    TestHttpModule.clearLicenseError();
    TestHttpModule.setNow(Instant.now());
  }

  @Test
  @GerritConfig(name = "auth.type", value = "DEVELOPMENT_BECOME_ANY_ACCOUNT")
  public void anonymousCallsAreNotRedirected() throws Exception {
    anonymousRestSession.get("/login").assertOK();
  }

  @Test
  @GerritConfig(name = "auth.type", value = "DEVELOPMENT_BECOME_ANY_ACCOUNT")
  public void onlyAdminLoginCallsAreRedirected() throws Exception {
    String location = assertRedirectedToBsl(loginAsAdmin());

    assertThat(TestHttpModule.getLicenseErrors())
        .containsExactly(X509LicenseCheck.NO_LICENSED_BSL_PLUGINS_MSG);
    TestHttpModule.clearLicenseError();

    URI locationUri = URI.create(location);
    String locationQuery = locationUri.getQuery();
    assertThat(locationQuery).contains("plugin=gerrit-bsl-license");
    assertThat(locationQuery).contains("returnURL=");

    RestResponse userResponse = anonymousRestSession.get("/login?account_id=" + user.id().id());
    assertNotRedirectedToBsl(userResponse);
    assertNoLicenseErrors();
  }

  @Test
  @GerritConfig(name = "auth.type", value = "DEVELOPMENT_BECOME_ANY_ACCOUNT")
  public void adminLoginIsNotRedirectedToBsl_validX509LicenseIsPresent() throws Exception {
    configureX509BslLicenseFile(X509_LICENSE_FILE_CERTIFICATE_FOR_GERRIT_BSL_LICENSE);
    TestHttpModule.setNow(INSTANT_WITHIN_X509_VALIDITY_PERIOD);

    assertNotRedirectedToBsl(loginAsAdmin());
    assertNoLicenseErrors();
  }

  @Test
  @GerritConfig(name = "auth.type", value = "DEVELOPMENT_BECOME_ANY_ACCOUNT")
  public void adminLoginIsRedirectedToBsl_expiredX509LicenseIsPresent() throws Exception {
    configureX509BslLicenseFile(X509_LICENSE_FILE_CERTIFICATE_FOR_GERRIT_BSL_LICENSE);
    TestHttpModule.setNow(INSTANT_AFTER_X509_VALIDITY_PERIOD);

    assertRedirectedToBsl(loginAsAdmin());
    assertThat(TestHttpModule.getLicenseErrors())
        .containsExactly(X509LicenseCheck.LICENSED_BSL_PLUGINS_OUTSIDE_VALIDITY_PERIOD_MSG);
  }

  @Test
  @GerritConfig(name = "auth.type", value = "DEVELOPMENT_BECOME_ANY_ACCOUNT")
  public void adminLoginIsRedirectedToBsl_invalidLicenseIsPresent() throws Exception {
    configureX509BslLicenseFile(
        X509_LICENSE_FILE_CERTIFICATE_FOR_ANOTHER_HOST,
        X509_LICENSE_FILE_CERTIFICATE_SIGNED_BY_ANOTHER_CA);
    TestHttpModule.setNow(INSTANT_WITHIN_X509_VALIDITY_PERIOD);

    assertRedirectedToBsl(loginAsAdmin());
    assertThat(TestHttpModule.getLicenseErrors())
        .containsExactly(
            X509LicenseCheck.LICENSED_BSL_PLUGINS_NOT_MATCHING_CERT_CN_MSG,
            X509LicenseCheck.LICENSED_BSL_PLUGINS_CERT_NOT_VALID_MSG);
  }

  @Test
  @GerritConfig(name = "auth.type", value = "DEVELOPMENT_BECOME_ANY_ACCOUNT")
  public void adminLoginIsRedirectedToBsl_notExpiredLicenseForAnotherPluginIsPresent()
      throws Exception {
    configureX509BslLicenseFile(X509_LICENSE_FILE_CERTIFICATE_FOR_ANOTHER_PLUGIN);
    TestHttpModule.setNow(INSTANT_WITHIN_X509_VALIDITY_PERIOD);

    assertRedirectedToBsl(loginAsAdmin());
    assertNoLicenseErrors();
  }

  @Test
  @GerritConfig(name = "auth.type", value = "DEVELOPMENT_BECOME_ANY_ACCOUNT")
  public void adminAlreadyLoggedInCallsAreNotRedirected() throws Exception {
    assertThat(adminRestSession.get("/login?account_id=" + admin.id().id()).getStatusCode())
        .isNotEqualTo(SC_MOVED_TEMPORARILY);
    assertNoLicenseErrors();
  }

  @Test
  @GerritConfig(name = "gerrit.installModule", value = LIB_MODULE_CLASS_NAME)
  @GerritConfig(name = "auth.type", value = "DEVELOPMENT_BECOME_ANY_ACCOUNT")
  public void pluginAndLibModuleAdminLoginIsRedirectedOnceListingBoth() throws Exception {
    String location = assertRedirectedToBsl(loginAsAdmin());

    Map<String, String> params =
        URLEncodedUtils.parse(URI.create(location), StandardCharsets.UTF_8).stream()
            .collect(Collectors.toMap(NameValuePair::getName, NameValuePair::getValue));
    assertThat(Arrays.asList(params.get("plugin").split(",")))
        .containsExactly("gerrit-bsl-license", LIB_MODULE_COMPONENT_NAME);
    assertThat(params.get("returnURL")).doesNotContain(RedirectAdminsToBslUrl.BSL_BASE_URL);
  }

  @Test
  @NoHttpd
  @GerritConfig(name = "gerrit.installModule", value = LIB_MODULE_CLASS_NAME)
  @GerritConfig(name = "auth.type", value = "DEVELOPMENT_BECOME_ANY_ACCOUNT")
  public void pluginAndBslLibModuleDoesImpactGerritWithoutHttpDaemon() throws Exception {
    assertThat(server.getHttpdInjector()).isEmpty();

    AdHocPluginHackForLibModule pluginHackForLibModule =
        server.getTestInjector().getInstance(AdHocPluginHackForLibModule.class);
    assertThat(pluginHackForLibModule.webLoginListeners).isNull();
  }

  private RestResponse loginAsAdmin() throws Exception {
    return anonymousRestSession.get("/login?account_id=" + admin.id().id());
  }

  private void configureX509BslLicenseFile(CharSequence... x509LicenseCertificatesPems)
      throws IOException {
    Path bslLicenseFile = sitePaths.etc_dir.resolve(BSL_LICENSE_FILE);
    Files.writeString(bslLicenseFile, String.join("\n", x509LicenseCertificatesPems));
  }

  private static String assertRedirectedToBsl(RestResponse response) {
    assertThat(response.getStatusCode()).isEqualTo(SC_MOVED_TEMPORARILY);
    String location = response.getHeader(LOCATION);
    assertThat(location).startsWith(RedirectAdminsToBslUrl.BSL_BASE_URL);
    return location;
  }

  private static void assertNotRedirectedToBsl(RestResponse userResponse) {
    assertThat(userResponse.getStatusCode()).isEqualTo(SC_MOVED_TEMPORARILY);
    String userLocation = userResponse.getHeader(LOCATION);
    assertThat(userLocation).doesNotContain(RedirectAdminsToBslUrl.BSL_BASE_URL);
  }

  private static void assertNoLicenseErrors() {
    assertThat(TestHttpModule.getLicenseErrors()).isEmpty();
  }
}
