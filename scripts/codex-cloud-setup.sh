#!/usr/bin/env bash
set -euo pipefail

GRADLE_VERSION="9.4.1"
ANDROID_API="36"
ANDROID_BUILD_TOOLS="36.0.0"
ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-$HOME/android-sdk}"
GRADLE_HOME="${GRADLE_HOME:-$HOME/.local/gradle-$GRADLE_VERSION}"

install_system_packages() {
  if command -v javac >/dev/null && command -v curl >/dev/null && command -v unzip >/dev/null; then
    return
  fi

  if command -v sudo >/dev/null; then
    sudo apt-get update
    sudo DEBIAN_FRONTEND=noninteractive apt-get install -y openjdk-17-jdk curl unzip python3
  elif [ "$(id -u)" = "0" ]; then
    apt-get update
    DEBIAN_FRONTEND=noninteractive apt-get install -y openjdk-17-jdk curl unzip python3
  else
    echo "JDK 17, curl, unzip e Python 3 são necessários, mas não há permissão para instalá-los." >&2
    exit 1
  fi
}

persist_environment() {
  local java_home
  java_home="$(dirname "$(dirname "$(readlink -f "$(command -v javac)")")")"

  export JAVA_HOME="$java_home"
  export ANDROID_HOME="$ANDROID_SDK_ROOT"
  export ANDROID_SDK_ROOT
  export GRADLE_HOME
  export PATH="$GRADLE_HOME/bin:$ANDROID_SDK_ROOT/platform-tools:$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$PATH"

  local marker="# Nexo Driver Codex Cloud"
  if ! grep -Fq "$marker" "$HOME/.bashrc" 2>/dev/null; then
    {
      echo "$marker"
      echo "export JAVA_HOME=\"$JAVA_HOME\""
      echo "export ANDROID_HOME=\"$ANDROID_SDK_ROOT\""
      echo "export ANDROID_SDK_ROOT=\"$ANDROID_SDK_ROOT\""
      echo "export GRADLE_HOME=\"$GRADLE_HOME\""
      echo 'export PATH="$GRADLE_HOME/bin:$ANDROID_SDK_ROOT/platform-tools:$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$PATH"'
    } >> "$HOME/.bashrc"
  fi
}

install_gradle() {
  if [ -x "$GRADLE_HOME/bin/gradle" ]; then
    return
  fi

  local archive
  archive="$(mktemp --suffix=.zip)"
  curl -fsSL "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip" -o "$archive"
  mkdir -p "$(dirname "$GRADLE_HOME")"
  unzip -q "$archive" -d "$(dirname "$GRADLE_HOME")"
}

resolve_android_tools_url() {
  local repository_xml="$1"
  python3 - "$repository_xml" <<'PY'
import sys
import xml.etree.ElementTree as ET

root = ET.parse(sys.argv[1]).getroot()

def children_with_suffix(node, suffix):
    return [child for child in node.iter() if child.tag.endswith(suffix)]

for package in children_with_suffix(root, "remotePackage"):
    if package.attrib.get("path") != "cmdline-tools;latest":
        continue
    for archive in children_with_suffix(package, "archive"):
        hosts = children_with_suffix(archive, "host-os")
        if not hosts or (hosts[0].text or "").strip() != "linux":
            continue
        urls = children_with_suffix(archive, "url")
        if urls and urls[0].text:
            print("https://dl.google.com/android/repository/" + urls[0].text.strip())
            raise SystemExit(0)

raise SystemExit("Não foi possível localizar Android command-line tools para Linux.")
PY
}

install_android_sdk() {
  local sdkmanager="$ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager"
  if [ ! -x "$sdkmanager" ]; then
    local repository_xml archive unpack_dir tools_url
    repository_xml="$(mktemp --suffix=.xml)"
    archive="$(mktemp --suffix=.zip)"
    unpack_dir="$(mktemp -d)"

    curl -fsSL "https://dl.google.com/android/repository/repository2-1.xml" -o "$repository_xml"
    tools_url="$(resolve_android_tools_url "$repository_xml")"
    curl -fsSL "$tools_url" -o "$archive"
    unzip -q "$archive" -d "$unpack_dir"
    mkdir -p "$ANDROID_SDK_ROOT/cmdline-tools"
    mv "$unpack_dir/cmdline-tools" "$ANDROID_SDK_ROOT/cmdline-tools/latest"
  fi

  yes | "$sdkmanager" --licenses >/dev/null || true
  "$sdkmanager" \
    "platform-tools" \
    "platforms;android-$ANDROID_API" \
    "build-tools;$ANDROID_BUILD_TOOLS"
}

validate_workspace() {
  npm install --ignore-scripts
  npm test
  npm run validate:fixtures
  gradle -p android tasks --no-daemon >/dev/null
}

install_system_packages
persist_environment
install_gradle
install_android_sdk
validate_workspace

echo "Ambiente Nexo Driver pronto: Node $(node --version), Java $(javac -version 2>&1), Gradle $GRADLE_VERSION e Android API $ANDROID_API."
