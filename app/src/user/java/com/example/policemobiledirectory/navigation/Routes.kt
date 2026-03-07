package com.example.policemobiledirectory.navigation

object Routes {

    // --- Core Screens ---
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val EMPLOYEE_LIST = "employee_list"
    const val ADD_EMPLOYEE = "add_employee"
    const val EDIT_EMPLOYEE = "edit_employee"
    const val ADD_OFFICER = "add_officer"
    const val ABOUT = "about"

    // --- Admin Screens ---
    const val ADMIN_PANEL = "admin_panel"
    const val EMPLOYEE_STATS = "employee_stats"
    const val OFFICER_STATS = "officer_stats"
    const val PENDING_APPROVALS = "pending_approvals"
    const val SEND_NOTIFICATION = "send_notification"
    const val UPLOAD_CSV = "upload_csv"
    const val ADD_USEFUL_LINK = "add_useful_link"
    const val UPLOAD_DOCUMENT = "upload_document"
    const val MY_PROFILE = "my_profile"
    const val DOCUMENTS = "documents"
    const val USEFUL_LINKS = "useful_links"
    const val USER_REGISTRATION = "user_registration"
    const val FORGOT_PIN = "forgot_pin"
    const val GOOGLE_SIGN_IN = "google_sign_in"
    const val GOOGLE_SIGN_OUT = "google_sign_out"
    const val LOGOUT = "logout"
    const val NOTIFICATIONS = "notifications"

    const val GALLERY_SCREEN = "gallery_screen"
    const val TERMS_AND_CONDITIONS = "terms_and_conditions"
    const val NUDI_CONVERTER = "nudi_converter"
    const val DUTY_REGISTER = "duty_register"
    const val MANAGE_CONSTANTS = "manage_constants" // ✅ New Route

    // --- Leave Manager ---
    const val LEAVE_DASHBOARD = "leave_dashboard"
    const val LEAVE_ENTRY = "leave_entry"
    const val LEAVE_REPORTS = "leave_reports"
    const val LEAVE_CL = "leave_cl"
    const val LEAVE_EL = "leave_el"
    const val LEAVE_HPL = "leave_hpl"
    const val LEAVE_WO = "leave_wo"
    const val LEAVE_CCL = "leave_ccl"
    const val LEAVE_MCL = "leave_mcl"
    const val LEAVE_OTHER = "leave_other"
    const val LEAVE_EDIT = "leave_edit/{entryId}"

    fun leaveEditRoute(entryId: String) = "leave_edit/$entryId"

}
