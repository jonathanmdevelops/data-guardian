package jpm.dg;

import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * A class providing encryption and decryption services.
 * 
 */
public class Crypto {

	// Ordered hex alphabet.
	private final static String HEX = "0123456789ABCDEF";

	/**
	 * Password encrypts text using AES.
	 * 
	 * @param pass
	 *            The user-provided password.
	 * @param clearText
	 *            The text to encrypt.
	 * @return A hexadecimal representation of the encrypted text.
	 * @throws Exception
	 */
	public static String encrypt(String pass, String clearText)
			throws Exception {
		byte[] rawKey = getRawKey(pass.getBytes("ISO-8859-1"));
		Cipher cipher = Cipher.getInstance("AES");
		cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(rawKey, "AES"));
		byte[] result = cipher.doFinal(clearText.getBytes("ISO-8859-1"));
		StringBuffer buffer = new StringBuffer(2 * result.length);
		for (int i = 0; i < result.length; i++) {
			buffer.append(HEX.charAt((result[i] >> 4) & 0x0f)).append(
					HEX.charAt(result[i] & 0x0f));
		}
		return buffer.toString();
	}

	/**
	 * Decrypts text.
	 * 
	 * @param pass  User-provided password.
	 * @param cipherText
	 * @return The correct cleartext iff the password is correct.
	 * @throws Exception
	 */
	public static String decrypt(String pass, String cipherText)
			throws Exception {
		byte[] rawKey = getRawKey(pass.getBytes("ISO-8859-1"));
		byte[] encrypted = toByte(cipherText);
		Cipher cipher = Cipher.getInstance("AES");
		cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(rawKey, "AES"));
		return new String(cipher.doFinal(encrypted));
	}

	/**
	 * Generates a key
	 * 
	 * @param seed
	 * @return The relevant 128-bit AES key.
	 * @throws Exception
	 */
	private static byte[] getRawKey(byte[] seed) throws Exception {
		KeyGenerator kgen = KeyGenerator.getInstance("AES");
		SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
		sr.setSeed(seed);
		kgen.init(128, sr);
		SecretKey skey = kgen.generateKey();
		byte[] raw = skey.getEncoded();
		return raw;
	}

	/**
	 * Calculates whether a message can be encrypted without exceeding an images
	 * capacity.
	 * 
	 * @param message
	 *            The message to encrypt.
	 * @param numPixels
	 *            The number of pixels of the cover image.
	 * @return True if and only if the encrypted message size is below image
	 *         capacity.
	 */
	public static boolean isBelowCapacity(String message, int numPixels) {
		byte[] clear = message.getBytes();
		// Adjust for delimiters.
		long clearLen = clear.length + 6;
		long encryptedLength = (clearLen / 16 + 1) * 16;
		return (encryptedLength <= ((numPixels * 3) / 4));
	}

	/**
	 * Converts a String to bytes.
	 * 
	 * @param hexString
	 *            A hex String.
	 * @return The hex String as a byte array.
	 */
	public static byte[] toByte(String hexString) {
		int len = hexString.length() / 2;
		byte[] result = new byte[len];
		for (int i = 0; i < len; i++)
			result[i] = Integer.valueOf(hexString.substring(2 * i, 2 * i + 2),
					16).byteValue();
		return result;
	}
}