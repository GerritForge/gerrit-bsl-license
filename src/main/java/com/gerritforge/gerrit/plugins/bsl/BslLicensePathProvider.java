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

import com.google.gerrit.server.config.SitePaths;
import com.google.inject.Inject;
import com.google.inject.Provider;
import java.nio.file.Path;

public class BslLicensePathProvider implements Provider<Path> {
  public static final String BSL_LICENSE_FILE = "gerritforge-bsl-license.cer";

  private final SitePaths sitePaths;

  @Inject
  BslLicensePathProvider(SitePaths sitePaths) {
    this.sitePaths = sitePaths;
  }

  @Override
  public Path get() {
    return sitePaths.etc_dir.resolve(BSL_LICENSE_FILE);
  }
}
