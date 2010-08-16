package info.staticfree.android.units;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

public class UnitsContentProvider extends ContentProvider {
	public final static String AUTHORITY = "info.staticfree.android.units";
	public final static String
    	TYPE_HISTORY_ENTRY_ITEM = "vnd.android.cursor.item/vnd.info.staticfree.android.units.history_entry",
    	TYPE_HISTORY_ENTRY_DIR  = "vnd.android.cursor.dir/vnd.info.staticfree.android.units.history_entry",
    	TYPE_UNIT_USAGE_ITEM    = "vnd.android.cursor.item/vnd.info.staticfree.android.units.unit_usage",
    	TYPE_UNIT_USAGE_DIR     = "vnd.android.cursor.dir/vnd.info.staticfree.android.units.unit_usage";

	private final static int
		MATCHER_HISTORY_ENTRY_ITEM = 1,
		MATCHER_HISTORY_ENTRY_DIR  = 2,
		MATCHER_UNIT_USAGE_ITEM    = 3,
		MATCHER_UNIT_USAGE_DIR     = 4;

    private static UriMatcher uriMatcher;
    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(AUTHORITY, HistoryEntry.PATH, MATCHER_HISTORY_ENTRY_DIR);
        uriMatcher.addURI(AUTHORITY, HistoryEntry.PATH+"/#", MATCHER_HISTORY_ENTRY_ITEM);

        uriMatcher.addURI(AUTHORITY, UsageEntry.PATH, MATCHER_UNIT_USAGE_DIR);
        uriMatcher.addURI(AUTHORITY, UsageEntry.PATH + "/#", MATCHER_UNIT_USAGE_ITEM);
    }

	public final static String
		HISTORY_ENTRY_TABLE_NAME = "history";

	private static class DatabaseHelper extends SQLiteOpenHelper {
		private final static String DB_NAME = "content.db";
		private final static int DB_VER = 1;


        public DatabaseHelper(Context context) {
        	super(context, DB_NAME, null, DB_VER);
        }

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE "+ HISTORY_ENTRY_TABLE_NAME + " (" +
				HistoryEntry._ID + " INTEGER PRIMARY KEY," +
				HistoryEntry._HAVE + " TEXT," +
				HistoryEntry._WANT + " TEXT," +
				HistoryEntry._RESULT + " REAL," +
				HistoryEntry._WHEN + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
			");");

		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL("DROP TABLE IF EXISTS " + HISTORY_ENTRY_TABLE_NAME);

		}

	}

    private DatabaseHelper dbHelper;
    private UnitUsageDBHelper unitDbHelper;

	@Override
	public boolean onCreate() {
        dbHelper = new DatabaseHelper(getContext());
        unitDbHelper = new UnitUsageDBHelper(getContext());
        return true;
	}



	@Override
	public String getType(Uri uri) {
		switch (uriMatcher.match(uri)){
		case MATCHER_HISTORY_ENTRY_DIR:
			return TYPE_HISTORY_ENTRY_DIR;
		case MATCHER_HISTORY_ENTRY_ITEM:
			return TYPE_HISTORY_ENTRY_ITEM;

		case MATCHER_UNIT_USAGE_DIR:
			return TYPE_UNIT_USAGE_DIR;
		case MATCHER_UNIT_USAGE_ITEM:
			return TYPE_UNIT_USAGE_ITEM;

        default:
                throw new IllegalArgumentException("Cannot get type for URI "+uri);
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues cv) {

		Uri newItem;
		switch (uriMatcher.match(uri)){
		case MATCHER_HISTORY_ENTRY_DIR:{
			final SQLiteDatabase db = dbHelper.getWritableDatabase();
			newItem = ContentUris.withAppendedId(HistoryEntry.CONTENT_URI, db.insert(HISTORY_ENTRY_TABLE_NAME, null, cv));
		}break;

		case MATCHER_UNIT_USAGE_DIR:{
			final SQLiteDatabase db = unitDbHelper.getWritableDatabase();
			newItem = ContentUris.withAppendedId(UsageEntry.CONTENT_URI, db.insert(UnitUsageDBHelper.DB_USAGE_TABLE, null, cv));
		}
        default:
                throw new IllegalArgumentException("Cannot insert into "+uri);
		}
		getContext().getContentResolver().notifyChange(uri, null);
		return newItem;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {

        long id;
        Cursor c;
		switch (uriMatcher.match(uri)){
		case MATCHER_HISTORY_ENTRY_DIR:{
			final SQLiteDatabase db = dbHelper.getWritableDatabase();
	        final SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

			qb.setTables(HISTORY_ENTRY_TABLE_NAME);
			c = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);
		}break;

		case MATCHER_HISTORY_ENTRY_ITEM:{
			final SQLiteDatabase db = dbHelper.getWritableDatabase();
	        final SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

			qb.setTables(HISTORY_ENTRY_TABLE_NAME);
            id = ContentUris.parseId(uri);
            qb.appendWhere(HistoryEntry._ID + "="+id);
			c = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);
		}break;

		case MATCHER_UNIT_USAGE_DIR:{
			final SQLiteDatabase db = unitDbHelper.getWritableDatabase();
	        final SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
	        qb.setTables(UnitUsageDBHelper.DB_USAGE_TABLE);

	        c = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);
		}break;

		case MATCHER_UNIT_USAGE_ITEM:{
			final SQLiteDatabase db = unitDbHelper.getWritableDatabase();
	        final SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
	        qb.setTables(UnitUsageDBHelper.DB_USAGE_TABLE);
            id = ContentUris.parseId(uri);
            qb.appendWhere(UsageEntry._ID + "="+id);
	        c = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);
		}break;

			default:
				throw new IllegalArgumentException("Cannot query "+uri);
		}

        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {

		int numUpdated;
		switch (uriMatcher.match(uri)){
		case MATCHER_HISTORY_ENTRY_DIR:{
			final SQLiteDatabase db = dbHelper.getWritableDatabase();
			numUpdated = db.update(HISTORY_ENTRY_TABLE_NAME, values, selection, selectionArgs);
		}break;

		case MATCHER_HISTORY_ENTRY_ITEM:{
			final SQLiteDatabase db = dbHelper.getWritableDatabase();

			final long id = ContentUris.parseId(uri);
			if (selection != null){
				selection = "("+selection+")"+ " AND "+HistoryEntry._ID+"="+id+"";
			}else{
				selection = HistoryEntry._ID+"="+id;
			}
			numUpdated = db.update(HISTORY_ENTRY_TABLE_NAME, values, selection, selectionArgs);
		}break;

		case MATCHER_UNIT_USAGE_DIR:{
			final SQLiteDatabase db = unitDbHelper.getWritableDatabase();
			numUpdated = db.update(UnitUsageDBHelper.DB_USAGE_TABLE, values, selection, selectionArgs);
		}break;

		case MATCHER_UNIT_USAGE_ITEM:{
			final SQLiteDatabase db = unitDbHelper.getWritableDatabase();

			final long id = ContentUris.parseId(uri);
			if (selection != null){
				selection = "("+selection+")"+ " AND "+UsageEntry._ID+"="+id+"";
			}else{
				selection = UsageEntry._ID+"="+id;
			}
			numUpdated = db.update(UnitUsageDBHelper.DB_USAGE_TABLE, values, selection, selectionArgs);
		}break;

		default:
			throw new IllegalArgumentException("Cannot update "+uri);
		}
		getContext().getContentResolver().notifyChange(uri, null);
		return numUpdated;
	}

	@Override
	public int delete(Uri uri, String where, String[] whereArgs) {

		int numDeleted;
		switch (uriMatcher.match(uri)){
		case MATCHER_HISTORY_ENTRY_DIR:{
			final SQLiteDatabase db = dbHelper.getWritableDatabase();

			numDeleted = db.delete(HISTORY_ENTRY_TABLE_NAME, where, whereArgs);
		}break;

		case MATCHER_HISTORY_ENTRY_ITEM:{
			final SQLiteDatabase db = dbHelper.getWritableDatabase();
			final long id = ContentUris.parseId(uri);
			if (where != null){
				where = "("+where+")"+ " AND "+HistoryEntry._ID+"="+id+"";
			}else{
				where = HistoryEntry._ID+"="+id;
			}
			numDeleted = db.delete(HISTORY_ENTRY_TABLE_NAME, where, whereArgs);
		}break;

		case MATCHER_UNIT_USAGE_DIR:{
			final SQLiteDatabase db = unitDbHelper.getWritableDatabase();

			numDeleted = db.delete(UnitUsageDBHelper.DB_USAGE_TABLE, where, whereArgs);
		}break;

		case MATCHER_UNIT_USAGE_ITEM:{
			final SQLiteDatabase db = unitDbHelper.getWritableDatabase();
			final long id = ContentUris.parseId(uri);
			if (where != null){
				where = "("+where+")"+ " AND "+UsageEntry._ID+"="+id+"";
			}else{
				where = UsageEntry._ID+"="+id;
			}
			numDeleted = db.delete(UnitUsageDBHelper.DB_USAGE_TABLE, where, whereArgs);
		}break;

		default:
			throw new IllegalArgumentException("Cannot delete "+uri);
		}
		getContext().getContentResolver().notifyChange(uri, null);
		return numDeleted;
	}

}
