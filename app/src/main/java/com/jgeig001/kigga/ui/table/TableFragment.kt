package com.jgeig001.kigga.ui.table

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.jgeig001.kigga.R
import com.jgeig001.kigga.databinding.FragmentTableBinding
import com.jgeig001.kigga.model.domain.ModelWrapper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class TableFragment : Fragment() {

    @Inject
    lateinit var model: ModelWrapper

    private val tableViewModel: TableViewModel by viewModels()

    private lateinit var binding: FragmentTableBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // Obtain binding
        this.binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_table, container, false)

        // Bind layout with ViewModel
        this.binding.viewModel = this.tableViewModel

        // LiveData needs the lifecycle owner
        this.binding.lifecycleOwner = this

        return this.binding.root
    }

}