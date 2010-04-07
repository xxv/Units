package info.staticfree.android.units;

import info.staticfree.android.units.ValueGui.ConversionException;
import info.staticfree.android.units.ValueGui.ReciprocalException;
import net.sourceforge.unitsinjava.EvalError;
import net.sourceforge.unitsinjava.Util;
import net.sourceforge.unitsinjava.Value;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;


public class Units extends Activity implements OnClickListener {
	private final static String TAG = Units.class.getSimpleName();
	
	private EditText wantEditText;
	private EditText haveEditText;
	private Button goButton;
	private TextView resultView;
	private TextView errorMsgView;
	private ListView history;
	private LinearLayout historyDrawer;
	private Button historyClose;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    
        setContentView(R.layout.main);
        
        wantEditText = ((EditText)findViewById(R.id.want));
        haveEditText = ((EditText)findViewById(R.id.have));
        goButton = ((Button)findViewById(R.id.go));
        errorMsgView = ((TextView)findViewById(R.id.error_msg));
        resultView = ((TextView)findViewById(R.id.result));
        history = ((ListView)findViewById(R.id.history_list));
        historyDrawer = ((LinearLayout)findViewById(R.id.history_drawer));
        historyClose = ((Button)findViewById(R.id.history_close));
        
		goButton.setOnClickListener(this);
		
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
    		return haveExpr + " = " + Util.shownumber(result) + wantExpr;
    	}
    }
    
    public void addToHistory(String haveExpr, String wantExpr, double result){
    	final HistoryEntry histEnt = new HistoryEntry(haveExpr, wantExpr, result);
    	resultView.setText(histEnt.toString());
    	historyAdapter.add(histEnt);
    }
    
    public void loadAutocomplete(){
    	
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

    		try {
    			errorMsgView.setText(null);
    			resultVal = ValueGui.convertNonInteractive(have,  want);

    		} catch (final ReciprocalException re){
    			errorMsgView.setText("reciprocal conversion");
    			resultVal = ValueGui.convertNonInteractive(re.reciprocal, want);
    		}

    		addToHistory(haveStr, wantStr, resultVal);

    	} catch (final EvalError e) {

    		result.setText(null);
    		errorMsgView.setText(e.getMessage());
    		return;
    		
    	} catch (final ConversionException e) {

    		result.setText(null);
    		errorMsgView.setText(e.getMessage());
    		return;
    	}
    }

    public void onClick(View v) {
    	switch (v.getId()){
    	case R.id.go:
    		go();
    		break;
    	}	
    }
    
    private ArrayAdapter historyAdapter;
}
