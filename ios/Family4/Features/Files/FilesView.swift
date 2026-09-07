import SwiftUI

struct FilesView: View {
    @EnvironmentObject var store: AppStore
    @State private var searchText = ""
    @State private var sortBy: SortOption = .date

    enum SortOption: String, CaseIterable { case date = "Date", name = "Name", size = "Size" }

    private let demoFiles: [FileItem] = [
        FileItem(name: "Family Budget 2025.xlsx", size: "48 KB", type: "spreadsheet", date: Date().addingTimeInterval(-86400)),
        FileItem(name: "Vacation Plans.pdf", size: "2.1 MB", type: "pdf", date: Date().addingTimeInterval(-172800)),
        FileItem(name: "School Schedule.docx", size: "124 KB", type: "doc", date: Date().addingTimeInterval(-259200)),
        FileItem(name: "Insurance Docs.pdf", size: "5.8 MB", type: "pdf", date: Date().addingTimeInterval(-604800)),
        FileItem(name: "Family Photo 2024.jpg", size: "3.4 MB", type: "image", date: Date().addingTimeInterval(-1209600)),
    ]

    var filteredFiles: [FileItem] {
        let f = searchText.isEmpty ? demoFiles : demoFiles.filter { $0.name.localizedCaseInsensitiveContains(searchText) }
        switch sortBy {
        case .date: return f.sorted { $0.date > $1.date }
        case .name: return f.sorted { $0.name < $1.name }
        case .size: return f
        }
    }

    var body: some View {
        NavigationStack {
            ZStack {
                Color.bgPrimary.ignoresSafeArea()
                VStack(spacing: 0) {
                    SectionHeaderView(title: "Files")

                    HStack(spacing: 10) {
                        SearchBar(text: $searchText, placeholder: "Search files…")
                        Menu {
                            ForEach(SortOption.allCases, id: \.self) { opt in
                                Button(opt.rawValue) { sortBy = opt }
                            }
                        } label: {
                            Image(systemName: "arrow.up.arrow.down")
                                .foregroundColor(.accentCyan)
                                .font(.system(size: 16))
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 10)
                    .background(Color.bgSurface)

                    if filteredFiles.isEmpty {
                        EmptyStateView(title: "No files", subtitle: "Upload files to share with the family",
                                       systemIcon: "folder.fill")
                    } else {
                        List(filteredFiles) { file in
                            FileRow(file: file)
                                .listRowBackground(Color.bgPrimary)
                                .listRowSeparator(.hidden)
                                .listRowInsets(EdgeInsets(top: 4, leading: 16, bottom: 4, trailing: 16))
                        }
                        .listStyle(.plain)
                        .scrollContentBackground(.hidden)
                    }
                }
            }
            .navigationBarHidden(true)
        }
    }
}

struct FileItem: Identifiable {
    let id = UUID()
    let name: String
    let size: String
    let type: String  // pdf | doc | spreadsheet | image | other
    let date: Date

    var icon: String {
        switch type {
        case "pdf":         return "doc.fill"
        case "doc":         return "doc.text.fill"
        case "spreadsheet": return "tablecells.fill"
        case "image":       return "photo.fill"
        default:            return "doc.fill"
        }
    }

    var iconColor: Color {
        switch type {
        case "pdf":         return .accentRed
        case "doc":         return .accentBlue
        case "spreadsheet": return .accentGreen
        case "image":       return .accentCyan
        default:            return .textMuted
        }
    }
}

struct FileRow: View {
    let file: FileItem

    var body: some View {
        HStack(spacing: 14) {
            Image(systemName: file.icon)
                .font(.system(size: 22))
                .foregroundColor(file.iconColor)
                .frame(width: 44, height: 44)
                .background(file.iconColor.opacity(0.12))
                .clipShape(RoundedRectangle(cornerRadius: 10))

            VStack(alignment: .leading, spacing: 3) {
                Text(file.name)
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(.textPrimary)
                    .lineLimit(1)
                HStack(spacing: 8) {
                    Text(file.size)
                        .font(.system(size: 11))
                        .foregroundColor(.textMuted)
                    Text("·")
                        .foregroundColor(.textMuted)
                    Text(file.date.formatted(date: .abbreviated, time: .omitted))
                        .font(.system(size: 11))
                        .foregroundColor(.textMuted)
                }
            }
            Spacer()
            Image(systemName: "ellipsis")
                .foregroundColor(.textMuted)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 10)
        .background(Color.bgCard)
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color.cardStroke, lineWidth: 1))
    }
}
