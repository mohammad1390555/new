package com.example.data.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object CryptoHelper {
    private const val ANDROID_KEY_STORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "VibeForgeCryptoKey"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    init {
        initKeyStore()
    }

    private fun initKeyStore() {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEY_STORE
            )
            keyGenerator.init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build()
            )
            keyGenerator.generateKey()
        }
    }

    private fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        return (keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
    }

    fun encrypt(plainText: String): String {
        if (plainText.isEmpty()) return ""
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
        val iv = cipher.iv
        val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        
        // Combine IV and Encrypted Bytes
        // IV is usually 12 bytes for GCM. Let's prepend IV to the encrypted bytes
        val ivAndEncrypted = ByteArray(iv.size + encryptedBytes.size)
        System.arraycopy(iv, 0, ivAndEncrypted, 0, iv.size)
        System.arraycopy(encryptedBytes, 0, ivAndEncrypted, iv.size, encryptedBytes.size)
        
        return Base64.encodeToString(ivAndEncrypted, Base64.DEFAULT)
    }

    fun decrypt(cipherText: String): String {
        if (cipherText.isEmpty()) return ""
        try {
            val ivAndEncrypted = Base64.decode(cipherText, Base64.DEFAULT)
            if (ivAndEncrypted.size < 12) return ""
            
            // Extract 12 bytes IV
            val iv = ByteArray(12)
            System.arraycopy(ivAndEncrypted, 0, iv, 0, 12)
            
            val encryptedBytes = ByteArray(ivAndEncrypted.size - 12)
            System.arraycopy(ivAndEncrypted, 12, encryptedBytes, 0, encryptedBytes.size)
            
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)
            
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            return String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }
}
