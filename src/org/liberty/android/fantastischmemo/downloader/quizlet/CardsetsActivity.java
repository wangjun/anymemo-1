package org.liberty.android.fantastischmemo.downloader.quizlet;

import roboguice.util.Ln;


public class CardsetsActivity extends QuizletAccountActivity {
	
	@Override
    protected void onAuthenticated(final String[] authTokens) {
		Ln.i("The oauth taken is " + authTokens[0]);
    }
	
}