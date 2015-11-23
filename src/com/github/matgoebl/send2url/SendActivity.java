package com.github.matgoebl.send2url;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.widget.Toast;

public class SendActivity extends Activity
{
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		String serverUrl = prefs.getString("serverurl", "");

		Intent intent = getIntent();
		if (intent != null && intent.getAction().equals(Intent.ACTION_SEND)) {
			Bundle extras = intent.getExtras();
			try {
				AbstractHttpEntity postData;
				if (intent.getType().equals("text/plain")) {
					//Use an entity with a known length to work around the
					//POST-requires-content-length-header bug in lighttpd
					postData = new StringEntity(intent.getStringExtra(Intent.EXTRA_TEXT));
				} else if(extras != null && extras.containsKey(Intent.EXTRA_STREAM)) {
					Uri uri = (Uri) extras.getParcelable(Intent.EXTRA_STREAM);
					ContentResolver contentResolver = getContentResolver();
					postData = new InputStreamEntity(contentResolver.openInputStream(uri),-1);
					postData.setChunked(true);
				} else {
					Toast.makeText(getBaseContext(), getString(R.string.UnknownType),
							Toast.LENGTH_LONG).show();
					setResult(Activity.RESULT_CANCELED);
					finish();
					return;
				}

				httpPost(serverUrl, postData, intent.getType());
				Toast.makeText(getBaseContext(), getString(R.string.SendSuccess),
						Toast.LENGTH_LONG).show();
				setResult(Activity.RESULT_OK);
			} catch (Exception e) {
				Toast.makeText(getBaseContext(), getString(R.string.SendFailed) + ":\n" + e.toString(),
						Toast.LENGTH_LONG).show();
				setResult(Activity.RESULT_CANCELED);
			}
		}
		finish();
	}

	public static void httpPost(String serverUrl, AbstractHttpEntity postData, String type) throws ClientProtocolException, IOException, Exception  {
		DefaultHttpClient httpclient = new DefaultHttpClient();
		HttpPost httppost = new HttpPost(serverUrl);

		URL url = new URL(serverUrl);
		String userInfo = url.getUserInfo();
		if( userInfo != null ) {
			httppost.addHeader("Authorization", "Basic " + Base64.encodeToString(userInfo.getBytes(), Base64.NO_WRAP));
		}
		if (type == null) {
			type = "binary/octet-stream";
		}

		postData.setContentType(type);
		httppost.setEntity(postData);

		HttpResponse response = httpclient.execute(httppost);
		response.getEntity().getContent().close();
		httpclient.getConnectionManager().shutdown();

		int status = response.getStatusLine().getStatusCode();
		if ( status < 200 || status > 299 ) {
			throw new Exception(response.getStatusLine().toString());
		}
	}
}

