package com.arcao.geocaching4locus.error.handler;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.text.format.DateFormat;

import com.arcao.geocaching.api.StatusCode;
import com.arcao.geocaching.api.exception.InvalidCredentialsException;
import com.arcao.geocaching.api.exception.InvalidResponseException;
import com.arcao.geocaching.api.exception.InvalidSessionException;
import com.arcao.geocaching.api.exception.LiveGeocachingApiException;
import com.arcao.geocaching.api.exception.NetworkException;
import com.arcao.geocaching4locus.App;
import com.arcao.geocaching4locus.R;
import com.arcao.geocaching4locus.authentication.util.AccountManager;
import com.arcao.geocaching4locus.authentication.util.AccountRestrictions;
import com.arcao.geocaching4locus.base.constants.AppConstants;
import com.arcao.geocaching4locus.base.util.HtmlUtil;
import com.arcao.geocaching4locus.base.util.ResourcesUtil;
import com.arcao.geocaching4locus.error.ErrorActivity;
import com.arcao.geocaching4locus.error.exception.CacheNotFoundException;
import com.arcao.geocaching4locus.error.exception.IntendedException;
import com.arcao.geocaching4locus.error.exception.LocusMapRuntimeException;
import com.arcao.geocaching4locus.error.exception.NoResultFoundException;
import com.arcao.geocaching4locus.settings.SettingsActivity;
import com.arcao.geocaching4locus.settings.fragment.AccountsPreferenceFragment;
import com.arcao.wherigoservice.api.WherigoServiceException;
import com.github.scribejava.core.exceptions.OAuthException;

import org.apache.commons.lang3.StringUtils;
import org.oshkimaadziig.george.androidutils.SpanFormatter;

import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.InterruptedIOException;
import java.net.ConnectException;
import java.net.UnknownHostException;

import timber.log.Timber;

public class ExceptionHandler {
    private final Context context;

    public ExceptionHandler(@NonNull Context context) {
        this.context = context;
    }

    @NonNull
    public Intent handle(@NonNull Throwable t) {
        Timber.e(t);

        Intent positiveAction = null;
        CharSequence baseMessage = "%s";

        if (t instanceof IntendedException) {
            positiveAction = ((IntendedException) t).getIntent();
            t = t.getCause();
            baseMessage = SpanFormatter.format(HtmlUtil.fromHtml("%%s<br /><br />%s"), context.getText(R.string.error_continue_locus_map));
        }

        // special handling for some API exceptions
        if (t instanceof LiveGeocachingApiException) {
            Intent intent = handleLiveGeocachingApiExceptions((LiveGeocachingApiException) t, positiveAction, baseMessage);
            if (intent != null)
                return intent;
        }

        ErrorActivity.IntentBuilder builder = new ErrorActivity.IntentBuilder(context);
        if (positiveAction != null) {
            builder
                    .positiveAction(positiveAction)
                    .positiveButtonText(R.string.button_yes)
                    .negativeButtonText(R.string.button_no);
        }

        if (t instanceof InvalidCredentialsException) {
            return builder
                    .message(R.string.error_no_account)
                    .positiveAction(
                            SettingsActivity.createIntent(context, AccountsPreferenceFragment.class))
                    .positiveButtonText(R.string.button_ok)
                    .negativeButtonText(null)
                    .build();
        } else if (t instanceof InvalidSessionException ||
                (t instanceof LiveGeocachingApiException && ((LiveGeocachingApiException) t).getStatusCode() == StatusCode.NotAuthorized)) {
            App.get(context).getAccountManager().removeAccount();
            return builder
                    .message(R.string.error_session_expired)
                    .positiveAction(
                            SettingsActivity.createIntent(context, AccountsPreferenceFragment.class))
                    .positiveButtonText(R.string.button_ok)
                    .negativeButtonText(null)
                    .build();
        } else if (t instanceof InvalidResponseException) {
            return builder
                    .message(baseMessage, ResourcesUtil.getText(context, R.string.error_invalid_api_response, t.getMessage()))
                    .exception(t)
                    .build();
        } else if (t instanceof CacheNotFoundException) {
            String[] geocacheCodes = ((CacheNotFoundException) t).getCacheCodes();
            return builder
                    .message(baseMessage,
                            ResourcesUtil.getQuantityText(context,
                                    R.plurals.plural_error_geocache_not_found,
                                    geocacheCodes.length, TextUtils.join(", ", geocacheCodes)
                            )
                    ).build();
        } else if (t instanceof NetworkException ||
                (t instanceof WherigoServiceException && ((WherigoServiceException) t).getCode() == WherigoServiceException.ERROR_CONNECTION_ERROR)) {
            builder.message(baseMessage, context.getText(R.string.error_network_unavailable));

            // Allow sending error report for exceptions that not caused by timeout or unknown host
            Throwable innerT = t.getCause();
            if (innerT != null && !(innerT instanceof InterruptedIOException) && !(innerT instanceof UnknownHostException)
                    && !(innerT instanceof ConnectException) && !(innerT instanceof EOFException) && !isSSLConnectionException(innerT)) {
                builder.exception(t);
            }

            return builder.build();
        } else if (t instanceof NoResultFoundException) {
            return builder
                    .message(R.string.error_no_result)
                    .build();
        } else if (t instanceof LocusMapRuntimeException) {
            t = t.getCause();
            String message = StringUtils.defaultString(t.getMessage());

            return builder
                    .title(R.string.title_locus_map_error)
                    .message(SpanFormatter.format(HtmlUtil.fromHtml("%s<br>Exception: %s"), message, t.getClass().getSimpleName()))
                    .exception(t)
                    .build();
        } else if (t instanceof FileNotFoundException || t.getCause() instanceof FileNotFoundException) {
            return builder
                    .message(R.string.error_no_write_file_permission)
                    .build();
        } else if (t instanceof OAuthException && t.getMessage().equals("oauth_verifier argument was incorrect.")) {
            return builder
                    .message(R.string.error_invalid_authorization_code)
                    .build();
        } else {
            String message = StringUtils.defaultString(t.getMessage());
            return builder
                    .message(baseMessage, SpanFormatter.format(HtmlUtil.fromHtml("%s<br>Exception: %s"), message, t.getClass().getSimpleName()))
                    .exception(t)
                    .build();
        }
    }

    private boolean isSSLConnectionException(Throwable t) {
        String message = t.getMessage();

        return !TextUtils.isEmpty(message) && (message.contains("Connection reset by peer")
                || message.contains("Connection timed out"));
    }

    private Intent handleLiveGeocachingApiExceptions(LiveGeocachingApiException t, Intent positiveAction, CharSequence baseMessage) {
        AccountManager accountManager = App.get(context).getAccountManager();
        boolean premiumMember = accountManager.isPremium();
        AccountRestrictions restrictions = accountManager.getRestrictions();

        ErrorActivity.IntentBuilder builder = new ErrorActivity.IntentBuilder(context);

        switch (t.getStatusCode()) {
            case CacheLimitExceeded: // 118: user reach the quota limit

                int title = premiumMember ? R.string.title_premium_member_warning : R.string.title_basic_member_warning;
                int message = premiumMember ? R.string.error_premium_member_full_geocaching_quota_exceeded : R.string.error_basic_member_full_geocaching_quota_exceeded;

                // apply format on a text
                int cachesPerPeriod = (int) restrictions.getMaxFullGeocacheLimit();
                int period = (int) restrictions.getFullGeocacheLimitPeriod();

                String periodString;
                if (period < AppConstants.SECONDS_PER_MINUTE) {
                    periodString = context.getResources().getQuantityString(R.plurals.plurals_minute, period, period);
                } else {
                    period /= AppConstants.SECONDS_PER_MINUTE;
                    periodString = context.getResources().getQuantityString(R.plurals.plurals_hour, period, period);
                }

                CharSequence renewTime = DateFormat.getTimeFormat(context).format(restrictions.getRenewFullGeocacheLimit());
                CharSequence cacheString = ResourcesUtil.getQuantityText(context, R.plurals.plurals_geocache, cachesPerPeriod, cachesPerPeriod);
                CharSequence errorText = ResourcesUtil.getText(context, message, cacheString, periodString, renewTime);

                builder
                        .title(title)
                        .message(baseMessage, errorText);

                if (positiveAction != null) {
                    builder
                            .positiveAction(positiveAction)
                            .positiveButtonText(R.string.button_yes)
                            .negativeButtonText(R.string.button_no);
                }

                return builder.build();

            case NumberOfCallsExceeded: // 140: too many method calls per minute
                builder
                        .title(R.string.title_method_quota_exceeded)
                        .message(baseMessage, context.getText(R.string.error_method_quota_exceeded));

                if (positiveAction != null) {
                    builder
                            .positiveAction(positiveAction)
                            .positiveButtonText(R.string.button_yes)
                            .negativeButtonText(R.string.button_no);
                }

                return builder.build();

            case PremiumMembershipRequiredForBookmarksExcludeFilter:
            case PremiumMembershipRequiredForDifficultyFilter:
            case PremiumMembershipRequiredForFavoritePointsFilter:
            case PremiumMembershipRequiredForGeocacheContainerSizeFilter:
            case PremiumMembershipRequiredForGeocacheNameFilter:
            case PremiumMembershipRequiredForHiddenByUserFilter:
            case PremiumMembershipRequiredForNotHiddenByUserFilter:
            case PremiumMembershipRequiredForTerrainFilter:
            case PremiumMembershipRequiredForTrackableCountFilter:
                accountManager.updateAccountNextTime();
                return builder
                        .title(R.string.title_premium_member_warning)
                        .message(R.string.error_premium_filter)
                        .build();

            default:
                return null;
        }
    }
}
