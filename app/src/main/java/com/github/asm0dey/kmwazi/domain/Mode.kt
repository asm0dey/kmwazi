package com.github.asm0dey.kmwazi.domain

sealed interface Mode {
    data object ChooseOne : Mode
    data class SplitIntoGroups(val groupSize: Int) : Mode
    data object DefineOrder : Mode
}

sealed interface Result {
    data class One(val winnerId: Long) : Result
    data class Groups(val groups: List<List<Long>>) : Result
    data class Order(val order: List<Long>) : Result
}