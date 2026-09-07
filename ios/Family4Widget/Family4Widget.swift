import WidgetKit
import SwiftUI

// MARK: - Family4 Home Screen Widget

struct Family4WidgetEntry: TimelineEntry {
    let date: Date
    let memberCount: Int
    let onlineCount: Int
    let nextEventTitle: String
    let nextEventTime: String
}

struct Family4WidgetProvider: TimelineProvider {
    func placeholder(in context: Context) -> Family4WidgetEntry {
        Family4WidgetEntry(date: Date(), memberCount: 4, onlineCount: 2,
                           nextEventTitle: "Family Dinner", nextEventTime: "7:00 PM")
    }

    func getSnapshot(in context: Context, completion: @escaping (Family4WidgetEntry) -> Void) {
        completion(placeholder(in: context))
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<Family4WidgetEntry>) -> Void) {
        let entry = Family4WidgetEntry(
            date: Date(), memberCount: 4, onlineCount: 2,
            nextEventTitle: "Next Event", nextEventTime: ""
        )
        let timeline = Timeline(entries: [entry], policy: .atEnd)
        completion(timeline)
    }
}

// MARK: - Widget View

struct Family4WidgetView: View {
    let entry: Family4WidgetEntry

    var body: some View {
        ZStack {
            // Background
            LinearGradient(
                colors: [Color(red: 0.10, green: 0.10, blue: 0.18),
                         Color(red: 0.09, green: 0.13, blue: 0.24)],
                startPoint: .topLeading, endPoint: .bottomTrailing
            )

            VStack(alignment: .leading, spacing: 8) {
                // Header
                HStack(spacing: 6) {
                    Image(systemName: "f.circle.fill")
                        .font(.system(size: 16, weight: .black))
                        .foregroundColor(Color(red: 0, green: 0.83, blue: 1.0))
                    Text("Family4")
                        .font(.system(size: 14, weight: .bold))
                        .foregroundColor(.white)
                    Spacer()
                }

                Divider().background(Color.white.opacity(0.1))

                // Stats
                HStack(spacing: 16) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("\(entry.onlineCount)")
                            .font(.system(size: 22, weight: .black))
                            .foregroundColor(Color(red: 0, green: 0.83, blue: 1.0))
                        Text("Online")
                            .font(.system(size: 10))
                            .foregroundColor(Color.white.opacity(0.55))
                    }
                    VStack(alignment: .leading, spacing: 2) {
                        Text("\(entry.memberCount)")
                            .font(.system(size: 22, weight: .black))
                            .foregroundColor(.white)
                        Text("Members")
                            .font(.system(size: 10))
                            .foregroundColor(Color.white.opacity(0.55))
                    }
                }

                Spacer()

                // Quick actions
                HStack(spacing: 8) {
                    WidgetActionButton(icon: "camera.fill",  label: "Camera")
                    WidgetActionButton(icon: "bubble.left.fill", label: "Chat")
                    WidgetActionButton(icon: "calendar",     label: "Events")
                }
            }
            .padding(14)
        }
    }
}

struct WidgetActionButton: View {
    let icon: String
    let label: String

    var body: some View {
        VStack(spacing: 4) {
            Image(systemName: icon)
                .font(.system(size: 14))
                .foregroundColor(Color(red: 0, green: 0.83, blue: 1.0))
                .frame(width: 32, height: 32)
                .background(Color.white.opacity(0.08))
                .clipShape(RoundedRectangle(cornerRadius: 8))
            Text(label)
                .font(.system(size: 9))
                .foregroundColor(Color.white.opacity(0.55))
        }
    }
}

// MARK: - Widget Definition

@main
struct Family4Widget: Widget {
    let kind: String = "Family4Widget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: Family4WidgetProvider()) { entry in
            Family4WidgetView(entry: entry)
                .containerBackground(for: .widget) {
                    Color(red: 0.10, green: 0.10, blue: 0.18)
                }
        }
        .configurationDisplayName("Family4")
        .description("Stay connected with your family at a glance.")
        .supportedFamilies([.systemSmall, .systemMedium])
    }
}

// MARK: - Preview

#Preview(as: .systemSmall) {
    Family4Widget()
} timeline: {
    Family4WidgetEntry(date: Date(), memberCount: 4, onlineCount: 2,
                       nextEventTitle: "Soccer Practice", nextEventTime: "3:30 PM")
}
