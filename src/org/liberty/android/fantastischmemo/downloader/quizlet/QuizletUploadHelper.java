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
import org.liberty.android.fantastischmemo.dao.CardDao;
import org.liberty.android.fantastischmemo.domain.Card;

import roboguice.util.Ln;

class QuizletUploadHelper {
	
	/**
	 * Upload cardsets list to Quizlet from a db file
	 * @param db file
	 * @param authToken oauth token
	 * @return null
	 * @throws IOException IOException If http response code is not 2xx
	 */
    public void uploadToQuizlet(File file, String authToken) throws IOException {
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
            URL url = new URL("https://api.quizlet.com/2.0/sets");      
    		OutputStreamWriter writer =  makeApiCall(url, authToken);
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
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
	/**
	 * Make Post API call to Quizlet server with oauth
	 * @param url API call endpoint
	 * @param authToken oauth auth token
	 * @return OutputStreamWriter of API call
	 * @throws IOException If http response code is not 2xx
	 */
	private OutputStreamWriter makeApiCall (URL url, String authToken) throws IOException {
 		HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
 		conn.setDoInput(true);
   		conn.setDoOutput(true);
		conn.setRequestMethod("POST");
		conn.addRequestProperty("Authorization", "Bearer " + authToken);
		conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded"); 
		OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
        
        if (conn.getResponseCode() / 100 >= 3) {
        	throw new IOException("Response code: " +  conn.getResponseCode());    
        }
        else {
            String s = new String(IOUtils.toByteArray(conn.getInputStream()));
            Ln.i("Response code: " + conn.getResponseCode() + " Response is: "+ s);
        }
        
        return writer;
	}
}