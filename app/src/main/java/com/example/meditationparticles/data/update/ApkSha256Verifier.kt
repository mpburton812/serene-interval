package com.example.meditationparticles.data.update

import java.io.File
import java.security.MessageDigest

object ApkSha256Verifier {
    fun computeHex(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read == -1) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { byte ->
            "%02x".format(byte)
        }
    }

    fun verify(file: File, expectedSha256: String) {
        val normalizedExpected = expectedSha256.trim().lowercase()
        require(normalizedExpected.matches(Regex("[0-9a-f]{64}"))) {
            "Invalid expectedSha256 in release manifest."
        }
        val actual = computeHex(file)
        check(actual == normalizedExpected) {
            "Downloaded APK hash mismatch. Expected $normalizedExpected but got $actual."
        }
    }
}
