import SwiftUI

struct OnboardingView: View {
    @EnvironmentObject var store: AppStore
    @State private var page: Int = 0
    @State private var name: String = ""
    @State private var familyCode: String = ""

    private let pages: [(emoji: String, title: String, subtitle: String)] = [
        ("👨‍👩‍👧‍👦", "Welcome to Family4",    "Stay connected with everyone who matters most."),
        ("📍",          "Live Location",         "Always know where your family is — instantly."),
        ("💬",          "Encrypted Chat",         "Private, end-to-end encrypted family messages."),
        ("🔔",          "Smart Alerts",           "Bedtime reminders, safe-zone alerts, SOS."),
    ]

    var body: some View {
        ZStack {
            LinearGradient.heroGradient.ignoresSafeArea()

            VStack(spacing: 0) {
                if page < pages.count {
                    featurePage(pages[page])
                        .transition(.asymmetric(
                            insertion: .move(edge: .trailing),
                            removal: .move(edge: .leading)
                        ))
                } else {
                    setupPage
                        .transition(.asymmetric(
                            insertion: .move(edge: .trailing),
                            removal: .move(edge: .leading)
                        ))
                }
            }
        }
        .animation(.easeInOut(duration: 0.35), value: page)
    }

    // MARK: - Feature page
    private func featurePage(_ p: (emoji: String, title: String, subtitle: String)) -> some View {
        VStack(spacing: 0) {
            Spacer()

            // Logo mark
            ZStack {
                Circle()
                    .fill(Color.accentCyan.opacity(0.12))
                    .frame(width: 180, height: 180)
                Text(p.emoji)
                    .font(.system(size: 80))
            }
            .padding(.bottom, 48)

            Text(p.title)
                .font(.system(size: 32, weight: .bold))
                .foregroundColor(.white)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 32)
                .padding(.bottom, 16)

            Text(p.subtitle)
                .font(.system(size: 16))
                .foregroundColor(.textSecondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 40)

            Spacer()

            // Page dots
            HStack(spacing: 8) {
                ForEach(0..<pages.count + 1, id: \.self) { i in
                    Circle()
                        .fill(i == page ? Color.accentCyan : Color.white.opacity(0.3))
                        .frame(width: i == page ? 20 : 8, height: 8)
                        .clipShape(Capsule())
                        .animation(.spring(), value: page)
                }
            }
            .padding(.bottom, 40)

            Button {
                withAnimation { page += 1 }
            } label: {
                Text(page == pages.count - 1 ? "Get Started →" : "Next →")
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundColor(.bgPrimary)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 16)
                    .background(Color.accentCyan)
                    .clipShape(RoundedRectangle(cornerRadius: 16))
            }
            .padding(.horizontal, 32)
            .padding(.bottom, 48)
        }
    }

    // MARK: - Setup page
    private var setupPage: some View {
        ScrollView {
            VStack(spacing: 28) {
                Spacer(minLength: 40)

                Text("🏠")
                    .font(.system(size: 64))

                Text("Set Up Your Profile")
                    .font(.system(size: 28, weight: .bold))
                    .foregroundColor(.white)

                Text("Choose your display name and optionally enter a family code to join an existing group.")
                    .font(.system(size: 15))
                    .foregroundColor(.textSecondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 32)

                // Name field
                VStack(alignment: .leading, spacing: 8) {
                    Text("YOUR NAME")
                        .font(.system(size: 11, weight: .bold))
                        .foregroundColor(.textMuted)
                        .tracking(1.2)
                    TextField("e.g. Mom, Dad, Alex…", text: $name)
                        .foregroundColor(.textPrimary)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 14)
                        .background(Color.bgCard)
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                        .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color.cardStroke, lineWidth: 1))
                }
                .padding(.horizontal, 32)

                // Family code
                VStack(alignment: .leading, spacing: 8) {
                    Text("FAMILY CODE (OPTIONAL)")
                        .font(.system(size: 11, weight: .bold))
                        .foregroundColor(.textMuted)
                        .tracking(1.2)
                    TextField("Enter invite code to join a group", text: $familyCode)
                        .foregroundColor(.textPrimary)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 14)
                        .background(Color.bgCard)
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                        .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color.cardStroke, lineWidth: 1))
                }
                .padding(.horizontal, 32)

                Button {
                    let n = name.trimmingCharacters(in: .whitespaces)
                    store.completeOnboarding(name: n.isEmpty ? "Family Member" : n)
                } label: {
                    Text("Enter Family4 →")
                        .font(.system(size: 17, weight: .semibold))
                        .foregroundColor(.bgPrimary)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 16)
                        .background(Color.accentCyan)
                        .clipShape(RoundedRectangle(cornerRadius: 16))
                }
                .padding(.horizontal, 32)
                .padding(.top, 8)

                Spacer(minLength: 40)
            }
        }
    }
}
