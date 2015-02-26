/*
 * Copyright (C) 2014 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.chromecastsample;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.support.v7.media.MediaRouter.RouteInfo;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import com.google.android.gms.cast.ApplicationMetadata;
import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.Cast.ApplicationConnectionResult;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.RemoteMediaPlayer;
import com.google.android.gms.cast.RemoteMediaPlayer.MediaChannelResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

/**
 * Sample activity to demonstrate discovering Cast devices using the Media Router. The discovered devices are displayed as a list.
 * 
 * @see http://developer.android.com/guide/topics/media/mediarouter.html
 */
public class DeviceDiscoveryActivity extends Activity {

	static final String TAG = DeviceDiscoveryActivity.class
			.getSimpleName();
	private static final String APP_ID = CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID;
	private GoogleApiClient mApiClient;
	private MediaRouter mMediaRouter;
	private MediaRouteSelector mMediaRouteSelector;
	private MediaRouter.Callback mMediaRouterCallback;
	private CastDevice mSelectedDevice;
	private Cast.Listener mCastListener;
	private RemoteMediaPlayer mRemoteMediaPlayer;
	private ArrayList<String> mRouteNames = new ArrayList<String>();
	private ConnectionCallbacks mConnectionCallbacks;
	private ConnectionFailedListener mConnectionFailedListener;
	Context context;
	final ArrayList<MediaRouter.RouteInfo> mRouteInfos = new ArrayList<MediaRouter.RouteInfo>();
	Button btnPlay;
	private WebSettings webSettings;
	private String LastPart;
	String convertedUrl;
	ListView listview;
	private ArrayAdapter<String> mAdapter;

	@Override
	public void onBackPressed() {
		// TODO Auto-generated method stub
		super.onBackPressed();
		this.finish();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// Create a list control for displaying the names of the
		// devices discovered by the MediaRouter
		btnPlay = (Button) findViewById(R.id.button1);
		btnPlay.setEnabled(false);
		context = DeviceDiscoveryActivity.this;
		listview = (ListView) findViewById(R.id.list);
		mCastListener = new CastListener();
		mRemoteMediaPlayer = new RemoteMediaPlayer();
		mConnectionCallbacks = new ConnectionCallbacks();
		mConnectionFailedListener = new ConnectionFailedListener();

		mRouteNames = new ArrayList<String>();
		mAdapter = new ArrayAdapter<String>(DeviceDiscoveryActivity.this,
				android.R.layout.simple_list_item_1, mRouteNames);
		listview.setAdapter(mAdapter);

		mMediaRouter = MediaRouter.getInstance(getApplicationContext());
		// Create a MediaRouteSelector for the type of routes your app supports
		mMediaRouteSelector = new MediaRouteSelector.Builder()
		.addControlCategory(
				CastMediaControlIntent.categoryForCast(APP_ID)).build();
		// Create a MediaRouter callback for discovery events
		mMediaRouterCallback = new MyMediaRouterCallback();
		((Button) findViewById(R.id.btnDisconnect)).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				if (mApiClient != null && mApiClient.isConnected()) {
					mApiClient.disconnect();
					mApiClient = null;

					Cast.CastOptions apiOptions = Cast.CastOptions.builder(
							mSelectedDevice, mCastListener).build();

					mApiClient = new GoogleApiClient.Builder(context)
					.addApi(Cast.API, apiOptions)
					.addConnectionCallbacks(mConnectionCallbacks)
					.addOnConnectionFailedListener(mConnectionFailedListener)
					.build();
					mApiClient.connect();

				}

			}
		});
		listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, final View view,
					int position, long id) {
				Log.d(TAG, "onItemClick: position=" + position);

				MediaRouter.RouteInfo info = mRouteInfos.get(position);
				mMediaRouter.selectRoute(info);
			}

		});
		// findViewById(R.id.button2).setOnClickListener(new OnClickListener() {
		//
		// @Override
		// public void onClick(View v) {
		// convertUrl("http://music.hdi.cdn.ril.com/mod/_definst_/mp4:hdindiamusic/audiofiles/8/7062/8_7062_7_32.mp4/playlist.m3u8?uid=tarandeep_singh&action=low&sid=1423648281-1725108897&srv=jiobeats&cid=8_7062_7");
		//
		// }
		// });
		btnPlay.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				String s = convertedUrl;
				Play(btnPlay,
						"");

			}
		});

	}

	public static String getDomainName(String url) throws URISyntaxException {
		URI uri = new URI(url);
		String domain = uri.getHost();
		return domain.startsWith("www.") ? domain.substring(4) : domain;
	}

	@Override
	protected void onResume() {
		super.onResume();

		// Add the callback to start device discovery
		mMediaRouter.addCallback(mMediaRouteSelector, mMediaRouterCallback,
				MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN);
	}

	@Override
	protected void onPause() {
		// Remove the callback to stop device discovery
		mMediaRouter.removeCallback(mMediaRouterCallback);
		super.onPause();
	}

	private class CastListener extends Cast.Listener {
		@Override
		public void onApplicationDisconnected(int statusCode) {
			Log.e(TAG, "Cast.Listener.onApplicationDisconnected: " + statusCode);
			try {
				Cast.CastApi.removeMessageReceivedCallbacks(mApiClient,
						mRemoteMediaPlayer.getNamespace());
			} catch (IOException e) {
				Log.e(TAG, "Exception while launching application", e);
			}
		}
	}

	public class ConnectionCallbacks implements
	GoogleApiClient.ConnectionCallbacks {
		@Override
		public void onConnectionSuspended(int cause) {
			Log.d(TAG, "ConnectionCallbacks.onConnectionSuspended");
		}

		@Override
		public void onConnected(Bundle connectionHint) {
			Log.d(TAG, "ConnectionCallbacks.onConnected");
			Cast.CastApi.launchApplication(mApiClient, APP_ID)
			.setResultCallback(new ConnectionResultCallback());
		}
	}

	private final class ConnectionResultCallback implements
	ResultCallback<ApplicationConnectionResult> {
		@Override
		public void onResult(ApplicationConnectionResult result) {
			Status status = result.getStatus();
			ApplicationMetadata appMetaData = result.getApplicationMetadata();

			if (status.isSuccess()) {
				Log.e(TAG, "ConnectionResultCallback: " + appMetaData.getName());
				btnPlay.setEnabled(true);
				try {
					Cast.CastApi.setMessageReceivedCallbacks(mApiClient,
							mRemoteMediaPlayer.getNamespace(),
							mRemoteMediaPlayer);
				} catch (IOException e) {
					Log.w(TAG, "Exception while launching application", e);
				}
			} else {
				btnPlay.setEnabled(false);
				Log.e(TAG,
						"ConnectionResultCallback. Unable to launch the game. statusCode: "
								+ status.getStatusCode());
			}
		}
	}

	public class ConnectionFailedListener implements
	GoogleApiClient.OnConnectionFailedListener {
		@Override
		public void onConnectionFailed(ConnectionResult result) {
			Log.d(TAG, "ConnectionFailedListener.onConnectionFailed");
			// setSelectedDevice(null);
		}
	}

	private class MyMediaRouterCallback extends MediaRouter.Callback {

		@Override
		public void onRouteAdded(MediaRouter router, MediaRouter.RouteInfo info) {
			Log.d(TAG, "onRouteAdded: info=" + info);

			// Add route to list of discovered routes
			synchronized (this) {
				mRouteInfos.add(info);
				mRouteNames.add(info.getName() + " (" + info.getDescription()
						+ ")");
				mAdapter.notifyDataSetChanged();
			}
		}

		@Override
		public void onRouteRemoved(MediaRouter router,
				MediaRouter.RouteInfo info) {
			Log.d(TAG, "onRouteRemoved: info=" + info);

			// Remove route from list of routes
			synchronized (this) {
				for (int i = 0; i < mRouteInfos.size(); i++) {
					MediaRouter.RouteInfo routeInfo = mRouteInfos.get(i);
					if (routeInfo.equals(info)) {
						mRouteInfos.remove(i);
						mRouteNames.remove(i);
						mAdapter.notifyDataSetChanged();
						return;
					}
				}
			}
		}

		@Override
		public void onRouteSelected(MediaRouter router, RouteInfo info) {
			Log.d(TAG, "onRouteSelected: info=" + info);
			mSelectedDevice = CastDevice.getFromBundle(info.getExtras());
			setSelectedDevice(mSelectedDevice);
		}

		@Override
		public void onRouteUnselected(MediaRouter router, RouteInfo info) {
			Log.d(TAG, "onRouteUnselected: info=" + info);
			mSelectedDevice = null;
		}

		public void setSelectedDevice(CastDevice device) {
			Log.d(TAG, "setSelectedDevice: " + device);
			mSelectedDevice = device;

			if (mSelectedDevice != null) {
				try {
					disconnectApiClient();
					connectApiClient();
				} catch (IllegalStateException e) {
					Log.w(TAG, "Exception while connecting API client", e);
					disconnectApiClient();
				}
			} else {
				if (mApiClient != null) {

					disconnectApiClient();
				}
				btnPlay.setEnabled(false);
				mMediaRouter.selectRoute(mMediaRouter.getDefaultRoute());
			}
		}

		private void connectApiClient() {
			Cast.CastOptions apiOptions = Cast.CastOptions.builder(
					mSelectedDevice, mCastListener).build();

			mApiClient = new GoogleApiClient.Builder(context)
			.addApi(Cast.API, apiOptions)
			.addConnectionCallbacks(mConnectionCallbacks)
			.addOnConnectionFailedListener(mConnectionFailedListener)
			.build();
			mApiClient.connect();
		}

		private void disconnectApiClient() {
			if (mApiClient != null && mApiClient.isConnected()) {
				mApiClient.disconnect();
				mApiClient = null;
			}

		}

	}

	private void convertUrl(String url) {
		// String getDomainName;
		// String parts[] = url.split(".com");
		// LastPart = parts[1];
		// try {
		// // getDomainName = getDomainName(url);
		// // String newUrl = "http://" + getDomainName;
		// // Log.d("URL", " :Domain " + getDomainName);
		// // webSettings = webview.getSettings();
		// // webSettings.setJavaScriptEnabled(true);
		// // webSettings.setBuiltInZoomControls(false);
		// // webview.setWebViewClient(new aboutWebViewClient());
		// // webview.loadUrl(newUrl);
		// } catch (URISyntaxException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }

	}

	public class aboutWebViewClient extends WebViewClient {

		@Override
		public void onPageStarted(WebView view, String url, Bitmap favicon) {

			super.onPageStarted(view, url, favicon);

		}

		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {

			return super.shouldOverrideUrlLoading(view, url);

		}

		@Override
		public void onPageFinished(WebView view, String url) {

			super.onPageFinished(view, url);
			convertedUrl = url.concat(LastPart);
			Log.d("URL", "Loaded Url:" + url);
		}
	}

	private void Play(Button play, String file) {
		Log.e("Play **********", mApiClient.isConnected() + " ++++++++Status");
		if (play.getText().toString().equalsIgnoreCase("PAUSE")) {
			if (mApiClient != null) {
				if (mApiClient.isConnected()) {
					if (mRemoteMediaPlayer != null) {
						mRemoteMediaPlayer.pause(mApiClient);

					}
				}
			}
			play.setText("PLAY");
		} else if (play.getText().toString().equalsIgnoreCase("PLAY")) {
			if (mApiClient != null) {
				if (mApiClient.isConnected()) {
					if (mRemoteMediaPlayer != null) {
						mRemoteMediaPlayer.play(mApiClient);

					}
				}
			}
			play.setText("PAUSE");
		} else {
			try {
				Log.e("Play Testing", "mRemoteMediaPlayer is play");
				play.setText("PAUSE");
				MediaMetadata mediaMetadata = new MediaMetadata(
						MediaMetadata.MEDIA_TYPE_MOVIE);
				mediaMetadata.putString(MediaMetadata.KEY_TITLE, "My video");
				MediaInfo mediaInfo = new MediaInfo.Builder(file)
				.setContentType("application/x-mpegurl")
				.setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
				.setMetadata(mediaMetadata).build();
				mRemoteMediaPlayer
				.load(mApiClient, mediaInfo, true)
				.setResultCallback(
						new ResultCallback<RemoteMediaPlayer.MediaChannelResult>() {

							@Override
							public void onResult(
									MediaChannelResult result) {
								if (result.getStatus().isSuccess()) {
									Log.d("", result.getStatus() + "");
								}
							}
						});
			} catch (IllegalStateException e) {
				Log.e(TAG, e.getMessage());
			} catch (Exception e) {
				Log.e(TAG, e.getMessage());
			}
		}

	}
}
