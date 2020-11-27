package com.jgeig001.kigga.ui.table

import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.jgeig001.kigga.model.domain.ModelWrapper
import com.jgeig001.kigga.model.domain.Table

class TableViewModel @ViewModelInject constructor(
    private var model: ModelWrapper,
    @Assisted private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private lateinit var table:Table



}