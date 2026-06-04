package com.anomalyzed.docscanner.updater

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class AndroidManifestSecurityTest {
    @Test
    fun downloadReceiverIsNotExported() {
        val manifest = findManifest()
        assertTrue("AndroidManifest.xml should exist", manifest.isFile)

        val document = DocumentBuilderFactory.newInstance()
            .apply { isNamespaceAware = true }
            .newDocumentBuilder()
            .parse(manifest)

        val receivers = document.getElementsByTagName("receiver")
        var exportedValue: String? = null

        for (i in 0 until receivers.length) {
            val receiver = receivers.item(i)
            val name = receiver.attributes
                .getNamedItemNS(ANDROID_NS, "name")
                ?.nodeValue

            if (name == ".updater.DownloadReceiver") {
                exportedValue = receiver.attributes
                    .getNamedItemNS(ANDROID_NS, "exported")
                    ?.nodeValue
                break
            }
        }

        assertNotNull("DownloadReceiver should be declared", exportedValue)
        assertEquals("false", exportedValue)
    }

    private fun findManifest(): File {
        return listOf(
            File("src/main/AndroidManifest.xml"),
            File("app/src/main/AndroidManifest.xml")
        ).first { it.isFile }
    }

    private companion object {
        const val ANDROID_NS = "http://schemas.android.com/apk/res/android"
    }
}
