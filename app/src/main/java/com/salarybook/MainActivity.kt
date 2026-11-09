
package com.salarybook

import android.app.ActivityManager
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.salarybook.ui.SalaryBookApp

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val icon = BitmapFactory.decodeResource(
            resources,
            R.mipmap.ic_launcher
        )

        setTaskDescription(
            ActivityManager.TaskDescription(
                getString(R.string.app_name),
                icon
            )
        )

        enableEdgeToEdge()

        setContent {
            SalaryBookApp()
        }
    }
}
