package com.example.policemobiledirectory.utils

import com.example.policemobiledirectory.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityConfig @Inject constructor() {
    fun getSecretToken(): String {
        return BuildConfig.APPS_SCRIPT_SECRET_TOKEN
    }

    fun getExpectedSignatureHash(): String {
        return BuildConfig.EXPECTED_SIGNATURE_HASH
    }
}
