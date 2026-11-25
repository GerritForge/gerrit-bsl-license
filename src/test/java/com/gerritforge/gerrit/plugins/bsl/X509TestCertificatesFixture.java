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

import java.time.Instant;
import org.junit.Ignore;

@Ignore // Do not pickup this class as running test
public final class X509TestCertificatesFixture {

  static final String BSL_PLUGIN_NAME = "gerrit-bsl-license";

  static final String BSL_PLUGIN_NAME_OTHER = "other-licensed-plugin";

  static final CharSequence X509_LICENSE_FILE_CERTIFICATE_FOR_GERRIT_BSL_LICENSE =
      """
      -----BEGIN CERTIFICATE-----
      MIIEIDCCAwigAwIBAgICE8owDQYJKoZIhvcNAQELBQAwgbIxCzAJBgNVBAYTAlVT
      MRMwEQYDVQQIDApDYWxpZm9ybmlhMRIwEAYDVQQHDAlTdW5ueXZhbGUxGTAXBgNV
      BAoMEEdlcnJpdEZvcmdlIEluYy4xFzAVBgNVBAsMDkdlcnJpdEZvcmdlIENBMSEw
      HwYDVQQDDBhHZXJyaXRGb3JnZSBCU0wgQ0EgUm9vdDExIzAhBgkqhkiG9w0BCQEW
      FGluZm9AZ2Vycml0Zm9yZ2UuY29tMB4XDTI2MTEyNTIwMTUxN1oXDTI2MTIwNTIw
      MTUxN1owgbcxCzAJBgNVBAYTAlVTMRMwEQYDVQQIDApDYWxpZm9ybmlhMRkwFwYD
      VQQKDBBHZXJyaXRGb3JnZSBJbmMuMTswOQYDVQQLDDJHZXJyaXQgRW50ZXJwcmlz
      ZSBCU0wgTGljZW5zZSBmb3IgR2Vycml0Rm9yZ2UgMjAyNzESMBAGA1UEAwwJbG9j
      YWxob3N0MScwJQYJKoZIhvcNAQkBFhhsdWNhLm1pbGFuZXNpb0BnbWFpbC5jb20w
      ggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDJ0kD8O1r91rxe0E1Bvxti
      RKQBiyg/a9QFIfh1uhlSKk0YOET65pX/uWkKhngQ/tkkGSEHuZ40+A5eBR99pIKE
      y9gVUr4lrZ3hfXVu1+Kb9FlUGAzTOjQz4vQVJsdVW26e6Hi1gv/gVnIur/NXmP+i
      u5Vd1WpfZIGeCiCpm2hoK9W/FU+GHHoSp2j0N668tALWnvTKMTVPhFVXjZ4S/ehx
      qaLFCC4n3ZQ2llm7ovtNEAje3zAl888WHVWvQZGzaTlD+xVPW90N/s++EtIK0hWz
      1GzdZsCJslwUpQ9Gop+poiRcN/6r4mCc02hl3BhvYl3yhjKC96ul5pogk7k2nynt
      AgMBAAGjOTA3MAkGA1UdEwQCMAAwCwYDVR0PBAQDAgWgMB0GA1UdEQQWMBSCEmdl
      cnJpdC1ic2wtbGljZW5zZTANBgkqhkiG9w0BAQsFAAOCAQEAWkM8VlUPqR/R5LxA
      UQIyjgtUBua580UDS3odlciIBGW5tD4i+3J1Zwhm144G5d6Euz7wi/uEd+NoT6RV
      pkaQdEvhCzjjZj4iMAU+RgeZoOtRUttODBhBCp7PxcDYIJwpVx6pDTgAUTp54BBr
      MWWUITcbxs+lWgT7r8UuM8LkRipBF39vzAJ+OHNRveGNNd0q+Ufnq2oKZPlZk4tJ
      LcH8m/M09dedFBubXdJDnbj9a3vao9doUerYsMojS5TOL+r3sC0iJg+7R+Dd3taf
      GLf/w7GFLY8HLeNNGc4TNVihf9DlPNa01icRsNLZNf+dWS1A9Fx+4d65U74tka3s
      NTtYDw==
      -----END CERTIFICATE-----
      """;

  static final CharSequence X509_LICENSE_FILE_CERTIFICATE_FOR_ANOTHER_HOST =
      """
      -----BEGIN CERTIFICATE-----
      MIIEIjCCAwqgAwIBAgICE8wwDQYJKoZIhvcNAQELBQAwgbIxCzAJBgNVBAYTAlVT
      MRMwEQYDVQQIDApDYWxpZm9ybmlhMRIwEAYDVQQHDAlTdW5ueXZhbGUxGTAXBgNV
      BAoMEEdlcnJpdEZvcmdlIEluYy4xFzAVBgNVBAsMDkdlcnJpdEZvcmdlIENBMSEw
      HwYDVQQDDBhHZXJyaXRGb3JnZSBCU0wgQ0EgUm9vdDExIzAhBgkqhkiG9w0BCQEW
      FGluZm9AZ2Vycml0Zm9yZ2UuY29tMB4XDTI2MTEyNTIxNTAxMVoXDTI2MTIwNTIx
      NTAxMVowgbkxCzAJBgNVBAYTAlVTMRMwEQYDVQQIDApDYWxpZm9ybmlhMRkwFwYD
      VQQKDBBHZXJyaXRGb3JnZSBJbmMuMTswOQYDVQQLDDJHZXJyaXQgRW50ZXJwcmlz
      ZSBCU0wgTGljZW5zZSBmb3IgR2Vycml0Rm9yZ2UgMjAyNzEUMBIGA1UEAwwLYW5v
      dGhlcmhvc3QxJzAlBgkqhkiG9w0BCQEWGGx1Y2EubWlsYW5lc2lvQGdtYWlsLmNv
      bTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBALJLkIQuERW6ZN/s/bve
      NRn1NnMJ/DlnFs7xliugEQFTPylB5jd9c4Lf+BcB/OcSW25VFpOWdb1WVfR13xiN
      tDOCjhYoci2WxCdBVQiwEZg8J/hkj7tN/5hMkXnjIKFyijKfakDqPRsSv18O16cn
      zipdc7w4kCTDnMJeeZpEr9g/0wMCEkcs0mNZ54uTvoLoQbKfXI/Yq4+DreKOOJlP
      DWDqIdISFHNxf1RS9+OsJWBQyh6G638JnVGROtG2y4HHUE0GVHaYrRdXdAYl3bvQ
      VEj+AuKzDssKkybpkOykzl5MuIVKJexYl85UT7bSzEhnHpLbglK+V6OjOivZPBeG
      T6MCAwEAAaM5MDcwCQYDVR0TBAIwADALBgNVHQ8EBAMCBaAwHQYDVR0RBBYwFIIS
      Z2Vycml0LWJzbC1saWNlbnNlMA0GCSqGSIb3DQEBCwUAA4IBAQBdMlPZMnfNSZmu
      C7SrXPctxj8hcSmwvGFf03V2Cj5N6JSCm7FkUzclcWO5620nu5V6/TGdgRmW7u40
      eFqqrT5c5Sz/k7cNtpGyDqBnUpAynzm3z+A196O2r+/qPZanVq9zYiGaGPRZnM66
      Vi0NqxvY/9mP/EYk9rf4pcSETYjgp7syphDHNpXs7DyhUC1XabN6jbWkZhrs+H8C
      tal1cYeVky10p0eRpmQgTSvoY4QPwfskHaQ7/GnTGRIwy9PG8HqaFK3+CbrfhSCv
      RAAO63CW/3OcmveBZwSFrjzpW3wKvKZWnSLv0CoHA9rI6JvHzbHtUz6huHTfOEPq
      tfuDnB30
      -----END CERTIFICATE-----
      """;

  static final CharSequence X509_LICENSE_FILE_CERTIFICATE_FOR_ANOTHER_PLUGIN =
      """
      -----BEGIN CERTIFICATE-----
      MIIEIzCCAwugAwIBAgICE8swDQYJKoZIhvcNAQELBQAwgbIxCzAJBgNVBAYTAlVT
      MRMwEQYDVQQIDApDYWxpZm9ybmlhMRIwEAYDVQQHDAlTdW5ueXZhbGUxGTAXBgNV
      BAoMEEdlcnJpdEZvcmdlIEluYy4xFzAVBgNVBAsMDkdlcnJpdEZvcmdlIENBMSEw
      HwYDVQQDDBhHZXJyaXRGb3JnZSBCU0wgQ0EgUm9vdDExIzAhBgkqhkiG9w0BCQEW
      FGluZm9AZ2Vycml0Zm9yZ2UuY29tMB4XDTI2MTEyNTIxNDYyMFoXDTI2MTIwNTIx
      NDYyMFowgbcxCzAJBgNVBAYTAlVTMRMwEQYDVQQIDApDYWxpZm9ybmlhMRkwFwYD
      VQQKDBBHZXJyaXRGb3JnZSBJbmMuMTswOQYDVQQLDDJHZXJyaXQgRW50ZXJwcmlz
      ZSBCU0wgTGljZW5zZSBmb3IgR2Vycml0Rm9yZ2UgMjAyNzESMBAGA1UEAwwJbG9j
      YWxob3N0MScwJQYJKoZIhvcNAQkBFhhsdWNhLm1pbGFuZXNpb0BnbWFpbC5jb20w
      ggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQC1wov5XJ6NiXrInKDXlQ/K
      jNsrGtjKYyxRWqtEq00nSq6sVZUW658sLrSFY5ZvIeviH9nDrmxcdWiu3npXr7gR
      gghqpSTg+tGesTFpgU3mWaoZg54AfQzqaImYzqLlS2YXL7v1OKqQMrF6SoBcW3fe
      QTMH9Ltrb5+AaB71b1E7G/TzuMCaborNS3GwBdolVh8g7qDCR90ckKCgmOOysqMc
      rKvmieXJHce6xF6AClrYWFgC8m83w578mEK8+5NaH88JMtOWUcuSWRcyBtKTRbRz
      aBQPB93r8OMKa7TdW+b+w0XfPaEPdU+IgsCZOesC/AX+BRtuyDhIpvkAtx+zQtP7
      AgMBAAGjPDA6MAkGA1UdEwQCMAAwCwYDVR0PBAQDAgWgMCAGA1UdEQQZMBeCFW90
      aGVyLWxpY2Vuc2VkLXBsdWdpbjANBgkqhkiG9w0BAQsFAAOCAQEAUTZ38b+y1Iim
      SrJUK/qo8otteTItvxUeoeMHFaOkN0CCSunkQKtVyyhjBKcEmWJvM92CWT4vdasI
      np8H9lDznuXtaljkC3lHmNMrerCekcjDEfA2ZMRcbKIEm6UzLWkwQsQSUvSGA5o4
      RfCG6EX5ug7MIbmlb0gkd9LovewUBPSsnWoEuQPlFRUpCSh8kMlrgM80AtwqOtVj
      tCUCp3q01fF6hD5L6i0IzicbhpEbvDZG3Wl70jLTDtD3UCG9VZQ0B8QNA3HfxCSZ
      qCcvlVgdyerH6FEwuzmrj2QbAagp2lQ36zbtTyw+4nPA+HfW59T97xkjq1QbmplU
      JO0r32H2ag==
      -----END CERTIFICATE-----
      """;

  static final CharSequence X509_LICENSE_FILE_CERTIFICATE_SIGNED_BY_ANOTHER_CA =
      """
      -----BEGIN CERTIFICATE-----
      MIIFMDCCAxigAwIBAgICE8AwDQYJKoZIhvcNAQELBQAwgY0xCzAJBgNVBAYTAkdC
      MQ8wDQYDVQQIDAZMb25kb24xDzANBgNVBAcMBkxvbmRvbjEYMBYGA1UECgwPR2Vy
      cml0Rm9yZ2UgTHRkMRwwGgYDVQQDDBNHZXJyaXRGb3JnZSBSb290IENBMSQwIgYJ
      KoZIhvcNAQkBFhVhZG1pbkBnZXJyaXRmb3JnZS5jb20wHhcNMjYxMTI1MjE1NzQ1
      WhcNMjYxMjA1MjE1NzQ1WjCBqjELMAkGA1UEBhMCR0IxDzANBgNVBAgMBkxvbmRv
      bjEYMBYGA1UECgwPR2Vycml0Rm9yZ2UgTHRkMTcwNQYDVQQLDC5HZXJyaXQgRW50
      ZXJwcmlzZSBTdXBwb3J0IGZvciBHZXJyaXRGb3JnZSAyMDI2MRIwEAYDVQQDDAls
      b2NhbGhvc3QxIzAhBgkqhkiG9w0BCQEWFGluZm9AZ2Vycml0Zm9yZ2UuY29tMIIB
      IjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA57mMNQcHsjN5yQDjvbnpeyuB
      CUYv+0eEyA/kahhqje9RPFCavWFwf/jCRupR75YntkAbJkPPVbzD4MggM7nuGDbt
      RESQ00zppt6IVsJfh2BJiqZa1gn3t1wblx8iFp/+PkU2XXwvaIGKvxLPjCwcb0yS
      Hdev1fHpmc8nUgJ5U3aO8bXjgwqIiNlmjY5qrzXpcs4bhDTD+eSmNYyOZYOLoGRI
      wVVE74LK++fuk+2tkQfAEPisYXWVApoKFmkQbixbdtUikMQq5UGgFaWHc7BDM8iq
      J5RppkK8BtEyn/iOGx6bokdtwfGAs4/mv0U/gYGl7SPeg9aYeJqj3tpSOei5mwID
      AQABo3sweTAJBgNVHRMEAjAAMCwGCWCGSAGG+EIBDQQfFh1PcGVuU1NMIEdlbmVy
      YXRlZCBDZXJ0aWZpY2F0ZTAdBgNVHQ4EFgQUNS/mzFMTdVKiM24zAW5xvKx35Egw
      HwYDVR0jBBgwFoAUUMlpslR4JRAmbimJRb0QXU8lPvIwDQYJKoZIhvcNAQELBQAD
      ggIBAD3JNXtJrr+/f/mkRR4FVsg+fIZtvQ3L+PDfAp4Lgk+0/THNwf8aJtv2PfEJ
      uI2pdJXnSFDu+3TGe+G2Yce+wceQ0yeCCrgfYBMSgW8NeCAkhgMP4cX3XNrhNjh0
      DPS7RurkTE6O3HmD1sujmgDQkZq8kGU2qjzVHGVNTOda4oXx0gyJMMoNnmCS6TZA
      KC15zWVbym9sLwS49fRBrxq1pv1e9NC3+I3cIj/E3ssHwqYo8dRqa+QlH2mEtwSz
      T3q3oCZgTtkSw/ntSVwrHiYSLHJ+Pka+tyYZ476h1UvcyRDXqm+e9pkjfmQI0dIn
      8NUUJDPK0lMDHuBGCQZVMfRWXCzDxlyVVFsLedIHawH2kTcqM3F8/5o/95cr+pk6
      hentz9dZSQ/s5WV1PA2fz6z7PNR9PTA2pIU/ycifc04vVzth0XQzfBcRoSKDOMBL
      2RW4QnM2W6ZGpEJQSMeHcLV3D45m21Qk0fqwomC3/PXFKzqa1hhafuQQgT+ejGpz
      O+6zwg02nMmt01dpoZwLmyXgeqmc27Dkr+F32kYsKYFMz8KsHaLSKu44QAC+Dv7V
      YTvXUmvRKtaHVdrBanRaEec6l1Rlc2jb2tyIB6PB40j/ze1x5l3km3DQ6mAh8AMH
      1t9Cq/sY+Y/lBd7WexRdVziYbsgzcsCCFtS4OsD+0PWLVQ6j
      -----END CERTIFICATE-----
      """;

  static final Instant INSTANT_WITHIN_X509_VALIDITY_PERIOD = Instant.parse("2026-11-26T00:00:00Z");
  static final Instant INSTANT_AFTER_X509_VALIDITY_PERIOD = Instant.parse("2027-11-25T00:00:00Z");
}
