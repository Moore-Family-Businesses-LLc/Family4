import SwiftUI

struct DashboardView: View {
    @EnvironmentObject var store: AppStore
    @State private var currentTime = Date()
    private let timer = Timer.publish(every: 1, on: .main, in: .common).autoconnect()

    var body: some View {
        ZStack {
            Color.bgPrimary.ignoresSafeArea()

            ScrollView {
                VStack(spacing: 0) {
                    SectionHeaderView(title: "Dashboard")

                    // ── Hero glass card ───────────────────────────────────────
                    GlassCard {
                        VStack(alignment: .leading, spacing: 12) {
                            HStack {
                                VStack(alignment: .leading, spacing: 4) {
                                    Text(greetingText)
                                        .font(.system(size: 15, weight: .medium))
                                        .foregroundColor(.textSecondary)
                                    Text(store.currentUser.displayName)
                                        .font(.system(size: 26, weight: .black))
                                        .foregroundColor(.textPrimary)
                                }
                                Spacer()
                                // Live clock
                                VStack(alignment: .trailing, spacing: 2) {
                                    Text(currentTime.formatted(date: .omitted, time: .shortened))
                                        .font(.system(size: 22, weight: .bold, design: .monospaced))
                                        .foregroundColor(.accentCyan)
                                    Text(currentTime.formatted(.dateTime.weekday(.wide).month().day()))
                                        .font(.system(size: 11))
                                        .foregroundColor(.textMuted)
                                }
                            }

                            Divider().background(Color.divider)

                            // Stats row
                            HStack(spacing: 0) {
                                StatCell(
                                    value: "\(store.members.filter { $0.isOnline }.count)",
                                    label: "Online",
                                    accentColor: .accentCyan
                                )
                                Spacer()
                                StatCell(
                                    value: "\(upcomingEventCount)",
                                    label: "Events",
                                    accentColor: .accentPurple
                                )
                                Spacer()
                                StatCell(
                                    value: "\(store.tasks.filter { !$0.isCompleted }.count)",
                                    label: "Tasks",
                                    accentColor: .accentBlue
                                )
                            }
                        }
                        .padding(20)
                    }
                    .padding(.horizontal, 16)
                    .padding(.top, 12)

                    // ── Family members row ────────────────────────────────────
                    SectionLabel(text: "Family")

                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 12) {
                            ForEach(store.members) { member in
                                MemberChip(member: member)
                            }
                        }
                        .padding(.horizontal, 16)
                    }

                    // ── Quick actions ─────────────────────────────────────────
                    SectionLabel(text: "Quick Access")

                    LazyVGrid(columns: [
                        GridItem(.flexible()), GridItem(.flexible())
                    ], spacing: 12) {
                        QuickActionCard(icon: "calendar", label: "Calendar",  color: .accentCyan,   dest: AnyView(CalendarView()))
                        QuickActionCard(icon: "checkmark.circle", label: "Tasks", color: .accentPurple, dest: AnyView(TasksView()))
                        QuickActionCard(icon: "note.text", label: "Notes",    color: .accentBlue,   dest: AnyView(NotesView()))
                        QuickActionCard(icon: "exclamationmark.triangle.fill", label: "SOS", color: .accentRed, dest: AnyView(EmergencySOSView()))
                    }
                    .padding(.horizontal, 16)

                    // ── Recent activity ───────────────────────────────────────
                    SectionLabel(text: "Recent Activity")

                    VStack(spacing: 1) {
                        ForEach(recentActivity, id: \.self) { item in
                            HStack(spacing: 12) {
                                Text("▍")
                                    .font(.system(size: 18, weight: .black))
                                    .foregroundColor(.accentCyan)
                                Text(item)
                                    .font(.system(size: 14))
                                    .foregroundColor(.textSecondary)
                                Spacer()
                            }
                            .padding(.horizontal, 16)
                            .padding(.vertical, 10)
                            .background(Color.bgSurface)
                        }
                    }

                    Spacer(minLength: 32)
                }
            }
        }
        .onReceive(timer) { currentTime = $0 }
    }

    private var greetingText: String {
        let hour = Calendar.current.component(.hour, from: Date())
        switch hour {
        case 5..<12:  return "Good morning,"
        case 12..<17: return "Good afternoon,"
        case 17..<21: return "Good evening,"
        default:      return "Good night,"
        }
    }

    private var upcomingEventCount: Int {
        let now = Date()
        let next7 = now.addingTimeInterval(7 * 86400)
        return store.calendarEvents.filter { $0.startTime >= now && $0.startTime <= next7 }.count
    }

    private var recentActivity: [String] {
        var items: [String] = []
        if let last = store.boardPosts.first { items.append("New post from \(last.authorName)") }
        if let last = store.allMessages.last(where: { !$0.isSentByMe }) { items.append("Message from \(last.senderId)") }
        let done = store.tasks.filter { $0.completedAt != nil }.count
        if done > 0 { items.append("\(done) task\(done == 1 ? "" : "s") completed today") }
        items.append("Family4 is running — \(store.members.count) members connected")
        return Array(items.prefix(5))
    }
}

// MARK: - Member Chip
struct MemberChip: View {
    let member: FamilyMember

    var body: some View {
        VStack(spacing: 6) {
            ZStack(alignment: .bottomTrailing) {
                AvatarView(name: member.displayName, size: 52)
                Circle()
                    .fill(member.isOnline ? Color.onlineGreen : Color.offlineGray)
                    .frame(width: 12, height: 12)
                    .overlay(Circle().stroke(Color.bgPrimary, lineWidth: 2))
            }
            Text(member.displayName)
                .font(.system(size: 11, weight: .medium))
                .foregroundColor(.textSecondary)
                .lineLimit(1)
        }
        .frame(width: 64)
    }
}

// MARK: - Quick Action Card
struct QuickActionCard: View {
    let icon: String
    let label: String
    let color: Color
    let dest: AnyView

    var body: some View {
        NavigationLink(destination: dest) {
            HStack(spacing: 0) {
                Rectangle()
                    .fill(color)
                    .frame(width: 4)
                HStack(spacing: 12) {
                    Image(systemName: icon)
                        .font(.system(size: 20))
                        .foregroundColor(color)
                        .frame(width: 36, height: 36)
                        .background(color.opacity(0.15))
                        .clipShape(RoundedRectangle(cornerRadius: 8))
                    Text(label)
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(.textPrimary)
                    Spacer()
                    Image(systemName: "chevron.right")
                        .font(.system(size: 11))
                        .foregroundColor(.textMuted)
                }
                .padding(14)
            }
            .background(Color.bgCard)
            .clipShape(RoundedRectangle(cornerRadius: 12))
            .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color.cardStroke, lineWidth: 1))
        }
        .buttonStyle(.plain)
    }
}
