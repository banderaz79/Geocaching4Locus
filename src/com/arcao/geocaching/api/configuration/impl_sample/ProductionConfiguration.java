package com.arcao.geocaching.api.configuration.impl_sample;

import com.arcao.geocaching.api.configuration.OAuthGeocachingApiConfiguration;

public class ProductionConfiguration implements OAuthGeocachingApiConfiguration {
	private static final String SERVICE_URL = "https://api.groundspeak.com/LiveV6/geocaching.svc";
	private static final String OAUTH_URL = "https://www.geocaching.com/oauth/mobileoauth.ashx";

	private static final String CONSUMER_KEY = "YOUR_OAUTH_KEY";
	private static final String CONSUMER_SECRET = "YOUR_OAUTH_SECRET";

	@Override
	public String getApiServiceEntryPointUrl() {
		return SERVICE_URL;
	}

	@Override
	public String getConsumerKey() {
		return CONSUMER_KEY;
	}

	@Override
	public String getConsumerSecret() {
		return CONSUMER_SECRET;
	}

	@Override
	public String getOAuthRequestUrl() {
		return OAUTH_URL;
	}

	@Override
	public String getOAuthAuthorizeUrl() {
		return OAUTH_URL;
	}

	@Override
	public String getOAuthAccessUrl() {
		return OAUTH_URL;
	}
}
