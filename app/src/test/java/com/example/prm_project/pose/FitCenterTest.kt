package com.example.prm_project.pose

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Test

class FitCenterTest {

    @Test
    fun `480x640 이미지를 1080x1200 칸에 넣으면 좌우 여백 90`() {
        val fit = FitCenter(480, 640, 1080f, 1200f)

        assertEquals(1.875f, fit.scale, 1e-4f)
        assertEquals(Offset(90f, 0f), fit.toView(0f, 0f))
        assertEquals(Offset(540f, 600f), fit.toView(0.5f, 0.5f))
        assertEquals(Offset(990f, 1200f), fit.toView(1f, 1f))
    }

    @Test
    fun `칸이 이미지보다 세로로 납작하면 위아래 여백`() {
        val fit = FitCenter(480, 640, 1080f, 2400f) // scale = 1080/480 = 2.25, 이미지 높이 1440

        assertEquals(Offset(0f, 480f), fit.toView(0f, 0f))
        assertEquals(Offset(1080f, 1920f), fit.toView(1f, 1f))
    }
}
