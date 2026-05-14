package com.infraforge.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM encryption for user API keys stored in the database.
 *
 * Format of stored value: Base64(IV[12 bytes] || ciphertext || GCM tag[16 bytes])
 *
 * The encryption key is loaded from application config (environment variable in prod).
 * It must be exactly 32 characters — validated at startup.
 *
 * Security notes:
 *   - GCM provides both confidentiality and integrity (authenticated encryption)
 *   - A fresh random IV is generated per encryption — never reuse IVs with the same key
 *   - The raw key is never returned to the frontend after saving (masked as ••••••••)
 *   - Changing the encryption key invalidates all stored keys — rotate carefully
 *
 * Phase 6 note: if key rotation is needed, add a key version prefix to the stored value
 * and support decryption with old key + re-encryption with new key on login.
 */
@Service
public class AiKeyEncryptionService {

    private static final Logger log = LoggerFactory.getLogger(AiKeyEncryptionService.class);

    private static final String ALGORITHM  = "AES/GCM/NoPadding";
    private static final int    IV_LENGTH  = 12;  // 96-bit IV recommended for GCM
    private static final int    TAG_LENGTH = 128; // 128-bit authentication tag

    @Value("${infraforge.ai.encryption-key}")
    private String encryptionKeyRaw;

    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        if (encryptionKeyRaw == null || encryptionKeyRaw.length() != 32) {
            throw new IllegalStateException(
                "[AiKeyEncryptionService] infraforge.ai.encryption-key must be exactly 32 characters. " +
                "Current length: " + (encryptionKeyRaw == null ? "null" : encryptionKeyRaw.length())
            );
        }
        secretKey = new SecretKeySpec(encryptionKeyRaw.getBytes(StandardCharsets.UTF_8), "AES");
        log.info("[AiKeyEncryptionService] AES-256-GCM key loaded successfully");
    }

    /**
     * Encrypt an API key for storage.
     * @param plaintext the raw API key
     * @return Base64-encoded IV + ciphertext + GCM tag
     */
    public String encrypt(String plaintext) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH, iv));

            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // Prepend IV to ciphertext before Base64 encoding
            byte[] combined = new byte[IV_LENGTH + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, IV_LENGTH);
            System.arraycopy(ciphertext, 0, combined, IV_LENGTH, ciphertext.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("[AiKeyEncryptionService] Encryption failed", e);
        }
    }

    /**
     * Decrypt a stored encrypted API key.
     * @param encoded Base64-encoded IV + ciphertext + GCM tag
     * @return the original plaintext API key
     * @throws RuntimeException if decryption fails (wrong key, corrupted data)
     */
    public String decrypt(String encoded) {
        try {
            byte[] combined = Base64.getDecoder().decode(encoded);

            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH);

            byte[] ciphertext = new byte[combined.length - IV_LENGTH];
            System.arraycopy(combined, IV_LENGTH, ciphertext, 0, ciphertext.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH, iv));

            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("[AiKeyEncryptionService] Decryption failed — key may have changed", e);
        }
    }
}