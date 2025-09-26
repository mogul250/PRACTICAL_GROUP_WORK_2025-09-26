package com.ccms;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;

public class PasswordUtil {
    // Reasonable modern defaults for desktop apps
    private static final int ITERATIONS = 120_000; // ~100k+ is recommended
    private static final int KEY_LENGTH = 256;     // bits
    private static final int SALT_LEN = 16;        // bytes

    public static String hash(String rawPassword) {
        Objects.requireNonNull(rawPassword, "password");
        byte[] salt = new byte[SALT_LEN];
        new SecureRandom().nextBytes(salt);
        byte[] dk = pbkdf2(rawPassword.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
        // Store as: PBKDF2$<iter>$<saltB64>$<dkB64>
        return "PBKDF2$" + ITERATIONS + "$" +
               Base64.getEncoder().encodeToString(salt) + "$" +
               Base64.getEncoder().encodeToString(dk);
    }

    public static boolean matches(String rawPassword, String stored) {
        try {
            if (rawPassword == null || stored == null) return false;
            String[] parts = stored.split("\\$");
            if (parts.length != 4 || !parts[0].equals("PBKDF2")) return false;
            int iters = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            byte[] actual = pbkdf2(rawPassword.toCharArray(), salt, iters, expected.length * 8);
            return constantTimeEquals(expected, actual);
        } catch (Exception e) {
            return false;
        }
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLenBits) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLenBits);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return skf.generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new RuntimeException("PBKDF2 failure", e);
        }
    }

    private static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a == null || b == null || a.length != b.length) return false;
        int result = 0;
        for (int i = 0; i < a.length; i++) result |= a[i] ^ b[i];
        return result == 0;
    }
}
