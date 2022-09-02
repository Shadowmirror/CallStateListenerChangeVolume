package com.example.callstatelistenerchangevolume

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.content.getSystemService
import com.example.callstatelistenerchangevolume.databinding.ActivityMainBinding
import com.example.callstatelistenerchangevolume.service.PhoneCallStatesService

class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager



        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnStart.setOnClickListener {
            checkDisturbState()
            PhoneCallStatesService.startService(this)
        }

        binding.btnClose.setOnClickListener {
            PhoneCallStatesService.stopService(this)
        }


        binding.btnCheckMode.setOnClickListener {
            var musicMode = when (audioManager.ringerMode) {
                AudioManager.RINGER_MODE_NORMAL -> "当前音量模式：正常"
                AudioManager.RINGER_MODE_SILENT -> "当前音量模式：静音"
                AudioManager.RINGER_MODE_VIBRATE -> "当前音量模式：震动"
                else -> {
                    "未知模式"
                }
            }
            Log.i("miao", "onCreate: $musicMode")
            Toast.makeText(this, musicMode, Toast.LENGTH_LONG).show()
        }

    }

    // 检查并设置修改免打扰权限
    private fun checkDisturbState() {
        val mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !mNotificationManager.isNotificationPolicyAccessGranted) {
            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            this.startActivity(intent)
        }
    }
}