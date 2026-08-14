package com.homely.smarthome.core.model

import com.google.firebase.Timestamp

data class Home(
    val id: String = "",
    val name: String = "",
    val ownerId: String = "",
    val memberIds: List<String> = emptyList(),
    val timeZone: String = "Asia/Colombo",
    val createdAt: Timestamp? = null,
)

data class Floor(
    val id: String = "",
    val name: String = "",
    val imageUrl: String = "",
    val gridRows: Int = 8,
    val gridColumns: Int = 12,
    val sortOrder: Int = 0,
)

