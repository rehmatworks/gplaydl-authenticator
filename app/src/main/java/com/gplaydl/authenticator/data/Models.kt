package com.gplaydl.authenticator.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Visibility of a shared Google account, mirroring the dispenser's enum. */
enum class Visibility(val wire: String) {
    Public("public"),
    Private("private"),
    ;

    companion object {
        fun from(wire: String?) = if (wire == "public") Public else Private
    }
}

@Serializable
data class DispenserUser(
    val id: String,
    val email: String = "",
    val kind: String = "web",
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
    val visibility: String,
    val status: String,
    val source: String = "app",
    val mintCount: Long = 0,
    val failureCount: Int = 0,
    val lastUsedAt: String? = null,
    val lastSyncedAt: String? = null,
) {
    val isPublic: Boolean get() = visibility == "public"
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
data class PoolStats(
    @SerialName("publicAccounts") val publicAccounts: Long = 0,
    @SerialName("mints24h") val mints24h: Long = 0,
    @SerialName("totalMints") val totalMints: Long = 0,
    @SerialName("contributors") val contributors: Long = 0,
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
