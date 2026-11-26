# 포즈 라이브러리 비교와 선정 (#18)

ML Kit Pose Detection과 MediaPipe Pose Landmarker를 같은 조건에서 비교하고 #25에서 쓸 라이브러리를 고른다.

## 결론

**MediaPipe Pose Landmarker (full 모델)** 을 쓴다. 실기기(Galaxy S24 Ultra, SM-S928N, Android 17) 측정에서 아래 기준을 통과해 확정했다(p90 24.5ms).

- 기준(측정 전에 정함): 실기기에서 full 모델 한 프레임 처리 시간 p90 ≤ 66ms (15fps 이상)
- 통과 못 하면: 같은 표에서 기준을 통과하는 것 중 "화면 밖 관절" 항목이 나은 것을 고른다. lite·ML Kit 기본 모델을 쓰게 되면 화면 밖 판정(#29)을 다시 설계한다.

이유는 세 가지다.

1. **화면 밖 관절을 알아본다.** #3 결정은 "화면 밖 = 사용자 원인(Miss), 가려짐 = 소프트웨어 원인(Great)"이라 ①이 둘을 구분해야 한다. 아래 측정에서 MediaPipe full만 화면 밖 손목을 경계 밖 좌표 + 낮은 신뢰도로 알려 줬다. 가벼운 모델(MediaPipe lite, ML Kit 기본)은 화면 밖 손목을 경계 안쪽(x≈0.98)에 높은 신뢰도(0.94~0.95)로 붙여 놓았다.
2. **신뢰도가 두 종류다.** MediaPipe는 `presence`(화면 안에 있을 확률)와 `visibility`(가려지지 않고 보일 확률)를 따로 준다. 화면 밖과 가려짐을 나누는 데 그대로 쓸 수 있다. ML Kit은 `inFrameLikelihood` 하나다.
3. **유지 보수.** ML Kit Pose는 2024-08 `18.0.0-beta5` 이후 새 버전이 없고 아직 beta다. MediaPipe `tasks-vision`은 2026-11 `1.2.3`이 나왔다.

## 비교표

| 항목 | ML Kit 기본 | ML Kit 정확도 | MediaPipe lite | MediaPipe full |
|---|---|---|---|---|
| 의존성 | `pose-detection:18.0.0-beta5` | `pose-detection-accurate:18.0.0-beta5` | `tasks-vision:1.2.3` + 모델 5.8MB | `tasks-vision:1.2.3` + 모델 9.4MB |
| APK 증가 (전체 ABI) | +84.5MB | +88.1MB | +75.9MB | +79.3MB |
| APK 증가 (arm64 폰 1대 몫) | +28.7MB | +32.3MB | +21.9MB | +25.3MB |
| 처리 시간 (에뮬레이터, 참고용) | 12.0~15.3ms | 15.5ms | 10.3~12.6ms | 15.3~16.8ms |
| **처리 시간 (실기기, 평균 / p90)** | 12.3 / 26.0ms | 19.4 / 28.5ms | 30.9 / 41.3ms | 19.6 / **24.5ms** |
| 화면 밖 손목 (실기기, 실제 x=1.28) | x=0.99, 0.96 ❌ | x=1.09, 0.33 | x=0.98, presence 0.94 ❌ | x=1.22, presence 0.08 |
| 관절 수 | 33 | 33 | 33 | 33 |
| 좌표 | 픽셀, 회전 적용 후 기준 | 같음 | 0~1, **회전 전 버퍼** 기준 | 같음 |
| 신뢰도 | `inFrameLikelihood` | 같음 | `visibility`, `presence` | 같음 |
| 화면 밖 손목 (실제 x=1.28) | x=0.99, 0.95 ❌ | x=1.09, 0.34 | x=0.98, 0.94 ❌ | x=1.22, presence 0.08 |
| 여러 사람 | 1명 | 1명 | 여러 명 가능 | 여러 명 가능 |

- 관절 번호는 두 라이브러리 모두 BlazePose 33점 순서가 같다(0=코, 15·16=손목, 27·28=발목). LEFT/RIGHT는 사용자 몸 기준이다.

## 측정 방법

### APK 크기
- master(698268b)에 의존성 한 줄만 더해 `./gradlew assembleRelease`(이 프로젝트 설정대로 R8 꺼짐)로 만든 unsigned APK 크기를 기준 APK(12.16MB)와 비교했다. MB는 10⁶바이트다.
- APK를 풀어 압축 크기 기준으로 `lib/arm64-v8a/` + 모델 자산 + dex 등 ABI와 무관한 파일의 증가분을 "폰 1대 몫"으로 셌다. 지금처럼 APK 하나로 배포하면 x86 등 다른 ABI 몫까지 전부 들어간다. 크기가 문제면 `abiFilters += "arm64-v8a"`로 줄일 수 있다.

### 처리 시간
- 계측 테스트로 같은 이미지(전신 사진을 4:3으로 잘라 640×480, 카메라 분석 해상도와 같음)를 영상 모드(ML Kit `STREAM_MODE`, MediaPipe `VIDEO`)로 20번 예열 후 100번 처리해 평균·p50·p90을 쟀다.
- 에뮬레이터: Pixel 6 AVD, API 35 x86_64, PC CPU(WHPX). **폰 성능과 다르므로 순위를 정하는 데 쓰지 않는다.** 측정 코드가 도는지 확인한 값이다.
- 실기기: Galaxy S24 Ultra(SM-S928N), Android 17, 충전 중, 2026-11-26. 같은 테스트 6개를 한 번에 돌렸다(`Finished 6 tests on SM-S928N - 17`).
- 실기기에서 lite가 full보다 느렸다(p90 41.3 vs 24.5ms). 원인은 확인하지 않았다. 결론은 full이라 영향이 없다.
- 실기기 측정 명령 (측정 코드는 머지하지 않는 `bench/pose-library` 브랜치에 있다):

```
./gradlew connectedDebugAndroidTest
adb logcat -d -s PoseBench
```

### 화면 밖 관절
- 같은 사진의 오른쪽 44%를 잘라 사람의 한쪽 팔꿈치(실제 x≈1.13)와 손목(x≈1.28)을 화면 밖으로 보냈다. 단일 이미지 모드의 새 검출기로 한 번 돌려 좌표와 신뢰도를 봤다.
- 사진 1장, 에뮬레이터 결과라 경향만 본다. 실기기에서 실제로 화면 밖으로 걸어 나가며 다시 확인한다(#29 완료 조건과 함께).

### 좌표 기준 (회전)
- 바로 선 사진을 반시계 90° 눕힌 버퍼에 "시계 방향 90° 돌려야 선다"(CameraX `rotationDegrees` 규칙)를 넘겼다.
- ML Kit은 회전 후(바로 선) 이미지 기준 좌표를 줬다. MediaPipe는 90을 줘야 사람을 찾았고, 좌표는 회전 전 버퍼 기준이었다.
- 그래서 #25의 `FrameMapper`는 MediaPipe 좌표에 회전·거울 반전을 적용한다. ML Kit이면 회전 0으로 넣는다.

## 붙이는 난이도

| | ML Kit | MediaPipe |
|---|---|---|
| CameraX 연결 | `camera-mlkit-vision`의 `MlKitAnalyzer`로 몇 줄 | `ImageProxy.toBitmap()`(RGBA 출력) → `detectAsync` |
| 모델 파일 | 라이브러리에 포함 | `assets/`에 `.task` 파일 추가 |
| 결과 받기 | `Task` 콜백 | `LIVE_STREAM` 모드 결과 리스너 (`callbackFlow`로 감쌈) |
| 주의 | `inFrameLikelihood` 하나뿐 | 프레임 시각이 단조 증가해야 함, 좌표 회전은 직접 |

MediaPipe가 몇 줄 더 들지만 `FrameMapper`(#25)가 회전·반전을 이미 한 곳에서 처리하므로 차이는 작다.
