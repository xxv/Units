package info.staticfree.android.units;
/*
 * UnitDetails.java
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
import android.app.Activity;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.AdapterView.OnItemClickListener;

public class UnitDetails extends Activity {
	final String[] usageEntryProjection = {UsageEntry._ID, UsageEntry._UNIT, UsageEntry._FACTOR_FPRINT};

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		setContentView(R.layout.unit_details);
		super.onCreate(savedInstanceState);

		final Intent intent = getIntent();
		final String action = intent.getAction();



		if (Intent.ACTION_VIEW.equals(action)){
			final Cursor c = managedQuery(intent.getData(), usageEntryProjection, null, null, UsageEntry.SORT_DEFAULT);
			if (c.moveToFirst()){
				loadFromCursor(c);
			}else{
				Log.e("UnitDetails", "Could not load "+intent.getDataString());
				finish();
			}
		}

	}

	private void loadFromCursor(Cursor c){
		final String unitName = c.getString(c.getColumnIndex(UsageEntry._UNIT));
		final String factorFprint = c.getString(c.getColumnIndex(UsageEntry._FACTOR_FPRINT));

		final Cursor classification  = managedQuery(ClassificationEntry.getFprintUri(factorFprint), ClassificationEntry.PROJECTION, null, null, null);
		if (classification.moveToFirst()){
			final String classificationName = classification.getString(classification.getColumnIndex(ClassificationEntry._DESCRIPTION));
			setTitle(classificationName+": "+unitName);
		}else{
			setTitle("Unit: "+unitName);
		}

		final String[] conformingSelectionArgs = {factorFprint};
		final Cursor conforming = managedQuery(UsageEntry.CONTENT_URI, usageEntryProjection, UsageEntry._FACTOR_FPRINT+"=?", conformingSelectionArgs, UsageEntry.SORT_DEFAULT);

		final String[] from ={UsageEntry._UNIT};
		final int[] to = {android.R.id.text1};
		final ListView conformable = ((ListView)findViewById(R.id.conformable));
		conformable.setAdapter(new SimpleCursorAdapter(this, android.R.layout.simple_list_item_1, conforming, from, to));
		conformable.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long id) {
				startActivity(new Intent(Intent.ACTION_VIEW, ContentUris.withAppendedId(UsageEntry.CONTENT_URI, id)));

			}
		});;
	}
}
