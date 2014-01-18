package org.liberty.android.fantastischmemo.downloader.quizlet;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

import org.apache.commons.io.IOUtils;
import org.liberty.android.fantastischmemo.AnyMemoDBOpenHelper;
import org.liberty.android.fantastischmemo.AnyMemoDBOpenHelperManager;
import org.liberty.android.fantastischmemo.R;
import org.liberty.android.fantastischmemo.dao.CardDao;

import org.liberty.android.fantastischmemo.domain.Card;
import org.liberty.android.fantastischmemo.downloader.quizlet.QuizletAccountActivity;
import org.liberty.android.fantastischmemo.ui.FileBrowserFragment;
import org.liberty.android.fantastischmemo.utils.AMGUIUtility;


import roboguice.util.Ln;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;

public class QuizletUploadActivity extends QuizletAccountActivity {
	
    private String authToken = null;
    
    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.upload_quizlet_screen);
    }
    
    @Override
    protected void onAuthenticated(final String[] authTokens) {

        this.authToken = authTokens[0];

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        FileBrowserFragment fragment = new FileBrowserFragment();
        fragment.setOnFileClickListener(fileClickListener);
        ft.add(R.id.file_list, fragment);
        ft.commit();
    }
    
    private void uploadToQuizlet(File file) {
    	
        // First read card because if it failed we don't even bother uploading.    	
        AnyMemoDBOpenHelper helper = AnyMemoDBOpenHelperManager.getHelper(file.getAbsolutePath());
        List<Card> cardList = null;
        try {
            final CardDao cardDao = helper.getCardDao();
            cardList = cardDao.queryForAll();
        } finally {
            AnyMemoDBOpenHelperManager.releaseHelper(helper);
        }

        // Following doing upload
        try {
            URL url1 = new URL("https://api.quizlet.com/2.0/sets");      
     		HttpsURLConnection conn = (HttpsURLConnection) url1.openConnection();
     		conn.setDoInput(true);
       		conn.setDoOutput(true);
    		conn.setRequestMethod("POST");
    		conn.addRequestProperty("Authorization", "Bearer " + authToken);
    		conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded"); 
    		 
    		OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
    		StringBuilder data = new StringBuilder();
    		data.append(String.format("whitespace=%s",URLEncoder.encode("1", "UTF-8")));
    		data.append(String.format("&title=%s",URLEncoder.encode(file.getName(), "UTF-8")));
    		
    		//Get cards from cardList
            for (int i = 0; i < cardList.size(); i++) {
                Card c = cardList.get(i);
                data.append(String.format("&terms[]=%s",URLEncoder.encode(c.getQuestion(), "UTF-8")));
                data.append(String.format("&definitions[]=%s",URLEncoder.encode(c.getAnswer(), "UTF-8")));
            }
            
    		data.append(String.format("&lang_terms=%s",URLEncoder.encode("en", "UTF-8")));
    		data.append(String.format("&lang_definitions=%s",URLEncoder.encode("en", "UTF-8")));
    		data.append(String.format("&allow_discussion=%s",URLEncoder.encode("true", "UTF-8")));
    		            
    		writer.write(data.toString());
            writer.close();    		

            if (conn.getResponseCode() / 100 >= 3) {
            	throw new IOException("Response code " +  conn.getResponseCode());    
            }
            else {
                Ln.i("The response is " + conn.getResponseCode());   
                String s = new String(IOUtils.toByteArray(conn.getInputStream()));
                Ln.i("Response is "+ s);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    private FileBrowserFragment.OnFileClickListener fileClickListener =
            new FileBrowserFragment.OnFileClickListener() {

                @Override
                public void onClick(File file) {
                    showUploadDialog(file);
                }
            };
         
            private void showUploadDialog(final File file) {
                new AlertDialog.Builder(this)
                    .setTitle(R.string.upload_text)
                    .setMessage(String.format(getString(R.string.upload_quizlet_message), file.getName()))
                    .setPositiveButton(R.string.ok_text, new DialogInterface.OnClickListener(){
                        @Override
                        public void onClick(DialogInterface arg0, int arg1){
                            UploadTask task = new UploadTask();
                            task.execute(file);
                        }
                    })
                    .setNegativeButton(R.string.cancel_text, null)
                    .show();
            }

            private class UploadTask extends AsyncTask<File, Void, Exception> {

                private ProgressDialog progressDialog;

                @Override
                public void onPreExecute() {
                    super.onPreExecute();
                    progressDialog = new ProgressDialog(QuizletUploadActivity.this);
                    progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                    progressDialog.setTitle(getString(R.string.loading_please_wait));
                    progressDialog.setMessage(getString(R.string.upload_wait));
                    progressDialog.setCancelable(false);
                    progressDialog.show();
                }

                @Override
                public Exception doInBackground(File... files) {
                    File file = files[0];
                    try {
                        uploadToQuizlet(file);                        
                    } catch (Exception e) {
                        Log.e(TAG, "Error uploading ", e);
                        return e;
                    }
                    return null;
                }


                @Override
                public void onPostExecute(Exception e){
                    if (e != null) {
                        AMGUIUtility.displayException(QuizletUploadActivity.this, getString(R.string.error_text), getString(R.string.error_text), e);
                    } else {
                        new AlertDialog.Builder(QuizletUploadActivity.this)
                            .setTitle(R.string.successfully_uploaded_text)
                            .setMessage(R.string.quizlet_successfully_uploaded_message)
                            .setPositiveButton(R.string.ok_text, null)
                            .show();
                    }
                    progressDialog.dismiss();
                }
            }
}
