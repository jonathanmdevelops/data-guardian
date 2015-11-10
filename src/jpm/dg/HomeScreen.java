package jpm.dg;

import android.os.Bundle;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ListAdapter;

/**
 * Controls the Home screen.
 *
 */
public class HomeScreen extends MediaActivity implements OnClickListener {

	// Activity request codes.
	private static final int HIDE_TEXT = 0;
	private static final int RETRIEVE_TEXT = 2;
	private static final int RETRIEVE_IMAGE = 3;
	
	private static final String PREFS_NAME = "DATA_GUARD";
	private static final String FIRST_LOAD = "IS_FIRST_LOAD";
	private ListAdapter typeAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.home);
		// Setup the hide data button.
		ImageButton lock = (ImageButton) findViewById(R.id.encryptButton);
		lock.setOnClickListener(this);
		// Setup the retrieve data button.
		ImageButton unlocked = (ImageButton) findViewById(R.id.decryptButton);
		unlocked.setOnClickListener(this);
		
		// Create an adapter for the options dialog.
		typeAdapter = new ArrayAdapterWithIcon(this, new String[] { "Text",
				"Image" }, new Integer[] { R.drawable.text, R.drawable.image });
		loadPreferences();
	}

	/**
	 * Loads shared preferences from a file.
	 */
	private void loadPreferences() {
		SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME,
				MODE_PRIVATE);
		boolean isFirstLoad = sharedPreferences.getBoolean(FIRST_LOAD, true);
		// If it is the first time a user has launched the app then
		// show an instructions dialog.
		if (isFirstLoad) {
			Dialogs.showBasicDialog(this, "Is this your first time?",
					getString(R.string.app_summary));
			SharedPreferences.Editor editor = sharedPreferences.edit();
			editor.putBoolean(FIRST_LOAD, false);
			editor.commit();
		}
	}

	@Override
	public void onClick(View v) {
		// Storage must be available for operations to be performed.
		if (!Util.isStorageAvailable(this)) {
			return;
		}
		if (v.getId() == R.id.encryptButton) {
			options(true);
		}
		if (v.getId() == R.id.decryptButton) {
			options(false);
		}
	}

	/**
	 * Generate an options menu based on input.
	 * @param hide True when user has clicked "hide data".
	 */
	private void options(boolean hide) {
		String title;
		android.content.DialogInterface.OnClickListener listener;
		final MediaActivity base = this;
		if (hide) {
			title = "What do you want to hide?";
			listener = new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int item) {
					if (item == 0) {
						base.selectionOptions(HIDE_TEXT, "Select a cover image");
					} else {
						startActivity(new Intent(getApplicationContext(),
								ImageHideScreen.class));
					}
				}
			};
		} else {
			title = "What do you want to retrieve?";
			listener = new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int item) {
					if (item == 0) {
						base.getImageFromGallery(RETRIEVE_TEXT);
					} else {
						base.getImageFromGallery(RETRIEVE_IMAGE);
					}
				}
			};
		}
		Dialogs.showAdapterDialog(this, title, typeAdapter, listener);
	}

	/**
	 * Launches the relevant screen after an image has been selected.
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == Activity.RESULT_OK) {
			Intent i;
			requestCode = (requestCode >= CAMERA_OFFSET) ? (requestCode - CAMERA_OFFSET)
					: requestCode;
			switch (requestCode) {
			case HIDE_TEXT:
				i = new Intent(getApplicationContext(), TextHideScreen.class);
				break;
			case RETRIEVE_TEXT:
				i = new Intent(getApplicationContext(),
						TextRetrieveScreen.class);
				break;
			default:
				i = new Intent(getApplicationContext(),
						ImageRetrieveScreen.class);
			}
			if (data.getData().toString() != null) {
				// Pass data to the intent.
				i.putExtra("imageUri", data.getData().toString());
				i.putExtra("isFromCamera", (requestCode >= CAMERA_OFFSET));
			}
			startActivity(i);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.home, menu);
		return true;
	}

	/**
	 * Manages about and icon source information.
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_about:
			showInfo(true);
			return true;
		case R.id.icon_sources:
			showInfo(false);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	/**
	 * Displayes application information.
	 * @param isAbout True if the info requested is "About".
	 * False if "Icon Sources".
	 */
	private void showInfo(boolean isAbout) {
		String title, message;
		title = isAbout ? "About Simple Steg" : "Iconography";
		message = isAbout ? "Developed by ---"
				: getString(R.string.icon_sources);
		Dialogs.showBasicDialog(this, title, message);
	}
}