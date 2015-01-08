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

package com.screenshare.renard314.screenshare;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.SurfaceTexture;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;


import com.screenshare.renard314.screensharelib.DecoderAsyncTask;
import com.screenshare.renard314.screensharelib.MediaCodecFactory;
import com.screenshare.renard314.screensharelib.VideoChunk;

import java.io.IOException;


public class MainActivity extends Activity implements EncoderAsyncTask.MediaCodecListener, TextureView.SurfaceTextureListener {

	private static final short GET_MEDIA_PROJECTION_CODE = 986;

	// Encoder parameters.  We use the same width/height as the virtual display.
	private static final String LOG_TAG = MainActivity.class.getSimpleName();
	private static final String PREF_NAME = MainActivity.class.getName();
	private static final String RECEIVER_IP_KEY = "RECEIVER_IP";
	private EncoderAsyncTask mEncoderAsyncTask;
	//private DecoderAsyncTask mDecoderAsyncTask;
	private SenderAsyncTask mSenderAsyncTask;
	private TextView mStatsTextView;
	private TextureView mTextureView;
	private Button mStartButton;
	private EditText mReceiverIpEditText;
	private MediaCodecFactory mMediaCodecFactory;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);

		final int height = metrics.heightPixels / 3;
		final int width = metrics.widthPixels / 3;
		mMediaCodecFactory = new MediaCodecFactory(width, height);

		setContentView(R.layout.activity_main);
		mTextureView = new TextureView(this);
		mTextureView.setLayoutParams(new RelativeLayout.LayoutParams(width, height));
		mTextureView.setSurfaceTextureListener(this);
		final RelativeLayout previewContainer = (RelativeLayout) findViewById(R.id.previewContainer);
		previewContainer.addView(mTextureView);
		mStartButton = (Button) findViewById(R.id.start_screensharing_button);
		mStatsTextView = (TextView)findViewById(R.id.encoder_stats_textView);
		mReceiverIpEditText = (EditText) findViewById(R.id.textView_ip);
		final SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, 0);
		String ip=sharedPreferences.getString(RECEIVER_IP_KEY,null);
		mReceiverIpEditText.setText(ip);

		mStartButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(mEncoderAsyncTask==null){
					@SuppressWarnings("ResourceType") MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
					mStartButton.setText(getString(R.string.stop));
					startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(),GET_MEDIA_PROJECTION_CODE);
				} else {
					mStartButton.setEnabled(false);
					mStartButton.setText(getString(R.string.stopping));
					mEncoderAsyncTask.cancel(true);
					mEncoderAsyncTask=null;
					//mDecoderAsyncTask.cancel(true);
					//mDecoderAsyncTask = null;

				}
			}
		});
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		String ip = mReceiverIpEditText.getText().toString();
		final SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, 0);
		final SharedPreferences.Editor edit = sharedPreferences.edit();
		edit.putString(RECEIVER_IP_KEY,ip);
		edit.commit();

		if(resultCode == RESULT_OK && requestCode==GET_MEDIA_PROJECTION_CODE){
			try {
				@SuppressWarnings("ResourceType") MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
				final MediaProjection mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data);
				mEncoderAsyncTask = new EncoderAsyncTask(this, mediaProjection, mMediaCodecFactory);
				mEncoderAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
				mSenderAsyncTask = new SenderAsyncTask(ip);
				mSenderAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			} catch (IOException e) {
				mStartButton.setEnabled(false);
				mStartButton.setText(getString(R.string.mediacodec_error));
				e.printStackTrace();
			}

		}
	}

	@Override
	protected void onDestroy() {
		if(mEncoderAsyncTask!=null) {
			mEncoderAsyncTask.cancel(true);
			mEncoderAsyncTask=null;
		}
//        if(mDecoderAsyncTask!=null){
//            mDecoderAsyncTask.cancel(true);
//            mDecoderAsyncTask = null;
//        }
		if(mSenderAsyncTask!=null){
			mSenderAsyncTask.cancel(true);
			mSenderAsyncTask = null;
		}
		super.onDestroy();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		//noinspection SimplifiableIfStatement
		if (id == R.id.action_settings) {
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onData(VideoChunk chunk) {
		//Log.i(LOG_TAG, "new frame: " + chunk.getData().length + " bytes");
		//mStatsTextView.setText("size " + chunk.getData().length);
//		if(mDecoderAsyncTask!=null) {
//			mDecoderAsyncTask.addChunk(chunk);
//		}
		if(mSenderAsyncTask!=null){
			mSenderAsyncTask.addChunk(chunk);
		}


	}



	@Override
	public void onEnd() {
		Log.i(LOG_TAG, "encoder stopped");
		mStartButton.setText(getString(R.string.start_capturing));
		mStartButton.setEnabled(true);


	}

	@Override
	public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
		Log.i(LOG_TAG, "onSurfaceTextureAvailable ("+width + "/"+height+")");
//		try {
//			final Surface surface = new Surface(surfaceTexture);
//			mDecoderAsyncTask = new DecoderAsyncTask(mMediaCodecFactory, surface,null);
//			mDecoderAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
//
//		} catch (IOException e) {
//			e.printStackTrace();
//		}

	}

	@Override
	public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
		Log.i(LOG_TAG, "onSurfaceTextureSizeChanged ("+width + "/"+height+")");

	}

	@Override
	public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
		Log.i(LOG_TAG, "onSurfaceTextureDestroyed)");
		return false;
	}

	@Override
	public void onSurfaceTextureUpdated(SurfaceTexture surface) {
		Log.i(LOG_TAG, "onSurfaceTextureUpdated)");
	}
}
