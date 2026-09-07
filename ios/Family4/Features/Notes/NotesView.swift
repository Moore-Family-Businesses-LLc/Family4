import SwiftUI

// MARK: - Notes List
struct NotesView: View {
    @EnvironmentObject var store: AppStore
    @State private var searchText = ""
    @State private var showAdd = false
    @State private var editingNote: NoteItem? = nil
    @State private var isGrid = true

    var filteredNotes: [NoteItem] {
        let active = store.notes.filter { !$0.isArchived }
        if searchText.isEmpty { return active }
        return active.filter {
            $0.title.localizedCaseInsensitiveContains(searchText) ||
            $0.content.localizedCaseInsensitiveContains(searchText)
        }
    }

    var pinnedNotes: [NoteItem]  { filteredNotes.filter { $0.isPinned } }
    var regularNotes: [NoteItem] { filteredNotes.filter { !$0.isPinned } }

    var body: some View {
        NavigationStack {
            ZStack {
                Color.bgPrimary.ignoresSafeArea()
                VStack(spacing: 0) {
                    // Header with search
                    HStack(spacing: 10) {
                        Image(systemName: "f.circle.fill")
                            .foregroundColor(.accentCyan).opacity(0.85)
                            .font(.system(size: 20))
                        SearchBar(text: $searchText, placeholder: "Search notes…")
                        Button {
                            withAnimation { isGrid.toggle() }
                        } label: {
                            Image(systemName: isGrid ? "list.bullet" : "square.grid.2x2")
                                .foregroundColor(.accentCyan)
                                .font(.system(size: 18))
                        }
                    }
                    .padding(.horizontal, 12)
                    .padding(.vertical, 8)
                    .background(Color.bgSurface)

                    if filteredNotes.isEmpty {
                        EmptyStateView(title: "No notes yet", subtitle: "Tap + to create your first note",
                                       systemIcon: "note.text")
                    } else {
                        ScrollView {
                            if !pinnedNotes.isEmpty {
                                SectionLabel(text: "Pinned")
                                noteGrid(pinnedNotes)
                            }
                            if !regularNotes.isEmpty {
                                if !pinnedNotes.isEmpty { SectionLabel(text: "Others") }
                                noteGrid(regularNotes)
                            }
                            Spacer(minLength: 80)
                        }
                    }
                }

                // FAB
                VStack {
                    Spacer()
                    HStack {
                        Spacer()
                        FABView { showAdd = true }
                            .padding(24)
                    }
                }
            }
            .sheet(isPresented: $showAdd) { NoteDetailView(note: nil) }
            .sheet(item: $editingNote) { note in NoteDetailView(note: note) }
            .navigationBarHidden(true)
        }
    }

    @ViewBuilder
    private func noteGrid(_ notes: [NoteItem]) -> some View {
        if isGrid {
            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 8) {
                ForEach(notes) { note in
                    NoteCard(note: note)
                        .onTapGesture { editingNote = note }
                        .contextMenu {
                            Button(note.isPinned ? "Unpin" : "Pin") { store.togglePin(note) }
                            Button("Delete", role: .destructive) { store.deleteNote(note) }
                        }
                }
            }
            .padding(.horizontal, 8)
        } else {
            LazyVStack(spacing: 6) {
                ForEach(notes) { note in
                    NoteCard(note: note)
                        .onTapGesture { editingNote = note }
                        .contextMenu {
                            Button(note.isPinned ? "Unpin" : "Pin") { store.togglePin(note) }
                            Button("Delete", role: .destructive) { store.deleteNote(note) }
                        }
                }
            }
            .padding(.horizontal, 8)
        }
    }
}

// MARK: - Note Card
struct NoteCard: View {
    let note: NoteItem

    var body: some View {
        HStack(spacing: 0) {
            Rectangle()
                .fill(Color(hex: note.colorHex == "#1E2240" ? "#00D4FF" : note.colorHex))
                .frame(width: 4)
            VStack(alignment: .leading, spacing: 6) {
                if !note.title.isEmpty {
                    Text(note.title)
                        .font(.system(size: 14, weight: .bold))
                        .foregroundColor(.textPrimary)
                        .lineLimit(2)
                }
                if !note.content.isEmpty {
                    Text(note.content)
                        .font(.system(size: 12))
                        .foregroundColor(.textSecondary)
                        .lineLimit(6)
                }
                HStack {
                    if note.isPinned {
                        Image(systemName: "pin.fill")
                            .font(.system(size: 10))
                            .foregroundColor(.textMuted)
                    }
                    Spacer()
                    Text(note.updatedAt.formatted(date: .abbreviated, time: .omitted))
                        .font(.system(size: 10))
                        .foregroundColor(.textMuted)
                }
            }
            .padding(12)
        }
        .background(Color(hex: note.colorHex))
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color.white.opacity(0.08), lineWidth: 1))
    }
}

// MARK: - Note Detail / Editor
struct NoteDetailView: View {
    @Environment(\.dismiss) var dismiss
    @EnvironmentObject var store: AppStore
    var note: NoteItem?

    @State private var title: String
    @State private var content: String
    @State private var colorHex: String
    @State private var isPinned: Bool

    private let colorOptions = ["#1E2240","#77172E","#692B17","#7C4A03","#264D3B","#0D3B2E","#1E3A5F","#42275E","#6B1941"]

    init(note: NoteItem?) {
        self.note = note
        _title    = State(initialValue: note?.title ?? "")
        _content  = State(initialValue: note?.content ?? "")
        _colorHex = State(initialValue: note?.colorHex ?? "#1E2240")
        _isPinned = State(initialValue: note?.isPinned ?? false)
    }

    var body: some View {
        ZStack {
            Color(hex: colorHex).ignoresSafeArea()
            VStack(spacing: 0) {
                // Toolbar
                HStack {
                    Button("Cancel") { dismiss() }
                        .foregroundColor(.textMuted)
                    Spacer()
                    HStack(spacing: 16) {
                        Button {
                            withAnimation { isPinned.toggle() }
                        } label: {
                            Image(systemName: isPinned ? "pin.fill" : "pin")
                                .foregroundColor(isPinned ? .accentCyan : .textMuted)
                        }
                        Button("Save") { save() }
                            .font(.system(size: 15, weight: .semibold))
                            .foregroundColor(.accentCyan)
                    }
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 14)

                // Color picker strip
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(colorOptions, id: \.self) { hex in
                            Circle()
                                .fill(Color(hex: hex))
                                .frame(width: 28, height: 28)
                                .overlay(Circle().stroke(Color.white.opacity(colorHex == hex ? 0.8 : 0.2), lineWidth: 2))
                                .scaleEffect(colorHex == hex ? 1.25 : 1.0)
                                .onTapGesture { withAnimation(.spring()) { colorHex = hex } }
                        }
                    }
                    .padding(.horizontal, 16)
                }
                .padding(.vertical, 8)

                Divider().background(Color.white.opacity(0.1))

                // Text fields
                ScrollView {
                    VStack(alignment: .leading, spacing: 0) {
                        TextField("Title", text: $title)
                            .font(.system(size: 20, weight: .bold))
                            .foregroundColor(.textPrimary)
                            .padding(.horizontal, 20)
                            .padding(.top, 16)
                            .padding(.bottom, 8)

                        TextField("Note…", text: $content, axis: .vertical)
                            .font(.system(size: 15))
                            .foregroundColor(.textSecondary)
                            .lineLimit(20...)
                            .padding(.horizontal, 20)
                    }
                }
            }
        }
    }

    private func save() {
        if var existing = note {
            existing.title = title
            existing.content = content
            existing.colorHex = colorHex
            existing.isPinned = isPinned
            existing.updatedAt = Date()
            store.updateNote(existing)
        } else {
            let n = NoteItem(title: title, content: content, colorHex: colorHex, isPinned: isPinned)
            if !title.isEmpty || !content.isEmpty { store.addNote(n) }
        }
        dismiss()
    }
}
