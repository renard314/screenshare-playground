
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
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.view.Surface;

import java.io.IOException;

/**
 * Created by renard on 18/12/14.
 */
public class MediaCodecFactory {
	private static final String MIME_TYPE = "video/avc";
	private static final int FRAME_RATE = 5;               // 15fps
	private static final int IFRAME_INTERVAL = 4;          // 10 seconds between I-frames
	private static final int BIT_RATE = 400000;            // 2Mbps
	private final int mWidth;
	private final int mHeight;

	public MediaCodecFactory(int width, int height){


		mWidth = width;
		mHeight = height;
	}


	public MediaCodec createVideoEncoder() throws IOException {
		// Encoded video resolution matches virtual display.
		MediaFormat encoderFormat = MediaFormat.createVideoFormat(MIME_TYPE, mWidth, mHeight);

		encoderFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
		encoderFormat.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);
		encoderFormat.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
		encoderFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);
		MediaCodec encoder = MediaCodec.createEncoderByType(MIME_TYPE);
		encoder.configure(encoderFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
		return encoder;
	}

	public MediaCodec createVideoDecoder(Surface surface) throws IOException {
		MediaFormat outputFormat = MediaFormat.createVideoFormat(MIME_TYPE, mWidth, mHeight);
		outputFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
		outputFormat.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);
		outputFormat.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
		outputFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);
		MediaCodec decoder = MediaCodec.createDecoderByType(MIME_TYPE);
		decoder.configure(outputFormat, surface, null, 0);
		return decoder;
	}

	public int getWidth() {
		return mWidth;
	}

	public int getHeight() {
		return mHeight;
	}
}
