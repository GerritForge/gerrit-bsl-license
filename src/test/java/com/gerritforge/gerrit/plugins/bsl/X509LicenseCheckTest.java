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

import static com.gerritforge.gerrit.plugins.bsl.X509TestCertificatesFixture.BSL_PLUGIN_NAME;
import static com.gerritforge.gerrit.plugins.bsl.X509TestCertificatesFixture.BSL_PLUGIN_NAME_OTHER;
import static com.gerritforge.gerrit.plugins.bsl.X509TestCertificatesFixture.INSTANT_AFTER_X509_VALIDITY_PERIOD;
import static com.gerritforge.gerrit.plugins.bsl.X509TestCertificatesFixture.INSTANT_WITHIN_X509_VALIDITY_PERIOD;
import static com.gerritforge.gerrit.plugins.bsl.X509TestCertificatesFixture.X509_LICENSE_FILE_CERTIFICATE_FOR_ANOTHER_HOST;
import static com.gerritforge.gerrit.plugins.bsl.X509TestCertificatesFixture.X509_LICENSE_FILE_CERTIFICATE_FOR_ANOTHER_PLUGIN;
import static com.gerritforge.gerrit.plugins.bsl.X509TestCertificatesFixture.X509_LICENSE_FILE_CERTIFICATE_FOR_GERRIT_BSL_LICENSE;
import static com.gerritforge.gerrit.plugins.bsl.X509TestCertificatesFixture.X509_LICENSE_FILE_CERTIFICATE_SIGNED_BY_ANOTHER_CA;
import static com.google.common.truth.Truth.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class X509LicenseCheckTest {

  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

  @Test
  public void certificateAccepted_validIssuedByTrustAnchor() throws IOException {
    StringBuilder errors = new StringBuilder();

    Set<String> plugins =
        new X509LicenseCheck(
                bslLicensePath(X509_LICENSE_FILE_CERTIFICATE_FOR_GERRIT_BSL_LICENSE),
                () -> "http://localhost/",
                () -> INSTANT_WITHIN_X509_VALIDITY_PERIOD,
                errors::append)
            .pluginsWithValidLicense();
    assertThat(plugins).containsExactly(BSL_PLUGIN_NAME);
    assertThat(errors.toString()).isEmpty();
  }

  @Test
  public void certificateRejected_validIssuedByTrustAnchorOnTheWrongHostname() throws IOException {
    StringBuilder errors = new StringBuilder();

    Set<String> plugins =
        new X509LicenseCheck(
                bslLicensePath(X509_LICENSE_FILE_CERTIFICATE_FOR_GERRIT_BSL_LICENSE),
                () -> "http://wrong-hostname/",
                () -> INSTANT_WITHIN_X509_VALIDITY_PERIOD,
                errors::append)
            .pluginsWithValidLicense();
    assertThat(plugins).isEmpty();
    assertThat(errors.toString())
        .isEqualTo(X509LicenseCheck.LICENSED_BSL_PLUGINS_NOT_MATCHING_CERT_CN_MSG);
  }

  @Test
  public void certificateRejected_validIssuedByTrustAnchorOnMissingCanonicalWebUrl()
      throws IOException {
    StringBuilder errors = new StringBuilder();

    Set<String> plugins =
        new X509LicenseCheck(
                bslLicensePath(X509_LICENSE_FILE_CERTIFICATE_FOR_GERRIT_BSL_LICENSE),
                () -> null,
                () -> INSTANT_WITHIN_X509_VALIDITY_PERIOD,
                errors::append)
            .pluginsWithValidLicense();
    assertThat(plugins).isEmpty();
    assertThat(errors.toString())
        .isEqualTo(X509LicenseCheck.LICENSED_BSL_PLUGINS_EMPTY_CANONICAL_WEB_URL);
  }

  @Test
  public void certificateAccepted_twoCertsValidIssuedByTrustAnchor() throws IOException {
    StringBuilder errors = new StringBuilder();

    Set<String> plugins =
        new X509LicenseCheck(
                bslLicensePath(
                    X509_LICENSE_FILE_CERTIFICATE_FOR_GERRIT_BSL_LICENSE,
                    X509_LICENSE_FILE_CERTIFICATE_FOR_ANOTHER_PLUGIN),
                () -> "http://localhost/",
                () -> INSTANT_WITHIN_X509_VALIDITY_PERIOD,
                errors::append)
            .pluginsWithValidLicense();
    assertThat(plugins).containsExactly(BSL_PLUGIN_NAME, BSL_PLUGIN_NAME_OTHER);
    assertThat(errors.toString()).isEmpty();
  }

  @Test
  public void certificateRejected_expiredIssuedByTrustAnchor() throws IOException {
    StringBuilder errors = new StringBuilder();

    Set<String> plugins =
        new X509LicenseCheck(
                bslLicensePath(X509_LICENSE_FILE_CERTIFICATE_FOR_GERRIT_BSL_LICENSE),
                () -> "http://localhost/",
                () -> INSTANT_AFTER_X509_VALIDITY_PERIOD,
                errors::append)
            .pluginsWithValidLicense();
    assertThat(plugins).isEmpty();
    assertThat(errors.toString())
        .isEqualTo(X509LicenseCheck.LICENSED_BSL_PLUGINS_OUTSIDE_VALIDITY_PERIOD_MSG);
  }

  @Test
  public void certificateAccepted_mixOfValidAndInvalidCertificates() throws IOException {
    List<String> errors = new ArrayList<>();

    Set<String> plugins =
        new X509LicenseCheck(
                bslLicensePath(
                    X509_LICENSE_FILE_CERTIFICATE_SIGNED_BY_ANOTHER_CA,
                    X509_LICENSE_FILE_CERTIFICATE_FOR_ANOTHER_HOST,
                    X509_LICENSE_FILE_CERTIFICATE_FOR_GERRIT_BSL_LICENSE),
                () -> "http://localhost/",
                () -> INSTANT_WITHIN_X509_VALIDITY_PERIOD,
                errors::add)
            .pluginsWithValidLicense();
    assertThat(plugins).containsExactly(BSL_PLUGIN_NAME);
    assertThat(errors)
        .containsExactly(
            X509LicenseCheck.LICENSED_BSL_PLUGINS_CERT_NOT_VALID_MSG,
            X509LicenseCheck.LICENSED_BSL_PLUGINS_NOT_MATCHING_CERT_CN_MSG);
  }

  @Test
  public void certificateRejected_validIssuedByAnotherCA() throws IOException {
    StringBuilder errors = new StringBuilder();

    Set<String> plugins =
        new X509LicenseCheck(
                bslLicensePath(X509_LICENSE_FILE_CERTIFICATE_SIGNED_BY_ANOTHER_CA),
                () -> "http://localhost/",
                () -> INSTANT_WITHIN_X509_VALIDITY_PERIOD,
                errors::append)
            .pluginsWithValidLicense();
    assertThat(plugins).isEmpty();
    assertThat(errors.toString())
        .isEqualTo(X509LicenseCheck.LICENSED_BSL_PLUGINS_CERT_NOT_VALID_MSG);
  }

  private Path bslLicensePath(CharSequence... licenseCerts) throws IOException {
    return Files.writeString(tempFolder.newFile().toPath(), String.join("\n", licenseCerts));
  }
}
