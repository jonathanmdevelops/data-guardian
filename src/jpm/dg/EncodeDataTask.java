package jpm.dg;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.graphics.Bitmap;
import android.os.AsyncTask;

/**
 * A template for encoding tasks.
 *
 */
public abstract class EncodeDataTask extends AsyncTask<Void, Object, Void> {

	protected Context context;
	protected boolean success;
	protected ProgressDialog progressDialog;
	private String fileName;
	private HidingActivity parentActivity;
	
	public EncodeDataTask(String fileName, Context context, HidingActivity parentActivity) {
		this.context = context;
		this.fileName = fileName;
		this.parentActivity = parentActivity;
		parentActivity.encodingTask = this;
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		progressDialog = new ProgressDialog(context);
		progressDialog.setTitle("Encoding Data");
        progressDialog.setMessage("Preparing...");
        progressDialog.setIndeterminate(false);
        progressDialog.setMax(100);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		progressDialog.setOnCancelListener(new OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				cancel(true);
			}
		});
		progressDialog.show();
	};
	
	@Override
    protected void onProgressUpdate(Object... values) {
        super.onProgressUpdate(values);
        if(values[0] instanceof Integer) {
        	progressDialog.setProgress((Integer) values[0]);
        } else {
        	progressDialog.setMessage((String) values[0]);
        }
    }
	
	public void updateProgress(Integer value){
        publishProgress(value);
    }

	@Override
	protected Void doInBackground(Void... params) {
		publishProgress("Manipulating image...");
		Bitmap result = parentActivity.getBitmap();
		if (isCancelled()) {
			if (result != null) {
				result.recycle();
				result = null;
				System.gc();
			}
		}
		if (result != null) {
			publishProgress("Saving image...");
			success = parentActivity.saveImage(fileName, result);
			result.recycle();
			result = null;
			System.gc();
		}
		publishProgress(100);
		return null;
	}
	
	@Override
	protected void onPostExecute(Void result) {
		super.onPostExecute(null);
		if(progressDialog != null) {
			progressDialog.dismiss();
		}
		String title, message;
		int icon;
		if (success) {
			Dialogs.showEmailDialog(context, fileName);
		} else {
			title = "Operation Failed";
			message = "The data could not be saved";
			icon = R.drawable.warning;
			Dialogs.showDialogWithIcon(context, title, message, icon);
		}
	};
}
