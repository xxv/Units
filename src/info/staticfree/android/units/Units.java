package info.staticfree.android.units;

import info.staticfree.android.units.ValueGui.ConversionException;
import info.staticfree.android.units.ValueGui.ReciprocalException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import net.sourceforge.unitsinjava.EvalError;
import net.sourceforge.unitsinjava.Value;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.ClipboardManager;
import android.text.Editable;
import android.text.InputType;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.MultiAutoCompleteTextView;
import android.widget.SimpleCursorTreeAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.TextView.OnEditorActionListener;

// TODO high: add search to pop-up menu
// TODO high: add functions from BuiltInFunctions.table
// TODO high: find more useful button to put in place of swap. Maybe use? Clear? Maybe just keep simple.
// TODO high: redo graphics to better visually integrate with keypad. Maybe go with white-on-black theme?
// TODO high: show keyboard icon for 2nd tap (can't do this easily, as one can't detect if soft keyboard is shown or not). May need to scrap this idea.
// TODO high: fix temperature conversion
// TODO high: fix mdpi app icon on Android 1.6
// TODO high: ldpi smaller icon for about
// TODO high: ldpi fix keyboard
// TODO med: auto-ranging for metric units (auto add "kilo-" or "micro-")
// TODO med: allow for returning composite Imperial and time units. eg. "3 hours + 12 minutes" instead of "3.2 hours"
// TODO med: remove soft keyboard for non-touch devices
// TODO med: look into performance bug on ADP device. Slows down considerably when backspacing whole entry.
// TODO med: add date headers for history, to consolidate items ("yesterday", "1 week ago", etc.)
// TODO med: add function parenthesis auto complete
// TODO low: longpress on unit for description (look in unit addition error message for hints)
// TODO low: Auto-scale text for display (square)
public class Units extends Activity implements OnClickListener, OnEditorActionListener, OnTouchListener, OnLongClickListener {
	@SuppressWarnings("unused")
	private final static String TAG = Units.class.getSimpleName();

	private MultiAutoCompleteTextView wantEditText;
	private MultiAutoCompleteTextView haveEditText;
	private TextView resultView;
	private ListView history;
	private LinearLayout historyDrawer;
	private Button historyClose;
	private LinearLayout numberpad;

	private UnitUsageDBHelper unitUsageDBHelper;

    private HistoryAdapter mHistoryAdapter;

    public final static String STATE_RESULT_TEXT = "info.staticfree.android.units.RESULT_TEXT";

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
        numberpad = ((LinearLayout)findViewById(R.id.numberpad));

        mHistoryAdapter = new HistoryAdapter(this, managedQuery(HistoryEntry.CONTENT_URI, HistoryAdapter.PROJECTION, null, null, HistoryEntry.SORT_DEFAULT));
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
		final int rows = numberpad.getChildCount();
		for (int row = 0; row < rows; row++){
			final ViewGroup v = (ViewGroup)numberpad.getChildAt(row);
			final int columns = v.getChildCount();
			for (int column = 0; column < columns; column++){
				final View button = v.getChildAt(column);
				button.setOnClickListener(buttonListener);
				button.setOnLongClickListener(buttonListener);
			}
		}

		unitUsageDBHelper = new UnitUsageDBHelper(this);

		wantEditText.setOnEditorActionListener(this);

		final UnitsMultiAutoCompleteTokenizer tokenizer = new UnitsMultiAutoCompleteTokenizer();
		haveEditText.setTokenizer(tokenizer);
		wantEditText.setTokenizer(tokenizer);
		haveEditText.setOnTouchListener(this);
		wantEditText.setOnTouchListener(this);

		if (savedInstanceState != null){
			resultView.setText(savedInstanceState.getCharSequence(STATE_RESULT_TEXT));
		}
    }

    @Override
    protected void onStart() {
    	super.onStart();
		haveEditText.setAdapter(new UnitUsageDBHelper.UnitCursorAdapter(this,
				managedQuery(UsageEntry.CONTENT_URI, null, null, null, UnitUsageDBHelper.USAGE_SORT),
				wantEditText));
		wantEditText.setAdapter(new UnitUsageDBHelper.UnitCursorAdapter(this,
				managedQuery(UsageEntry.CONTENT_URI, null, null, null, UnitUsageDBHelper.USAGE_SORT),
				haveEditText));
    }

    @Override
    protected void onResume() {
    	super.onResume();

    	if (unitUsageDBHelper.getUnitUsageDbCount() == 0){
    		new LoadInitialUnitUsageTask().execute();
    	}
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
    	super.onSaveInstanceState(outState);
    	outState.putCharSequence(STATE_RESULT_TEXT, resultView.getText());
    }

    private void setHistoryVisible(boolean visible){
    	if (visible && historyDrawer.getVisibility() == View.INVISIBLE){
    		historyDrawer.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.history_show));
    		historyDrawer.setVisibility(View.VISIBLE);

    	}else if(! visible && historyDrawer.getVisibility() == View.VISIBLE){
			historyDrawer.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.history_hide));
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
    	private final double result;

    	public AddToHistoryRunnable(String haveExpr, String wantExpr, double result) {
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
    public void addToHistory(String haveExpr, String wantExpr, double result, boolean reciprocal){
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

    private void setCurrentEntry(Uri entry){
    	final Cursor c = managedQuery(entry, PROJECTION_LOAD_FROM_HISTORY, null, null, null);
    	if (c.moveToFirst()){
			setCurrentEntry(c.getString(c.getColumnIndex(HistoryEntry._HAVE)), c.getString(c.getColumnIndex(HistoryEntry._WANT)));
			c.close();
    	}else{
    		// why can't we load the history?
    	}
    }

    private void setCurrentEntry(String have, String want){
		haveEditText.setText(have + " ");// extra space is to prevent auto-complete from triggering.
		wantEditText.setText(want + (want.length() > 0 ? " " : ""));

		haveEditText.requestFocus();
		haveEditText.setSelection(haveEditText.length());
    }

    private CharSequence getEntryAsCharSequence(Uri entry){
    	final Cursor c = managedQuery(entry, PROJECTION_LOAD_FROM_HISTORY, null, null, null);
    	if (c.moveToFirst()){

			final CharSequence text = HistoryEntry.toCharSequence(c, c.getColumnIndex(HistoryEntry._HAVE), c.getColumnIndex(HistoryEntry._WANT), c.getColumnIndex(HistoryEntry._RESULT));
			c.close();
			return text;
    	}else{
    		// why can't we load the history?
    		return null;
    	}
    }

    // TODO there's got to be a translate function that's more efficient than this...
    public static String unicodeToAscii(String unicodeInput){
    	return unicodeInput.
    	replaceAll("÷", "/").
    	replaceAll("×", "*").
    	replaceAll("²", "^2").
    	replaceAll("³", "^3").
    	replaceAll("⁴", "^4").
    	replaceAll("−", "-");
    }

    // TODO filter error messages and output translate to unicode from engine. error msgs and Inifinity → ∞
    public void go(){
    	final String haveStr = haveEditText.getText().toString().trim();
    	String wantStr = wantEditText.getText().toString().trim();

    	try {
    		Value have = null;
    		try {
    			if (haveStr.length() == 0){
    				haveEditText.requestFocus();
    				haveEditText.setError(getText(R.string.err_have_empty));
    				return;
    			}
    			have = ValueGui.fromUnicodeString(haveStr);

    		}catch (final EvalError e){
    			haveEditText.requestFocus();
    			haveEditText.setError(e.getLocalizedMessage());
    			return;
    		}

    		Value want = null;
    		try {
    			want = ValueGui.fromUnicodeString(wantStr);

    		}catch (final EvalError e){
    			wantEditText.requestFocus();
    			wantEditText.setError(e.getLocalizedMessage());
    			return;
    		}

    		double resultVal;
    		boolean reciprocal = false;

    		try {
    			wantEditText.setError(null);

    			// if no want value is specified, provide a definition.
        		if (wantStr.length() > 0){
        			resultVal = ValueGui.convertNonInteractive(have,  want);

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
			final Cursor c = managedQuery(itemUri, PROJECTION_LOAD_FROM_HISTORY, null, null, null);
			if (c.moveToFirst()){
				setCurrentEntry(c.getDouble(c.getColumnIndex(HistoryEntry._RESULT))
							+ " " + c.getString(c.getColumnIndex(HistoryEntry._WANT)), "");
				setHistoryVisible(false);
				c.close();
			}
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

    private final OnChildClickListener allUnitChildClickListener = new OnChildClickListener() {

		public boolean onChildClick(ExpandableListView parent, View v,
				int groupPosition, int childPosition, long id) {
			sendTextAsSoftKeyboard(((TextView)v).getText().toString() + " ");
			dismissDialog(DIALOG_ALL_UNITS);
			return true;
		}
	};

	private static final int
		DIALOG_ABOUT = 0,
		DIALOG_ALL_UNITS = 1;
	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id){
		case DIALOG_ABOUT:
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
				builder.setMessage("Error: could not load README.xhtml");
				e.printStackTrace();
			}

            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface dialog, int which) {
                            setResult(RESULT_OK);
                    }
            });
            return builder.create();

		case DIALOG_ALL_UNITS:{
			final Builder b = new Builder(Units.this);
			b.setTitle(R.string.dialog_all_units_title);
			final ExpandableListView unitExpandList = new ExpandableListView(Units.this);
			final String[] groupProjection = {UsageEntry._ID, UsageEntry._UNIT, UsageEntry._FACTOR_FPRINT};
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

		default:
			throw new IllegalArgumentException("Unknown dialog ID:" +id);
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
			final View currentFocus = getCurrentFocus();

			switch (v.getId()){
			case R.id.backspace:{
				dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
				dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL));

			} break;

			case R.id.equal:
				go();
				break;

			case R.id.unit_entry:{
				if (currentFocus instanceof MultiAutoCompleteTextView){
					// 2000 is just a magic number that is less than the total number of units,
					// but greater than the number of possibly conforming units. Discovered empirically.
					if (((MultiAutoCompleteTextView) currentFocus).getAdapter().getCount() > 2000){
						showDialog(DIALOG_ALL_UNITS);
					}else{
						((MultiAutoCompleteTextView) currentFocus).setError(null);
						((MultiAutoCompleteTextView)currentFocus).showDropDown();
					}
				}
			}break;

			default:
				sendTextAsSoftKeyboard(((Button)v).getText().toString());
			}

		}

		public boolean onLongClick(View v) {
			switch (v.getId()){
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

	/**
	 * Load the initial usage data on the first run of the application.
	 *
	 * @author steve
	 *
	 */
	private class LoadInitialUnitUsageTask extends AsyncTask<Void, Void, Void>{
		private ProgressDialog pd;
		@Override
		protected void onPreExecute() {
			pd = ProgressDialog.show(Units.this, getText(R.string.app_name), getText(R.string.dialog_loading_units));
		}
		@Override
		protected Void doInBackground(Void... params) {
			unitUsageDBHelper.loadInitialUnitUsage();
			unitUsageDBHelper.loadUnitClassifications();

			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			pd.dismiss();
		}
	}

}
