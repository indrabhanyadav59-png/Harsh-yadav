package com.example.location

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.repository.IndraGameRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

data class LocationTrackingSnapshot(
    val isTracking: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val accuracyMeters: Float? = null,
    val updatedAtEpochMs: Long = 0L,
    val statusMessage: String = "Location sync off"
)

object LocationTrackingState {
    private val _state = MutableStateFlow(LocationTrackingSnapshot())
    val state: StateFlow<LocationTrackingSnapshot> = _state.asStateFlow()

    fun setStatus(isTracking: Boolean, message: String) {
        _state.value = _state.value.copy(
            isTracking = isTracking,
            statusMessage = message
        )
    }

    fun updateCoordinate(
        latitude: Double,
        longitude: Double,
        accuracyMeters: Float?,
        message: String
    ) {
        _state.value = _state.value.copy(
            isTracking = true,
            latitude = latitude,
            longitude = longitude,
            accuracyMeters = accuracyMeters,
            updatedAtEpochMs = System.currentTimeMillis(),
            statusMessage = message
        )
    }

    fun reset() {
        _state.value = LocationTrackingSnapshot()
    }
}

class LocationTrackingService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var targetUserId: String? = null
    private var updatesRequested = false

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { processLocation(it) }
        }
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        ensureNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopTrackingInternal()
            stopSelf()
            return START_NOT_STICKY
        }

        if (!hasLocationPermission(this)) {
            LocationTrackingState.setStatus(false, "Location permission required")
            stopSelf()
            return START_NOT_STICKY
        }

        targetUserId = intent?.getStringExtra(EXTRA_USER_ID)?.takeIf { it.isNotBlank() }
            ?: runCatching { Firebase.auth.currentUser?.uid }.getOrNull()

        val startedForeground = startForegroundSafely()
        if (!startedForeground) {
            LocationTrackingState.setStatus(false, "Could not start foreground location service")
            stopSelf()
            return START_NOT_STICKY
        }

        requestLocationUpdatesSafely()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopTrackingInternal()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startForegroundSafely(): Boolean {
        return try {
            val notification = buildNotification(getString(R.string.location_notification_text))
            val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            } else {
                0
            }
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, serviceType)
            true
        } catch (_: SecurityException) {
            false
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationUpdatesSafely() {
        if (!hasLocationPermission(this)) return
        LocationTrackingState.setStatus(true, "Acquiring GPS coordinates...")

        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    processLocation(location)
                }
            }

            if (!updatesRequested) {
                val request = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 10_000L)
                    .setMinUpdateIntervalMillis(5_000L)
                    .setMinUpdateDistanceMeters(5f)
                    .build()
                fusedLocationClient.requestLocationUpdates(
                    request,
                    locationCallback,
                    Looper.getMainLooper()
                )
                updatesRequested = true
            }
        } catch (_: SecurityException) {
            LocationTrackingState.setStatus(false, "Location permission revoked")
            stopSelf()
        }
    }

    internal fun processLocation(location: Location) {
        val lat = location.latitude
        val lng = location.longitude
        if (!lat.isFinite() || !lng.isFinite() || lat !in -90.0..90.0 || lng !in -180.0..180.0) {
            return
        }
        val accuracy = if (location.hasAccuracy()) location.accuracy else null
        val coordText = String.format(Locale.US, "%.5f, %.5f", lat, lng)
        LocationTrackingState.updateCoordinate(
            latitude = lat,
            longitude = lng,
            accuracyMeters = accuracy,
            message = "Synced ($coordText)"
        )

        runCatching {
            val manager = getSystemService(NotificationManager::class.java)
            manager?.notify(NOTIFICATION_ID, buildNotification("GPS: $coordText"))
        }

        val uid = runCatching { Firebase.auth.currentUser?.uid }.getOrNull()
        if (uid != null && (targetUserId == null || targetUserId == uid)) {
            serviceScope.launch {
                val repo = IndraGameRepository(applicationContext)
                repo.updatePlayerLocation(
                    uid = uid,
                    latitude = lat,
                    longitude = lng,
                    accuracyMeters = accuracy?.toDouble(),
                    trackingEnabled = true
                )
            }
        }
    }

    private fun stopTrackingInternal() {
        if (updatesRequested && ::fusedLocationClient.isInitialized) {
            runCatching { fusedLocationClient.removeLocationUpdates(locationCallback) }
            updatesRequested = false
        }
        LocationTrackingState.setStatus(false, "Location sync off")
        runCatching { ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE) }
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.location_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.location_notification_text)
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(contentText: String): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.location_notification_title))
            .setContentText(contentText)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .build()
    }

    companion object {
        const val ACTION_START = "com.example.location.ACTION_START"
        const val ACTION_STOP = "com.example.location.ACTION_STOP"
        const val EXTRA_USER_ID = "extra_user_id"
        const val CHANNEL_ID = "location_tracking_channel"
        const val NOTIFICATION_ID = 4102

        fun hasLocationPermission(context: Context): Boolean {
            val coarse = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            val fine = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            return coarse || fine
        }

        fun requiredPermissions(): Array<String> {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.POST_NOTIFICATIONS
                )
            } else {
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            }
        }

        fun startIntent(context: Context, userId: String? = null): Intent =
            Intent(context, LocationTrackingService::class.java).apply {
                action = ACTION_START
                if (!userId.isNullOrBlank()) {
                    putExtra(EXTRA_USER_ID, userId)
                }
            }

        fun stopIntent(context: Context): Intent =
            Intent(context, LocationTrackingService::class.java).apply {
                action = ACTION_STOP
            }

        fun startTracking(context: Context, userId: String? = null) {
            ContextCompat.startForegroundService(context, startIntent(context, userId))
        }

        fun stopTracking(context: Context) {
            LocationTrackingState.setStatus(false, "Location sync off")
            runCatching {
                context.stopService(Intent(context, LocationTrackingService::class.java))
            }
        }
    }
}
