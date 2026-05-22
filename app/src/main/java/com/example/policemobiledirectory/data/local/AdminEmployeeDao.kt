package com.example.policemobiledirectory.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AdminEmployeeDao {

    @Query("SELECT * FROM admin_employees ORDER BY name ASC")
    fun getAllEmployees(): Flow<List<AdminEmployeeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmployee(employee: AdminEmployeeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmployees(employees: List<AdminEmployeeEntity>)

    @Update
    suspend fun updateEmployee(employee: AdminEmployeeEntity)
    
    @Query("UPDATE admin_employees SET pin = :newPin WHERE email = :email")
    suspend fun updatePinByEmail(email: String, newPin: String)

    @Query("DELETE FROM admin_employees")
    suspend fun clearEmployees()

    @Query("DELETE FROM admin_employees WHERE kgid NOT IN (:kgids)")
    suspend fun deleteStaleEmployees(kgids: List<String>)

    @Query("DELETE FROM admin_employees WHERE kgid = :kgid")
    suspend fun deleteByKgid(kgid: String)

    @Query("SELECT * FROM admin_employees WHERE kgid = :kgid LIMIT 1")
    suspend fun getEmployeeByKgid(kgid: String): AdminEmployeeEntity?

    @Query("SELECT * FROM admin_employees WHERE email = :email LIMIT 1")
    suspend fun getEmployeeByEmail(email: String): AdminEmployeeEntity?

    @Query("SELECT * FROM admin_employees WHERE searchBlob LIKE :query ORDER BY name ASC LIMIT 100")
    fun searchByBlob(query: String): Flow<List<AdminEmployeeEntity>>
}
