package android.content.res;

import java.io.IOException;

import android.content.res.ResourceTypes.ResChunk_header;
import android.content.res.ResourceTypes.ResStringPool_ref;
import android.content.res.ResourceTypes.ResXMLTree_header;
import android.content.res.ResourceTypes.ResXMLTree_node;
import android.util.Errors;

public class ResXMLTree extends ResXMLParser {
	int mError = Errors.NO_INIT;
	//byte[] mOwnedData = null;
	ResXMLTree_header mHeader;
	int mSize;
	int mDataEnd;
	ResStringPool mStrings = new ResStringPool();
	int[] mResIds;
	int mNumResIds;
	ResXMLTree_node mRootNode;
	int mRootExt;
	int mRootCode;

	public ResXMLTree() {
		super.init(this);
		restart();
	}

	public ResXMLTree(byte[] data, int offset, int size, boolean copyData) {
		super.init(this);
		mReader = new IntReader(data, offset, false);
		setTo(data, offset, size, copyData);
	}

	public int setTo(byte[] data, int offset, int size, boolean copyData) {
		try {
			uninit();

			if (mReader == null)
				mReader = new IntReader(data, offset, false);
			mEventCode = START_DOCUMENT;

			mReader.setPosition(offset);
			ResChunk_header chunk = new ResChunk_header(mReader.getData(),
					mReader.getPosition(), mReader.readInt(2),
					mReader.readInt(2), mReader.readInt());
			ChunkUtil.checkType(chunk.type, ResourceTypes.RES_XML_TYPE);
			mHeader = new ResXMLTree_header(chunk);
			mSize = mHeader.header.size;
			if (mHeader.header.headerSize > mSize || mSize > size) {
				mError = Errors.BAD_TYPE;
				restart();
				return mError;
			}
			mDataEnd = mHeader.header.pointer.offset + mSize;

			mStrings.uninit();
			mRootNode = null;
			mResIds = null;
			mNumResIds = 0;

			// look for string block, res_map block and first xml block
			mReader.setPosition(mHeader.header.pointer.offset
					+ mHeader.header.headerSize);
			chunk = new ResChunk_header(mReader.getData(),
					mReader.getPosition(), mReader.readInt(2),
					mReader.readInt(2), mReader.readInt());
			ResChunk_header lastChunk = chunk;
			while (chunk.pointer.offset < (mDataEnd - ResChunk_header.sizeof())
					&& (chunk.pointer.offset < (mDataEnd - chunk.size))) {
				// TODO validate_chunk
				int type = chunk.type;
				int chunkSize = chunk.size;
				//				System.out.println("Chunk start at " + mReader.getPosition()
				//						+ ", type = 0x" + Integer.toHexString(type)
				//						+ ", size = 0x" + Integer.toHexString(chunkSize));
				if (type == ResourceTypes.RES_STRING_POOL_TYPE) {
					mStrings.setTo(data, chunk.pointer.offset, chunkSize, false);
				} else if (type == ResourceTypes.RES_XML_RESOURCE_MAP_TYPE) {
					mReader.setPosition(chunk.pointer.offset + chunk.headerSize);
					mNumResIds = (chunk.size - chunk.headerSize) / 4;
					mResIds = mReader.readIntArray(mNumResIds);
				} else if (type >= ResourceTypes.RES_XML_FIRST_CHUNK_TYPE
						&& type <= ResourceTypes.RES_XML_LAST_CHUNK_TYPE) {
					mCurNode = new ResXMLTree_node(lastChunk, mReader.readInt(),
							new ResStringPool_ref(mReader.readInt()));
					if (nextNode() == BAD_DOCUMENT) {
						mError = Errors.BAD_TYPE;
						restart();
						return mError;
					}
					mRootNode = mCurNode;
					mRootExt = mCurExt;
					mRootCode = mEventCode;
					break;
				} else {
					System.out.println("Skipping unknown chunk!");
				}
				lastChunk = chunk;
				mReader.setPosition(lastChunk.pointer.offset + lastChunk.size);
				chunk = new ResChunk_header(mReader.getData(),
						mReader.getPosition(), mReader.readInt(2),
						mReader.readInt(2), mReader.readInt());
			}

			if (mRootNode == null) {
				System.out.println("");
				mError = Errors.BAD_TYPE;
				restart();
				return mError;
			}

			mError = mStrings.getError();
		} catch (IOException e) {
			e.printStackTrace();
		}
		restart();
		return mError;
	}

	public int getError() {
		return mError;
	}

	public void uninit() {
		mStrings.uninit();
		mReader = null;
		restart();
	}
}
