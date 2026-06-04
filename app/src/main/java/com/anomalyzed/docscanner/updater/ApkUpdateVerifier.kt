package com.anomalyzed.docscanner.updater

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import android.os.Environment
import java.io.File
import java.net.URI
import java.security.MessageDigest
import java.util.Locale

internal data class ApkVerificationResult(
    val isTrusted: Boolean,
    val reason: String
)

internal object ApkUpdateVerifier {
    private const val TRUSTED_RELEASE_HOST = "github.com"
    private const val TRUSTED_RELEASE_PATH_PREFIX =
        "/CorsiDanilo/simple-document-scanner/releases/download/"
    private const val SHA256_PREFIX = "sha256:"
    private val hexChars = "0123456789abcdef".toCharArray()

    fun normalizeSha256(value: String?): String? {
        val raw = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val hash = if (raw.regionMatches(0, SHA256_PREFIX, 0, SHA256_PREFIX.length, true)) {
            raw.substring(SHA256_PREFIX.length).trim()
        } else {
            raw
        }.lowercase(Locale.US)

        return hash.takeIf {
            it.length == 64 && it.all { char -> char in '0'..'9' || char in 'a'..'f' }
        }
    }

    fun isTrustedDownloadUrl(downloadUrl: String): Boolean {
        val uri = runCatching { URI(downloadUrl) }.getOrNull() ?: return false
        val host = uri.host ?: return false
        return uri.scheme.equals("https", ignoreCase = true) &&
            host.equals(TRUSTED_RELEASE_HOST, ignoreCase = true) &&
            (uri.path ?: "").startsWith(TRUSTED_RELEASE_PATH_PREFIX)
    }

    fun hasExpectedSha256(file: File, expectedSha256: String): Boolean {
        val normalizedExpected = normalizeSha256(expectedSha256) ?: return false
        return calculateSha256(file) == normalizedExpected
    }

    fun calculateSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read == -1) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().toHex()
    }

    fun verifyDownloadedApk(
        context: Context,
        file: File,
        expectedSha256: String
    ): ApkVerificationResult {
        val normalizedExpected = normalizeSha256(expectedSha256)
            ?: return ApkVerificationResult(false, "missing_or_invalid_expected_sha256")

        if (!isInsideAppDownloads(context, file)) {
            return ApkVerificationResult(false, "outside_app_downloads")
        }

        val actualSha256 = calculateSha256(file)
        if (actualSha256 != normalizedExpected) {
            return ApkVerificationResult(false, "sha256_mismatch")
        }

        return verifyPackageNameAndSigner(context, file)
    }

    fun isInsideAppDownloads(context: Context, file: File): Boolean {
        val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: return false
        val root = downloadsDir.canonicalFile
        var current: File? = file.canonicalFile

        while (current != null) {
            if (current == root) return true
            current = current.parentFile
        }

        return false
    }

    @Suppress("DEPRECATION")
    private fun verifyPackageNameAndSigner(context: Context, file: File): ApkVerificationResult {
        val packageManager = context.packageManager
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            PackageManager.GET_SIGNATURES
        }

        val archiveInfo = packageManager.getPackageArchiveInfo(file.absolutePath, flags)
            ?: return ApkVerificationResult(false, "invalid_apk")

        if (archiveInfo.packageName != context.packageName) {
            return ApkVerificationResult(false, "package_name_mismatch")
        }

        val installedInfo = packageManager.getPackageInfo(context.packageName, flags)
        val archiveSignerSha256 = signerCertificateSha256(archiveInfo)
        val installedSignerSha256 = signerCertificateSha256(installedInfo)

        if (archiveSignerSha256.isEmpty() || installedSignerSha256.isEmpty()) {
            return ApkVerificationResult(false, "missing_signing_certificate")
        }

        if (archiveSignerSha256 != installedSignerSha256) {
            return ApkVerificationResult(false, "signer_certificate_mismatch")
        }

        return ApkVerificationResult(true, "verified")
    }

    @Suppress("DEPRECATION")
    private fun signerCertificateSha256(packageInfo: PackageInfo): Set<String> {
        val signatures: Array<Signature> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.signingInfo?.apkContentsSigners ?: emptyArray()
        } else {
            packageInfo.signatures ?: emptyArray()
        }

        return signatures.map { signature ->
            MessageDigest.getInstance("SHA-256")
                .digest(signature.toByteArray())
                .toHex()
        }.toSet()
    }

    private fun ByteArray.toHex(): String {
        val result = CharArray(size * 2)
        forEachIndexed { index, byte ->
            val value = byte.toInt() and 0xff
            result[index * 2] = hexChars[value ushr 4]
            result[index * 2 + 1] = hexChars[value and 0x0f]
        }
        return String(result)
    }
}
