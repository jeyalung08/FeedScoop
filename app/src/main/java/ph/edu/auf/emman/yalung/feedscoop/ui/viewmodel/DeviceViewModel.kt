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
import javax.inject.Inject

private const val TAG = "DeviceVM"

/**
 * Bridges the Android app and ScoopSense ESP32-C3 via Firebase RTDB.
 *
 * /device/live        — ESP32 writes, app reads (real-time listener)
 * /device/order       — App writes, ESP32 polls every 200ms
 * /device/calibration — App writes tare=true, ESP32 executes + clears
 *
 * /device/order fields:
 *   requiredWeight  — target weight in kg
 *   orderActive     — true while order is in progress
 *   orderComplete   — true = Accept tapped (ESP32 adds to cumulative + clears)
 *   orderCancelled  — true = Proceed or Cancel tapped (ESP32 resets to IDLE + clears)
 */
@HiltViewModel
class DeviceViewModel @Inject constructor(
    private val database: FirebaseDatabase
) : ViewModel() {

    // ── Live data from /device/live ──────────────────────────────────
    private val _currentWeight    = MutableStateFlow(0.0)
    val currentWeight: StateFlow<Double> = _currentWeight.asStateFlow()

    private val _cumulativeWeight = MutableStateFlow(0.0)
    val cumulativeWeight: StateFlow<Double> = _cumulativeWeight.asStateFlow()

    private val _deviceStatus = MutableStateFlow("IDLE")
    val deviceStatus: StateFlow<String> = _deviceStatus.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    // ── Calibration status ───────────────────────────────────────────
    private val _calibrationStatus = MutableStateFlow("Not Calibrated")
    val calibrationStatus: StateFlow<String> = _calibrationStatus.asStateFlow()

    // ── Firebase refs ────────────────────────────────────────────────
    private val liveRef        = database.getReference("device/live")
    private val orderRef       = database.getReference("device/order")
    private val calibrationRef = database.getReference("device/calibration")
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
                if (!snapshot.exists()) { _isConnected.value = false; return }

                val current = snapshot.child("currentWeight").getValue(Double::class.java) ?: 0.0
                val total   = snapshot.child("totalWeight").getValue(Double::class.java)   ?: 0.0
                val status  = snapshot.child("status").getValue(String::class.java)        ?: "IDLE"

                _currentWeight.value    = current
                _cumulativeWeight.value = total
                _deviceStatus.value     = status
                _isConnected.value      = true

                Log.d(TAG, "live: current=$current total=$total status=$status")
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "listener cancelled: ${error.message}")
                _isConnected.value = false
            }
        }
        liveRef.addValueEventListener(listener)
        liveListener = listener
    }

    // ─────────────────────────────────────────────────────────────────
    // START ORDER
    // Writes requiredWeight + orderActive=true + clears all flags
    // ─────────────────────────────────────────────────────────────────
    fun startOrder(requiredWeightKg: Double) {
        Log.d(TAG, "startOrder: requiredWeight=$requiredWeightKg")
        val map = mapOf(
            "requiredWeight"  to requiredWeightKg,
            "orderActive"     to true,
            "orderComplete"   to false,
            "orderCancelled"  to false
        )
        orderRef.setValue(map) { error, _ ->
            if (error != null) Log.e(TAG, "startOrder FAILED: ${error.message}")
            else Log.d(TAG, "startOrder written")
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // ACCEPT MEASUREMENT
    // Writes orderComplete=true — ESP32 adds current weight to
    // cumulative and stays in MEASURING (or goes to COMPLETE if done)
    // ─────────────────────────────────────────────────────────────────
    fun acceptMeasurement() {
        Log.d(TAG, "acceptMeasurement: writing orderComplete=true")
        orderRef.child("orderComplete").setValue(true) { error, _ ->
            if (error != null) Log.e(TAG, "acceptMeasurement FAILED: ${error.message}")
            else Log.d(TAG, "orderComplete written")
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // TARE SCALE
    // Writes tare=true to /device/calibration
    // ESP32 calls tareNoDelay() and clears the flag
    // ─────────────────────────────────────────────────────────────────
    fun tareScale() {
        Log.d(TAG, "tareScale: sending tare command")
        calibrationRef.setValue(mapOf("tare" to true)) { error, _ ->
            if (error != null) Log.e(TAG, "tareScale FAILED: ${error.message}")
            else Log.d(TAG, "tare written")
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // RESET ORDER (Proceed / Cancel)
    // FIX: Now writes orderCancelled=true instead of orderActive=false.
    // This is an explicit, unambiguous signal that can never be
    // accidentally triggered by a stale orderActive value.
    // ESP32 reads orderCancelled=true → calls resetToIdle() → buzzer
    // stops, LEDs off, state=IDLE, ready for next order.
    // ─────────────────────────────────────────────────────────────────
    fun resetOrder() {
        Log.d(TAG, "resetOrder: writing orderCancelled=true")
        val map = mapOf(
            "orderActive"    to false,
            "orderComplete"  to false,
            "orderCancelled" to true
        )
        orderRef.setValue(map) { error, _ ->
            if (error != null) Log.e(TAG, "resetOrder FAILED: ${error.message}")
            else Log.d(TAG, "orderCancelled written")
        }
        // Reset local state immediately so UI updates without waiting
        _currentWeight.value    = 0.0
        _cumulativeWeight.value = 0.0
        _deviceStatus.value     = "IDLE"
    }

    // ─────────────────────────────────────────────────────────────────
    // CALIBRATION (CalibrationScreen tare button)
    // ─────────────────────────────────────────────────────────────────
    fun startCalibration() {
        _calibrationStatus.value = "Sending tare command..."
        viewModelScope.launch {
            calibrationRef.setValue(mapOf("tare" to true))
            _calibrationStatus.value = "Tare command sent"
            Log.d(TAG, "Calibration tare sent")
        }
    }

    // Legacy alias
    fun resetWeight() = resetOrder()

    override fun onCleared() {
        super.onCleared()
        liveListener?.let { liveRef.removeEventListener(it) }
    }
}