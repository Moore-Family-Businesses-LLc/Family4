import SwiftUI

// MARK: - Root navigation controller
struct RootView: View {
    @EnvironmentObject var store: AppStore

    var body: some View {
        Group {
            if !store.hasCompletedOnboarding {
                OnboardingView()
            } else if !store.isAuthenticated {
                OnboardingView()
            } else {
                MainTabView()
            }
        }
        .animation(.easeInOut, value: store.isAuthenticated)
    }
}

// MARK: - Main Tab View
struct MainTabView: View {
    @EnvironmentObject var store: AppStore
    @State private var selectedTab: Tab = .dashboard

    enum Tab: Int, CaseIterable {
        case dashboard, camera, chat, map, more

        var label: String {
            switch self {
            case .dashboard: return "Home"
            case .camera:    return "Camera"
            case .chat:      return "Chat"
            case .map:       return "Map"
            case .more:      return "More"
            }
        }

        var icon: String {
            switch self {
            case .dashboard: return "house.fill"
            case .camera:    return "camera.fill"
            case .chat:      return "bubble.left.and.bubble.right.fill"
            case .map:       return "map.fill"
            case .more:      return "square.grid.2x2.fill"
            }
        }
    }

    var body: some View {
        ZStack(alignment: .bottom) {
            TabView(selection: $selectedTab) {
                DashboardView()
                    .tag(Tab.dashboard)

                CameraView()
                    .tag(Tab.camera)

                ChatListView()
                    .tag(Tab.chat)

                MapView()
                    .tag(Tab.map)

                MoreView()
                    .tag(Tab.more)
            }
            .tabViewStyle(.page(indexDisplayMode: .never))

            // Custom bottom nav bar
            CustomTabBar(selectedTab: $selectedTab)
        }
        .background(Color.bgPrimary.ignoresSafeArea())
        .preferredColorScheme(.dark)
    }
}

// MARK: - Custom Tab Bar
struct CustomTabBar: View {
    @Binding var selectedTab: MainTabView.Tab
    @EnvironmentObject var store: AppStore

    var body: some View {
        VStack(spacing: 0) {
            Divider().background(Color.divider)
            HStack(spacing: 0) {
                ForEach(MainTabView.Tab.allCases, id: \.self) { tab in
                    Button {
                        withAnimation(.easeInOut(duration: 0.2)) { selectedTab = tab }
                    } label: {
                        VStack(spacing: 4) {
                            ZStack {
                                Image(systemName: tab.icon)
                                    .font(.system(size: 22))
                                    .foregroundColor(selectedTab == tab ? .accentCyan : .textMuted)

                                // Badge for chat
                                if tab == .chat && store.unreadCount > 0 {
                                    Text("\(min(store.unreadCount, 99))")
                                        .font(.caption2.bold())
                                        .foregroundColor(.bgPrimary)
                                        .padding(.horizontal, 5)
                                        .padding(.vertical, 2)
                                        .background(Color.accentCyan)
                                        .clipShape(Capsule())
                                        .offset(x: 10, y: -10)
                                }
                                // Badge for more (pending tasks)
                                if tab == .more {
                                    let pending = store.tasks.filter { !$0.isCompleted }.count
                                    if pending > 0 {
                                        Text("\(min(pending, 99))")
                                            .font(.caption2.bold())
                                            .foregroundColor(.bgPrimary)
                                            .padding(.horizontal, 5)
                                            .padding(.vertical, 2)
                                            .background(Color.accentPurple)
                                            .clipShape(Capsule())
                                            .offset(x: 10, y: -10)
                                    }
                                }
                            }
                            Text(tab.label)
                                .font(.system(size: 10))
                                .foregroundColor(selectedTab == tab ? .accentCyan : .textMuted)
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 10)
                    }
                    .buttonStyle(.plain)
                }
            }
            .background(Color.bgSurface)
        }
    }
}
