package com.jgeig001.kigga.model.domain

/**
 * sometimes matches can not be played of divers reasons
 * to represent the state concerning this use this enum
 */
enum class SuspensionState {
    REGULAR, // regular kickoff
    SUSPENDED, // match got canceled => "abgs."
    RESCHEDULED; // a new kickoff is scheduled => bet view again

    companion object {
        fun getState(i: Int): SuspensionState {
            return when (i) {
                0 -> REGULAR
                1 -> SUSPENDED
                2 -> RESCHEDULED
                else -> RESCHEDULED
            }
        }
    }
}