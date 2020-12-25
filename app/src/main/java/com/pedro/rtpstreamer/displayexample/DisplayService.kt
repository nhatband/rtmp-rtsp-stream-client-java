package com.pedro.rtpstreamer.displayexample

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.pedro.rtplibrary.base.DisplayBase
import com.pedro.rtplibrary.rtmp.RtmpDisplay
import com.pedro.rtplibrary.rtsp.RtspDisplay
import com.pedro.rtpstreamer.R
import com.pedro.rtpstreamer.utils.ConnectCheckerRtp


/**
 * Basic RTMP/RTSP service streaming implementation with camera2
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
class DisplayService : Service() {
//    private lateinit var manager: WindowManager
//    private lateinit var view: View
//    private lateinit var params: WindowManager.LayoutParams
//    private var xForRecord = 0
//    private var yForRecord = 0
//    private val location = IntArray(2)
//    private val isOn = false
    private var endpoint: String? = null

    override fun onCreate() {
        super.onCreate()
//        view = LayoutInflater.from(this).inflate(R.layout.widget, null)
//        val overlayParam =
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
//                } else {
//                    WindowManager.LayoutParams.TYPE_PHONE
//                }
//        params = WindowManager.LayoutParams(
//                WindowManager.LayoutParams.WRAP_CONTENT,
//                WindowManager.LayoutParams.WRAP_CONTENT,
//                overlayParam,
//                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
//                PixelFormat.TRANSLUCENT)
//        manager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
//        manager.addView(view, params)
//        //adding an touchlistener to make drag movement of the floating widget
//        val builder = VmPolicy.Builder()
//        StrictMode.setVmPolicy(builder.build())
//        view.findViewById<View>(R.id.buttonClick).setOnTouchListener(object : OnTouchListener {
//            private var initialX = 0
//            private var initialY = 0
//            private var initialTouchX = 0f
//            private var initialTouchY = 0f
//            override fun onTouch(v: View, event: MotionEvent): Boolean {
//                Log.d("TOUCH", "THIS IS TOUCHED")
//                when (event.action) {
//                    MotionEvent.ACTION_DOWN -> {
//                        initialX = params.x
//                        initialY = params.y
//                        initialTouchX = event.rawX
//                        initialTouchY = event.rawY
//                        //this code is helping the widget to move around the screen with fingers
//                        params.x = initialX + (event.rawX - initialTouchX).toInt()
//                        params.y = initialY + (event.rawY - initialTouchY).toInt()
//                        manager.updateViewLayout(view, params)
//                    }
//                    MotionEvent.ACTION_UP, MotionEvent.ACTION_MOVE -> {
//                        params.x = initialX + (event.rawX - initialTouchX).toInt()
//                        params.y = initialY + (event.rawY - initialTouchY).toInt()
//                        manager.updateViewLayout(view, params)
//                    }
//                }
//                return false
//            }
//        })
//        val startButton: TextView = view.findViewById<TextView>(R.id.buttonClick)

//        manager.updateViewLayout(view, params)
        Log.e(TAG, "RTP Display service create")
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelId, NotificationManager.IMPORTANCE_HIGH)
            notificationManager?.createNotificationChannel(channel)
        }
        keepAliveTrick()


    }

    private fun keepAliveTrick() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            val notification = NotificationCompat.Builder(this, channelId)
                    .setOngoing(true)
                    .setContentTitle("")
                    .setContentText("").build()
            startForeground(1, notification)
        } else {
            startForeground(1, Notification())
        }
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    Log.e(TAG, "RTP Display service started")
    endpoint = intent?.extras?.getString("endpoint")
    if (endpoint != null) {
      prepareStreamRtp()
      startStreamRtp(endpoint!!)
    }
    return START_STICKY
  }

    companion object {
        private val TAG = "DisplayService"
        private val channelId = "rtpDisplayStreamChannel"
        private val notifyId = 123456
        private var notificationManager: NotificationManager? = null
        private var displayBase: DisplayBase? = null
        private var contextApp: Context? = null
        private var resultCode: Int? = null
        private var data: Intent? = null

        fun init(context: Context) {
            contextApp = context
            if (displayBase == null) displayBase = RtmpDisplay(context, true, connectCheckerRtp)
        }

        fun setData(resultCode: Int, data: Intent) {
            this.resultCode = resultCode
            this.data = data
        }

        fun sendIntent(): Intent? {
            if (displayBase != null) {
                return displayBase!!.sendIntent()
            } else {
                return null
            }
        }

        fun isStreaming(): Boolean {
            return if (displayBase != null) displayBase!!.isStreaming else false
        }

        fun isRecording(): Boolean {
            return if (displayBase != null) displayBase!!.isRecording else false
        }

        fun stopStream() {
            if (displayBase != null) {
                if (displayBase!!.isStreaming) displayBase!!.stopStream()
            }
        }

        private val connectCheckerRtp = object : ConnectCheckerRtp {
            override fun onConnectionSuccessRtp() {
                showNotification("Stream started")
                Log.e(TAG, "RTP service destroy")
            }

            override fun onNewBitrateRtp(bitrate: Long) {

            }

            override fun onConnectionFailedRtp(reason: String) {
                showNotification("Stream connection failed")
                Log.e(TAG, "RTP service destroy")
            }

            override fun onDisconnectRtp() {
                showNotification("Stream stopped")
            }

            override fun onAuthErrorRtp() {
                showNotification("Stream auth error")
            }

            override fun onAuthSuccessRtp() {
                showNotification("Stream auth success")
            }
        }

        private fun showNotification(text: String) {
            contextApp?.let {
                val notification = NotificationCompat.Builder(it, channelId)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("RTP Display Stream")
                        .setContentText(text).build()
                notificationManager?.notify(notifyId, notification)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.e(TAG, "RTP Display service destroy")
        stopStream()
    }

    private fun prepareStreamRtp() {
        stopStream()
        if (endpoint!!.startsWith("rtmp")) {
            displayBase = RtmpDisplay(baseContext, true, connectCheckerRtp)
            displayBase?.setIntentResult(resultCode!!, data)
        } else {
            displayBase = RtspDisplay(baseContext, true, connectCheckerRtp)
            displayBase?.setIntentResult(resultCode!!, data)
        }
    }

    private fun startStreamRtp(endpoint: String) {
        if (!displayBase!!.isStreaming) {
            if (displayBase!!.prepareVideo() && displayBase!!.prepareAudio()) {
                displayBase!!.startStream(endpoint)
            }
        } else {
            showNotification("You are already streaming :(")
        }
    }




}
