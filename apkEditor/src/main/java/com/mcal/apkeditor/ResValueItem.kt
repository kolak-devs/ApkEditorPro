package com.mcal.apkeditor

import android.view.LayoutInflater
import android.view.ViewGroup
import com.mcal.apkeditor.databinding.ItemStringvalueStaticBinding
import com.mikepenz.fastadapter.binding.AbstractBindingItem

class ResValueItem : AbstractBindingItem<ItemStringvalueStaticBinding>() {
    private var strKey: String? = null
    private var strValue: String? = null

    /** The type of the Item. Can be a hardcoded INT, but preferred is a defined id */
    override val type: Int
        get() = R.id.string_value

    /**
     * This method is called by generateView(Context ctx), generateView(Context ctx, ViewGroup parent) and getViewHolder(ViewGroup parent)
     * it will generate the ViewBinding. You have to provide the correct binding class.
     */
    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ItemStringvalueStaticBinding {
        return ItemStringvalueStaticBinding.inflate(inflater, parent, false)
    }

    override fun bindView(binding: ItemStringvalueStaticBinding, payloads: List<Any>) {
        binding.stringValue.text = strValue
        binding.stringName.text = strKey
    }

}