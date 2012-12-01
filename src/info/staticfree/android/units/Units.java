package info.staticfree.android.units;
/*
 * Units.java
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
import info.staticfree.android.units.ValueGui.ConversionException;
import info.staticfree.android.units.ValueGui.ReciprocalException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

import net.sourceforge.unitsinjava.DefinedFunction;
import net.sourceforge.unitsinjava.EvalError;
import net.sourceforge.unitsinjava.Function;
import net.sourceforge.unitsinjava.Value;

import org.jared.commons.ui.WorkspaceView;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.ClipboardManager;
import android.text.Editable;
import android.text.InputType;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.MultiAutoCompleteTextView;
import android.widget.SimpleCursorAdapter;
import android.widget.SimpleCursorTreeAdapter;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

// TODO high: move category strings to a system that can handle runtime i18n changes. maybe put string refs in the DB?
// TODO high: fix mdpi app icon on Android 1.6
// TODO high: ldpi smaller icon for about
// TODO med: add implicit "last result" eg. "1+1=" then press "+1" to get "3"
// TODO med: auto-ranging for metric units (auto add "kilo-" or "micro-")
// TODO med: allow for returning composite Imperial and time units. eg. "3 hours + 12 minutes" instead of "3.2 hours"
// TODO med: remove soft keyboard for non-touch devices
// TODO med: look into performance bug on ADP device. Slows down considerably when backspacing whole entry.
// TODO med: add date headers for history, to consolidate items ("yesterday", "1 week ago", etc.)
// TODO med: show keyboard icon for 2nd tap (can't do this easily, as one can't detect if soft keyboard is shown or not). May need to scrap this idea.
// TODO low: longpress on unit for description (look in unit addition error message for hints)
// TODO low: Auto-scale text for display (square)
public class Units extends FragmentActivity implements OnClickListener, OnEditorActionListener, OnTouchListener, OnLongClickListener, LoaderCallbacks<Cursor> {
	@SuppressWarnings("unused")
	private final static String TAG = Units.class.getSimpleName();

	private MultiAutoCompleteTextView wantEditText;
	private MultiAutoCompleteTextView haveEditText;
	private TextView resultView;
	private ListView history;
	private LinearLayout historyDrawer;
	private Button historyClose;
	private WorkspaceView workspace;

	private UnitUsageDBHelper unitUsageDBHelper;

    private HistoryAdapter mHistoryAdapter;

    public final static String XMLNS="http://staticfree.info/ns/android/units";

    public final static String
    	ACTION_USE_UNIT = "info.staticfree.android.units.ACTION_USE_UNIT",
    	EXTRA_UNIT_NAME = "info.staticfree.android.units.EXTRA_UNIT_NAME";

    public final static String
    	STATE_RESULT_TEXT = "info.staticfree.android.units.RESULT_TEXT",
    	STATE_DRAWER_OPENED = "info.staticfree.android.units.DRAWER_OPENED",
    	STATE_DIALOG_UNIT_CATEGORY = "info.staticfree.android.units.STATE_DIALOG_UNIT_CATEGORY";

    private static final int REQUEST_PICK_UNIT = 0;

    private static final int
    	LOADER_HISTORY = 100,
    	LOADER_USAGE = 101;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        wantEditText = ((MultiAutoCompleteTextView)findViewById(R.id.want));
        haveEditText = ((MultiAutoCompleteTextView)findViewById(R.id.have));
        defaultInputType = wantEditText.getInputType();
        wantEditText.setOnFocusChangeListener(inputBoxOnFocusChange);
        haveEditText.setOnFocusChangeListener(inputBoxOnFocusChange);

        resultView = ((TextView)findViewById(R.id.result));
        history = ((ListView)findViewById(R.id.history_list));
        historyDrawer = ((LinearLayout)findViewById(R.id.history_drawer));
        historyClose = ((Button)findViewById(R.id.history_close));
        workspace = (WorkspaceView)findViewById(R.id.numpad_switcher);
        //workspace.setTouchSlop(); // XXX scale
        //workspace.setShowTabIndicator(false);

        mHistoryAdapter = new HistoryAdapter(this, null);
        history.setAdapter(mHistoryAdapter);
		// TODO consolidate listeners
		history.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {

            	setCurrentEntry(ContentUris.withAppendedId(HistoryEntry.CONTENT_URI, mHistoryAdapter.getItemId(position)));

            	setHistoryVisible(false);
			}
		});
		history.setOnCreateContextMenuListener(this);

		resultView.setOnClickListener(this);
		resultView.setOnCreateContextMenuListener(this);
		historyClose.setOnClickListener(this);

		// Go through the numberpad and add all the onClick listeners.
		// Make sure to update if the layout changes.
		setGridChildrenListener(((LinearLayout)findViewById(R.id.numberpad)), buttonListener, buttonListener);
		setGridChildrenListener((ViewGroup) findViewById(R.id.numberpad2), buttonListener, buttonListener);

		final View backspace = findViewById(R.id.backspace);
		backspace.setOnClickListener(buttonListener);
		backspace.setOnLongClickListener(buttonListener);

		unitUsageDBHelper = new UnitUsageDBHelper(this);

		final Object instance = getLastNonConfigurationInstance();
		if (instance instanceof LoadInitialUnitUsageTask){
			mLoadInitialUnitUsageTask = (LoadInitialUnitUsageTask) instance;
			mLoadInitialUnitUsageTask.setActivity(this);
		}else{
			if (unitUsageDBHelper.getUnitUsageDbCount() == 0){
	    		mLoadInitialUnitUsageTask = new LoadInitialUnitUsageTask();
	    		mLoadInitialUnitUsageTask.setActivity(this);
	    		mLoadInitialUnitUsageTask.execute();
	    	}
		}

		wantEditText.setOnEditorActionListener(this);

		final UnitsMultiAutoCompleteTokenizer tokenizer = new UnitsMultiAutoCompleteTokenizer();
		haveEditText.setTokenizer(tokenizer);
		wantEditText.setTokenizer(tokenizer);
		haveEditText.setOnTouchListener(this);
		wantEditText.setOnTouchListener(this);

		if (savedInstanceState != null){
			resultView.setText(savedInstanceState.getCharSequence(STATE_RESULT_TEXT));
			setHistoryVisible(savedInstanceState.getBoolean(STATE_DRAWER_OPENED, false), false);
			mDialogUnitCategoryUnit = savedInstanceState.getString(STATE_DIALOG_UNIT_CATEGORY);
		}

		final Intent intent = getIntent();
		handleIntent(intent);

		getSupportLoaderManager().initLoader(LOADER_HISTORY, null, this);
		getSupportLoaderManager().initLoader(LOADER_USAGE, null, this);

    }

    private void handleIntent(Intent intent){
		final String action = intent.getAction();
		if (Intent.ACTION_SEARCH.equals(action)){
			final Intent pickUnit = new Intent(Intent.ACTION_PICK, UsageEntry.CONTENT_URI);
			final String query = intent.getExtras().getString(SearchManager.QUERY);
			pickUnit.putExtra(UnitList.EXTRA_UNIT_QUERY, query);

			startActivityForResult(pickUnit, REQUEST_PICK_UNIT);

		}else if(ACTION_USE_UNIT.equals(action)){
			sendUnitAsSoftKeyboard(intent.getData());
		}
    }

    @Override
    protected void onNewIntent(Intent intent) {
    	super.onNewIntent(intent);
    	handleIntent(intent);
    }

    private UnitUsageDBHelper.UnitCursorAdapter mHaveUsageAdapter;
    private UnitUsageDBHelper.UnitCursorAdapter mWantUsageAdapter;

    @Override
    protected void onStart() {
    	super.onStart();
    	mHaveUsageAdapter = new UnitUsageDBHelper.UnitCursorAdapter(this,
				null, wantEditText);
		haveEditText.setAdapter(mHaveUsageAdapter);

		mWantUsageAdapter = new UnitUsageDBHelper.UnitCursorAdapter(this,
				null, haveEditText);
		wantEditText.setAdapter(mWantUsageAdapter);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
    	super.onSaveInstanceState(outState);
    	outState.putCharSequence(STATE_RESULT_TEXT, resultView.getText());
    	outState.putBoolean(STATE_DRAWER_OPENED, isHistoryVisible());
    	outState.putString(STATE_DIALOG_UNIT_CATEGORY, mDialogUnitCategoryUnit);
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
    	return mLoadInitialUnitUsageTask;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	super.onActivityResult(requestCode, resultCode, data);
    	switch (requestCode){
	    	case REQUEST_PICK_UNIT:{
	    		if (resultCode == RESULT_OK){
	    			sendTextAsSoftKeyboard(data.getExtras().getString(EXTRA_UNIT_NAME) + " ");
	    		}
	    	}break;
    	}
    }

	public Loader<Cursor> onCreateLoader(int id, Bundle args) {

		switch (id){
		case LOADER_HISTORY:
			return new CursorLoader(this, HistoryEntry.CONTENT_URI, HistoryAdapter.PROJECTION, null, null, HistoryEntry.SORT_DEFAULT);
		case LOADER_USAGE:
			return new CursorLoader(this, UsageEntry.CONTENT_URI, null, null, null, UnitUsageDBHelper.USAGE_SORT);
			default:
				throw new IllegalArgumentException("unknown loader ID");
		}
	}

	public void onLoadFinished(Loader<Cursor> loader, Cursor c) {
		switch (loader.getId()){
		case LOADER_HISTORY:
			mHistoryAdapter.swapCursor(c);
			break;

		case LOADER_USAGE:
			mHaveUsageAdapter.swapCursor(c);
			mWantUsageAdapter.swapCursor(c);
		}

	}

	public void onLoaderReset(Loader<Cursor> loader) {
		// TODO Auto-generated method stub

	}

    /**
     * @param vg Given a view group with view groups inside it, set all children to have the same onClickListeners.
     * @param onClickListener
     * @param onLongClickListener
     */
    private void setGridChildrenListener(ViewGroup vg, OnClickListener onClickListener, OnLongClickListener onLongClickListener){
		// Go through the children and add all the onClick listeners.
		// Make sure to update if the layout changes.
		final int rows = vg.getChildCount();
		for (int row = 0; row < rows; row++){
			final ViewGroup v = (ViewGroup)vg.getChildAt(row);
			final int columns = v.getChildCount();
			for (int column = 0; column < columns; column++){
				final View button = v.getChildAt(column);
				button.setOnClickListener(onClickListener);
				button.setOnLongClickListener(onLongClickListener);
			}
		}
    }

    private boolean isHistoryVisible(){
    	return historyDrawer.getVisibility() == View.VISIBLE;
    }

    private void setHistoryVisible(boolean visible){
    	setHistoryVisible(visible, true);
    }
    private void setHistoryVisible(boolean visible, boolean animate){
    	if (visible && historyDrawer.getVisibility() == View.INVISIBLE){
    		if (animate) {
    			historyDrawer.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.history_show));
    		}
    		historyDrawer.setVisibility(View.VISIBLE);
    		historyDrawer.requestFocus();

    	}else if(! visible && historyDrawer.getVisibility() == View.VISIBLE){
			if (animate){
				historyDrawer.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.history_hide));
			}
			historyDrawer.setVisibility(View.INVISIBLE);
    	}
    }

    /**
     * Adds the given entry to the history database.
     *
     * @author steve
     *
     */
    private class AddToHistoryRunnable implements Runnable {
    	private final String haveExpr, wantExpr;
    	private final Double result;

    	public AddToHistoryRunnable(String haveExpr, String wantExpr, Double result) {
    		this.haveExpr = haveExpr;
    		this.wantExpr = wantExpr;
    		this.result = result;
		}
		public void run() {
			final ContentValues cv = new ContentValues();
	    	cv.put(HistoryEntry._HAVE, haveExpr);
	    	cv.put(HistoryEntry._WANT, wantExpr);
	    	cv.put(HistoryEntry._RESULT, result);

	    	getContentResolver().insert(HistoryEntry.CONTENT_URI, cv);

		}
    }

    // TODO make reciprocal notice better animated so it doesn't modify main layout
    public void addToHistory(String haveExpr, String wantExpr, Double result, boolean reciprocal){
    	haveExpr = haveExpr.trim();
    	wantExpr = wantExpr.trim();
    	new AddToUsageTask().execute(haveExpr, wantExpr);
    	haveExpr = reciprocal ? "1÷(" + haveExpr + ")" : haveExpr;
    	resultView.setText(HistoryEntry.toCharSequence(haveExpr, wantExpr, result));

    	// done on a new thread to avoid hanging the UI while the DB is being updated.
    	new Thread(new AddToHistoryRunnable(haveExpr, wantExpr, result)).start();

    	final View reciprocalNotice = findViewById(R.id.reciprocal_notice);
    	if (reciprocal){
    		resultView.requestFocus();

    		reciprocalNotice.setVisibility(View.VISIBLE);
    		reciprocalNotice.startAnimation(AnimationUtils.makeInAnimation(this, true));
    	}else{
    		reciprocalNotice.setVisibility(View.GONE);
    		resultView.setError(null);
    	}
    }


    private final static String[] PROJECTION_LOAD_FROM_HISTORY = {HistoryEntry._HAVE, HistoryEntry._WANT, HistoryEntry._RESULT};

    /**
     * Set the current entries to that of a history entry.
     *
     * @param entry a history entry URI
     */
    private void setCurrentEntry(Uri entry){
    	final Cursor c = getContentResolver().query(entry, PROJECTION_LOAD_FROM_HISTORY, null, null, null);
    	if (c.moveToFirst()){
			setCurrentEntry(c.getString(c.getColumnIndex(HistoryEntry._HAVE)), c.getString(c.getColumnIndex(HistoryEntry._WANT)));
    	}
		c.close();
    }

    private void setCurrentEntry(String have, String want){
		haveEditText.setText(have + " ");// extra space is to prevent auto-complete from triggering.
		wantEditText.setText(want + (want.length() > 0 ? " " : ""));

		haveEditText.requestFocus();
		haveEditText.setSelection(haveEditText.length());
    }

    /**
     * Converts the history entry to a CharSequence.
     *
     * @param entry A history entry URI
     * @return the name of the unit, as a CharSequence
     */
    private CharSequence getEntryAsCharSequence(Uri entry){
    	CharSequence text = null;
    	final Cursor c = getContentResolver().query(entry, PROJECTION_LOAD_FROM_HISTORY, null, null, null);
    	if (c.moveToFirst()){
			text = HistoryEntry.toCharSequence(c, c.getColumnIndex(HistoryEntry._HAVE), c.getColumnIndex(HistoryEntry._WANT), c.getColumnIndex(HistoryEntry._RESULT));
    	}
    	c.close();
    	return text;
    }

    public static HashMap<Character, String> UNICODE_TRANS = new HashMap<Character, String>();
    static {
    	UNICODE_TRANS.put('÷', "/");
    	UNICODE_TRANS.put('×', "*");
    	UNICODE_TRANS.put('÷', "/");
    	UNICODE_TRANS.put('×', "*");
    	UNICODE_TRANS.put('²', "^2");
    	UNICODE_TRANS.put('³', "^3");
    	UNICODE_TRANS.put('⁴', "^4");
    	UNICODE_TRANS.put('−', "-");
    	UNICODE_TRANS.put('µ', "micro");
    	UNICODE_TRANS.put('π', "pi");
    	UNICODE_TRANS.put('Π', "pi");
    	UNICODE_TRANS.put('€', "euro");
    	UNICODE_TRANS.put('¥', "japanyen");
    	UNICODE_TRANS.put('₤', "greatbritainpound");
    	UNICODE_TRANS.put('√', "sqrt");
    	UNICODE_TRANS.put('∛', "cuberoot");
    	UNICODE_TRANS.put('½', "1|2");
    	UNICODE_TRANS.put('⅓', "1|3");
    	UNICODE_TRANS.put('⅔', "2|3");
    	UNICODE_TRANS.put('¼', "1|4");
    	UNICODE_TRANS.put('⅕', "1|5");
    	UNICODE_TRANS.put('⅖', "2|5");
    	UNICODE_TRANS.put('⅗', "3|5");
    	UNICODE_TRANS.put('⅙', "1|6");
    	UNICODE_TRANS.put('⅛', "1|8");
    	UNICODE_TRANS.put('⅜', "3|8");
    	UNICODE_TRANS.put('⅝', "5|8");
    }

    public static String unicodeToAscii(String unicodeInput){
    	final StringBuilder sb = new StringBuilder();
    	final int len = unicodeInput.length();
    	for (int i = 0; i < len; i++){
    		final char c = unicodeInput.charAt(i);
    		final String sub = UNICODE_TRANS.get(c);
    		if (sub != null){
    			sb.append(sub);
    		}else{
    			sb.append(c);
    		}
    	}
    	return sb.toString();
    }

    // TODO filter error messages and output translate to unicode from engine. error msgs and Inifinity → ∞
    public void go(){
    	String haveStr = haveEditText.getText().toString().trim();
    	String wantStr = wantEditText.getText().toString().trim();

    	try {
    		Value have = null;
    		try {
    			if (haveStr.length() == 0){
    				haveEditText.requestFocus();
    				haveEditText.setError(getText(R.string.err_have_empty));
    				return;
    			}
    			haveStr = ValueGui.closeParens(haveStr);
    			have = ValueGui.fromUnicodeString(haveStr);

    		}catch (final EvalError e){
    			haveEditText.requestFocus();
    			haveEditText.setError(e.getLocalizedMessage());
    			return;
    		}

    		Value want = null;
    		Function func = null;
    		try {
    			func = DefinedFunction.table.get(wantStr);
    			if (func == null && wantStr.endsWith("(")){
    				func = DefinedFunction.table.get(wantStr.subSequence(0, wantStr.length()-1));
    			}
    			if (func == null){
    				wantStr = ValueGui.closeParens(wantStr);
    				want = ValueGui.fromUnicodeString(wantStr);
    			}

    		}catch (final EvalError e){
    			wantEditText.requestFocus();
    			wantEditText.setError(e.getLocalizedMessage());
    			return;
    		}

    		Double resultVal;
    		boolean reciprocal = false;

    		try {
    			wantEditText.setError(null);

    			// if no want value is specified, provide a definition.
        		if (wantStr.length() > 0){
        			if (func != null){
        				// functions are a special case and don't have a reciprocal, so the result
        				// is just stored in the wantStr.
        				resultVal = null;
        				wantStr = ValueGui.convertNonInteractive(ValueGui.fromUnicodeString(haveStr), func);
        			}else{
        				resultVal = ValueGui.convertNonInteractive(have,  want);
        			}

        		}else{
        			resultVal = have.factor;
        		     final StringBuffer haveDef = new StringBuffer();

        		      haveDef.append(have.numerator.asString());

        		      if (have.denominator.size()>0) {
        		        haveDef.append(" ÷").append(have.denominator.asString());
        		    }

        			wantStr = haveDef.toString();
        		}


    		} catch (final ReciprocalException re){
    			reciprocal = true;

    			resultVal = ValueGui.convertNonInteractive(re.reciprocal, want);
    		}

    		allClear();

    		addToHistory(haveStr, wantStr, resultVal, reciprocal);

    	} catch (final ConversionException e) {

    		resultView.setText(null);
    		wantEditText.requestFocus();
    		wantEditText.setError(getText(R.string.err_no_conform));
    		return;
    	}
    }

    public void allClear(){
    	haveEditText.getEditableText().clear();
    	haveEditText.setError(null);
		wantEditText.getEditableText().clear();
		wantEditText.setError(null);
		haveEditText.requestFocus();
    }

    public void onClick(View v) {
    	switch (v.getId()){
    	case R.id.result:
    		setHistoryVisible(true);
    		break;

    	case R.id.history_close:
    		setHistoryVisible(false);
    		break;
    	}
    }


	public boolean onLongClick(View v) {
		switch (v.getId()){
		case R.id.result:
			v.showContextMenu();
			return true;
		}
		return false;
	}

	private static final int
		MENU_COPY       = 0,
		MENU_SEND       = 1,
		MENU_USE_RESULT = 2,
		MENU_REEDIT     = 3;

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {

		menu.add(Menu.NONE, MENU_REEDIT, Menu.FIRST, R.string.ctx_menu_reedit);
		menu.add(Menu.NONE, MENU_COPY, Menu.CATEGORY_SYSTEM, android.R.string.copy);
		menu.add(Menu.NONE, MENU_SEND, Menu.CATEGORY_SYSTEM, R.string.ctx_menu_send);
		menu.add(Menu.NONE, MENU_USE_RESULT, Menu.CATEGORY_SECONDARY, R.string.ctx_menu_use_result);

	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		final ContextMenuInfo ctxMenuInfo = item.getMenuInfo();
		int position = history.getCount() - 1;
		if (ctxMenuInfo instanceof AdapterContextMenuInfo){
			position = ((AdapterContextMenuInfo) ctxMenuInfo).position;
		}
		final Uri itemUri = ContentUris.withAppendedId(HistoryEntry.CONTENT_URI, mHistoryAdapter.getItemId(position));

		switch (item.getItemId()){
		case MENU_COPY: {
			final CharSequence itemText = getEntryAsCharSequence(itemUri);
			final ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
			clipboard.setText(itemText);
			Toast.makeText(this, getString(R.string.toast_copy, itemText),
					Toast.LENGTH_SHORT).show();
		} break;

		case MENU_REEDIT: {
			setCurrentEntry(itemUri);
			setHistoryVisible(false);
		}break;

		case MENU_SEND: {
			final CharSequence itemText = getEntryAsCharSequence(itemUri);
			startActivity(Intent.createChooser(
					new Intent(Intent.ACTION_SEND)
						.setType("text/plain")
						.putExtra(Intent.EXTRA_TEXT,
								itemText),
					getText(R.string.ctx_menu_send_title)));
		}break;

		case MENU_USE_RESULT: {
			final Cursor c = getContentResolver().query(itemUri, PROJECTION_LOAD_FROM_HISTORY, null, null, null);
			if (c.moveToFirst()){
				final int resultCol = c.getColumnIndex(HistoryEntry._RESULT);
				setCurrentEntry((c.isNull(resultCol) ? "" : (c.getDouble(resultCol)
							+ " ")) + c.getString(c.getColumnIndex(HistoryEntry._WANT)), "");
				setHistoryVisible(false);
			}
			c.close();
		}break;
		}

		return super.onContextItemSelected(item);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()){
		case R.id.swap_inputs:{
			if (getCurrentFocus() == haveEditText){
				swapInputs(haveEditText, wantEditText);

			}else if (getCurrentFocus() == wantEditText){
				swapInputs(wantEditText, haveEditText);
			}else{
				swapInputs(null, null);
			}
			return true;
		}
		case R.id.about:
			showDialog(DIALOG_ABOUT);
			return true;

		case R.id.show_history:
			setHistoryVisible(true);
			return true;

		case R.id.clear_history:
			getContentResolver().delete(HistoryEntry.CONTENT_URI, null, null);
			resultView.setText(null);
			return true;

		case R.id.search:
			onSearchRequested();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

    /**
     * Read an InputStream into a String until it hits EOF.
     *
     * @param in
     * @return the complete contents of the InputStream
     * @throws IOException
     */
    static public String inputStreamToString(InputStream in) throws IOException{
            final int bufsize = 8196;
            final char[] cbuf = new char[bufsize];

            final StringBuffer buf = new StringBuffer(bufsize);

            final InputStreamReader in_reader = new InputStreamReader(in);

            for (int readBytes = in_reader.read(cbuf, 0, bufsize);
                    readBytes > 0;
                    readBytes = in_reader.read(cbuf, 0, bufsize)) {
                    buf.append(cbuf, 0, readBytes);
            }

            return buf.toString();
    }

    private void sendTextAsSoftKeyboard(String text){
    	dispatchKeyEvent(new KeyEvent(SystemClock.uptimeMillis(), text, Units.class.hashCode(), KeyEvent.FLAG_SOFT_KEYBOARD));
    }

    private void sendTextAsSoftKeyboard(String text, boolean moveToDefault){
    	dispatchKeyEvent(new KeyEvent(SystemClock.uptimeMillis(), text, Units.class.hashCode(), KeyEvent.FLAG_SOFT_KEYBOARD));
    	if (moveToDefault){
    		workspace.moveToDefaultScreen();
    	}
    }

    private void sendUnitAsSoftKeyboard(Uri unit){
		final String[] projection = {UsageEntry._ID, UsageEntry._UNIT};
		final Cursor c = getContentResolver().query(unit, projection, null, null, null);
		if (c.moveToFirst()){
			sendTextAsSoftKeyboard(c.getString(c.getColumnIndex(UsageEntry._UNIT)) + " ");
		}
		c.close();
    }

    private final OnChildClickListener allUnitChildClickListener = new OnChildClickListener() {

		public boolean onChildClick(ExpandableListView parent, View v,
				int groupPosition, int childPosition, long id) {
			sendTextAsSoftKeyboard(((TextView)v).getText().toString() + " ");
			dismissDialog(DIALOG_ALL_UNITS);
			return true;
		}
	};

	private final DialogInterface.OnClickListener dialogUnitCategoryOnClickListener = new DialogInterface.OnClickListener() {

		public void onClick(DialogInterface dialog, int which) {

			sendUnitAsSoftKeyboard(ContentUris.withAppendedId(UsageEntry.CONTENT_URI, dialogUnitCategoryList.getItemId(which)));
			workspace.moveToDefaultScreen();
		}
	};

	private static final int
		DIALOG_ABOUT = 0,
		DIALOG_ALL_UNITS = 1,
		DIALOG_LOADING_UNITS = 2,
		DIALOG_UNIT_CATEGORY = 3;
	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id){
		case DIALOG_ABOUT:{
            final Builder builder = new AlertDialog.Builder(this);

            builder.setTitle(R.string.dialog_about_title);
            builder.setIcon(R.drawable.icon);

            try {
                final WebView wv = new WebView(this);
				final InputStream is = getAssets().open("README.xhtml");
				wv.loadDataWithBaseURL("file:///android_asset/", inputStreamToString(is), "application/xhtml+xml", "utf-8", null);
				wv.setBackgroundColor(0);
				builder.setView(wv);
			} catch (final IOException e) {
				builder.setMessage(R.string.err_no_load_about);
				e.printStackTrace();
			}

            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface dialog, int which) {
                            setResult(RESULT_OK);
                    }
            });
            return builder.create();
		}

		case DIALOG_ALL_UNITS:{
			final Builder b = new Builder(Units.this);
			b.setTitle(R.string.dialog_all_units_title);
			final ExpandableListView unitExpandList = new ExpandableListView(Units.this);
			unitExpandList.setId(android.R.id.list);
			final String[] groupProjection = {UsageEntry._ID, UsageEntry._UNIT, UsageEntry._FACTOR_FPRINT};
			// any selection below will select from the grouping description
			final Cursor cursor = managedQuery(UsageEntry.CONTENT_URI_CONFORM_TOP, groupProjection, null, null, UnitUsageDBHelper.USAGE_SORT);

			unitExpandList.setAdapter(new UnitsExpandableListAdapter(cursor, this,
					android.R.layout.simple_expandable_list_item_1, android.R.layout.simple_expandable_list_item_1,
					new String[] {UsageEntry._UNIT},
	                new int[] {android.R.id.text1},
	                new String[] {UsageEntry._UNIT}, new int[] {android.R.id.text1}));
			unitExpandList.setCacheColorHint(0);
			unitExpandList.setOnChildClickListener(allUnitChildClickListener);
			b.setView(unitExpandList);
			return b.create();
		}

		case DIALOG_UNIT_CATEGORY:{
			final Builder b = new Builder(new ContextThemeWrapper(this, android.R.style.Theme_Black));
			final String[] from = {UsageEntry._UNIT};
			final int[] to = {android.R.id.text1};
			b.setTitle("all units");
			final String[] projection = {UsageEntry._ID, UsageEntry._UNIT, UsageEntry._FACTOR_FPRINT};
			final Cursor c = managedQuery(UsageEntry.CONTENT_URI, projection, null, null, UnitUsageDBHelper.USAGE_SORT);
			dialogUnitCategoryList = new SimpleCursorAdapter(this, android.R.layout.select_dialog_item, c, from, to);
			b.setAdapter(dialogUnitCategoryList, dialogUnitCategoryOnClickListener);

			return b.create();
		}

		case DIALOG_LOADING_UNITS:{
			final ProgressDialog pd = new ProgressDialog(this);
			pd.setIndeterminate(true);
			pd.setTitle(R.string.app_name);
			pd.setMessage(getText(R.string.dialog_loading_units));
			return pd;
		}

		default:
			throw new IllegalArgumentException("Unknown dialog ID:" +id);
		}
	}

	private String mDialogUnitCategoryUnit = "m";
	private SimpleCursorAdapter dialogUnitCategoryList;
	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		switch (id){
		case DIALOG_UNIT_CATEGORY:{
			dialog.setTitle(mDialogUnitCategoryUnit);

			final String[] projection = {UsageEntry._ID, UsageEntry._UNIT, UsageEntry._FACTOR_FPRINT};
			final Cursor c = managedQuery(UsageEntry.getEntriesMatchingFprint(UnitUsageDBHelper.getFingerprint(mDialogUnitCategoryUnit)),
					projection, null, null, UnitUsageDBHelper.USAGE_SORT);
			dialogUnitCategoryList.changeCursor(c);
			final ListView lv = ((AlertDialog)dialog).getListView();
			lv.setSelectionFromTop(0, 0);

		}break;
		default:
			super.onPrepareDialog(id, dialog);
		}

	}

	private void swapInputs(EditText focused, EditText unfocused){

		final Editable e = wantEditText.getText();
		int start = 0, end = 0;
		if (focused != null){
			start = focused.getSelectionStart();
			end   = focused.getSelectionEnd();
		}

		wantEditText.setText(haveEditText.getText());
		haveEditText.setText(e);
		if (unfocused != null){
			unfocused.requestFocus();
			unfocused.setSelection(start, end);
		}
	}

    public class UnitsExpandableListAdapter extends SimpleCursorTreeAdapter {
    	private final int factorFprintColumn;
    	private final String[] childProjection = {UsageEntry._ID, UsageEntry._UNIT};

        public UnitsExpandableListAdapter(Cursor cursor, Activity context, int groupLayout,
                int childLayout, String[] groupFrom, int[] groupTo, String[] childrenFrom,
                int[] childrenTo) {

            super(context, cursor, groupLayout, groupFrom, groupTo, childLayout, childrenFrom,
                    childrenTo);
            factorFprintColumn = cursor.getColumnIndex(UsageEntry._FACTOR_FPRINT);
        }

        @Override
        protected Cursor getChildrenCursor(Cursor groupCursor) {
            // Given the group, we return a cursor for all the children within that group.
        	final String factorFprint = groupCursor.getString(factorFprintColumn);

        	final String[] selectionArgs = {factorFprint};
        	return managedQuery(UsageEntry.CONTENT_URI, childProjection, UsageEntry._FACTOR_FPRINT+"=?", selectionArgs, UnitUsageDBHelper.USAGE_SORT);
        }
    }

	private final ButtonEventListener buttonListener = new ButtonEventListener();
	private class ButtonEventListener implements OnClickListener, OnLongClickListener {

		public void onClick(View v) {

			switch (v.getId()){
			case R.id.backspace:{
				dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
				dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL));

			} break;

			case R.id.equal:
				go();
				break;

			case R.id.unit_entry:{
				final View currentFocus = getCurrentFocus();
				if (currentFocus instanceof MultiAutoCompleteTextView){
					// 2000 is just a magic number that is less than the total number of units,
					// but greater than the number of possibly conforming units. Discovered empirically.
					if (((MultiAutoCompleteTextView) currentFocus).getAdapter().getCount() > 2000){
						onSearchRequested();
					}else{
						((MultiAutoCompleteTextView) currentFocus).setError(null);
						((MultiAutoCompleteTextView)currentFocus).showDropDown();
					}
				}

			}break;

			case R.id.length:
				mDialogUnitCategoryUnit = "m";
				showDialog(DIALOG_UNIT_CATEGORY);
				break;
			case R.id.weight:
				mDialogUnitCategoryUnit = "g";
				showDialog(DIALOG_UNIT_CATEGORY);
				break;
			case R.id.time:
				mDialogUnitCategoryUnit = "hr";
				showDialog(DIALOG_UNIT_CATEGORY);
				break;

			// functions
			case R.id.sin:
			case R.id.cos:
			case R.id.tan:
			case R.id.atan:
			case R.id.log:
			case R.id.ln:
				sendTextAsSoftKeyboard(((Button)v).getText().toString() + "( ", true);
				break;

			// constants
			case R.id.pi:
			case R.id.light:
			case R.id.energy:
				sendTextAsSoftKeyboard(((Button)v).getText().toString() + " ", true);
				break;

			case R.id.square:
				sendTextAsSoftKeyboard("² ", true);
				break;

			case R.id.cube:
				sendTextAsSoftKeyboard("³ ", true);
				break;

			case R.id.milli:
			case R.id.kilo:
			case R.id.centi:{
				String prefix = ((Button)v).getText().toString();
				prefix = prefix.substring(0, prefix.length() - 1);
				sendTextAsSoftKeyboard(prefix, false);
			}break;


			default:
				sendTextAsSoftKeyboard(((Button)v).getText().toString(), true);
			}
		}

		public boolean onLongClick(View v) {
			switch (v.getId()){
			case R.id.unit_entry:{
				showDialog(DIALOG_ALL_UNITS);
				return true;
			}
				case R.id.power:{
					sendTextAsSoftKeyboard("E");
					return true;
				}
				case R.id.div:{
					sendTextAsSoftKeyboard("|");
					return true;
				}
				case R.id.backspace:{
					final View currentFocus = getCurrentFocus();
					if (currentFocus instanceof EditText){
						((EditText)currentFocus).getEditableText().clear();
						((EditText)currentFocus).setError(null);
					}
					return true;
				}
			}
			return false;
		}
	}

	public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
		switch (v.getId()){
			case R.id.want:
				go();
				final InputMethodManager imm = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(v.getWindowToken(), 0);


				return true;
		}
		return false;
	};

	private int defaultInputType;
	// make sure to reset the input type when losing focus.
	private final OnFocusChangeListener inputBoxOnFocusChange = new OnFocusChangeListener() {
		public void onFocusChange(View v, boolean hasFocus) {
			if (!hasFocus){
				((MultiAutoCompleteTextView)v).setInputType(defaultInputType);
			}
		}
	};
	public boolean onTouch(View v, MotionEvent event) {
		switch (v.getId()){

		// this is used to prevent the first touch on these editors from triggering the IME soft keyboard.
		case R.id.want:
		case R.id.have:
			if (event.getAction() == MotionEvent.ACTION_DOWN){
				final EditText editor = (EditText)v;
				if (v.hasFocus()){
					editor.setInputType(defaultInputType);
					v.requestFocus();
					return false;

				}

				editor.setInputType(InputType.TYPE_NULL);

			}
		}

		return false;
	}

	/**
	 * Add the units in the given expression(s) to the usage database.
	 * @author steve
	 *
	 */
	private class AddToUsageTask extends AsyncTask<String, Void, Void>{

		@Override
		protected Void doInBackground(String... params) {
			for (final String param: params){
				UnitUsageDBHelper.logUnitsInExpression(param, getContentResolver());
			}
			return null;
		}
	}

	private LoadInitialUnitUsageTask mLoadInitialUnitUsageTask;

	/**
	 * Load the initial usage data on the first run of the application.
	 *
	 * @author steve
	 *
	 */
	private class LoadInitialUnitUsageTask extends AsyncTask<Void, Void, Void>{
		private Activity mActivity;
		public void setActivity(Activity activity){
			mActivity = activity;
		}
		@Override
		protected void onPreExecute() {
			showDialog(DIALOG_LOADING_UNITS);
		}
		@Override
		protected Void doInBackground(Void... params) {
			unitUsageDBHelper.loadInitialUnitUsage();
			unitUsageDBHelper.loadUnitClassifications();

			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			try {
				if (mActivity != null){
					mActivity.dismissDialog(DIALOG_LOADING_UNITS);
				}
			}catch (final IllegalArgumentException ie){
				// it's alright if it was dismissed already.
			}
			mLoadInitialUnitUsageTask = null;
		}
	}
}
