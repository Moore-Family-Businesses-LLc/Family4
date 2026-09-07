package com.family4.app.auto

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.*

/**
 * Android Auto Chat screen — shows recent messages as a list.
 * Tapping a message shows options to reply via voice or like.
 */
class ChatScreen(carContext: CarContext) : Screen(carContext) {

    override fun onGetTemplate(): Template {
        val items = ItemList.Builder()
            .addItem(
                Row.Builder()
                    .setTitle("Family Chat")
                    .addText("Open the Family4 app to send messages")
                    .build()
            )
            .setNoItemsMessage("No recent messages")
            .build()

        return ListTemplate.Builder()
            .setTitle("Family Messages")
            .setHeaderAction(Action.BACK)
            .setSingleList(items)
            .build()
    }
}
