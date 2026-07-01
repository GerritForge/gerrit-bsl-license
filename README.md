# Gerrit BSL Licensing Awareness Plugin

In September 2025, GerritForge transitioned select Gerrit Code Review plugins to a
[Business Source License (BSL) policy](https://gitenterprise.me/2025/09/30/re-licensing-gerritforge-plugins-welcome-to-gerrit-enterprise/).

This project serves as a foundational library to support that transition.
It equips GerritForge's BSL-licensed plugins with the ability to automatically redirect Gerrit
administrators to a dedicated licensing page.

This ensures that admins maintain full visibility and awareness when their Gerrit ecosystem
includes plugins governed by the BSL.

## ⚖️  License

This project is licensed under the **Business Source License 1.1** (BSL 1.1).

BSL is a "source-available" license that balances the collaborative benefits of
 open-source access with temporary commercial restrictions.

* **License Details:** The complete text of the BSL 1.1 can be found in the `LICENSE` file at
  the root of this repository.

* **Commercial Support:** If your intended use case exceeds the scope of the **Additional Use Grant**
  and you require a commercial license, please reach out to [GerritForge Sales](https://gerritforge.com/contact).

## 🛠️ How to Build

This plugin must be built from within the Gerrit Code Review source tree.

### Prerequisites

Ensure that this repository's directory is linked or cloned into the `plugins/` directory of your Gerrit source tree.

### Build Steps

1. Navigate to the root of your Gerrit source tree.
2. Run the following Bazelisk command:

```bash
bazelisk build //plugins/gerrit-bsl-license
```

### Artifact Location

Upon a successful build, the compiled .jar artifact will be available at:

```
bazel-bin/plugins/gerrit-bsl-license/gerrit-bsl-license.jar
```
