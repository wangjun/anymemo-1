package org.liberty.android.fantastischmemo.downloader.quizlet;

import java.util.ArrayList;
import java.util.List;

import org.liberty.android.fantastischmemo.downloader.AbstractDownloaderFragment;
import org.liberty.android.fantastischmemo.downloader.DownloadItem;
import org.liberty.android.fantastischmemo.downloader.DownloadItem.ItemType;

public class CardsetsListFragment extends AbstractDownloaderFragment {
	
    public static final String EXTRA_AUTH_TOKEN = "authToken";
    
    public static final String EXTRA_USER_ID = "userId";
	
	@Override
	protected List<DownloadItem> initialRetrieve() throws Exception {
		DownloadItem item1 = new DownloadItem(ItemType.Database, "hello", "For undergraduate students", "here");
		DownloadItem item2 = new DownloadItem(ItemType.Database, "hi", "For undergraduate students", "here");
        List<DownloadItem> downloadItemList = new ArrayList<DownloadItem>(50);
        downloadItemList.add(item1);
        downloadItemList.add(item2);
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