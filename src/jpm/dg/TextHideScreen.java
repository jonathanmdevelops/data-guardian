package jpm.dg;

import java.io.InputStream;
import java.util.Arrays;
import android.R.color;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Controls the text hiding screen.
 * 
 */
public class TextHideScreen extends HidingActivity implements OnClickListener {

	private EditText editText;
	private Button hideButton;
	private ImageView imageView;
	private TextView imageText;
	private TextView inputStatusText;
	private Bitmap bitmap;
	private int charactersLeft;
	private int maxCharacters;
	private Uri currentUri;
	private int[] originalButtonLocation = new int[2];
	private boolean imageFromCamera, locationSet;

	private String currentPassword;
	private boolean isEncrypted;
	private int currentSecurity;

	private final static String[] SEC_LEVELS = { "Minimum", "Low", "Medium",
			"High", "Maximum" };

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.text_hide);
		editText = (EditText) findViewById(R.id.textInput);
		editText.setEnabled(false);
		hideButton = (Button) findViewById(R.id.hideButton);
		hideButton.setOnClickListener(this);
		locationSet = false;
		imageView = (ImageView) findViewById(R.id.imageView);
		imageView.setBackgroundColor(color.black);
		imageText = (TextView) findViewById(R.id.imageText);
		imageText.setOnClickListener(this);
		imageView.setOnClickListener(this);
		inputStatusText = (TextView) findViewById(R.id.inputStatusText);
		editText.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
			}

			@Override
			public void afterTextChanged(Editable s) {
				charactersLeft = maxCharacters - s.toString().length();
				inputStatusText.setText(charactersLeft + " Characters left");
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {

			}
		});
		final View activityRootView = findViewById(R.id.root);
		activityRootView.getViewTreeObserver().addOnGlobalLayoutListener(
				new OnGlobalLayoutListener() {
					public void onGlobalLayout() {
						if (!locationSet) {
							hideButton
									.getLocationOnScreen(originalButtonLocation);
							locationSet = true;
							return;
						}
						int[] newPos = new int[2];
						hideButton.getLocationOnScreen(newPos);
						if (Arrays.equals(newPos, originalButtonLocation)) {
							imageView.setVisibility(View.VISIBLE);
							imageText.setVisibility(View.VISIBLE);
						} else {
							imageView.setVisibility(View.GONE);
							imageText.setVisibility(View.GONE);
						}
					}
				});
		imageFromCamera = false;
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			imageFromCamera = extras.getBoolean("isFromCamera");
			loadImage(Uri.parse(extras.getString("imageUri")));
		}

		currentFileName = "";
		currentPassword = "";
		isEncrypted = false;
		currentSecurity = 0;
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.hideButton && bitmap != null) {
			displaySaveDialog();
		}
		if (v.getId() == R.id.imageText || v.getId() == R.id.imageView) {
			selectionOptions(0, "Select a cover image");
		}
	}

	/**
	 * Displays and controls a dialog for saving with a password and a security
	 * level.
	 */
	public void displaySaveDialog() {
		LayoutInflater inflater = getLayoutInflater();
		View alertLayout = inflater.inflate(R.layout.encode_dialog, null);

		final EditText fileNameField = (EditText) alertLayout
				.findViewById(R.id.fileNameField);
		final EditText passwordField = (EditText) alertLayout
				.findViewById(R.id.passField);
		final TextView passLabel = (TextView) alertLayout
				.findViewById(R.id.passLabel);
		final CheckBox cbShowPassword = (CheckBox) alertLayout
				.findViewById(R.id.cb_ShowPassword);
		final SeekBar seekBar = (SeekBar) alertLayout
				.findViewById(R.id.seekbar);
		final TextView securityLabel = (TextView) alertLayout
				.findViewById(R.id.securityLabel);

		seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				currentSecurity = progress;
				securityLabel
						.setText("Security Level: " + SEC_LEVELS[progress]);
			}

			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			public void onStopTrackingTouch(SeekBar seekBar) {
			}
		});

		cbShowPassword.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				isEncrypted = cbShowPassword.isChecked();
				if (isEncrypted) {
					passwordField.setEnabled(true);
					passLabel.setVisibility(View.VISIBLE);
					passwordField.setVisibility(View.VISIBLE);
				} else {
					passwordField.setEnabled(false);
					passLabel.setVisibility(View.INVISIBLE);
					passwordField.setVisibility(View.INVISIBLE);
				}

			}
		});

		cbShowPassword.setChecked(!isEncrypted);
		cbShowPassword.performClick();
		fileNameField.setText(currentFileName);
		fileNameField.setSelection(fileNameField.getText().length());
		passwordField.setText(currentPassword);
		seekBar.setProgress(currentSecurity);

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Encode Text:");
		builder.setView(alertLayout);

		// Set up the buttons
		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				currentFileName = fileNameField.getText().toString()
						.replace(" ", "_").trim();
				String error = Util.isValidFileName(currentFileName);
				boolean isBelowCapacity = true;
				currentPassword = null;
				if (cbShowPassword.isChecked()
						&& !passwordField.getText().toString().isEmpty()) {
					currentPassword = passwordField.getText().toString();
					isBelowCapacity = Crypto.isBelowCapacity(editText.getText()
							.toString(), (bitmap.getWidth() * bitmap
							.getHeight()));
				}
				if (error.equals("") && isBelowCapacity) {
					new HideTextTask(TextHideScreen.this).execute();
				} else {
					if (!isBelowCapacity) {
						error = "This data will exceed the image capacity if encrypted";
					}
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
						currentFileName = fileNameField.getText().toString()
								.replace(" ", "_").trim();
						currentPassword = passwordField.getText().toString();
						dialog.cancel();
						return;
					}
				});

		builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				currentFileName = fileNameField.getText().toString()
						.replace(" ", "_").trim();
				currentPassword = passwordField.getText().toString();
			}
		});

		builder.show();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == Activity.RESULT_OK) {
			imageFromCamera = (requestCode >= CAMERA_OFFSET);
			loadImage(data.getData());
		}
	}

	/**
	 * Loads an image from a uri.
	 * 
	 * @param sourceUri
	 */
	private void loadImage(Uri sourceUri) {
		sendBroadcast(new Intent(
				Intent.ACTION_MEDIA_MOUNTED,
				Uri.parse("file://" + Environment.getExternalStorageDirectory())));
		InputStream stream = null;
		try {
			if (bitmap != null) {
				bitmap.recycle();
				bitmap = null;
			}

			imageView.setImageBitmap(null);
			stream = getContentResolver().openInputStream(sourceUri);

			BitmapFactory.Options boundOptions = new BitmapFactory.Options();
			boundOptions.inJustDecodeBounds = true;
			BitmapFactory.decodeStream(stream, null, boundOptions);
			stream.close();

			BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
			bitmapOptions.inSampleSize = Util.getSafeSampleSize(
					boundOptions.outWidth, boundOptions.outHeight);

			stream = this.getContentResolver().openInputStream(sourceUri);
			bitmap = BitmapFactory.decodeStream(stream, null,
					bitmapOptions);
			stream.close();
			if (imageFromCamera) {
				bitmap = Util.rotateBitmap(this, bitmap);
			}
			imageView.setImageBitmap(bitmap);
			currentUri = sourceUri;
			editText.setEnabled(true);

			int totalPixels = bitmap.getWidth() * bitmap.getHeight();
			maxCharacters = ((totalPixels * 3) / 4) - 6;
			String current = editText.getText().toString();
			if (current != null && current.length() > maxCharacters) {
				current.substring(0, maxCharacters);
				editText.setText(current);
			}
			charactersLeft = maxCharacters
					- editText.getText().toString().length();
			inputStatusText.setText(charactersLeft + " Characters left");
			InputFilter[] filters = new InputFilter[1];
			filters[0] = new InputFilter.LengthFilter(maxCharacters);
			editText.setFilters(filters);

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (stream != null)
				try {
					stream.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
		}
	}

	/**
	 * Retrieves an encoded Bitmap.
	 */
	public Bitmap getBitmap() {

		int width = bitmap.getWidth();
		int height = bitmap.getHeight();
		int[] originalPixelData = new int[width * height];

		String message = (Util.TEST_MODE) ? Util.getTestString(width * height,
				1.00, getBaseContext()) : editText.getText().toString();

		// Split the bitmap into a one dimensional array
		bitmap.getPixels(originalPixelData, 0, width, 0, 0, width, height);
		int density = bitmap.getDensity();
		bitmap.recycle();

		byte[] encodedBytes = encodeMessage(originalPixelData, message,
				currentPassword, currentSecurity);
		originalPixelData = null;
		bitmap = null;
		if (encodingTask.isCancelled()) {
			return null;
		}
		int[] encodedPixelData = Util.byteArrayToIntArray(encodedBytes);
		encodedBytes = null;

		System.gc();
		Bitmap encodedBitmap = Bitmap.createBitmap(width, height,
				Config.ARGB_8888);
		// Make sure to keep the same density
		encodedBitmap.setDensity(density);
		return Util.createBitmap(0, width, height, encodedBitmap,
				encodedPixelData);
	}

	/**
	 * Handles hiding text.
	 * 
	 */
	private class HideTextTask extends EncodeDataTask {

		public HideTextTask(Context context) {
			super(currentFileName, context, parent);
		}

		@Override
		protected void onCancelled() {
			if (bitmap == null) {
				loadImage(currentUri);
			}
			progressDialog.dismiss();
			if (success) {
				Dialogs.showEmailDialog(context, currentFileName);
			}
		}

		@Override
		protected void onPostExecute(Void result) {
			if (bitmap == null) {
				loadImage(currentUri);
			}
			super.onPostExecute(null);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (bitmap != null) {
			bitmap.recycle();
			bitmap = null;
		}
		imageView.setImageBitmap(null);
		System.gc();
	}
}
