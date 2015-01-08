
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

package com.screenshare.renard314.screensharelib;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Pair;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by renard on 18/12/14.
 */
public class DecoderAsyncTask extends AsyncTask<Void, Pair<Integer,Integer>, Void> {

	private static final String LOG_TAG = DecoderAsyncTask.class.getSimpleName();
	private final OnVideoSizeChangedListener mListener;
	MediaCodec mDecoder;
    LinkedBlockingQueue<VideoChunk> mVideoChunks = new LinkedBlockingQueue<VideoChunk>();
    List<Pair<Long,Integer>> mReceivedChunks = new ArrayList<Pair<Long,Integer>>();

	public DecoderAsyncTask(MediaCodecFactory factory, Surface surface, OnVideoSizeChangedListener listener) throws IOException {
		mListener = listener;
		mDecoder = factory.createVideoDecoder(surface);
	}

	public void addChunk(VideoChunk chunk) {
        //mReceivedChunks.add(Pair.create(System.currentTimeMillis(), mVideoChunks.size()+1));
        mVideoChunks.add(chunk);
    }

	@Override
	protected Void doInBackground(Void... params) {
        Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
        mDecoder.start();
		final int TIMEOUT_USEC = 10000;
		MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
		boolean isCancelled = isCancelled();
		int inputBufIndex = -1;
		while (!isCancelled) {
			inputBufIndex = mDecoder.dequeueInputBuffer(TIMEOUT_USEC);
			if (inputBufIndex >= 0) {
				isCancelled = isCancelled();
				if(isCancelled()){
					Log.i(LOG_TAG, "got cancel signal");
					break;
				}
				VideoChunk chunk = null;
				try {
					//Log.d(LOG_TAG,"waiting for data to decode from "+ mVideoChunks.size()+" chunks");
                    Log.i(LOG_TAG,"decoder queue = "+ (mVideoChunks.size()+1));
                    chunk = pickFrame();
					//Log.d(LOG_TAG,"got data");
				} catch (InterruptedException e) {
					e.printStackTrace();
					continue;
				}
				// Copy a chunk of input to the decoder.  The first chunk should have
				// the BUFFER_FLAG_CODEC_CONFIG flag set.
				ByteBuffer inputBuf = mDecoder.getInputBuffer(inputBufIndex);
				inputBuf.clear();
				inputBuf.put(chunk.getData());
				int flags = chunk.getFlags();
				long time = chunk.getTimeUs();
				mDecoder.queueInputBuffer(inputBufIndex, 0, inputBuf.position(), time, flags);
				//Log.d(LOG_TAG, "submitted frame to dec, size=" + inputBuf.position() + " flags=" + flags);
			}
			boolean decoderDone =  false;
			while(!decoderDone){
				int decoderStatus = mDecoder.dequeueOutputBuffer(info, TIMEOUT_USEC);
				if (decoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
					// no output available yet
					decoderDone = true;
				}else if (decoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
					//decoderOutputBuffers = decoder.getOutputBuffers();
					Log.d(LOG_TAG, "decoder output buffers changed (we don't care)");
				} else if (decoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
					// expected before first buffer of data
					MediaFormat newFormat = mDecoder.getOutputFormat();
					int width = newFormat.getInteger(MediaFormat.KEY_WIDTH);
					int height = newFormat.getInteger(MediaFormat.KEY_HEIGHT);
					publishProgress(Pair.create(width,height));
					Log.d(LOG_TAG, "decoder output format changed: " + newFormat);
				} else if (decoderStatus < 0) {
					throw new IllegalStateException("unexpected result from decoder.dequeueOutputBuffer: "+decoderStatus);
				} else { // decoderStatus >= 0
					//Log.d(LOG_TAG, "surface decoder given buffer " + decoderStatus + " (size=" + info.size + ")");
					// The ByteBuffers are null references, but we still get a nonzero
					// size for the decoded data.
					boolean doRender = (info.size != 0);
					mDecoder.releaseOutputBuffer(decoderStatus, doRender);
				}

			}
		}
		if(inputBufIndex!=-1) {
			// End of stream -- send empty frame with EOS flag set.
			mDecoder.queueInputBuffer(inputBufIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
			Log.d(LOG_TAG, "sent input EOS (with zero-length frame)");
		}


		mDecoder.stop();

		return null;
	}

    private VideoChunk pickFrame() throws InterruptedException {
        VideoChunk chunk = mVideoChunks.take();
        return chunk;
        //find newest keyframe. discard all frames before that that are no config frames
        //if no keyframe is present just take oldest frame
//        synchronized (mVideoChunks){
//            while(mVideoChunks.size()>0){
//                VideoChunk chunk = mVideoChunks.takeLast();
//                if(chunk.i)
//
//            }
//            for(int i=mVideoChunks.size()-1;i>=0;i--){
//                VideoChunk chunk = mVideoChunks.element()
//            }
//            for(VideoChunk chunk: mVideoChunks){
//                if(chunk.isKeyFrame())
//            }
//        }
//        try {
//            return mVideoChunks.takeFirst();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//            return null;
//        }
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        if(mListener!=null){
            mListener.onVideoEnded(mReceivedChunks);
        }
    }

    @Override
	protected void onProgressUpdate(Pair<Integer, Integer>... values) {
		super.onProgressUpdate(values);
		if(mListener!=null){
			mListener.onVideoSizeChanged(values[0].first, values[0].second);
		}
	}
}
