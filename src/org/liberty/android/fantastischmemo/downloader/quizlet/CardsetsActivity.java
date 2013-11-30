package org.liberty.android.fantastischmemo.downloader.quizlet;

import java.io.IOException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.liberty.android.fantastischmemo.R;
import org.liberty.android.fantastischmemo.downloader.quizlet.CardsetsListFragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import roboguice.util.Ln;

import java.net.MalformedURLException;

public class CardsetsActivity extends QuizletAccountActivity {
	private String oauthToken;
	private String userId;
	
	@Override
    protected void onAuthenticated(final String[] authTokens) {
		Ln.i("The oauth taken is " + authTokens[0]);
		Ln.i("My user name is: " + authTokens[1]);
		oauthToken = authTokens[0];
		userId = authTokens[1];
		new Thread() {
			@Override
			public void run() {
		        try {
		            URL url1 = new URL("https://api.quizlet.com/2.0/users/" + userId + "/sets");
		            HttpsURLConnection conn = (HttpsURLConnection) url1.openConnection();
		            conn.addRequestProperty("Authorization", "Bearer " + String.format(oauthToken));

		            String s = new String(IOUtils.toByteArray(conn.getInputStream()));
		            Ln.i("The relust set aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa: " + s);
		            JSONObject jsonObject = new JSONObject(s);
		            if (jsonObject.has("error")) {
		                String error = jsonObject.getString("error");
		                Log.e(TAG, "API call error: " + error);
		            }
		            
		        } catch (MalformedURLException e) {
		             Ln.e("MalformedURLException", e);
		             assert false;
		        } catch (IOException e) {
		             Ln.e("IOException", e);
		             assert false;
		        } catch (JSONException e) {
		             Ln.e("JSONException", e);
		             assert false;
		        }
			}
		}.start();
		
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        Fragment newFragment = new CardsetsListFragment();
        Bundle args = new Bundle();
        args.putString(CardsetsListFragment.EXTRA_AUTH_TOKEN, oauthToken);
        args.putString(CardsetsListFragment.EXTRA_USER_ID, userId);
        newFragment.setArguments(args);
        ft.add(R.id.cardsets_list, newFragment);
        ft.commit();		
    }
	
    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.cardsets_list_screen);
    }
}

