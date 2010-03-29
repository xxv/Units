package info.staticfree.android.units;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Vector;

import net.sourceforge.unitsinjava.Env;
import net.sourceforge.unitsinjava.Tables;
import net.sourceforge.unitsinjava.Value;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;


public class Units extends Activity implements OnClickListener {
	
	private EditText wantEditText;
	private EditText haveEditText;
	private Button goButton;
	private TextView result;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    
        setContentView(R.layout.main);
        
        wantEditText = ((EditText)findViewById(R.id.want));
        haveEditText = ((EditText)findViewById(R.id.have));
        goButton = ((Button)findViewById(R.id.go));
        result = ((TextView)findViewById(R.id.answer));
        
        Env.filenames = new Vector<String>();
        Env.filenames.add("units.dat");
        

        Env.out = new Env.Writer(){
        	@Override
        	public void print(String s) {
        		result.append(s);
        		
        	}
        	
        	@Override
        	public void println(String s) {
        		result.append(s + "\n");
        		
        	}
        };
        
        Env.err = new Env.Writer() {
			
			@Override
			public void println(String s) {
				result.append(s);
				
			}
			
			@Override
			public void print(String s) {
				result.append(s + "\n");
				
			}
		};
        
        Env.files = new Env.FileAcc() {
			
			@Override
			public BufferedReader open(String name) {
				try {
					Log.d("Units", "reading definitions from "+name);
					final InputStream is = getAssets().open(name);
					
					return new BufferedReader(new InputStreamReader(is), 16000);
				}catch (final IOException ioe){
					ioe.printStackTrace();
				}
				return null;
			}
		};
		
        Tables.build();
        
		goButton.setOnClickListener(this);
    }
    
    public void go(){
    	final String haveStr = haveEditText.getText().toString().trim();
    	final String wantStr = wantEditText.getText().toString().trim();
    	
    	final Value have = Value.fromString(haveStr);
    	if (have == null){
    		haveEditText.requestFocus();
    		return;
    	}
    	final Value want = Value.fromString(wantStr);
    	if (want == null){
    		wantEditText.requestFocus();
    		return;
    	}
    	
    	Value.convert(haveStr, have, wantStr, want);
    }
    
    public void onClick(View v) {
    	switch (v.getId()){
    	case R.id.go:
    		go();
    		break;
    	}
    	
    }
}