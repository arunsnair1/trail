package com.caladapt.data.repository

import com.caladapt.data.db.dao.UserProfileDao
import com.caladapt.data.db.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserProfileRepository @Inject constructor(
    private val dao: UserProfileDao
) {
    fun getProfile(): Flow<UserProfileEntity?> = dao.getProfile()

    suspend fun getProfileOnce(): UserProfileEntity? = dao.getProfileOnce()

    suspend fun hasProfile(): Boolean = dao.getCount() > 0

    suspend fun saveProfile(profile: UserProfileEntity): Long = dao.insert(profile)

    suspend fun updateProfile(profile: UserProfileEntity) = dao.update(profile)

    suspend fun clearProfile() = dao.deleteAll()
}
