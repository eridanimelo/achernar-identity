package com.eridani.achernar.identity.shared.crypto;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Cifra AES-256-GCM com a chave-mestra (SEC-24, D-25). Formato: {@code iv(12) || ciphertext||tag}.
 * O parâmetro {@code purpose} entra como AAD, impedindo reutilizar um blob em outro contexto.
 */
public final class MasterKeyCipher {

    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final String DEV_INSECURE_SEED = "achernar-identity-dev-insecure-master-key";

    private final SecretKey key;
    private final SecureRandom random = new SecureRandom();

    private MasterKeyCipher(byte[] keyMaterial) {
        this.key = new SecretKeySpec(Sha256.digest(keyMaterial), "AES");
    }

    /** @param masterKeyBase64 valor de {@code ACHERNAR_MASTER_KEY} (>= 32 bytes em base64) */
    public static MasterKeyCipher fromBase64(String masterKeyBase64) {
        byte[] raw = Base64.getDecoder().decode(masterKeyBase64.trim());
        if (raw.length < 32) {
            throw new IllegalArgumentException("ACHERNAR_MASTER_KEY deve ter ao menos 32 bytes");
        }
        return new MasterKeyCipher(raw);
    }

    /** Somente desenvolvimento (PV-05): exige o opt-in explícito {@code ACHERNAR_DEV_INSECURE_KEY=true}. */
    public static MasterKeyCipher devInsecure() {
        return new MasterKeyCipher(DEV_INSECURE_SEED.getBytes(StandardCharsets.UTF_8));
    }

    public byte[] encrypt(byte[] plain, String purpose) {
        try {
            byte[] iv = new byte[IV_BYTES];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            cipher.updateAAD(purpose.getBytes(StandardCharsets.UTF_8));
            byte[] enc = cipher.doFinal(plain);
            return ByteBuffer.allocate(iv.length + enc.length).put(iv).put(enc).array();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Falha ao cifrar", e);
        }
    }

    public byte[] decrypt(byte[] blob, String purpose) {
        try {
            if (blob == null || blob.length <= IV_BYTES) {
                throw new IllegalArgumentException("Blob cifrado inválido");
            }
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, blob, 0, IV_BYTES));
            cipher.updateAAD(purpose.getBytes(StandardCharsets.UTF_8));
            return cipher.doFinal(blob, IV_BYTES, blob.length - IV_BYTES);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Falha ao decifrar (chave-mestra incorreta ou dado adulterado)", e);
        }
    }

    public byte[] encryptString(String plain, String purpose) {
        return encrypt(plain.getBytes(StandardCharsets.UTF_8), purpose);
    }

    public String decryptString(byte[] blob, String purpose) {
        return new String(decrypt(blob, purpose), StandardCharsets.UTF_8);
    }
}
