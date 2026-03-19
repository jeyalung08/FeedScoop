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
 * /device/order fields the firmware reads:
 *   requiredWeight   — target weight in kg
 *   orderActive      — true while order is in progress
 *   orderComplete    — true = Accept tapped (ESP32 adds to cumulative, tares, clears)
 *   orderCancelled   — true = Proceed or Cancel tapped (ESP32 resets to IDLE, clears)
 *   resetCumulative  — true = reset cumulative total and tare (ESP32 zeroes + clears)
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
    private val buttonRef      = database.getReference("device/button")
    private var liveListener:   ValueEventListener? = null
    private var buttonListener: ValueEventListener? = null

    // True when the physical button on the scoop was pressed in IDLE state.
    // The app reads this to navigate to AvailableProductsScreen.
    private val _buttonPressed = MutableStateFlow(false)
    val buttonPressed: StateFlow<Boolean> = _buttonPressed.asStateFlow()

    init {
        startListeningToDevice()
        startListeningToButton()
    }

    // ─────────────────────────────────────────────────────────────────
    // Real-time listener on /device/live
    // ESP32 pushes here every 300ms via writeLiveData()
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
                // Explicitly zero weights when device returns to IDLE
                // so the app never shows stale measurement from previous order
                if (status == "IDLE") {
                    _currentWeight.value    = 0.0
                    _cumulativeWeight.value = 0.0
                }
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
    // Real-time listener on /device/button
    // ESP32 writes pressed=true when physical button is pressed in IDLE.
    // App reads this, navigates to AvailableProductsScreen, then clears it.
    // ─────────────────────────────────────────────────────────────────
    private fun startListeningToButton() {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val pressed = snapshot.child("pressed").getValue(Boolean::class.java) ?: false
                if (pressed) {
                    _buttonPressed.value = true
                    Log.d(TAG, "Physical button pressed — signalling app")
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "button listener cancelled: ${error.message}")
            }
        }
        buttonRef.addValueEventListener(listener)
        buttonListener = listener
    }

    // Call this after navigating to AvailableProductsScreen so the
    // signal is cleared and the button can be used again next time.
    fun clearButtonPressed() {
        _buttonPressed.value = false
        buttonRef.child("pressed").setValue(false)
        Log.d(TAG, "buttonPressed cleared")
    }

    // ─────────────────────────────────────────────────────────────────
    // START ORDER
    // Writes requiredWeight + orderActive=true + clears all flags
    // ESP32 picks this up within ~200ms and calls startOrder()
    // ─────────────────────────────────────────────────────────────────
    fun startOrder(requiredWeightKg: Double) {
        Log.d(TAG, "startOrder: requiredWeight=$requiredWeightKg")
        val map = mapOf(
            "requiredWeight"  to requiredWeightKg,
            "orderActive"     to true,
            "orderComplete"   to false,
            "orderCancelled"  to false,
            "resetCumulative" to false
        )
        orderRef.setValue(map) { error, _ ->
            if (error != null) Log.e(TAG, "startOrder FAILED: ${error.message}")
            else Log.d(TAG, "startOrder written")
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // ACCEPT MEASUREMENT
    // Writes orderComplete=true
    // ESP32 reads current scale weight, adds to totalWeight, tares, clears flag
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
    // ESP32 calls tareNoDelay() + resetSmoothBuffer() and clears flag
    // ─────────────────────────────────────────────────────────────────
    fun tareScale() {
        Log.d(TAG, "tareScale: sending tare command")
        calibrationRef.setValue(mapOf("tare" to true)) { error, _ ->
            if (error != null) Log.e(TAG, "tareScale FAILED: ${error.message}")
            else Log.d(TAG, "tare written")
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // RESET CUMULATIVE WEIGHT
    // Writes resetCumulative=true — ESP32 zeroes totalWeight and tares
    // Order stays active with same target weight
    // ─────────────────────────────────────────────────────────────────
    fun resetCumulative(requiredWeightKg: Double) {
        Log.d(TAG, "resetCumulative: requiredWeight=$requiredWeightKg")
        val map = mapOf(
            "requiredWeight"  to requiredWeightKg,
            "orderActive"     to true,
            "orderComplete"   to false,
            "orderCancelled"  to false,
            "resetCumulative" to true
        )
        orderRef.setValue(map) { error, _ ->
            if (error != null) Log.e(TAG, "resetCumulative FAILED: ${error.message}")
            else Log.d(TAG, "resetCumulative written")
        }
        // Reset local immediately so UI updates without waiting for Firebase
        _cumulativeWeight.value = 0.0
    }

    // ─────────────────────────────────────────────────────────────────
    // RESET ORDER (Proceed to checkout / Cancel order)
    // Writes orderCancelled=true — explicit, unambiguous cancel signal
    // ESP32 reads it → stops buzzer → LEDs off → resets to IDLE
    // ─────────────────────────────────────────────────────────────────
    fun resetOrder() {
        Log.d(TAG, "resetOrder: writing orderCancelled=true")
        val map = mapOf(
            "orderActive"     to false,
            "orderComplete"   to false,
            "orderCancelled"  to true,
            "resetCumulative" to false
        )
        orderRef.setValue(map) { error, _ ->
            if (error != null) Log.e(TAG, "resetOrder FAILED: ${error.message}")
            else Log.d(TAG, "orderCancelled written")
        }
        // Reset local state immediately
        _currentWeight.value    = 0.0
        _cumulativeWeight.value = 0.0
        _deviceStatus.value     = "IDLE"
    }

    // ─────────────────────────────────────────────────────────────────
    // CALIBRATION (CalibrationScreen tare button)
    // Same as tareScale() but also updates calibrationStatus in UI
    // ─────────────────────────────────────────────────────────────────
    fun startCalibration() {
        _calibrationStatus.value = "Sending tare command..."
        viewModelScope.launch {
            calibrationRef.setValue(mapOf("tare" to true))
            _calibrationStatus.value = "Tare command sent"
            Log.d(TAG, "Calibration tare sent")
        }
    }

    // Legacy alias — keeps existing callers compiling
    fun resetWeight() = resetOrder()

    override fun onCleared() {
        super.onCleared()
        liveListener?.let   { liveRef.removeEventListener(it) }
        buttonListener?.let { buttonRef.removeEventListener(it) }
    }
}