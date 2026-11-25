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

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.flogger.FluentLogger;
import com.google.gerrit.server.config.CanonicalWebUrl;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
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
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateParsingException;
import java.security.cert.PKIXCertPathValidatorResult;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Singleton
class X509LicenseCheck implements LicenseCheck {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();
  private static final Pattern X509_CN_PATTERN = Pattern.compile("CN=([^,]+)");
  private static final int X509_CERT_EXT_DNS_TYPE = 2; // GeneralNameInterface.NAME_DNS value

  // All the following static constants are visible at package level for being reused in tests
  static final String BSL_LICENSE_FILE = "gerritforge-bsl-license.cer";
  static final String GERRITFORGE_BSL_ROOT_CER = "/gerritforge-bsl-root.cer";
  static final Set<String> NO_LICENSED_BSL_PLUGINS = Set.of();
  static final String NO_LICENSED_BSL_PLUGINS_MSG = "No BSL license file";
  static final String LICENSED_BSL_PLUGINS_EXPIRED_MSG =
      "[BSL License] X.509 certificate '%s' outside its validity period";
  static final String LICENSED_BSL_PLUGINS_CERT_NOT_VALID_MSG =
      "[BSL License] Unable to verify X.509 certificate license '%s'";
  static final String LICENSED_BSL_PLUGINS_NOT_MATCHING_CERT_CN_MSG =
      "[BSL License] none of X.509 Certificate common names '%s' match the canonicalWebUrl '%s'";
  static final String LICENSED_BSL_PLUGINS_UNABLE_TO_VERIFY_MSG =
      "[BSL License] Unable to verify BSL license file";
  public static final String LICENSED_BSL_PLUGINS_EMPTY_CANONICAL_WEB_URL =
      "[BSL License] canonicalWebUrl is empty in gerrit.config, no license check is possible";

  private final Path bslLicensePath;
  private final Provider<String> canonicalWebUrlProvider;
  private final Provider<Instant> nowProvider;
  private final Consumer<String> errorConsumer;

  static AbstractModule module() {
    return new AbstractModule() {
      @Override
      protected void configure() {
        bind(Instant.class).annotatedWith(InstantNow.class).toProvider(Instant::now);
        bind(Key.get(new TypeLiteral<Consumer<String>>() {}).withAnnotation(ErrorConsumer.class))
            .toInstance(e -> {});
        bind(Path.class)
            .annotatedWith(BslLicensePath.class)
            .toProvider(BslLicensePathProvider.class);
      }
    };
  }

  @Inject
  X509LicenseCheck(
      @BslLicensePath Path bslLicensePath,
      @CanonicalWebUrl Provider<String> canonicalWebUrlProvider,
      @InstantNow Provider<Instant> nowProvider,
      @ErrorConsumer Consumer<String> errorConsumer) {
    this.bslLicensePath = bslLicensePath;
    this.canonicalWebUrlProvider = canonicalWebUrlProvider;
    this.nowProvider = nowProvider;
    this.errorConsumer = errorConsumer;
  }

  @Override
  public Set<String> pluginsWithValidLicense() {
    ImmutableSet.Builder<String> licensedPluginsBuilder = ImmutableSet.builder();

    File licenseFile = bslLicensePath.toFile();
    if (!licenseFile.exists()) {
      errorConsumer.accept(NO_LICENSED_BSL_PLUGINS_MSG);
      return NO_LICENSED_BSL_PLUGINS;
    }

    String canonicalWebUrl =
        Strings.nullToEmpty(canonicalWebUrlProvider.get()).toLowerCase(Locale.ROOT).trim();

    if (canonicalWebUrl.isEmpty()) {
      errorConsumer.accept(LICENSED_BSL_PLUGINS_EMPTY_CANONICAL_WEB_URL);
      logger.atWarning().log(LICENSED_BSL_PLUGINS_EMPTY_CANONICAL_WEB_URL);
      return NO_LICENSED_BSL_PLUGINS;
    }

    try (FileInputStream bundleStream = new FileInputStream(licenseFile);
        InputStream rootCertStream =
            X509LicenseCheck.class.getResourceAsStream(GERRITFORGE_BSL_ROOT_CER)) {
      CertificateFactory certFactory = CertificateFactory.getInstance("X.509");

      List<X509Certificate> licenseCertificates =
          certFactory.generateCertificates(bundleStream).stream()
              .map(X509Certificate.class::cast)
              // Prevent hijacking a root certificate into the license file
              .filter(X509LicenseCheck::notSelfSignedCertificate)
              .toList();

      Set<TrustAnchor> trustAnchors =
          certFactory.generateCertificates(rootCertStream).stream()
              .map(X509Certificate.class::cast)
              .map(c -> new TrustAnchor(c, null))
              .collect(Collectors.toSet());

      Instant now = nowProvider.get();

      Set<String> licensedPlugins;
      for (X509Certificate cert : licenseCertificates) {
        try {
          PKIXCertPathValidatorResult unused =
              validateAgainstGerritForgeRootCertificate(certFactory, trustAnchors, cert, now);
          licensedPlugins = getSubjectAltNames(cert);
        } catch (CertPathValidatorException
            | InvalidAlgorithmParameterException
            | NoSuchAlgorithmException
            | CertificateParsingException e) {
          Throwable eCause = e.getCause();
          String errorMsg =
              (e instanceof CertPathValidatorException cve
                          && cve.getReason().equals(CertPathValidatorException.BasicReason.EXPIRED))
                      || (eCause instanceof CertPathValidatorException ccve
                          && ccve.getReason()
                              .equals(CertPathValidatorException.BasicReason.EXPIRED))
                  ? LICENSED_BSL_PLUGINS_EXPIRED_MSG
                  : LICENSED_BSL_PLUGINS_CERT_NOT_VALID_MSG;
          errorConsumer.accept(errorMsg);
          logger.atSevere().withCause(e).log(errorMsg, cert.getSubjectX500Principal().getName());
          continue;
        }

        String certSubjectDN = cert.getSubjectX500Principal().getName();
        URI canonicalWebUri = URI.create(canonicalWebUrl);

        Matcher matcher = X509_CN_PATTERN.matcher(certSubjectDN);
        boolean certMatch = false;
        while (matcher.find() && !certMatch) {
          certMatch = matcher.group(1).equalsIgnoreCase(canonicalWebUri.getHost());
        }

        if (!certMatch) {
          errorConsumer.accept(LICENSED_BSL_PLUGINS_NOT_MATCHING_CERT_CN_MSG);
          logger.atWarning().log(
              LICENSED_BSL_PLUGINS_NOT_MATCHING_CERT_CN_MSG, certSubjectDN, canonicalWebUrl);
          continue;
        }

        Date notAfter = cert.getNotAfter();
        String x509CertName = cert.getSubjectX500Principal().getName();
        logger.atFine().log(
            "[BSL License] X.509 Certificate %s has expiry date %s", x509CertName, notAfter);
        long numberOfDaysToExpire = Duration.between(now, notAfter.toInstant()).toDays();
        logger.atInfo().log(
            "[BSL License] X.509 Certificate %s for plugins %s is valid and has %d days left",
            x509CertName, licensedPlugins, numberOfDaysToExpire);
        licensedPluginsBuilder.addAll(licensedPlugins);
      }

      return licensedPluginsBuilder.build();
    } catch (IOException | CertificateException e) {
      errorConsumer.accept(LICENSED_BSL_PLUGINS_UNABLE_TO_VERIFY_MSG);
      logger.atSevere().withCause(e).log("[BSL License] Unable to verify BSL license file");
      return NO_LICENSED_BSL_PLUGINS;
    }
  }

  private static PKIXCertPathValidatorResult validateAgainstGerritForgeRootCertificate(
      CertificateFactory certFactory,
      Set<TrustAnchor> trustAnchors,
      X509Certificate licenseCertificate,
      Instant now)
      throws CertificateException,
          InvalidAlgorithmParameterException,
          CertPathValidatorException,
          NoSuchAlgorithmException {
    var params = new PKIXParameters(trustAnchors);
    params.setRevocationEnabled(false);
    params.setDate(Date.from(now));

    return (PKIXCertPathValidatorResult)
        CertPathValidator.getInstance("PKIX")
            .validate(certFactory.generateCertPath(List.of(licenseCertificate)), params);
  }

  private static boolean notSelfSignedCertificate(X509Certificate c) {
    return !c.getSubjectX500Principal().equals(c.getIssuerX500Principal());
  }

  private Set<String> getSubjectAltNames(X509Certificate cert) throws CertificateParsingException {
    Collection<List<?>> certsAlternativeNames = cert.getSubjectAlternativeNames();
    if (certsAlternativeNames == null) {
      return NO_LICENSED_BSL_PLUGINS;
    }
    return certsAlternativeNames.stream()
        .filter(ext -> ext.get(0).equals(X509_CERT_EXT_DNS_TYPE))
        .map(ext -> ext.get(1))
        .map(String.class::cast)
        .map(s -> s.toLowerCase(Locale.ROOT))
        .collect(Collectors.toSet());
  }
}
