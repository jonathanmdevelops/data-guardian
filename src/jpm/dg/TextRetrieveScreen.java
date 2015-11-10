package jpm.dg;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

/**
 * Controls the text retrieval screen.
 * 
 */
public class TextRetrieveScreen extends RetrievingActivity implements
		OnClickListener {

	private TextView retrievedText;
	private Button imageButton, passButton;
	private Bitmap bitmap;
	private Uri sourceUri;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.text_retrieve);
		retrievedText = (TextView) findViewById(R.id.textView);
		imageButton = (Button) findViewById(R.id.newImageButton);
		imageButton.setOnClickListener(this);
		passButton = (Button) findViewById(R.id.passwordButton);
		passButton.setOnClickListener(this);
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			sourceUri = Uri.parse(extras.getString("imageUri"));
			loadImage();
		}
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.newImageButton) {
			getImageFromGallery(0);
		} else {
			loadImage();
		}
	}

	/**
	 * Loads the full bitmap selected, with no scaling.
	 */
	private void loadImage() {
		if (bitmap != null) {
			bitmap.recycle();
			bitmap = null;
		}
		bitmap = loadFullImage(sourceUri);
		if (bitmap != null) {
			getPassword();
		}
	}

	/**
	 * Shows and retrieves the data from a password dialog.
	 */
	private void getPassword() {
		if (bitmap == null) {
			return;
		}
		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		builder.setTitle("Encryption");
		builder.setMessage("Enter your password: ");
		// Set an EditText view to get user input
		final EditText input = new EditText(this);
		builder.setView(input);

		builder.setPositiveButton("Enter",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						new RetrieveTextTask(TextRetrieveScreen.this, input
								.getText().toString()).execute();
					}
				});

		builder.setNegativeButton("No Password",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						new RetrieveTextTask(TextRetrieveScreen.this, null)
								.execute();
					}
				});
		builder.show();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == Activity.RESULT_OK) {
			sourceUri = data.getData();
			loadImage();
		}
	}

	/**
	 * Handles retrieving text from an image.
	 * 
	 */
	private class RetrieveTextTask extends DecodeDataTask {

		private String key;

		public RetrieveTextTask(Context context, String key) {
			super(context, parent);
			this.key = key;
		}

		@Override
		protected String doInBackground(Void... params) {
			String message = null;
			publishProgress("Analysing image...");
			try {
				message = decodeImage(key);
			} catch (Exception e) {
				message = null;
			} catch (Throwable t) {
				message = null;
			}
			if (bitmap != null) {
				bitmap.recycle();
				bitmap = null;
			}
			System.gc();
			return message;
		}

		@Override
		protected void onPostExecute(Object output) {
			super.onPostExecute(output);
			progressDialog.dismiss();
			String result = (String) output;
			if (result != null && !result.equals("")) {
				retrievedText.setText(result);
			} else {
				retrievedText.setText("");
				Dialogs.showErrorDialog(context, Dialogs.NO_DATA);
			}

		};

		/**
		 * Retrieves text and decrypts it (if needed).
		 * 
		 * @param key
		 *            The text password.
		 * @return The secret message to be displayed to the user.
		 */
		private String decodeImage(String key) {
			int[] pixels = null;
			try {
				pixels = new int[bitmap.getWidth() * bitmap.getHeight()];
				bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0,
						bitmap.getWidth(), bitmap.getHeight());
				publishProgress("Collecting data...");
				return ""
						+ decodeMessage(pixels, key, HidingActivity.IS_RANDOM);
			} catch (Throwable e) {
				pixels = null;
				return null;
			}
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (bitmap != null) {
			bitmap.recycle();
			bitmap = null;
		}
		System.gc();
	}
}