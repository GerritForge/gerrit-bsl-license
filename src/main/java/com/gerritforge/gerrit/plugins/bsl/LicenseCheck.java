package com.gerritforge.gerrit.plugins.bsl;

import com.google.inject.ImplementedBy;

@ImplementedBy(X509LicenseCheck.class)
public interface LicenseCheck {
  boolean isValid();
}
