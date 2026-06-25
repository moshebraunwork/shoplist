package com.mobrauntech.shoplist.data.remote

import com.mobrauntech.shoplist.data.local.ItemEntity
import com.mobrauntech.shoplist.data.local.SectionEntity

// ---- Items / sections wire format (matches the server columns) ----
data class ItemDto(
    val id: String,
    val name: String = "",
    val description: String = "",
    val count: Int = 1,
    val section: String = "Other",
    val imageUrl: String? = null,
    val status: String = "active",
    val completedAt: Long? = null,
    val createdAt: Long = 0,
    val nameUpdatedAt: Long = 0,
    val descUpdatedAt: Long = 0,
    val countUpdatedAt: Long = 0,
    val sectionUpdatedAt: Long = 0,
    val imageUpdatedAt: Long = 0,
    val statusUpdatedAt: Long = 0,
    val updatedAt: Long = 0
)

data class SectionDto(
    val name: String,
    val position: Int = 0,
    val updatedAt: Long = 0
)

data class SyncRequest(
    val since: Long,
    val items: List<ItemDto>,
    val sections: List<SectionDto>
)

data class SyncResponse(
    val now: Long = 0,
    val items: List<ItemDto> = emptyList(),
    val sections: List<SectionDto> = emptyList()
)

// ---- Suggestions ----
data class SuggestResponse(
    val reuse: List<ItemDto> = emptyList(),
    val duplicates: List<ItemDto> = emptyList()
)

// ---- Images ----
data class ImagesResponse(val images: List<String> = emptyList())

// ---- AI sectioning ----
data class SectionRequest(val name: String, val description: String)
data class SectionResponse(val section: String = "Other")

// ---- Upload ----
data class UploadResponse(val url: String = "")

// ---- Mapping helpers ----
fun ItemEntity.toDto() = ItemDto(
    id, name, description, count, section, imageUrl, status, completedAt, createdAt,
    nameUpdatedAt, descUpdatedAt, countUpdatedAt, sectionUpdatedAt, imageUpdatedAt,
    statusUpdatedAt, updatedAt
)

fun ItemDto.toEntity(dirty: Boolean = false) = ItemEntity(
    id, name, description, count, section, imageUrl, status, completedAt, createdAt,
    nameUpdatedAt, descUpdatedAt, countUpdatedAt, sectionUpdatedAt, imageUpdatedAt,
    statusUpdatedAt, updatedAt, dirty
)

fun SectionEntity.toDto() = SectionDto(name, position, updatedAt)
fun SectionDto.toEntity(dirty: Boolean = false) = SectionEntity(name, position, updatedAt, dirty)
