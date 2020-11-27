package com.jgeig001.kigga.utils

object NameChanger {

    private val map = mapOf("RBL" to "RB Leipzig")

    fun needChange(name: String): Boolean {
        return name in map
    }

    fun doChange(name: String): String {
        return map[name] ?: error("no name substitution available")
    }

}