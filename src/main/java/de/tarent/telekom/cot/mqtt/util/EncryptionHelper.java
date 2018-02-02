package de.tarent.telekom.cot.mqtt.util;

import org.bouncycastle.crypto.engines.XTEAEngine;
import org.bouncycastle.crypto.params.KeyParameter;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * The class that prepares the input key and the data to be encrypted to be
 * processed by the XTEA (Extended Tiny Encryption Algorithm) provided by the
 * Bouncy Castle library. Does the required type conversions. Performs the
 * padding of the data then proceeds to the encryption/decryption.
 * We use ECB mode (i.e. no block chaining), because the text we need to
 * encrypt is so short, that it will not have duplicate blocks anyway.
 */
public class EncryptionHelper {

    private static final int BLOCK_SIZE = 8;


    /**
     * The method that formats the data for the encryption then calls the XTEA in
     * order to perform the encryption.
     *
     * @param secret that is used for the encryption, can only be 16 byte long.
     * @param data   to be encrypted, the length is arbitrary.
     * @return the data after it is encrypted, in byte array type.
     * @throws IllegalArgumentException
     */
    @Nonnull
    public byte[] encrypt(@Nonnull final Secret secret, @Nonnull final byte[] data) {
        final XTEAEngine engine = new XTEAEngine();
        engine.init(true, new KeyParameter(secret.getKey()));

        byte[] paddedData = pad(data);

        for (int i = 0; i < paddedData.length; i += BLOCK_SIZE) {
            engine.processBlock(paddedData, i, paddedData, i);
        }

        return paddedData;
    }


    /**
     * The method that receives the encrypted data in an byte array format. Decrypts
     * the data. Removes the additional bytes due to padding and returns the
     * decrypted data.
     *
     * @param secret that is used to decrypt, should be identical to the key that is
     *               used for the encryption.
     * @param data   to be decrypted.
     * @return data that is decrypted, in bytes.
     * @throws IllegalArgumentException If syntactically invalid encrypted data is passed.
     */
    @Nonnull
    public byte[] decrypt(@Nonnull final Secret secret, @Nonnull final byte[] data) {
        Objects.requireNonNull(secret);
        Objects.requireNonNull(data);
        assertValidBlockSize(data);

        final XTEAEngine engine = new XTEAEngine();
        engine.init(false, new KeyParameter(secret.getKey()));

        for (int i = 0; i < data.length; i += BLOCK_SIZE) {
            engine.processBlock(data, i, data, i);
        }
        assertValidPadding(data);

        final int paddingLength = getPaddingLength(data);
        final byte[] shortenedArray = new byte[data.length - paddingLength];

        System.arraycopy(data, 0, shortenedArray, 0, data.length - paddingLength);

        return shortenedArray;
    }

    /**
     * Prepares the data for the encryption using the PKCS7 padding method
     * (https://en.wikipedia.org/wiki/Padding_(cryptography)#PKCS7)
     *
     * @param data, byte array that is to be padded.
     * @return padded data, byte array
     */
    public byte[] pad(@Nonnull final byte[] data) {
        final int multiple = 8;
        final int initialLength = data.length;

        final int paddingByte = multiple - (initialLength % multiple);

        final byte[] paddedData = new byte[initialLength + paddingByte];

        System.arraycopy(data, 0, paddedData, 0, initialLength);

        for (int i = 0; i < paddingByte; i++) {
            paddedData[initialLength + i] = (byte) paddingByte;
        }

        return paddedData;
    }

    /**
     * Asserts that the given data has a valid block size.
     * <p>
     * Size *must* be:
     * <p>
     * - a multiple of 8
     * - at least 8
     *
     * @param encryptedData The encrypted data.
     */
    private void assertValidBlockSize(@Nonnull final byte[] encryptedData) {
        if (encryptedData.length < BLOCK_SIZE) {
            throw new IllegalArgumentException(
                "Encrypted data must consist of at least one block of size " + BLOCK_SIZE + ".");
        }
        if (encryptedData.length % BLOCK_SIZE != 0) {
            throw new IllegalArgumentException(
                "Encrypted data is not padded, byte length must be a multiple of " + BLOCK_SIZE + ". Actual length is "
                    + encryptedData.length + ".");
        }
    }

    /**
     * Asserts that the given byte sequence has a valid padding according to the PKCS7.
     * <p>
     * - A padding *must* exist
     * - Padding bytes are in range [1..8]
     * - The value of the padding bytes equals the number of padded bytes
     * - All padding bytes must have the same value
     *
     * @param decryptedData The padded data.
     */
    private void assertValidPadding(@Nonnull final byte[] decryptedData) {
        final int paddingLength = getPaddingLength(decryptedData);
        if (paddingLength < 1 || paddingLength > 8) {
            throw new IllegalArgumentException(
                "Communicated padding length must be in range [1..8]. Actual value is " + paddingLength + ".");
        }

        for (int i = 0; i < paddingLength; i++) {
            if (decryptedData[decryptedData.length - i - 1] == paddingLength) {
                // Everything ok, the padding byte has the expected value.
                continue;
            }

            final String message = "Expected exactly " + paddingLength + " padding bytes with value " + paddingLength
                + ", but got padding sequence [";
            final StringBuilder errorMessage = new StringBuilder(message);
            for (int j = 0; j < paddingLength; j++) {
                errorMessage.append((int) decryptedData[decryptedData.length - paddingLength + j]).append("\n");
            }

            final String trimmedMessage = errorMessage.toString().trim() + "].";
            throw new IllegalArgumentException(trimmedMessage);
        }
    }

    /**
     * With PKCS7-padding, the number of padding bytes equals the value of the padding bytes.
     *
     * @param decryptedData The padded data.
     * @return The padding length in range [1..8].
     */
    private int getPaddingLength(final byte[] decryptedData) {
        return (int) decryptedData[decryptedData.length - 1];
    }
}
