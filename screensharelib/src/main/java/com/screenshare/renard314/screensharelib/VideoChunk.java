
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

import java.nio.ByteBuffer;

/**
 * Created by renard on 18/12/14.
 */
public class VideoChunk {
	private final byte[] mData;
	private final int mFlags;
	private final long mTimeUs;

	public VideoChunk(byte[] buffer, int flags, long presentationTimeUs) {
		mData = buffer;
		this.mFlags = flags;
		this.mTimeUs = presentationTimeUs;
	}

	public VideoChunk(ByteBuffer buffer, int flags, long presentationTimeUs) {
		mData = new byte[buffer.remaining()];
		buffer.get(mData);
		this.mFlags = flags;
		this.mTimeUs = presentationTimeUs;
	}

    public boolean isConfigFrame(){
        return (getFlags()&MediaCodec.BUFFER_FLAG_CODEC_CONFIG)==MediaCodec.BUFFER_FLAG_CODEC_CONFIG;
    }

    public boolean isKeyFrame(){
        return (getFlags()&MediaCodec.BUFFER_FLAG_KEY_FRAME)==MediaCodec.BUFFER_FLAG_KEY_FRAME;
    }




	public byte[] getData() {
		return mData;
	}

	public long getTimeUs() {
		return mTimeUs;
	}

	public int getFlags() {
		return mFlags;
	}

}
