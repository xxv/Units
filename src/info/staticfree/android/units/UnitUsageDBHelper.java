package info.staticfree.android.units;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.unitsinjava.EvalError;
import net.sourceforge.unitsinjava.Unit;
import net.sourceforge.unitsinjava.Value;

import org.json.JSONObject;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.widget.FilterQueryProvider;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

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

	private static final int DB_VERSION = 3;

    public UnitUsageDBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        this.context = context;
    }

	@Override
	public void onCreate(SQLiteDatabase db) {
		   db.execSQL("CREATE TABLE '"+DB_USAGE_TABLE+
           "' ('"   + UsageEntry._ID + "' INTEGER PRIMARY KEY," +
           		"'" + UsageEntry._UNIT+"' VARCHAR(255)," +
           		"'" + UsageEntry._USE_COUNT + "' INTEGER," +
           		"'" + UsageEntry._FACTOR_FPRINT + "' TEXT," +
           		"CONSTRAINT unit_unique UNIQUE ("+UsageEntry._UNIT+") ON CONFLICT IGNORE" +
           	")");
		   db.execSQL("CREATE INDEX 'factor_fprints' ON "+DB_USAGE_TABLE + " (" +UsageEntry._FACTOR_FPRINT+ ")");
	}

	public int getUnitUsageDbCount(SQLiteDatabase db){
		final String[] proj = {UsageEntry._ID};
		if (!db.isOpen()){
			return -1;
		}
		final Cursor c = db.query(DB_USAGE_TABLE, proj, null, null, null, null, null);
		c.moveToFirst();
		final int count = c.getCount();
		c.close();
		return count;
	}

	public static String getFingerprint(String unitName){
		   String fpr = "";
		   try {
			   final Value unit = ValueGui.fromString(unitName);


			   fpr = ValueGui.getFingerprint(unit);
		   }catch (final EvalError e){
			   // skip things we can't handle
		   }
		   return fpr;
	}

	public void loadInitialUnitUsage(SQLiteDatabase db){
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

		   db.beginTransaction();
		   for (final String unitName: sortedUnits){
			   cv.put(UsageEntry._UNIT, unitName);
			   cv.put(UsageEntry._USE_COUNT, allUnitWeights.get(unitName));

			   final String fpr = getFingerprint(unitName);
			   cv.put(UsageEntry._FACTOR_FPRINT, fpr);
			   Log.d(TAG, unitName + ": " + fpr);
			   db.insert(DB_USAGE_TABLE, null, cv);
		   }
		   db.setTransactionSuccessful();
		   db.endTransaction();
		   Log.d(TAG, "done!");
	}

	@SuppressWarnings("unchecked")
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
    @SuppressWarnings("unchecked")
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
	private static final String CONFORMING_SELECTION = UsageEntry._FACTOR_FPRINT + " = ?";



    /**
     * Creates an Adapter that looks for the start of a unit string from the database.
     * For use with the MultiAutoCompleteTextView
     *
     * @param db the unit usage database
     * @return an Adapter that uses the Simple Dropdown Item view
     */
    public UnitCursorAdapter getUnitPrefixAdapter(Activity context, SQLiteDatabase db, TextView otherEntry){

    	final Cursor dbCursor = db.query(DB_USAGE_TABLE, null, null, null, null, null, USAGE_SORT);

    	context.startManagingCursor(dbCursor);

    	final UnitCursorAdapter adapter = new UnitCursorAdapter(context, dbCursor, db, otherEntry);




    	return adapter;
    }

	private static String cachedEntryText;
	private static String[] cachedEntryFprintArgs;

	/**
	 * @param otherEntry
	 * @return
	 */
	private synchronized static String[] getConformingSelectionArgs(TextView otherEntry){
		final String otherEntryText = otherEntry.getText().toString();
		if (otherEntryText.length() > 0){
			if (otherEntryText.toString().equals(cachedEntryText)){
				return cachedEntryFprintArgs;
			}
			try {
				final String[] conformingSelectionArgs = {ValueGui.getFingerprint(ValueGui.fromString(Units.unicodeToAscii(otherEntryText)))};
				cachedEntryText = otherEntryText;
				cachedEntryFprintArgs = conformingSelectionArgs;
				return conformingSelectionArgs;
			}catch (final EvalError e){
				return null;
			}
		}
		return null;
	}

    public class UnitCursorAdapter extends SimpleCursorAdapter {
    	private final SQLiteDatabase db;

    	private static final int MSG_REQUERY = 0;
    	private boolean runningQuery;
    	private final Activity mActivity;
    	private final TextView otherEntry;
    	private final Handler mHandler = new Handler(){
    		@Override
			public void handleMessage(Message msg) {
    			switch (msg.what){
    			case MSG_REQUERY:
    	    		if (!runningQuery && db.isOpen()){
    	    			changeCursor(getFilterQueryProvider().runQuery(null));
    	    			notifyDataSetInvalidated();
    	    		}
    				break;
    			}
    		};
    	};
    	public UnitCursorAdapter (Activity context, Cursor dbCursor, SQLiteDatabase db, TextView otherEntry){
    		super(context, android.R.layout.simple_dropdown_item_1line,
    				dbCursor,
    				new String[] {UsageEntry._UNIT},
    				new int[] {android.R.id.text1});

    		this.otherEntry = otherEntry;
    		this.mActivity = context;
    		this.db = db;
        	setStringConversionColumn(dbCursor.getColumnIndex(UsageEntry._UNIT));

        	final TextUpdateWatcher tuw = new TextUpdateWatcher(this);
        	otherEntry.addTextChangedListener(tuw);
        	otherEntry.setOnFocusChangeListener(tuw);

        	// a filter that searches for units starting with the given constraint
        	setFilterQueryProvider(new UnitMatcherFilterQueryProvider(db, otherEntry));
    	}

    	public void otherEntryUpdated(){
    		if (!mHandler.hasMessages(MSG_REQUERY)){
    			mHandler.sendEmptyMessage(MSG_REQUERY);
    		}
    	}

    	private Cursor queryWithConforming(SQLiteDatabase db, TextView otherEntry, String selection, String[] selectionArgs){
        	String conformingSelection = null;
        	String[] conformingSelectionArgs = getConformingSelectionArgs(otherEntry);

        	if (selection != null){
        		if (conformingSelectionArgs != null){
    	    		conformingSelection = CONFORMING_SELECTION + " AND " + selection;
    	    		final ArrayList<String> args = new ArrayList<String>();
    	    		args.add(conformingSelectionArgs[0]);
    	    		args.addAll(Arrays.asList(selectionArgs));
    	    		conformingSelectionArgs = args.toArray(new String[]{});
        		}else{
        			conformingSelection = selection;
        			conformingSelectionArgs = selectionArgs;
        		}
        	}else if (conformingSelectionArgs != null){
        		conformingSelection = CONFORMING_SELECTION;
        	}
        	Cursor c = db.query(DB_USAGE_TABLE, null, conformingSelection, conformingSelectionArgs, null, null, USAGE_SORT);
        	// If we don't get anything by conforming, the user may be attempting to ask for
        	// a complex result and we should just return everything.
        	if (c.getCount() == 0){
        		c.close();
        		c = db.query(DB_USAGE_TABLE, null, selection, selectionArgs, null, null, USAGE_SORT);
        	}
        	return c;
    	}

        private class UnitMatcherFilterQueryProvider implements FilterQueryProvider {
        	private final SQLiteDatabase db;
        	// matches a unit being entered in-progress (at the end of the expression)
        	// intentionally does not match single-letter units (what's the point for autocompleting these?)

        	TextView otherEntry;

        	public UnitMatcherFilterQueryProvider(SQLiteDatabase db, TextView otherEntry){
        		this.db = db;
        		this.otherEntry = otherEntry;
        	}


        	public synchronized Cursor runQuery(CharSequence constraint) {
        		runningQuery = true;

        		final String[] selectionArgs = null;
        		final String selection = null;
        		Cursor c = null;

    			if (constraint == null || constraint.length() == 0){
    				c = queryWithConforming(db, otherEntry, selection, selectionArgs);
    			}else{
    				final Matcher m = UNIT_EXTRACT_REGEX.matcher(constraint);
    				String modConstraint;
    				if (m.matches()){
    					modConstraint = m.group(1);
    					c = queryWithConforming(db, otherEntry, UsageEntry._UNIT +" GLOB ?", new String[] {modConstraint+"*"});
    				}else{
    					c = queryWithConforming(db, otherEntry, null, null);
    				}
    			}
    			mActivity.startManagingCursor(c);
    			runningQuery = false;
    			return c;
    		}

        }

        private class TextUpdateWatcher implements TextWatcher, OnFocusChangeListener {
        	private boolean dirty = false;

        	private final UnitCursorAdapter adapter;

        	public TextUpdateWatcher(UnitCursorAdapter adapter) {
        		this.adapter = adapter;
    		}
    		public void afterTextChanged(Editable s) {}

    		public void beforeTextChanged(CharSequence s, int start, int count,
    				int after) {}

    		public void onTextChanged(CharSequence s, int start, int before,
    				int count) {
    			if (s.length() == 0){
    				// change immediately if cleared.
					adapter.otherEntryUpdated();
					dirty = false;
    			}else{
    				dirty = true;
    			}

    		}

    		public void onFocusChange(View v, boolean hasFocus) {
    			if (!hasFocus && dirty){
    				dirty = false;
					adapter.otherEntryUpdated();
    			}

    		}
        }
    }

    private static final Pattern UNIT_REGEX = Pattern.compile("([a-zA-Z]\\w+)");
	private static final Pattern UNIT_EXTRACT_REGEX = Pattern.compile(".*?([a-zA-Z]\\w+)");


	private final static String[] INCREMENT_QUERY_PROJECTION = {UsageEntry._UNIT};
    /**
     * Increments the usage counter for the given unit.
     * @param unit name of the unit
     * @param db the unit usage database
     */
    public static void logUnitUsed(String unit, SQLiteDatabase db){

    	final String[] selectionArgs = {unit};
    	final Cursor c = db.query(DB_USAGE_TABLE, INCREMENT_QUERY_PROJECTION, UsageEntry._UNIT + "=?", selectionArgs, null, null, null);
    	if (c.getCount() > 0){
        	// TODO efficient, but should probably be sanity checked.
    		db.execSQL("UPDATE " + DB_USAGE_TABLE + " SET " + UsageEntry._USE_COUNT + "=" + UsageEntry._USE_COUNT + " + 1 WHERE " + UsageEntry._UNIT + "='" + unit + "'" );
    	}else{
    		final ContentValues cv = new ContentValues();
    		cv.put(UsageEntry._UNIT, unit);
    		cv.put(UsageEntry._USE_COUNT, 1);
    		cv.put(UsageEntry._FACTOR_FPRINT, getFingerprint(unit));
    		db.insert(DB_USAGE_TABLE, null, cv);
    	}
    	c.close();
    }

    public static void logUnitsInExpression(String expression, SQLiteDatabase db){
    	final Matcher m = UNIT_REGEX.matcher(expression);
    	while(m.find()){
    		logUnitUsed(m.group(1), db);
    	}
    }

}
