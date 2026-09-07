import SwiftUI

struct FamilyBoardView: View {
    @EnvironmentObject var store: AppStore
    @State private var showAdd = false
    @State private var filterType: String = "all"

    var filteredPosts: [BoardPost] {
        if filterType == "all" { return store.boardPosts.sorted { $0.pinned && !$1.pinned } }
        return store.boardPosts.filter { $0.postType == filterType }
            .sorted { $0.pinned && !$1.pinned }
    }

    var body: some View {
        NavigationStack {
            ZStack {
                Color.bgPrimary.ignoresSafeArea()
                VStack(spacing: 0) {
                    SectionHeaderView(title: "Family Board", action: { showAdd = true }, actionLabel: "+ Post")

                    // Type filters
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            ForEach(["all","chat","announcement","event","photo","task"], id: \.self) { type in
                                Button {
                                    withAnimation { filterType = type }
                                } label: {
                                    Text(type == "all" ? "All" : type.capitalized)
                                        .font(.system(size: 12, weight: .semibold))
                                        .foregroundColor(filterType == type ? .bgPrimary : .textMuted)
                                        .padding(.horizontal, 14)
                                        .padding(.vertical, 7)
                                        .background(filterType == type ? Color.accentCyan : Color.bgCard)
                                        .clipShape(Capsule())
                                }
                                .buttonStyle(.plain)
                            }
                        }
                        .padding(.horizontal, 16)
                        .padding(.vertical, 10)
                    }
                    .background(Color.bgSurface)

                    if filteredPosts.isEmpty {
                        EmptyStateView(title: "No posts yet", subtitle: "Tap + Post to share something with the family",
                                       systemIcon: "rectangle.stack.fill")
                    } else {
                        ScrollView {
                            LazyVStack(spacing: 10) {
                                ForEach(filteredPosts) { post in
                                    BoardPostCard(post: post)
                                }
                            }
                            .padding(.horizontal, 12)
                            .padding(.top, 8)
                            Spacer(minLength: 32)
                        }
                    }
                }
            }
            .sheet(isPresented: $showAdd) { AddBoardPostSheet() }
            .navigationBarHidden(true)
        }
    }
}

// MARK: - Board Post Card
struct BoardPostCard: View {
    let post: BoardPost
    @EnvironmentObject var store: AppStore

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(spacing: 0) {
                // Vertical accent strip
                Rectangle()
                    .fill(Color(hex: post.typeColor))
                    .frame(width: 4)

                VStack(alignment: .leading, spacing: 10) {
                    // Author row
                    HStack(spacing: 10) {
                        AvatarView(name: post.authorName, size: 38)
                        VStack(alignment: .leading, spacing: 2) {
                            HStack(spacing: 8) {
                                Text(post.authorName)
                                    .font(.system(size: 14, weight: .bold))
                                    .foregroundColor(.textPrimary)
                                CyanChip(text: "\(post.typeEmoji) \(post.postType.capitalized)")
                                if post.pinned {
                                    Image(systemName: "pin.fill")
                                        .font(.system(size: 10))
                                        .foregroundColor(.accentCyan)
                                }
                            }
                            Text(relativeTime(post.createdAt))
                                .font(.system(size: 11))
                                .foregroundColor(.textMuted)
                        }
                        Spacer()
                        Button {
                            store.deleteBoardPost(post)
                        } label: {
                            Image(systemName: "xmark")
                                .font(.system(size: 12))
                                .foregroundColor(.textMuted.opacity(0.5))
                        }
                    }

                    // Content
                    Text(post.content)
                        .font(.system(size: 15))
                        .foregroundColor(.textPrimary)
                        .lineSpacing(4)
                        .padding(.leading, 48)

                    // Reactions
                    HStack(spacing: 4) {
                        ForEach(["❤️","👍","😂","🔥","⭐"], id: \.self) { emoji in
                            Button {
                                store.reactToPost(postId: post.id, emoji: emoji)
                            } label: {
                                HStack(spacing: 3) {
                                    Text(emoji).font(.system(size: 16))
                                    let count = post.reactions[emoji] ?? 0
                                    if count > 0 {
                                        Text("\(count)")
                                            .font(.system(size: 11))
                                            .foregroundColor(.textMuted)
                                    }
                                }
                                .padding(.horizontal, 8)
                                .padding(.vertical, 5)
                                .background(Color.bgElevated)
                                .clipShape(Capsule())
                            }
                            .buttonStyle(.plain)
                        }
                        Spacer()
                        if !post.reactions.isEmpty {
                            Text(post.reactions.map { "\($0.key)\($0.value)" }.joined(separator: " "))
                                .font(.system(size: 11))
                                .foregroundColor(.textMuted)
                        }
                    }
                    .padding(.leading, 48)
                }
                .padding(14)
            }
        }
        .background(Color.bgCard)
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color.cardStroke, lineWidth: 1))
    }

    private func relativeTime(_ date: Date) -> String {
        let diff = -date.timeIntervalSinceNow
        if diff < 60 { return "just now" }
        if diff < 3600 { return "\(Int(diff/60))m ago" }
        if diff < 86400 { return "\(Int(diff/3600))h ago" }
        return date.formatted(date: .abbreviated, time: .omitted)
    }
}

// MARK: - Add Post Sheet
struct AddBoardPostSheet: View {
    @Environment(\.dismiss) var dismiss
    @EnvironmentObject var store: AppStore
    @State private var content = ""
    @State private var postType = "chat"

    let types = ["chat","announcement","event","photo","task"]

    var body: some View {
        NavigationStack {
            ZStack {
                Color.bgPrimary.ignoresSafeArea()
                VStack(spacing: 20) {
                    SectionLabel(text: "Post Type")
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            ForEach(types, id: \.self) { t in
                                Button { postType = t } label: {
                                    Text(t.capitalized)
                                        .font(.system(size: 13, weight: .semibold))
                                        .foregroundColor(postType == t ? .bgPrimary : .accentCyan)
                                        .padding(.horizontal, 16)
                                        .padding(.vertical, 8)
                                        .background(postType == t ? Color.accentCyan : Color.accentCyan.opacity(0.12))
                                        .clipShape(Capsule())
                                }
                                .buttonStyle(.plain)
                            }
                        }
                        .padding(.horizontal, 16)
                    }

                    SectionLabel(text: "Content")
                    TextEditor(text: $content)
                        .foregroundColor(.textPrimary)
                        .scrollContentBackground(.hidden)
                        .background(Color.bgCard)
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                        .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color.cardStroke, lineWidth: 1))
                        .frame(minHeight: 140)
                        .padding(.horizontal, 16)

                    Spacer()
                }
                .padding(.top, 8)
            }
            .navigationTitle("New Post")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }.foregroundColor(.textMuted)
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Post") {
                        let post = BoardPost(
                            authorId: store.currentUser.id,
                            authorName: store.currentUser.displayName,
                            content: content,
                            postType: postType
                        )
                        store.addBoardPost(post)
                        dismiss()
                    }
                    .foregroundColor(content.isEmpty ? .textMuted : .accentCyan)
                    .disabled(content.isEmpty)
                }
            }
            .toolbarBackground(Color.bgSurface, for: .navigationBar)
            .toolbarColorScheme(.dark, for: .navigationBar)
        }
    }
}
