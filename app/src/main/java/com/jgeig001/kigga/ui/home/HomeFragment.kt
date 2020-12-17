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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jgeig001.kigga.R
import com.jgeig001.kigga.databinding.FragmentHomeBinding
import com.jgeig001.kigga.model.domain.BetPoints
import com.jgeig001.kigga.model.domain.Matchday
import com.jgeig001.kigga.model.domain.ModelWrapper
import com.jgeig001.kigga.utils.FavClubChooser
import com.jgeig001.kigga.utils.FloatRounder.round2D
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.android.synthetic.main.pie_chart.*
import kotlinx.android.synthetic.main.view_table3.*
import javax.inject.Inject
import kotlin.math.roundToInt

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private val ARG_USER = "model"

    @Inject
    lateinit var model: ModelWrapper

    private val homeViewModel: HomeViewModel by viewModels()

    private lateinit var recyclerView: RecyclerView
    lateinit var seasonStatsAdapter: RecyclerView.Adapter<SeasonStatsViewHolder>

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

        this.recyclerView = seasons_stats_recyclerView
        this.recyclerView.setHasFixedSize(true)
        this.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        this.recyclerView.adapter = seasonStatsAdapter
    }

    private fun setAllTimeValues() {
        alltimePointsLabel.text = "Alle gesammelten Punkte: ${model.getPointsAllTime()}"

        val matchesWithBet = model.matchesWithBetAllTime()
        Matchday.MAX_MATCHDAYS * Matchday.MAX_MATCHES * model.getListOfSeasons().size
        val allSeasonsDistributionMap = model.getAllSeasonsDistributionMap()

        val co = allSeasonsDistributionMap[BetPoints.RIGHT_OUTCOME] ?: 0 / matchesWithBet
        correct_outcome_amount.text = "($co)"
        val co_p = round2D((co.toDouble() / matchesWithBet.toDouble()) * 100)
        correct_outcome_percentage.text = "$co_p%"
        chart_blue.progress = co_p.roundToInt()

        val cr = allSeasonsDistributionMap[BetPoints.RIGHT_RESULT] ?: 0 / matchesWithBet
        correct_result_amount.text = "($cr)"
        val cr_p = round2D((cr.toDouble() / matchesWithBet.toDouble()) * 100)
        correct_result_percentage.text = "$cr_p%"
        chart_green.progress = cr_p.roundToInt() + co_p.roundToInt()

        val wr = allSeasonsDistributionMap[BetPoints.WRONG] ?: 0 / matchesWithBet
        wrong_amount.text = "($wr)"
        val wr_p = round2D((wr.toDouble() / matchesWithBet.toDouble()) * 100)
        wrong_percentage.text = "$wr_p%"
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