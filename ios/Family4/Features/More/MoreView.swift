import SwiftUI

// MARK: - More Grid (hub for secondary features)
struct MoreView: View {
    @EnvironmentObject var store: AppStore

    private struct MoreItem: Identifiable {
        let id = UUID()
        let icon: String
        let label: String
        let color: Color
        let dest: AnyView
    }

    private var items: [MoreItem] { [
        MoreItem(icon: "note.text",        label: "Notes",        color: .accentBlue,   dest: AnyView(NotesView())),
        MoreItem(icon: "calendar",         label: "Calendar",     color: .accentCyan,   dest: AnyView(CalendarView())),
        MoreItem(icon: "checkmark.circle", label: "Tasks",        color: .accentPurple, dest: AnyView(TasksView())),
        MoreItem(icon: "rectangle.stack",  label: "Family Board", color: .accentOrange, dest: AnyView(FamilyBoardView())),
        MoreItem(icon: "folder.fill",      label: "Files",        color: .accentBlue,   dest: AnyView(FilesView())),
        MoreItem(icon: "photo.stack",      label: "Albums",       color: .accentGreen,  dest: AnyView(AlbumsView())),
        MoreItem(icon: "cloud.sun.fill",   label: "Weather",      color: .accentCyan,   dest: AnyView(WeatherView())),
        MoreItem(icon: "heart.fill",       label: "Health",       color: .accentRed,    dest: AnyView(HealthView())),
        MoreItem(icon: "radio.fill",       label: "Walkie-Talkie",color: .accentPurple, dest: AnyView(WalkieTalkieView())),
        MoreItem(icon: "exclamationmark.triangle.fill", label: "SOS", color: .accentRed, dest: AnyView(EmergencySOSView())),
        MoreItem(icon: "cart.fill",        label: "Shopping",     color: .accentGreen,  dest: AnyView(ShoppingView())),
        MoreItem(icon: "house.fill",       label: "Chores",       color: .accentOrange, dest: AnyView(ChoresView())),
        MoreItem(icon: "chart.bar.fill",   label: "Polls",        color: .accentBlue,   dest: AnyView(PollsView())),
        MoreItem(icon: "moon.fill",        label: "Bedtime",      color: .accentPurple, dest: AnyView(BedtimeView())),
        MoreItem(icon: "lock.shield.fill", label: "Parent Zone",  color: .accentCyan,   dest: AnyView(ParentZoneView())),
        MoreItem(icon: "gearshape.fill",   label: "Settings",     color: .textMuted,    dest: AnyView(SettingsView())),
    ] }

    var body: some View {
        NavigationStack {
            ZStack {
                Color.bgPrimary.ignoresSafeArea()
                VStack(spacing: 0) {
                    SectionHeaderView(title: "More")

                    ScrollView {
                        LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                            ForEach(items) { item in
                                NavigationLink(destination: item.dest) {
                                    MoreGridCell(icon: item.icon, label: item.label, color: item.color)
                                }
                                .buttonStyle(.plain)
                            }
                        }
                        .padding(16)
                    }
                }
            }
            .navigationBarHidden(true)
        }
    }
}

struct MoreGridCell: View {
    let icon: String
    let label: String
    let color: Color

    var body: some View {
        VStack(spacing: 10) {
            Image(systemName: icon)
                .font(.system(size: 24))
                .foregroundColor(color)
                .frame(width: 52, height: 52)
                .background(color.opacity(0.15))
                .clipShape(RoundedRectangle(cornerRadius: 14))
            Text(label)
                .font(.system(size: 11, weight: .semibold))
                .foregroundColor(.textSecondary)
                .multilineTextAlignment(.center)
                .lineLimit(2)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 14)
        .background(Color.bgCard)
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color.cardStroke, lineWidth: 1))
    }
}
