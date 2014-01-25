package org.liberty.android.fantastischmemo.downloader.quizlet;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

import javax.inject.Inject;
import javax.net.ssl.HttpsURLConnection;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.liberty.android.fantastischmemo.AMEnv;
import org.liberty.android.fantastischmemo.AnyMemoDBOpenHelper;
import org.liberty.android.fantastischmemo.AnyMemoDBOpenHelperManager;
import org.liberty.android.fantastischmemo.dao.CardDao;
import org.liberty.android.fantastischmemo.domain.Card;
import org.liberty.android.fantastischmemo.domain.Category;
import org.liberty.android.fantastischmemo.domain.LearningData;
import org.liberty.android.fantastischmemo.downloader.DownloadItem;
import org.liberty.android.fantastischmemo.downloader.DownloaderUtils;
import org.liberty.android.fantastischmemo.utils.AMFileUtil;

import roboguice.util.Ln;
import android.net.Uri;

import java.util.List;

class QuizletDownloadHelper {

	private DownloaderUtils downloaderUtils;

	private AMFileUtil amFileUtil;
	
	@Inject
	public void setDownloaderUtils(DownloaderUtils downloaderUtils) {
		this.downloaderUtils = downloaderUtils;
	}

	@Inject
	public void setAmFileUtil(AMFileUtil amFileUtil) {
		this.amFileUtil = amFileUtil;
	}
	
	/**
	 * Fetch cardsets list from Quizlet
	 * @param userId user name
	 * @param authToken oauth token
	 * @return cardsets list
	 * @throws IOException IOException If http response code is not 2xx
	 * @throws JSONException If the response is invalid JSON
	 */
	public List<DownloadItem> fetchCardsetsList(String userId, String authToken) throws IOException, JSONException {
		List<DownloadItem> downloadItemList = new ArrayList<DownloadItem>();
		URL url = new URL("https://api.quizlet.com/2.0/users/" + userId + "/sets");
		String response = makeApiCall(url, authToken);
		
		JSONArray jsonArray = new JSONArray(response);
		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject jsonItem = jsonArray.getJSONObject(i);

			String address = jsonItem.getString("url");
			String description = new StringBuilder().append("<br />")
					.append(jsonItem.getInt("term_count")).append("<br />")
					.append(jsonItem.getLong("created_date")).append("<br />")
					.append(jsonItem.getString("description")).append("<br />")
					.append(jsonItem.getString("created_by")).toString();

			DownloadItem item = new DownloadItem(
					DownloadItem.ItemType.Database,
					jsonItem.getString("title"), description, address);
			item.setExtras("id", "" + jsonItem.getInt("id"));
			downloadItemList.add(item);
		}

		return downloadItemList;
	}
	
	/**
	 * Download cardsets list fron Quizlet and save to a db file
	 * @param setId cardset ID
	 * @param authToken oauth token
	 * @return The path of saved db file
	 * @throws IOException IOException If http response code is not 2xx
	 * @throws JSONException If the response is invalid JSON
	 */
	public String downloadCardset(String setId, String authToken) throws IOException, JSONException {
		URL url = new URL("https://api.quizlet.com/2.0/sets/" + setId);
		String response = makeApiCall(url, authToken);

		JSONObject rootObject = new JSONObject(response);
		JSONArray flashcardsArray = rootObject.getJSONArray("terms");
		int termCount = rootObject.getInt("term_count");
		boolean hasImage = rootObject.getBoolean("has_images");
		List<Card> cardList = new ArrayList<Card>(termCount);

		// handle image
		String dbname = downloaderUtils.validateDBName(rootObject.getString("title")) + ".db";
		String imagePath = AMEnv.DEFAULT_IMAGE_PATH + dbname + "/";
		if (hasImage) {
			FileUtils.forceMkdir(new File(imagePath));
		}

		for (int i = 0; i < flashcardsArray.length(); i++) {
			JSONObject jsonItem = flashcardsArray.getJSONObject(i);
			String question = jsonItem.getString("term");
			String answer = jsonItem.getString("definition");

			// Download images, ignore image downloading error.
			try {
				if (jsonItem.has("image") && !jsonItem.isNull("image")
						&& hasImage) {
					JSONObject imageItem = jsonItem.getJSONObject("image");
					String imageUrl = imageItem.getString("url");
					String downloadFilename = Uri.parse(imageUrl)
							.getLastPathSegment();
					downloaderUtils.downloadFile(imageUrl, imagePath
							+ downloadFilename);
					answer += "<br /><img src=\"" + downloadFilename + "\"/>";
				}
			} catch (Exception e) {
				Ln.e("Error downloading image.", e);
			}
			Card card = new Card();
			card.setQuestion(question);
			card.setAnswer(answer);
			card.setCategory(new Category());
			card.setLearningData(new LearningData());
			cardList.add(card);
		}

		/* Make a valid dbname from the title */
		String dbpath = AMEnv.DEFAULT_ROOT_PATH;
		String fullpath = dbpath + dbname;
		amFileUtil.deleteFileWithBackup(fullpath);
		AnyMemoDBOpenHelper helper = AnyMemoDBOpenHelperManager
				.getHelper(fullpath);
		try {
			CardDao cardDao = helper.getCardDao();
			cardDao.createCards(cardList);
			long count = helper.getCardDao().getTotalCount(null);
			if (count <= 0L) {
				throw new RuntimeException("Downloaded empty db.");
			}
		} finally {
			AnyMemoDBOpenHelperManager.releaseHelper(helper);
		}

		return fullpath;
	}
	
	/**
	 * Make API call to Quizlet server with oauth
	 * @param url API call endpoint
	 * @param authToken oauth auth token
	 * @return Response of API call
	 * @throws IOException If http response code is not 2xx
	 */
	private String makeApiCall (URL url, String authToken) throws IOException {
		
		HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
		conn.addRequestProperty("Authorization", "Bearer " + authToken);

		String response = new String(IOUtils.toByteArray(conn.getInputStream()));
        if (conn.getResponseCode() / 100 >= 3) {
        	throw new IOException("Response code: " +  conn.getResponseCode() + " Response is: " + response);    
        }
        
        return response;
	}
}