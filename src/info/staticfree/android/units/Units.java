package info.staticfree.android.units;

import info.staticfree.android.units.ValueGui.ConversionException;
import info.staticfree.android.units.ValueGui.ReciprocalException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import net.sourceforge.unitsinjava.EvalError;
import net.sourceforge.unitsinjava.Util;
import net.sourceforge.unitsinjava.Value;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.ClipboardManager;
import android.text.Editable;
import android.text.InputType;
import android.util.Log;
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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.MultiAutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.TextView.OnEditorActionListener;

// TODO high: either implement history database or persist it in memory on activity state save
// TODO high: fix mdpi app icon on Android 1.6
// TODO med: add function parenthesis auto complete
// TODO low: longpress on unit for description (look in unit addition error message for hints)
// TODO low: Auto-scale text for display (square)
public class Units extends Activity implements OnClickListener, OnEditorActionListener, OnTouchListener, OnLongClickListener {
	private final static String TAG = Units.class.getSimpleName();

	private MultiAutoCompleteTextView wantEditText;
	private MultiAutoCompleteTextView haveEditText;
	private TextView resultView;
	private ListView history;
	private LinearLayout historyDrawer;
	private Button historyClose;
	private LinearLayout numberpad;

	private UnitUsageDBHelper unitUsageDBHelper;
	private SQLiteDatabase unitUsageDB;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate()");
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

        // TODO move history to a database, provide settings to clear/disable.
		historyAdapter = new ArrayAdapter<HistoryEntry>(this, android.R.layout.simple_list_item_1);
		history.setAdapter(historyAdapter);
		// TODO consolidate listeners
		history.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
            	final HistoryEntry entry = historyAdapter.getItem(position);

            	setCurrentEntry(entry);

            	setHistoryVisible(false);
			}
		});
		history.setOnCreateContextMenuListener(this);

		resultView.setOnClickListener(this);
		resultView.setOnCreateContextMenuListener(this);
		historyClose.setOnClickListener(this);

		findViewById(R.id.swap_inputs).setOnClickListener(buttonListener);

		Log.d(TAG, "setting listeners");
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
		Log.d(TAG, "Done.");


		unitUsageDBHelper = new UnitUsageDBHelper(this);

		wantEditText.setOnEditorActionListener(this);

		final UnitsMultiAutoCompleteTokenizer tokenizer = new UnitsMultiAutoCompleteTokenizer();
		haveEditText.setTokenizer(tokenizer);
		wantEditText.setTokenizer(tokenizer);
		haveEditText.setOnTouchListener(this);
		wantEditText.setOnTouchListener(this);
    }

    @Override
    protected void onResume() {
    	super.onResume();

    	unitUsageDB = unitUsageDBHelper.getWritableDatabase();
		haveEditText.setAdapter(unitUsageDBHelper.getUnitPrefixAdapter(this, unitUsageDB, wantEditText));
		wantEditText.setAdapter(unitUsageDBHelper.getUnitPrefixAdapter(this, unitUsageDB, haveEditText));

    	if (unitUsageDBHelper.getUnitUsageDbCount(unitUsageDB) == 0){
    		new LoadInitialUnitUsageTask().execute();
    	}
    }

    @Override
    protected void onPause() {
    	super.onPause();
    	unitUsageDB.close();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
    	// TODO Auto-generated method stub
    	super.onSaveInstanceState(outState);

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

    private class HistoryEntry {
    	public String haveExpr;
    	public String wantExpr;
    	public double result;
    	public HistoryEntry(String haveExpr, String wantExpr, double result) {
    		this.haveExpr = haveExpr;
    		this.wantExpr = wantExpr;
    		this.result = result;
		}

    	@Override
    	public String toString() {
    		return haveExpr + " = " + Util.shownumber(result) + " " + wantExpr;
    	}
    }

    // TODO make reciprocal notice better animated so it doesn't modify main layout
    public void addToHistory(String haveExpr, String wantExpr, double result, boolean reciprocal){
    	haveExpr = haveExpr.trim();
    	wantExpr = wantExpr.trim();
    	new AddToUsageTask().execute(haveExpr, wantExpr);
    	final HistoryEntry histEnt = new HistoryEntry(reciprocal ? "1÷(" + haveExpr + ")" : haveExpr, wantExpr, result);
    	resultView.setText(histEnt.toString());
    	historyAdapter.add(histEnt);

    	final View reciprocalNotice = findViewById(R.id.reciprocal_notice);
    	if (reciprocal){
    		//resultView.setError("reciprocal conversion");
    		resultView.requestFocus();

    		reciprocalNotice.setVisibility(View.VISIBLE);
    		reciprocalNotice.startAnimation(AnimationUtils.makeInAnimation(this, true));
    	}else{
    		reciprocalNotice.setVisibility(View.GONE);
    		resultView.setError(null);
    	}
    }

    private void setCurrentEntry(HistoryEntry entry){
    	haveEditText.setText(entry.haveExpr + " ");// extra space is to prevent auto-complete from triggering.
    	wantEditText.setText(entry.wantExpr + (entry.wantExpr.length() > 0 ? " " : ""));
    	haveEditText.requestFocus();
    	haveEditText.setSelection(haveEditText.length());

    }

    // TODO there's got to be a translate function that's more efficient than this...
    public static String unicodeToAscii(String unicodeInput){
    	return unicodeInput.
    	replaceAll("÷", "/").
    	replaceAll("×", "*").
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
    			have = ValueGui.fromString(unicodeToAscii(haveStr));

    		}catch (final EvalError e){
    			haveEditText.requestFocus();
    			haveEditText.setError(e.getLocalizedMessage());
    			return;
    		}

    		Value want = null;
    		try {
    			want = ValueGui.fromString(unicodeToAscii(wantStr));

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
		final HistoryEntry historyItem = (HistoryEntry) history.getItemAtPosition(position);
		final String historyItemString = historyItem.toString();

		switch (item.getItemId()){
		case MENU_COPY: {
			final ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
			clipboard.setText(historyItemString);
			Toast.makeText(this, getString(R.string.toast_copy, historyItemString),
					Toast.LENGTH_SHORT).show();
		} break;

		case MENU_REEDIT: {
			setCurrentEntry(historyItem);
			setHistoryVisible(false);
		}break;

		case MENU_SEND: {
			startActivity(Intent.createChooser(
					new Intent(Intent.ACTION_SEND)
						.setType("text/plain")
						.putExtra(Intent.EXTRA_TEXT,
								historyItemString),
					getText(R.string.ctx_menu_send_title)));
		}break;

		case MENU_USE_RESULT: {
			setCurrentEntry(new HistoryEntry(historyItem.result + " " + historyItem.wantExpr, "", 0));
			setHistoryVisible(false);
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
		case R.id.about:
			showDialog(DIALOG_ABOUT);
			return true;

		case R.id.show_history:
			setHistoryVisible(true);
			return true;

		case R.id.clear_history:
			historyAdapter.clear();
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


	private static final int
		DIALOG_ABOUT = 0;
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

		}
		return null;
	}

    private ArrayAdapter<HistoryEntry> historyAdapter;

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
			case R.id.swap_inputs:{


				if (getCurrentFocus() == haveEditText){
					swapInputs(haveEditText, wantEditText);

				}else if (getCurrentFocus() == wantEditText){
					swapInputs(wantEditText, haveEditText);
				}else{
					swapInputs(null, null);
				}



			}break;

			case R.id.unit_entry:
				if (currentFocus instanceof MultiAutoCompleteTextView){
					((MultiAutoCompleteTextView) currentFocus).setError(null);
					((MultiAutoCompleteTextView)currentFocus).showDropDown();
				}
				break;

			default:
				dispatchKeyEvent(new KeyEvent(SystemClock.uptimeMillis(), ((Button)v).getText().toString(), Units.class.hashCode(), KeyEvent.FLAG_SOFT_KEYBOARD));
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
				UnitUsageDBHelper.logUnitsInExpression(param, unitUsageDB);
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
			unitUsageDBHelper.loadInitialUnitUsage(unitUsageDB);

			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			pd.dismiss();
		}
	}

}
