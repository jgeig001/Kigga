package com.jgeig001.kigga.ui.bet

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jgeig001.kigga.R
import com.jgeig001.kigga.databinding.FragmentBetBinding
import com.jgeig001.kigga.model.domain.*
import com.jgeig001.kigga.utils.SharedPreferencesManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_bet.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
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
        this.setupSpinner()

        // setup
        this.recyclerView = recyclerViewID
        this.recyclerView.setHasFixedSize(true)
        this.layoutManager = LinearLayoutManager(this.context)
        this.betAdapter =
            BetAdapter(this.viewModel.getMatchdayList(), model.getHistory(), requireContext())

        this.recyclerView.layoutManager = this.layoutManager
        this.recyclerView.adapter = this.betAdapter

        this.scrollToCurMatchday()

        model.getHistory().firstLoadFinishedCallback {
            Log.d("123", "# -> callback {...}")
            viewModel.updateLiveDataList(0)
            betAdapter.afterFirstLoadDone(viewModel.getMatchdayList())
            GlobalScope.launch(Dispatchers.Main) {
                setupSpinner()
                scrollToCurMatchday()
            }
        }

        for (livedata in viewModel.liveDataList) {
            livedata.observe(
                viewLifecycleOwner,
                Observer {
                    if (it != null)
                        betAdapter.refreshData(it)
                }
            )
        }

    }

    /**
     * scroll recyclerView to the current matchday
     */
    private fun scrollToCurMatchday() {
        var i = 0
        // TODO: latest season or cur selected season
        this.model.getCurSeason()?.let { curSeason ->
            for (matchday in curSeason.getMatchdays()) {
                if (matchday == this.model.getHistory()
                        .getFirstMatchdayWithMissingResults()!!.second
                ) {
                    Log.d("123", "scrollTo: $i")
                    this.recyclerView.layoutManager!!.scrollToPosition(i)
                    break
                }
                i += 1
            }
        }
    }

    private fun setupSpinner() {
        val seasonAdapter: ArrayAdapter<Season> = ArrayAdapter(
            this.requireActivity(),
            android.R.layout.simple_spinner_item,
            this.model.getListOfSeasons()
        )
        seasonAdapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line)
        season_spinner.adapter = seasonAdapter
        season_spinner.setSelection(this.viewModel.getSelectedSeasonIndex())
        season_spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                viewModel.setSelectedSeasonIndex(position)
                SharedPreferencesManager.writeInt(
                    requireContext(),
                    History.SELECTED_SEASON_SP_KEY,
                    position
                )
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                viewModel.setSelectedSeasonIndex(0)
            }
        }
    }

}
