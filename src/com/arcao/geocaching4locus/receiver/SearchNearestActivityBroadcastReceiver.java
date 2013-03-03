package com.arcao.geocaching4locus.receiver;

import java.lang.ref.WeakReference;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.arcao.geocaching4locus.ErrorActivity;
import com.arcao.geocaching4locus.R;
import com.arcao.geocaching4locus.SearchNearestActivity;
import com.arcao.geocaching4locus.fragment.AbstractDialogFragment;
import com.arcao.geocaching4locus.fragment.AbstractDialogFragment.CancellableDialog;
import com.arcao.geocaching4locus.fragment.DownloadProgressDialogFragment;
import com.arcao.geocaching4locus.service.SearchGeocacheService;

public class SearchNearestActivityBroadcastReceiver extends BroadcastReceiver implements CancellableDialog {
	protected WeakReference<SearchNearestActivity> activityRef;
	protected DownloadProgressDialogFragment pd;
	
	public SearchNearestActivityBroadcastReceiver(SearchNearestActivity activity) {
		activityRef = new WeakReference<SearchNearestActivity>(activity);
	}
	
	public void register(Context ctx) {
		IntentFilter filter = new IntentFilter();		
		filter.addAction(SearchGeocacheService.ACTION_PROGRESS_UPDATE);
		filter.addAction(SearchGeocacheService.ACTION_PROGRESS_COMPLETE);
		filter.addAction(ErrorActivity.ACTION_ERROR);
		
		LocalBroadcastManager.getInstance(ctx).registerReceiver(this, filter);
		Log.i(getClass().getName(), "Receiver registred.");
	}
	
	public void unregister(Context ctx) {
		LocalBroadcastManager.getInstance(ctx).unregisterReceiver(this);
		
		if (pd != null)
			pd.dismiss();
	}
	
	@Override
	public void onReceive(Context context, final Intent intent) {
		SearchNearestActivity activity = activityRef.get();
		if (activity == null)
			return;
		
		if (SearchGeocacheService.ACTION_PROGRESS_UPDATE.equals(intent.getAction())) {
			if (pd == null) {
				pd = DownloadProgressDialogFragment.newInstance(R.string.downloading, intent.getIntExtra(SearchGeocacheService.PARAM_COUNT, 1), intent.getIntExtra(SearchGeocacheService.PARAM_CURRENT, 0));
				pd.setOnCancelListener(this);
			} else {
				pd.setProgress(intent.getIntExtra(SearchGeocacheService.PARAM_CURRENT, 0));				
			}
			
			if (!pd.isShowing())
				pd.show(activity.getSupportFragmentManager(), DownloadProgressDialogFragment.TAG);
		} else if (SearchGeocacheService.ACTION_PROGRESS_COMPLETE.equals(intent.getAction())) {
			if (pd != null && pd.isShowing())
				pd.dismiss();
			
			if (intent.getIntExtra(SearchGeocacheService.PARAM_COUNT, 0) != 0 && !activity.isFinishing()) {
				activity.finish();
			}
		} else if (ErrorActivity.ACTION_ERROR.equals(intent.getAction())) {
			if (pd != null && pd.isShowing())
				pd.dismiss();

			Intent errorIntent = new Intent(intent);
			errorIntent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);			
			activity.startActivity(errorIntent);
		}
	}
	
	@Override
	public void onCancel(AbstractDialogFragment dialogFragment) {
		SearchNearestActivity activity = activityRef.get();
		if (activity == null)
			return;
		
		activity.stopService(new Intent(activity, SearchGeocacheService.class));
	}
}
