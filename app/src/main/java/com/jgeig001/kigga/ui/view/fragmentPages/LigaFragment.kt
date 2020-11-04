package com.jgeig001.kigga.ui.view.fragmentPages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jgeig001.kigga.R
import com.jgeig001.kigga.databinding.FragmentLigaBinding
import com.jgeig001.kigga.model.domain.*
import com.jgeig001.kigga.ui.view.LigaAdapter
import com.jgeig001.kigga.ui.view.ViewModelFactory
import com.jgeig001.kigga.viewmodel.LigaViewModel
import kotlinx.android.synthetic.main.fragment_liga.*


class LigaFragment : Fragment() {

    private val ARG_USER = "user"

    private lateinit var model: ModelWrapper

    private lateinit var binding: FragmentLigaBinding

    private lateinit var viewModel: LigaViewModel

    private lateinit var ligaAdapter: LigaAdapter
    private lateinit var layoutManager: RecyclerView.LayoutManager
    private lateinit var recyclerView: RecyclerView


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
        this.viewModel =
            ViewModelProviders.of(this, ViewModelFactory(this.model))[LigaViewModel::class.java]

        // Obtain binding
        this.binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_liga, container, false)

        // Bind layout with ViewModel
        this.binding.ligaviewmodel = this.viewModel

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
        val seasonAdapter:ArrayAdapter<Season> = ArrayAdapter(
            this.activity!!,
            android.R.layout.simple_spinner_item,
            this.model.getListOfSeasons()
        )
        seasonAdapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line)
        spinner.adapter = seasonAdapter
        spinner.setSelection(this.viewModel.getSelectedSeasonIndex())
        season_spinner.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
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
        })

        // setup
        this.recyclerView = recyclerViewID
        this.recyclerView.setHasFixedSize(true)
        this.layoutManager = LinearLayoutManager(this.context)
        this.ligaAdapter = LigaAdapter(this.viewModel, this.model.getBets())

        this.recyclerView.layoutManager = this.layoutManager
        this.recyclerView.adapter = this.ligaAdapter


        // scroll recyclerView to the current matchday
        var i = 0
        for (matchday in this.model.getCurSeason()!!.getMatchdays()) {
            if (matchday == this.model.getHistory().getFirstMatchdayWithMissingResults()!!.second) {
                this.recyclerView.layoutManager!!.scrollToPosition(i)
                break
            }
            i += 1
        }

        // observer
        this.viewModel.matchday_list.observe(this, Observer<ArrayList<Matchday>> {
            ligaAdapter.refreshData(viewModel, model.getBets())
        })

        this.viewModel.bets.observe(this, Observer<HashMap<Match, Bet>> {
            ligaAdapter.refreshData(viewModel, model.getBets())
        })

    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param user Parameter.
         * @return A new instance of fragment LigaFragmentX.
         */
        @JvmStatic
        fun newInstance(model: ModelWrapper) =
            LigaFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_USER, model)
                }
            }
    }

}
