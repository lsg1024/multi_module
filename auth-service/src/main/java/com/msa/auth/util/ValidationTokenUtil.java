package com.msa.auth.util;

public class ValidationTokenUtil {

    public static void validateOwner(String owner) {
        if (owner == null) {
            throw new RuntimeException("접속 실패");
        }
    }

    public static void validateNickname(String nickname) {
        if (nickname == null) {
            throw new RuntimeException("접속 실패");
        }
    }

    public static void validateCategoryIsRefresh(String category) {
        if (!category.equals("refresh")) {
            throw new RuntimeException("유지시간 만료");
        }
    }

}
