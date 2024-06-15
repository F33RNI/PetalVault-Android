/**
 * This file is part of the PetalVault-Android password manager distribution.
 * See <https://github.com/F33RNI/PetalVault-Android>.
 * Copyright (C) 2024 Fern Lane
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, version 3.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.f3rni.petalvault;

import android.util.Base64;
import android.util.Log;

import org.json.JSONObject;
import org.spongycastle.crypto.generators.SCrypt;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class CryptoUtils {
    private static final String TAG = CryptoUtils.class.getName();

    private static final String AES_MODE = "AES/CBC/PKCS7Padding";
    private static final String PASSWORD_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()-_=+<>?";
    private static final int PASSWORD_LENGTH = 24;

    private static final SecureRandom secureRandom = new SecureRandom();
    private static final int MASTER_KEY_COST = 65536;

    /**
     * Decrypts and decompresses dictionary data.
     *
     * @param encrypted dictionary containing "enc" and "iv" keys
     * @param masterKey derived key (32 bytes) for v>=2.0.0
     * @return decrypted dictionary or null in case of error
     */
    public static JSONObject decryptEntry(JSONObject encrypted, byte[] masterKey) {
        try {
            // Decrypt
            byte[] ivBytes = base64Decode(encrypted.getString("iv"));
            byte[] entryEncrypted = base64Decode(encrypted.getString("enc"));
            Cipher cipher = Cipher.getInstance(AES_MODE);
            SecretKey secretKey = new SecretKeySpec(masterKey, "AES");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(ivBytes));
            byte[] entryDecrypted = cipher.doFinal(entryEncrypted);

            // Decompress
            byte[] entryUncompressed = decompress(entryDecrypted);

            // Split checksum
            byte[] entryBytes = Arrays.copyOfRange(entryUncompressed, 0, entryUncompressed.length - 16);
            byte[] entryChecksumOriginal = Arrays.copyOfRange(entryUncompressed, entryUncompressed.length - 16, entryUncompressed.length);

            // Verify checksum
            byte[] entryChecksum = md5(entryBytes);
            if (!Arrays.equals(entryChecksumOriginal, entryChecksum)) {
                throw new Exception("Checksum verification error");
            }

            // Convert to dictionary
            JSONObject entryDict = new JSONObject("{" + new String(entryBytes, StandardCharsets.UTF_8) + "}");

            // Check for ID key
            if (!entryDict.has("id")) throw new Exception("No 'id' key");
            return entryDict;
        } catch (Exception e) {
            Log.e(TAG, "Error decrypting entry", e);
        }
        return null;
    }

    /**
     * Compresses and encrypts dictionary data.
     *
     * @param decrypted decrypted dictionary. Must contain "id" key
     * @param masterKey derived key (32 bytes) for v>=2.0.0
     * @return encrypted dictionary (with "enc" and "iv" keys) or null in case of error
     */
    public static JSONObject encryptEntry(JSONObject decrypted, byte[] masterKey) {
        try {
            // Convert to bytes and calculate checksum
            String entryStr = decrypted.toString().replace(" ", "").replace("\\/", "/");
            entryStr = entryStr.substring(1, entryStr.length() - 1);
            byte[] entryBytes = entryStr.getBytes(StandardCharsets.UTF_8);
            byte[] entryChecksum = md5(entryBytes);
            byte[] entryWithChecksum = concatenate(entryBytes, entryChecksum);

            // Compress and pad data
            byte[] entryCompressed = compress(entryWithChecksum);

            // Encrypt
            byte[] ivBytes = new byte[16];
            secureRandom.nextBytes(ivBytes);
            Cipher cipher = Cipher.getInstance(AES_MODE);
            SecretKey secretKey = new SecretKeySpec(masterKey, "AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(ivBytes));
            byte[] entryEncrypted = cipher.doFinal(entryCompressed);

            // Convert to base64
            String enc = base64Encode(entryEncrypted);
            String iv = base64Encode(ivBytes);

            JSONObject result = new JSONObject();
            result.put("enc", enc);
            result.put("iv", iv);

            return result;
        } catch (Exception e) {
            Log.e(TAG, "Error encrypting entry", e);
        }
        return null;
    }

    /**
     * Encrypts mnemonic with master password.
     *
     * @param mnemonic       mnemonic phrase to encrypt as list of words
     * @param masterPassword strong master password
     * @return tuple (padded and encrypted mnemonic with checksum, 32B salt of scrypt, 16B IV of AES)
     */
    public static MnemonicEncrypted encryptMnemonic(String[] mnemonic, String masterPassword) {
        try {
            // Derive key from master password
            byte[] masterSalt1 = new byte[32];
            secureRandom.nextBytes(masterSalt1);
            byte[] derivedKey = SCrypt.generate(masterPassword.getBytes(StandardCharsets.UTF_8), masterSalt1, MASTER_KEY_COST, 8, 1, 32);

            // Convert mnemonic to str->bytes, add checksum and pad
            String mnemonicStr = String.join(" ", mnemonic);
            byte[] mnemonicBytes = mnemonicStr.getBytes(StandardCharsets.UTF_8);
            byte[] checksum = md5(mnemonicBytes);
            byte[] mnemonicWithChecksum = concatenate(mnemonicBytes, checksum);

            // Encrypt
            byte[] masterSalt2 = new byte[16];
            secureRandom.nextBytes(masterSalt2);
            Cipher cipher = Cipher.getInstance(AES_MODE);
            SecretKey secretKey = new SecretKeySpec(derivedKey, "AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(masterSalt2));
            byte[] mnemonicEncrypted = cipher.doFinal(mnemonicWithChecksum);

            return new MnemonicEncrypted(mnemonicEncrypted, masterSalt1, masterSalt2);
        } catch (Exception e) {
            Log.e(TAG, "Error encrypting mnemonic", e);
        }
        return null;
    }

    /**
     * Decrypts mnemonic with master password.
     *
     * @param mnemonicEncrypted padded and encrypted mnemonic with checksum, salt 1, salt 2
     * @param masterPassword    strong master password
     * @return mnemonic phrase as array of words
     * @throws Exception decrypt / check error
     */
    public static String[] decryptMnemonic(MnemonicEncrypted mnemonicEncrypted, String masterPassword) throws Exception {
        // Derive key from master password
        byte[] derivedKey = SCrypt.generate(masterPassword.getBytes(StandardCharsets.UTF_8), mnemonicEncrypted.salt1, MASTER_KEY_COST, 8, 1, 32);

        // Decrypt
        Cipher cipher = Cipher.getInstance(AES_MODE);
        SecretKey secretKey = new SecretKeySpec(derivedKey, "AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(mnemonicEncrypted.salt2));
        byte[] mnemonicDecrypted = cipher.doFinal(mnemonicEncrypted.mnemonicEncrypted);

        // Extract checksum
        byte[] mnemonicBytes = Arrays.copyOfRange(mnemonicDecrypted, 0, mnemonicDecrypted.length - 16);
        byte[] mnemonicChecksum = Arrays.copyOfRange(mnemonicDecrypted, mnemonicDecrypted.length - 16, mnemonicDecrypted.length);

        // Check
        byte[] checksumNew = md5(mnemonicBytes);
        if (!Arrays.equals(checksumNew, mnemonicChecksum)) {
            throw new Exception("Checksums are not equal! Wrong password?");
        }

        // Convert to list of strings
        String mnemonicStr = new String(mnemonicBytes, StandardCharsets.UTF_8);
        return mnemonicStr.split(" ");
    }

    /**
     * Derives master key from entropy.
     *
     * @param entropy    128-bit entropy from mnemonic
     * @param masterSalt existing salt (32-bytes) or null to generate a new one
     * @return tuple (32-bytes master key, 32-bytes salt)
     */
    public static MasterKey entropyToMasterKey(byte[] entropy, byte[] masterSalt) {
        if (masterSalt == null) {
            masterSalt = new byte[32];
            secureRandom.nextBytes(masterSalt);
        }
        byte[] masterKey = SCrypt.generate(entropy, masterSalt, MASTER_KEY_COST, 8, 1, 32);
        return new MasterKey(masterKey, masterSalt);
    }

    /**
     * Securely generates random password
     *
     * @return secure password of length PASSWORD_LENGTH
     */
    public static String generateSecurePassword() {
        StringBuilder password = new StringBuilder(PASSWORD_LENGTH);

        for (int i = 0; i < PASSWORD_LENGTH; i++) {
            int randomIndex = secureRandom.nextInt(PASSWORD_ALPHABET.length());
            password.append(PASSWORD_ALPHABET.charAt(randomIndex));
        }

        return password.toString();
    }


    private static byte[] md5(byte[] data) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        return md.digest(data);
    }

    private static byte[] compress(byte[] data) throws IOException {
        Deflater deflater = new Deflater();
        deflater.setInput(data);
        deflater.finish();
        byte[] buffer = new byte[1024];
        int length;
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            while (!deflater.finished()) {
                length = deflater.deflate(buffer);
                byteArrayOutputStream.write(buffer, 0, length);
            }
            return byteArrayOutputStream.toByteArray();
        }
    }

    private static byte[] decompress(byte[] data) throws IOException, DataFormatException {
        Inflater inflater = new Inflater();
        inflater.setInput(data);
        byte[] buffer = new byte[1024];
        int length;
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            while (!inflater.finished()) {
                length = inflater.inflate(buffer);
                byteArrayOutputStream.write(buffer, 0, length);
            }
            return byteArrayOutputStream.toByteArray();
        }
    }

    private static byte[] concatenate(byte[] a, byte[] b) {
        byte[] result = new byte[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }

    public static String base64Encode(byte[] data) {
        return Base64.encodeToString(data, Base64.NO_WRAP);
    }

    public static byte[] base64Decode(String data) {
        return Base64.decode(data, Base64.NO_WRAP);
    }

    /**
     * Securely generates random array of bytes
     *
     * @param size number of bytes to generate
     * @return array with random bytes
     */
    public static byte[] generateRandom(int size) {
        byte[] randomBytes = new byte[size];
        secureRandom.nextBytes(randomBytes);
        return randomBytes;
    }

    // Utility class for storing encrypted mnemonic with salts
    public static class MnemonicEncrypted {
        public final byte[] mnemonicEncrypted, salt1, salt2;

        MnemonicEncrypted(byte[] mnemonicEncrypted, byte[] salt1, byte[] salt2) {
            this.mnemonicEncrypted = mnemonicEncrypted;
            this.salt1 = salt1;
            this.salt2 = salt2;
        }
    }

    // Utility class for storing derived master key and it's salt
    public static class MasterKey {
        public final byte[] masterKey, salt;

        public MasterKey(byte[] masterKey, byte[] salt) {
            this.masterKey = masterKey;
            this.salt = salt;
        }
    }
}
