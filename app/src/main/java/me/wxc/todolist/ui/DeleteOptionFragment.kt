package me.wxc.todolist.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import me.wxc.todolist.databinding.FragmentDeleteOptionBinding

class DeleteOptionFragment : DialogFragment() {
    private lateinit var binding: FragmentDeleteOptionBinding
    private var deleteOptionBlock: (deleteOption: DeleteOption) -> Unit = {}

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        if (!::binding.isInitialized) {
            binding = FragmentDeleteOptionBinding.inflate(inflater)
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.deleteOne.setOnClickListener {
            deleteOptionBlock(DeleteOption.ONE)
            dismissAllowingStateLoss()
        }
        binding.deleteFrom.setOnClickListener {
            deleteOptionBlock(DeleteOption.FROM)
            dismissAllowingStateLoss()
        }
        binding.deleteAll.setOnClickListener {
            deleteOptionBlock(DeleteOption.ALL)
            dismissAllowingStateLoss()
        }
        binding.cancel.setOnClickListener {
            dismissAllowingStateLoss()
        }
    }

    companion object {
        private const val TAG = "deleteOptionBlock"
        fun show(fragment: Fragment, deleteOptionBlock: (deleteOption: DeleteOption) -> Unit) {
            DeleteOptionFragment().apply {
                this.deleteOptionBlock = deleteOptionBlock
            }.show(fragment.childFragmentManager, TAG)
        }
    }

    enum class DeleteOption {
        ONE, FROM, ALL
    }
}