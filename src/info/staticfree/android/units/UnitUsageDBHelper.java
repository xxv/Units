package info.staticfree.android.units;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.unitsinjava.Unit;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.widget.FilterQueryProvider;
import android.widget.SimpleCursorAdapter;

public class UnitUsageDBHelper extends SQLiteOpenHelper {
	public static final String
		DB_NAME = "units",
		DB_USAGE_TABLE = "usage";
	
	private static final int DB_VERSION = 1;
	
    public UnitUsageDBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
}

	@Override
	public void onCreate(SQLiteDatabase db) {
		   db.execSQL("CREATE TABLE '"+DB_USAGE_TABLE+
           "' ('"   + UsageEntry._ID + "' INTEGER PRIMARY KEY," +
           		"'" + UsageEntry._UNIT+"' VARCHAR(255)," +
           		"'" + UsageEntry._USE_COUNT + "' INTEGER" + 
           	")");
		   
		   // load the initial table in
		   final ContentValues cv = new ContentValues();
		   cv.put(UsageEntry._USE_COUNT, 0);
		   for (final String unitName: Unit.table.keySet()){
			   cv.put(UsageEntry._UNIT, unitName);
			   
			   db.insert(DB_USAGE_TABLE, null, cv);
		   }
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS "+ DB_USAGE_TABLE);
        onCreate(db);
		
	}
	
    /**
     * Creates an Adapter that looks for the start of a unit string from the database.
     * For use with the AutoCompleteTextView
     * 
     * @param db the unit usage database
     * @return an Adapter that uses the Simple Dropdown Item view
     */
    public SimpleCursorAdapter getUnitPrefixAdapter(Context context, SQLiteDatabase db){
    	
    	final Cursor dbCursor = db.query(DB_USAGE_TABLE, null, null, null, null, null, UsageEntry._USE_COUNT + " DESC");

    	// Simple it says - ha!
    	final SimpleCursorAdapter adapter = new SimpleCursorAdapter(context, 
    			android.R.layout.simple_dropdown_item_1line, 
    			dbCursor, 
    			new String[] {UsageEntry._UNIT}, 
    			new int[] {android.R.id.text1} );

    	adapter.setStringConversionColumn(dbCursor.getColumnIndex(UsageEntry._UNIT));

    	// a filter that searches for units starting with the given constraint
    	adapter.setFilterQueryProvider(new UnitMatcherFilterQueryProvider(db));
    	return adapter;
    }
    
    private class UnitMatcherFilterQueryProvider implements FilterQueryProvider {
    	private final SQLiteDatabase db;
    	// matches a unit being entered in-progress (at the end of the expression)
    	// intentionally does not match single-letter units (what's the point for autocompleting these?)
    	private final Pattern unitRegex = Pattern.compile("^.*?([a-zA-Z]\\w+)$");
    	
    	public UnitMatcherFilterQueryProvider(SQLiteDatabase db){
    		this.db = db;
    	}
    	
    	public Cursor runQuery(CharSequence constraint) {
			if (constraint == null || constraint.length() == 0){
				return db.query(DB_USAGE_TABLE, null, null, null, null, null, UsageEntry._USE_COUNT + " DESC");
			}else{
				final Matcher m = unitRegex.matcher(constraint);
				String modConstraint;
				if (m.matches()){
					modConstraint = m.group(1);
				}else{
					return null;
				}

				Log.d("REGEX", modConstraint);
				return db.query(DB_USAGE_TABLE, 
						null, 
						UsageEntry._UNIT +" GLOB ?", 
								new String[] {modConstraint+"*"}, null, null, UsageEntry._USE_COUNT + " DESC, " + UsageEntry._UNIT + " ASC");
			}
		}
    }

    /**
     * Increments the usage counter for the given unit.
     * @param unit name of the unit
     * @param db the unit usage database
     */
    public void logUnitUsed(String unit, SQLiteDatabase db){
    	// TODO efficient, but should probably be sanity checked.
    	db.execSQL("UPDATE " + DB_USAGE_TABLE + " SET " + UsageEntry._USE_COUNT + "=" + UsageEntry._USE_COUNT + " + 1 WHERE " + UsageEntry._UNIT + "='" + unit + "'" );
    	
    }

}
