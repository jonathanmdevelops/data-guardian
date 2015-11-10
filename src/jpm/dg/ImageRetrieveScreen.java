package jpm.dg;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

/**
 * Controls the image retrieval screen.
 *
 */
public class ImageRetrieveScreen extends RetrievingActivity implements
		OnClickListener {

	private ImageView imageView;
	private Bitmap sourceBitmap, retrievedBitmap;
	private Uri sourceUri;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.image_retrieve);
		
		imageView = (ImageView) findViewById(R.id.imageView);
		Button imageButton = (Button) findViewById(R.id.newImageButton);
		imageButton.setOnClickListener(this);
		
		Button saveButton = (Button) findViewById(R.id.saveButton);
		saveButton.setOnClickListener(this);
		
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
			if (!Util.isStorageAvailable(this)) {
				return;
			}
			displaySaveDialog("");
		}
	}

	/**
	 * Load an image from a source and decode it.
	 */
	private void loadImage() {
		if (sourceBitmap != null) {
			sourceBitmap.recycle();
			sourceBitmap = null;
		}
		sourceBitmap = loadFullImage(sourceUri);
		if (sourceBitmap != null) {
			new RetrieveImageTask(ImageRetrieveScreen.this).execute();
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == Activity.RESULT_OK) {
			sourceUri = data.getData();
			loadImage();
		}
	}

	/**
	 * Decode the Bitmap currently in memory.
	 * @return True if and only if a secret image is found.
	 */
	private boolean decodeImage() {
		int[] pixels = null;
		byte[] b = null;
		int width = sourceBitmap.getWidth();
		int height = sourceBitmap.getHeight();
		try {
			pixels = new int[width * height];
			sourceBitmap.getPixels(pixels, 0, width, 0, 0, width, height);
			if (sourceBitmap != null) {
				sourceBitmap.recycle();
				sourceBitmap = null;
			}
			b = Util.convertArray(pixels);
			pixels = null;
			System.gc();
			retrievedBitmap = decodeImage(b);
			b = null;
			return (retrievedBitmap != null);
		} catch (Throwable e) {
			e.printStackTrace();
			pixels = null;
			b = null;
			return false;
		}
	}

	/**
	 * Handles the decoding of the Bitmap.
	 *
	 */
	public class RetrieveImageTask extends DecodeDataTask {
		
		private boolean success;
		
		public RetrieveImageTask(Context context) {
			super(context, parent);
			success = false;
		}


		@Override
		protected void onPostExecute(Object result) {
			imageView.setImageBitmap(retrievedBitmap);
			progressDialog.dismiss();
			if (!success) {
				Dialogs.showDialogWithIcon(context, "Operation Failed",
						"No image could be retrieved", R.drawable.warning);
			}
		}

		@Override
		protected Void doInBackground(Void... params) {
			if (retrievedBitmap != null) {
				retrievedBitmap.recycle();
				retrievedBitmap = null;
			}
			success = decodeImage();
			return null;
		};
	}

	/**
	 * Saves an image asynchronously.
	 *
	 */
	public class SaveImageTask extends AsyncTask<Void, Void, Void> {
		
		private Context context;
		private String fileName;
		private boolean success;
		private ProgressDialog progressDialog;

		public SaveImageTask(Context context, String fileName) {
			this.context = context;
			this.fileName = fileName;
			success = false;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			progressDialog = ProgressDialog.show(context, "Saving Image", "",
					true, true);
			progressDialog.setOnCancelListener(new OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					cancel(true);
				}
			});
		};

		@Override
		protected void onCancelled() {
			if(progressDialog != null) {
				progressDialog.dismiss();
			}
			if (success) {
				showResult();
			}
		}

		@Override
		protected Void doInBackground(Void... params) {
			success = saveImage(fileName, retrievedBitmap);
			return null;
		};

		@Override
		protected void onPostExecute(Void result) {
			progressDialog.dismiss();
			sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED,
					Uri.parse("file://"
							+ Environment.getExternalStorageDirectory())));
			showResult();
		}

		/**
		 * Displays the result of the decoding task.
		 */
		private void showResult() {
			String title, message;
			int icon;
			if (success) {
				title = ("Operation Completed");
				message = "File saved in Personal folder as " + fileName
						+ ".png";
				icon = R.drawable.tick;
			} else {
				title = "Operation Failed";
				message = "The data could not be saved";
				icon = R.drawable.warning;
			}
			Dialogs.showDialogWithIcon(context, title, message, icon);
		}
	}

	/**
	 * Displays a save dialog.
	 * @param initName The filename to initialise with.
	 */
	public void displaySaveDialog(String initName) {
		if(retrievedBitmap == null) {
			return;
		}
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.filename));
		final EditText input = new EditText(this);
		input.setText(initName);
		input.setInputType(InputType.TYPE_CLASS_TEXT);
		builder.setView(input);
		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String fileName = input.getText().toString().replace(" ", "_")
						.trim();
				String error = Util.isValidFileName(fileName);
				if (error.equals("")) {
					new SaveImageTask(ImageRetrieveScreen.this, fileName)
							.execute();
				} else {
					Util.showToast(getApplicationContext(), error);
					displaySaveDialog(fileName);
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

		builder.show();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (sourceBitmap != null) {
			sourceBitmap.recycle();
			sourceBitmap = null;
		}
		if (retrievedBitmap != null) {
			retrievedBitmap.recycle();
			retrievedBitmap = null;
		}
		System.gc();
	}
}