package com.jgeig001.kigga.ui.view.fragmentPages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.jgeig001.kigga.R
import com.jgeig001.kigga.databinding.FragmentHomeBinding

import com.jgeig001.kigga.viewmodel.HomeViewModel
import com.jgeig001.kigga.model.domain.ModelWrapper
import com.jgeig001.kigga.ui.view.ViewModelFactory


class HomeFragment : Fragment() {

    private val ARG_USER = "model"

    private lateinit var model: ModelWrapper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            this.model = it.get(ARG_USER) as ModelWrapper
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val viewModel =
            ViewModelProviders.of(this, ViewModelFactory(this.model))[HomeViewModel::class.java]

        // Obtain binding
        val binding: FragmentHomeBinding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_home, container, false)

        // Bind layout with ViewModel
        binding.homeviewmodel = viewModel

        // LiveData needs the lifecycle owner
        binding.lifecycleOwner = this

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param model Parameter.
         * @return A new instance of fragment HomeFragmentX.
         */
        @JvmStatic
        fun newInstance(model: ModelWrapper) =
            HomeFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_USER, model)
                }
            }
    }

}