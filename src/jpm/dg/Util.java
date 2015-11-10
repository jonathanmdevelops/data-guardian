package jpm.dg;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.view.Display;
import android.view.WindowManager;
import android.widget.Toast;

/**
 * A set of common utility functions.
 * 
 */
public class Util {

	private final static char[] ILLEGAL_CHARACTERS = { '/', '\n', '\r', '\t',
			'\0', '\f', '`', '?', '*', '\\', '<', '>', '|', '\"', ':' };
	public final static int PIXEL_LIMIT = 1000 * 1000;

	public final static boolean TEST_MODE = false;

	/**
	 * Concatenates two arrays.
	 * 
	 * @param A
	 * @param B
	 * @return The result of a + b.
	 */
	public static byte[] concat(byte[] A, byte[] B) {
		int aLen = A.length;
		int bLen = B.length;
		byte[] C = new byte[aLen + bLen];
		System.arraycopy(A, 0, C, 0, aLen);
		System.arraycopy(B, 0, C, aLen, bLen);
		return C;
	}

	/**
	 * Concatenates two arrays.
	 * 
	 * @param A
	 * @param B
	 * @return The result of a + b.
	 */
	public static int[] concat(int[] A, int[] B) {
		int aLen = A.length;
		int bLen = B.length;
		int[] C = new int[aLen + bLen];
		System.arraycopy(A, 0, C, 0, aLen);
		System.arraycopy(B, 0, C, aLen, bLen);
		return C;
	}

	/**
	 * Converts a packed integer array to a byte array.
	 * 
	 * @param array
	 *            The integer array.
	 * @return A byte array of three times the size containing the unpacked
	 *         numbers.
	 */
	public static byte[] convertArray(int[] array) {
		byte[] newarray = new byte[array.length * 3];

		for (int i = 0; i < array.length; i++) {

			newarray[i * 3] = (byte) ((array[i] >> 16) & 0xFF);
			newarray[i * 3 + 1] = (byte) ((array[i] >> 8) & 0xFF);
			newarray[i * 3 + 2] = (byte) ((array[i]) & 0xFF);

		}
		return newarray;
	}

	/**
	 * Converts a single integer to a byte array.S
	 * 
	 * @return A byte array of three times the size containing the unpacked
	 *         numbers.
	 */
	public byte[] intToByteArray(int value) {
		return new byte[] { (byte) (value >>> 24), (byte) (value >>> 16),
				(byte) (value >>> 8), (byte) value };
	}

	/**
	 * Converts from a byte array to a packed integer array.
	 * 
	 * @param b
	 * @return An integer array of a third of the size, with bytes packed in
	 *         order.
	 */
	public static int[] byteArrayToIntArray(byte[] b) {
		int size = b.length / 3;
		// Memory management
		System.runFinalization();
		System.gc();
		int[] result = new int[size];
		int off = 0;
		int index = 0;
		while (off < (b.length / 3) * 3) {
			result[index] = byteArrayToInt(b, off);
			index++;
			off += 3;
		}
		return result;
	}

	public static int byteArrayToInt(byte[] b, int offset) {
		int value = 0x00000000;
		for (int i = 0; i < 3; i++) {
			int shift = (3 - 1 - i) * 8;
			value |= (b[i + offset] & 0x000000FF) << shift;
		}
		value = value & 0x00FFFFFF;
		return value;
	}

	/**
	 * Creates a Bitmap from a set of pixel intensities of specified width and
	 * height.
	 * 
	 * @param index
	 *            Index to begin reading pixels.
	 * @param width
	 * @param height
	 * @param target
	 *            Target Bitmap object.
	 * @param pixelData
	 * @return
	 */
	public static Bitmap createBitmap(int index, int width, int height,
			Bitmap target, int[] pixelData) {
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				target.setPixel(x, y, Color
						.argb(0xFF, pixelData[index] >> 16 & 0xFF,
								pixelData[index] >> 8 & 0xFF,
								pixelData[index++] & 0xFF));
			}
		}
		pixelData = null;
		return target;
	}

	/**
	 * Calculates a safe sample size of an image.
	 * 
	 * @param width
	 * @param height
	 * @return A scale that reduces the image the least amount whilst keeping it
	 *         within the total pixel limit.
	 */
	public static int getSafeSampleSize(double width, double height) {
		int size = (int) (width * height);
		if (size > PIXEL_LIMIT) {
			int sampleSize = 1;
			int e;
			for (e = 0; size > PIXEL_LIMIT; e++) {
				sampleSize = (int) Math.pow(2, e);
				size = (int) (width * height) / ((int) Math.pow(sampleSize, 2));
			}
			return sampleSize;
		} else {
			return 1;
		}
	}

	/**
	 * An SD card test.
	 * 
	 * @param context
	 * @return True if and only if a valid SD exists to be written on.
	 */
	public static boolean isStorageAvailable(Context context) {
		Boolean isSDPresent = android.os.Environment.getExternalStorageState()
				.equals(android.os.Environment.MEDIA_MOUNTED);
		if (!isSDPresent) {
			showToast(context, "No available SD card can be found");
		}
		return isSDPresent;
	}

	/**
	 * 
	 * @param fileName
	 * @return True if and only if the filename given is valid.
	 */
	public static String isValidFileName(String fileName) {
		String errorMessage = "";
		if (fileName.equals(null) || fileName.equals("")) {
			return "Filenames cannot be empty";
		}
		String illegals = "";
		for (int i = 0; i < fileName.length(); i++) {
			for (int j = 0; j < ILLEGAL_CHARACTERS.length; j++) {
				if (fileName.charAt(i) == ILLEGAL_CHARACTERS[j]) {
					illegals += fileName.charAt(i) + ", ";
				}
			}
		}
		if (illegals.length() != 0) {
			errorMessage = "Illegal characters: "
					+ illegals.substring(0, illegals.length() - 2);
		}
		return errorMessage;
	}

	public static void showToast(Context context, String message) {
		Toast.makeText(context, message, Toast.LENGTH_LONG).show();
	}

	@SuppressWarnings("deprecation")
	@SuppressLint("NewApi")
	/**
	 * Rotates a birmap according to its size.
	 * @param context
	 * @param bitmap
	 * @return The correctly oriented Bitmap.
	 */
	public static Bitmap rotateBitmap(Context context, Bitmap bitmap) {
		int width, height;
		WindowManager wm = (WindowManager) context
				.getSystemService(Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();
		if (Build.VERSION.SDK_INT >= 13) {
			Point size = new Point();
			display.getSize(size);
			width = size.x;
			height = size.y;
		} else {
			width = display.getWidth();
			height = display.getHeight();
		}
		if (width > height) {
			Matrix matrix = new Matrix();
			matrix.postRotate(90);
			bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
					bitmap.getHeight(), matrix, true);
		}
		return bitmap;
	}

	/**
	 * Sends an email with the given file as an attachment.
	 * 
	 * @param context
	 * @param filePath
	 */
	public static void sendEmail(Context context, String filePath) {
		Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
		emailIntent.setType("application/image");
		emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL,
				new String[] {});
		emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,
				"");
		emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, "");
		emailIntent.putExtra(Intent.EXTRA_STREAM,
				Uri.parse("file://" + filePath));
		context.startActivity(Intent.createChooser(emailIntent, "Send mail..."));
	}

	/**
	 * Calculates the total number of LSBs that have been changed between two
	 * sets of byte arrays.
	 * 
	 * @param original
	 * @param modified
	 * @return The total number of bits different between original and modified.
	 */
	public static int totalValueChange(byte[] original, byte[] modified) {
		int count = 0;
		int length = Math.min(original.length, modified.length);
		byte lsb = 0x03;
		for (int i = 0; i < length; i++) {
			byte a = original[i];
			byte b = modified[i];
			a &= lsb;
			b &= lsb;
			count += Math.abs(a - b);
		}
		return count;
	}

	/**
	 * Retrieves a test String.
	 * 
	 * @param numPixels
	 *            The pixels of the target image.
	 * @param percentage
	 *            The percentage of the image to fill.
	 * @param context
	 * @return A String that will fill the correct amount of the image capacity.
	 */
	public static String getTestString(int numPixels, double percentage,
			Context context) {
		String test = context.getResources().getString(R.string.test_text);
		StringBuffer buffer = new StringBuffer();

		double totalBytes = numPixels * 3;
		int textLength = test.getBytes().length;
		double bytesNeeded = totalBytes / 4;
		bytesNeeded *= percentage;
		int target = (int) bytesNeeded - 6;

		while (buffer.toString().getBytes().length < target) {
			if (textLength >= target) {
				buffer.append(test.substring(0, target - 1));
			} else {
				buffer.append(test);
			}
		}

		return buffer.toString();
	}
}