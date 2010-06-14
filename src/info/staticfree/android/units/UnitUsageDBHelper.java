package info.staticfree.android.units;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.unitsinjava.Unit;

import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.widget.FilterQueryProvider;
import android.widget.SimpleCursorAdapter;

/**
 * In order to sort through all the possible units, a database of weights is used.
 * Units with a higher weight are shown first in all lists. 
 * Weights are initialized from static JSON files, including regional weights (eg.
 * to put imperial units above metric in the US).
 * 
 * @author steve
 *
 */
public class UnitUsageDBHelper extends SQLiteOpenHelper {
	public final static String TAG = UnitUsageDBHelper.class.getSimpleName();
	public static final String
		DB_NAME = "units",
		DB_USAGE_TABLE = "usage";
	private final Context context;
	
	private static final int DB_VERSION = 1;
	
    public UnitUsageDBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        this.context = context;
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
		   
		   Log.d(TAG, "init all weights hash");
		   final HashMap<String, Integer> allUnitWeights = 
			   new HashMap<String, Integer>(Unit.table.keySet().size());
		   Log.d(TAG, "adding all known weights...");
		   for (final String unitName: Unit.table.keySet()){
			   // don't add all uppercase names
			   if (! unitName.toUpperCase().equals(unitName)){
				   allUnitWeights.put(unitName, 0);
			   }
		   }
		   Log.d(TAG, "adding common weights");
		   addAll(loadInitialWeights(R.raw.common_weights), allUnitWeights);
		   Log.d(TAG, "adding regional weights");
		   addAll(loadInitialWeights(R.raw.regional_weights), allUnitWeights);

		   // This is so that things of common weight end up in non-random order
		   // without having to do an SQL order-by.
		   final ArrayList<String> sortedUnits = new ArrayList<String>(allUnitWeights.keySet());
		   Log.d(TAG, "Sorting units...");
		   Collections.sort(sortedUnits);
		   Log.d(TAG, "Adding all sorted units...");
		   // TODO put this in a background thread and show progress bar.
		   db.beginTransaction();
		   for (final String unitName: sortedUnits){
			   cv.put(UsageEntry._UNIT, unitName);
			   cv.put(UsageEntry._USE_COUNT, allUnitWeights.get(unitName));
			   
			   db.insert(DB_USAGE_TABLE, null, cv);
		   }
		   db.setTransactionSuccessful();
		   db.endTransaction();
		   Log.d(TAG, "done!");
	}
	
	private void addAll(JSONObject unitWeights, HashMap<String, Integer> allWeights){
		for (final Iterator i = unitWeights.keys(); i.hasNext(); ){
			final String key = (String)i.next(); 
			if (allWeights.containsKey(key)){
				allWeights.put(key, allWeights.get(key) + unitWeights.optInt(key));
			}else{
				allWeights.put(key, unitWeights.optInt(key));
			}
		}
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS "+ DB_USAGE_TABLE);
        onCreate(db);
		
	}
	
    /**
     * A weight file is just a static JSON file used to set the initial
     * weights for unit recommendation precedence. 
     * 
     * @param resourceId
     * @return
     */
    private JSONObject loadInitialWeights(int resourceId){
    	final InputStream is = context.getResources().openRawResource(resourceId);
    	
    	final StringBuilder jsonString = new StringBuilder();
    	try{
    		
	    	for (final BufferedReader isReader = new BufferedReader(new InputStreamReader(is), 16000);
	    			isReader.ready();){
	    		jsonString.append(isReader.readLine());
	    	}
	    	final JSONObject jo = new JSONObject(jsonString.toString());
	    	
	    	// remove all "comments", which are just key entries that start with "--"
	    	for (final Iterator i = jo.keys(); i.hasNext(); ){
	    		final String key = (String)i.next();
	    		if (key.startsWith("--")){
	    			i.remove();
	    		}
	    	}
	    	
	    	return jo;
	    	
    	}catch (final Exception e){
    		e.printStackTrace();
    	}
    	return null;
    }
	
    public final static String USAGE_SORT =  UsageEntry._USE_COUNT + " DESC, "+UsageEntry._UNIT + " ASC";
    /**
     * Creates an Adapter that looks for the start of a unit string from the database.
     * For use with the MultiAutoCompleteTextView
     * 
     * @param db the unit usage database
     * @return an Adapter that uses the Simple Dropdown Item view
     */
    public SimpleCursorAdapter getUnitPrefixAdapter(Context context, SQLiteDatabase db){
    	
    	final Cursor dbCursor = db.query(DB_USAGE_TABLE, null, null, null, null, null, USAGE_SORT);

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
				return db.query(DB_USAGE_TABLE, null, null, null, null, null, USAGE_SORT);
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
								new String[] {modConstraint+"*"}, null, null, USAGE_SORT);
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