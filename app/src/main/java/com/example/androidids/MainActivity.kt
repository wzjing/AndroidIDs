package com.example.androidids

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.app.ActivityCompat
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (Build.VERSION.SDK_INT >= 23 &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_PHONE_STATE),
                0x101
            )
        } else {
            setIds()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (permissions[0] == Manifest.permission.READ_PHONE_STATE
            && requestCode == 0x101
            && grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            setIds()
        }
    }

    private fun setIds() {
        textView.text = """
            Android_ID  : ${DeviceUtils.getAndroidId(this)}   
            IMEI0       : ${DeviceUtils.getIMEI(this)}
            IMEI1       : ${DeviceUtils.getIMEI(this, 1)}
            MEID        : ${DeviceUtils.getMEID(this, 0)}
        """.trimIndent()

        DeviceUtils.getOAID(this) {
            textView.append("\nOAID     : $it")
        }
    }
}
