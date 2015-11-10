package jpm.dg;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import android.graphics.Bitmap;

/**
 * A template for embedding activities.
 * 
 */
public abstract class HidingActivity extends MediaActivity {

	// The bit positions of the colour values.
	private final static int[] BINARY_CHANNELS = { 16, 8, 0 };

	public volatile EncodeDataTask encodingTask;

	protected String currentFileName;
	protected final HidingActivity parent = this;

	/**
	 * Encodes a String in pixel data.
	 * 
	 * @param originalPixelData
	 *            Pixel colour values.
	 * @param message
	 *            Secret message.
	 * @param key
	 *            Password (possibly null).
	 * @param security
	 *            Security level for optimised embedding.
	 * @return The bytes with data embedded.
	 */
	public byte[] encodeMessage(int[] originalPixelData, String message,
			String key, int security) {
		message.replace(START_MESSAGE_CONSTANT, "");
		message.replace(START_COMPRESSED_CONSTANT, "");
		message.replace(END_MESSAGE_CONSTANT, "");

		String compressed = Compression.compress(message);

		boolean shouldCompress = (compressed.length() < message.length())
				&& key == null;

		String prefix = (shouldCompress && !Util.TEST_MODE) ? START_COMPRESSED_CONSTANT
				: START_MESSAGE_CONSTANT;
		byte[] messageBytes = null;
		try {
			message = shouldCompress ? compressed : message;
			if (key == null) {
				message += END_MESSAGE_CONSTANT;
				message = prefix + message;
				messageBytes = message.getBytes("ISO-8859-1");
			} else {
				messageBytes = Util.concat(Util.concat(
						prefix.getBytes("ISO-8859-1"),
						Crypto.encrypt(key, message).getBytes("ISO-8859-1")),
						END_MESSAGE_CONSTANT.getBytes("ISO-8859-1"));
			}
		} catch (Exception e) {
			messageBytes = message.getBytes();
			e.printStackTrace();
		}
		// return encodeData(originalPixelData, messageBytes, IS_RANDOM, task);
		return optimisedEmbedding(originalPixelData, messageBytes, security,
				false);
	}

	/**
	 * Embed bytes within pixel data.
	 * 
	 * @param originalData
	 *            Clean data.
	 * @param messageBytes
	 *            Bytes to embed.
	 * @param isRandom
	 *            If true the data is distributed randomly throughout the image.
	 * @return
	 */
	private byte[] encodeData(int[] originalData, byte[] messageBytes,
			boolean isRandom) {
		int shiftIndex = 4;
		byte[] encodedPixelData = Util.convertArray(originalData);
		int messageIndex = 0;
		int resultIndex = 0;
		boolean msgEnded = false;
		Random rng = new Random(originalData[0]);
		Set<Integer> usedPixels = new HashSet<Integer>();
		usedPixels.add(0);
		int element = -1;
		while (!msgEnded && !encodingTask.isCancelled()) {
			byte tmp = 0;
			if (isRandom) {
				element = 0;
				while (usedPixels.contains(element)) {
					element = rng.nextInt(originalData.length);
				}
				usedPixels.add(element);
			} else {
				element++;
				float progress = (float) element;
				progress /= ((float) messageBytes.length * 1.33);
				progress *= 100;
				encodingTask.updateProgress((int) progress);
			}
			resultIndex = element * 3;
			for (int channelIndex = 0; channelIndex < CHANNELS; channelIndex++) {
				if (!msgEnded) {
					tmp = (byte) ((((originalData[element] >> BINARY_CHANNELS[channelIndex]) & 0xFF) & 0xFC) | ((messageBytes[messageIndex] >> SHIFT_VALUES[(shiftIndex++)
							% SHIFT_VALUES.length]) & 0x3));
					if (shiftIndex % SHIFT_VALUES.length == 0) {
						messageIndex++;
					}
					if (messageIndex == messageBytes.length) {
						msgEnded = true;
					}
				} else {
					tmp = (byte) ((((originalData[element] >> BINARY_CHANNELS[channelIndex]) & 0xFF)));
				}
				encodedPixelData[resultIndex++] = tmp;
			}
		}
		return encodedPixelData;
	}

	private byte[] optimisedEmbedding(int[] original, byte[] messageBytes,
			int security, boolean aimLower) {
		Random rng = new Random();
		byte[] output = encodeData(original, messageBytes, IS_RANDOM);
		byte[] inter = Arrays.copyOf(output, output.length);
		int originalChange = Util.totalValueChange(Util.convertArray(original),
				output);
		int currentBest = originalChange;
		int totalRuns = security * 25;
		totalRuns = (security > 0) ? totalRuns : 1;

		for (int i = 0; i < totalRuns && !encodingTask.isCancelled(); i++) {
			int[] newData = Arrays.copyOf(original, original.length);
			int R = (((newData[0] >> 16) & 0xff) + (-3 + rng.nextInt(6))) % 256;
			int G = (((newData[0] >> 8) & 0xff) + (-3 + rng.nextInt(6))) % 256;
			int B = ((newData[0] & 0xff) + (-3 + rng.nextInt(6))) % 256;
			newData[0] = 0xff000000 | (R << 16) | (G << 8) | B;

			inter = encodeData(newData, messageBytes, IS_RANDOM);
			int change = Util.totalValueChange(Util.convertArray(newData),
					inter);
			if ((change < currentBest && aimLower)
					|| (change > currentBest && !aimLower)) {
				currentBest = change;
				output = Arrays.copyOf(inter, inter.length);
			}
			float progress = (float) i;
			progress /= totalRuns;
			progress *= 100;
			encodingTask.updateProgress((int) progress);
		}
		return output;
	}

	public byte[] encodeImage(int[] originalPixelData, int imageCols,
			int imageRows, int[] secretData, int secretWidth, int secretHeight) {
		int[] dimensions = { secretWidth, secretHeight };
		secretData = Util.concat(dimensions, secretData);
		byte[] messageBytes = Util.convertArray(secretData);
		return encodeData(originalPixelData, messageBytes, false);
	}

	public abstract Bitmap getBitmap();

}
