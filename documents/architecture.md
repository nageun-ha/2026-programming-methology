## 1. 데이터 저장 방식

성취감을 높여 운동을 지속할 동기를 제공한다.

- **로컬**: 로컬에 이전 내역 등 기록을 저장함. 로그인은 따로 없이 함.
- **구현방법**: Room의 `exercise-records.db`에 운동 1회 요약을 한 행으로 저장한다. 자동 생성 ID를 사용해 같은 날 완료한 운동도 각각 보관한다.
- **조회**: `RecordRepository.recordsBetween(from, to)`는 양 끝 날짜를 포함한다. 날짜는 `LocalDate`의 epoch day로 저장하고 날짜순으로 반환한다.
