package com.example.update

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.example.R
import java.security.KeyStore
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/**
 * اعتماد امن به سرورهای گیت‌هاب روی اندرویدهای قدیمی.
 *
 * مشکل: مخزن گواهی اندروید ۶ و ۷.۰ گواهی‌های ریشه جدید را ندارد؛ از جمله
 *  - ISRG Root X1 (زنجیره Let's Encrypt برای release-assets.githubusercontent.com، از اندروید 7.1.1 به بعد)
 *  - Sectigo Public Server Authentication Root E46 (زنجیره github.com و api.github.com)
 * در نتیجه دانلود APK با خطای «Trust anchor for certification path not found» شکست می‌خورد.
 *
 * راه‌حل: به جای غیرفعال کردن بررسی گواهی (Trust-All)، گواهی‌های ریشه رسمی داخل برنامه
 * (res/raw/github_ca_*.pem) قرار گرفته‌اند. TrustManager ترکیبی ابتدا با مخزن گواهی سیستم
 * اعتبارسنجی می‌کند و فقط در صورت شکست، همان زنجیره را با گواهی‌های ریشه داخلی بررسی می‌کند.
 * بررسی نام میزبان (Hostname Verification) هم توسط HttpsURLConnection به شکل عادی انجام می‌شود.
 */
object GitHubTrust {
    private const val TAG = "GitHubTrust"

    private val BUNDLED_ROOTS = intArrayOf(
        R.raw.github_ca_isrg_root_x1,
        R.raw.github_ca_sectigo_public_server_root_e46,
        R.raw.github_ca_usertrust_ecc,
        R.raw.github_ca_usertrust_rsa
    )

    @Volatile
    private var cachedFactory: SSLSocketFactory? = null

    /**
     * SSLSocketFactory امن (گواهی‌های سیستم + گواهی‌های ریشه داخلی). در صورت بروز هر خطا null برمی‌گردد
     * تا اتصال با تنظیمات پیش‌فرض سیستم ادامه یابد.
     */
    fun socketFactory(context: Context): SSLSocketFactory? {
        cachedFactory?.let { return it }
        return synchronized(this) {
            cachedFactory ?: buildSocketFactory(context.applicationContext)?.also { cachedFactory = it }
        }
    }

    private fun buildSocketFactory(context: Context): SSLSocketFactory? {
        return try {
            val systemTm = defaultTrustManager(null)
            val bundledTm = defaultTrustManager(loadBundledKeyStore(context))
            val composite = CompositeTrustManager(systemTm, bundledTm)
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, arrayOf(composite), null)
            sslContext.socketFactory
        } catch (e: Exception) {
            Log.e(TAG, "Failed to build GitHub trust socket factory: ${e.message}", e)
            null
        }
    }

    private fun loadBundledKeyStore(context: Context): KeyStore {
        val cf = CertificateFactory.getInstance("X.509")
        val ks = KeyStore.getInstance(KeyStore.getDefaultType())
        ks.load(null, null)
        BUNDLED_ROOTS.forEachIndexed { index, resId ->
            context.resources.openRawResource(resId).use { input ->
                ks.setCertificateEntry("github_root_$index", cf.generateCertificate(input))
            }
        }
        return ks
    }

    private fun defaultTrustManager(keyStore: KeyStore?): X509TrustManager {
        val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        tmf.init(keyStore)
        return tmf.trustManagers.filterIsInstance<X509TrustManager>().first()
    }

    @SuppressLint("CustomX509TrustManager")
    private class CompositeTrustManager(
        private val system: X509TrustManager,
        private val bundled: X509TrustManager
    ) : X509TrustManager {

        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
            system.checkClientTrusted(chain, authType)
        }

        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
            try {
                system.checkServerTrusted(chain, authType)
            } catch (systemError: CertificateException) {
                try {
                    bundled.checkServerTrusted(chain, authType)
                } catch (bundledError: CertificateException) {
                    systemError.addSuppressed(bundledError)
                    throw systemError
                }
            }
        }

        override fun getAcceptedIssuers(): Array<X509Certificate> =
            system.acceptedIssuers + bundled.acceptedIssuers
    }
}
