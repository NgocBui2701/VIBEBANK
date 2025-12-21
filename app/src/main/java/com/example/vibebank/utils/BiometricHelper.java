package com.example.vibebank.utils;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

public class BiometricHelper {
    private static final String KEY_NAME = "MyAppBiometricKey";
    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";

    // 1. Tạo Key yêu cầu xác thực sinh trắc học
    public void generateSecretKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
        keyStore.load(null);

        // Chỉ tạo mới nếu chưa có
        if (!keyStore.containsAlias(KEY_NAME)) {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE);
            KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(KEY_NAME,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .setUserAuthenticationRequired(true) // BẮT BUỘC: Phải có vân tay mới được dùng key này
                    // .setInvalidatedByBiometricEnrollment(true) // Nếu cài thêm ngón tay mới, key sẽ bị hủy (tùy chọn)
                    ;
            keyGenerator.init(builder.build());
            keyGenerator.generateKey();
        }
    }

    // 2. Lấy Cipher để MÃ HÓA (dùng khi bật Switch)
    public Cipher getCipherForEncryption() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
        keyStore.load(null);
        SecretKey secretKey = (SecretKey) keyStore.getKey(KEY_NAME, null);

        Cipher cipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/"
                + KeyProperties.BLOCK_MODE_CBC + "/"
                + KeyProperties.ENCRYPTION_PADDING_PKCS7);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        return cipher;
    }

    // 3. Lấy Cipher để GIẢI MÃ
    public Cipher getCipherForDecryption(byte[] iv) throws Exception {
        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
        keyStore.load(null);
        SecretKey secretKey = (SecretKey) keyStore.getKey(KEY_NAME, null);

        Cipher cipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/"
                + KeyProperties.BLOCK_MODE_CBC + "/"
                + KeyProperties.ENCRYPTION_PADDING_PKCS7);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));
        return cipher;
    }

    // Hàm xóa Key cũ bị lỗi
    public void deleteKey() {
        try {
            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
            keyStore.load(null);
            keyStore.deleteEntry(KEY_NAME);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}