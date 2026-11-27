package com.example.prm_project.data

import android.content.Context
import android.content.SharedPreferences
import com.example.prm_project.model.CalibrationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 각도와 지연 보정값을 앱 전용 환경설정 파일에 함께 저장한다. */
class CalibrationConfigStore internal constructor(private val preferences: SharedPreferences) {
    constructor(context: Context) : this(
        context.applicationContext.getSharedPreferences("calibration_config", Context.MODE_PRIVATE),
    )

    fun load(): CalibrationConfig {
        val values = preferences.all
        val defaults = CalibrationConfig()
        return CalibrationConfig(
            toleranceOffsetDeg = values[TOLERANCE_OFFSET] as? Float ?: defaults.toleranceOffsetDeg,
            poseLatencyOffsetMs = values[LATENCY_OFFSET] as? Long ?: defaults.poseLatencyOffsetMs,
        )
    }

    /** 디스크에 저장된 뒤 반환한다. 저장에 실패하면 예외를 던진다. */
    suspend fun save(config: CalibrationConfig): Unit = withContext(Dispatchers.IO) {
        check(
            preferences.edit()
                .putFloat(TOLERANCE_OFFSET, config.toleranceOffsetDeg)
                .putLong(LATENCY_OFFSET, config.poseLatencyOffsetMs)
                .commit(),
        ) { "Could not persist calibration config" }
    }

    private companion object {
        const val TOLERANCE_OFFSET = "tolerance_offset_deg"
        const val LATENCY_OFFSET = "pose_latency_offset_ms"
    }
}
