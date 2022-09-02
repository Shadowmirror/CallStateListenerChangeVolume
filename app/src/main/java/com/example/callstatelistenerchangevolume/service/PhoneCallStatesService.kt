package com.example.callstatelistenerchangevolume.service

import android.app.*
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.callstatelistenerchangevolume.MainActivity
import com.example.callstatelistenerchangevolume.R

class PhoneCallStatesService : Service() {

    private val TAG = "PhoneCallStatesService"
    private lateinit var telephonyManager: TelephonyManager
    private lateinit var listener: PhoneStateListener
    private lateinit var mAudioManager: AudioManager
    private var mMaxVolume: Int = 0
    private var isOnCall: Boolean = false

    private val mForegroundNF: ForegroundNotify by lazy { ForegroundNotify(this) }


    companion object {
        fun startService(context: Context) {
            val intent = Intent(context, PhoneCallStatesService::class.java)
            val bundle = Bundle()
            intent.putExtras(bundle)
            //启动服务
            // Android 8.0使用startForegroundService在前台启动新服务
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }

        }

        fun stopService(context: Context) {
            val intent = Intent(context, PhoneCallStatesService::class.java)
            context.stopService(intent)
        }
    }


    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        mForegroundNF.startForegroundNotification()
        isOnCall = false
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        mAudioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        mMaxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_RING)
        Log.i(TAG, "onCreate: mAudioManager isVolumeFixed = ${mAudioManager.isVolumeFixed}")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (null == intent) {
            return START_NOT_STICKY
        }
        // 解决 Android 12 版本兼容问题
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephonyManager.registerTelephonyCallback(
                this.mainExecutor,
                object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                    override fun onCallStateChanged(state: Int) {
                        checkState(state)
                    }
                }
            )
        } else {
            listener = object : PhoneStateListener() {
                override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                    checkState(state)
                }
            }
            telephonyManager.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
        }
        mForegroundNF.startForegroundNotification()
        return START_STICKY
    }

    // 根据不同的电话状态做出相应行为
    private fun checkState(state: Int) {
        when (state) {
            TelephonyManager.CALL_STATE_IDLE -> Log.d(TAG, "onCallStateChanged: 挂断")
            TelephonyManager.CALL_STATE_OFFHOOK -> Log.d(TAG, "onCallStateChanged: 接听")
            TelephonyManager.CALL_STATE_RINGING -> {
                Log.d(TAG, "onCallStateChanged: 响铃")
                setStreamVolume(mMaxVolume)
            }
            else -> {
                Log.d(TAG, "onCallStateChanged: Other State")
            }
        }
    }

    private fun setStreamVolume(volume: Int) {
        mAudioManager.setStreamVolume(AudioManager.STREAM_RING, volume, AudioManager.FLAG_SHOW_UI)
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, AudioManager.FLAG_SHOW_UI)
    }

    override fun onDestroy() {
        mForegroundNF.stopForegroundNotification()
        super.onDestroy()
    }


}


// 初始化前台通知，停止前台通知 这个的好处是顶部没有提醒
class ForegroundNotify(private val service: PhoneCallStatesService) : ContextWrapper(service) {
    companion object {
        private const val START_ID = 8355601
        private const val CHANNEL_ID = "app_foreground_service"
        private const val CHANNEL_NAME = "前台保活服务"
    }

    private var mNotificationManager: NotificationManager? = null

    private var mCompatBuilder: NotificationCompat.Builder? = null

    private var compatBuilder: NotificationCompat.Builder? = null
        get() {
            if (mCompatBuilder == null) {
                val notificationIntent = Intent(this, MainActivity::class.java)
                notificationIntent.action = Intent.ACTION_MAIN
                notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER)
                notificationIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                // 动作意图
                val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    PendingIntent.getActivity(this, (Math.random() * 10 + 10).toInt(), notificationIntent, PendingIntent.FLAG_IMMUTABLE)
                } else {
                    PendingIntent.getActivity(this, (Math.random() * 10 + 10).toInt(), notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT)
                }
                val notificationBuilder: NotificationCompat.Builder = NotificationCompat.Builder(this, CHANNEL_ID)
                notificationBuilder.setContentTitle("铃声监听")
                notificationBuilder.setContentText("防止静音手机导致听不到铃声")
                notificationBuilder.setSmallIcon(R.mipmap.ic_launcher)
                notificationBuilder.setContentIntent(pendingIntent)
                mCompatBuilder = notificationBuilder
            }
            return mCompatBuilder
        }

    init {
        createNotificationChannel()
    }

    // 创建通知渠道
    private fun createNotificationChannel() {
        mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
        // Android 8.0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            )
            channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            channel.setShowBadge(false)
            mNotificationManager?.createNotificationChannel(channel)
        }
    }


    // 开启前台通知
    fun startForegroundNotification() {
        service.startForeground(START_ID, compatBuilder?.build())
    }

    // 停止前台服务并清除通知
    fun stopForegroundNotification() {
        mNotificationManager?.cancelAll()
        service.stopForeground(true)
    }
}