package com.example.mycontrolapp.utils
import android.os.Build
import javax.inject.Qualifier


@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RemoteEnabled

object DeviceInfo {
    fun isEmulator(): Boolean {
        val fp = Build.FINGERPRINT.lowercase()
        val model = Build.MODEL.lowercase()
        val product = Build.PRODUCT.lowercase()
        val brand = Build.BRAND.lowercase()
        val manufacturer = Build.MANUFACTURER.lowercase()
        val hardware = Build.HARDWARE.lowercase()

        return fp.contains("generic") ||
                fp.contains("emulator") ||
                model.contains("google_sdk") ||
                model.contains("android sdk built for x86") ||
                model.contains("emulator") ||
                product.contains("sdk") ||
                product.contains("emulator") ||
                brand.startsWith("generic") ||
                manufacturer.contains("genymotion") ||
                hardware.contains("goldfish") ||
                hardware.contains("ranchu")
    }
}