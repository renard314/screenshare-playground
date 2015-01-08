
/*
 * Copyright (c) 2015 Renard Wellnitz.
 *
 *  This file is part of ScreenShare.
 *
 *     Foobar is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Foobar is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.screenshare.renard314.decoder;

import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.format.Formatter;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.TextureView;
import android.widget.TextView;


import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.screenshare.renard314.screensharelib.DecoderAsyncTask;
import com.screenshare.renard314.screensharelib.MediaCodecFactory;
import com.screenshare.renard314.screensharelib.OnVideoSizeChangedListener;
import com.screenshare.renard314.screensharelib.VideoChunk;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends Activity implements ReceiverAsyncTask.ReceiverListener, TextureView.SurfaceTextureListener, OnVideoSizeChangedListener {

	private static final String LOG_TAG = MainActivity.class.getSimpleName();
	private ReceiverAsyncTask mTask;
	private final MediaCodecFactory mMediaCodecFactory = new MediaCodecFactory(0,0);
	private DecoderAsyncTask mDecoderAsyncTask;
	TextureView mTextureView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
		String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
		TextView text = (TextView) findViewById(R.id.textView);
		mTextureView = (TextureView)findViewById(R.id.textureView);
		mTextureView.setSurfaceTextureListener(this);
		mTextureView.setOpaque(false);

		text.setText(ip);
		mTask = new ReceiverAsyncTask(this);
		mTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mTask.cancel(true);
	}


	@Override
	public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
		Log.i(LOG_TAG, "onSurfaceTextureAvailable (" + width + "/" + height + ")");
		try {
			final Surface surface = new Surface(surfaceTexture);
			mDecoderAsyncTask = new DecoderAsyncTask(mMediaCodecFactory, surface, this);
			mDecoderAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	@Override
	public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

	}

	@Override
	public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
		return false;
	}

	@Override
	public void onSurfaceTextureUpdated(SurfaceTexture surface) {
	}

	@Override
	public void onVideoChunk(VideoChunk chunk) {

        if(mDecoderAsyncTask!=null) {
			mDecoderAsyncTask.addChunk(chunk);
		}
	}

    @Override
    public void onConnectionLost() {
        if(!isDestroyed()){
            mTask = new ReceiverAsyncTask(this);
            mTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }

    }

    @Override
	public void onVideoSizeChanged(int videoWidth, int videoHeight) {
		// Get the SurfaceView layout parameters
		android.view.ViewGroup.LayoutParams lp = mTextureView.getLayoutParams();
		float videoProportion = (float) videoWidth / (float) videoHeight;
		// Get the width of the screen

		int screenWidth = mTextureView.getWidth();
		int screenHeight = mTextureView.getHeight();
		float screenProportion = (float) screenWidth / (float) screenHeight;
		if (videoProportion > screenProportion) {
			//video is wider than our screen
			lp.width = screenWidth;
			lp.height = (int) ((float) screenWidth / videoProportion);
		} else {
			lp.width = (int) (videoProportion * (float) screenHeight);
			lp.height = screenHeight;
		}
		// Commit the layout parameters
		mTextureView.setLayoutParams(lp);

	}

    @Override
    public void onVideoEnded(List<Pair<Long, Integer>> chunksTimeSeries) {

    }
}
