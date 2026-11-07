package com.elwsam.operations.updates

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import org.junit.Assert.*
import org.junit.Test

class UpdatePolicyTest {
    private val bytes = "a complete apk download".toByteArray()
    private fun hash(value: ByteArray) = MessageDigest.getInstance("SHA-256").digest(value).joinToString("") { "%02x".format(it) }

    @Test fun validDownloadIsCopiedAndVerifiedToCompletion() {
        val output = ByteArrayOutputStream()
        val progress = mutableListOf<Int>()
        UpdatePolicy.copyVerified(ByteArrayInputStream(bytes), output, bytes.size.toLong(), hash(bytes), progress::add)
        assertArrayEquals(bytes, output.toByteArray())
        assertEquals(100, progress.last())
    }

    @Test fun incompleteDownloadCannotBeInstalled() {
        assertThrows(IllegalArgumentException::class.java) {
            UpdatePolicy.copyVerified(ByteArrayInputStream(bytes.copyOf(5)), ByteArrayOutputStream(), bytes.size.toLong(), hash(bytes)) { }
        }
    }

    @Test fun changedDownloadCannotBeInstalled() {
        assertThrows(IllegalArgumentException::class.java) {
            UpdatePolicy.copyVerified(ByteArrayInputStream(bytes), ByteArrayOutputStream(), bytes.size.toLong(), "0".repeat(64)) { }
        }
    }

    @Test fun oversizedBodyIsRejectedEvenWhenServerLiesAboutLength() {
        assertThrows(IllegalArgumentException::class.java) {
            UpdatePolicy.copyVerified(ByteArrayInputStream(bytes), ByteArrayOutputStream(), 3, hash(bytes)) { }
        }
    }

    @Test fun oldAndEqualVersionsAreNeverOfferedForInstall() {
        for (offered in listOf(4L, 5L)) assertThrows(IllegalArgumentException::class.java) {
            UpdatePolicy.requireValidMetadata(5, offered, bytes.size.toLong(), hash(bytes))
        }
        UpdatePolicy.requireValidMetadata(5, 6, bytes.size.toLong(), hash(bytes))
    }

    @Test fun invalidSizeAndChecksumAreRejectedBeforeDownload() {
        for (size in listOf(0L, -1L, UpdatePolicy.MAX_BYTES + 1)) assertThrows(IllegalArgumentException::class.java) {
            UpdatePolicy.requireValidMetadata(5, 6, size, hash(bytes))
        }
        assertThrows(IllegalArgumentException::class.java) {
            UpdatePolicy.requireValidMetadata(5, 6, bytes.size.toLong(), "bad hash")
        }
    }
}
