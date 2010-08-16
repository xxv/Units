package info.staticfree.android.units;

import android.net.Uri;
import android.provider.BaseColumns;

public class UsageEntry implements BaseColumns {
	public static final String
		_UNIT = "unit",
		_USE_COUNT = "usecount",
		_FACTOR_FPRINT = "factors";

	public static final String
		PATH = "units";

	public final static Uri
		CONTENT_URI = Uri.parse("content://" + UnitsContentProvider.AUTHORITY + "/" + PATH);
}
