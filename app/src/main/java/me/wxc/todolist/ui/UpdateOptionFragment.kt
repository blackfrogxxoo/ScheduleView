package me.wxc.todolist.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import me.wxc.todolist.databinding.FragmentUpdateOptionBinding

class UpdateOptionFragment : DialogFragment() {
    private lateinit var binding: FragmentUpdateOptionBinding
    private var updateOptionBlock: (deleteOption: UpdateOption) -> Unit = {}

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        if (!::binding.isInitialized) {
            binding = FragmentUpdateOptionBinding.inflate(inflater)
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.updateOne.setOnClickListener {
            updateOptionBlock(UpdateOption.ONE)
            dismissAllowingStateLoss()
        }
        binding.updateFrom.setOnClickListener {
            updateOptionBlock(UpdateOption.FROM)
            dismissAllowingStateLoss()
        }
        binding.updateAll.setOnClickListener {
            updateOptionBlock(UpdateOption.ALL)
            dismissAllowingStateLoss()
        }
        binding.cancel.setOnClickListener {
            dismissAllowingStateLoss()
        }
    }

    companion object {
        private const val TAG = "updateOptionBlock"
        fun show(fragment: Fragment, updateOptionBlock: (deleteOption: UpdateOption) -> Unit) {
            UpdateOptionFragment().apply {
                this.updateOptionBlock = updateOptionBlock
            }.show(fragment.childFragmentManager, TAG)
        }
    }

    enum class UpdateOption {
        ONE, FROM, ALL
    }
}