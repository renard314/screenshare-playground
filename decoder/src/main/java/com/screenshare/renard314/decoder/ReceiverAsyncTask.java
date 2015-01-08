
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

import android.os.AsyncTask;
import android.util.Log;
import android.util.Pair;

import com.screenshare.renard314.screensharelib.VideoChunk;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by renard on 22/12/14.
 */
public class ReceiverAsyncTask extends AsyncTask<Void,VideoChunk,Void>{

	private static final String LOG_TAG = ReceiverAsyncTask.class.getSimpleName();


	interface ReceiverListener {
		void onVideoChunk(VideoChunk chunk);
        void onConnectionLost();
    }

	private ServerSocket mServerSocket;
	private final ReceiverListener mListener;

	ReceiverAsyncTask(ReceiverListener listener) {
		this.mListener = listener;
	}

	@Override
	protected Void doInBackground(Void... params) {
        Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
        final Socket accept;
		try {
			mServerSocket = new ServerSocket(1234);
			Log.i(LOG_TAG, "waiting for connection");
			accept = mServerSocket.accept();
			Log.i(LOG_TAG, "Socket accepted");
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		final DataInputStream inputStream;
		try {
			inputStream = new DataInputStream(accept.getInputStream());
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		while(!isCancelled()){
			try {
				int length  = inputStream.readInt();
				int flags  = inputStream.readInt();
				long timeUs  = inputStream.readLong();
				byte[] data = new byte[length];
				inputStream.readFully(data, 0, length);
				VideoChunk chunk = new VideoChunk(data,flags,timeUs);
                publishProgress(chunk);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

    @Override
    protected void onPostExecute(Void aVoid) {
        mListener.onConnectionLost();
    }

    @Override
	protected void onProgressUpdate(VideoChunk... values) {
		super.onProgressUpdate(values);
		mListener.onVideoChunk(values[0]);
	}
}
