package com.nowayhome.housecheck.security

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

private const val cipherAlgorithm = "AES/GCM/NoPadding"
private const val keyAlgorithm = "AES"
private const val gcmTagLengthBits = 128
private const val ivLengthBytes = 12
private const val textPrefix = "enc:"
private const val binaryPrefix = "NWH1"
private const val defaultEncryptionSecret = "housecheck-phase1-local-only-secret"

@Component
class HouseCheckCryptoService(
    @Value("\${housecheck.security.encryption.secret:$defaultEncryptionSecret}") secret: String,
) {
    init {
        HouseCheckCryptorHolder.configure(secret)
    }

    fun encryptText(plainText: String): String = HouseCheckCryptorHolder.encryptText(plainText)

    fun decryptText(cipherText: String): String = HouseCheckCryptorHolder.decryptText(cipherText)

    fun encryptBytes(plainBytes: ByteArray): ByteArray = HouseCheckCryptorHolder.encryptBytes(plainBytes)

    fun decryptBytes(cipherBytes: ByteArray): ByteArray = HouseCheckCryptorHolder.decryptBytes(cipherBytes)
}

@Converter
class EncryptedStringAttributeConverter : AttributeConverter<String, String> {
    override fun convertToDatabaseColumn(attribute: String?): String? {
        return attribute?.let(HouseCheckCryptorHolder::encryptText)
    }

    override fun convertToEntityAttribute(dbData: String?): String? {
        return dbData?.let(HouseCheckCryptorHolder::decryptText)
    }
}

private object HouseCheckCryptorHolder {
    @Volatile
    private var cryptor: AesGcmCryptor = AesGcmCryptor(defaultEncryptionSecret)

    fun configure(secret: String) {
        cryptor = AesGcmCryptor(secret)
    }

    fun encryptText(plainText: String): String = cryptor.encryptText(plainText)

    fun decryptText(cipherText: String): String = cryptor.decryptText(cipherText)

    fun encryptBytes(plainBytes: ByteArray): ByteArray = cryptor.encryptBytes(plainBytes)

    fun decryptBytes(cipherBytes: ByteArray): ByteArray = cryptor.decryptBytes(cipherBytes)
}

private class AesGcmCryptor(secret: String) {
    private val random = SecureRandom()
    private val secretKey = SecretKeySpec(
        MessageDigest.getInstance("SHA-256").digest(secret.toByteArray(StandardCharsets.UTF_8)),
        keyAlgorithm,
    )

    fun encryptText(plainText: String): String {
        if (plainText.startsWith(textPrefix)) {
            return plainText
        }
        val encryptedBytes = encryptRaw(plainText.toByteArray(StandardCharsets.UTF_8))
        return textPrefix + Base64.getEncoder().encodeToString(encryptedBytes)
    }

    fun decryptText(cipherText: String): String {
        if (!cipherText.startsWith(textPrefix)) {
            return cipherText
        }
        val encryptedBytes = Base64.getDecoder().decode(cipherText.removePrefix(textPrefix))
        return decryptRaw(encryptedBytes).toString(StandardCharsets.UTF_8)
    }

    fun encryptBytes(plainBytes: ByteArray): ByteArray {
        if (hasBinaryPrefix(plainBytes)) {
            return plainBytes
        }
        return binaryPrefix.toByteArray(StandardCharsets.UTF_8) + encryptRaw(plainBytes)
    }

    fun decryptBytes(cipherBytes: ByteArray): ByteArray {
        if (!hasBinaryPrefix(cipherBytes)) {
            return cipherBytes
        }
        return decryptRaw(cipherBytes.copyOfRange(binaryPrefix.length, cipherBytes.size))
    }

    private fun encryptRaw(plainBytes: ByteArray): ByteArray {
        val iv = ByteArray(ivLengthBytes).also(random::nextBytes)
        val cipher = Cipher.getInstance(cipherAlgorithm).apply {
            init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(gcmTagLengthBits, iv))
        }
        val cipherBytes = cipher.doFinal(plainBytes)
        return ByteBuffer.allocate(iv.size + cipherBytes.size)
            .put(iv)
            .put(cipherBytes)
            .array()
    }

    private fun decryptRaw(encryptedBytes: ByteArray): ByteArray {
        val buffer = ByteBuffer.wrap(encryptedBytes)
        val iv = ByteArray(ivLengthBytes).also(buffer::get)
        val cipherBytes = ByteArray(buffer.remaining()).also(buffer::get)
        val cipher = Cipher.getInstance(cipherAlgorithm).apply {
            init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(gcmTagLengthBits, iv))
        }
        return cipher.doFinal(cipherBytes)
    }

    private fun hasBinaryPrefix(bytes: ByteArray): Boolean {
        if (bytes.size < binaryPrefix.length) {
            return false
        }
        return bytes.copyOfRange(0, binaryPrefix.length).contentEquals(binaryPrefix.toByteArray(StandardCharsets.UTF_8))
    }
}
