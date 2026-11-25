package com.example.prm_project.data

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.prm_project.model.CalibrationConfig
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CalibrationConfigStoreTest {
    @Test
    fun defaultsAndLatestSavedOffsetsAreAvailableToNewStore() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "calibration_config_test"
        context.deleteSharedPreferences(name)
        try {
            val store = CalibrationConfigStore(context.getSharedPreferences(name, Context.MODE_PRIVATE))
            assertEquals(CalibrationConfig(0f, 0L), store.load())

            store.save(CalibrationConfig(4f, 200L))
            val updated = CalibrationConfig(2.5f, -75L)
            store.save(updated)

            val reopened = CalibrationConfigStore(context.getSharedPreferences(name, Context.MODE_PRIVATE))
            assertEquals(updated, reopened.load())
        } finally {
            context.deleteSharedPreferences(name)
        }
    }
}
