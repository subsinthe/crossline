package com.example.subsinthe.crossline

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.widget.Toast
import com.example.subsinthe.crossline.util.ObservableValue
import com.example.subsinthe.crossline.util.loggerFor

class PermissionListener(private val mainActivity: Activity) {
    private val results = HashMap<Int, ObservableValue<Boolean>>()

    fun requestReadExternalStorage(handler: () -> Unit) = request(
        handler,
        0,
        android.Manifest.permission.READ_EXTERNAL_STORAGE,
        "External storage reading allows us to scan filesystem for music"
    )

    fun report(code: Int, permissions: Array<String>, grantResults: IntArray) {
        val result = results.get(code)
        if (result == null) {
            LOG.severe("Internal error - no handlers for request $code")
            return
        }

        val r = grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED
        val status = if (r) "granted" else "denied"

        LOG.info("Permission $status for request $code")
        result.value = r
    }

    private fun request(handler: () -> Unit, code: Int, permission: String, rationale: String) {
        val result = results.getOrPut(code, { ObservableValue<Boolean>(false) })

        result.subscribe {
            if (it) {
                try {
                    handler()
                } catch (ex: Throwable) {
                    LOG.warning("Uncaught exception from handler: $ex")
                }
            }
        }

        if (result.value)
            return
        if (Build.VERSION.SDK_INT < 23 || checkPermission(permission))
            result.value = true
        else
            requestPermission(code, permission, rationale)
    }

    private fun checkPermission(permission: String) = ContextCompat.checkSelfPermission(
        mainActivity, permission
    ) == PackageManager.PERMISSION_GRANTED

    private fun requestPermission(code: Int, permission: String, rationale: String) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(mainActivity, permission)) {
            Toast.makeText(
                mainActivity,
                "$rationale. Please allow this permission in App Settings.",
                Toast.LENGTH_LONG
            ).show()
        } else {
            ActivityCompat.requestPermissions(mainActivity, arrayOf(permission), code)
        }
    }

    private companion object { val LOG = loggerFor<PermissionListener>() }
}
