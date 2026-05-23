package com.nowayhome.housecheck.security

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HouseCheckCryptoServiceTest {
    private val cryptoService = HouseCheckCryptoService(secret = "housecheck-unit-test-secret")

    @Test
    fun encryptsAndDecryptsSensitiveValuesWithoutLeavingPlaintextAtRest() {
        val landlordName = "민감 임대인"
        val documentBytes = "%PDF-1.4 private-pdf".encodeToByteArray()

        val encryptedText = cryptoService.encryptText(landlordName)
        val encryptedBytes = cryptoService.encryptBytes(documentBytes)

        assertTrue(encryptedText != landlordName)
        assertTrue(!encryptedText.contains(landlordName))
        assertTrue(!encryptedBytes.contentEquals(documentBytes))
        assertEquals(landlordName, cryptoService.decryptText(encryptedText))
        assertContentEquals(documentBytes, cryptoService.decryptBytes(encryptedBytes))
    }
}
