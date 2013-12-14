package org.liberty.android.fantastischmemo.downloader.quizlet;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;
import org.liberty.android.fantastischmemo.downloader.AbstractDownloaderFragment;
import org.liberty.android.fantastischmemo.downloader.DownloadItem;
import org.liberty.android.fantastischmemo.downloader.DownloadItem.ItemType;
import org.liberty.android.fantastischmemo.downloader.DownloaderQuizlet.DescriptionBuilder;

public class CardsetsListFragment extends AbstractDownloaderFragment {
	
    public static final String EXTRA_AUTH_TOKEN = "authToken";
    
    public static final String EXTRA_USER_ID = "userId";
	
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
		conn.addRequestProperty("Authorization", "Bearer " + String.format(oauthToken));

		String s = new String(IOUtils.toByteArray(conn.getInputStream()));
		Ln.i("The relust sets are: " + s);
		JSONObject jsonObject = new JSONObject(s);		            
		if (jsonObject.has("error")) {
		            String error = jsonObject.getString("error");
		            Log.e(TAG, "API call error: " + error);
		        }
		            
		JSONArray jsonArray = new JSONArray(s);
		for(int i = 0; i < jsonArray.length(); i++){
		     JSONObject jsonItem = jsonArray.getJSONObject(i);

		     String address = jsonItem.getString("url");
		     String description = new StringBuilder(this)
		           .append(jsonItem.getInt("term_count"))
		           .append(jsonItem.getLong("created_date"))
		           .append(jsonItem.getString("description"))
		           .append(jsonItem.getString("created_by"));

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
		return null;
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