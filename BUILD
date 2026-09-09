package(default_visibility = ["//visibility:public"])

load("//tools/bzl:junit.bzl", "junit_tests")
load(
    "//tools/bzl:plugin.bzl",
    "PLUGIN_DEPS",
    "PLUGIN_TEST_DEPS",
    "gerrit_plugin",
)
load("//tools/bzl:javadoc.bzl", "java_doc")

gerrit_plugin(
    name = "gerrit-bsl-license",
    srcs = glob(["src/main/java/**/*.java"]),
    deps = [],
)

junit_tests(
    name = "gerrit-bsl-license_tests",
    size = "small",
    srcs = glob(["src/test/java/**/*.java"]),
    tags = ["gerrit-bsl-license"],
    deps = PLUGIN_DEPS + PLUGIN_TEST_DEPS + [
        ":gerrit-bsl-license__plugin",
    ],
)

java_doc(
    name = "gerrit-bsl-license-javadoc",
    libs = [":gerrit-bsl-license__plugin"],
    pkgs = ["com.gerritforge"],
    title = "Gerrit BSL License",
)
