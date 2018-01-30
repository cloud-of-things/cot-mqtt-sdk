package de.tarent.telekom.cot.mqtt.util;


import javax.annotation.Nonnull;
import java.nio.charset.Charset;
import java.util.Objects;

/**
 * A secret that can be used for encryption.
 *
 * A secret consists of a key with exactly 16 bytes.
 *
 * @see EncryptionHelper
 */
public class Secret {

    /**
     * The (exact) required key length in bytes.
     */
    public static final int KEY_LENGTH_IN_BYTES = 16;

    /**
     * The secret key with a length of 16 bytes.
     */
    @Nonnull
    private final byte[] key;

    /**
     * Convenience method that creates a Secret from the given string.
     *
     * The ASCII byte sequence of the string is used as key. In ASCII,
     * each character consists of a single byte.
     *
     * @param key The key as String.
     */
    @Nonnull
    public Secret(@Nonnull final String key) {
        this(Objects.requireNonNull(key).getBytes(Charset.forName("ASCII")));
    }

    /**
     * @param key The secret key.
     */
    public Secret(@Nonnull final byte[] key) {
        Objects.requireNonNull(key);
        if (key.length != KEY_LENGTH_IN_BYTES) {
            throw new IllegalArgumentException("The key must have a length of exactly " + KEY_LENGTH_IN_BYTES + " bytes.");
        }
        this.key = new byte[KEY_LENGTH_IN_BYTES];
        System.arraycopy(key, 0, this.key, 0, KEY_LENGTH_IN_BYTES);
    }

    /**
     * @return The key of 16 bytes.
     */
    @Nonnull
    public byte[] getKey() {
        return key;
    }
}
