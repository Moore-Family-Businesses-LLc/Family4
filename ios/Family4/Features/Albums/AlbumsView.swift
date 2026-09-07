import SwiftUI
import PhotosUI

struct AlbumsView: View {
    @EnvironmentObject var store: AppStore
    @State private var showAdd = false
    @State private var selectedAlbum: PhotoAlbum? = nil

    var body: some View {
        NavigationStack {
            ZStack {
                Color.bgPrimary.ignoresSafeArea()
                VStack(spacing: 0) {
                    SectionHeaderView(title: "Albums", action: { showAdd = true }, actionLabel: "+ Album")

                    if store.albums.isEmpty {
                        EmptyStateView(title: "No albums yet", subtitle: "Tap + Album to create your first photo album",
                                       systemIcon: "photo.on.rectangle.angled")
                    } else {
                        ScrollView {
                            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                                ForEach(store.albums) { album in
                                    AlbumCard(album: album)
                                        .onTapGesture { selectedAlbum = album }
                                }
                            }
                            .padding(16)
                        }
                    }
                }
            }
            .sheet(isPresented: $showAdd) { AddAlbumSheet() }
            .sheet(item: $selectedAlbum) { album in AlbumDetailView(album: album) }
            .navigationBarHidden(true)
        }
    }
}

struct AlbumCard: View {
    let album: PhotoAlbum
    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            ZStack {
                Color.bgCard
                Image(systemName: "photo.fill")
                    .font(.system(size: 40))
                    .foregroundColor(.textMuted.opacity(0.3))
            }
            .frame(height: 120)
            .clipShape(RoundedRectangle(cornerRadius: 12))

            VStack(alignment: .leading, spacing: 3) {
                Text(album.name)
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundColor(.textPrimary)
                    .lineLimit(1)
                Text(album.createdAt.formatted(date: .abbreviated, time: .omitted))
                    .font(.system(size: 11))
                    .foregroundColor(.textMuted)
            }
            .padding(.vertical, 8)
            .padding(.horizontal, 4)
        }
    }
}

struct AlbumDetailView: View {
    let album: PhotoAlbum
    @EnvironmentObject var store: AppStore
    @State private var selectedItems: [PhotosPickerItem] = []
    @State private var selectedImages: [UIImage] = []
    @Environment(\.dismiss) var dismiss

    var albumPhotos: [FamilyPhoto] { store.photos.filter { $0.albumId == album.id } }

    var body: some View {
        NavigationStack {
            ZStack {
                Color.bgPrimary.ignoresSafeArea()
                ScrollView {
                    LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible()), GridItem(.flexible())], spacing: 2) {
                        ForEach(selectedImages.indices, id: \.self) { i in
                            Image(uiImage: selectedImages[i])
                                .resizable().scaledToFill()
                                .frame(height: 120)
                                .clipped()
                        }
                    }
                }
            }
            .navigationTitle(album.name)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Done") { dismiss() }.foregroundColor(.accentCyan)
                }
                ToolbarItem(placement: .primaryAction) {
                    PhotosPicker(selection: $selectedItems, matching: .images) {
                        Image(systemName: "plus").foregroundColor(.accentCyan)
                    }
                }
            }
            .toolbarBackground(Color.bgSurface, for: .navigationBar)
            .toolbarColorScheme(.dark, for: .navigationBar)
        }
        .onChange(of: selectedItems) { items in
            Task {
                for item in items {
                    if let data = try? await item.loadTransferable(type: Data.self),
                       let img = UIImage(data: data) {
                        selectedImages.append(img)
                    }
                }
            }
        }
    }
}

struct AddAlbumSheet: View {
    @Environment(\.dismiss) var dismiss
    @EnvironmentObject var store: AppStore
    @State private var name = ""

    var body: some View {
        NavigationStack {
            ZStack {
                Color.bgPrimary.ignoresSafeArea()
                VStack(spacing: 20) {
                    SectionLabel(text: "Album Name")
                    TextField("e.g. Summer 2025, Birthdays…", text: $name)
                        .textFieldStyle(F4TextFieldStyle())
                    Spacer()
                }
                .padding(.top, 8)
            }
            .navigationTitle("New Album")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }.foregroundColor(.textMuted)
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Create") {
                        let album = PhotoAlbum(name: name)
                        store.albums.append(album)
                        store.persist()
                        dismiss()
                    }
                    .foregroundColor(name.isEmpty ? .textMuted : .accentCyan)
                    .disabled(name.isEmpty)
                }
            }
            .toolbarBackground(Color.bgSurface, for: .navigationBar)
            .toolbarColorScheme(.dark, for: .navigationBar)
        }
    }
}
