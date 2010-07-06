package info.staticfree.android.units;

import info.staticfree.android.units.ValueGui.ConversionException;
import info.staticfree.android.units.ValueGui.ReciprocalException;
import net.sourceforge.unitsinjava.EvalError;
import net.sourceforge.unitsinjava.Util;
import net.sourceforge.unitsinjava.Value;
import android.app.Activity;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.MultiAutoCompleteTextView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

// TODO On add from history, move cursor to end of input box
// TODO Auto-scale text for display (square)
// TODO white BG on input boxes
// TODO add function parenthesis auto complete
// TODO longpress on history + result for copy, use result
// TODO longpress on unit for description (look in unit addition error message for hints)
// TODO add help + about box
// TODO create app icon
public class Units extends Activity implements OnClickListener, OnEditorActionListener, OnTouchListener {
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

        // TODO add long-press options to result for copy, send
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

            	haveEditText.setText(entry.haveExpr);
            	wantEditText.setText(entry.wantExpr);

            	setHistoryVisible(false);
			}
		});

		resultView.setOnClickListener(this);
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
		unitUsageDB = unitUsageDBHelper.getWritableDatabase();

		haveEditText.setAdapter(unitUsageDBHelper.getUnitPrefixAdapter(this, unitUsageDB, wantEditText));
		wantEditText.setAdapter(unitUsageDBHelper.getUnitPrefixAdapter(this, unitUsageDB, haveEditText));

		wantEditText.setOnEditorActionListener(this);

		final UnitsMultiAutoCompleteTokenizer tokenizer = new UnitsMultiAutoCompleteTokenizer();
		haveEditText.setTokenizer(tokenizer);
		wantEditText.setTokenizer(tokenizer);
		haveEditText.setOnTouchListener(this);
		wantEditText.setOnTouchListener(this);
    }

    private void setHistoryVisible(boolean visible){
    	if (visible){
    		historyDrawer.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.history_show));
    		historyDrawer.setVisibility(View.VISIBLE);
    	}else{
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

    // TODO there's got to be a translate function that's more efficient than this...
    public String unicodeToAscii(String unicodeInput){
    	return unicodeInput.
    	replaceAll("÷", "/").
    	replaceAll("×", "*").
    	replaceAll("−", "-");
    }

    // TODO filter error messages and output translate to unicode from engine. error msgs and Inifinity → ∞
    public void go(){
    	final String haveStr = haveEditText.getText().toString();
    	String wantStr = wantEditText.getText().toString();

    	try {
    		Value have = null;
    		try {
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
    		wantEditText.setError(e.getMessage());
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

    private ArrayAdapter<HistoryEntry> historyAdapter;

	private final ButtonEventListener buttonListener = new ButtonEventListener();

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
					((MultiAutoCompleteTextView)currentFocus).showDropDown();
				}
				break;

			default:
				final Button cb = (Button)v;

				if (currentFocus instanceof EditText){
					((EditText)currentFocus).getEditableText().insert(((EditText)currentFocus).getSelectionStart(), cb.getText());
				}
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
}
