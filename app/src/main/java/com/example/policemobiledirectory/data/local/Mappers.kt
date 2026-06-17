package com.example.policemobiledirectory.data.local

import com.example.policemobiledirectory.model.Employee
import java.util.Date

/**
 * Extension function to convert PendingRegistrationEntity to Employee.
 */
fun PendingRegistrationEntity.toEmployee(overridePhotoUrl: String? = null): Employee {
    return Employee(
        kgid = this.kgid,
        name = this.name,
        email = this.email,
        pin = this.pin,
        mobile1 = this.mobile1,
        mobile2 = this.mobile2,
        rank = this.rank,
        metalNumber = this.metalNumber,
        district = this.district,
        station = this.station,
        unit = this.unit,
        bloodGroup = this.bloodGroup,
        photoUrl = overridePhotoUrl ?: this.photoUrl,
        photoUrlFromGoogle = this.photoUrlFromGoogle,
        firebaseUid = this.firebaseUid,
        isApproved = true,
        isAdmin = this.isAdmin ?: false,
        createdAt = this.submittedAt ?: Date(), 
        updatedAt = Date(),
        landline = this.landline,
        landline2 = this.landline2,
        isManualStation = this.isManualStation ?: false,
        isManualSubSection = this.isManualSubSection ?: false,
        gender = this.gender,
        serviceStartDate = this.serviceStartDate,
        dateOfBirth = this.dateOfBirth,
        subSection = this.subSection,
        dutyRole = this.dutyRole,
        height = null,
        weight = null,
        caste = null,
        subCaste = null,
        familyDetails = null,
        educationDetails = null
    )
}

fun EmployeeEntity.toEmployee(): Employee {
    return Employee(
        kgid = this.kgid,
        name = this.name,
        email = this.email,
        pin = this.pin,
        mobile1 = this.mobile1,
        mobile2 = this.mobile2,
        rank = this.rank,
        metalNumber = this.metalNumber,
        district = this.district,
        station = this.station,
        bloodGroup = this.bloodGroup,
        photoUrl = this.photoUrl,
        photoUrlFromGoogle = this.photoUrlFromGoogle,
        fcmToken = this.fcmToken,
        firebaseUid = this.firebaseUid,
        isAdmin = this.isAdmin ?: false,
        isApproved = this.isApproved ?: true,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        unit = this.unit,
        isManualStation = this.isManualStation ?: false,
        isManualSubSection = this.isManualSubSection ?: false,
        gender = this.gender,
        serviceStartDate = this.serviceStartDate,
        dateOfBirth = this.dateOfBirth,
        subSection = this.subSection,
        dutyRole = this.dutyRole,
        landline = this.landline,
        landline2 = this.landline2,
        height = this.height,
        weight = this.weight,
        caste = this.caste,
        subCaste = this.subCaste,
        familyDetails = this.familyDetails,
        educationDetails = this.educationDetails
    )
}

fun Employee.toEntity(): EmployeeEntity {
    return EmployeeEntity(
        kgid = this.kgid,
        name = this.name,
        email = this.email,
        pin = this.pin,
        mobile1 = this.mobile1,
        mobile2 = this.mobile2,
        rank = this.rank,
        metalNumber = this.metalNumber,
        district = this.district,
        station = this.station,
        bloodGroup = this.bloodGroup,
        photoUrl = this.photoUrl,
        fcmToken = this.fcmToken,
        isAdmin = this.isAdmin,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        firebaseUid = this.firebaseUid,
        photoUrlFromGoogle = this.photoUrlFromGoogle,
        isApproved = this.isApproved,
        unit = this.unit,
        searchBlob = this.searchBlob,
        landline = this.landline,
        landline2 = this.landline2,
        isManualStation = this.isManualStation,
        isManualSubSection = this.isManualSubSection,
        gender = this.gender,
        serviceStartDate = this.serviceStartDate,
        dateOfBirth = this.dateOfBirth,
        subSection = this.subSection,
        dutyRole = this.dutyRole,
        height = this.height,
        weight = this.weight,
        caste = this.caste,
        subCaste = this.subCaste,
        familyDetails = this.familyDetails,
        educationDetails = this.educationDetails
    )
}
