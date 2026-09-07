package com.family4.app.ui.shopping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.family4.app.data.db.dao.ShoppingDao
import com.family4.app.data.db.entity.ShoppingItemEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShoppingViewModel @Inject constructor(
    private val shoppingDao: ShoppingDao
) : ViewModel() {

    val items: StateFlow<List<ShoppingItemEntity>> = shoppingDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addItem(name: String, qty: String, category: String) {
        viewModelScope.launch {
            shoppingDao.insert(ShoppingItemEntity(name = name, quantity = qty,
                category = category, addedBy = "self"))
        }
    }

    fun toggleChecked(item: ShoppingItemEntity) {
        viewModelScope.launch { shoppingDao.setChecked(item.id, !item.isChecked) }
    }

    fun deleteItem(item: ShoppingItemEntity) {
        viewModelScope.launch { shoppingDao.delete(item) }
    }

    fun clearChecked() {
        viewModelScope.launch { shoppingDao.clearChecked() }
    }
}
