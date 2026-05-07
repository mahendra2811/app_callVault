package com.callNest.app.data.backup

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AES-256-GCM encryption helper.
 *
 * Encrypted blob layout: `MAGIC(4) | VERSION(1) | SALT(16) | IV(12) | CIPHERTEXT+TAG(rest)`.
 *
 * **DECISIONS deviation**: spec mentions Tink keysets. Using javax.crypto +
 * PBKDF2 here is functionally equivalent and avoids shipping a keyset
 * file separately — the salt is embedded in every blob. AeadConfig is no
 * longer required, but the project still has Tink available for any future
 * features that need its primitives.
 */
@Singleton
class EncryptionHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var passphrase: String? = null

    /** Stash the passphrase for the current session. */
    fun init(passphrase: String) {
        this.passphrase = passphrase
    }

    /** True when [init] has been called this session. */
    val isInitialized: Boolean get() = passphrase != null

    /** Encrypt [bytes] using the current passphrase. */
    fun encrypt(bytes: ByteArray): ByteArray {
        val pass = requireNotNull(passphrase) { "EncryptionHelper not initialized" }
        val salt = ByteArray(SALT_LEN).also { SecureRandom().nextBytes(it) }
        val iv = ByteArray(IV_LEN).also { SecureRandom().nextBytes(it) }
        val key = deriveKey(pass, salt)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, iv))
        }
        val ct = cipher.doFinal(bytes)
        val out = ByteArray(MAGIC.size + 1 + salt.size + iv.size + ct.size)
        var p = 0
        System.arraycopy(MAGIC, 0, out, p, MAGIC.size); p += MAGIC.size
        out[p++] = VERSION
        System.arraycopy(salt, 0, out, p, salt.size); p += salt.size
        System.arraycopy(iv, 0, out, p, iv.size); p += iv.size
        System.arraycopy(ct, 0, out, p, ct.size)
        return out
    }

    /** Decrypt [bytes] produced by [encrypt]. Throws on tamper or wrong passphrase. */
    fun decrypt(bytes: ByteArray): ByteArray {
        val pass = requireNotNull(passphrase) { "EncryptionHelper not initialized" }
        require(bytes.size > MAGIC.size + 1 + SALT_LEN + IV_LEN) { "Backup file is too short." }
        for (i in MAGIC.indices) require(bytes[i] == MAGIC[i]) { "Not a callNest backup." }
        var p = MAGIC.size
        val ver = bytes[p++].toInt()
        require(ver == VERSION.toInt()) { "Unsupported backup version $ver" }
        val salt = bytes.copyOfRange(p, p + SALT_LEN); p += SALT_LEN
        val iv = bytes.copyOfRange(p, p + IV_LEN); p += IV_LEN
        val ct = bytes.copyOfRange(p, bytes.size)
        val key = deriveKey(pass, salt)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, iv))
        }
        return cipher.doFinal(ct)
    }

    /** PBKDF2-HMAC-SHA256, 120k iterations → 32-byte AES key. */
    private fun deriveKey(passphrase: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(passphrase.toCharArray(), salt, ITERATIONS, KEY_BITS)
        val skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return skf.generateSecret(spec).encoded
    }

    /** Internal storage dir reserved for any future keyset persistence. */
    @Suppress("unused")
    private fun storageDir(): File =
        File(context.filesDir, "backup").apply { if (!exists()) mkdirs() }

    private companion object {
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val ITERATIONS = 120_000
        const val KEY_BITS = 256
        const val TAG_BITS = 128
        const val SALT_LEN = 16
        const val IV_LEN = 12
        val MAGIC = byteArrayOf('C'.code.toByte(), 'V'.code.toByte(), 'B'.code.toByte(), '1'.code.toByte())
        const val VERSION: Byte = 1
    }
}
