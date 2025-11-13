package com.team11.smartgym.data;

import android.util.Base64;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Utility for securely hashing and verifying passwords using PBKDF2.
 *
 * - Uses a per-password random salt
 * - Stores (hash, salt, iterations) as Base64-encoded strings
 * - No plaintext password is ever persisted
 */
public final class PasswordHasher {

    // You can tweak these if needed (higher iterations = slower but more secure)
    private static final int DEFAULT_ITERATIONS = 65536;
    private static final int KEY_LENGTH_BITS = 256; // 256-bit hash
    private static final String PBKDF_ALGORITHM = "PBKDF2WithHmacSHA256";

    private PasswordHasher() {
        // no instances
    }

    /**
     * Result object holding hash, salt, and iterations.
     */
    public static final class HashedPassword {
        public final String hashBase64;
        public final String saltBase64;
        public final int iterations;

        public HashedPassword(String hashBase64, String saltBase64, int iterations) {
            this.hashBase64 = hashBase64;
            this.saltBase64 = saltBase64;
            this.iterations = iterations;
        }
    }

    /**
     * Hash a plaintext password with a fresh random salt.
     */
    public static HashedPassword hashPassword(String plainPassword) {
        if (plainPassword == null) {
            throw new IllegalArgumentException("Password cannot be null");
        }

        byte[] salt = generateRandomSalt(16); // 128-bit salt
        byte[] hash = pbkdf2(plainPassword.toCharArray(), salt, DEFAULT_ITERATIONS, KEY_LENGTH_BITS);

        String hashB64 = Base64.encodeToString(hash, Base64.NO_WRAP);
        String saltB64 = Base64.encodeToString(salt, Base64.NO_WRAP);
        return new HashedPassword(hashB64, saltB64, DEFAULT_ITERATIONS);
    }

    /**
     * Verify a plaintext password against stored hash+salt+iterations.
     */
    public static boolean verifyPassword(String plainPassword,
                                         String storedHashBase64,
                                         String storedSaltBase64,
                                         int storedIterations) {
        if (plainPassword == null || storedHashBase64 == null || storedSaltBase64 == null) {
            return false;
        }

        byte[] salt;
        byte[] expectedHash;

        try {
            salt = Base64.decode(storedSaltBase64, Base64.NO_WRAP);
            expectedHash = Base64.decode(storedHashBase64, Base64.NO_WRAP);
        } catch (IllegalArgumentException e) {
            // Corrupted Base64
            return false;
        }

        byte[] computedHash = pbkdf2(
                plainPassword.toCharArray(),
                salt,
                storedIterations > 0 ? storedIterations : DEFAULT_ITERATIONS,
                expectedHash.length * 8
        );

        return slowEquals(expectedHash, computedHash);
    }

    // ===== Internal helpers =====

    private static byte[] generateRandomSalt(int lengthBytes) {
        byte[] salt = new byte[lengthBytes];
        SecureRandom sr = new SecureRandom();
        sr.nextBytes(salt);
        return salt;
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLengthBits) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLengthBits);
            SecretKeyFactory skf = SecretKeyFactory.getInstance(PBKDF_ALGORITHM);
            return skf.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Error while hashing password", e);
        }
    }

    /**
     * Time-constant comparison to avoid timing attacks.
     */
    private static boolean slowEquals(byte[] a, byte[] b) {
        if (a == null || b == null) return false;
        if (a.length != b.length) return false;

        int diff = 0;
        for (int i = 0; i < a.length; i++) {
            diff |= a[i] ^ b[i];
        }
        return diff == 0;
    }
}
