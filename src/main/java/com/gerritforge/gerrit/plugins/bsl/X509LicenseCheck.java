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
import com.google.gerrit.server.config.CanonicalWebUrl;
import com.google.gerrit.server.config.SitePaths;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Locale;

@Singleton
public class X509LicenseCheck implements LicenseCheck {
  public static final String BSL_LICENSE_FILE = "gerritforge-bsl-license.cer";
  private static final Integer DNS_TYPE =
      2; // The GeneralNameInterface.NAME_DNS value, inaccessible because of being an internal
         // package
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final Path bslLicensePath;
  private final Provider<String> canonicalWebUrlProvider;

  X509LicenseCheck(SitePaths sitePath, @CanonicalWebUrl Provider<String> canonicalWebUrlProvider) {
    this.bslLicensePath = sitePath.etc_dir.resolve(BSL_LICENSE_FILE);
    this.canonicalWebUrlProvider = canonicalWebUrlProvider;
  }

  @Override
  public boolean isValid() {
    File licenseFile = bslLicensePath.toFile();
    if (!licenseFile.exists()) {
      return false;
    }

    String canonicalWebUrl = canonicalWebUrlProvider.get().toLowerCase(Locale.ROOT);

    try (FileInputStream certStream = new FileInputStream(licenseFile)) {
      CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
      X509Certificate cert = (X509Certificate) certFactory.generateCertificate(certStream);

      List<String> certsAlternativeNames =
          (List<String>)
              cert.getSubjectAlternativeNames().stream()
                  .filter(names -> names.get(0).equals(DNS_TYPE))
                  .map(names -> names.get(1))
                  .toList();

      boolean certMatch = certsAlternativeNames.stream().anyMatch(canonicalWebUrl::contains);
      if (!certMatch) {
        logger.atWarning().log(
            "BSL: none of X.509 Certificate DNS names %s match the canonical web URL %s",
            certsAlternativeNames, canonicalWebUrl);
        return false;
      }

      Date notAfter = cert.getNotAfter();
      String x509CertName = cert.getSubjectX500Principal().getName();
      logger.atFine().log("BSL: X.509 Certificate %s has expiry date %s", x509CertName, notAfter);
      long numberOfDaysToExpire = Duration.between(Instant.now(), notAfter.toInstant()).toDays();
      if (numberOfDaysToExpire >= 0) {
        logger.atInfo().log(
            "BSL: X.509 Certificate %s is valid and has %d days left",
            x509CertName, numberOfDaysToExpire);
        return true;
      } else {
        logger.atWarning().log("BSL: X.509 Certificate for %s **EXPIRED**", x509CertName);
        return false;
      }

    } catch (IOException e) {
      logger.atSevere().withCause(e).log("Unable to verify BSL license");
      return false;
    } catch (CertificateException e) {
      logger.atSevere().withCause(e).log("Unable to verify BSL X.509 Certificate license");
      return false;
    }
  }
}
