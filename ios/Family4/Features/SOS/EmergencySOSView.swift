import SwiftUI

struct EmergencySOSView: View {
    @EnvironmentObject var store: AppStore
    @State private var isSending = false
    @State private var countdown = 5
    @State private var showCancelAlert = false
    @State private var timer: Timer? = nil

    var body: some View {
        ZStack {
            Color.bgPrimary.ignoresSafeArea()
            VStack(spacing: 0) {
                SectionHeaderView(title: "Emergency SOS")

                ScrollView {
                    VStack(spacing: 24) {
                        // Warning card
                        GlassCard {
                            VStack(spacing: 12) {
                                Image(systemName: "exclamationmark.triangle.fill")
                                    .font(.system(size: 48))
                                    .foregroundColor(.accentRed)
                                Text("Emergency SOS")
                                    .font(.system(size: 22, weight: .black))
                                    .foregroundColor(.textPrimary)
                                Text("Hold the button below to send your location and an SOS alert to all family members.")
                                    .font(.system(size: 14))
                                    .foregroundColor(.textSecondary)
                                    .multilineTextAlignment(.center)
                            }
                            .padding(24)
                            .frame(maxWidth: .infinity)
                        }
                        .padding(.horizontal, 16)

                        // SOS button
                        VStack(spacing: 16) {
                            ZStack {
                                // Pulsing rings
                                ForEach(0..<3, id: \.self) { i in
                                    Circle()
                                        .stroke(Color.accentRed.opacity(0.3 - Double(i) * 0.1), lineWidth: 2)
                                        .frame(width: CGFloat(120 + i * 30), height: CGFloat(120 + i * 30))
                                        .scaleEffect(isSending ? 1.3 : 1.0)
                                        .animation(
                                            .easeInOut(duration: 1.0)
                                            .repeatForever(autoreverses: true)
                                            .delay(Double(i) * 0.2),
                                            value: isSending
                                        )
                                }

                                Button {
                                    if isSending {
                                        cancelSOS()
                                    } else {
                                        startSOS()
                                    }
                                } label: {
                                    VStack(spacing: 6) {
                                        Text(isSending ? "\(countdown)" : "SOS")
                                            .font(.system(size: isSending ? 36 : 32, weight: .black))
                                            .foregroundColor(.white)
                                        if isSending {
                                            Text("Tap to cancel")
                                                .font(.system(size: 11))
                                                .foregroundColor(.white.opacity(0.8))
                                        }
                                    }
                                    .frame(width: 110, height: 110)
                                    .background(isSending ? Color.accentRed : Color.accentRed.opacity(0.8))
                                    .clipShape(Circle())
                                    .shadow(color: Color.accentRed.opacity(0.5), radius: 20, x: 0, y: 8)
                                }
                            }
                            .frame(height: 200)

                            if !isSending {
                                Text("HOLD for 3 seconds to activate")
                                    .font(.system(size: 12, weight: .semibold))
                                    .foregroundColor(.textMuted)
                                    .tracking(1)
                            }
                        }

                        // Quick actions
                        SectionLabel(text: "Quick Actions")
                        VStack(spacing: 1) {
                            SOSActionRow(icon: "phone.fill", label: "Call 911",
                                         color: .accentRed) {
                                if let url = URL(string: "tel://911") { UIApplication.shared.open(url) }
                            }
                            SOSActionRow(icon: "location.fill", label: "Share Location",
                                         color: .accentCyan) {
                                // Share sheet
                            }
                            SOSActionRow(icon: "message.fill", label: "Text SOS to Family",
                                         color: .accentPurple) {
                                sendSOSMessage()
                            }
                        }
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                        .padding(.horizontal, 16)

                        Spacer(minLength: 32)
                    }
                    .padding(.top, 16)
                }
            }
        }
    }

    private func startSOS() {
        isSending = true
        countdown = 5
        timer = Timer.scheduledTimer(withTimeInterval: 1, repeats: true) { t in
            Task { @MainActor in
                if self.countdown > 1 {
                    self.countdown -= 1
                } else {
                    t.invalidate()
                    self.sendSOSMessage()
                    self.isSending = false
                }
            }
        }
    }

    private func cancelSOS() {
        timer?.invalidate()
        isSending = false
        countdown = 5
    }

    private func sendSOSMessage() {
        for member in store.members {
            store.sendMessage(to: member.id, content: "🚨 SOS! I need help! Please contact me immediately.", type: "TEXT")
        }
    }
}

struct SOSActionRow: View {
    let icon: String
    let label: String
    let color: Color
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 14) {
                Image(systemName: icon)
                    .font(.system(size: 16))
                    .foregroundColor(color)
                    .frame(width: 36, height: 36)
                    .background(color.opacity(0.15))
                    .clipShape(RoundedRectangle(cornerRadius: 8))
                Text(label)
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundColor(.textPrimary)
                Spacer()
                Image(systemName: "chevron.right")
                    .font(.system(size: 12))
                    .foregroundColor(.textMuted)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 14)
            .background(Color.bgSurface)
        }
        .buttonStyle(.plain)
    }
}
