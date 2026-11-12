package com.example.prm_project.model

/**
 * 판정 등급. Perfect > Great > Bad > Miss 순서다.
 *
 * 성공 판정은 [isSuccess] 한 곳에만 둔다.
 * Bad 성공 여부 미정, 바꾸려면 여기만 수정한다(현재 false).
 */
enum class Grade(val isSuccess: Boolean) {
    PERFECT(true),
    GREAT(true),
    BAD(false),
    MISS(false),
}
