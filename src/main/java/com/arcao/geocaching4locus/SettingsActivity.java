package com.arcao.geocaching4locus;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.MenuItem;
import com.arcao.geocaching4locus.fragment.preference.HeaderPreferenceFragment;

public class SettingsActivity extends ActionBarActivity {
	private static final String TAG = SettingsActivity.class.getName();
	public static final String PARAM_FRAGMENT = "fragment";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		try {
			String fragmentName = getIntent().getStringExtra(PARAM_FRAGMENT);
			if (fragmentName != null) {
				// Display the fragment as the main content.
				getSupportFragmentManager().beginTransaction().replace(android.R.id.content, (Fragment) Class.forName(fragmentName).newInstance()).commit();
				return;
			}
		} catch (Throwable e) {
			Log.e(TAG, e.getMessage(), e);
		}

		getSupportFragmentManager().beginTransaction().replace(android.R.id.content, new HeaderPreferenceFragment()).commit();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			// Respond to the action bar's Up/Home button
			case android.R.id.home:
				finish();
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	public static Intent createIntent(Context context) {
		return new Intent(context, SettingsActivity.class);
	}


	public static Intent createIntent(Context context, Class<?> preferenceFragment) {
		return createIntent(context).putExtra(SettingsActivity.PARAM_FRAGMENT, preferenceFragment.getName());
	}
}