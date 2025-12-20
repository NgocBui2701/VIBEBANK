package com.example.vibebank.utils;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtils {

    // Hàm dùng khi Đăng ký (Tạo Hash từ mật khẩu nhập vào)
    public static String hashPassword(String plainTextPassword) {
        // gensalt() tạo ra một chuỗi ngẫu nhiên để mỗi lần hash đều khác nhau
        // dù 2 người có mật khẩu giống hệt nhau
        return BCrypt.hashpw(plainTextPassword, BCrypt.gensalt());
    }

    // Hàm dùng khi Đăng nhập (Kiểm tra mật khẩu nhập vào có khớp với Hash trong DB không)
    public static boolean checkPassword(String candidatePassword, String storedHash) {
        if (storedHash == null || !storedHash.startsWith("$2a$"))
            return false;

        return BCrypt.checkpw(candidatePassword, storedHash);
    }
}