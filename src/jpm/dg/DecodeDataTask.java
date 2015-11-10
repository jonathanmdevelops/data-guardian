package jpm.dg;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.AsyncTask;

/**
 * A template for decoding tasks.
 *
 */
public abstract class DecodeDataTask extends AsyncTask<Void, String, Object> {

	protected Context context;
	protected ProgressDialog progressDialog;
	protected RetrievingActivity parentActivity;
	
	public DecodeDataTask(Context context, RetrievingActivity parentActivity) {
		this.context = context;
		this.parentActivity = parentActivity;
		parentActivity.decodingTask = this;
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		progressDialog = new ProgressDialog(context);
		progressDialog.setTitle("Decoding Data");
        progressDialog.setMessage("Preparing...");
        progressDialog.setIndeterminate(true);
		progressDialog.setOnCancelListener(new OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				cancel(true);
			}
		});
		progressDialog.show();
	};
	
	@Override
    protected void onProgressUpdate(String... values) {
        super.onProgressUpdate(values);
        progressDialog.setMessage(values[0]);
    }
	
	public void updateProgress(String message){
        publishProgress(message);
    }
	
	@Override
	protected void onCancelled() {
		if(progressDialog != null) {
			progressDialog.dismiss();
		}
	}
}
