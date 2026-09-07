import SwiftUI

// MARK: - Brand Colours (matches Android colors.xml exactly)
extension Color {
    // Backgrounds
    static let bgPrimary   = Color(hex: "#1A1A2E")
    static let bgSurface   = Color(hex: "#1E2240")
    static let bgCard      = Color(hex: "#242844")
    static let bgElevated  = Color(hex: "#2D325A")
    static let bgInput     = Color(hex: "#1E2240")

    // Accents
    static let accentCyan   = Color(hex: "#00D4FF")
    static let accentPurple = Color(hex: "#7B2FFF")
    static let accentBlue   = Color(hex: "#3B82D4")
    static let accentRed    = Color(hex: "#FF3D71")
    static let accentGreen  = Color(hex: "#00E096")
    static let accentOrange = Color(hex: "#FFAA00")

    // Text
    static let textPrimary   = Color.white
    static let textSecondary = Color(hex: "#CCD1E4")
    static let textMuted     = Color(hex: "#8892B0")
    static let textDisabled  = Color(hex: "#4A5580")

    // Status
    static let onlineGreen  = Color(hex: "#00E096")
    static let offlineGray  = Color(hex: "#8892B0")
    static let errorRed     = Color(hex: "#CF6679")
    static let warningAmber = Color(hex: "#FFAA00")
    static let divider      = Color.white.opacity(0.10)
    static let cardStroke   = Color(hex: "#00D4FF").opacity(0.10)
    static let glassSurface = Color(hex: "#2D325A").opacity(0.10)
    static let glassStroke  = Color(hex: "#00D4FF").opacity(0.15)

    // Glow
    static let glowCyan   = Color(hex: "#00D4FF").opacity(0.30)
    static let glowPurple = Color(hex: "#7B2FFF").opacity(0.30)

    // Note colours (Google Keep dark-mode palette)
    static let noteDefault = Color(hex: "#1E2240")
    static let noteRed     = Color(hex: "#77172E")
    static let noteOrange  = Color(hex: "#692B17")
    static let noteYellow  = Color(hex: "#7C4A03")
    static let noteGreen   = Color(hex: "#264D3B")
    static let noteTeal    = Color(hex: "#0D3B2E")
    static let noteBlue    = Color(hex: "#1E3A5F")
    static let notePurple  = Color(hex: "#42275E")
    static let notePink    = Color(hex: "#6B1941")
    static let noteGray    = Color(hex: "#232427")

    init(hex: String) {
        let hex = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        var int: UInt64 = 0
        Scanner(string: hex).scanHexInt64(&int)
        let a, r, g, b: UInt64
        switch hex.count {
        case 3:  (a, r, g, b) = (255, (int >> 8) * 17, (int >> 4 & 0xF) * 17, (int & 0xF) * 17)
        case 6:  (a, r, g, b) = (255, int >> 16, int >> 8 & 0xFF, int & 0xFF)
        case 8:  (a, r, g, b) = (int >> 24, int >> 16 & 0xFF, int >> 8 & 0xFF, int & 0xFF)
        default: (a, r, g, b) = (255, 0, 0, 0)
        }
        self.init(.sRGB, red: Double(r)/255, green: Double(g)/255, blue: Double(b)/255, opacity: Double(a)/255)
    }
}

// MARK: - Typography
struct AppFont {
    static func hero(_ size: CGFloat = 28) -> Font { .system(size: size, weight: .bold, design: .default) }
    static func title(_ size: CGFloat = 18) -> Font { .system(size: size, weight: .bold) }
    static func body(_ size: CGFloat = 15) -> Font  { .system(size: size, weight: .regular) }
    static func label(_ size: CGFloat = 11) -> Font { .system(size: size, weight: .bold) }
    static func caption(_ size: CGFloat = 12) -> Font { .system(size: size, weight: .regular) }
    static func mono(_ size: CGFloat = 13) -> Font  { .system(size: size, design: .monospaced) }
}

// MARK: - Gradient helpers
extension LinearGradient {
    static let heroGradient = LinearGradient(
        colors: [Color(hex: "#1A1A2E"), Color(hex: "#16213E"), Color(hex: "#0F3460")],
        startPoint: .top, endPoint: .bottom
    )
    static let cyanGradient = LinearGradient(
        colors: [Color.accentCyan, Color.accentBlue],
        startPoint: .leading, endPoint: .trailing
    )
}

// MARK: - View Modifiers
struct GlassCardModifier: ViewModifier {
    func body(content: Content) -> some View {
        content
            .background(Color.glassSurface)
            .overlay(RoundedRectangle(cornerRadius: 20).stroke(Color.glassStroke, lineWidth: 1))
            .cornerRadius(20)
    }
}

struct SurfaceCardModifier: ViewModifier {
    var cornerRadius: CGFloat = 16
    func body(content: Content) -> some View {
        content
            .background(Color.bgCard)
            .overlay(RoundedRectangle(cornerRadius: cornerRadius).stroke(Color.cardStroke, lineWidth: 1))
            .cornerRadius(cornerRadius)
    }
}

extension View {
    func glassCard() -> some View { modifier(GlassCardModifier()) }
    func surfaceCard(cornerRadius: CGFloat = 16) -> some View { modifier(SurfaceCardModifier(cornerRadius: cornerRadius)) }
}
