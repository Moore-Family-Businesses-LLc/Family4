package com.family4.app.auto

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Android Auto Dashboard — shows a greeting and quick-action rows.
 */
class DashboardScreen(carContext: CarContext) : Screen(carContext) {

    override fun onGetTemplate(): Template {
        val time = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())

        val items = ItemList.Builder()
            .addItem(
                Row.Builder()
                    .setTitle("Family4 — $time")
                    .addText("Welcome back! Your family is connected.")
                    .build()
            )
            .addItem(
                Row.Builder()
                    .setTitle("Family Members")
                    .addText("See where everyone is")
                    .setOnClickListener { screenManager.push(FamilyMembersScreen(carContext)) }
                    .build()
            )
            .addItem(
                Row.Builder()
                    .setTitle("Messages")
                    .addText("Check family chat")
                    .setOnClickListener { screenManager.push(ChatScreen(carContext)) }
                    .build()
            )
            .build()

        return ListTemplate.Builder()
            .setTitle("Family4")
            .setHeaderAction(Action.APP_ICON)
            .setSingleList(items)
            .build()
    }
}
