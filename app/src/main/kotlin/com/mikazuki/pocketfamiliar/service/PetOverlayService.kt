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
import com.mikazuki.pocketfamiliar.data.FamiliarProgressRepository
import com.mikazuki.pocketfamiliar.data.PetSettingsRepository
import com.mikazuki.pocketfamiliar.model.FamiliarRewardEngine
import com.mikazuki.pocketfamiliar.model.PetRegistry
import com.mikazuki.pocketfamiliar.model.PetSettings
import com.mikazuki.pocketfamiliar.model.TouchInteraction
import com.mikazuki.pocketfamiliar.overlay.PetOverlayManager
import com.mikazuki.pocketfamiliar.pet.behavior.PetState
import com.mikazuki.pocketfamiliar.pet.behavior.PetStateMachine
import com.mikazuki.pocketfamiliar.pet.behavior.isAirborne
import com.mikazuki.pocketfamiliar.pet.physics.ForcedTransition
import com.mikazuki.pocketfamiliar.pet.physics.ImpactSeverity
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
import kotlin.math.hypot

private const val TAG = "PetOverlayService"
private const val NOTIFICATION_ID = 1001
private const val CHANNEL_ID = "pocket_familiar_overlay"
private const val TICK_MS = 16L
private const val THROW_THRESHOLD_PX_S = 180f
private const val SOFT_CATCH_SPEED_PX_S = 650f

const val ACTION_STOP_SERVICE = "com.mikazuki.pocketfamiliar.STOP"

class PetOverlayService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private lateinit var overlayManager: PetOverlayManager
    private lateinit var physics: PetPhysicsEngine
    private lateinit var stateMachine: PetStateMachine
    private lateinit var batteryMonitor: BatteryMonitor
    private lateinit var settingsRepository: PetSettingsRepository
    private lateinit var progressRepository: FamiliarProgressRepository
    private lateinit var displayManager: DisplayManager

    private var settings: PetSettings = PetSettings()
    private var tickJob: Job? = null
    private var isOverlayRunning = false
    private var currentJuggleCombo = 0
    private var touchGestureConsumed = false

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayChanged(displayId: Int) { if (isOverlayRunning) updateScreenDimensions() }
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit
    }

    override fun onCreate() {
        super.onCreate()
        settingsRepository = PetSettingsRepository(applicationContext)
        progressRepository = FamiliarProgressRepository(applicationContext)
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
            onTouchInteraction = ::onTouchInteraction,
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_SERVICE) {
            stopSelf()
            return START_NOT_STICKY
        }

        createNotificationChannel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, buildNotification())
        }

        if (!Settings.canDrawOverlays(this)) {
            Log.e(TAG, "Overlay permission not granted")
            stopSelf()
            return START_NOT_STICKY
        }
        if (isOverlayRunning) return START_STICKY

        batteryMonitor.register()
        displayManager.registerDisplayListener(displayListener, Handler(Looper.getMainLooper()))

        serviceScope.launch {
            settings = settingsRepository.settingsFlow.first()
            startOverlay()

            settingsRepository.settingsFlow.collect { newSettings ->
                val sizeChanged = newSettings.petSize != settings.petSize
                val profileChanged = newSettings.selectedPetId != settings.selectedPetId
                settings = newSettings

                if (sizeChanged) {
                    overlayManager.updatePetSize(newSettings.petSize)
                    physics.petWidth = overlayManager.petSizePx
                    physics.petHeight = overlayManager.petSizePx
                }
                if (profileChanged) {
                    val profile = PetRegistry.getById(newSettings.selectedPetId)
                    overlayManager.setProfile(profile)
                    physics.profile = profile.physics
                    currentJuggleCombo = 0
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        tickJob?.cancel()
        stateMachine.stop()
        batteryMonitor.unregister()
        displayManager.unregisterDisplayListener(displayListener)
        overlayManager.remove()
        serviceScope.cancel()
        isOverlayRunning = false
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startOverlay() {
        val sw = overlayManager.getScreenWidth()
        val sh = overlayManager.getScreenHeight()
        val nb = overlayManager.getNavBarHeightPx()
        val profile = PetRegistry.getById(settings.selectedPetId)

        overlayManager.create(settings.petSize, sw / 2, sh / 4)
        overlayManager.setProfile(profile)

        physics.apply {
            this.profile = profile.physics
            screenWidth = sw
            screenHeight = sh
            bottomInsetPx = nb
            petWidth = overlayManager.petSizePx
            petHeight = overlayManager.petSizePx
            x = (sw / 2 - petWidth / 2).toFloat()
            y = (sh / 4).toFloat()
        }

        isOverlayRunning = true
        stateMachine.start()
        startTickLoop()
    }

    private fun startTickLoop() {
        tickJob?.cancel()
        tickJob = serviceScope.launch {
            var lastMs = System.currentTimeMillis()
            while (true) {
                delay(TICK_MS)
                val now = System.currentTimeMillis()
                val delta = ((now - lastMs) / 1000f).coerceAtMost(0.1f)
                lastMs = now
                tick(delta)
            }
        }
    }

    private fun tick(delta: Float) {
        val state = stateMachine.currentState
        if (state !is PetState.Held) {
            val transition = physics.update(state, delta, settings.movementSpeed)
            transition?.let(::handleForcedTransition)
            overlayManager.updatePosition(physics.x, physics.y)
        }
        overlayManager.tick()
    }

    private fun onPetStateChanged(state: PetState) {
        overlayManager.applyState(state)
        if (state is PetState.Jumping) {
            physics.launchFromEdge(fromLeftWall = physics.x <= 1f)
        }
    }

    private fun onDragStarted(startX: Float, startY: Float) {
        touchGestureConsumed = false
        val previousState = stateMachine.currentState
        val catchSpeed = hypot(physics.velocityX, physics.velocityY)

        if (previousState.isAirborne) {
            currentJuggleCombo += 1
            rewardTouch(TouchInteraction.CATCH, currentJuggleCombo)
            rewardTouch(TouchInteraction.JUGGLE, currentJuggleCombo)
            if (catchSpeed <= SOFT_CATCH_SPEED_PX_S) {
                rewardTouch(TouchInteraction.SOFT_CATCH, currentJuggleCombo)
            }
            progressRepository.recordJuggle(activeProfile().id, currentJuggleCombo)
        } else {
            currentJuggleCombo = 0
        }

        physics.x = startX
        physics.y = startY
        physics.velocityX = 0f
        physics.velocityY = 0f
        stateMachine.forceState(PetState.Held)
    }

    private fun onDragMoved(x: Float, y: Float) {
        physics.x = x
        physics.y = y
    }

    private fun onTouchInteraction(interaction: TouchInteraction) {
        touchGestureConsumed = true
        rewardTouch(interaction, currentJuggleCombo.coerceAtLeast(1))
        stateMachine.forceState(PetState.Happy)
    }

    private fun onDragReleased(releaseVelocityX: Float, releaseVelocityY: Float) {
        physics.x = overlayManager.currentX().toFloat()
        physics.y = overlayManager.currentY().toFloat()

        if (touchGestureConsumed) {
            touchGestureConsumed = false
            physics.velocityX = 0f
            physics.velocityY = 0f
            return
        }

        physics.onThrown(releaseVelocityX, releaseVelocityY)
        val releaseSpeed = hypot(releaseVelocityX, releaseVelocityY)
        if (releaseSpeed >= THROW_THRESHOLD_PX_S) {
            rewardTouch(TouchInteraction.THROW, currentJuggleCombo.coerceAtLeast(1))
            stateMachine.forceState(PetState.Thrown)
        } else {
            stateMachine.forceState(PetState.Falling)
        }
    }

    private fun rewardTouch(interaction: TouchInteraction, combo: Int) {
        val profile = activeProfile()
        val reward = FamiliarRewardEngine.rewardForInteraction(interaction, profile.preferences, combo)
        val progress = progressRepository.addReward(profile.id, reward)
        Log.d(
            TAG,
            "interaction=$interaction combo=$combo bond=${progress.bondXp} play=${progress.playXp} level=${progress.level} playLevel=${progress.playLevel} bonus=${reward.preferenceBonusApplied}",
        )
    }

    private fun activeProfile() = PetRegistry.getById(settings.selectedPetId)

    private fun handleForcedTransition(t: ForcedTransition) {
        when (t) {
            ForcedTransition.TurnLeft -> stateMachine.forceState(PetState.WalkLeft)
            ForcedTransition.TurnRight -> stateMachine.forceState(PetState.WalkRight)
            ForcedTransition.ClimbLeft -> stateMachine.forceState(PetState.ClimbLeft)
            ForcedTransition.ClimbRight -> stateMachine.forceState(PetState.ClimbRight)
            ForcedTransition.JumpOff -> stateMachine.forceState(PetState.Jumping)
            ForcedTransition.Land -> {
                currentJuggleCombo = 0
                val landingState = when (physics.lastImpactSeverity) {
                    ImpactSeverity.HARD,
                    ImpactSeverity.CATASTROPHIC -> PetState.HardLanding
                    ImpactSeverity.SOFT,
                    ImpactSeverity.NORMAL -> PetState.Idle
                }
                stateMachine.forceState(landingState)
            }
        }
    }

    private fun updateScreenDimensions() {
        physics.onScreenSizeChanged(
            overlayManager.getScreenWidth(),
            overlayManager.getScreenHeight(),
            overlayManager.getNavBarHeightPx(),
        )
    }

    private fun createNotificationChannel() {
        val ch = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.notification_channel_description)
            setShowBadge(false)
            enableVibration(false)
            enableLights(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }

    private fun buildNotification(): Notification {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val stop = PendingIntent.getService(
            this, 1,
            Intent(this, PetOverlayService::class.java).apply { action = ACTION_STOP_SERVICE },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setContentIntent(open)
            .addAction(0, getString(R.string.notification_action_stop), stop)
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
