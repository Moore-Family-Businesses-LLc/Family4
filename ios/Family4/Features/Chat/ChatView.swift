import SwiftUI

// MARK: - Chat List
struct ChatListView: View {
    @EnvironmentObject var store: AppStore
    @State private var searchText = ""

    var filteredMembers: [FamilyMember] {
        if searchText.isEmpty { return store.members }
        return store.members.filter { $0.displayName.localizedCaseInsensitiveContains(searchText) }
    }

    var body: some View {
        NavigationStack {
            ZStack {
                Color.bgPrimary.ignoresSafeArea()
                VStack(spacing: 0) {
                    SectionHeaderView(title: "Chat")
                    HStack {
                        SearchBar(text: $searchText, placeholder: "Search family…")
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 10)
                    .background(Color.bgSurface)

                    if filteredMembers.isEmpty {
                        EmptyStateView(title: "No family members", subtitle: "Invite someone to get started",
                                       systemIcon: "person.2.fill")
                    } else {
                        ScrollView {
                            LazyVStack(spacing: 1) {
                                ForEach(filteredMembers) { member in
                                    NavigationLink(destination: ChatDetailView(member: member)) {
                                        ChatListRow(member: member, store: store)
                                    }
                                    .buttonStyle(.plain)
                                }
                            }
                        }
                    }
                }
            }
            .navigationBarHidden(true)
        }
    }
}

struct ChatListRow: View {
    let member: FamilyMember
    let store: AppStore

    var lastMessage: ChatMessage? {
        store.messages(for: member.id).last
    }
    var unread: Int {
        store.messages(for: member.id).filter { !$0.isRead && !$0.isSentByMe }.count
    }

    var body: some View {
        HStack(spacing: 14) {
            ZStack(alignment: .bottomTrailing) {
                AvatarView(name: member.displayName, size: 50)
                Circle().fill(member.isOnline ? Color.onlineGreen : Color.offlineGray)
                    .frame(width: 13, height: 13)
                    .overlay(Circle().stroke(Color.bgPrimary, lineWidth: 2))
            }
            VStack(alignment: .leading, spacing: 3) {
                HStack {
                    Text(member.displayName)
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundColor(.textPrimary)
                    Spacer()
                    if let msg = lastMessage {
                        Text(msg.timestamp.formatted(date: .omitted, time: .shortened))
                            .font(.system(size: 11))
                            .foregroundColor(.textMuted)
                    }
                }
                HStack {
                    Text(lastMessage?.content ?? (member.statusMessage.isEmpty ? "Tap to message" : member.statusMessage))
                        .font(.system(size: 13))
                        .foregroundColor(.textMuted)
                        .lineLimit(1)
                    Spacer()
                    if unread > 0 {
                        Text("\(unread)")
                            .font(.system(size: 11, weight: .bold))
                            .foregroundColor(.bgPrimary)
                            .frame(minWidth: 20, minHeight: 20)
                            .background(Color.accentCyan)
                            .clipShape(Circle())
                    }
                }
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .background(Color.bgSurface)
    }
}

// MARK: - Chat Detail
struct ChatDetailView: View {
    let member: FamilyMember
    @EnvironmentObject var store: AppStore
    @State private var messageText = ""
    @State private var scrollID = UUID()

    var messages: [ChatMessage] { store.messages(for: member.id) }

    var body: some View {
        ZStack {
            Color.bgPrimary.ignoresSafeArea()
            VStack(spacing: 0) {
                // Header
                HStack(spacing: 12) {
                    AvatarView(name: member.displayName, size: 38)
                    VStack(alignment: .leading, spacing: 2) {
                        Text(member.displayName).font(.system(size: 16, weight: .semibold)).foregroundColor(.textPrimary)
                        Text(member.isOnline ? "Online" : "Offline")
                            .font(.system(size: 12)).foregroundColor(member.isOnline ? .onlineGreen : .textMuted)
                    }
                    Spacer()
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 12)
                .background(Color.bgSurface)

                Divider().background(Color.divider)

                // Messages
                ScrollViewReader { proxy in
                    ScrollView {
                        LazyVStack(spacing: 6) {
                            ForEach(messages) { msg in
                                MessageBubble(message: msg)
                                    .id(msg.id)
                            }
                            Color.clear.frame(height: 1).id("bottom")
                        }
                        .padding(.vertical, 12)
                    }
                    .onChange(of: messages.count) { _ in
                        withAnimation { proxy.scrollTo("bottom") }
                    }
                    .onAppear {
                        proxy.scrollTo("bottom")
                    }
                }

                // Input bar
                HStack(spacing: 12) {
                    TextField("Message…", text: $messageText)
                        .foregroundColor(.textPrimary)
                        .padding(.horizontal, 14)
                        .padding(.vertical, 10)
                        .background(Color.bgCard)
                        .clipShape(RoundedRectangle(cornerRadius: 20))

                    Button {
                        let text = messageText.trimmingCharacters(in: .whitespaces)
                        guard !text.isEmpty else { return }
                        store.sendMessage(to: member.id, content: text)
                        messageText = ""
                    } label: {
                        Image(systemName: "arrow.up.circle.fill")
                            .font(.system(size: 32))
                            .foregroundColor(messageText.isEmpty ? .textMuted : .accentCyan)
                    }
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 12)
                .background(Color.bgSurface)
            }
        }
        .navigationBarHidden(true)
    }
}

struct MessageBubble: View {
    let message: ChatMessage

    var body: some View {
        HStack {
            if message.isSentByMe { Spacer(minLength: 60) }
            VStack(alignment: message.isSentByMe ? .trailing : .leading, spacing: 3) {
                Text(message.content)
                    .font(.system(size: 15))
                    .foregroundColor(message.isSentByMe ? .bgPrimary : .textPrimary)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 9)
                    .background(message.isSentByMe ? Color.accentCyan : Color.bgCard)
                    .clipShape(
                        RoundedRectangle(cornerRadius: 18)
                    )

                HStack(spacing: 4) {
                    Text(message.timestamp.formatted(date: .omitted, time: .shortened))
                        .font(.system(size: 10))
                        .foregroundColor(.textMuted)
                    if message.isSentByMe {
                        Image(systemName: message.isRead ? "checkmark.circle.fill" : "checkmark.circle")
                            .font(.system(size: 10))
                            .foregroundColor(message.isRead ? .accentCyan : .textMuted)
                    }
                }
            }
            if !message.isSentByMe { Spacer(minLength: 60) }
        }
        .padding(.horizontal, 16)
    }
}
