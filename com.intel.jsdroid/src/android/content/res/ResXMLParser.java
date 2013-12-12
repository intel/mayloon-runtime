package android.content.res;

import java.io.IOException;
import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParserException;

import android.content.res.ResourceTypes.ResChunk_header;
import android.content.res.ResourceTypes.ResStringPool_ref;
import android.content.res.ResourceTypes.ResXMLTree_attrExt;
import android.content.res.ResourceTypes.ResXMLTree_attribute;
import android.content.res.ResourceTypes.ResXMLTree_cdataExt;
import android.content.res.ResourceTypes.ResXMLTree_endElementExt;
import android.content.res.ResourceTypes.ResXMLTree_namespaceExt;
import android.content.res.ResourceTypes.ResXMLTree_node;
import android.content.res.ResourceTypes.Res_value;
import android.util.Errors;
import android.util.TypedValue;

public class ResXMLParser {
	public ResXMLParser() {
	}

	public ResXMLParser(ResXMLTree tree) {
		init(tree);
	}

	public void init(ResXMLTree tree) {
		mTree = tree;
		mReader = tree.mReader;
		mEventCode = BAD_DOCUMENT;
	}

	public void restart() {
		mCurNode = null;
		mEventCode = mTree.mError == Errors.NO_ERROR ? START_DOCUMENT
				: BAD_DOCUMENT;
	}

	public ResStringPool getStrings() {
		return mTree.mStrings;
	}

	public int getEventType() throws XmlPullParserException {
		return mEventCode;
	}

	public int next() {
		if (mEventCode == START_DOCUMENT) {
			mCurNode = mTree.mRootNode;
			mCurExt = mTree.mRootExt;
			return (mEventCode = mTree.mRootCode);
		} else if (mEventCode >= FIRST_CHUNK_CODE) {
			try {
				return nextNode();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return mEventCode;
	}

	public int nextNode() throws IOException {
		if (mEventCode < 0) {
			return mEventCode;
		}

		do {
			mReader.setPosition(mCurNode.header.pointer.offset
					+ mCurNode.header.size);
			if (mReader.getPosition() >= mTree.mDataEnd) {
				mCurNode = null;
				return (mEventCode = END_DOCUMENT);
			}
			ResXMLTree_node next = new ResXMLTree_node(new ResChunk_header(
					mReader.getData(), mReader.getPosition(),
					mReader.readInt(2), mReader.readInt(2), mReader.readInt()),
					mReader.readInt(), new ResStringPool_ref(mReader.readInt()));
			mCurNode = next;
			int headerSize = next.header.headerSize;
			int totalSize = next.header.size;
			mCurExt = next.header.pointer.offset + headerSize;
			int minExtSize = 0;
			int eventCode = next.header.type;
			switch ((mEventCode = eventCode)) {
			case ResourceTypes.RES_XML_START_NAMESPACE_TYPE:
			case ResourceTypes.RES_XML_END_NAMESPACE_TYPE:
				minExtSize = ResXMLTree_namespaceExt.sizeof();
				break;
			case ResourceTypes.RES_XML_START_ELEMENT_TYPE:
				minExtSize = ResXMLTree_attrExt.sizeof();
				break;
			case ResourceTypes.RES_XML_END_ELEMENT_TYPE:
				minExtSize = ResXMLTree_endElementExt.sizeof();
				break;
			case ResourceTypes.RES_XML_CDATA_TYPE:
				minExtSize = ResXMLTree_cdataExt.sizeof();
				break;
			default:
				continue;
			}
			if ((totalSize - headerSize) < minExtSize) {
				return (mEventCode = BAD_DOCUMENT);
			}
			return eventCode;
		} while (true);
	}

	public int getCommentID() {
		return mCurNode != null ? mCurNode.comment.index : -1;
	}

	public int getLineNumber() {
		return mCurNode != null ? mCurNode.lineNumber : -1;
	}

	public int getTextID() {
		try {
			if (mEventCode == TEXT) {
				mReader.setPosition(mCurExt);
				ResXMLTree_cdataExt ext = new ResXMLTree_cdataExt(
						mReader.getPosition(), new ResStringPool_ref(
								mReader.readInt()), new Res_value(
								mReader.readInt(2), mReader.readByte(),
								mReader.readByte(), mReader.readInt()));
				return ext.data.index;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return -1;
	}

	/**
	private final void doNext() throws IOException {
		if (m_event == END_DOCUMENT) {
			return;
		}

		int event = m_event;
		resetEventInfo();

		while (true) {
			if (m_decreaseDepth) {
				m_decreaseDepth = false;
				m_namespaces.decreaseDepth();
			}

			// Fake END_DOCUMENT event.
			if (event == END_TAG && m_namespaces.getDepth() == 1
					&& m_namespaces.getCurrentCount() == 0) {
				m_event = END_DOCUMENT;
				break;
			}

			int chunkType;
			if (event == START_DOCUMENT) {
				// Fake event, see CHUNK_XML_START_TAG handler.
				chunkType = ResourceTypes.RES_XML_START_ELEMENT_TYPE;
			} else {
				chunkType = m_reader.readInt();
			}

			if (chunkType == ResourceTypes.RES_XML_RESOURCE_MAP_TYPE) {
				int chunkSize = m_reader.readInt();
				if (chunkSize < 8 || (chunkSize % 4) != 0) {
					throw new IOException("Invalid resource ids size ("
							+ chunkSize + ").");
				}
				m_resourceIDs = m_reader.readIntArray(chunkSize / 4 - 2);
				continue;
			}

			if (chunkType < ResourceTypes.RES_XML_FIRST_CHUNK_TYPE
					|| chunkType > ResourceTypes.RES_XML_LAST_CHUNK_TYPE) {
				throw new IOException("Invalid chunk type (" + chunkType + ").");
			}

			// Fake START_DOCUMENT event.
			if (chunkType == ResourceTypes.RES_XML_START_ELEMENT_TYPE
					&& event == -1) {
				m_event = START_DOCUMENT;
				break;
			}

			// Common header.
			m_reader.skipInt();//chunkSize
			int lineNumber = m_reader.readInt();
			m_reader.skipInt();//0xFFFFFFFF

			if (chunkType == ResourceTypes.RES_XML_START_NAMESPACE_TYPE
					|| chunkType == ResourceTypes.RES_XML_END_NAMESPACE_TYPE) {
				if (chunkType == ResourceTypes.RES_XML_START_NAMESPACE_TYPE) {
					int prefix = m_reader.readInt();
					int uri = m_reader.readInt();
					m_namespaces.push(prefix, uri);
				} else {
					m_reader.skipInt();//prefix
					m_reader.skipInt();//uri
					m_namespaces.pop();
				}
				continue;
			}

			m_lineNumber = lineNumber;

			if (chunkType == ResourceTypes.RES_XML_START_ELEMENT_TYPE) {
				m_namespaceUri = m_reader.readInt();
				m_name = m_reader.readInt();
				m_reader.skipInt();//flags?
				int attributeCount = m_reader.readInt();
				m_idAttribute = (attributeCount >>> 16) - 1;
				attributeCount &= 0xFFFF;
				m_classAttribute = m_reader.readInt();
				m_styleAttribute = (m_classAttribute >>> 16) - 1;
				m_classAttribute = (m_classAttribute & 0xFFFF) - 1;
				m_attributes = m_reader.readIntArray(attributeCount
						* ATTRIBUTE_LENGHT);
				for (int i = ATTRIBUTE_IX_VALUE_TYPE; i < m_attributes.length;) {
					m_attributes[i] = (m_attributes[i] >>> 24);
					i += ATTRIBUTE_LENGHT;
				}
				m_namespaces.increaseDepth();
				m_event = START_TAG;
				break;
			}

			if (chunkType == ResourceTypes.RES_XML_END_ELEMENT_TYPE) {
				m_namespaceUri = m_reader.readInt();
				m_name = m_reader.readInt();
				m_event = END_TAG;
				m_decreaseDepth = true;
				break;
			}

			if (chunkType == ResourceTypes.RES_XML_CDATA_TYPE) {
				m_name = m_reader.readInt();
				m_reader.skipInt();//?
				m_reader.skipInt();//?
				m_event = TEXT;
				break;
			}
		}
	}
	/**/

	public int getElementNamespaceID() {
		try {
			if (mEventCode == START_TAG) {
				mReader.setPosition(mCurExt);
				ResXMLTree_attrExt ext = new ResXMLTree_attrExt(
						mReader.getPosition(), new ResStringPool_ref(
								mReader.readInt()), new ResStringPool_ref(
								mReader.readInt()), mReader.readInt(2),
						mReader.readInt(2), mReader.readInt(2),
						mReader.readInt(2), mReader.readInt(2),
						mReader.readInt(2));
				return ext.ns.index;
			}
			if (mEventCode == END_TAG) {
				mReader.setPosition(mCurExt);
				ResXMLTree_endElementExt ext = new ResXMLTree_endElementExt(
						mReader.getPosition(), new ResStringPool_ref(
								mReader.readInt()), new ResStringPool_ref(
								mReader.readInt()));
				return ext.ns.index;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return -1;
	}

	public String getElementNamespace() {
		int id = getElementNamespaceID();
		return id >= 0 ? mTree.mStrings.stringAt(id) : null;
	}

	public int getElementNameID() {
		try {
			if (mEventCode == START_TAG) {
				mReader.setPosition(mCurExt);
				ResXMLTree_attrExt ext = new ResXMLTree_attrExt(
						mReader.getPosition(), new ResStringPool_ref(
								mReader.readInt()), new ResStringPool_ref(
								mReader.readInt()), mReader.readInt(2),
						mReader.readInt(2), mReader.readInt(2),
						mReader.readInt(2), mReader.readInt(2),
						mReader.readInt(2));
				return ext.name.index;
			}
			if (mEventCode == END_TAG) {
				mReader.setPosition(mCurExt);
				ResXMLTree_endElementExt ext = new ResXMLTree_endElementExt(
						mReader.getPosition(), new ResStringPool_ref(
								mReader.readInt()), new ResStringPool_ref(
								mReader.readInt()));
				return ext.name.index;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return -1;
	}

	public String getElementName() {
		int id = getElementNameID();
		return id >= 0 ? mTree.mStrings.stringAt(id) : null;
	}

	public int getAttributeCount() {
		try {
			if (mEventCode == START_TAG) {
				mReader.setPosition(mCurExt);
				ResXMLTree_attrExt ext = new ResXMLTree_attrExt(
						mReader.getPosition(), new ResStringPool_ref(
								mReader.readInt()), new ResStringPool_ref(
								mReader.readInt()), mReader.readInt(2),
						mReader.readInt(2), mReader.readInt(2),
						mReader.readInt(2), mReader.readInt(2),
						mReader.readInt(2));
				return ext.attributeCount;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return 0;
	}

	public int getAttributeNamespaceID(int idx) {
		try {
			if (mEventCode == START_TAG) {
				mReader.setPosition(mCurExt);
				ResXMLTree_attrExt tag = new ResXMLTree_attrExt(
						mReader.getPosition(), new ResStringPool_ref(
								mReader.readInt()), new ResStringPool_ref(
								mReader.readInt()), mReader.readInt(2),
						mReader.readInt(2), mReader.readInt(2),
						mReader.readInt(2), mReader.readInt(2),
						mReader.readInt(2));
				if (idx < tag.attributeCount) {
					mReader.setPosition(tag.offset + tag.attributeStart
							+ tag.attributeSize * idx);
					ResXMLTree_attribute attr = new ResXMLTree_attribute(
							mReader.getPosition(), new ResStringPool_ref(
									mReader.readInt()), new ResStringPool_ref(
									mReader.readInt()), new ResStringPool_ref(
									mReader.readInt()), new Res_value(
									mReader.readInt(2), mReader.readByte(),
									mReader.readByte(), mReader.readInt()));
					return attr.ns.index;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return -2; // FIXME -2 means what
	}

	public String getAttributeNamespace(int idx) {
		int id = getAttributeNamespaceID(idx);
		return id >= 0 ? mTree.mStrings.stringAt(id) : null;
	}

	public int getAttributeNameID(int idx) {
		try {
			if (mEventCode == START_TAG) {
				mReader.setPosition(mCurExt);
				ResXMLTree_attrExt tag = new ResXMLTree_attrExt(
						mReader.getPosition(), new ResStringPool_ref(
								mReader.readInt()), new ResStringPool_ref(
								mReader.readInt()), mReader.readInt(2),
						mReader.readInt(2), mReader.readInt(2),
						mReader.readInt(2), mReader.readInt(2),
						mReader.readInt(2));
				if (idx < tag.attributeCount) {
					mReader.setPosition(tag.offset + tag.attributeStart
							+ tag.attributeSize * idx);
					ResXMLTree_attribute attr = new ResXMLTree_attribute(
							mReader.getPosition(), new ResStringPool_ref(
									mReader.readInt()), new ResStringPool_ref(
									mReader.readInt()), new ResStringPool_ref(
									mReader.readInt()), new Res_value(
									mReader.readInt(2), mReader.readByte(),
									mReader.readByte(), mReader.readInt()));
					return attr.name.index;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return -1; // FIXME -1 means what
	}

	public String getAttributeName(int idx) {
		int id = getAttributeNameID(idx);
		return id >= 0 ? mTree.mStrings.stringAt(id) : null;
	}

	public int getAttributeNameResID(int idx) {
		int id = getAttributeNameID(idx);
		if (id >= 0 && id < mTree.mNumResIds)
			return mTree.mResIds[id];
		return 0;
	}

	public int getAttributeValueStringID(int idx) {
		try {
			if (mEventCode == START_TAG) {
				mReader.setPosition(mCurExt);
				ResXMLTree_attrExt tag = new ResXMLTree_attrExt(
						mReader.getPosition(), new ResStringPool_ref(
								mReader.readInt()), new ResStringPool_ref(
								mReader.readInt()), mReader.readInt(2),
						mReader.readInt(2), mReader.readInt(2),
						mReader.readInt(2), mReader.readInt(2),
						mReader.readInt(2));
				if (idx < tag.attributeCount) {
					mReader.setPosition(tag.offset + tag.attributeStart
							+ tag.attributeSize * idx);
					ResXMLTree_attribute attr = new ResXMLTree_attribute(
							mReader.getPosition(), new ResStringPool_ref(
									mReader.readInt()), new ResStringPool_ref(
									mReader.readInt()), new ResStringPool_ref(
									mReader.readInt()), new Res_value(
									mReader.readInt(2), mReader.readByte(),
									mReader.readByte(), mReader.readInt()));
					return attr.rawValue.index;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return -1; // FIXME -1 means what
	}

	public int getAttributeValue(int idx, Res_value outValue) {
		try {
			if (mEventCode == START_TAG) {
				mReader.setPosition(mCurExt);
				ResXMLTree_attrExt tag = new ResXMLTree_attrExt(
						mReader.getPosition(), new ResStringPool_ref(
								mReader.readInt()), new ResStringPool_ref(
								mReader.readInt()), mReader.readInt(2),
						mReader.readInt(2), mReader.readInt(2),
						mReader.readInt(2), mReader.readInt(2),
						mReader.readInt(2));
				if (idx < tag.attributeCount) {
					mReader.setPosition(tag.offset + tag.attributeStart
							+ tag.attributeSize * idx);
					ResXMLTree_attribute attr = new ResXMLTree_attribute(
							mReader.getPosition(), new ResStringPool_ref(
									mReader.readInt()), new ResStringPool_ref(
									mReader.readInt()), new ResStringPool_ref(
									mReader.readInt()), new Res_value(
									mReader.readInt(2), mReader.readByte(),
									mReader.readByte(), mReader.readInt()));
					outValue.copyFrom(attr.typedValue);
					return Res_value.sizeof();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return Errors.BAD_TYPE;
	}

	public int getAttributeDataType(int idx) {
		try {
			if (mEventCode == START_TAG) {
				mReader.setPosition(mCurExt);
				ResXMLTree_attrExt tag = new ResXMLTree_attrExt(
						mReader.getPosition(), new ResStringPool_ref(
								mReader.readInt()), new ResStringPool_ref(
								mReader.readInt()), mReader.readInt(2),
						mReader.readInt(2), mReader.readInt(2),
						mReader.readInt(2), mReader.readInt(2),
						mReader.readInt(2));
				if (idx < tag.attributeCount) {
					mReader.setPosition(tag.offset + tag.attributeStart
							+ tag.attributeSize * idx);
					ResXMLTree_attribute attr = new ResXMLTree_attribute(
							mReader.getPosition(), new ResStringPool_ref(
									mReader.readInt()), new ResStringPool_ref(
									mReader.readInt()), new ResStringPool_ref(
									mReader.readInt()), new Res_value(
									mReader.readInt(2), mReader.readByte(),
									mReader.readByte(), mReader.readInt()));
					return attr.typedValue.dataType;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return TypedValue.TYPE_NULL;
	}

	public int getAttributeData(int idx) {
		try {
			if (mEventCode == START_TAG) {
				mReader.setPosition(mCurExt);
				ResXMLTree_attrExt tag = new ResXMLTree_attrExt(
						mReader.getPosition(), new ResStringPool_ref(
								mReader.readInt()), new ResStringPool_ref(
								mReader.readInt()), mReader.readInt(2),
						mReader.readInt(2), mReader.readInt(2),
						mReader.readInt(2), mReader.readInt(2),
						mReader.readInt(2));
				if (idx < tag.attributeCount) {
					mReader.setPosition(tag.offset + tag.attributeStart
							+ tag.attributeSize * idx);
					ResXMLTree_attribute attr = new ResXMLTree_attribute(
							mReader.getPosition(), new ResStringPool_ref(
									mReader.readInt()), new ResStringPool_ref(
									mReader.readInt()), new ResStringPool_ref(
									mReader.readInt()), new Res_value(
									mReader.readInt(2), mReader.readByte(),
									mReader.readByte(), mReader.readInt()));
					return attr.typedValue.data;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return 0;
	}

	public int indexOfAttribute(String namespace, String name) {
		//		System.out.println("finding: " + name + " mEventCode = " + mEventCode);
		if (mEventCode == START_TAG) {
			int N = getAttributeCount();
			//			System.out.println("getAttributeCount: " + N);
			for (int i = 0; i < N; ++i) {
				String curNs = getAttributeNamespace(i);
				String curAttr = getAttributeName(i);
				//				if (curNs != null)
				//					System.out.println("attribute[" + i + "]: " + curNs + ":"
				//							+ curAttr);
				//				else
				//					System.out.println("attribute[" + i + "]: " + curAttr);
				if (curAttr.equals(name) == true) {
					if (namespace == null) {
						if (curNs == null)
							return i;
					} else if (curNs != null) {
						if (curNs.equals(namespace) == true)
							return i;
					}
				}
			}
		}

		return Errors.NAME_NOT_FOUND;
	}

	public int indexOfID() {
		try {
			if (mEventCode == START_TAG) {
				mReader.setPosition(mCurExt);
				ResXMLTree_attrExt ext = new ResXMLTree_attrExt(
						mReader.getPosition(), new ResStringPool_ref(
								mReader.readInt()), new ResStringPool_ref(
								mReader.readInt()), mReader.readInt(2),
						mReader.readInt(2), mReader.readInt(2),
						mReader.readInt(2), mReader.readInt(2),
						mReader.readInt(2));
				int idx = ext.idIndex;
				if (idx > 0)
					return (idx - 1);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return Errors.NAME_NOT_FOUND;
	}

	public int indexOfClass() {
		try {
			if (mEventCode == START_TAG) {
				mReader.setPosition(mCurExt);
				ResXMLTree_attrExt ext = new ResXMLTree_attrExt(
						mReader.getPosition(), new ResStringPool_ref(
								mReader.readInt()), new ResStringPool_ref(
								mReader.readInt()), mReader.readInt(2),
						mReader.readInt(2), mReader.readInt(2),
						mReader.readInt(2), mReader.readInt(2),
						mReader.readInt(2));
				int idx = ext.classIndex;
				if (idx > 0)
					return (idx - 1);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return Errors.NAME_NOT_FOUND;
	}

	public int indexOfStyle() {
		try {
			if (mEventCode == START_TAG) {
				mReader.setPosition(mCurExt);
				ResXMLTree_attrExt ext = new ResXMLTree_attrExt(
						mReader.getPosition(), new ResStringPool_ref(
								mReader.readInt()), new ResStringPool_ref(
								mReader.readInt()), mReader.readInt(2),
						mReader.readInt(2), mReader.readInt(2),
						mReader.readInt(2), mReader.readInt(2),
						mReader.readInt(2));
				int idx = ext.styleIndex;
				if (idx > 0)
					return (idx - 1);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return Errors.NAME_NOT_FOUND;
	}

	public static final int BAD_DOCUMENT = -1;
	public static final int START_DOCUMENT = 0;
	public static final int END_DOCUMENT = 1;
	public static final int FIRST_CHUNK_CODE = ResourceTypes.RES_XML_FIRST_CHUNK_TYPE;
	public static final int START_NAMESPACE = ResourceTypes.RES_XML_START_NAMESPACE_TYPE;
	public static final int END_NAMESPACE = ResourceTypes.RES_XML_END_NAMESPACE_TYPE;
	public static final int START_TAG = ResourceTypes.RES_XML_START_ELEMENT_TYPE;
	public static final int END_TAG = ResourceTypes.RES_XML_END_ELEMENT_TYPE;
	public static final int TEXT = ResourceTypes.RES_XML_CDATA_TYPE;

	// set to protected in the future
	public ResXMLTree mTree;
	public int mEventCode;
	public ResXMLTree_node mCurNode;
	public int mCurExt;

	public IntReader mReader = null;
}
