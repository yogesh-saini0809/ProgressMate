package com.progressmate.app.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.*
import com.progressmate.app.data.AppDatabase
import com.progressmate.app.data.TaskEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

class FloatingWidgetService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private lateinit var params: WindowManager.LayoutParams
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private lateinit var database: AppDatabase
    private var activeTask: TaskEntity? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        setupLayout()
        observeData()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupLayout() {
        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 50
            y = 200
        }

       val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(android.graphics.Color.rgb(30, 41, 59))
                cornerRadius = 32f
            }
            elevation = 18f
            clipToOutline = true
        }

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 20, 28, 20)
        }

        val topRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val checkbox = CheckBox(this).apply {
            buttonTintList = android.content.res.ColorStateList.valueOf(0xFF38BDF8.toInt())
        }

        val taskTitle = TextView(this).apply {
            tag = "task_text"
            text = "Loading today's focus..."
            setTextColor(0xFFF8FAFC.toInt())
            textSize = 14f
            maxLines = 1
            setPadding(8, 0, 16, 0)
        }

        val timerBadge = TextView(this).apply {
            tag = "timer_text"
            text = "25:00"
            setTextColor(0xFF38BDF8.toInt())
            textSize = 13f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }

        val closeBtn = ImageView(this).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            setColorFilter(0xFF94A3B8.toInt())
            setPadding(16, 0, 0, 0)
            setOnClickListener { stopSelf() }
        }

        topRow.addView(checkbox)
        topRow.addView(taskTitle)
        topRow.addView(timerBadge)
        topRow.addView(closeBtn)
        rootLayout.addView(topRow)
        card.addView(rootLayout)

        floatingView = card

        floatingView.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f

            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(floatingView, params)
                        return true
                    }
                }
                return false
            }
        })

        checkbox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && activeTask != null) {
                serviceScope.launch(Dispatchers.IO) {
                    database.progressDao().updateTask(
                        activeTask!!.copy(
                            isCompleted = true,
                            completedAtEpochMs = System.currentTimeMillis()
                        )
                    )
                }
            }
        }

        windowManager.addView(floatingView, params)

        serviceScope.launch {
            TimerService.timerFlow.collectLatest { state ->
                val mins = state.remainingSeconds / 60
                val secs = state.remainingSeconds % 60
                timerBadge.text = "%02d:%02d".format(mins, secs)
            }
        }
    }

    private fun observeData() {
        serviceScope.launch {
            while (isActive) {
                val task = database.progressDao().getTopUnfinishedTask()
                activeTask = task
                withContext(Dispatchers.Main) {
                    val tv = floatingView.findViewWithTag<TextView>("task_text")
                    tv?.text = task?.title ?: "No active tasks!"
                }
                delay(3000)
            }
        }
    }

    override fun onDestroy() {
        if (::floatingView.isInitialized) {
            windowManager.removeView(floatingView)
        }
        serviceScope.cancel()
        super.onDestroy()
    }
}
