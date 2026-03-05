// File: ui/viewmodel/DeviceViewModel.kt
package ph.edu.auf.emman.yalung.feedscoop.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeviceViewModel @Inject constructor() : ViewModel() {

    // Current weight from prototype/load cell (updated via BLE/WebSocket/etc.)
    private val _currentWeight = MutableStateFlow(0.0)
    val currentWeight: StateFlow<Double> = _currentWeight.asStateFlow()

    // Device connection status
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    // Calibration status message
    private val _calibrationStatus = MutableStateFlow("Not Calibrated")
    val calibrationStatus: StateFlow<String> = _calibrationStatus.asStateFlow()

    // Cumulative accepted weight in current order process
    private val _cumulativeWeight = MutableStateFlow(0.0)
    val cumulativeWeight: StateFlow<Double> = _cumulativeWeight.asStateFlow()

    /** Called when new weight data arrives from the prototype (via BLE / serial bridge) */
    fun updateWeight(weight: Double) {
        _currentWeight.value = weight
    }

    /** Accept the current scoop measurement — adds to cumulative weight and resets current */
    fun acceptMeasurement() {
        _cumulativeWeight.value += _currentWeight.value
        _currentWeight.value = 0.0
    }

    /** Reset both current and cumulative weight (new order or cancel) */
    fun resetWeight() {
        _currentWeight.value = 0.0
        _cumulativeWeight.value = 0.0
    }

    /** Reset only cumulative (tare for next order process) */
    fun resetCumulative() {
        _cumulativeWeight.value = 0.0
    }

    // --- Device connection ---

    fun connectDevice() {
        viewModelScope.launch {
            // TODO: Implement actual BLE/serial connection logic
            _isConnected.value = true
        }
    }

    fun disconnectDevice() {
        viewModelScope.launch {
            // TODO: Implement actual disconnect logic
            _isConnected.value = false
        }
    }

    // --- Calibration ---

    fun startCalibration() {
        viewModelScope.launch {
            _calibrationStatus.value = "Calibrating..."
            // TODO: Send calibration command to firmware
            // Simulate completion:
            _calibrationStatus.value = "Calibration Complete"
        }
    }
}