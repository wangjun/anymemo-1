package org.liberty.android.fantastischmemo.downloader.quizlet;

import java.util.List;

import javax.inject.Inject;

import org.liberty.android.fantastischmemo.downloader.AbstractDownloaderFragment;
import org.liberty.android.fantastischmemo.downloader.DownloadItem;

import android.os.Bundle;

public class CardsetsListFragment extends AbstractDownloaderFragment {

	public static final String EXTRA_AUTH_TOKEN = "authToken";

	public static final String EXTRA_USER_ID = "userId";

	private String authToken;

	private String userId;
	
	private QuizletDownloadHelper quizletDownloadHelper;

	@Inject
	public void setQuizletDownloadHelper(QuizletDownloadHelper quizletDownloadHelper) {
		this.quizletDownloadHelper = quizletDownloadHelper;
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
		return quizletDownloadHelper.fetchCardsetsList(userId, authToken);
	}

	@Override
	protected String fetchDatabase(DownloadItem di) throws Exception {
		String setId = di.getExtras("id");
		return quizletDownloadHelper.downloadCardset(setId, authToken);
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