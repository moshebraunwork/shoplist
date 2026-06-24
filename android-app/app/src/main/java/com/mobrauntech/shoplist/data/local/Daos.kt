package com.mobrauntech.shoplist.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {

    @Query("SELECT * FROM items WHERE status = 'active' ORDER BY createdAt ASC")
    fun activeItems(): Flow<List<ItemEntity>>

    // Completed + deleted, most recently finished first.
    @Query("SELECT * FROM items WHERE status IN ('complete','deleted') ORDER BY completedAt DESC")
    fun finishedItems(): Flow<List<ItemEntity>>

    @Query("SELECT COUNT(*) FROM items WHERE dirty = 1")
    fun dirtyCount(): Flow<Int>

    @Query("SELECT * FROM items WHERE dirty = 1")
    suspend fun getDirty(): List<ItemEntity>

    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun getById(id: String): ItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<ItemEntity>)

    @Query("UPDATE items SET dirty = 0 WHERE id IN (:ids)")
    suspend fun clearDirty(ids: List<String>)
}

@Dao
interface SectionDao {

    @Query("SELECT * FROM sections ORDER BY position ASC")
    fun sections(): Flow<List<SectionEntity>>

    @Query("SELECT * FROM sections WHERE dirty = 1")
    suspend fun getDirty(): List<SectionEntity>

    @Query("SELECT * FROM sections WHERE name = :name")
    suspend fun getByName(name: String): SectionEntity?

    @Query("SELECT COALESCE(MAX(position), 0) FROM sections")
    suspend fun maxPosition(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(section: SectionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(sections: List<SectionEntity>)

    @Query("UPDATE sections SET dirty = 0 WHERE name IN (:names)")
    suspend fun clearDirty(names: List<String>)
}
