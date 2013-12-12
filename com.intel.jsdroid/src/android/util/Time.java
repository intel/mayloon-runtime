package android.util;

import java.util.Date;

public class Time {
	public static String getCurrentTime() {
		Date d = new Date();
		int mYear = d.getYear() + 1900;
		int mMonth = d.getMonth() + 1;
		int mDay = d.getDate();
		int mHour = d.getHours();
		int mMinute = d.getMinutes();
		int mSecond = d.getSeconds();

		return mYear + "-" + mMonth + "-" + mDay + " " + pad(mHour) + ":"
				+ pad(mMinute) + ":" + pad(mSecond);
	}

	private static String pad(int c) {
		if (c >= 10)
			return String.valueOf(c);
		else
			return "0" + String.valueOf(c);
	}
}
