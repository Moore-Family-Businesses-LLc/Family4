import SwiftUI

// MARK: - Shopping List
struct ShoppingView: View {
    @EnvironmentObject var store: AppStore
    @State private var showAdd = false
    @State private var newItemName = ""
    @State private var newItemQty = "1"
    @State private var newItemCategory = "General"

    var categorised: [String: [ShoppingItem]] {
        Dictionary(grouping: store.shoppingItems, by: \.category)
    }

    var body: some View {
        NavigationStack {
            ZStack {
                Color.bgPrimary.ignoresSafeArea()
                VStack(spacing: 0) {
                    SectionHeaderView(title: "Shopping List", action: { showAdd = true }, actionLabel: "+ Item")

                    if store.shoppingItems.isEmpty {
                        EmptyStateView(title: "List is empty", subtitle: "Tap + Item to add groceries",
                                       systemIcon: "cart.fill")
                    } else {
                        List {
                            ForEach(categorised.keys.sorted(), id: \.self) { cat in
                                Section(header: Text(cat).foregroundColor(.accentCyan).font(.system(size: 11, weight: .bold)).tracking(1)) {
                                    ForEach(categorised[cat]!) { item in
                                        ShoppingItemRow(item: item)
                                            .listRowBackground(Color.bgSurface)
                                            .listRowSeparator(.hidden)
                                    }
                                    .onDelete { offsets in
                                        let items = categorised[cat]!
                                        for i in offsets { store.deleteShoppingItem(items[i]) }
                                    }
                                }
                            }
                        }
                        .listStyle(.insetGrouped)
                        .scrollContentBackground(.hidden)
                    }
                }
            }
            .sheet(isPresented: $showAdd) { AddShoppingItemSheet() }
            .navigationBarHidden(true)
        }
    }
}

struct ShoppingItemRow: View {
    let item: ShoppingItem
    @EnvironmentObject var store: AppStore

    var body: some View {
        HStack(spacing: 12) {
            Button { store.toggleShoppingItem(item) } label: {
                Image(systemName: item.isChecked ? "checkmark.square.fill" : "square")
                    .font(.system(size: 22))
                    .foregroundColor(item.isChecked ? .accentCyan : .textMuted)
            }
            .buttonStyle(.plain)
            Text(item.name)
                .strikethrough(item.isChecked)
                .foregroundColor(item.isChecked ? .textMuted : .textPrimary)
                .font(.system(size: 15))
            Spacer()
            Text("×\(item.quantity)")
                .font(.system(size: 12))
                .foregroundColor(.textMuted)
        }
        .padding(.vertical, 4)
    }
}

struct AddShoppingItemSheet: View {
    @Environment(\.dismiss) var dismiss
    @EnvironmentObject var store: AppStore
    @State private var name = ""
    @State private var quantity = "1"
    @State private var category = "General"

    let categories = ["General","Produce","Dairy","Meat","Bakery","Frozen","Beverages","Household","Personal Care"]

    var body: some View {
        NavigationStack {
            ZStack {
                Color.bgPrimary.ignoresSafeArea()
                VStack(spacing: 20) {
                    SectionLabel(text: "Item Name")
                    TextField("e.g. Milk, Eggs…", text: $name).textFieldStyle(F4TextFieldStyle())
                    SectionLabel(text: "Quantity")
                    TextField("1", text: $quantity).textFieldStyle(F4TextFieldStyle()).keyboardType(.numberPad)
                    SectionLabel(text: "Category")
                    Picker("Category", selection: $category) {
                        ForEach(categories, id: \.self) { Text($0) }
                    }
                    .pickerStyle(.wheel)
                    .frame(height: 150)
                    .padding(.horizontal, 16)
                    .colorScheme(.dark)
                    Spacer()
                }
                .padding(.top, 8)
            }
            .navigationTitle("Add Item")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancel") { dismiss() }.foregroundColor(.textMuted) }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Add") {
                        let item = ShoppingItem(name: name, quantity: quantity, category: category,
                                               addedBy: store.currentUser.displayName)
                        store.addShoppingItem(item)
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

// MARK: - Chores
struct ChoresView: View {
    @EnvironmentObject var store: AppStore
    @State private var showAdd = false

    var body: some View {
        NavigationStack {
            ZStack {
                Color.bgPrimary.ignoresSafeArea()
                VStack(spacing: 0) {
                    SectionHeaderView(title: "Family Chores", action: { showAdd = true }, actionLabel: "+ Chore")

                    if store.chores.isEmpty {
                        EmptyStateView(title: "No chores assigned", subtitle: "Tap + Chore to assign tasks to family members",
                                       systemIcon: "house.fill")
                    } else {
                        List(store.chores) { chore in
                            ChoreRow(chore: chore)
                                .listRowBackground(Color.bgPrimary)
                                .listRowSeparator(.hidden)
                                .listRowInsets(EdgeInsets(top: 4, leading: 16, bottom: 4, trailing: 16))
                                .swipeActions(edge: .trailing) {
                                    Button(role: .destructive) { store.deleteChore(chore) } label: {
                                        Label("Delete", systemImage: "trash")
                                    }
                                }
                        }
                        .listStyle(.plain)
                        .scrollContentBackground(.hidden)
                    }
                }
            }
            .sheet(isPresented: $showAdd) { AddChoreSheet() }
            .navigationBarHidden(true)
        }
    }
}

struct ChoreRow: View {
    let chore: Chore
    @EnvironmentObject var store: AppStore

    var body: some View {
        HStack(spacing: 12) {
            Text(chore.iconEmoji).font(.system(size: 28))
            VStack(alignment: .leading, spacing: 3) {
                Text(chore.title)
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundColor(chore.isCompleted ? .textMuted : .textPrimary)
                    .strikethrough(chore.isCompleted)
                HStack(spacing: 8) {
                    CyanChip(text: chore.assignedTo.isEmpty ? "Unassigned" : chore.assignedTo)
                    Text("\(chore.pointValue) pts")
                        .font(.system(size: 11))
                        .foregroundColor(.textMuted)
                }
            }
            Spacer()
            Button { store.toggleChore(chore) } label: {
                Image(systemName: chore.isCompleted ? "checkmark.circle.fill" : "circle")
                    .font(.system(size: 24))
                    .foregroundColor(chore.isCompleted ? .accentCyan : .textMuted)
            }
            .buttonStyle(.plain)
        }
        .padding(14)
        .background(Color.bgCard)
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color.cardStroke, lineWidth: 1))
    }
}

struct AddChoreSheet: View {
    @Environment(\.dismiss) var dismiss
    @EnvironmentObject var store: AppStore
    @State private var title = ""
    @State private var assignedTo = ""
    @State private var points = 10
    @State private var emoji = "🧹"

    let emojis = ["🧹","🧺","🍳","🌱","🐕","🚗","📚","🗑️","🪟","🛒"]

    var body: some View {
        NavigationStack {
            ZStack {
                Color.bgPrimary.ignoresSafeArea()
                VStack(spacing: 16) {
                    SectionLabel(text: "Chore Name")
                    TextField("e.g. Wash dishes, Mow lawn…", text: $title).textFieldStyle(F4TextFieldStyle())
                    SectionLabel(text: "Assign To")
                    TextField("Family member name", text: $assignedTo).textFieldStyle(F4TextFieldStyle())
                    SectionLabel(text: "Icon")
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 12) {
                            ForEach(emojis, id: \.self) { e in
                                Text(e).font(.system(size: 28))
                                    .padding(8)
                                    .background(emoji == e ? Color.accentCyan.opacity(0.2) : Color.clear)
                                    .clipShape(RoundedRectangle(cornerRadius: 8))
                                    .onTapGesture { emoji = e }
                            }
                        }
                        .padding(.horizontal, 16)
                    }
                    SectionLabel(text: "Points: \(points)")
                    Slider(value: Binding(get: { Double(points) }, set: { points = Int($0) }), in: 5...50, step: 5)
                        .accentColor(.accentCyan)
                        .padding(.horizontal, 16)
                    Spacer()
                }
                .padding(.top, 8)
            }
            .navigationTitle("New Chore")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancel") { dismiss() }.foregroundColor(.textMuted) }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Add") {
                        store.addChore(Chore(title: title, assignedTo: assignedTo, pointValue: points, iconEmoji: emoji))
                        dismiss()
                    }
                    .foregroundColor(title.isEmpty ? .textMuted : .accentCyan)
                    .disabled(title.isEmpty)
                }
            }
            .toolbarBackground(Color.bgSurface, for: .navigationBar)
            .toolbarColorScheme(.dark, for: .navigationBar)
        }
    }
}

// MARK: - Polls
struct PollsView: View {
    @EnvironmentObject var store: AppStore
    @State private var showAdd = false

    var body: some View {
        NavigationStack {
            ZStack {
                Color.bgPrimary.ignoresSafeArea()
                VStack(spacing: 0) {
                    SectionHeaderView(title: "Family Polls", action: { showAdd = true }, actionLabel: "+ Poll")
                    if store.polls.isEmpty {
                        EmptyStateView(title: "No polls yet", subtitle: "Tap + Poll to ask the family something",
                                       systemIcon: "chart.bar.fill")
                    } else {
                        ScrollView {
                            LazyVStack(spacing: 12) {
                                ForEach(store.polls) { poll in
                                    PollCard(poll: poll)
                                }
                            }
                            .padding(16)
                        }
                    }
                }
            }
            .sheet(isPresented: $showAdd) { AddPollSheet() }
            .navigationBarHidden(true)
        }
    }
}

struct PollCard: View {
    let poll: FamilyPoll
    @EnvironmentObject var store: AppStore

    var body: some View {
        SurfaceCard {
            VStack(alignment: .leading, spacing: 12) {
                Text(poll.question)
                    .font(.system(size: 16, weight: .bold))
                    .foregroundColor(.textPrimary)

                ForEach(poll.options, id: \.self) { option in
                    let votes = (poll.votes[option]?.count ?? 0)
                    let total = poll.votes.values.map { $0.count }.reduce(0, +)
                    let pct = total > 0 ? Double(votes) / Double(total) : 0

                    Button { store.vote(pollId: poll.id, option: option, memberId: store.currentUser.id) } label: {
                        VStack(alignment: .leading, spacing: 5) {
                            HStack {
                                Text(option)
                                    .font(.system(size: 14))
                                    .foregroundColor(.textPrimary)
                                Spacer()
                                Text("\(votes) vote\(votes == 1 ? "" : "s")")
                                    .font(.system(size: 12))
                                    .foregroundColor(.textMuted)
                            }
                            GeometryReader { geo in
                                ZStack(alignment: .leading) {
                                    RoundedRectangle(cornerRadius: 3).fill(Color.bgElevated).frame(height: 6)
                                    RoundedRectangle(cornerRadius: 3)
                                        .fill(Color.accentCyan)
                                        .frame(width: geo.size.width * pct, height: 6)
                                }
                            }
                            .frame(height: 6)
                        }
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(16)
        }
    }
}

struct AddPollSheet: View {
    @Environment(\.dismiss) var dismiss
    @EnvironmentObject var store: AppStore
    @State private var question = ""
    @State private var options = ["", ""]

    var body: some View {
        NavigationStack {
            ZStack {
                Color.bgPrimary.ignoresSafeArea()
                ScrollView {
                    VStack(spacing: 16) {
                        SectionLabel(text: "Question")
                        TextField("Ask the family…", text: $question).textFieldStyle(F4TextFieldStyle())
                        SectionLabel(text: "Options")
                        ForEach(options.indices, id: \.self) { i in
                            TextField("Option \(i + 1)", text: $options[i]).textFieldStyle(F4TextFieldStyle())
                        }
                        Button("+ Add Option") {
                            if options.count < 6 { options.append("") }
                        }
                        .foregroundColor(.accentCyan)
                        Spacer()
                    }
                    .padding(.top, 8)
                }
            }
            .navigationTitle("New Poll")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancel") { dismiss() }.foregroundColor(.textMuted) }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Create") {
                        let opts = options.filter { !$0.isEmpty }
                        guard !question.isEmpty && opts.count >= 2 else { return }
                        store.addPoll(FamilyPoll(question: question, options: opts, createdBy: store.currentUser.id))
                        dismiss()
                    }
                    .foregroundColor(question.isEmpty ? .textMuted : .accentCyan)
                }
            }
            .toolbarBackground(Color.bgSurface, for: .navigationBar)
            .toolbarColorScheme(.dark, for: .navigationBar)
        }
    }
}

// MARK: - Bedtime Alerts
struct BedtimeView: View {
    @EnvironmentObject var store: AppStore
    @State private var showAdd = false

    var body: some View {
        NavigationStack {
            ZStack {
                Color.bgPrimary.ignoresSafeArea()
                VStack(spacing: 0) {
                    SectionHeaderView(title: "Bedtime Alerts", action: { showAdd = true }, actionLabel: "+ Alert")
                    if store.bedtimeAlerts.isEmpty {
                        EmptyStateView(title: "No bedtime alerts", subtitle: "Tap + Alert to set a schedule",
                                       systemIcon: "moon.fill")
                    } else {
                        List(store.bedtimeAlerts) { alert in
                            BedtimeRow(alert: alert)
                                .listRowBackground(Color.bgPrimary)
                                .listRowSeparator(.hidden)
                        }
                        .listStyle(.plain)
                        .scrollContentBackground(.hidden)
                    }
                }
            }
            .sheet(isPresented: $showAdd) { AddBedtimeSheet() }
            .navigationBarHidden(true)
        }
    }
}

struct BedtimeRow: View {
    let alert: BedtimeAlert
    var body: some View {
        SurfaceCard {
            HStack(spacing: 14) {
                Text("🌙").font(.system(size: 28))
                VStack(alignment: .leading, spacing: 4) {
                    Text(alert.memberId.isEmpty ? "Family" : alert.memberId)
                        .font(.system(size: 15, weight: .bold))
                        .foregroundColor(.textPrimary)
                    Text(String(format: "Bedtime: %02d:%02d  ·  Wake: %02d:%02d",
                                alert.bedtimeHour, alert.bedtimeMinute,
                                alert.wakeHour, alert.wakeMinute))
                        .font(.system(size: 13))
                        .foregroundColor(.textMuted)
                }
                Spacer()
                Image(systemName: alert.isEnabled ? "checkmark.circle.fill" : "circle")
                    .foregroundColor(alert.isEnabled ? .accentCyan : .textMuted)
            }
            .padding(14)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 4)
    }
}

struct AddBedtimeSheet: View {
    @Environment(\.dismiss) var dismiss
    @EnvironmentObject var store: AppStore
    @State private var memberId = ""
    @State private var bedtimeHour = 21
    @State private var wakeHour = 7

    var body: some View {
        NavigationStack {
            ZStack {
                Color.bgPrimary.ignoresSafeArea()
                VStack(spacing: 16) {
                    SectionLabel(text: "Family Member")
                    TextField("Name or All Family", text: $memberId).textFieldStyle(F4TextFieldStyle())
                    SectionLabel(text: "Bedtime Hour: \(bedtimeHour):00")
                    Slider(value: Binding(get: { Double(bedtimeHour) }, set: { bedtimeHour = Int($0) }), in: 18...23, step: 1)
                        .accentColor(.accentPurple).padding(.horizontal, 16)
                    SectionLabel(text: "Wake Hour: \(wakeHour):00")
                    Slider(value: Binding(get: { Double(wakeHour) }, set: { wakeHour = Int($0) }), in: 5...10, step: 1)
                        .accentColor(.accentCyan).padding(.horizontal, 16)
                    Spacer()
                }
                .padding(.top, 8)
            }
            .navigationTitle("New Bedtime Alert")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancel") { dismiss() }.foregroundColor(.textMuted) }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") {
                        store.bedtimeAlerts.append(BedtimeAlert(memberId: memberId, bedtimeHour: bedtimeHour, wakeHour: wakeHour))
                        store.persist()
                        dismiss()
                    }
                    .foregroundColor(.accentCyan)
                }
            }
            .toolbarBackground(Color.bgSurface, for: .navigationBar)
            .toolbarColorScheme(.dark, for: .navigationBar)
        }
    }
}

// MARK: - Parent Zone
struct ParentZoneView: View {
    @EnvironmentObject var store: AppStore

    var children: [FamilyMember] { store.members.filter { $0.role == "CHILD" } }

    var body: some View {
        NavigationStack {
            ZStack {
                Color.bgPrimary.ignoresSafeArea()
                VStack(spacing: 0) {
                    SectionHeaderView(title: "Parent Zone")
                    ScrollView {
                        VStack(spacing: 16) {
                            SectionLabel(text: "Children")
                            if children.isEmpty {
                                EmptyStateView(title: "No child accounts", subtitle: "Add family members with the CHILD role",
                                               systemIcon: "person.2.fill")
                            } else {
                                ForEach(children) { child in
                                    SurfaceCard {
                                        HStack(spacing: 14) {
                                            AvatarView(name: child.displayName, size: 44)
                                            VStack(alignment: .leading, spacing: 4) {
                                                Text(child.displayName)
                                                    .font(.system(size: 15, weight: .bold))
                                                    .foregroundColor(.textPrimary)
                                                Text(child.isOnline ? "Online" : "Offline")
                                                    .font(.system(size: 12))
                                                    .foregroundColor(child.isOnline ? .onlineGreen : .textMuted)
                                            }
                                            Spacer()
                                            VStack(alignment: .trailing, spacing: 4) {
                                                CyanChip(text: "120 min / day")
                                                Text("Screen Time Limit")
                                                    .font(.system(size: 10))
                                                    .foregroundColor(.textMuted)
                                            }
                                        }
                                        .padding(14)
                                    }
                                    .padding(.horizontal, 16)
                                }
                            }

                            SectionLabel(text: "Safe Zones")
                            SurfaceCard {
                                VStack(alignment: .leading, spacing: 8) {
                                    ForEach(store.safeZones) { zone in
                                        HStack {
                                            Circle().fill(Color(hex: zone.colorHex)).frame(width: 10, height: 10)
                                            Text(zone.name).font(.system(size: 14)).foregroundColor(.textPrimary)
                                            Spacer()
                                            Text("\(Int(zone.radiusMeters))m").font(.system(size: 12)).foregroundColor(.textMuted)
                                        }
                                    }
                                    if store.safeZones.isEmpty {
                                        Text("No safe zones defined")
                                            .font(.system(size: 13))
                                            .foregroundColor(.textMuted)
                                    }
                                }
                                .padding(16)
                                .frame(maxWidth: .infinity, alignment: .leading)
                            }
                            .padding(.horizontal, 16)

                            Spacer(minLength: 32)
                        }
                        .padding(.top, 8)
                    }
                }
            }
            .navigationBarHidden(true)
        }
    }
}
