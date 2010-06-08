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
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnLongClickListener;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
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
	private MultiAutoCompleteTextView currentEdit;
	
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

        wantEditText.setOnFocusChangeListener(this);
        haveEditText.setOnFocusChangeListener(this);
		
		currentEdit = haveEditText;
		
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
    
    // TODO filter results to translate unicode to/from engine. ÷ → / and Inifinity → ∞
    // TODO integrate reciprocal conversion a bit better.
    public void go(){
    	final String haveStr = haveEditText.getText().toString();
    	final String wantStr = wantEditText.getText().toString();
    	
    	try {
    		Value have = null;
    		try {
    			have = ValueGui.fromString(haveStr);

    		}catch (final EvalError e){
    			haveEditText.requestFocus();
    			haveEditText.setError(e.getLocalizedMessage());
    			return;
    		}
    		
    		Value want = null;
    		try {
    			want = ValueGui.fromString(wantStr);

    		}catch (final EvalError e){
    			wantEditText.requestFocus();
    			wantEditText.setError(e.getLocalizedMessage());
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
    		
    		allClear();
    		
    	} catch (final ConversionException e) {

    		resultView.setText(null);
    		errorMsgView.setText(e.getMessage());
    		return;
    	}
    }
    
    public void allClear(){
    	haveEditText.getEditableText().clear();
		wantEditText.getEditableText().clear();
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
				
			case R.id.swap_inputs:{
				final Editable e = wantEditText.getText();
				final int start = currentEdit.getSelectionStart();
				final int end   = currentEdit.getSelectionEnd();
				
				wantEditText.setText(haveEditText.getText());
				haveEditText.setText(e);
				
				if (currentEdit == haveEditText){
					wantEditText.requestFocus();
					
				}else{
					haveEditText.requestFocus();
				}
				currentEdit.setSelection(start, end);
				
			}break;
				
			case R.id.unit_entry:
				currentEdit.showDropDown();
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
			currentEdit = (MultiAutoCompleteTextView)v;
			break;
		}	
	}
}
