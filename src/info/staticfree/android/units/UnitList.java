package info.staticfree.android.units;
/*
 * UnitList.java
 * Copyright (C) 2010  Steve Pomeroy <steve@staticfree.info>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
import android.app.ListActivity;
import android.app.SearchManager;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;

/**
 * Displays a list of units, with an optional search query to search a
 * substring of all unit names.
 *
 * @author Steve Pomeroy
 *
 */
public class UnitList extends ListActivity implements OnClickListener {
	private SearchHighlightAdapter adapter;

	/**
	 * Add this to the PICK intent to select from a subset of all units.
	 * Parameter is a string.
	 */
	public static final String
		EXTRA_UNIT_QUERY = "info.staticfree.android.units.EXTRA_UNIT_QUERY";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.unit_list);
		super.onCreate(savedInstanceState);

		getListView().setEmptyView(findViewById(R.id.empty));
		((Button)findViewById(R.id.search)).setOnClickListener(this);

		loadFromIntent(getIntent());

        registerForContextMenu(this.getListView());

	}

	// This exists mostly for the search interaction to allow searching from within this activity.
	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);

		loadFromIntent(intent);
	}

	/**
	 * Load from the given intent. Can be called multiple times in a running activity.
	 *
	 * @param intent
	 */
	private void loadFromIntent(Intent intent){
		final String[] from = {UsageEntry._UNIT, ClassificationEntry._DESCRIPTION};
		final int[] to = {android.R.id.text1, android.R.id.text2};
		final String[] projection = {UsageEntry._ID, UsageEntry._UNIT, ClassificationEntry._DESCRIPTION};

		final Cursor c;
		String query = null;

		final Uri data = UsageEntry.CONTENT_URI_WITH_CLASSIFICATION;

		final Bundle extras = intent.getExtras();
		if (extras.containsKey(SearchManager.QUERY)){
			query = extras.getString(SearchManager.QUERY);
		}else{
			query = extras.getString(EXTRA_UNIT_QUERY);
		}
		if (query != null){

			query = query.toLowerCase();

			final String[] selectionArgs = {"%"+query+"%"};
			c = managedQuery(data, projection, UsageEntry._UNIT+" LIKE ?", selectionArgs, UsageEntry.SORT_DEFAULT);

			setTitle(getString(R.string.search_title_unit, query));
		}else{
			c = managedQuery(data, projection, null, null, UsageEntry.SORT_DEFAULT);
		}

		if (adapter == null){
			adapter = new SearchHighlightAdapter(this, android.R.layout.simple_list_item_2, c, from, to, android.R.id.text1);
			adapter.setStringConversionColumn(c.getColumnIndex(UsageEntry._UNIT));
			setListAdapter(adapter);
		}else{
			adapter.changeCursor(c);
		}

		adapter.setQuery(query);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		final Cursor c = (Cursor) adapter.getItem(position);
		final String unitName = c.getString(c.getColumnIndex(UsageEntry._UNIT));

		final Intent intent = getIntent();
		if (Intent.ACTION_PICK.equals(intent.getAction())){
			pickUnit(ContentUris.withAppendedId(UsageEntry.CONTENT_URI, id), unitName);

		}else{
			viewUnit(ContentUris.withAppendedId(UsageEntry.CONTENT_URI, id), unitName);
		}
	}

	private void pickUnit(Uri unit, String unitName){
		final Intent pickedUnit = new Intent();
		pickedUnit.setData(unit);

		pickedUnit.putExtra(Units.EXTRA_UNIT_NAME, unitName);
		setResult(RESULT_OK, pickedUnit);
		finish();
	}

	private void viewUnit(Uri unit, String unitName){
		final Intent viewUnit = new Intent(Intent.ACTION_VIEW, unit);

		viewUnit.putExtra(Units.EXTRA_UNIT_NAME, unitName);
		startActivity(viewUnit);
	}

	private static final int
		MENU_PICK_UNIT = 0,
		MENU_UNIT_DETAILS = 1;

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);

		if (Intent.ACTION_PICK.equals(getIntent().getAction())){
			menu.add(0, MENU_PICK_UNIT, 0, R.string.menu_pick_unit);
		}
		menu.add(0, MENU_UNIT_DETAILS, 0, R.string.menu_unit_details);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
        final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        final Uri data = ContentUris.withAppendedId(UsageEntry.CONTENT_URI, info.id);

		final Cursor c = (Cursor) adapter.getItem(info.position);
		final String unitName = c.getString(c.getColumnIndex(UsageEntry._UNIT));

        switch (item.getItemId()) {
        case MENU_PICK_UNIT:
        	pickUnit(data, unitName);
        	return true;

        case MENU_UNIT_DETAILS:
        	viewUnit(data, unitName);
        	return true;

    	default:
        		return super.onContextItemSelected(item);
        }
	}

	/**
	 * Highlights the query specified by setQuery(). Will look for occurrences in
	 * TextView specified by setSearchedView()
	 *
	 * @author steve
	 *
	 */
	private class SearchHighlightAdapter extends SimpleCursorAdapter{
		private String query;
		private final int searchedId;

		/**
		 * Set the query string to find within the searchedTextView
		 *
		 * @param query
		 */
		public void setQuery(String query) {
			this.query = query;
		}

		public SearchHighlightAdapter(Context context, int layout, Cursor c,
				String[] from, int[] to, int searchedTextView) {
			super(context, layout, c, from, to);
			this.searchedId = searchedTextView;
		}


		@Override
		public void setViewText(TextView v, String text) {
			if (query != null){
				final int start = text.toLowerCase().indexOf(query);
				if (start >= 0 && v.getId() == searchedId){
					v.setText(Html.fromHtml((start > 0 ? text.substring(0, start): "")
						+ "<b>" + text.substring(start, start + query.length()) + "</b>"
						+(text.length() - start > query.length() ? text.substring(start + query.length()): "")));
				}else{
					super.setViewText(v, text);
				}
			}else{
				super.setViewText(v, text);
			}
		}
	}

	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.search:
				onSearchRequested();
			break;
		}
	}
}
