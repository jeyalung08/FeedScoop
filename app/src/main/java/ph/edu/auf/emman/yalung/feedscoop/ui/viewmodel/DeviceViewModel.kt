// FILE: ui/viewmodel/DeviceViewModel.kt
package ph.edu.auf.emman.yalung.feedscoop.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

private const val TAG = "DeviceVM"

/**
 * Manages all communication with the ScoopSense ESP32-C3 hardware
 * via Firebase Realtime Database.
 *
 * Firebase paths used:
 *   /device/live  — ESP32 writes current/total weight + status (app reads)
 *   /device/order — App writes requiredWeight, orderActive, orderComplete (ESP32 reads)
 *
 * No direct Bluetooth connection. Both app and ESP32 communicate through Firebase.
 */
@HiltViewModel
class DeviceViewModel @Inject constructor(
    private val database: FirebaseDatabase
) : ViewModel() {

    // ── Live data from ESP32 ─────────────────────────────────────────
    private val _currentWeight = MutableStateFlow(0.0)
    val currentWeight: StateFlow<Double> = _currentWeight.asStateFlow()

    private val _cumulativeWeight = MutableStateFlow(0.0)
    val cumulativeWeight: StateFlow<Double> = _cumulativeWeight.asStateFlow()

    // "IDLE" | "MEASURING" | "WAIT_DISPENSE" | "COMPLETE" | "OVERWEIGHT"
    private val _deviceStatus = MutableStateFlow("IDLE")
    val deviceStatus: StateFlow<String> = _deviceStatus.asStateFlow()

    // ── Connection / calibration ─────────────────────────────────────
    // isConnected = true when Firebase listener is receiving live data from ESP32
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _calibrationStatus = MutableStateFlow("Not Calibrated")
    val calibrationStatus: StateFlow<String> = _calibrationStatus.asStateFlow()

    // ── Firebase refs ────────────────────────────────────────────────
    private val liveRef  = database.getReference("device/live")
    private val orderRef = database.getReference("device/order")

    private var liveListener: ValueEventListener? = null

    init {
        startListeningToDevice()
    }

    // ─────────────────────────────────────────────────────────────────
    // Real-time listener on /device/live
    // ─────────────────────────────────────────────────────────────────
    private fun startListeningToDevice() {
        Log.d(TAG, "Attaching listener to /device/live")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    Log.d(TAG, "No live data yet from ESP32")
                    _isConnected.value = false
                    return
                }

                val current = snapshot.child("currentWeight").getValue(Double::class.java) ?: 0.0
                val total   = snapshot.child("totalWeight").getValue(Double::class.java)   ?: 0.0
                val status  = snapshot.child("status").getValue(String::class.java)         ?: "IDLE"

                Log.d(TAG, "live data: current=$current total=$total status=$status")

                _currentWeight.value    = current
                _cumulativeWeight.value = total
                _deviceStatus.value     = status
                _isConnected.value      = true
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "live listener cancelled: ${error.message}")
                _isConnected.value = false
            }
        }
        liveRef.addValueEventListener(listener)
        liveListener = listener
    }

    // ─────────────────────────────────────────────────────────────────
    // Write order commands to /device/order
    // ─────────────────────────────────────────────────────────────────

    /**
     * Start an order from the app side.
     * Writes requiredWeight + orderActive=true to Firebase.
     * ESP32 picks this up within ~500ms and begins measuring.
     */
    fun startOrder(requiredWeightKg: Double) {
        Log.d(TAG, "startOrder: requiredWeight=$requiredWeightKg")
        val map = mapOf(
            "requiredWeight" to requiredWeightKg,
            "orderActive"    to true,
            "orderComplete"  to false
        )
        orderRef.setValue(map) { error, _ ->
            if (error != null) Log.e(TAG, "startOrder FAILED: ${error.message}")
            else Log.d(TAG, "startOrder SUCCESS")
        }
    }

    /**
     * Accept current scoop measurement.
     * Writes orderComplete=true → ESP32 tares and resets for next scoop.
     */
    fun acceptMeasurement() {
        Log.d(TAG, "acceptMeasurement: writing orderComplete=true")
        orderRef.child("orderComplete").setValue(true) { error, _ ->
            if (error != null) Log.e(TAG, "acceptMeasurement FAILED: ${error.message}")
            else Log.d(TAG, "acceptMeasurement SUCCESS")
        }
    }

    /**
     * Cancel / reset the current order.
     * Clears all order flags in Firebase. ESP32 returns to IDLE.
     */
    fun resetOrder() {
        Log.d(TAG, "resetOrder")
        val map = mapOf(
            "requiredWeight" to 0.0,
            "orderActive"    to false,
            "orderComplete"  to false
        )
        orderRef.setValue(map) { error, _ ->
            if (error != null) Log.e(TAG, "resetOrder FAILED: ${error.message}")
            else Log.d(TAG, "resetOrder SUCCESS")
        }
        // Reset local state immediately so UI updates without waiting for ESP32
        _currentWeight.value    = 0.0
        _cumulativeWeight.value = 0.0
        _deviceStatus.value     = "IDLE"
    }

    /**
     * Trigger calibration from the app.
     * Writes a tare command — ESP32 should be extended to read this
     * and call LoadCell.tareNoDelay().
     */
    fun startCalibration() {
        _calibrationStatus.value = "Calibrating..."
        viewModelScope.launch {
            try {
                database.getReference("device/calibration")
                    .setValue(mapOf("tare" to true, "timestamp" to System.currentTimeMillis()))
                    .await()
                _calibrationStatus.value = "Calibration Complete"
                Log.d(TAG, "Calibration command sent")
            } catch (e: Exception) {
                _calibrationStatus.value = "Calibration Failed"
                Log.e(TAG, "Calibration failed: ${e.message}")
            }
        }
    }

    // Legacy compatibility — kept so existing callers don't break
    fun resetWeight() = resetOrder()

    override fun onCleared() {
        super.onCleared()
        liveListener?.let { liveRef.removeEventListener(it) }
    }
}