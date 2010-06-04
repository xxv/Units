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
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnLongClickListener;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.MultiAutoCompleteTextView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;


public class Units extends Activity implements OnClickListener, OnFocusChangeListener {
	private final static String TAG = Units.class.getSimpleName();
	
	private MultiAutoCompleteTextView wantEditText;
	private MultiAutoCompleteTextView haveEditText;
	private TextView resultView;
	private TextView errorMsgView;
	private ListView history;
	private LinearLayout historyDrawer;
	private Button historyClose;
	private LinearLayout numberpad;
	private EditText currentEdit;
	
	private UnitUsageDBHelper unitUsageDBHelper;
	private SQLiteDatabase unitUsageDB;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate()");
        setContentView(R.layout.main);
        
        wantEditText = ((MultiAutoCompleteTextView)findViewById(R.id.want));
        haveEditText = ((MultiAutoCompleteTextView)findViewById(R.id.have));
        errorMsgView = ((TextView)findViewById(R.id.error_msg));
        resultView = ((TextView)findViewById(R.id.result));
        history = ((ListView)findViewById(R.id.history_list));
        historyDrawer = ((LinearLayout)findViewById(R.id.history_drawer));
        historyClose = ((Button)findViewById(R.id.history_close));
        numberpad = ((LinearLayout)findViewById(R.id.numberpad));
        
        wantEditText.addTextChangedListener(textChangeWatcher);
        haveEditText.addTextChangedListener(textChangeWatcher);
        wantEditText.setOnFocusChangeListener(this);
        haveEditText.setOnFocusChangeListener(this);
		
		currentEdit = haveEditText;
		
		historyAdapter = new ArrayAdapter<HistoryEntry>(this, android.R.layout.simple_list_item_1);
		history.setAdapter(historyAdapter);
		setHistoryVisible(false);
		
		resultView.setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
				setHistoryVisible(true);
				
			}
		});
		historyClose.setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
			setHistoryVisible(false);
				
			}
		});
		
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
		final SimpleCursorAdapter usageAdapter = unitUsageDBHelper.getUnitPrefixAdapter(this, unitUsageDB);
		haveEditText.setAdapter(usageAdapter);
		wantEditText.setAdapter(usageAdapter);
		
		final UnitsMultiAutoCompleteTokenizer tokenizer = new UnitsMultiAutoCompleteTokenizer();
		haveEditText.setTokenizer(tokenizer);
		wantEditText.setTokenizer(tokenizer);
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
    
    public void addToHistory(String haveExpr, String wantExpr, double result){
    	final HistoryEntry histEnt = new HistoryEntry(haveExpr, wantExpr, result);
    	resultView.setText(histEnt.toString());
    	historyAdapter.add(histEnt);
    }
    
    public void updateCalculation(boolean incremental) throws ConversionException {
    	final String haveStr = haveEditText.getText().toString();
    	final String wantStr = wantEditText.getText().toString();
    	
    		final Value have = ValueGui.fromString(haveStr);
    		if (have == null){
    			if (!incremental){
    				haveEditText.requestFocus();
    			}
    			return;
    		}
    		final Value want = ValueGui.fromString(wantStr);
    		if (want == null){
    			if (!incremental){
    				wantEditText.requestFocus();
    			}
    			return;
    		}

    		double resultVal;
    		final boolean reciprocal = false;

    		
		resultVal = ValueGui.convertNonInteractive(have,  want);
		
		resultView.setText(haveStr + " = " + Util.shownumber(resultVal) + " " + wantStr);
    }
    
    public void go(){
    	final String haveStr = haveEditText.getText().toString();
    	final String wantStr = wantEditText.getText().toString();
    	
    	try {
    		final Value have = ValueGui.fromString(haveStr);
    		if (have == null){
    			haveEditText.requestFocus();
    			return;
    		}
    		final Value want = ValueGui.fromString(wantStr);
    		if (want == null){
    			wantEditText.requestFocus();
    			return;
    		}

    		double resultVal;
    		boolean reciprocal = false;

    		try {
    			errorMsgView.setText(null);
    			resultVal = ValueGui.convertNonInteractive(have,  want);

    		} catch (final ReciprocalException re){
    			reciprocal = true;
    			errorMsgView.setText("reciprocal conversion");
    			resultVal = ValueGui.convertNonInteractive(re.reciprocal, want);
    		}

    		addToHistory(reciprocal ? "1/(" + haveStr + ")" : haveStr, wantStr, resultVal);

    	} catch (final EvalError e) {

    		resultView.setText(null);
    		errorMsgView.setText(e.getMessage());
    		return;
    		
    	} catch (final ConversionException e) {

    		resultView.setText(null);
    		errorMsgView.setText(e.getMessage());
    		return;
    	}
    }

    public void onClick(View v) {
    	// XXX delete me
    	switch (v.getId()){
    	}	
    }
    
    private ArrayAdapter<HistoryEntry> historyAdapter;
    
    private final TextWatcher textChangeWatcher = new TextWatcher() {
		
		public void onTextChanged(CharSequence s, int start, int before, int count) {
			try {
				updateCalculation(true);
			} catch (final ConversionException e) {
				//ignore as we're incrementally doing this
			} catch(final EvalError e) {
				// 
			}
			
		}
		
		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {
			// TODO Auto-generated method stub
			
		}
		
		public void afterTextChanged(Editable s) {
			// TODO Auto-generated method stub
			
		}
	};
	
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
				
			case R.id.mul:
				dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_STAR));
				break;
				
			case R.id.div:
				dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_SLASH));
				break;
				
			case R.id.minus:
				dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MINUS));
				break;
				
			case R.id.swap_inputs:
				final Editable e = wantEditText.getText();
				wantEditText.setText(haveEditText.getText());
				haveEditText.setText(e);
				break;
				
			case R.id.unit_entry:
				getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
				break;
				
			default:
				final Button cb = (Button)v;
				currentEdit.getEditableText().insert(currentEdit.getSelectionStart(), cb.getText());
			}
			
		}
		
		public boolean onLongClick(View v) {
			switch (v.getId()){
				case R.id.backspace:{
					currentEdit.getEditableText().clear();
					
					return true;
				}
			}
			return false;
		}
	};

	public void onFocusChange(View v, boolean hasFocus) {
		switch (v.getId()){
		case R.id.want:
		case R.id.have:
			currentEdit = (EditText)v;
			break;
		}
		
	}
}
