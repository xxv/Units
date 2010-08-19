package info.staticfree.android.units;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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

import org.json.JSONException;
import org.json.JSONObject;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
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

	// TODO add a preference that remembers the last loaded version. Load new
	// units and fingerprints.
	private static final String UNITS_DAT_VERSION = "1.50";
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

	public int getUnitUsageDbCount(){
		final SQLiteDatabase db = getReadableDatabase();
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

	@SuppressWarnings("unchecked")
	public HashMap<String,String> loadFingerprints(){
		final HashMap<String, String> fingerprints = new HashMap<String, String>();
		try {
			final JSONObject fprints = loadJsonObjectFromRawResource(context, R.raw.fingerprints);
			for (final Iterator i = fprints.keys(); i.hasNext(); ){
				final String key = (String) i.next();
				fingerprints.put(key, fprints.optString(key));
			}
		} catch (final Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return fingerprints;
	}


	public static String getFingerprint(String unitName){

		String fpr = null;
		try {
			final Value unit = ValueGui.fromUnicodeString(unitName);

			fpr = ValueGui.getFingerprint(unit);
		}catch (final EvalError e){
			// skip things we can't handle
		}
		return fpr;
	}

	public void loadInitialUnitUsage(){
		final SQLiteDatabase db = getWritableDatabase();
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

		final HashMap<String,String> fingerprints = loadFingerprints();

		db.beginTransaction();
		for (final String unitName: sortedUnits){
			cv.put(UsageEntry._UNIT, unitName);
			cv.put(UsageEntry._USE_COUNT, allUnitWeights.get(unitName));

			final String fpr = fingerprints.containsKey(unitName) ? fingerprints.get(unitName) : getFingerprint(unitName);

			fingerprints.put(unitName, fpr);
			cv.put(UsageEntry._FACTOR_FPRINT, fpr);
			db.insert(DB_USAGE_TABLE, null, cv);
		}
		db.setTransactionSuccessful();
		db.endTransaction();

		context.getContentResolver().notifyChange(UsageEntry.CONTENT_URI, null);

		// If we have the right permissons, save the fingerprints file to a JSON file
		// which can then be imported into the app to speed up initial fingerpint loading.
		if (context.checkCallingOrSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
			final File externalStorage = Environment.getExternalStorageDirectory();
			final File fprintsOutput = new File(externalStorage, "units_fingerprints.json");
			final JSONObject jo = new JSONObject(fingerprints);
			try {
				final FileWriter fw = new FileWriter(fprintsOutput);
				fw.write(jo.toString(1));
				fw.close();
				Log.i(TAG, "fingerprints written to: "+fprintsOutput.getCanonicalPath());

			} catch (final Exception e) {
				e.printStackTrace();
			}
		}
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
		try{

			final JSONObject jo = loadJsonObjectFromRawResource(context, resourceId);

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

	private static JSONObject loadJsonObjectFromRawResource(Context context, int resourceId) throws IOException, JSONException{
		final InputStream is = context.getResources().openRawResource(resourceId);

		final StringBuilder jsonString = new StringBuilder();

		for (final BufferedReader isReader = new BufferedReader(new InputStreamReader(is), 16000);
		isReader.ready();){
			jsonString.append(isReader.readLine());
		}
		return new JSONObject(jsonString.toString());
	}


	public final static String USAGE_SORT =  UsageEntry._USE_COUNT + " DESC, "+UsageEntry._UNIT + " ASC";
	private static final String CONFORMING_SELECTION = UsageEntry._FACTOR_FPRINT + " = ?";

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
				final String[] conformingSelectionArgs = {ValueGui.getFingerprint(ValueGui.fromUnicodeString(otherEntryText))};
				cachedEntryText = otherEntryText;
				cachedEntryFprintArgs = conformingSelectionArgs;
				return conformingSelectionArgs;
			}catch (final EvalError e){
				return null;
			}
		}
		return null;
	}

	public static class UnitCursorAdapter extends SimpleCursorAdapter {

		private static final int MSG_REQUERY = 0;
		private boolean runningQuery;
		private final Activity mActivity;
		private final Handler mHandler = new Handler(){
			private int retryCount = 0;
			@Override
			public void handleMessage(Message msg) {
				switch (msg.what){
				case MSG_REQUERY:
					if (!runningQuery){
						changeCursor(getFilterQueryProvider().runQuery(null));
						notifyDataSetInvalidated();
						retryCount = 0;
					}else{
						// XXX hack to work around race condition when clearing
						// both fields at the same time.
						if (retryCount < 2){
							mHandler.sendEmptyMessageDelayed(MSG_REQUERY, 100);
							retryCount++;
						}
					}
					break;
				}
			};
		};
		public UnitCursorAdapter (Activity context, Cursor dbCursor, TextView otherEntry){
			super(context, android.R.layout.simple_dropdown_item_1line,
					dbCursor,
					new String[] {UsageEntry._UNIT},
					new int[] {android.R.id.text1});

			this.mActivity = context;
			setStringConversionColumn(dbCursor.getColumnIndex(UsageEntry._UNIT));

			final TextUpdateWatcher tuw = new TextUpdateWatcher(this);
			otherEntry.addTextChangedListener(tuw);
			otherEntry.setOnFocusChangeListener(tuw);

			// a filter that searches for units starting with the given constraint
			setFilterQueryProvider(new UnitMatcherFilterQueryProvider(otherEntry));
		}

		public synchronized void otherEntryUpdated(){
			if (!mHandler.hasMessages(MSG_REQUERY)){
				mHandler.sendEmptyMessage(MSG_REQUERY);
			}
		}

		private Cursor queryWithConforming(TextView otherEntry, String selection, String[] selectionArgs){
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
			Cursor c = mActivity.managedQuery(UsageEntry.CONTENT_URI, null, conformingSelection, conformingSelectionArgs, USAGE_SORT);
			// If we don't get anything by conforming, the user may be attempting to ask for
			// a complex result and we should just return everything.
			if (c.getCount() == 0){
				c.close();
				c = mActivity.managedQuery(UsageEntry.CONTENT_URI, null, selection, selectionArgs, USAGE_SORT);
			}
			return c;
		}

		private class UnitMatcherFilterQueryProvider implements FilterQueryProvider {
			// matches a unit being entered in-progress (at the end of the expression)
			// intentionally does not match single-letter units (what's the point for autocompleting these?)

			TextView otherEntry;

			public UnitMatcherFilterQueryProvider(TextView otherEntry){
				this.otherEntry = otherEntry;
			}

			public synchronized Cursor runQuery(CharSequence constraint) {
				runningQuery = true;

				final String[] selectionArgs = null;
				final String selection = null;
				Cursor c = null;

				if (constraint == null || constraint.length() == 0){
					c = queryWithConforming(otherEntry, selection, selectionArgs);
				}else{
					final Matcher m = UNIT_EXTRACT_REGEX.matcher(constraint);
					String modConstraint;
					if (m.matches()){
						modConstraint = m.group(1);
						c = queryWithConforming(otherEntry, UsageEntry._UNIT +" GLOB ?", new String[] {modConstraint+"*"});
					}else{
						c = queryWithConforming(otherEntry, null, null);
					}
				}
				runningQuery = false;
				return c;
			}

		}

		private static class TextUpdateWatcher implements TextWatcher, OnFocusChangeListener {
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


	private final static String[] INCREMENT_QUERY_PROJECTION = {UsageEntry._ID, UsageEntry._USE_COUNT, UsageEntry._UNIT};
	/**
	 * Increments the usage counter for the given unit.
	 * @param unit name of the unit
	 * @param db the unit usage database
	 */
	public static void logUnitUsed(String unit, ContentResolver cr){
		final String[] selectionArgs = {unit};
		final Cursor c = cr.query(UsageEntry.CONTENT_URI, INCREMENT_QUERY_PROJECTION, UsageEntry._UNIT + "=?", selectionArgs, null);
		if (c.getCount() > 0){
			c.moveToFirst();
			final int useCount = c.getInt(c.getColumnIndex(UsageEntry._USE_COUNT));
			final int id = c.getInt(c.getColumnIndex(UsageEntry._ID));
			final ContentValues cv = new ContentValues();
			cv.put(UsageEntry._USE_COUNT, useCount+1);

			cr.update(ContentUris.withAppendedId(UsageEntry.CONTENT_URI, id), cv, null, null);
		}else{
			final ContentValues cv = new ContentValues();
			cv.put(UsageEntry._UNIT, unit);
			cv.put(UsageEntry._USE_COUNT, 1);
			cv.put(UsageEntry._FACTOR_FPRINT, getFingerprint(unit));
			cr.insert(UsageEntry.CONTENT_URI, cv);
		}
		c.close();
	}

	public static void logUnitsInExpression(String expression, ContentResolver cr){
		final Matcher m = UNIT_REGEX.matcher(expression);
		while(m.find()){
			logUnitUsed(m.group(1), cr);
		}
	}

}
