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
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.PKIXCertPathValidatorResult;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Singleton
public class X509LicenseCheck implements LicenseCheck {
  public static final String BSL_LICENSE_FILE = "gerritforge-bsl-license.cer";
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();
  private static final String GERRIT_FORGE_BSL_CA_ROOT_1 =
      "CN=GerritForge BSL CA Root1,OU=GerritForge CA,O=GerritForge"
          + " Inc.,L=Sunnyvale,ST=California,C=US";
  private static final Pattern X509_CN_PATTERN = Pattern.compile("CN=([^,]+)");
  public static final String GERRITFORGE_BSL_ROOT_CER = "/gerritforge-bsl-root.cer";

  private final Path bslLicensePath;
  private final Provider<String> canonicalWebUrlProvider;

  @Inject
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

    Optional<String> canonicalWebUrl =
        Optional.ofNullable(canonicalWebUrlProvider.get()).map(url -> url.toLowerCase(Locale.ROOT));

    if (canonicalWebUrl.isEmpty()) {
      logger.atWarning().log(
          "[BSL License] canonicalWebUrl is empty in gerrit.config, no license check is possible");
      return false;
    }

    try (FileInputStream bundleStream = new FileInputStream(licenseFile);
        InputStream rootCertStream =
            X509LicenseCheck.class.getResourceAsStream(GERRITFORGE_BSL_ROOT_CER)) {
      CertificateFactory certFactory = CertificateFactory.getInstance("X.509");

      List<X509Certificate> licenseCertificates =
          certFactory.generateCertificates(bundleStream).stream()
              .map(X509Certificate.class::cast)
              .filter(c -> c.getSubjectX500Principal().equals(c.getIssuerX500Principal()))
              .toList();

      Set<TrustAnchor> trustAnchors =
          certFactory.generateCertificates(rootCertStream).stream()
              .map(X509Certificate.class::cast)
              .map(c -> new TrustAnchor(c, null))
              .collect(Collectors.toSet());

      var params = new PKIXParameters(trustAnchors);
      params.setRevocationEnabled(false);

      PKIXCertPathValidatorResult validationResult =
          (PKIXCertPathValidatorResult)
              CertPathValidator.getInstance("PKIX")
                  .validate(certFactory.generateCertPath(licenseCertificates), params);
      if (!isGerritForgeIncRootCert(validationResult.getTrustAnchor().getTrustedCert())) {
        return false;
      }

      for (X509Certificate cert : licenseCertificates) {
        try {
          cert.checkValidity();
        } catch (CertificateExpiredException | CertificateNotYetValidException e) {
          logger.atSevere().withCause(e).log(
              "[BSL License] X.509 certificate '%s' outside its validity period",
              cert.getSubjectX500Principal().getName());
          continue;
        }

        String certSubjectDN = cert.getSubjectX500Principal().getName();

        Optional<URI> canonicalWebUri = canonicalWebUrl.map(URI::create);

        Matcher matcher = X509_CN_PATTERN.matcher(certSubjectDN);
        boolean certMatch = false;
        while (matcher.find() && !certMatch) {
          certMatch = matcher.group(1).equalsIgnoreCase(canonicalWebUri.get().getHost());
        }

        if (!certMatch) {
          logger.atWarning().log(
              "[BSL License] none of X.509 Certificate common names '%s' match the canonical web URL '%s'",
              certSubjectDN, canonicalWebUrl);
          continue;
        }

        Date notAfter = cert.getNotAfter();
        String x509CertName = cert.getSubjectX500Principal().getName();
        logger.atFine().log(
            "[BSL License] X.509 Certificate %s has expiry date %s", x509CertName, notAfter);
        long numberOfDaysToExpire = Duration.between(Instant.now(), notAfter.toInstant()).toDays();
        if (numberOfDaysToExpire >= 0) {
          logger.atInfo().log(
              "[BSL License] X.509 Certificate %s is valid and has %d days left",
              x509CertName, numberOfDaysToExpire);
          return true;
        }

        logger.atWarning().log("[BSL License] X.509 Certificate for %s **EXPIRED**", x509CertName);
      }
      return false;
    } catch (IOException e) {
      logger.atSevere().withCause(e).log("[BSL License] Unable to verify BSL license file");
      return false;
    } catch (CertPathValidatorException
        | CertificateException
        | InvalidAlgorithmParameterException
        | NoSuchAlgorithmException e) {
      logger.atSevere().withCause(e).log(
          "[BSL License] Unable to verify X.509 certificate license");
      return false;
    }
  }

  private boolean isGerritForgeIncRootCert(X509Certificate cert) {
    return cert.getSubjectX500Principal().getName().contains(GERRIT_FORGE_BSL_CA_ROOT_1);
  }
}
