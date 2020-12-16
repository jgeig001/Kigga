package com.jgeig001.kigga.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.jgeig001.kigga.R
import com.jgeig001.kigga.databinding.FragmentHomeBinding
import com.jgeig001.kigga.model.domain.Matchday
import com.jgeig001.kigga.model.domain.ModelWrapper
import com.jgeig001.kigga.utils.FavClubChooser
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.android.synthetic.main.pie_chart.*
import kotlinx.android.synthetic.main.view_table3.*
import javax.inject.Inject

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private val ARG_USER = "model"

    @Inject
    lateinit var model: ModelWrapper

    private val homeViewModel: HomeViewModel by viewModels()

    lateinit var seasonStatsAdapter: BaseAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            this.model = it.get(ARG_USER) as ModelWrapper
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Obtain binding
        val binding: FragmentHomeBinding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_home, container, false)

        // Bind layout with ViewModel
        binding.viewModel = homeViewModel

        // LiveData needs the lifecycle owner
        binding.lifecycleOwner = this

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        observeLiveData()
        selec_fav_btn.setOnClickListener { chooseFavClub() }
        favouriteClub.text = FavClubChooser.getFavClubName(requireContext())
        setAllTimeValues()
        seasonStatsAdapter = SeasonStatsAdapter(model.getListOfSeasons(), model, requireContext())
    }

    private fun setAllTimeValues() {
        alltimePointsLabel.text = "Alle gesammelten Punkte: ${model.getPointsAllTime()}"
        val matchesAmount = Matchday.MAX_MATCHDAYS * Matchday.MAX_MATCHES
        correct_result_percentage.text =
            (model.getSeasonsCorrectResult(model.getListOfSeasons()) / matchesAmount).toString()
        correct_outcome_percentage.text =
            (model.getSeasonsCorrectOutcome(model.getListOfSeasons()) / matchesAmount).toString()
        wrong_percentage.text =
            (model.getSeasonsWrong(model.getListOfSeasons()) / matchesAmount).toString()
    }

    private fun observeLiveData() {
        homeViewModel.favClubLiveData.observe(
            viewLifecycleOwner,
            Observer {
                if (FavClubChooser.hasFavouriteClub(requireContext())) {
                    selec_fav_btn.visibility = View.INVISIBLE
                } else {
                    selec_fav_btn.visibility = View.VISIBLE
                }
                homeViewModel.updateMiniTable()
                favouriteClub.text = FavClubChooser.getFavClubName(requireContext())
            }
        )
    }

    private fun chooseFavClub() {
        val dialog =
            FavClubChooser.getLiveDataClubChooserDialog(
                requireContext(),
                model.getLiga(),
                homeViewModel.favClubLiveData
            )
        dialog.show()
    }

}