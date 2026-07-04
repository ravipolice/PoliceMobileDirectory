package com.example.policemobiledirectory.utils

import com.example.policemobiledirectory.model.Employee
import com.example.policemobiledirectory.model.Officer
import org.junit.Assert.assertEquals
import org.junit.Test

class EmployeeUtilsTest {

    @Test
    fun `Employee name is returned raw without formatting`() {
        // Employee with AAO rank should NOT have their name formatted
        val employee = Employee(
            name = "Ravi Kumar",
            rank = "AAO",
            unit = "CLM",
            district = "Ramanagara"
        )
        val displayName = getContactDisplayName(employee, null)
        assertEquals("Ravi Kumar", displayName)
    }

    @Test
    fun `Officer name is formatted correctly for AAO range unit`() {
        // Officer with generic AAO name should be formatted to "AAO <District>"
        val officer = Officer(
            name = "AAO",
            rank = "AAO",
            unit = "L&O",
            district = "Ramanagara"
        )
        val displayName = getContactDisplayName(null, officer)
        assertEquals("AAO Ramanagara", displayName)
    }

    @Test
    fun `Officer name is formatted correctly for AAO special unit`() {
        // Officer with generic AAO name in a special unit like CLM
        val officer = Officer(
            name = "AAO",
            rank = "AAO",
            unit = "CLM"
        )
        val displayName = getContactDisplayName(null, officer)
        assertEquals("AAO CLM", displayName)
    }

    @Test
    fun `Officer name is formatted for PSI with clean station and district`() {
        // Officer with name equal to rank "PSI"
        val officer = Officer(
            name = "PSI",
            rank = "PSI",
            station = "Bagalkot Town PS",
            district = "Bagalkot"
        )
        val displayName = getContactDisplayName(null, officer)
        assertEquals("PSI Bagalkot Town Bagalkot", displayName)
    }

    @Test
    fun `Officer name is formatted for CPI circle including district`() {
        // Officer with name equal to rank "CPI" working in a circle where district is different
        val officer = Officer(
            name = "CPI",
            rank = "CPI",
            station = "Malur Circle",
            district = "Kolar"
        )
        val displayName = getContactDisplayName(null, officer)
        assertEquals("CPI Malur Circle Kolar", displayName)
    }

    @Test
    fun `Officer name is formatted when rawName is empty and rank is full rank name`() {
        // Officer with blank name and full rank string "Police Sub-Inspector"
        val officer = Officer(
            name = "",
            rank = "Police Sub-Inspector",
            station = "Abbinahole PS",
            district = "Chitradurga"
        )
        val displayName = getContactDisplayName(null, officer)
        assertEquals("PSI Abbinahole Chitradurga", displayName)
    }

    @Test
    fun `Officer name is formatted with station and district when rawName has dots like DySP`() {
        val officer = Officer(
            name = "Dy.SP",
            rank = "Deputy Superintendent of Police",
            station = "Chitradurga Sub-Division",
            district = "Chitradurga"
        )
        val displayName = getContactDisplayName(null, officer)
        assertEquals("DySP Chitradurga Sub-Division Chitradurga", displayName)
    }

    @Test
    fun `Officer name is formatted with station and district when rawName has spaces like Dy SP`() {
        val officer = Officer(
            name = "Dy. SP",
            rank = "Deputy Superintendent of Police",
            station = "Chitradurga Sub-Division",
            district = "Chitradurga"
        )
        val displayName = getContactDisplayName(null, officer)
        assertEquals("DySP Chitradurga Sub-Division Chitradurga", displayName)
    }

    @Test
    fun `Officer name is formatted with station and district when rawName has dots like PSI`() {
        val officer = Officer(
            name = "P.S.I.",
            rank = "Police Sub-Inspector",
            station = "Bagalkot Town PS",
            district = "Bagalkot"
        )
        val displayName = getContactDisplayName(null, officer)
        assertEquals("PSI Bagalkot Town Bagalkot", displayName)
    }

    @Test
    fun `Officer name is formatted with station and district when rawName is WPSI with dots`() {
        val officer = Officer(
            name = "W.P.S.I.",
            rank = "WPSI",
            station = "Women PS",
            district = "Bagalkot"
        )
        val displayName = getContactDisplayName(null, officer)
        assertEquals("WPSI Women Bagalkot", displayName)
    }

    @Test
    fun `getShortRangeName abbreviates range names correctly`() {
        assertEquals("CR", getShortRangeName("Central Range"))
        assertEquals("NR", getShortRangeName("Northern Range"))
        assertEquals("SR", getShortRangeName("Southern Range"))
        assertEquals("WR", getShortRangeName("Western Range"))
        assertEquals("ER", getShortRangeName("Eastern Range"))
        assertEquals("ER", getShortRangeName("Davangere Range"))
        assertEquals("BR", getShortRangeName("Ballari Range"))
        assertEquals("NER", getShortRangeName("North-Eastern Range"))
        assertEquals("NER", getShortRangeName("Northeastern Range"))
        assertEquals("NER", getShortRangeName("North Eastern Range"))
        assertEquals("Something CR Else", getShortRangeName("Something Central Range Else"))
    }
}
