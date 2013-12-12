package android.content.res;

import android.util.Errors;
import android.util.Log;

class _FileAsset extends Asset {
	public byte[] mBuf = null;
	public int mStart = 0;
	public int mLength = 0;
	public int mOffset = 0;
	public String mFileName = null;

	public byte[] getBuffer(boolean wordAligned) {
		return mBuf;
	}

	public int getLength() {
		return mLength;
	}

	// read whole file here, but set mStart and mLength according to offset/length
	public int openChunk(String fileName, int offset, int length) {
		int fileLength = -1;
		if(DEBUG)System.out.println("openChunk>>>File: " + fileName);
		/**
		 @j2sNative
		     var xmlhttp;
		     if (window.XMLHttpRequest) {// code for IE7+, Firefox, Chrome, Opera, Safari
		         xmlhttp=new XMLHttpRequest();
		     }
		     else  {// code for IE6, IE5
		         xmlhttp=new ActiveXObject("Microsoft.XMLHTTP");
		     }

		     xmlhttp.open("GET", fileName, false);
		     xmlhttp.responseType = "arraybuffer";
		     xmlhttp.send(null);
		     var data = null;
		     if (xmlhttp.response || xmlhttp.mozResponseArrayBuffer) {
		         data = new Uint8Array(xmlhttp.response || xmlhttp.mozResponseArrayBuffer);
		     }
		     if (data != null) {
		         var fileLength = data.length;
		         if (length < 0) length = fileLength;
		         if ((offset + length) > fileLength) {
		             return android.util.Errors.BAD_INDEX;
		         }
                 if (length == fileLength) {
                     this.mBuf = data;
                 } else {
                     this.mBuf = data.subarray(offset, offset + length);
                 }
		     }
		 */{}


		if (mBuf != null) {
			if(DEBUG) Log.d("Asset", "read: " + mBuf.length + " bytes");
		} else {
		    Log.e("Asset", "mBuf == null");
		}

		mStart = offset;
		mLength = length;
		mFileName = fileName;

		return Errors.NO_ERROR;
	}

	@Override
	public int read(byte[] buf, int count) {
		int actualRead = 0;
		while ((mOffset < mLength) && (actualRead < count)) {
			buf[actualRead] = mBuf[mOffset];
			mOffset++;
			actualRead++;
		}
		return actualRead;
	}

	// read the next byte, will be -1 if reach the end
	@Override
	public int read() {
		if (mOffset < mLength) {
			mOffset++;
			return mBuf[mOffset -1];
		} else return -1;

	}

	public static final int SEEK_SET = 1;	// set the mStart
	public static final int SEEK_CUR = 2;	// mStart + offset
	public static final int SEEK_END = 3;	// mEnd + offset (offset should be a negative)

	@Override
	public int seek(int offset, int whence) {
		switch(whence) {
		case SEEK_SET:
			mOffset = offset >= mLength ? mLength - 1: offset;
			break;
		case SEEK_CUR:
			offset = mOffset + offset;
			mOffset = offset >= mLength ? mLength - 1: offset;
			break;
		case SEEK_END:
			offset = mLength + offset;
			mOffset = offset >= mLength ? mLength - 1: offset;
			break;
		default:
			// nothing changed
		}
		return mOffset;
	}

    @Override
    public int getAssetRemainingLength() {
        return mLength - mOffset;
    }

}