package com.jgeig001.kigga.ui.table

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jgeig001.kigga.R
import com.jgeig001.kigga.databinding.FragmentTableBinding
import com.jgeig001.kigga.model.domain.History
import com.jgeig001.kigga.model.domain.ModelWrapper
import com.jgeig001.kigga.model.domain.Season
import com.jgeig001.kigga.ui.home.SeasonStatsViewHolder
import com.jgeig001.kigga.utils.SeasonSelect
import com.jgeig001.kigga.utils.SharedPreferencesManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_table.*
import javax.inject.Inject

@AndroidEntryPoint
class TableFragment : Fragment() {

    // TODO: reload table data

    @Inject
    lateinit var model: ModelWrapper

    private val tableViewModel: TableViewModel by viewModels()

    private lateinit var binding: FragmentTableBinding

    private lateinit var recyclerView: RecyclerView
    lateinit var tableAdapter: TableAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        // Obtain binding
        this.binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_table, container, false)

        // Bind layout with ViewModel
        this.binding.viewModel = this.tableViewModel

        // LiveData needs the lifecycle owner
        this.binding.lifecycleOwner = this

        return this.binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val selectedSeasonIndex = SeasonSelect.getSelectedSeasonIndex(requireContext())

        model.get_nth_season(selectedSeasonIndex)?.getTable()?.let { table ->
            tableAdapter =
                TableAdapter(table, model.getFavouriteClub(requireContext()))
            this.recyclerView = table_recyclerview
            this.recyclerView.setHasFixedSize(true)
            this.recyclerView.layoutManager = LinearLayoutManager(requireContext())
            this.recyclerView.adapter = tableAdapter
        }

        this.setupSpinner()

        tableViewModel.tableLiveData.observe(
            viewLifecycleOwner,
            Observer { table ->
                table?.let { t ->
                    tableAdapter.updateData(t)
                }
            }
        )
    }

    private fun setupSpinner() {
        val seasonAdapter: ArrayAdapter<Season> = ArrayAdapter(
            this.requireActivity(),
            android.R.layout.simple_spinner_item,
            this.model.getListOfSeasons()
        )
        seasonAdapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line)
        table_season_spinner.adapter = seasonAdapter
        table_season_spinner.setSelection(this.tableViewModel.getSelectedSeasonIndex())
        table_season_spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                tableViewModel.setSelectedSeasonIndex(position)
                SeasonSelect.setSelectedSeasonIndex(requireContext(), position)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                tableViewModel.setSelectedSeasonIndex(0)
            }
        }
    }

}