package com.screenshare.renard314.screenshare;

import android.media.MediaCodec;
import android.os.AsyncTask;
import android.util.Log;

import com.screenshare.renard314.screensharelib.VideoChunk;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Created by renard on 22/12/14.
 */
public class SenderAsyncTask extends AsyncTask<Void,Void,Void> {

	private static final String LOG_TAG = SenderAsyncTask.class.getSimpleName();
	private final String mIp;
	LinkedBlockingDeque<VideoChunk> mVideoChunks = new LinkedBlockingDeque<VideoChunk>();

	public void addChunk(VideoChunk chunk) {
        synchronized (mVideoChunks) {
//            boolean isKeyFrame = (chunk.getFlags() & MediaCodec.BUFFER_FLAG_KEY_FRAME) == MediaCodec.BUFFER_FLAG_KEY_FRAME;
//            if (isKeyFrame) {
//
//
//                Log.i(LOG_TAG, "adding keyframe");
//                VideoChunk configChunk = null;
//                for(VideoChunk c: mVideoChunks){
//                    boolean containsConfig = (chunk.getFlags() & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == MediaCodec.BUFFER_FLAG_CODEC_CONFIG;
//                    if(containsConfig){
//                        configChunk = c;
//                        break;
//                    }
//                }
//                mVideoChunks.clear();
//                if(configChunk!=null){
//                    mVideoChunks.add(configChunk);
//                }
//            }
            mVideoChunks.addFirst(chunk);
            if(mVideoChunks.size()>2) {
                Log.i(LOG_TAG, "Chunks: " + mVideoChunks.size());
            }
        }
	}

	SenderAsyncTask(String ip){
		mIp = ip;
	}

	@Override
	protected Void doInBackground(Void... params) {
        Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
        int port = 1234;
		java.net.Socket socket=null;
		try {
			socket = new java.net.Socket(mIp,port); // connect to server
		} catch (IOException e) {
			e.printStackTrace();
		}
		DataOutputStream dataOut = null;
		try {
			dataOut = new DataOutputStream(socket.getOutputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}

		while(!isCancelled()){
			VideoChunk chunk = null;
			try {
				//Log.d(LOG_TAG, "waiting for data to send");
				chunk = mVideoChunks.takeLast();
				//Log.d(LOG_TAG,"got data. writing to socket");
				int length = chunk.getData().length;

				dataOut.writeInt(length);
				dataOut.writeInt(chunk.getFlags());
				dataOut.writeLong(chunk.getTimeUs());
				dataOut.write(chunk.getData());
				dataOut.flush();
			} catch (InterruptedException e) {
				e.printStackTrace();
				continue;
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
		return null;
	}
}
