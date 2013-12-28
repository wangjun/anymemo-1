package org.liberty.android.fantastischmemo.downloader.quizlet;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.net.ssl.HttpsURLConnection;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.liberty.android.fantastischmemo.AMEnv;
import org.liberty.android.fantastischmemo.AnyMemoDBOpenHelper;
import org.liberty.android.fantastischmemo.AnyMemoDBOpenHelperManager;
import org.liberty.android.fantastischmemo.dao.CardDao;
import org.liberty.android.fantastischmemo.domain.Card;
import org.liberty.android.fantastischmemo.domain.Category;
import org.liberty.android.fantastischmemo.domain.LearningData;
import org.liberty.android.fantastischmemo.downloader.AbstractDownloaderFragment;
import org.liberty.android.fantastischmemo.downloader.DownloadItem;
import org.liberty.android.fantastischmemo.downloader.DownloaderQuizlet;
import org.liberty.android.fantastischmemo.downloader.DownloaderUtils;
import org.liberty.android.fantastischmemo.utils.AMFileUtil;
import org.liberty.android.fantastischmemo.utils.RecentListUtil;

import roboguice.util.Ln;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

public class CardsetsListFragment extends AbstractDownloaderFragment {
	
    public static final String EXTRA_AUTH_TOKEN = "authToken";
    
    public static final String EXTRA_USER_ID = "userId";
    
    private String authToken;
    
    private String userId;
    
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
    
	@Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Bundle args = getArguments();
        assert args != null : "The EXTRA_AUTH_TOKEN and EXTRA_USER_ID must be passed to CardsetsListFragment";
        this.authToken = args.getString(EXTRA_AUTH_TOKEN);
        this.userId = args.getString(EXTRA_USER_ID);
    }
    
	@Override
	protected List<DownloadItem> initialRetrieve() throws Exception {
        List<DownloadItem> downloadItemList = new ArrayList<DownloadItem>();

        URL url1 = new URL("https://api.quizlet.com/2.0/users/" + userId + "/sets");
		HttpsURLConnection conn = (HttpsURLConnection) url1.openConnection();
		conn.addRequestProperty("Authorization", "Bearer " + authToken);

		String s = new String(IOUtils.toByteArray(conn.getInputStream()));
		Ln.i("The relust sets are: " + s);
		            
		JSONArray jsonArray = new JSONArray(s);
		for(int i = 0; i < jsonArray.length(); i++){
		     JSONObject jsonItem = jsonArray.getJSONObject(i);

		     String address = jsonItem.getString("url");
		     String description = new StringBuilder()
                   .append("<br />")
		           .append(jsonItem.getInt("term_count"))
		           .append("<br />")
		           .append(jsonItem.getLong("created_date"))
		           .append("<br />")
		           .append(jsonItem.getString("description"))
		           .append("<br />")
		           .append(jsonItem.getString("created_by"))
		           .toString();

		     DownloadItem item = new DownloadItem(DownloadItem.ItemType.Database,
		                        jsonItem.getString("title"),
		                        description,
		                        address);
		     downloadItemList.add(item);
		}

        return downloadItemList;
	}
	
	@Override
	protected String fetchDatabase(DownloadItem di) throws Exception {
		String setId = di.getAddress().substring(19, 27);
		Ln.i("The setId is: " + setId);
        URL url1 = new URL("https://api.quizlet.com/2.0/sets/" + setId);
		HttpsURLConnection conn = (HttpsURLConnection) url1.openConnection();
		conn.addRequestProperty("Authorization", "Bearer " + authToken);

		String s = new String(IOUtils.toByteArray(conn.getInputStream()));
		Ln.i("The relust set is: " + s);
		
        JSONObject rootObject = new JSONObject(s);
        JSONArray flashcardsArray = rootObject.getJSONArray("terms");
        int termCount = rootObject.getInt("term_count");
        boolean hasImage = rootObject.getBoolean("has_images");
        List<Card> cardList = new ArrayList<Card>(termCount);
        
        // handle image
        String dbname = downloaderUtils.validateDBName(di.getTitle()) + ".db";
        String imagePath = AMEnv.DEFAULT_IMAGE_PATH + dbname + "/";
        if (hasImage) {
            FileUtils.forceMkdir(new File(imagePath));
        }


        for(int i = 0; i < flashcardsArray.length(); i++){
            JSONObject jsonItem = flashcardsArray.getJSONObject(i);
            String question = jsonItem.getString("term");
            String answer = jsonItem.getString("definition");

            // Download images, ignore image downloading error.
            try {
                if (jsonItem.has("image") && !jsonItem.isNull("image") && hasImage) {
                    JSONObject imageItem = jsonItem.getJSONObject("image");
                    String imageUrl = imageItem.getString("url");
                    String downloadFilename = Uri.parse(imageUrl).getLastPathSegment();
                    downloaderUtils.downloadFile(imageUrl, imagePath + downloadFilename);
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
        AnyMemoDBOpenHelper helper = AnyMemoDBOpenHelperManager.getHelper(fullpath);
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
	
	@Override
	protected void openCategory(DownloadItem di) {
        // Do nothing
	}

	@Override
	protected void goBack() {
        // Do nothing
	}
}