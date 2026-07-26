package com.gplaydl.authenticator.data

import kotlinx.serialization.Serializable

@Serializable
data class EnrollRequest(
    val deviceSecret: String,
    val label: String,
    val consentVersion: String,
)

@Serializable
data class SyncAccountRequest(
    val email: String,
    val aasToken: String,
    val visibility: String,
    val consentVersion: String,
)

@Serializable
data class VisibilityRequest(val visibility: String)

@Serializable
data class MeResponse(val user: DispenserUser)

@Serializable
data class StatusResponse(val status: String = "")

@Serializable
data class TestResult(
    val success: Boolean = false,
    val error: String = "",
    val durationMs: Long = 0,
)
