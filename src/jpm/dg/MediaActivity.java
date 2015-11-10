package jpm.dg;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import android.app.Activity;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.Media;

/**
 * A superclass for activities that require access to the camera and gallery
 * services.
 *
 */
public class MediaActivity extends Activity {

	public final static String END_MESSAGE_CONSTANT = "##!";
	public final static String START_MESSAGE_CONSTANT = "!##";
	public final static String START_COMPRESSED_CONSTANT = "!#@";
	public static final boolean IS_RANDOM = true;
	public final static int CHANNELS = 3;
	public final static int[] SHIFT_VALUES = { 6, 4, 2, 0 };
	
	public static final int CAMERA_OFFSET = 10;

	/**
	 * Retrieves an image from the device camera.
	 * @param code The request code.
	 */
	public void getImageFromCamera(int code) {
		Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		startActivityForResult(intent, code + CAMERA_OFFSET);
	}

	/**
	 * Retrieves an image from the device gallery.
	 * @param code The request code.
	 */
	public void getImageFromGallery(int code) {
		Intent intent = new Intent(Intent.ACTION_PICK);
		intent.setType("image/*");
		startActivityForResult(intent, code);
	}

	/**
	 * Loads a full-size Bitmap from memory.
	 * @param sourceUri The data source.
	 * @return An unscaled Bitmap.
	 */
	public Bitmap loadFullImage(Uri sourceUri) {
		InputStream s = null;
		try {
			s = getContentResolver().openInputStream(sourceUri);
			final BufferedInputStream is = new BufferedInputStream(s, 32 * 1024);
			final Options decodeBitmapOptions = new Options();
			final Options decodeBoundsOptions = new Options();
			decodeBoundsOptions.inJustDecodeBounds = true;
			is.mark(32 * 1024);
			BitmapFactory.decodeStream(is, null, decodeBoundsOptions);
			is.reset();
			final int width = decodeBoundsOptions.outWidth;
			final int height = decodeBoundsOptions.outHeight;
			if ((width * height) > Util.PIXEL_LIMIT) {
				Dialogs.showErrorDialog(this, Dialogs.IMAGE_TOO_LARGE);
			} else {
				decodeBitmapOptions.inSampleSize = 1;
				return BitmapFactory
						.decodeStream(is, null, decodeBitmapOptions);
			}
		} catch (Throwable t) {
			t.printStackTrace();
			if(t.getMessage().contains("Mark")) {
				Dialogs.showErrorDialog(this, Dialogs.IMAGE_TOO_LARGE);
			} else {
				Dialogs.showErrorDialog(this, Dialogs.NO_DATA);
			}
		} finally {
			if (s != null)
				try {
					s.close();
				} catch (Exception e) {
				}
		}
		return null;
	}

	/**
	 * Saves an image to the "Personal" folder.
	 * @param fileName The filename to use for the image.
	 * @param bmp The Bitmap object of the image.
	 * @return True if and only if the image was successfully saved.
	 */
	public boolean saveImage(String fileName, Bitmap bmp) {
		if (bmp != null && !bmp.isRecycled()) {
			File storagePath = new File(Environment.getExternalStoragePublicDirectory(
		            Environment.DIRECTORY_PICTURES).toString() + "/Personal"); 
		    storagePath.mkdirs();

			FileOutputStream out = null;
			File imageFile = new File(storagePath, String.format("%s.png",
					fileName));

			try {
				out = new FileOutputStream(imageFile);
				bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
				ContentValues values = new ContentValues(3);
				values.put(Images.Media.TITLE, "image");
				values.put(Images.Media.MIME_TYPE, "image/png");
				values.put("_data", imageFile.getAbsolutePath());
				getContentResolver().insert(Media.EXTERNAL_CONTENT_URI, values);
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			} finally {
				if (out != null) {
					try {
						out.flush();
						out.close();
					} catch (Exception e) {
						e.printStackTrace();
						return false;
					}

				}
			}
			return true;
		}
		return false;
	}

	/**
	 * Displays a set of source media options for hiding and retrieval.
	 * @param source Denotes the activities request source.
	 * @param instruction The instruction message to display.
	 */
	public void selectionOptions(final int source, String instruction) {
		final String[] items = new String[] { "Gallery", "Camera" };
		final Integer[] icons = new Integer[] { R.drawable.gallery,
				R.drawable.camera };
		Dialogs.showAdapterDialog(this, instruction, new ArrayAdapterWithIcon(
				this, items, icons), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int item) {
				if (item == 0) {
					getImageFromGallery(source);
				} else {
					getImageFromCamera(source);
				}
			}
		});
	}
}
