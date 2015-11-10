package jpm.dg;

import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.Vector;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;

/**
 * A template for decoding activities.
 *
 */
public abstract class RetrievingActivity extends MediaActivity {

	private static final byte[] AND_BYTES = { (byte) 0xC0, 0x30, 0x0C, 0x03 };

	protected final RetrievingActivity parent = this;
	public volatile DecodeDataTask decodingTask;

	/**
	 * Decodes a String from an image.
	 * @param originalData The image data.
	 * @param key The password (possibly null).
	 * @param isRandom True if extraction should be for random/optimised algorithms.
	 * @return The message discovered (if any).
	 */
	public String decodeMessage(int[] originalData, String key, boolean isRandom) {
		StringBuilder builder = new StringBuilder();
		int shiftIndex = 4;
		byte tmp = 0x00;
		Random rng = new Random(originalData[0]);
		boolean compressed = false;
		byte[] oneDPix = Util.convertArray(originalData);
		Set<Integer> usedElements = new HashSet<Integer>();
		usedElements.add(0);
		boolean msgEnded = false;
		int index = -1;
		while (!builder.toString().endsWith(END_MESSAGE_CONSTANT) && !msgEnded
				&& !decodingTask.isCancelled()) {
			if (isRandom) {
				index = 0;
				while (usedElements.contains(index)) {
					index = rng.nextInt(originalData.length);
				}
				usedElements.add(index);
			} else {
				index++;
			}
			for (int i = index * 3; i < (index * 3) + CHANNELS; i++) {
				tmp = (byte) (tmp | ((oneDPix[i] << SHIFT_VALUES[shiftIndex
						% SHIFT_VALUES.length]) & AND_BYTES[shiftIndex++
						% SHIFT_VALUES.length]));
				if (shiftIndex % SHIFT_VALUES.length == 0) {
					byte[] nonso = { tmp };
					String str;
					try {
						str = new String(nonso, "ISO-8859-1");
					} catch (UnsupportedEncodingException e) {
						str = "";
						e.printStackTrace();
					}
					if (builder.toString().endsWith(END_MESSAGE_CONSTANT)) {
						break;
					} else {
						builder.append(str);

						if (builder.toString().length() == START_MESSAGE_CONSTANT
								.length()) {
							if (START_MESSAGE_CONSTANT.equals(builder
									.toString())) {
								compressed = false;
							} else {
								if (START_COMPRESSED_CONSTANT.equals(builder
										.toString())) {
									compressed = true;
								} else {
									builder = null;
									msgEnded = true;
									break;
								}

							}
						}
					}
					tmp = 0x00;
				}
			}
		}
		return processMessage(builder, compressed, key);
	}

	/**
	 * Retrieves an image from a Bitmap.
	 * @param pixelData The bytes of the Bitmap.
	 * @return The resultant secret image (if any).
	 */
	public Bitmap decodeImage(byte[] pixelData) {
		Vector<Byte> v = new Vector<Byte>();
		decodingTask.updateProgress("Checking pixels...");
		try {
			int shiftIndex = 4;
			byte tmp = 0x00;
			for (int i = 0; i < pixelData.length && !decodingTask.isCancelled(); i++) {
				tmp = (byte) (tmp | ((pixelData[i] << SHIFT_VALUES[shiftIndex
						% SHIFT_VALUES.length]) & AND_BYTES[shiftIndex++
						% SHIFT_VALUES.length]));
				if (shiftIndex % SHIFT_VALUES.length == 0) {
					v.addElement(new Byte(tmp));
					tmp = 0x00;
				}
			}
			pixelData = null;
			System.gc();
			byte[] raw = new byte[v.size()];
			for (int i = 0; i < raw.length; i++) {
				raw[i] = v.get(i).byteValue();
			}
			v = null;
			System.gc();
			int[] values = Util.byteArrayToIntArray(raw);
			raw = null;
			System.gc();
			int width = values[0];
			int height = values[1];
			if (width <= 0 || height <= 0 || decodingTask.isCancelled()) {
				values = null;
				return null;
			}
			decodingTask.updateProgress("Reassembling image...");
			Bitmap encodedBitmap = Bitmap.createBitmap(width, height,
					Config.ARGB_8888);
			return Util.createBitmap(2, width, height, encodedBitmap, values);
		} catch (Exception e) {
			e.printStackTrace();
			v = null;
			pixelData = null;
			System.gc();
			return null;
		} catch (Throwable t) {
			t.printStackTrace();
			v = null;
			pixelData = null;
			System.gc();
			return null;
		}
	}

	public String processMessage(StringBuilder builder, boolean compressed,
			String key) {
		String message = builder.toString();
		if (message == null || message.length() < 7) {
			return "";
		}
		decodingTask.updateProgress("Processing message....");
		message = message.substring(START_MESSAGE_CONSTANT.length(),
				message.length() - END_MESSAGE_CONSTANT.length());
		if (key != null) {
			try {
				message = Crypto.decrypt(key, message);
			} catch (Exception e) {
				e.printStackTrace();
				return "Decryption Failed";
			}
		}
		message = compressed ? Compression.decompress(message) : message;
		return message;
	}

}
