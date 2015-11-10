package jpm.dg;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Environment;
import android.widget.ListAdapter;

/**
 * A class to generate common dialogs.
 * 
 */
public class Dialogs {

	// Error codes.
	public final static int IMAGE_TOO_LARGE = 0;
	public final static int NO_DATA = 1;
	public final static int LOAD_ERROR = 2;
	public final static String[] ERROR_MESSAGES = new String[] {
			"Image to large to process.\n\nUse images less than 1000x1000.",
			"No data could be found", "Error loading file" };

	/**
	 * Generates a message dialog builder.
	 * 
	 * @param context
	 * @param title
	 * @param message
	 * @return A completed builder, ready to show.
	 */
	private static Builder getDialog(Context context, String title,
			String message) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(title);
		builder.setMessage(message);
		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				dialog.dismiss();
			}
		});
		return builder;
	}

	/**
	 * Shows a basic message dialog.
	 * 
	 * @param context
	 * @param title
	 * @param message
	 */
	public static void showBasicDialog(Context context, String title,
			String message) {
		getDialog(context, title, message).show();
	}

	/**
	 * Adds an icon to a basic dialog.
	 * 
	 * @param context
	 * @param title
	 * @param message
	 * @param icon
	 */
	public static void showDialogWithIcon(Context context, String title,
			String message, int icon) {
		getDialog(context, title, message).setIcon(icon).show();
	}

	/**
	 * Generates and shows a completed email dialog.
	 * 
	 * @param context
	 * @param fileName
	 */
	public static void showEmailDialog(final Context context, String fileName) {
		final String fullPath = Environment
				.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString()
				+ "/Personal/" + fileName + ".png";
		AlertDialog.Builder builder = getDialog(context, "Operation Completed",
				"File saved in Personal folder as " + fileName + ".png")
				.setIcon(R.drawable.tick);
		builder.setPositiveButton("Email Image",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						Util.sendEmail(context, fullPath);
					}
				});
		builder.setNegativeButton("Done!",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.dismiss();
					}
				});
		builder.show();
	}

	/**
	 * Generates and shows the operation failed dialog.
	 * 
	 * @param context
	 * @param code
	 *            The error code.
	 */
	public static void showErrorDialog(Context context, int code) {
		getDialog(context, "Operation Failed", ERROR_MESSAGES[code]).setIcon(
				R.drawable.warning).show();
	}

	/**
	 * Generates and shows a list adapter dialog, with an attached listener.
	 * 
	 * @param context
	 * @param title
	 * @param adapter
	 * @param listener
	 */
	public static void showAdapterDialog(Context context, String title,
			ListAdapter adapter, OnClickListener listener) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(title);
		builder.setAdapter(adapter, listener);
		builder.show();
	}
}
