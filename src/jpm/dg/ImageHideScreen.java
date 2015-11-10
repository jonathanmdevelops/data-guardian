package jpm.dg;

import java.io.InputStream;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.Config;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

/**
 * Controls the Image hiding screen.
 * 
 */
public class ImageHideScreen extends HidingActivity implements OnClickListener {

	// Codes representing image type.
	private final static int HOST = 0;
	private final static int SECRET = 1;

	private Button hideButton;
	private ImageView hostImageView, secretImageView;
	private Bitmap hostBitmap, secretBitmap;
	private Uri hostUri, secretUri;
	private boolean hostFromCamera, secretFromCamera;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.image_hide);
		hideButton = (Button) findViewById(R.id.hideButton);
		hideButton.setOnClickListener(this);
		hostImageView = (ImageView) findViewById(R.id.hostImageView);
		hostImageView.setOnClickListener(this);
		secretImageView = (ImageView) findViewById(R.id.secretImageView);
		secretImageView.setOnClickListener(this);
		secretBitmap = null;
		hostBitmap = null;
		hostUri = null;
		secretUri = null;
		hostFromCamera = false;
		secretFromCamera = false;

		currentFileName = "";
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.hideButton:
			if (hostBitmap != null && secretBitmap != null) {
				displaySaveDialog();
			}
			break;
		case R.id.hostImageView:
			selectionOptions(HOST, "Select a cover image");
			break;
		case R.id.secretImageView:
			if (hostBitmap == null) {
				Util.showToast(ImageHideScreen.this,
						"Choose a Cover Image First");
			} else {
				selectionOptions(SECRET, "Select a secret image");
			}
		}
	}

	/**
	 * Manages data returned from camera or gallery sources.
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == Activity.RESULT_OK) {
			if (requestCode == HOST || requestCode == (HOST + CAMERA_OFFSET)) {
				hostFromCamera = (requestCode >= CAMERA_OFFSET);
				requestCode = HOST;
			} else {
				secretFromCamera = (requestCode >= CAMERA_OFFSET);
				requestCode = SECRET;
			}
			loadImage(data.getData(), requestCode);
		}
	}

	/**
	 * Displays a save dialog, allowing for filename input.
	 */
	public void displaySaveDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.filename));
		final EditText input = new EditText(this);
		input.setText(currentFileName);
		input.setInputType(InputType.TYPE_CLASS_TEXT);
		input.setSelection(input.getText().length());
		builder.setView(input);
		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				currentFileName = input.getText().toString();
				String error = Util.isValidFileName(currentFileName);
				if (error.equals("")) {
					new HideImageTask(ImageHideScreen.this)
							.execute();
				} else {
					Toast.makeText(getApplicationContext(), error,
							Toast.LENGTH_LONG).show();
					displaySaveDialog();
				}
			}
		});
		builder.setNegativeButton("Cancel",
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
						return;
					}
				});
		builder.setOnCancelListener(new OnCancelListener() {

			@Override
			public void onCancel(DialogInterface dialog) {
				currentFileName = input.getText().toString();
			}

		});
		builder.show();
	}

	/**
	 * Load an image from a given Uri.
	 * 
	 * @param sourceUri The Uri of the data.
	 * @param source The data source (cover image or secret image).
	 */
	private void loadImage(Uri sourceUri, int source) {
		sendBroadcast(new Intent(
				Intent.ACTION_MEDIA_MOUNTED,
				Uri.parse("file://" + Environment.getExternalStorageDirectory())));
		InputStream hostStream = null;
		InputStream secretStream = null;
		try {
			if (secretBitmap != null) {
				secretBitmap.recycle();
				secretBitmap = null;
			}
			if (source == HOST) {
				hostUri = sourceUri;
				hostStream = getContentResolver().openInputStream(hostUri);
				if (hostBitmap != null) {
					hostBitmap.recycle();
					hostBitmap = null;
				}
				hostBitmap = createScaledBitmapFromStream(hostStream, HOST);
				if (hostFromCamera) {
					//hostBitmap = Util.rotateBitmap(this, hostBitmap);
				}
			} else {
				secretUri = sourceUri;
			}
			if (secretUri != null) {
				secretStream = getContentResolver().openInputStream(secretUri);
				secretBitmap = createScaledBitmapFromStream(secretStream,
						SECRET);
				if (secretFromCamera) {
					//secretBitmap = Util.rotateBitmap(this, secretBitmap);
				}
			}
		} catch (Throwable t) {
			Dialogs.showErrorDialog(this, Dialogs.LOAD_ERROR);
		} finally {
			if (hostStream != null)
				try {
					hostStream.close();
				} catch (Exception e) {
				}
			if (secretStream != null)
				try {
					secretStream.close();
				} catch (Exception e) {
				}
		}
		secretImageView.setImageBitmap(secretBitmap);
		hostImageView.setImageBitmap(hostBitmap);
	}

	/**
	 * Scale the secret image to a within capacity size.
	 * 
	 * @param s
	 * @param source
	 * @return
	 */
	@SuppressWarnings("resource")
	protected Bitmap createScaledBitmapFromStream(InputStream stream, int source) {

		try {
			BitmapFactory.Options boundOptions = new BitmapFactory.Options();
			boundOptions.inJustDecodeBounds = true;
			BitmapFactory.decodeStream(stream, null, boundOptions);
			stream.close();

			final int originalWidth = boundOptions.outWidth;
			final int originalHeight = boundOptions.outHeight;
			
			BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
			
			int sampleSize;
			if (source == HOST) {
				sampleSize = Util.getSafeSampleSize(originalWidth,
						originalHeight);
				stream = this.getContentResolver().openInputStream(hostUri);
			} else {
				sampleSize = getSecretBitmapScale(originalWidth, originalHeight);
				stream = this.getContentResolver().openInputStream(secretUri);
			}
			bitmapOptions.inSampleSize = sampleSize;
			return BitmapFactory.decodeStream(stream, null, bitmapOptions);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Determine the sample size needed to scale the secret image.
	 * 
	 * @return The sample size as a power of two.
	 */
	private int getSecretBitmapScale(double width, double height) {
		double aspect = width / height;
		int hostPixels = hostBitmap.getWidth() * hostBitmap.getHeight();
		final int capacity = ((hostPixels * 3) / 4);
		double newSize = (width * height * 3) + 6;
		double scale = 1.0;
		double newWidth = width;
		double newHeight = height;

		while (newSize >= capacity) {
			scale += 0.5;
			newWidth = (int) (width / scale);
			newHeight = (int) (newWidth / aspect);
			newSize = (newWidth * newHeight * 3) + 6;
		}
		int sampleSize = 1;
		int e;
		for (e = 2; sampleSize < scale; e++) {
			sampleSize = (int) Math.pow(2, e);
		}
		return sampleSize;
	}

	/**
	 * Perform the image-in-image embedding operation.
	 * 
	 * @return The altered Bitmap, with secret image embedded.
	 */
	public Bitmap getBitmap() {
		int hostWidth = hostBitmap.getWidth();
		int hostHeight = hostBitmap.getHeight();
		
		int secretWidth = secretBitmap.getWidth();
		int secretHeight = secretBitmap.getHeight();
		
		int[] hostPixelData = new int[hostWidth * hostHeight];
		int[] secretPixelData = new int[secretWidth * secretHeight];
		// Split the bitmap into a one dimensional array
		hostBitmap.getPixels(hostPixelData, 0, hostWidth, 0, 0, hostWidth,
				hostHeight);
		secretBitmap.getPixels(secretPixelData, 0, secretWidth, 0, 0,
				secretWidth, secretHeight);
		int density = hostBitmap.getDensity();
		
		hostBitmap.recycle();
		secretBitmap.recycle();
		
		byte[] byteImage = encodeImage(hostPixelData, hostWidth,
				hostHeight, secretPixelData, secretWidth, secretHeight);
		
		hostPixelData = null;
		hostBitmap = null;
		secretPixelData = null;
		secretBitmap = null;
		
		int[] encodedPixelData = Util.byteArrayToIntArray(byteImage);
		byteImage = null;

		System.gc();
		Bitmap encodedBitmap = Bitmap.createBitmap(hostWidth, hostHeight,
				Config.ARGB_8888);
		// Make sure to keep the same density.
		encodedBitmap.setDensity(density);

		return Util.createBitmap(0, hostWidth, hostHeight, encodedBitmap,
				encodedPixelData);
	}

	/**
	 * Handles the embedding process of the secret image in the cover.
	 *
	 */
	private class HideImageTask extends EncodeDataTask {

		public HideImageTask(Context context) {
			super(currentFileName, context, parent);
		}
		
		@Override
		protected void onCancelled() {
			progressDialog.dismiss();
			if (success) {
				Dialogs.showEmailDialog(context, currentFileName);
			}
			loadImage(hostUri, HOST);
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(null);
			loadImage(hostUri, HOST);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (hostBitmap != null) {
			hostBitmap.recycle();
			hostBitmap = null;
		}
		hostImageView.setImageBitmap(null);

		if (secretBitmap != null) {
			secretBitmap.recycle();
			secretBitmap = null;
		}
		secretImageView.setImageBitmap(null);
		System.gc();
	}
}