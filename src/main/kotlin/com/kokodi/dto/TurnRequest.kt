package com.kokodi.dto

data class TurnRequest(
    val targetPlayerId: Long? = null  // Only needed for STEAL action
) 