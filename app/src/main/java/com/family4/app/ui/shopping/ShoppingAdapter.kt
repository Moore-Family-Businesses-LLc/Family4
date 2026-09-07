package com.family4.app.ui.shopping

import android.graphics.Paint
import android.view.*
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.family4.app.R
import com.family4.app.data.db.entity.ShoppingItemEntity

class ShoppingAdapter(
    private val onToggle: (ShoppingItemEntity) -> Unit,
    private val onDelete: (ShoppingItemEntity) -> Unit
) : ListAdapter<ShoppingItemEntity, ShoppingAdapter.VH>(DIFF) {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val cb     : CheckBox = view.findViewById(R.id.cbShoppingItem)
        val tvName : TextView = view.findViewById(R.id.tvShoppingItemName)
        val tvQty  : TextView = view.findViewById(R.id.tvShoppingQty)
        val tvCat  : TextView = view.findViewById(R.id.tvShoppingCategory)
        val btnDel : View     = view.findViewById(R.id.btnDeleteShoppingItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_shopping, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        holder.cb.isChecked = item.isChecked
        holder.tvName.text  = item.name
        holder.tvQty.text   = "×${item.quantity}"
        holder.tvCat.text   = item.category
        // Strike-through checked items
        if (item.isChecked) {
            holder.tvName.paintFlags = holder.tvName.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            holder.tvName.alpha = 0.4f
        } else {
            holder.tvName.paintFlags = holder.tvName.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            holder.tvName.alpha = 1f
        }
        holder.cb.setOnCheckedChangeListener(null)
        holder.cb.setOnCheckedChangeListener { _, _ -> onToggle(item) }
        holder.btnDel.setOnClickListener { onDelete(item) }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<ShoppingItemEntity>() {
            override fun areItemsTheSame(a: ShoppingItemEntity, b: ShoppingItemEntity) = a.id == b.id
            override fun areContentsTheSame(a: ShoppingItemEntity, b: ShoppingItemEntity) = a == b
        }
    }
}
