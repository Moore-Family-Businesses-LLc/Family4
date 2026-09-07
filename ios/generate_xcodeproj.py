#!/usr/bin/env python3
"""
generate_xcodeproj.py — Generates Family4.xcodeproj from the Swift source tree.

Run on macOS with Xcode installed:
    cd ios
    python3 generate_xcodeproj.py

This uses `xcodegen` (https://github.com/yonaskolb/XcodeGen) if available,
otherwise falls back to a minimal hand-crafted project.yaml spec.
"""

import os, subprocess, sys

PROJECT_YAML = """
name: Family4
options:
  bundleIdPrefix: com.family4
  deploymentTarget:
    iOS: "16.0"
  xcodeVersion: "15"
  defaultConfig: Debug

settings:
  base:
    SWIFT_VERSION: "5.9"
    IPHONEOS_DEPLOYMENT_TARGET: "16.0"
    MARKETING_VERSION: "1.0.0"
    CURRENT_PROJECT_VERSION: "1"
    DEVELOPMENT_TEAM: "$(DEVELOPMENT_TEAM)"
    CODE_SIGN_STYLE: Automatic

configs:
  Debug:   debug
  Release: release

targets:
  Family4:
    type: application
    platform: iOS
    deploymentTarget: "16.0"
    sources:
      - path: Family4
        excludes:
          - Resources/Info.plist
    resources:
      - Family4/Resources
    info:
      path: Family4/Resources/Info.plist
      properties:
        CFBundleDisplayName: Family4
        UILaunchScreen: {}
    settings:
      base:
        PRODUCT_BUNDLE_IDENTIFIER: com.family4.app
        INFOPLIST_FILE: Family4/Resources/Info.plist
    dependencies:
      - target: Family4Widget
        embed: true

  Family4Widget:
    type: app-extension
    platform: iOS
    deploymentTarget: "16.0"
    sources:
      - Family4Widget
    settings:
      base:
        PRODUCT_BUNDLE_IDENTIFIER: com.family4.app.widget
        INFOPLIST_FILE: Family4Widget/Info.plist

  Family4Tests:
    type: bundle.unit-test
    platform: iOS
    sources:
      - Family4Tests
    settings:
      base:
        PRODUCT_BUNDLE_IDENTIFIER: com.family4.tests
    dependencies:
      - target: Family4
"""

WIDGET_PLIST = """<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>CFBundleDisplayName</key><string>Family4</string>
    <key>CFBundleExecutable</key><string>$(EXECUTABLE_NAME)</string>
    <key>CFBundleIdentifier</key><string>$(PRODUCT_BUNDLE_IDENTIFIER)</string>
    <key>CFBundleInfoDictionaryVersion</key><string>6.0</string>
    <key>CFBundleName</key><string>$(PRODUCT_NAME)</string>
    <key>CFBundlePackageType</key><string>$(PRODUCT_BUNDLE_PACKAGE_TYPE)</string>
    <key>CFBundleShortVersionString</key><string>1.0</string>
    <key>CFBundleVersion</key><string>1</string>
    <key>NSExtension</key>
    <dict>
        <key>NSExtensionPointIdentifier</key>
        <string>com.apple.widgetkit-extension</string>
    </dict>
</dict>
</plist>"""

def main():
    os.chdir(os.path.dirname(os.path.abspath(__file__)))

    # Write project.yml for XcodeGen
    with open("project.yml", "w") as f:
        f.write(PROJECT_YAML)
    print("✓ project.yml written")

    # Write widget Info.plist
    os.makedirs("Family4Widget", exist_ok=True)
    with open("Family4Widget/Info.plist", "w") as f:
        f.write(WIDGET_PLIST)
    print("✓ Family4Widget/Info.plist written")

    # Try XcodeGen
    result = subprocess.run(["which", "xcodegen"], capture_output=True)
    if result.returncode == 0:
        print("✓ XcodeGen found — generating project…")
        subprocess.run(["xcodegen", "generate"], check=True)
        print("✓ Family4.xcodeproj generated!")
        print("\nNext steps:")
        print("  open Family4.xcodeproj")
        print("  Select your team in Signing & Capabilities")
        print("  Connect your iPhone and Run (⌘R)")
    else:
        print("⚠  XcodeGen not found.")
        print("   Install with: brew install xcodegen")
        print("   Then run:     python3 generate_xcodeproj.py")
        print("\n   Alternatively, install directly:")
        print("   https://github.com/yonaskolb/XcodeGen/releases")
        sys.exit(1)

if __name__ == "__main__":
    main()
