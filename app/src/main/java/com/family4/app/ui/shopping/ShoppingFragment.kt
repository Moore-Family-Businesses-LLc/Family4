package com.family4.app.ui.shopping

import android.os.Bundle
import android.view.*
import android.widget.EditText
import android.widget.Spinner
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.family4.app.R
import com.family4.app.databinding.FragmentShoppingBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ShoppingFragment : Fragment() {

    private var _binding: FragmentShoppingBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ShoppingViewModel by viewModels()
    private lateinit var adapter: ShoppingAdapter

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _binding = FragmentShoppingBinding.inflate(i, c, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ShoppingAdapter(
            onToggle = { item -> viewModel.toggleChecked(item) },
            onDelete = { item -> viewModel.deleteItem(item) }
        )
        binding.rvShopping.layoutManager = LinearLayoutManager(requireContext())
        binding.rvShopping.adapter = adapter

        binding.fabAddItem.setOnClickListener { showAddDialog() }
        binding.btnClearChecked.setOnClickListener { viewModel.clearChecked() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.items.collectLatest { items ->
                adapter.submitList(items)
                binding.tvEmptyShopping.visibility =
                    if (items.isEmpty()) View.VISIBLE else View.GONE
                binding.btnClearChecked.isEnabled = items.any { it.isChecked }
            }
        }
    }

    private fun showAddDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_shopping_item, null)
        val etName  = dialogView.findViewById<EditText>(R.id.etItemName)
        val etQty   = dialogView.findViewById<EditText>(R.id.etItemQty)
        val spCat   = dialogView.findViewById<Spinner>(R.id.spItemCategory)
        val cats = listOf("Produce", "Dairy", "Meat", "Bakery", "Frozen", "Drinks",
            "Snacks", "Cleaning", "Personal Care", "General")
        spCat.adapter = ArrayAdapter(requireContext(),
            android.R.layout.simple_spinner_item, cats).also {
                it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add Item")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val name = etName.text.toString().trim()
                if (name.isNotBlank()) {
                    val qty = etQty.text.toString().trim().ifEmpty { "1" }
                    val cat = cats[spCat.selectedItemPosition]
                    viewModel.addItem(name, qty, cat)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
