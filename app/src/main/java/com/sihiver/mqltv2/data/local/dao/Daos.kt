package com.sihiver.mqltv2.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sihiver.mqltv2.data.local.entity.ChannelEntity
import com.sihiver.mqltv2.data.local.entity.EpgEntity
import com.sihiver.mqltv2.data.local.entity.FavoriteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChannelDao {
    @Query("SELECT * FROM channels ORDER BY id")
    fun observeAll(): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels ORDER BY id")
    suspend fun getAll(): List<ChannelEntity>

    @Query("SELECT * FROM channels WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): ChannelEntity?

    @Query("SELECT * FROM channels WHERE category = :category ORDER BY id")
    suspend fun getByCategory(category: String): List<ChannelEntity>

    @Query(
        """
        SELECT * FROM channels
        WHERE name LIKE '%' || :query || '%'
           OR category LIKE '%' || :query || '%'
           OR program LIKE '%' || :query || '%'
        ORDER BY id
        """,
    )
    suspend fun search(query: String): List<ChannelEntity>

    @Query("SELECT COUNT(*) FROM channels")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(channels: List<ChannelEntity>)

    @Query("DELETE FROM channels")
    suspend fun deleteAll()
}

@Dao
interface FavoriteDao {
    @Query("SELECT channelId FROM favorites ORDER BY addedAt")
    fun observeIds(): Flow<List<Int>>

    @Query("SELECT channelId FROM favorites ORDER BY addedAt")
    suspend fun getIds(): List<Int>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE channelId = :channelId)")
    suspend fun isFavorite(channelId: Int): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE channelId = :channelId")
    suspend fun delete(channelId: Int)

    @Query("DELETE FROM favorites")
    suspend fun deleteAll()
}

@Dao
interface EpgDao {
    @Query("SELECT * FROM epg_programs WHERE channelId = :channelId ORDER BY time")
    suspend fun getForChannel(channelId: Int): List<EpgEntity>

    @Query("SELECT * FROM epg_programs ORDER BY channelId, time")
    fun observeAll(): Flow<List<EpgEntity>>

    @Query("SELECT * FROM epg_programs ORDER BY channelId, time")
    suspend fun getAll(): List<EpgEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(programs: List<EpgEntity>)

    @Query("DELETE FROM epg_programs")
    suspend fun deleteAll()

    @Query("DELETE FROM epg_programs WHERE channelId = :channelId")
    suspend fun deleteForChannel(channelId: Int)
}
