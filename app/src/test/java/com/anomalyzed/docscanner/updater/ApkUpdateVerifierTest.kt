package com.anomalyzed.docscanner.updater

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ApkUpdateVerifierTest {
    @Test
    fun normalizeSha256AcceptsGithubDigestPrefix() {
        val hash = "ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789"

        assertEquals(hash.lowercase(), ApkUpdateVerifier.normalizeSha256("sha256:$hash"))
    }

    @Test
    fun normalizeSha256RejectsMissingOrMalformedHashes() {
        assertNull(ApkUpdateVerifier.normalizeSha256(null))
        assertNull(ApkUpdateVerifier.normalizeSha256(""))
        assertNull(ApkUpdateVerifier.normalizeSha256("sha256:not-a-hash"))
        assertNull(ApkUpdateVerifier.normalizeSha256("g".repeat(64)))
    }

    @Test
    fun hasExpectedSha256RejectsWrongHash() {
        val file = File.createTempFile("update", ".apk")
        try {
            file.writeText("not an apk")

            val wrongHash = "0".repeat(64)

            assertFalse(ApkUpdateVerifier.hasExpectedSha256(file, wrongHash))
            assertTrue(
                ApkUpdateVerifier.hasExpectedSha256(
                    file,
                    ApkUpdateVerifier.calculateSha256(file)
                )
            )
        } finally {
            file.delete()
        }
    }

    @Test
    fun trustedDownloadUrlRequiresProjectGithubReleaseAsset() {
        assertTrue(
            ApkUpdateVerifier.isTrustedDownloadUrl(
                "https://github.com/CorsiDanilo/simple-document-scanner/releases/download/v1.0.5/app.apk"
            )
        )

        assertFalse(
            ApkUpdateVerifier.isTrustedDownloadUrl(
                "http://github.com/CorsiDanilo/simple-document-scanner/releases/download/v1.0.5/app.apk"
            )
        )
        assertFalse(
            ApkUpdateVerifier.isTrustedDownloadUrl(
                "https://github.com/other/simple-document-scanner/releases/download/v1.0.5/app.apk"
            )
        )
        assertFalse(
            ApkUpdateVerifier.isTrustedDownloadUrl(
                "https://objects.githubusercontent.com/github-production-release-asset/app.apk"
            )
        )
    }
}
