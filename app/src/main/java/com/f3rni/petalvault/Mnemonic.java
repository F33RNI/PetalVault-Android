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

import androidx.annotation.NonNull;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

public class Mnemonic {
    private static final SecureRandom secureRandom = new SecureRandom();
    private static final char[] hexArray = "0123456789ABCDEF".toCharArray();

    private final String[] wordlist;

    private final byte[] entropy = new byte[16];
    private final ArrayList<String> mnemonic = new ArrayList<>();

    /**
     * Initializes Mnemonic class
     *
     * @param wordlist BIP-39 words (lowercase)
     */
    Mnemonic(String[] wordlist) {
        this.wordlist = wordlist;
    }

    /**
     * @return mnemonic phrase as ArrayList of words
     */
    public ArrayList<String> getMnemonic() {
        return mnemonic;
    }

    /**
     * @return mnemonic phrase as array of words
     */
    public String[] getMnemonicArray() {
        return mnemonic.toArray(new String[0]);
    }

    /**
     * @return current mnemonic as string or empty string ("")
     */
    @NonNull
    public String toString() {
        if (mnemonic.isEmpty()) return "";
        return String.join(" ", mnemonic);
    }

    /**
     * @return current entropy as array of 16 bytes
     */
    public byte[] getEntropy() {
        return entropy;
    }

    /**
     * Generates mnemonic phrase from entropy
     *
     * @param entropy 128-bit (16-byte) entropy
     */
    public void fromEntropy(@NonNull byte[] entropy) {
        // Check length
        if (entropy.length != 16) throw new RuntimeException("Entropy must be 16-byte long");

        // Copy entropy
        for (short i = 0; i < entropy.length; i++)
            this.entropy[i] = entropy[i];

        // Get it's hash
        byte[] entropyHash = SHA256(entropy);

        // Copy first 4 bits of the hash as checksum
        boolean[] checksum = Arrays.copyOfRange(bytesToBits(entropyHash), 0, 4);

        // Add checksum to the end of the entropy bits
        boolean[] entropyWithChecksum = Arrays.copyOf(bytesToBits(entropy), bytesToBits(entropy).length + checksum.length);
        System.arraycopy(checksum, 0, entropyWithChecksum, bytesToBits(entropy).length, checksum.length);

        // Clear current mnemonic and add all new words
        mnemonic.clear();

        // Split entropyWithChecksum into groups of 11 bits and fill arrayList with words
        for (short i = 0; i < 12; i++) {
            boolean[] numBits = Arrays.copyOfRange(entropyWithChecksum, i * 11, i * 11 + 11);
            mnemonic.add(wordlist[bitsToInt(numBits)]);
        }
    }

    /**
     * Converts mnemonic phrase to entropy
     *
     * @param mnemonic mnemonic words separated with space
     */
    public void fromMnemonic(@NonNull String mnemonic) {
        // Split into words
        String[] words = mnemonic.toLowerCase(Locale.ROOT).trim().split(" ");
        fromMnemonic(words);
    }

    /**
     * Converts mnemonic phrase to entropy
     *
     * @param mnemonic array of mnemonic words
     */
    public void fromMnemonic(@NonNull String[] mnemonic) {
        // Check length
        if (mnemonic.length != 12) throw new RuntimeException("Mnemonic must be 12-word long");

        // Clear current mnemonic and add new words
        this.mnemonic.clear();
        this.mnemonic.addAll(Arrays.asList(mnemonic));

        // Convert mnemonic words to their respective indexes in the wordlist
        boolean[] bits = new boolean[mnemonic.length * 11];
        for (int i = 0; i < mnemonic.length; i++) {
            int index = Arrays.asList(wordlist).indexOf(mnemonic[i]);
            if (index == -1) throw new IllegalArgumentException("Mnemonic contains invalid word: " + mnemonic[i]);
            boolean[] wordBits = intToBits(index, 11);
            System.arraycopy(wordBits, 0, bits, i * 11, 11);
        }

        // Extract entropy and checksum bits
        boolean[] entropyBits = Arrays.copyOfRange(bits, 0, bits.length - 4);
        boolean[] checksumBits = Arrays.copyOfRange(bits, bits.length - 4, bits.length);

        // Convert entropy bits back to bytes
        for (int i = 0; i < entropy.length; i++) {
            entropy[i] = (byte) bitsToInt(Arrays.copyOfRange(entropyBits, i * 8, (i + 1) * 8));
        }

        // Verify checksum
        byte[] hash = SHA256(entropy);
        boolean[] hashBits = Arrays.copyOfRange(bytesToBits(hash), 0, 4);
        if (!Arrays.equals(checksumBits, hashBits)) throw new IllegalArgumentException("Invalid checksum");
    }

    /**
     * Generates random entropy and mnemonic phrase
     * (wrapper for fromEntropy(generateEntropy()))
     */
    public void generateRandom() {
        fromEntropy(generateEntropy());
    }

    /**
     * Converts array of bytes into HEX string
     *
     * @param bytes array of bytes
     * @return hex string from byte array
     */
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    /**
     * @return secure randomly generated 16-byte array
     */
    private static byte[] generateEntropy() {
        byte[] ent = new byte[16];
        secureRandom.nextBytes(ent);
        return ent;
    }

    /**
     * Converts bytes array into boolean array (array of bits)
     *
     * @param data array of bytes
     * @return bit representation of byte array
     */
    private static boolean[] bytesToBits(byte[] data) {
        boolean[] bits = new boolean[data.length * 8];
        for (int i = 0; i < data.length; ++i)
            for (int j = 0; j < 8; ++j)
                bits[(i * 8) + j] = (data[i] & (1 << (7 - j))) != 0;
        return bits;
    }

    /**
     * @param data array of bytes
     * @return SHA256 hash of input data
     */
    private static byte[] SHA256(byte[] data) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Converts boolean array to integer
     *
     * @param bits array of bits
     * @return int value of a bit array
     */
    private static int bitsToInt(boolean[] bits) {
        int n = 0;
        for (boolean bit : bits)
            n = (n << 1) + (bit ? 1 : 0);
        return n;
    }

    /**
     * Converts an integer to a boolean array (bits) with the specified length
     *
     * @param value  integer value to convert
     * @param length length of the resulting bit array
     * @return boolean array representing the bits
     */
    private static boolean[] intToBits(int value, int length) {
        boolean[] bits = new boolean[length];
        for (int i = length - 1; i >= 0; i--) {
            bits[i] = (value & 1) == 1;
            value >>= 1;
        }
        return bits;
    }
}
