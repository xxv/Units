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
    	TYPE_HISTORY_ENTRY_DIR  = "vnd.android.cursor.dir/vnd.info.staticfree.android.units.history_entry";

	private final static int
		MATCHER_HISTORY_ENTRY_ITEM = 1,
		MATCHER_HISTORY_ENTRY_DIR  = 2;

    private static UriMatcher uriMatcher;
    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(AUTHORITY, HistoryEntry.PATH, MATCHER_HISTORY_ENTRY_DIR);
        uriMatcher.addURI(AUTHORITY, HistoryEntry.PATH+"/#", MATCHER_HISTORY_ENTRY_ITEM);
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

	@Override
	public boolean onCreate() {
        dbHelper = new DatabaseHelper(getContext());
        return true;
	}



	@Override
	public String getType(Uri uri) {
		switch (uriMatcher.match(uri)){
		case MATCHER_HISTORY_ENTRY_DIR:
			return TYPE_HISTORY_ENTRY_DIR;
		case MATCHER_HISTORY_ENTRY_ITEM:
			return TYPE_HISTORY_ENTRY_ITEM;

        default:
                throw new IllegalArgumentException("Cannot get type for URI "+uri);
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues cv) {
		final SQLiteDatabase db = dbHelper.getWritableDatabase();
		Uri newItem;
		switch (uriMatcher.match(uri)){
		case MATCHER_HISTORY_ENTRY_DIR:
			newItem = ContentUris.withAppendedId(HistoryEntry.CONTENT_URI, db.insert(HISTORY_ENTRY_TABLE_NAME, null, cv));
			break;
        default:
                throw new IllegalArgumentException("Cannot insert into "+uri);
		}
		getContext().getContentResolver().notifyChange(uri, null);
		return newItem;
	}



	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		final SQLiteDatabase db = dbHelper.getWritableDatabase();

        final SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        long id;
        Cursor c;
		switch (uriMatcher.match(uri)){
		case MATCHER_HISTORY_ENTRY_DIR:
			qb.setTables(HISTORY_ENTRY_TABLE_NAME);
			c = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);
			break;

		case MATCHER_HISTORY_ENTRY_ITEM:
			qb.setTables(HISTORY_ENTRY_TABLE_NAME);
            id = ContentUris.parseId(uri);
            qb.appendWhere(HistoryEntry._ID + "="+id);
			c = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);
			break;

			default:
				throw new IllegalArgumentException("Cannot query "+uri);
		}

        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {

		final SQLiteDatabase db = dbHelper.getWritableDatabase();
		int numUpdated;
		switch (uriMatcher.match(uri)){
		case MATCHER_HISTORY_ENTRY_DIR:
			numUpdated = db.update(HISTORY_ENTRY_TABLE_NAME, values, selection, selectionArgs);
			break;

		case MATCHER_HISTORY_ENTRY_ITEM:
			final long id = ContentUris.parseId(uri);
			if (selection != null){
				selection = "("+selection+")"+ " AND "+HistoryEntry._ID+"="+id+"";
			}else{
				selection = HistoryEntry._ID+"="+id;
			}
			numUpdated = db.update(HISTORY_ENTRY_TABLE_NAME, values, selection, selectionArgs);
			break;

		default:
			throw new IllegalArgumentException("Cannot update "+uri);
		}
		getContext().getContentResolver().notifyChange(uri, null);
		return numUpdated;
	}

	@Override
	public int delete(Uri uri, String where, String[] whereArgs) {
		final SQLiteDatabase db = dbHelper.getWritableDatabase();
		int numDeleted;
		switch (uriMatcher.match(uri)){
		case MATCHER_HISTORY_ENTRY_DIR:
			numDeleted = db.delete(HISTORY_ENTRY_TABLE_NAME, where, whereArgs);
			break;

		case MATCHER_HISTORY_ENTRY_ITEM:
			final long id = ContentUris.parseId(uri);
			if (where != null){
				where = "("+where+")"+ " AND "+HistoryEntry._ID+"="+id+"";
			}else{
				where = HistoryEntry._ID+"="+id;
			}
			numDeleted = db.delete(HISTORY_ENTRY_TABLE_NAME, where, whereArgs);
			break;

		default:
			throw new IllegalArgumentException("Cannot delete "+uri);
		}
		getContext().getContentResolver().notifyChange(uri, null);
		return numDeleted;
	}

}
