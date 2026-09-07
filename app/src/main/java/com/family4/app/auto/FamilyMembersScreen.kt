package com.family4.app.auto

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.*

/**
 * Android Auto Family Members screen — lists family members as a simple ItemList.
 */
class FamilyMembersScreen(carContext: CarContext) : Screen(carContext) {

    override fun onGetTemplate(): Template {
        val items = ItemList.Builder()
            .addItem(
                Row.Builder()
                    .setTitle("Family Members")
                    .addText("Location sharing enabled")
                    .build()
            )
            .setNoItemsMessage("No family members added yet")
            .build()

        return ListTemplate.Builder()
            .setTitle("Family Members")
            .setHeaderAction(Action.BACK)
            .setSingleList(items)
            .build()
    }
}
