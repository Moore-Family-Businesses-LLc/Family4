import SwiftUI

struct TasksView: View {
    @EnvironmentObject var store: AppStore
    @State private var showAdd = false
    @State private var filterCompleted = false
    @State private var searchText = ""

    var filteredTasks: [TaskItem] {
        var result = store.tasks
        if !searchText.isEmpty {
            result = result.filter { $0.title.localizedCaseInsensitiveContains(searchText) }
        }
        if !filterCompleted {
            result = result.filter { !$0.isCompleted }
        }
        return result.sorted { lhs, rhs in
            if lhs.isCompleted != rhs.isCompleted { return !lhs.isCompleted }
            return lhs.priority > rhs.priority
        }
    }

    var body: some View {
        NavigationStack {
            ZStack {
                Color.bgPrimary.ignoresSafeArea()
                VStack(spacing: 0) {
                    SectionHeaderView(title: "Tasks", action: { showAdd = true }, actionLabel: "+ Task")

                    // Filter / search bar
                    HStack(spacing: 10) {
                        SearchBar(text: $searchText, placeholder: "Search tasks…")
                        Button {
                            withAnimation { filterCompleted.toggle() }
                        } label: {
                            Text(filterCompleted ? "All" : "Active")
                                .font(.system(size: 12, weight: .semibold))
                                .foregroundColor(.bgPrimary)
                                .padding(.horizontal, 12)
                                .padding(.vertical, 7)
                                .background(Color.accentCyan)
                                .clipShape(Capsule())
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 10)
                    .background(Color.bgSurface)

                    if filteredTasks.isEmpty {
                        EmptyStateView(
                            title: filterCompleted ? "No tasks" : "All tasks done! 🎉",
                            subtitle: filterCompleted ? "Tap + to add your first task" : "Tap 'All' to see completed tasks",
                            systemIcon: "checkmark.circle"
                        )
                    } else {
                        List {
                            ForEach(filteredTasks) { task in
                                TaskRow(task: task)
                                    .listRowBackground(Color.bgPrimary)
                                    .listRowSeparator(.hidden)
                                    .listRowInsets(EdgeInsets(top: 4, leading: 16, bottom: 4, trailing: 16))
                                    .swipeActions(edge: .trailing) {
                                        Button(role: .destructive) {
                                            store.deleteTask(task)
                                        } label: {
                                            Label("Delete", systemImage: "trash")
                                        }
                                    }
                            }
                        }
                        .listStyle(.plain)
                        .background(Color.bgPrimary)
                        .scrollContentBackground(.hidden)
                    }
                }
            }
            .sheet(isPresented: $showAdd) { AddTaskSheet() }
            .navigationBarHidden(true)
        }
    }
}

// MARK: - Task Row
struct TaskRow: View {
    let task: TaskItem
    @EnvironmentObject var store: AppStore

    var body: some View {
        HStack(spacing: 0) {
            // Priority strip
            Rectangle()
                .fill(Color(hex: task.priorityColor))
                .frame(width: 4)

            HStack(spacing: 12) {
                // Checkbox
                Button { store.toggleTask(task) } label: {
                    Image(systemName: task.isCompleted ? "checkmark.circle.fill" : "circle")
                        .font(.system(size: 22))
                        .foregroundColor(task.isCompleted ? .accentCyan : .textMuted)
                }
                .buttonStyle(.plain)

                VStack(alignment: .leading, spacing: 4) {
                    Text(task.title)
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundColor(task.isCompleted ? .textMuted : .textPrimary)
                        .strikethrough(task.isCompleted)

                    HStack(spacing: 8) {
                        PriorityBadge(priority: task.priority)
                        if let due = task.dueDate {
                            DatePill(date: due)
                        }
                        if !task.listName.isEmpty && task.listName != "General" {
                            CyanChip(text: task.listName)
                        }
                    }
                }
                Spacer()
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 12)
        }
        .background(Color.bgCard)
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color.cardStroke, lineWidth: 1))
    }
}

// MARK: - Add Task Sheet
struct AddTaskSheet: View {
    @Environment(\.dismiss) var dismiss
    @EnvironmentObject var store: AppStore

    @State private var title = ""
    @State private var description = ""
    @State private var priority: Int = 1
    @State private var dueDate: Date = Date()
    @State private var hasDueDate = false
    @State private var listName = "General"

    var body: some View {
        NavigationStack {
            ZStack {
                Color.bgPrimary.ignoresSafeArea()
                ScrollView {
                    VStack(spacing: 20) {
                        VStack(alignment: .leading, spacing: 6) {
                            SectionLabel(text: "Title")
                            TextField("What needs to be done?", text: $title)
                                .textFieldStyle(F4TextFieldStyle())
                        }

                        VStack(alignment: .leading, spacing: 6) {
                            SectionLabel(text: "Priority")
                            CyanSegmentedPicker(
                                options: [("Low",0),("Normal",1),("High",2),("Urgent",3)],
                                selection: $priority
                            )
                            .padding(.horizontal, 16)
                        }

                        VStack(alignment: .leading, spacing: 6) {
                            Toggle("Due Date", isOn: $hasDueDate)
                                .toggleStyle(SwitchToggleStyle(tint: .accentCyan))
                                .padding(.horizontal, 16)
                                .foregroundColor(.textPrimary)
                            if hasDueDate {
                                DatePicker("", selection: $dueDate, displayedComponents: .date)
                                    .datePickerStyle(.graphical)
                                    .colorScheme(.dark)
                                    .padding(.horizontal, 16)
                                    .accentColor(.accentCyan)
                            }
                        }

                        VStack(alignment: .leading, spacing: 6) {
                            SectionLabel(text: "List")
                            TextField("List name (e.g. Home, Work…)", text: $listName)
                                .textFieldStyle(F4TextFieldStyle())
                        }

                        VStack(alignment: .leading, spacing: 6) {
                            SectionLabel(text: "Notes")
                            TextField("Optional notes…", text: $description, axis: .vertical)
                                .textFieldStyle(F4TextFieldStyle())
                                .lineLimit(3...)
                        }

                        Spacer(minLength: 20)
                    }
                    .padding(.top, 8)
                }
            }
            .navigationTitle("New Task")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }.foregroundColor(.textMuted)
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Add") { save() }
                        .foregroundColor(title.isEmpty ? .textMuted : .accentCyan)
                        .disabled(title.isEmpty)
                }
            }
            .toolbarBackground(Color.bgSurface, for: .navigationBar)
            .toolbarColorScheme(.dark, for: .navigationBar)
        }
    }

    private func save() {
        let task = TaskItem(
            title: title,
            description: description,
            priority: priority,
            dueDate: hasDueDate ? dueDate : nil,
            listName: listName
        )
        store.addTask(task)
        dismiss()
    }
}
