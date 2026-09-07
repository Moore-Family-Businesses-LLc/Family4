import SwiftUI

// MARK: - Reusable UI Components

// ── Section Header ──────────────────────────────────────────────────────────
struct SectionHeaderView: View {
    let title: String
    var action: (() -> Void)? = nil
    var actionLabel: String = ""

    var body: some View {
        HStack(spacing: 10) {
            Image(systemName: "f.circle.fill")
                .resizable().frame(width: 20, height: 20)
                .foregroundColor(.accentCyan).opacity(0.75)

            Text(title)
                .font(.system(size: 18, weight: .bold))
                .foregroundColor(.textPrimary)

            Spacer()

            if let action = action {
                Button(actionLabel, action: action)
                    .font(.system(size: 13, weight: .medium))
                    .foregroundColor(.accentCyan)
            }
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 14)
        .background(Color.bgSurface)
    }
}

// ── Section Label (ALL CAPS muted) ──────────────────────────────────────────
struct SectionLabel: View {
    let text: String
    var body: some View {
        Text(text.uppercased())
            .font(.system(size: 11, weight: .bold))
            .foregroundColor(.textMuted)
            .tracking(1.2)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, 16)
            .padding(.top, 16)
            .padding(.bottom, 6)
    }
}

// ── Avatar Circle ───────────────────────────────────────────────────────────
struct AvatarView: View {
    let name: String
    var size: CGFloat = 40

    private static let avatarColors: [Color] = [
        .accentCyan, .accentPurple, .accentBlue, Color(hex: "#FF6B6B"),
        Color(hex: "#4ECDC4"), Color(hex: "#45B7D1"), Color(hex: "#96CEB4")
    ]

    private var color: Color {
        let idx = abs(name.hashValue) % Self.avatarColors.count
        return Self.avatarColors[idx]
    }

    private var initials: String {
        name.split(separator: " ").prefix(2)
            .compactMap { $0.first.map(String.init) }
            .joined().uppercased()
    }

    var body: some View {
        Text(initials)
            .font(.system(size: size * 0.38, weight: .bold))
            .foregroundColor(.white)
            .frame(width: size, height: size)
            .background(color)
            .clipShape(Circle())
    }
}

// ── Glass Card ───────────────────────────────────────────────────────────────
struct GlassCard<Content: View>: View {
    let content: Content
    init(@ViewBuilder content: () -> Content) { self.content = content() }

    var body: some View {
        content
            .background(Color.glassSurface)
            .clipShape(RoundedRectangle(cornerRadius: 20))
            .overlay(RoundedRectangle(cornerRadius: 20).stroke(Color.glassStroke, lineWidth: 1))
    }
}

// ── Surface Card ─────────────────────────────────────────────────────────────
struct SurfaceCard<Content: View>: View {
    let cornerRadius: CGFloat
    let content: Content
    init(cornerRadius: CGFloat = 16, @ViewBuilder content: () -> Content) {
        self.cornerRadius = cornerRadius; self.content = content()
    }

    var body: some View {
        content
            .background(Color.bgCard)
            .clipShape(RoundedRectangle(cornerRadius: cornerRadius))
            .overlay(RoundedRectangle(cornerRadius: cornerRadius).stroke(Color.cardStroke, lineWidth: 1))
    }
}

// ── Accent Strip Card ────────────────────────────────────────────────────────
struct AccentStripCard<Content: View>: View {
    let accentColor: Color
    let content: Content
    init(accentColor: Color = .accentCyan, @ViewBuilder content: () -> Content) {
        self.accentColor = accentColor; self.content = content()
    }

    var body: some View {
        HStack(spacing: 0) {
            Rectangle().fill(accentColor).frame(width: 4)
            content
        }
        .background(Color.bgCard)
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color.cardStroke, lineWidth: 1))
    }
}

// ── Priority Badge ───────────────────────────────────────────────────────────
struct PriorityBadge: View {
    let priority: Int
    var body: some View {
        Text(label)
            .font(.system(size: 9, weight: .bold))
            .foregroundColor(.bgPrimary)
            .padding(.horizontal, 6)
            .padding(.vertical, 2)
            .background(Color(hex: color))
            .clipShape(Capsule())
    }
    private var label: String {
        switch priority { case 0: return "LOW"; case 2: return "HIGH"; case 3: return "URGENT"; default: return "NORMAL" }
    }
    private var color: String {
        switch priority { case 0: return "#8892B0"; case 2: return "#FFAA00"; case 3: return "#FF3D71"; default: return "#00D4FF" }
    }
}

// ── Cyan Chip / Badge ────────────────────────────────────────────────────────
struct CyanChip: View {
    let text: String
    var body: some View {
        Text(text)
            .font(.system(size: 10, weight: .semibold))
            .foregroundColor(.accentCyan)
            .padding(.horizontal, 8)
            .padding(.vertical, 3)
            .background(Color.accentCyan.opacity(0.15))
            .overlay(Capsule().stroke(Color.accentCyan.opacity(0.4), lineWidth: 0.5))
            .clipShape(Capsule())
    }
}

// ── Empty State ──────────────────────────────────────────────────────────────
struct EmptyStateView: View {
    let title: String
    let subtitle: String
    var systemIcon: String = "f.circle.fill"

    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: systemIcon)
                .resizable().scaledToFit()
                .frame(width: 64, height: 64)
                .foregroundColor(.accentCyan.opacity(0.2))
            Text(title).font(.system(size: 17, weight: .bold)).foregroundColor(.textPrimary)
            Text(subtitle).font(.caption).foregroundColor(.textMuted).multilineTextAlignment(.center)
        }
        .padding(40)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

// ── Floating Action Button ───────────────────────────────────────────────────
struct FABView: View {
    let action: () -> Void
    var icon: String = "plus"

    var body: some View {
        Button(action: action) {
            Image(systemName: icon)
                .font(.system(size: 20, weight: .semibold))
                .foregroundColor(.bgPrimary)
                .frame(width: 56, height: 56)
                .background(Color.accentCyan)
                .clipShape(Circle())
                .shadow(color: .accentCyan.opacity(0.4), radius: 8, x: 0, y: 4)
        }
    }
}

// ── Stat Cell ────────────────────────────────────────────────────────────────
struct StatCell: View {
    let value: String
    let label: String
    var accentColor: Color = .accentCyan

    var body: some View {
        VStack(spacing: 4) {
            Text(value)
                .font(.system(size: 26, weight: .black))
                .foregroundColor(.textPrimary)
            Rectangle()
                .fill(accentColor)
                .frame(height: 2)
                .padding(.horizontal, 8)
            Text(label)
                .font(.system(size: 11, weight: .medium))
                .foregroundColor(.textMuted)
        }
    }
}

// ── Date Pill ────────────────────────────────────────────────────────────────
struct DatePill: View {
    let date: Date
    var body: some View {
        Text(date.formatted(date: .abbreviated, time: .omitted))
            .font(.system(size: 11, weight: .medium))
            .foregroundColor(.accentCyan)
            .padding(.horizontal, 8)
            .padding(.vertical, 3)
            .background(Color.accentCyan.opacity(0.12))
            .clipShape(Capsule())
    }
}

// ── Search Bar ───────────────────────────────────────────────────────────────
struct SearchBar: View {
    @Binding var text: String
    var placeholder: String = "Search…"

    var body: some View {
        HStack(spacing: 8) {
            Image(systemName: "magnifyingglass").foregroundColor(.textMuted).font(.system(size: 14))
            TextField(placeholder, text: $text)
                .foregroundColor(.textPrimary)
                .font(.system(size: 14))
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 9)
        .background(Color.bgCard)
        .clipShape(RoundedRectangle(cornerRadius: 20))
        .overlay(RoundedRectangle(cornerRadius: 20).stroke(Color.cardStroke, lineWidth: 1))
    }
}

// ── Segmented Picker ─────────────────────────────────────────────────────────
struct CyanSegmentedPicker<T: Hashable>: View {
    let options: [(label: String, value: T)]
    @Binding var selection: T

    var body: some View {
        HStack(spacing: 0) {
            ForEach(options, id: \.value) { opt in
                Button {
                    withAnimation(.easeInOut(duration: 0.18)) { selection = opt.value }
                } label: {
                    Text(opt.label)
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundColor(selection == opt.value ? .bgPrimary : .textMuted)
                        .padding(.vertical, 7)
                        .frame(maxWidth: .infinity)
                        .background(selection == opt.value ? Color.accentCyan : Color.clear)
                        .clipShape(RoundedRectangle(cornerRadius: 8))
                }
                .buttonStyle(.plain)
            }
        }
        .padding(3)
        .background(Color.bgCard)
        .clipShape(RoundedRectangle(cornerRadius: 10))
    }
}
