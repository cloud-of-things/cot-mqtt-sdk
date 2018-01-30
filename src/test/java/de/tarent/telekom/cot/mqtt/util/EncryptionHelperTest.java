package de.tarent.telekom.cot.mqtt.util;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import javax.annotation.Nonnull;
import java.util.Arrays;

public class EncryptionHelperTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    /**
     * System under test.
     */
    private EncryptionHelper helper;

    @Before
    public void setup() {
        helper = new EncryptionHelper();
    }

    @Test
    public void testPadderWithNonMultipleOfEight() {

        // This test verifies the padder() method. The method should take a byte array
        // of any length, and then convert it to another byte array of length that is a
        // multiple of eight. It does an "up" rounding. It fills the new slots in the
        // array with bytes obeying the PKCS#7 padding.

        // Let's take a byte array of length 20 and fill it with bytes of 1:

        byte[] initialArray = new byte[20];
        Arrays.fill(initialArray, (byte) 1);

        // Now let's feed this to the padder(). The resulting array should have a length
        // of 24 as this is the nearest upper multiple of eight. The last four bytes in
        // the array should contain bytes of 4.

        byte[] resultingArray = helper.pad(initialArray);

        Assert.assertEquals(resultingArray.length, 24);

        // The original 20 bytes should be untouched, therefore the first 20 entries in
        // the array should still be filled with bytes of 1:
        for (int i = 0; i < 20; i++) {
            Assert.assertEquals(resultingArray[i], (byte) (1));

        }

        // And the four new additional entries in the array should be filled with the
        // bytes of 4:
        for (int i = 20; i < 24; i++) {
            Assert.assertEquals(resultingArray[i], (byte) (4));

        }
    }


    @Test
    public void testPadderWithMultipleOfEight() {

        // If the input byte array is already a multiple of eight, then the padding adds
        // 8 additional bytes at the end of the array. Let's test that feature:

        byte[] arrayMultipleOfEight = new byte[72];
        Arrays.fill(arrayMultipleOfEight, (byte) 1);

        byte[] newResultingArray = helper.pad(arrayMultipleOfEight);

        // The resulting array should have a length of 80 since the padding adds 8 more
        // bytes if the initial array is already a multiple of eight.
        Assert.assertEquals(newResultingArray.length, 80);

        // The original 72 bytes should be untouched, therefore the first 72 entries in
        // the array should still be filled with bytes of 1:
        for (int i = 0; i < 72; i++) {
            Assert.assertEquals(newResultingArray[i], (byte) (1));

        }

        // And the eight new additional entries in the array should be filled with the
        // bytes of 8:
        for (int i = 72; i < 80; i++) {
            Assert.assertEquals(newResultingArray[i], (byte) (8));

        }

    }


    @Test
    public void testEncryptDecrypt() {

        // Let's create a key and a data to be encrypted:

        byte[] key = new byte[16];
        Arrays.fill(key, (byte) 2);

        String data = "Some text with strange characters such as ğ, ç, ş, ö, and numbers such as 2,3,4.";

        //Now let's convert it to a byte array:
        
        byte[] theByteArray = data.getBytes();
        
        // Now let's encrypt it using the key:

        EncryptionHelper helper = new EncryptionHelper();

        byte[] encryptedData = helper.encrypt(new Secret(key), theByteArray);

        // The encrypted data must be different than the original data:

        Assert.assertNotEquals(theByteArray, encryptedData);

        // The encrypted data has to be every time longer than the original data since
        // the padding will always append additional bytes:

        Assert.assertTrue(encryptedData.length > theByteArray.length);

        // However the difference in length cannot be larger than 8 as this is the
        // maximum number of bytes that the padding adds:

        Assert.assertTrue((encryptedData.length - theByteArray.length) <= 8);

        // Now let's decrypt the data:

        byte[] decryptedData = helper.decrypt(new Secret(key), encryptedData);

        // As expected, the decrypted data has to be identical to the original data:

        Assert.assertArrayEquals(decryptedData, theByteArray);
        
        //And let's convert the decrypted data to string and compare again, just to be sure:
        String convertedDecryptedData=new String(decryptedData);
        
        Assert.assertEquals(convertedDecryptedData,data);
        
    }

    @Test
    public void testDecryptThrowsExceptionIfEncryptedDataIsNotPadded() {
        final byte[] data = new byte[]{1, 2, 3, 4, 5};

        exception.expect(IllegalArgumentException.class);
        helper.decrypt(createSecret(), data);
    }

    @Test
    public void testDecryptThrowsExceptionIfEncryptedDataContainsTooLowPaddingByteValue() {
        // This has been encrypted with an invalid padding value of 0.
        final byte[] encryptedData = new byte[]{-53, -56, -27, 101, -58, 84, 69, 50};

        exception.expect(IllegalArgumentException.class);
        helper.decrypt(createSecret(), encryptedData);
    }

    @Test
    public void testDecryptThrowsExceptionIfEncryptedDataContainsTooHighPaddingByteValue() {
        // This has been encrypted with an invalid padding value of 9.
        final byte[] encryptedData = new byte[]{-83, 25, 29, -22, 36, 24, 2, -82};

        exception.expect(IllegalArgumentException.class);
        helper.decrypt(createSecret(), encryptedData);
    }

    @Test
    public void testDecryptThrowsExceptionIfEncryptedDataContainsInconsistentPaddingSequence() {
        // This has been encrypted with an invalid padding sequence of 1, 2, 3.
        byte[] encryptedData = new byte[]{-72, -76, 16, 55, -95, 116, -106, 101};

        exception.expect(IllegalArgumentException.class);
        helper.decrypt(createSecret(), encryptedData);
    }

    @Test
    public void testDecryptThrowsExceptionIfEncryptedDataIsEmpty() {
        exception.expect(IllegalArgumentException.class);
        helper.decrypt(createSecret(), new byte[]{});
    }

    @Test
    public void testPadding() {

        // Below test verifies all possible outcomes of padding:

        EncryptionHelper helper = new EncryptionHelper();

        Assert.assertArrayEquals(new byte[] { 8, 8, 8, 8, 8, 8, 8, 8 }, helper.pad(new byte[0]));

        Assert.assertArrayEquals(new byte[] { 10, 7, 7, 7, 7, 7, 7, 7 }, helper.pad(new byte[] { 10 }));

        Assert.assertArrayEquals(new byte[] { 10, 11, 6, 6, 6, 6, 6, 6 }, helper.pad(new byte[] { 10, 11 }));

        Assert.assertArrayEquals(new byte[] { 10, 11, 12, 5, 5, 5, 5, 5 }, helper.pad(new byte[] { 10, 11, 12 }));

        Assert.assertArrayEquals(new byte[] { 10, 11, 12, 13, 4, 4, 4, 4 }, helper.pad(new byte[] { 10, 11, 12, 13 }));

        Assert.assertArrayEquals(new byte[] { 10, 11, 12, 13, 14, 3, 3, 3 }, helper.pad(new byte[] { 10, 11, 12, 13, 14 }));

        Assert.assertArrayEquals(new byte[] { 10, 11, 12, 13, 14, 15, 2, 2 }, helper.pad(new byte[] { 10, 11, 12, 13, 14, 15 }));

        Assert.assertArrayEquals(new byte[] { 10, 11, 12, 13, 14, 15, 16, 1 }, helper.pad(new byte[] { 10, 11, 12, 13, 14, 15, 16 }));

        Assert.assertArrayEquals(new byte[] { 10, 11, 12, 13, 14, 15, 16, 17, 8, 8, 8, 8, 8, 8, 8, 8 }, helper.pad(new byte[] { 10, 11, 12, 13, 14, 15, 16, 17 }));

        Assert.assertArrayEquals(new byte[] { 10, 11, 12, 13, 14, 15, 16, 17, 18, 7, 7, 7, 7, 7, 7, 7 }, helper.pad(new byte[] { 10, 11, 12, 13, 14, 15, 16, 17, 18 }));
    
    }

    /**
     * @return A secret with static key.
     */
    @Nonnull
    private Secret createSecret() {
        byte[] key = new byte[Secret.KEY_LENGTH_IN_BYTES];
        Arrays.fill(key, (byte) 2);
        return new Secret(key);
    }
}
