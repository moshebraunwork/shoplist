package com.mobrauntech.shoplist.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

const val STATUS_ACTIVE = "active"
const val STATUS_COMPLETE = "complete"
const val STATUS_DELETED = "deleted"

@Entity(tableName = "items")
data class ItemEntity(
    @PrimaryKey val id: String,
    val name: String = "",
    val description: String = "",
    val count: Int = 1,
    val section: String = "Other",
    val imageUrl: String? = null,
    val status: String = STATUS_ACTIVE,
    val completedAt: Long? = null,
    val createdAt: Long = 0,
    val nameUpdatedAt: Long = 0,
    val descUpdatedAt: Long = 0,
    val countUpdatedAt: Long = 0,
    val sectionUpdatedAt: Long = 0,
    val imageUpdatedAt: Long = 0,
    val statusUpdatedAt: Long = 0,
    val updatedAt: Long = 0,
    // Local-only: true when there are unsynced changes for this row.
    val dirty: Boolean = false
)

@Entity(tableName = "sections")
data class SectionEntity(
    @PrimaryKey val name: String,
    val position: Int = 0,
    val updatedAt: Long = 0,
    val dirty: Boolean = false
)
