package ir.devtrader.investor.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Mirrors GET /api/v1/app/check-update exactly (controllers/api/appUpdateController.js on the
 * backend). All fields beyond status/updateAvailable are absent when no release has ever been
 * published — the nullable defaults make that response decode cleanly instead of throwing.
 */
@Serializable
data class CheckUpdateResponse(
    val status: String,
    val updateAvailable: Boolean,
    val latestVersionCode: Int? = null,
    val latestVersionName: String? = null,
    val updateType: String? = null,
    val downloadUrl: String? = null,
    val releaseNotes: String? = null,
) {
    val isMandatory: Boolean get() = updateType == "immediate"
}
