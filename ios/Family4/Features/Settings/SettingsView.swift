import SwiftUI

struct SettingsView: View {
    @EnvironmentObject var store: AppStore
    @AppStorage("temperatureUnit") private var tempUnit: String = "F"
    @AppStorage("walkieChannel") private var walkieChannel: Int = 1
    @State private var showSignOutAlert = false
    @State private var showPinSheet = false

    var body: some View {
        NavigationStack {
            ZStack {
                Color.bgPrimary.ignoresSafeArea()
                ScrollView {
                    VStack(spacing: 0) {
                        SectionHeaderView(title: "Settings")

                        // ── APPEARANCE ──────────────────────────────────────
                        SettingsSection(label: "APPEARANCE") {
                            SettingsToggleRow(icon: "moon.fill", label: "Dark Mode",
                                             subtitle: "Always on — Family4 is dark-first",
                                             isOn: .constant(true))
                            .disabled(true)

                            SettingsPickerRow(icon: "thermometer", label: "Temperature Unit",
                                             value: tempUnit == "F" ? "Fahrenheit (°F)" : "Celsius (°C)") {
                                tempUnit = tempUnit == "F" ? "C" : "F"
                            }
                        }

                        // ── NOTIFICATIONS ────────────────────────────────────
                        SettingsSection(label: "NOTIFICATIONS") {
                            SettingsToggleRow(icon: "bell.fill", label: "Push Notifications",
                                             subtitle: "Family alerts, messages, reminders",
                                             isOn: $store.settings.notificationsEnabled)

                            SettingsToggleRow(icon: "mic.fill", label: "Voice Activation",
                                             subtitle: "Say \"Family Four\" to wake the app",
                                             isOn: $store.settings.voiceActivationEnabled)
                            .onChange(of: store.settings.voiceActivationEnabled) { on in
                                if on { VoiceActivationManager.shared.start() }
                                else  { VoiceActivationManager.shared.stop()  }
                            }
                        }

                        // ── PRIVACY & SECURITY ───────────────────────────────
                        SettingsSection(label: "PRIVACY & SECURITY") {
                            SettingsToggleRow(icon: "faceid", label: "Biometric Lock",
                                             subtitle: "Face ID / Touch ID on open",
                                             isOn: $store.settings.biometricEnabled)

                            SettingsToggleRow(icon: "location.fill", label: "Location Sharing",
                                             subtitle: "Share your location with family",
                                             isOn: $store.settings.locationSharingEnabled)

                            SettingsToggleRow(icon: "exclamationmark.circle.fill", label: "SOS Auto-Call",
                                             subtitle: "Auto-call emergency services",
                                             isOn: $store.settings.sosAutoCallEnabled)

                            SettingsButtonRow(icon: "lock.fill", label: "App PIN", subtitle: "Set a PIN to lock the app") {
                                showPinSheet = true
                            }
                        }

                        // ── WALKIE-TALKIE ────────────────────────────────────
                        SettingsSection(label: "WALKIE-TALKIE") {
                            HStack {
                                Image(systemName: "radio.fill")
                                    .foregroundColor(.accentCyan)
                                    .frame(width: 32)
                                VStack(alignment: .leading, spacing: 2) {
                                    Text("Channel").font(.system(size: 15)).foregroundColor(.textPrimary)
                                    Text("1–99").font(.system(size: 12)).foregroundColor(.textMuted)
                                }
                                Spacer()
                                HStack(spacing: 12) {
                                    Button { if walkieChannel > 1 { walkieChannel -= 1 } } label: {
                                        Image(systemName: "minus.circle").foregroundColor(.accentCyan)
                                    }
                                    Text("\(walkieChannel)")
                                        .font(.system(size: 16, weight: .bold, design: .monospaced))
                                        .foregroundColor(.textPrimary)
                                        .frame(minWidth: 28)
                                    Button { if walkieChannel < 99 { walkieChannel += 1 } } label: {
                                        Image(systemName: "plus.circle").foregroundColor(.accentCyan)
                                    }
                                }
                            }
                            .padding(.horizontal, 16)
                            .padding(.vertical, 14)
                            .background(Color.bgSurface)
                        }

                        // ── CLOUD BACKUP ─────────────────────────────────────
                        SettingsSection(label: "CLOUD BACKUP") {
                            SettingsToggleRow(icon: "icloud.fill", label: "Drive Backup",
                                             subtitle: "Auto-backup to iCloud",
                                             isOn: $store.settings.driveBackupEnabled)
                        }

                        // ── DATA ─────────────────────────────────────────────
                        SettingsSection(label: "DATA") {
                            SettingsButtonRow(icon: "trash.fill", label: "Clear Chat History",
                                             subtitle: "Permanently delete all messages",
                                             isDestructive: true) {
                                store.allMessages.removeAll()
                            }
                        }

                        // ── ABOUT ─────────────────────────────────────────────
                        SettingsSection(label: "ABOUT") {
                            VStack(spacing: 12) {
                                Image(systemName: "f.circle.fill")
                                    .font(.system(size: 48))
                                    .foregroundColor(.accentCyan)
                                Text("Family4")
                                    .font(.system(size: 20, weight: .black))
                                    .foregroundColor(.textPrimary)
                                Text("Version 1.0.0 (iOS)")
                                    .font(.system(size: 12))
                                    .foregroundColor(.textMuted)
                                Text("Connecting families, everywhere.")
                                    .font(.system(size: 13))
                                    .foregroundColor(.accentCyan)
                            }
                            .frame(maxWidth: .infinity)
                            .padding(24)
                        }

                        // ── SIGN OUT ──────────────────────────────────────────
                        Button {
                            showSignOutAlert = true
                        } label: {
                            Text("Sign Out")
                                .font(.system(size: 16, weight: .semibold))
                                .foregroundColor(.accentRed)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 14)
                        }
                        .padding(.horizontal, 16)
                        .padding(.top, 12)
                        .padding(.bottom, 40)
                    }
                }
            }
            .navigationBarHidden(true)
            .alert("Sign Out?", isPresented: $showSignOutAlert) {
                Button("Sign Out", role: .destructive) { store.signOut() }
                Button("Cancel", role: .cancel) {}
            } message: {
                Text("You will need to set up your profile again.")
            }
            .sheet(isPresented: $showPinSheet) { PinSetupSheet() }
        }
    }
}

// MARK: - Settings Sub-components
struct SettingsSection<Content: View>: View {
    let label: String
    let content: Content
    init(label: String, @ViewBuilder content: () -> Content) {
        self.label = label; self.content = content()
    }
    var body: some View {
        VStack(spacing: 0) {
            SectionLabel(text: label)
            content
            Divider().background(Color.divider).padding(.horizontal, 16)
        }
    }
}

struct SettingsToggleRow: View {
    let icon: String
    let label: String
    var subtitle: String = ""
    @Binding var isOn: Bool

    var body: some View {
        HStack(spacing: 14) {
            Image(systemName: icon)
                .foregroundColor(.accentCyan)
                .frame(width: 32)
            VStack(alignment: .leading, spacing: 2) {
                Text(label).font(.system(size: 15)).foregroundColor(.textPrimary)
                if !subtitle.isEmpty {
                    Text(subtitle).font(.system(size: 12)).foregroundColor(.textMuted)
                }
            }
            Spacer()
            Toggle("", isOn: $isOn)
                .toggleStyle(SwitchToggleStyle(tint: .accentCyan))
                .labelsHidden()
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .background(Color.bgSurface)
    }
}

struct SettingsPickerRow: View {
    let icon: String
    let label: String
    let value: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 14) {
                Image(systemName: icon)
                    .foregroundColor(.accentCyan)
                    .frame(width: 32)
                Text(label).font(.system(size: 15)).foregroundColor(.textPrimary)
                Spacer()
                Text(value).font(.system(size: 13)).foregroundColor(.textMuted)
                Image(systemName: "chevron.right").font(.system(size: 12)).foregroundColor(.textMuted)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .background(Color.bgSurface)
        }
        .buttonStyle(.plain)
    }
}

struct SettingsButtonRow: View {
    let icon: String
    let label: String
    var subtitle: String = ""
    var isDestructive: Bool = false
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 14) {
                Image(systemName: icon)
                    .foregroundColor(isDestructive ? .accentRed : .accentCyan)
                    .frame(width: 32)
                VStack(alignment: .leading, spacing: 2) {
                    Text(label).font(.system(size: 15))
                        .foregroundColor(isDestructive ? .accentRed : .textPrimary)
                    if !subtitle.isEmpty {
                        Text(subtitle).font(.system(size: 12)).foregroundColor(.textMuted)
                    }
                }
                Spacer()
                Image(systemName: "chevron.right").font(.system(size: 12)).foregroundColor(.textMuted)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .background(Color.bgSurface)
        }
        .buttonStyle(.plain)
    }
}

struct PinSetupSheet: View {
    @Environment(\.dismiss) var dismiss
    @State private var pin = ""
    var body: some View {
        NavigationStack {
            ZStack {
                Color.bgPrimary.ignoresSafeArea()
                VStack(spacing: 20) {
                    Spacer()
                    Image(systemName: "lock.fill").font(.system(size: 48)).foregroundColor(.accentCyan)
                    Text("Set App PIN").font(.system(size: 22, weight: .bold)).foregroundColor(.textPrimary)
                    SecureField("4–6 digit PIN", text: $pin)
                        .keyboardType(.numberPad)
                        .textFieldStyle(F4TextFieldStyle())
                    Spacer()
                }
            }
            .navigationTitle("App PIN")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancel") { dismiss() }.foregroundColor(.textMuted) }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") { dismiss() }.foregroundColor(.accentCyan)
                }
            }
            .toolbarBackground(Color.bgSurface, for: .navigationBar)
            .toolbarColorScheme(.dark, for: .navigationBar)
        }
    }
}
