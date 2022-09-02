package com.example.callstatelistenerchangevolume

import android.app.ActivityManager
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
import androidx.annotation.RequiresApi
import androidx.core.content.getSystemService
import com.example.callstatelistenerchangevolume.databinding.ActivityMainBinding
import com.example.callstatelistenerchangevolume.service.PhoneCallStatesService
import com.example.callstatelistenerchangevolume.utils.clickWithLimit
import com.permissionx.guolindev.PermissionX
import java.security.Permission
import java.util.jar.Manifest

class MainActivity : AppCompatActivity() {

    private val serviceName = "com.example.callstatelistenerchangevolume.service.PhoneCallStatesService"
    lateinit var binding: ActivityMainBinding
    private val audioManager by lazy {
        getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    private val mNotificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        PermissionX.init(this)
            .permissions(android.Manifest.permission.READ_PHONE_STATE)
            .request { allGranted, grantedList, deniedList ->
                if (allGranted) {
                    Toast.makeText(this, "权限已同意", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "以下权限没有同意： $deniedList", Toast.LENGTH_LONG).show()
                }
            }

        initListener()

    }

    private fun initListener() {

        binding.btnStart.clickWithLimit {
            checkDisturbState()
            PhoneCallStatesService.startService(this)
            Toast.makeText(this, "服务正在启动，请检查通知栏是否启用服务", Toast.LENGTH_LONG).show()
        }

        binding.btnClose.clickWithLimit {
            PhoneCallStatesService.stopService(this)
            Toast.makeText(this, "铃声监听关闭成功", Toast.LENGTH_LONG).show()
        }


        binding.btnCheckMode.clickWithLimit {
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !mNotificationManager.isNotificationPolicyAccessGranted) {
            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            this.startActivity(intent)
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onResume() {
        super.onResume()
        val serviceWork = isServiceWork(this, serviceName)
        if (serviceWork && mNotificationManager.isNotificationPolicyAccessGranted) {
            Toast.makeText(this, "服务正常运行中", Toast.LENGTH_LONG).show()
        }else if(serviceWork){
            Toast.makeText(this, "免打扰权限未同意", Toast.LENGTH_LONG).show()
        }else{
            Toast.makeText(this, "服务未在运行，请点击开始监听", Toast.LENGTH_LONG).show()
        }
    }

    // 判断 Service 是否在工作
    fun isServiceWork(mContext: Context, serviceName: String): Boolean {
        var isWork = false
        val myAM = mContext
            .getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val myList: List<ActivityManager.RunningServiceInfo> = myAM.getRunningServices(40)
        if (myList.size <= 0) {
            return false
        }
        for (i in myList.indices) {
            val mName: String = myList[i].service.getClassName().toString()
            if (mName == serviceName) {
                isWork = true
                break
            }
        }
        return isWork
    }


}