package info.staticfree.android.units;

import android.net.Uri;
import android.provider.BaseColumns;

public class ClassificationEntry implements BaseColumns {
	public final static String
		_DESCRIPTION = "description",
		_FACTOR_FPRINT = "factors";

	public final static String
		PATH = "classification",
		PATH_BY_FPRINT = "fprint";

	public final static String[]
	    PROJECTION = {_ID, _DESCRIPTION, _FACTOR_FPRINT};


	public final static Uri
		CONTENT_URI = Uri.parse("content://" + UnitsContentProvider.AUTHORITY + "/" + PATH);

	public static Uri getFprintUri(String fprint){
		return Uri.withAppendedPath(CONTENT_URI, PATH_BY_FPRINT + "/" + fprint);
	}
}
