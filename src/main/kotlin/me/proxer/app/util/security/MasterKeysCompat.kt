@file:Suppress("DEPRECATION")

package me.proxer.app.util.security

import android.content.Context
import android.os.Build
import android.security.KeyPairGeneratorSpec
import androidx.security.crypto.MasterKeys
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.util.Date
import javax.security.auth.x500.X500Principal

/**
 * @author Ruben Gees
 */
class MasterKeysCompat {

    private companion object {
        private const val KEY_ALIAS = "_androidx_security_master_key_"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    }

    fun getOrCreate(context: Context): String {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            !keyExists() -> generateCompatKey(context)
            else -> KEY_ALIAS
        }
    }

    private fun generateCompatKey(context: Context): String {
        val spec = KeyPairGeneratorSpec.Builder(context)
            .setAlias(KEY_ALIAS)
            .setSubject(X500Principal("CN=Proxer"))
            .setSerialNumber(BigInteger.ONE)
            .setStartDate(Date())
            .setEndDate(Date())
            .setKeySize(2_048)
            .build()

        KeyPairGenerator.getInstance("RSA", ANDROID_KEYSTORE).apply {
            initialize(spec)
            generateKeyPair()
        }

        return KEY_ALIAS
    }

    private fun keyExists(): Boolean {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
            load(null)
        }

        return keyStore.containsAlias(KEY_ALIAS)
    }
}
