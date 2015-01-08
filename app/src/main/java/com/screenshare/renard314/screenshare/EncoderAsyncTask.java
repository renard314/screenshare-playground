
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

import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.projection.MediaProjection;
import android.os.AsyncTask;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;

import com.screenshare.renard314.screensharelib.MediaCodecFactory;
import com.screenshare.renard314.screensharelib.VideoChunk;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by renard on 17/12/14.
 */
public class EncoderAsyncTask extends AsyncTask<Void,VideoChunk,Void> {

	private static final String LOG_TAG = EncoderAsyncTask.class.getSimpleName();
	private final MediaCodecListener mListener;
	private final VirtualDisplay mVirtualDisplay;

	interface MediaCodecListener {
		void onData(VideoChunk info);
		void onEnd();
	}

	private MediaCodec mEncoder;


	EncoderAsyncTask(MediaCodecListener listener, MediaProjection projection, MediaCodecFactory factory) throws IOException {
		mListener = listener;
		mEncoder = 	factory.createVideoEncoder();
		Surface surface = mEncoder.createInputSurface();
		mEncoder.start();

		mVirtualDisplay = projection.createVirtualDisplay(LOG_TAG, factory.getWidth(), factory.getHeight(), DisplayMetrics.DENSITY_MEDIUM, 0, surface, null, null);
	}


	@Override
	protected Void doInBackground(Void... params) {
		Log.i(LOG_TAG, "starting encoder");
        Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

		final int TIMEOUT_USEC = 10000;
		MediaCodec.BufferInfo info=new MediaCodec.BufferInfo();
		while(!isCancelled()){
			int outputBufferIndex = mEncoder.dequeueOutputBuffer(info, TIMEOUT_USEC);
			if (outputBufferIndex >= 0) {
				ByteBuffer outputBuffer = mEncoder.getOutputBuffer(outputBufferIndex);
				if (info.size != 0) {
					// Adjust the ByteBuffer values to match BufferInfo.
					outputBuffer.position(info.offset);
					outputBuffer.limit(info.offset + info.size);
					VideoChunk chunk = new VideoChunk(outputBuffer,info.flags, info.presentationTimeUs);
					publishProgress(chunk);
				}
				mEncoder.releaseOutputBuffer(outputBufferIndex, false);
			}
		}
		Log.i(LOG_TAG, "got cancel signal");
		mVirtualDisplay.release();

//		//drain encoder of last frames
//		boolean encoderDrained = false;
//		while(!encoderDrained){
//			int outputBufferIndex = mEncoder.dequeueOutputBuffer(info, TIMEOUT_USEC);
//			if(outputBufferIndex!=MediaCodec.INFO_TRY_AGAIN_LATER){
//				handleEncoderOutput(info, outputBufferIndex);
//			} else {
//				encoderDrained = true;
//			}
//		}

		mEncoder.stop();
		mEncoder.release();
		return null;
	}


	@Override
	protected void onPostExecute(Void aVoid) {
		mListener.onEnd();
	}

	@Override
	protected void onCancelled() {
		mListener.onEnd();
	}

	@Override
	protected void onProgressUpdate(VideoChunk... values) {
		mListener.onData(values[0]);
	}
}
