package com.example.meditationparticles.data.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.File

class ApkSha256VerifierTest {
    @Test
    fun verifyMatchesComputedHash() {
        val file = File.createTempFile("apk-test", ".bin")
        file.writeBytes(byteArrayOf(1, 2, 3, 4, 5))

        val expected = ApkSha256Verifier.computeHex(file)

        ApkSha256Verifier.verify(file, expected)

        file.delete()
    }

    @Test
    fun verifyRejectsMismatchedHash() {
        val file = File.createTempFile("apk-test", ".bin")
        file.writeBytes(byteArrayOf(9, 8, 7))

        assertThrows(IllegalStateException::class.java) {
            ApkSha256Verifier.verify(
                file,
                "0000000000000000000000000000000000000000000000000000000000000000",
            )
        }

        file.delete()
    }
}
