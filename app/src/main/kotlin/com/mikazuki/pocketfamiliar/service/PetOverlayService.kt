package com.mikazuki.pocketfamiliar.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import com.mikazuki.pocketfamiliar.MainActivity
import com.mikazuki.pocketfamiliar.R
import com.mikazuki.pocketfamiliar.data.PetSettingsRepository
import com.mikazuki.pocketfamiliar.model.PetSettings
import com.mikazuki.pocketfamiliar.overlay.PetOverlayManager
import com.mikazuki.pocketfamiliar.pet.behavior.PetState
import com.mikazuki.pocketfamiliar.pet.behavior.PetStateMachine
import com.mikazuki.pocketfamiliar.pet.physics.ForcedTransition
import com.mikazuki.pocketfamiliar.pet.physics.PetPhysicsEngine
import com.mikazuki.pocketfamiliar.util.BatteryMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val TAG = "PetOverlayService"
private const val NOTIFICATION_ID = 1001
private const val CHANNEL_ID = "pocket_familiar_overlay"

/** Tick interval for the animation+physics loop (~60 fps). */
private const val TICK_MS = 16L

const val ACTION_STOP_SERVICE = "com.mikazuki.pocketfamiliar.STOP"

class PetOverlayService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private lateinit var overlayManager: PetOverlayManager
    private lateinit var physics: PetPhysicsEngine
    private lateinit var stateMachine: PetStateMachine
    private lateinit var batteryMonitor: BatteryMonitor
    private lateinit var settingsRepository: PetSettingsRepository
    private lateinit var displayManager: DisplayManager

    private var settings: PetSettings = PetSettings()
    private var tickJob: Job? = null
    private var isOverlayRunning = false

    // -------------------------------------------------------------------------
    // DisplayManager.DisplayListener — handles rotation / multi-window / foldables
    // -------------------------------------------------------------------------

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayChanged(displayId: Int) {
            if (!isOverlayRunning) return
            Log.d(TAG, "Display changed — updating screen dimensions")
            updateScreenDimensions()
        }
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit
    }

    // -------------------------------------------------------------------------
    // Service lifecycle
    // -------------------------------------------------------------------------

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")

        settingsRepository = PetSettingsRepository(applicationContext)
        batteryMonitor = BatteryMonitor(applicationContext)
        displayManager = getSystemService(DisplayManager::class.java)

        physics = PetPhysicsEngine()

        stateMachine = PetStateMachine(
            scope = serviceScope,
            onStateChanged = ::onPetStateChanged,
            isSleepEnabled = { settings.sleepEnabled },
        )

        overlayManager = PetOverlayManager(
            context = applicationContext,
            onDragStarted = ::onDragStarted,
            onDragMoved = ::onDragMoved,
            onDragReleased = ::onDragReleased,
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_SERVICE) {
            Log.d(TAG, "Stop action received")
            stopSelf()
            return START_NOT_STICKY
        }

        // Create the notification channel and call startForeground() as quickly
        // as possible (Android 14+ requires it within a few seconds).
        // Android 14+ (API 34) also requires passing the declared service type flag.
        createNotificationChannel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, buildNotification())
        }

        if (!Settings.canDrawOverlays(this)) {
            Log.e(TAG, "Overlay permission not granted — stopping service")
            stopSelf()
            return START_NOT_STICKY
        }

        // Prevent duplicate overlay if onStartCommand is called more than once
        // (e.g. the user taps Start Pet while the service is already running).
        if (isOverlayRunning) {
            Log.d(TAG, "Overlay already running — ignoring duplicate start")
            return START_STICKY
        }

        batteryMonitor.register()
        displayManager.registerDisplayListener(displayListener, Handler(Looper.getMainLooper()))

        serviceScope.launch {
            // Load settings synchronously before creating the overlay so the
            // initial pet size and speed are correct from the first frame.
            settings = settingsRepository.settingsFlow.first()
            startOverlay()

            // Keep reacting to future settings changes.
            settingsRepository.settingsFlow.collect { newSettings ->
                val sizeChanged = newSettings.petSize != settings.petSize
                settings = newSettings
                if (sizeChanged) {
                    overlayManager.updatePetSize(newSettings.petSize)
                    physics.petWidth = overlayManager.petSizePx
                    physics.petHeight = overlayManager.petSizePx
                }
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
        tickJob?.cancel()
        stateMachine.stop()
        batteryMonitor.unregister()
        displayManager.unregisterDisplayListener(displayListener)
        overlayManager.remove()
        serviceScope.cancel()
        isOverlayRunning = false
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // -------------------------------------------------------------------------
    // Overlay initialisation
    // -------------------------------------------------------------------------

    private fun startOverlay() {
        val screenWidth = overlayManager.getScreenWidth()
        val screenHeight = overlayManager.getScreenHeight()
        val navBarPx = overlayManager.getNavBarHeightPx()

        val startX = screenWidth / 2
        val startY = screenHeight / 4

        overlayManager.create(
            petScale = settings.petSize,
            startX = startX,
            startY = startY,
        )

        physics.apply {
            this.screenWidth = screenWidth
            this.screenHeight = screenHeight
            petWidth = overlayManager.petSizePx
            petHeight = overlayManager.petSizePx
            bottomInsetPx = navBarPx
            x = (startX - petWidth / 2).toFloat()
            y = startY.toFloat()
        }

        isOverlayRunning = true
        stateMachine.start()
        startTickLoop()

        Log.d(TAG, "Overlay started: screen=${screenWidth}x${screenHeight} navBar=${navBarPx}px petSize=${overlayManager.petSizePx}px")
    }

    // -------------------------------------------------------------------------
    // Tick loop (~60 fps via coroutine delay)
    // -------------------------------------------------------------------------

    private fun startTickLoop() {
        tickJob?.cancel()
        tickJob = serviceScope.launch {
            var lastMs = System.currentTimeMillis()
            while (true) {
                delay(TICK_MS)
                val now = System.currentTimeMillis()
                // Clamp delta to 100 ms to avoid huge jumps after the app is
                // backgrounded or the screen turns off.
                val delta = ((now - lastMs) / 1000f).coerceAtMost(0.1f)
                lastMs = now
                tick(delta)
            }
        }
    }

    private fun tick(deltaSeconds: Float) {
        // Skip physics-driven position update while the user is dragging —
        // the touch handler owns the position in that state.
        val state = stateMachine.currentState
        if (state !is PetState.Dragged) {
            val transition = physics.update(
                currentState = state,
                deltaSeconds = deltaSeconds,
                movementSpeed = settings.movementSpeed,
            )
            transition?.let { handleForcedTransition(it) }
            overlayManager.updatePosition(physics.x, physics.y)
        }

        // Always advance the sprite animation.
        overlayManager.tick()
    }

    // -------------------------------------------------------------------------
    // State callbacks
    // -------------------------------------------------------------------------

    private fun onPetStateChanged(state: PetState) {
        overlayManager.applyState(state)
    }

    /** Called when the user first touches the pet. */
    private fun onDragStarted(startX: Float, startY: Float) {
        // Sync physics with the actual window position at drag start so that
        // when we release and hand back to physics everything is consistent.
        physics.x = startX
        physics.y = startY
        stateMachine.forceState(PetState.Dragged)
    }

    /** Called on every move event during a drag. */
    private fun onDragMoved(x: Float, y: Float) {
        // Keep physics in sync with the drag position so release is seamless.
        physics.x = x
        physics.y = y
    }

    /** Called when the user lifts their finger. */
    private fun onDragReleased(releaseVelocityY: Float) {
        // physics.x/y are already up to date from the last onDragMoved call.
        physics.onDragReleased(releaseVelocityY)
        stateMachine.forceState(PetState.Falling)
    }

    private fun handleForcedTransition(transition: ForcedTransition) {
        when (transition) {
            ForcedTransition.TurnLeft -> stateMachine.forceState(PetState.WalkLeft)
            ForcedTransition.TurnRight -> stateMachine.forceState(PetState.WalkRight)
            ForcedTransition.Land -> stateMachine.forceState(PetState.Idle)
        }
    }

    // -------------------------------------------------------------------------
    // Screen dimension updates (rotation / multi-window)
    // -------------------------------------------------------------------------

    private fun updateScreenDimensions() {
        val newWidth = overlayManager.getScreenWidth()
        val newHeight = overlayManager.getScreenHeight()
        val newNavBar = overlayManager.getNavBarHeightPx()
        physics.onScreenSizeChanged(newWidth, newHeight, newNavBar)
        Log.d(TAG, "Screen dimensions updated to ${newWidth}x${newHeight} navBar=${newNavBar}px")
    }

    // -------------------------------------------------------------------------
    // Notification
    // -------------------------------------------------------------------------

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.notification_channel_description)
            setShowBadge(false)
            enableVibration(false)
            enableLights(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, PetOverlayService::class.java).apply { action = ACTION_STOP_SERVICE },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setContentIntent(openIntent)
            .addAction(0, getString(R.string.notification_action_stop), stopIntent)
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
