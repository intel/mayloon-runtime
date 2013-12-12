package android.content.res;

import android.graphics.drawable.Drawable;
import android.util.Errors;

public abstract class Asset {
	public static boolean DEBUG = false;
	public abstract int read(byte[] buf, int count);

	public abstract int read();	// read the next byte

	public abstract int seek(int offset, int whence);

	public void close() {

	}

	public abstract byte[] getBuffer(boolean wordAligned);

	public abstract int getLength() ;

    public abstract int getAssetRemainingLength();

	public String getAssetSource() {
		return mAssetSource;
	}

	// protected
	protected void setAssetSource(String path) {
		mAssetSource = path;
	}

	// private, friend class AssetManager
	public static Asset createFromFile(String fileName, int mode) {
//		_FileAsset pAsset = new _FileAsset();
		_FileAsset pAsset = null;
		try {
			pAsset = (_FileAsset) Class.forName(
			        "android.content.res._FileAsset")
			        .newInstance();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		int result = pAsset.openChunk(fileName, 0, -1);
		if (result != Errors.NO_ERROR) {
			pAsset = null;
			return null;
		}
		pAsset.mAccessMode = mode;
		return pAsset;
	}

	public String mAssetSource;
	public int mAccessMode;
	public Asset mNext;
	public Asset mPrev;


	// enum FileType
	public static final int kFileTypeUnknown = 0;
	public static final int kFileTypeNonexistent = 1;
	public static final int kFileTypeRegular = 2;
	public static final int kFileTypeDirectory = 3;
}
