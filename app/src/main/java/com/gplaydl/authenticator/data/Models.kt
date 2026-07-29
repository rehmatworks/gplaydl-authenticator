package com.gplaydl.authenticator.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DispenserUser(
    val id: String,
    val kind: String = "device",
    val label: String = "",
)

@Serializable
data class EnrollResponse(
    val user: DispenserUser,
    val apiKey: String,
)

@Serializable
data class SharedAccount(
    val id: String,
    val email: String,
    val status: String,
    val source: String = "app",
    val mintCount: Long = 0,
    val failureCount: Int = 0,
    val lastUsedAt: String? = null,
    val lastSyncedAt: String? = null,
) {
    val isHealthy: Boolean get() = status == "active"
}

@Serializable
data class AccountsResponse(val accounts: List<SharedAccount> = emptyList())

@Serializable
data class AccountResponse(val account: SharedAccount)

@Serializable
data class PairingCode(
    val code: String,
    val expiresAt: String = "",
    val url: String = "",
)

@Serializable
data class AppRelease(
    val version: String = "",
    val versionCode: Int = 0,
    val url: String = "",
)

@Serializable
data class ApiError(val error: String = "")

/** Credentials minted from a Google sign-in, before they are synced. */
data class MintedCredentials(
    val email: String,
    val aasToken: String,
    val displayName: String,
)
