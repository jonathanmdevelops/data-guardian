package jpm.dg;

import java.io.UnsupportedEncodingException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * A class for the compression and decompression of String objects.
 * 
 */
public class Compression {

	// The maximum factor increase of a String.
	private static final int MAX_INCREASE = 12;

	/**
	 * Compresses a String object using the DEFLATE algorithm.
	 * 
	 * @param inputString The text to be compressed.
	 * @return The compressed String.
	 */
	public static String compress(String inputString) {
		// Encode the String into bytes.
		byte[] input;
		try {
			input = inputString.getBytes("ISO-8859-1");
			int originalLength = input.length;
			// Compress the bytes.
			byte[] buffer = new byte[originalLength * MAX_INCREASE];
			Deflater compresser = new Deflater();
			compresser.setInput(input);
			compresser.finish();
			int compressedLength = compresser.deflate(buffer);
			// Fill the output array.
			byte[] output = new byte[compressedLength];
			for (int i = 0; i < output.length; i++) {
				output[i] = buffer[i];
			}
			return new String(output, "ISO-8859-1");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return inputString;
	}

	/**
	 * Decompresses a DEFLATED String.
	 * 
	 * @param inputString The compressed String.
	 * @return The decompressed String.
	 */
	public static String decompress(String inputString) {
		try {
			byte[] input = inputString.getBytes("ISO-8859-1");
			Inflater decompresser = new Inflater(false);
			decompresser.setInput(input, 0, input.length);
			byte[] result = new byte[input.length * MAX_INCREASE];
			int resultLength = decompresser.inflate(result);
			String outputString = new String(result, 0, resultLength,
					"ISO-8859-1");
			decompresser.end();
			return outputString;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return inputString;
	}
}