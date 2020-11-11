package com.jgeig001.kigga.ui.more

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.jgeig001.kigga.R
import com.jgeig001.kigga.databinding.FragmentMoreBinding
import com.jgeig001.kigga.model.domain.ModelWrapper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MoreFragment : Fragment() {

    @Inject
    lateinit var model: ModelWrapper

    private val moreViewModel: MoreViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Obtain binding
        val binding: FragmentMoreBinding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_more, container, false)

        // Bind layout with ViewModel
        binding.settingsviewmodel = moreViewModel

        // LiveData needs the lifecycle owner
        binding.lifecycleOwner = this

        return binding.root
    }

}
