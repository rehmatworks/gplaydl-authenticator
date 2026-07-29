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
)

@Serializable
data class StatusResponse(val status: String = "")
