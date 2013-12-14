package org.liberty.android.fantastischmemo.downloader.quizlet;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.liberty.android.fantastischmemo.downloader.AbstractDownloaderFragment;
import org.liberty.android.fantastischmemo.downloader.DownloadItem;

import roboguice.util.Ln;
import android.os.Bundle;

public class CardsetsListFragment extends AbstractDownloaderFragment {
	
    public static final String EXTRA_AUTH_TOKEN = "authToken";
    
    public static final String EXTRA_USER_ID = "userId";
    
    private String authToken;
    
    private String userId;
	
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