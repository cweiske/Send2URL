package de.cweiske.playVideoOnDreambox;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class SendActivity extends Activity
{
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		String serverUrl = prefs.getString("serverurl", "");

		Intent intent = getIntent();
		if (intent != null && intent.getAction().equals(Intent.ACTION_SEND)) {
			try {
				if (!intent.getType().equals("text/plain")) {
					Toast.makeText(getBaseContext(), getString(R.string.UnknownType),
							Toast.LENGTH_LONG).show();
					setResult(Activity.RESULT_CANCELED);
					finish();
					return;
				}

				httpPost(serverUrl, intent.getStringExtra(Intent.EXTRA_TEXT));
				Toast.makeText(getBaseContext(), getString(R.string.SendSuccess),
						Toast.LENGTH_LONG).show();
				setResult(Activity.RESULT_OK);
			} catch (Exception e) {
				Toast.makeText(getBaseContext(), getString(R.string.SendFailed) + ":\n" + e.getMessage(),
						Toast.LENGTH_LONG).show();
				setResult(Activity.RESULT_CANCELED);
			}
		}
		finish();
	}

	public static void httpPost(String serverUrl, String pageUrl) throws Exception
	{
		URL url = new URL(serverUrl);

		HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
		urlConnection.setDoOutput(true);//POST
		urlConnection.setFixedLengthStreamingMode(pageUrl.length());
		urlConnection.setRequestProperty("Content-Type", "text/plain");
		String userInfo = url.getUserInfo();
		if( userInfo != null ) {
			urlConnection.setRequestProperty(
				"Authorization",
				"Basic " + Base64.encodeToString(userInfo.getBytes(), Base64.NO_WRAP)
			);
		}

		OutputStream out = urlConnection.getOutputStream();
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, "UTF-8"));
		writer.write(pageUrl);
		writer.flush();
		writer.close();
		out.close();

		int statusCode = urlConnection.getResponseCode();
		if (statusCode < 200 || statusCode > 299 ) {
			throw new Exception(urlConnection.getResponseMessage());
		}
	}
}
