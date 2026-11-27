package com.example.prm_project.judge

import com.example.prm_project.model.DetectionState
import com.example.prm_project.model.Grade
import com.example.prm_project.model.Joint
import com.example.prm_project.model.JudgeResult
import com.example.prm_project.model.Landmark
import com.example.prm_project.model.PoseFrame
import com.example.prm_project.model.TargetPose
import kotlin.math.cos
import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 목표는 LEFT_ELBOW 90°, 허용 오차 15°. 오차 45°(15 × 3)에서 관절 점수가 0이 된다. */
class GradeJudgeTest {

    /** 100×100 이미지에서 LEFT_ELBOW 각도가 [elbowDeg]인 프레임. 어깨는 팔꿈치 바로 위에 둔다. */
    private fun frame(elbowDeg: Float, detection: DetectionState = DetectionState.DETECTED): PoseFrame {
        val rad = Math.toRadians(elbowDeg.toDouble())
        val wrist = Landmark(0.5f + 0.3f * sin(rad).toFloat(), 0.5f - 0.3f * cos(rad).toFloat(), 1f)
        val landmarks = mapOf(
            Joint.LEFT_SHOULDER to Landmark(0.5f, 0.2f, 1f),
            Joint.LEFT_ELBOW to Landmark(0.5f, 0.5f, 1f),
            Joint.LEFT_WRIST to wrist,
        )
        return PoseFrame(landmarks, 100, 100, 0L, detection)
    }

    private fun judgeAt(frame: PoseFrame): JudgeResult = judge(frame, TARGET, beatIndex = 0, elapsedMs = 0L)

    @Test
    fun `정확도 높음은 PERFECT로 성공`() {
        val result = judgeAt(frame(93f)) // 오차 3° → 1 − 3/45 = 0.933

        assertEquals(Grade.PERFECT, result.grade)
        assertTrue(result.grade.isSuccess)
    }

    @Test
    fun `정확도 중간은 GREAT로 성공`() {
        val result = judgeAt(frame(101f)) // 오차 11° → 1 − 11/45 = 0.756

        assertEquals(Grade.GREAT, result.grade)
        assertTrue(result.grade.isSuccess)
    }

    @Test
    fun `정확도 낮음은 BAD`() {
        val result = judgeAt(frame(112f)) // 오차 22° → 1 − 22/45 = 0.511

        assertEquals(Grade.BAD, result.grade)
    }

    @Test
    fun `정확도 매우 낮음은 MISS로 실패`() {
        val result = judgeAt(frame(130f)) // 오차 40° → 1 − 40/45 = 0.111

        assertEquals(Grade.MISS, result.grade)
        assertFalse(result.grade.isSuccess)
    }

    @Test
    fun `화면 밖으로 나가 관절이 하나도 없으면 MISS로 실패`() {
        val result = judgeAt(frame(90f).copy(landmarks = emptyMap(), detection = DetectionState.USER_CAUSED_LOSS))

        assertEquals(Grade.MISS, result.grade)
        assertFalse(result.grade.isSuccess)
        assertFalse(result.recognitionFailed)
    }

    @Test
    fun `소프트웨어 원인 인식 실패는 GREAT로 성공하고 recognitionFailed가 true`() {
        val result = judgeAt(frame(130f, DetectionState.SOFTWARE_LOSS)) // 각도가 틀려도 GREAT

        assertEquals(Grade.GREAT, result.grade)
        assertTrue(result.grade.isSuccess)
        assertTrue(result.recognitionFailed)
    }

    @Test
    fun `목표 관절을 하나도 잴 수 없으면 소프트웨어 원인 인식 실패와 같다`() {
        val full = frame(90f)
        val result = judgeAt(full.copy(landmarks = full.landmarks - Joint.LEFT_WRIST)) // 손목이 가려짐

        assertEquals(Grade.GREAT, result.grade)
        assertTrue(result.recognitionFailed)
        assertNull(result.wrongJoints)
    }

    @Test
    fun `일부 관절만 잴 수 없으면 남은 관절로 판정한다`() {
        val twoJoints = TARGET.copy(targetAngles = TARGET.targetAngles + (Joint.LEFT_SHOULDER to 30f))
        val result = judge(frame(93f), twoJoints, beatIndex = 0, elapsedMs = 0L) // 엉덩이가 없어 어깨 각도는 못 잼

        assertEquals(Grade.PERFECT, result.grade) // 0점으로 치면 0.467 → BAD
        assertNull(result.wrongJoints)
    }

    @Test
    fun `관절 하나면 허용 오차 안까지만 GREAT 이상`() {
        val atTolerance = matchRate(mapOf(Joint.LEFT_ELBOW to 105f), TARGET) // 오차 15° → 0.667
        val overTolerance = matchRate(mapOf(Joint.LEFT_ELBOW to 105.5f), TARGET) // 오차 15.5° → 0.656

        assertTrue(atTolerance >= GradeThresholds.GREAT_MIN)
        assertTrue(overTolerance < GradeThresholds.GREAT_MIN)
    }

    @Test
    fun `모든 관절이 허용 오차 안이면 wrongJoints가 null`() {
        val result = judgeAt(frame(101f)) // 오차 11° ≤ 15°

        assertNull(result.wrongJoints)
    }

    /**
     * 왼팔을 몸통에서 [shoulderDeg]만큼 벌리고 곧게 편 프레임. 어깨 각도 = [shoulderDeg], 팔꿈치 각도 = 180°.
     * [out] 관절은 화면 밖이라 landmarks에서 빼고 outOfFrame에 넣는다.
     */
    private fun armFrame(
        shoulderDeg: Float,
        out: Set<Joint> = emptySet(),
        detection: DetectionState = if (out.isEmpty()) DetectionState.DETECTED else DetectionState.USER_CAUSED_LOSS,
    ): PoseFrame {
        val rad = Math.toRadians(shoulderDeg.toDouble())
        fun along(r: Float) = Landmark(0.5f + r * sin(rad).toFloat(), 0.3f + r * cos(rad).toFloat(), 1f)
        val all = mapOf(
            Joint.LEFT_HIP to Landmark(0.5f, 0.7f, 1f),
            Joint.LEFT_SHOULDER to Landmark(0.5f, 0.3f, 1f),
            Joint.LEFT_ELBOW to along(0.15f),
            Joint.LEFT_WRIST to along(0.3f),
        )
        return PoseFrame(all - out, 100, 100, 0L, detection, outOfFrame = out)
    }

    private fun judgeArm(frame: PoseFrame, target: TargetPose = ARM) = judge(frame, target, beatIndex = 0, elapsedMs = 0L)

    @Test
    fun `손목만 화면 밖이어도 어깨 각도가 맞으면 판정한다`() {
        val result = judgeArm(armFrame(90f, out = setOf(Joint.LEFT_WRIST))) // 팔꿈치 각도는 잘림, 어깨 각도 1/2 측정

        assertEquals(Grade.PERFECT, result.grade)
        assertFalse(result.recognitionFailed)
    }

    @Test
    fun `손목과 팔꿈치가 화면 밖이면 MISS`() {
        val result = judgeArm(armFrame(90f, out = setOf(Joint.LEFT_WRIST, Joint.LEFT_ELBOW))) // 잰 각도 0개

        assertEquals(Grade.MISS, result.grade)
        assertFalse(result.recognitionFailed)
    }

    @Test
    fun `잘린 각도가 있고 잰 각도가 MIN_MEASURED_RATIO 미만이면 MISS`() {
        val withKnee = ARM.copy(targetAngles = ARM.targetAngles + (Joint.LEFT_KNEE to 170f)) // 무릎은 가려짐
        val result = judgeArm(armFrame(90f, out = setOf(Joint.LEFT_WRIST)), withKnee) // 1/3 < 0.5

        assertEquals(Grade.MISS, result.grade)
    }

    @Test
    fun `잘린 각도는 wrongJoints에 넣지 않는다`() {
        val result = judgeArm(armFrame(110f, out = setOf(Joint.LEFT_WRIST))) // 어깨 오차 20° → 0.556

        assertEquals(Grade.BAD, result.grade)
        assertEquals(listOf(Joint.LEFT_SHOULDER), result.wrongJoints)
    }

    @Test
    fun `모든 관절이 보이면 잰 각도 전부로 판정한다`() {
        val result = judgeArm(armFrame(110f)) // (0.556 + 1) / 2 = 0.778

        assertEquals(Grade.GREAT, result.grade)
        assertEquals(listOf(Joint.LEFT_SHOULDER), result.wrongJoints)
    }

    private companion object {
        val TARGET = TargetPose("test", "테스트", mapOf(Joint.LEFT_ELBOW to 90f), toleranceDeg = 15f)

        /** 팔을 옆으로 곧게 든 자세. */
        val ARM = TargetPose(
            "arm", "팔 들기", mapOf(Joint.LEFT_SHOULDER to 90f, Joint.LEFT_ELBOW to 180f), toleranceDeg = 15f,
        )
    }
}
