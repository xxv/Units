package info.staticfree.android.units;

import android.net.Uri;
import android.provider.BaseColumns;

public class UsageEntry implements BaseColumns {
	public static final String
		_UNIT = "unit",
		_USE_COUNT = "usecount",
		_FACTOR_FPRINT = "factors";

	public static final String
		PATH = "units",
		PATH_CONFORM_TOP = PATH+"/by_conform";

	public final static Uri
		CONTENT_URI = Uri.parse("content://" + UnitsContentProvider.AUTHORITY + "/" + PATH),
		CONTENT_URI_CONFORM_TOP = Uri.parse("content://" + UnitsContentProvider.AUTHORITY + "/" + PATH_CONFORM_TOP);
}
