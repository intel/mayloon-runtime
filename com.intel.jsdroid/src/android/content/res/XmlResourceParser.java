package android.content.res;

import org.xmlpull.v1.XmlPullParser;
import android.util.AttributeSet;

public interface XmlResourceParser extends XmlPullParser, AttributeSet {
	public void close();

	public int getAttributeValueType(int index);

	public int getAttributeValueData(int index);
}
