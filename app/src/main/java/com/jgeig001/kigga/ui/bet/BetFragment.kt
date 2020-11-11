package com.jgeig001.kigga.ui.bet

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jgeig001.kigga.R
import com.jgeig001.kigga.databinding.FragmentBetBinding
import com.jgeig001.kigga.model.domain.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_bet.*
import javax.inject.Inject


@AndroidEntryPoint
class BetFragment : Fragment(R.layout.fragment_bet) {

    private lateinit var binding: FragmentBetBinding

    private val viewModel: BetViewModel by viewModels()

    @Inject
    lateinit var model: ModelWrapper

    private lateinit var betAdapter: BetAdapter
    private lateinit var layoutManager: RecyclerView.LayoutManager
    private lateinit var recyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Obtain binding
        this.binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_bet, container, false)

        // Bind layout with ViewModel
        this.binding.viewmodel = this.viewModel

        // LiveData needs the lifecycle owner
        this.binding.lifecycleOwner = this

        // LiveData
        this.binding.recyclerViewID.adapter

        return this.binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // bind spinner to lisOfSeasons
        val spinner: Spinner = view.findViewById(R.id.season_spinner)
        val seasonAdapter: ArrayAdapter<Season> = ArrayAdapter(
            this.requireActivity(),
            android.R.layout.simple_spinner_item,
            this.model.getListOfSeasons()
        )
        seasonAdapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line)
        spinner.adapter = seasonAdapter
        spinner.setSelection(this.viewModel.getSelectedSeasonIndex())
        season_spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                viewModel.setSelectedSeasonIndex(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                viewModel.setSelectedSeasonIndex(0)
            }
        }

        // setup
        this.recyclerView = recyclerViewID
        this.recyclerView.setHasFixedSize(true)
        this.layoutManager = LinearLayoutManager(this.context)
        this.betAdapter =
            BetAdapter(this.viewModel.getMatchdayList(), requireContext())

        this.recyclerView.layoutManager = this.layoutManager
        this.recyclerView.adapter = this.betAdapter


        // scroll recyclerView to the current matchday
        var i = 0
        for (matchday in this.model.getCurSeason()!!.getMatchdays()) {
            if (matchday == this.model.getHistory().getFirstMatchdayWithMissingResults()!!.second) {
                this.recyclerView.layoutManager!!.scrollToPosition(i)
                break
            }
            i += 1
        }

        // observe live data
        for (livedata in viewModel.liveDataList) {
            livedata.observe(
                viewLifecycleOwner,
                Observer {
                    Log.d("123", "observer...")
                    betAdapter.refreshData(it)
                }
            )
        }

    }

}
