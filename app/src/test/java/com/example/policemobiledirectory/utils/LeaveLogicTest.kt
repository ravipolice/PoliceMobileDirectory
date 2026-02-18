package com.example.policemobiledirectory.utils

import com.example.policemobiledirectory.model.Employee
import com.example.policemobiledirectory.model.LeaveBalance
import com.example.policemobiledirectory.model.LeaveEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LeaveLogicTest {

    @Test
    fun `CL balance deduction works`() {
        val balance = LeaveBalance(clRemaining = 15.0)
        val entry = LeaveEntry(leaveType = "CL", totalDays = 3.0)
        val updated = LeaveBalanceCalculator.applyLeave(balance, entry)
        assertEquals(12.0, updated.clRemaining, 0.001)
    }

    @Test
    fun `CL max 7 days rule works`() {
        val employee = Employee(gender = "Male")
        val balance = LeaveBalance(clRemaining = 15.0)
        val entry = LeaveEntry(leaveType = "CL", totalDays = 8.0)
        val result = LeaveValidationEngine.validateLeave(employee, balance, entry, 0)
        assertTrue(result.isFailure)
        assertEquals("Casual Leave cannot exceed 7 days at a time", result.exceptionOrNull()?.message)
    }

    @Test
    fun `ML gender check works`() {
        val employee = Employee(gender = "Male")
        val balance = LeaveBalance()
        val entry = LeaveEntry(leaveType = "ML", totalDays = 180.0)
        val result = LeaveValidationEngine.validateLeave(employee, balance, entry, 0)
        assertTrue(result.isFailure)
        assertEquals("Leave type not allowed for selected gender", result.exceptionOrNull()?.message)
    }

    @Test
    fun `WO monthly limit works`() {
        val employee = Employee(gender = "Male")
        val balance = LeaveBalance()
        val entry = LeaveEntry(leaveType = "WO", totalDays = 1.0)
        val result = LeaveValidationEngine.validateLeave(employee, balance, entry, 4)
        assertTrue(result.isFailure)
        assertEquals("Monthly Weekly Off limit reached (4)", result.exceptionOrNull()?.message)
    }
}
