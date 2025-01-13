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

package de.buttercookie.units;

import static android.content.res.Configuration.SCREENLAYOUT_SIZE_LARGE;
import static android.content.res.Configuration.SCREENLAYOUT_SIZE_MASK;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
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
import android.widget.ExpandableListAdapter;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.sourceforge.unitsinjava.DefinedFunction;
import net.sourceforge.unitsinjava.EvalError;
import net.sourceforge.unitsinjava.Function;
import net.sourceforge.unitsinjava.UnitList;
import net.sourceforge.unitsinjava.Value;

import org.jared.commons.ui.WorkspaceView;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

import de.buttercookie.units.ValueGui.ConversionException;
import de.buttercookie.units.ValueGui.ReciprocalException;

// TODO med: add implicit "last result" eg. "1+1=" then press "+1" to get "3"
// TODO med: auto-ranging for metric units (auto add "kilo-" or "micro-")
// TODO med: add date headers for history, to consolidate items ("yesterday", "1 week ago", etc.)
// TODO med: show keyboard icon for 2nd tap (can't do this easily, as one can't detect if soft keyboard is shown or not). May need to scrap this idea.
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
    private WorkspaceView workspace;

    private UnitUsageDBHelper unitUsageDBHelper;

    private HistoryAdapter mHistoryAdapter;

    private InitialisationTask mInitialisationTask;
    private boolean runInitialisationTask = false;
    private boolean invalidateUnitsList = false;

    public final static String
            ACTION_USE_UNIT = "de.buttercookie.units.ACTION_USE_UNIT",
            EXTRA_UNIT_NAME = "de.buttercookie.units.EXTRA_UNIT_NAME";

    public final static String
            STATE_RESULT_TEXT = "de.buttercookie.units.RESULT_TEXT",
            STATE_DRAWER_OPENED = "de.buttercookie.units.DRAWER_OPENED",
            STATE_DIALOG_UNIT_CATEGORY = "de.buttercookie.units.STATE_DIALOG_UNIT_CATEGORY";

    private static final int REQUEST_PICK_UNIT = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        wantEditText = findViewById(R.id.want);
        haveEditText = findViewById(R.id.have);
        defaultInputType = wantEditText.getInputType();
        wantEditText.setOnFocusChangeListener(inputBoxOnFocusChange);
        haveEditText.setOnFocusChangeListener(inputBoxOnFocusChange);

        resultView = findViewById(R.id.result);
        history = findViewById(R.id.history_list);
        historyDrawer = findViewById(R.id.history_drawer);
        historyClose = findViewById(R.id.history_close);
        workspace = findViewById(R.id.numpad_switcher);
        //workspace.setTouchSlop(); // XXX scale
        //workspace.setShowTabIndicator(false);

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
        setGridChildrenListener(findViewById(R.id.numberpad), buttonListener, buttonListener);
        setGridChildrenListener(findViewById(R.id.numberpad2), buttonListener, buttonListener);

        final View backspace = findViewById(R.id.backspace);
        backspace.setOnClickListener(buttonListener);
        backspace.setOnLongClickListener(buttonListener);

        unitUsageDBHelper = new UnitUsageDBHelper(this);

        final Object instance = getLastNonConfigurationInstance();
        if (instance instanceof InitialisationTask) {
            mInitialisationTask = (InitialisationTask) instance;
            mInitialisationTask.attach(this);
        } else {
            runInitialisationTask = true;
        }

        wantEditText.setOnEditorActionListener(this);

        final UnitsMultiAutoCompleteTokenizer tokenizer = new UnitsMultiAutoCompleteTokenizer();
        haveEditText.setTokenizer(tokenizer);
        wantEditText.setTokenizer(tokenizer);
        haveEditText.setOnTouchListener(this);
        wantEditText.setOnTouchListener(this);

        if (savedInstanceState != null) {
            resultView.setText(savedInstanceState.getCharSequence(STATE_RESULT_TEXT));
            setHistoryVisible(savedInstanceState.getBoolean(STATE_DRAWER_OPENED, false), false);
            mDialogUnitCategoryUnit = savedInstanceState.getString(STATE_DIALOG_UNIT_CATEGORY);
        }

        final Intent intent = getIntent();
        handleIntent(intent);

    }

    private void handleIntent(Intent intent) {
        final String action = intent.getAction();
        if (Intent.ACTION_SEARCH.equals(action)) {
            final Intent pickUnit = new Intent(this, UnitListActivity.class)
                    .setAction(Intent.ACTION_PICK).setData(UsageEntry.CONTENT_URI);
            final String query = intent.getExtras().getString(SearchManager.QUERY);
            pickUnit.putExtra(UnitListActivity.EXTRA_UNIT_QUERY, query);

            startActivityForResult(pickUnit, REQUEST_PICK_UNIT);

        } else if (ACTION_USE_UNIT.equals(action)) {
            sendUnitAsSoftKeyboard(intent.getData());
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    @Override
    protected void onStart() {
        super.onStart();
        haveEditText.setAdapter(new UnitUsageDBHelper.UnitCursorAdapter(this,
                UsageEntry.CONTENT_URI_NO_UNIT_ALIAS,
                wantEditText));
        wantEditText.setAdapter(new UnitUsageDBHelper.UnitCursorAdapter(this,
                UsageEntry.CONTENT_URI,
                haveEditText));
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        if (runInitialisationTask) {
            runInitialisationTask = false;
            mInitialisationTask = new InitialisationTask(this);
            mInitialisationTask.execute();
        }

        super.onPostCreate(savedInstanceState);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putCharSequence(STATE_RESULT_TEXT, resultView.getText());
        outState.putBoolean(STATE_DRAWER_OPENED, isHistoryVisible());
        outState.putString(STATE_DIALOG_UNIT_CATEGORY, mDialogUnitCategoryUnit);
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        if (mInitialisationTask != null) {
            mInitialisationTask.detach();
        }
        return mInitialisationTask;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_PICK_UNIT: {
                if (resultCode == RESULT_OK) {
                    sendTextAsSoftKeyboard(data.getExtras().getString(EXTRA_UNIT_NAME) + " ");
                }
            }
            break;
        }
    }

    /**
     * @param vg                  Given a view group with view groups inside it, set all children to have the same onClickListeners.
     * @param onClickListener
     * @param onLongClickListener
     */
    private void setGridChildrenListener(ViewGroup vg, OnClickListener onClickListener, OnLongClickListener onLongClickListener) {
        // Go through the children and add all the onClick listeners.
        // Make sure to update if the layout changes.
        final int rows = vg.getChildCount();
        for (int row = 0; row < rows; row++) {
            final ViewGroup v = (ViewGroup) vg.getChildAt(row);
            final int columns = v.getChildCount();
            for (int column = 0; column < columns; column++) {
                final View button = v.getChildAt(column);
                button.setOnClickListener(onClickListener);
                button.setOnLongClickListener(onLongClickListener);
            }
        }
    }

    private boolean isHistoryVisible() {
        return historyDrawer.getVisibility() == View.VISIBLE;
    }

    private void setHistoryVisible(boolean visible) {
        setHistoryVisible(visible, true);
    }

    private void setHistoryVisible(boolean visible, boolean animate) {
        if (visible && historyDrawer.getVisibility() == View.INVISIBLE) {
            if (animate) {
                historyDrawer.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.history_show));
            }
            historyDrawer.setVisibility(View.VISIBLE);
            historyDrawer.requestFocus();

        } else if (!visible && historyDrawer.getVisibility() == View.VISIBLE) {
            if (animate) {
                historyDrawer.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.history_hide));
            }
            historyDrawer.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * Adds the given entry to the history database.
     *
     * @author steve
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
    public void addToHistory(@NonNull String haveExpr, @NonNull String wantExpr,
                             @Nullable String unitAlias,
                             Double result, boolean reciprocal) {
        haveExpr = haveExpr.trim();
        wantExpr = wantExpr.trim();
        new AddToUsageTask().execute(haveExpr,
                unitAlias != null ? unitAlias.trim() : wantExpr);
        haveExpr = reciprocal ? "1÷(" + haveExpr + ")" : haveExpr;
        resultView.setText(HistoryEntry.toCharSequence(haveExpr, wantExpr, result));

        // done on a new thread to avoid hanging the UI while the DB is being updated.
        new Thread(new AddToHistoryRunnable(haveExpr, wantExpr, result)).start();

        final View reciprocalNotice = findViewById(R.id.reciprocal_notice);
        if (reciprocal) {
            resultView.requestFocus();

            reciprocalNotice.setVisibility(View.VISIBLE);
            reciprocalNotice.startAnimation(AnimationUtils.makeInAnimation(this, true));
        } else {
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
    private void setCurrentEntry(Uri entry) {
        final Cursor c = getContentResolver().query(entry, PROJECTION_LOAD_FROM_HISTORY, null, null, null);
        if (c.moveToFirst()) {
            setCurrentEntry(c.getString(c.getColumnIndex(HistoryEntry._HAVE)), c.getString(c.getColumnIndex(HistoryEntry._WANT)));
        }
        c.close();
    }

    @SuppressLint("SetTextI18n")
    private void setCurrentEntry(String have, String want) {
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
    private CharSequence getEntryAsCharSequence(Uri entry) {
        CharSequence text = null;
        final Cursor c = getContentResolver().query(entry, PROJECTION_LOAD_FROM_HISTORY, null, null, null);
        if (c.moveToFirst()) {
            text = HistoryEntry.toCharSequence(c, c.getColumnIndex(HistoryEntry._HAVE), c.getColumnIndex(HistoryEntry._WANT), c.getColumnIndex(HistoryEntry._RESULT));
        }
        c.close();
        return text;
    }

    public static final HashMap<Character, String> UNICODE_TRANS = new HashMap<>();

    static {
        UNICODE_TRANS.put('÷', "/");
        UNICODE_TRANS.put('×', "*");
        UNICODE_TRANS.put('²', "^2");
        UNICODE_TRANS.put('³', "^3");
        UNICODE_TRANS.put('⁴', "^4");
        UNICODE_TRANS.put('−', "-");
        UNICODE_TRANS.put('π', "pi");
        UNICODE_TRANS.put('Π', "pi");
        UNICODE_TRANS.put('√', "sqrt");
        UNICODE_TRANS.put('∛', "cuberoot");
    }

    public static String unicodeToAscii(String unicodeInput) {
        final StringBuilder sb = new StringBuilder();
        final int len = unicodeInput.length();
        for (int i = 0; i < len; i++) {
            final char c = unicodeInput.charAt(i);
            final String sub = UNICODE_TRANS.get(c);
            if (sub != null) {
                sb.append(sub);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    // TODO filter error messages and output translate to unicode from engine. error msgs and Inifinity → ∞
    public void go() {
        String haveStr = haveEditText.getText().toString().trim();
        String wantStr = wantEditText.getText().toString().trim();

        try {
            Value have;
            try {
                if (haveStr.length() == 0) {
                    haveEditText.requestFocus();
                    haveEditText.setError(getText(R.string.err_have_empty));
                    return;
                }
                haveStr = ValueGui.closeParens(haveStr);
                have = ValueGui.fromUnicodeString(haveStr);

            } catch (final EvalError e) {
                haveEditText.requestFocus();
                haveEditText.setError(e.getLocalizedMessage());
                return;
            }

            Double resultVal;
            boolean reciprocal = false;

            String uList = UnitList.isUnitList(wantStr);
            String unitAlias = null;
            if (uList != null) {
                unitAlias = wantStr;
                UnitList ul;
                try {
                    ul = new UnitList(uList);
                } catch (final EvalError e) {
                    wantEditText.requestFocus();
                    wantEditText.setError(e.getLocalizedMessage());
                    return;
                }

                wantEditText.setError(null);
                // unit lists are a special case and don't have a reciprocal, so the result
                // is just stored in the wantStr.
                resultVal = null;
                wantStr = ValueGui.convertNonInteractive(this, haveStr, have, ul);

            } else {
                Value want = null;
                Function func;
                try {
                    func = DefinedFunction.table.get(wantStr);
                    if (func == null && wantStr.endsWith("(")) {
                        func = DefinedFunction.table.get(wantStr.substring(0, wantStr.length() - 1));
                    }
                    if (func == null) {
                        wantStr = ValueGui.closeParens(wantStr);
                        want = ValueGui.fromUnicodeString(wantStr);
                    }

                } catch (final EvalError e) {
                    wantEditText.requestFocus();
                    wantEditText.setError(e.getLocalizedMessage());
                    return;
                }

                try {
                    wantEditText.setError(null);

                    // if no want value is specified, provide a definition.
                    if (wantStr.length() > 0) {
                        if (func != null) {
                            // functions are a special case and don't have a reciprocal, so the result
                            // is just stored in the wantStr.
                            resultVal = null;
                            wantStr = ValueGui.convertNonInteractive(ValueGui.fromUnicodeString(haveStr), func);
                        } else {
                            resultVal = ValueGui.convertNonInteractive(have, want);
                        }

                    } else {
                        resultVal = have.factor;
                        final StringBuilder haveDef = new StringBuilder();

                        haveDef.append(have.numerator.asString());

                        if (have.denominator.size() > 0) {
                            haveDef.append(" ÷").append(have.denominator.asString());
                        }

                        wantStr = haveDef.toString();
                    }


                } catch (final ReciprocalException re) {
                    reciprocal = true;

                    resultVal = ValueGui.convertNonInteractive(re.reciprocal, want);
                }
            }

            allClear();

            addToHistory(haveStr, wantStr, unitAlias, resultVal, reciprocal);

        } catch (final ConversionException e) {
            resultView.setText(null);
            wantEditText.requestFocus();
            wantEditText.setError(getText(R.string.err_no_conform));
        }
    }

    public void allClear() {
        haveEditText.getEditableText().clear();
        haveEditText.setError(null);
        wantEditText.getEditableText().clear();
        wantEditText.setError(null);
        haveEditText.requestFocus();
    }

    @SuppressLint("NonConstantResourceId")
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.result:
                setHistoryVisible(true);
                break;

            case R.id.history_close:
                setHistoryVisible(false);
                break;
        }
    }


    @SuppressLint("NonConstantResourceId")
    public boolean onLongClick(View v) {
        switch (v.getId()) {
            case R.id.result:
                v.showContextMenu();
                return true;
        }
        return false;
    }

    private static final int
            MENU_COPY = 0,
            MENU_SEND = 1,
            MENU_USE_RESULT = 2,
            MENU_REEDIT = 3;

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
        if (ctxMenuInfo instanceof AdapterContextMenuInfo) {
            position = ((AdapterContextMenuInfo) ctxMenuInfo).position;
        }
        final Uri itemUri = ContentUris.withAppendedId(HistoryEntry.CONTENT_URI, mHistoryAdapter.getItemId(position));

        switch (item.getItemId()) {
            case MENU_COPY: {
                final CharSequence itemText = getEntryAsCharSequence(itemUri);
                final ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                clipboard.setText(itemText);
                Toast.makeText(this, getString(R.string.toast_copy, itemText),
                        Toast.LENGTH_SHORT).show();
            }
            break;

            case MENU_REEDIT: {
                setCurrentEntry(itemUri);
                setHistoryVisible(false);
            }
            break;

            case MENU_SEND: {
                final CharSequence itemText = getEntryAsCharSequence(itemUri);
                startActivity(Intent.createChooser(
                        new Intent(Intent.ACTION_SEND)
                                .setType("text/plain")
                                .putExtra(Intent.EXTRA_TEXT,
                                        itemText),
                        getText(R.string.ctx_menu_send_title)));
            }
            break;

            case MENU_USE_RESULT: {
                final Cursor c = getContentResolver().query(itemUri, PROJECTION_LOAD_FROM_HISTORY, null, null, null);
                if (c.moveToFirst()) {
                    final int resultCol = c.getColumnIndex(HistoryEntry._RESULT);
                    setCurrentEntry((c.isNull(resultCol) ? "" : (c.getDouble(resultCol)
                            + " ")) + c.getString(c.getColumnIndex(HistoryEntry._WANT)), "");
                    setHistoryVisible(false);
                }
                c.close();
            }
            break;
        }

        return super.onContextItemSelected(item);
    }

    @Override
    public void openOptionsMenu() {
        Configuration config = getResources().getConfiguration();
        if (requireTabletMenuHack(config)) {
            final int originalScreenSize = config.screenLayout & SCREENLAYOUT_SIZE_MASK;
            config.screenLayout &= ~SCREENLAYOUT_SIZE_MASK;
            config.screenLayout |= SCREENLAYOUT_SIZE_LARGE;

            super.openOptionsMenu();

            config.screenLayout &= ~SCREENLAYOUT_SIZE_MASK;
            config.screenLayout |= originalScreenSize;
        } else {
            super.openOptionsMenu();
        }
    }

    private boolean requireTabletMenuHack(Configuration config) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB &&
                (config.screenLayout & SCREENLAYOUT_SIZE_MASK)
                        > SCREENLAYOUT_SIZE_LARGE;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (BuildConfig.DEBUG) {
            MenuItem item = menu.findItem(R.id.dump_fingerprints);
            item.setVisible(unitUsageDBHelper.canDebugDumpFingerprints());
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.swap_inputs: {
                if (getCurrentFocus() == haveEditText) {
                    swapInputs(haveEditText, wantEditText);

                } else if (getCurrentFocus() == wantEditText) {
                    swapInputs(wantEditText, haveEditText);
                } else {
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

            case R.id.dump_fingerprints:
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... voids) {
                        unitUsageDBHelper.debugDumpFingerprints();
                        return null;
                    }
                }.execute();
                Toast.makeText(this, getString(R.string.toast_dump_fingerprints),
                        Toast.LENGTH_SHORT).show();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onOptionsMenuClosed(Menu menu) {
        super.onOptionsMenuClosed(menu);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Workaround for https://issuetracker.google.com/issues/315761686
            invalidateOptionsMenu();
        }
    }

    /**
     * Read an InputStream into a String until it hits EOF.
     *
     * @param in
     * @return the complete contents of the InputStream
     * @throws IOException
     */
    static public String inputStreamToString(InputStream in) throws IOException {
        final int bufsize = 8196;
        final char[] cbuf = new char[bufsize];

        final StringBuilder buf = new StringBuilder(bufsize);

        final InputStreamReader in_reader = new InputStreamReader(in);

        for (int readBytes = in_reader.read(cbuf, 0, bufsize);
             readBytes > 0;
             readBytes = in_reader.read(cbuf, 0, bufsize)) {
            buf.append(cbuf, 0, readBytes);
        }

        return buf.toString();
    }

    private void sendTextAsSoftKeyboard(String text) {
        dispatchKeyEvent(new KeyEvent(SystemClock.uptimeMillis(), text, Units.class.hashCode(), KeyEvent.FLAG_SOFT_KEYBOARD));
    }

    private void sendTextAsSoftKeyboard(String text, boolean moveToDefault) {
        dispatchKeyEvent(new KeyEvent(SystemClock.uptimeMillis(), text, Units.class.hashCode(), KeyEvent.FLAG_SOFT_KEYBOARD));
        if (moveToDefault) {
            workspace.moveToDefaultScreen();
        }
    }

    private void sendUnitAsSoftKeyboard(Uri unit) {
        final String[] projection = {UsageEntry._ID, UsageEntry._UNIT};
        final Cursor c = getContentResolver().query(unit, projection, null, null, null);
        if (c.moveToFirst()) {
            sendTextAsSoftKeyboard(c.getString(c.getColumnIndex(UsageEntry._UNIT)) + " ");
        }
        c.close();
    }

    private final OnChildClickListener allUnitChildClickListener = new OnChildClickListener() {

        public boolean onChildClick(ExpandableListView parent, View v,
                                    int groupPosition, int childPosition, long id) {
            sendTextAsSoftKeyboard(((TextView) v).getText().toString() + " ");
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
        switch (id) {
            case DIALOG_ABOUT: {
                final Builder builder = new AlertDialog.Builder(this);

                builder.setTitle(R.string.dialog_about_title);
                builder.setIcon(R.mipmap.ic_launcher);

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

                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        setResult(RESULT_OK);
                    }
                });
                return builder.create();
            }

            case DIALOG_ALL_UNITS: {
                final Builder b = new Builder(Units.this);
                b.setTitle(R.string.dialog_all_units_title);
                final ExpandableListView unitExpandList = new ExpandableListView(Units.this);
                unitExpandList.setId(android.R.id.list);
                unitExpandList.setAdapter(getAllUnits());
                unitExpandList.setCacheColorHint(0);
                unitExpandList.setOnChildClickListener(allUnitChildClickListener);
                b.setView(unitExpandList);
                return b.create();
            }

            case DIALOG_UNIT_CATEGORY: {
                final Builder b = new Builder(new ContextThemeWrapper(this, R.style.UnitCategoryDialogTheme));
                final String[] from = {UsageEntry._UNIT};
                final int[] to = {android.R.id.text1};
                b.setTitle("all units");
                final String[] projection = {UsageEntry._ID, UsageEntry._UNIT, UsageEntry._FACTOR_FPRINT};
                final Cursor c = managedQuery(UsageEntry.CONTENT_URI, projection, null, null, UnitUsageDBHelper.USAGE_SORT);
                dialogUnitCategoryList = new SimpleCursorAdapter(this, android.R.layout.select_dialog_item, c, from, to);
                b.setAdapter(dialogUnitCategoryList, dialogUnitCategoryOnClickListener);

                return b.create();
            }

            case DIALOG_LOADING_UNITS: {
                final ProgressDialog pd = new ProgressDialog(this);
                pd.setIndeterminate(true);
                pd.setTitle(R.string.app_name);
                pd.setMessage(getText(R.string.dialog_loading_units));
                return pd;
            }

            default:
                throw new IllegalArgumentException("Unknown dialog ID:" + id);
        }
    }

    private ExpandableListAdapter getAllUnits() {
        final String[] groupProjection = {UsageEntry._ID, UsageEntry._UNIT, UsageEntry._FACTOR_FPRINT};
        // any selection below will select from the grouping description
        final Cursor cursor = managedQuery(UsageEntry.CONTENT_URI_CONFORM_TOP, groupProjection, null, null, UnitUsageDBHelper.USAGE_SORT);

        return new UnitsExpandableListAdapter(cursor, this,
                android.R.layout.simple_expandable_list_item_1, android.R.layout.simple_expandable_list_item_1,
                new String[]{UsageEntry._UNIT},
                new int[]{android.R.id.text1},
                new String[]{UsageEntry._UNIT}, new int[]{android.R.id.text1});
    }

    private String mDialogUnitCategoryUnit = "m";
    private SimpleCursorAdapter dialogUnitCategoryList;

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        switch (id) {
            case DIALOG_UNIT_CATEGORY: {
                dialog.setTitle(mDialogUnitCategoryUnit);

                final String[] projection = {UsageEntry._ID, UsageEntry._UNIT, UsageEntry._FACTOR_FPRINT};
                final Cursor c = managedQuery(UsageEntry.getEntriesMatchingFprint(UnitUsageDBHelper.computeFingerprint(mDialogUnitCategoryUnit)),
                        projection, null, null, UnitUsageDBHelper.USAGE_SORT);
                stopManagingCursor(dialogUnitCategoryList.getCursor());
                dialogUnitCategoryList.changeCursor(c);
                final ListView lv = ((AlertDialog) dialog).getListView();
                lv.setSelectionFromTop(0, 0);

            }
            break;

            case DIALOG_ALL_UNITS: {
                if (invalidateUnitsList) {
                    invalidateUnitsList = false;
                    final ExpandableListView unitExpandList = dialog.findViewById(android.R.id.list);
                    unitExpandList.setAdapter(getAllUnits());
                }
            }
            break;

            default:
                super.onPrepareDialog(id, dialog);
        }

    }

    private void swapInputs(EditText focused, EditText unfocused) {

        final Editable e = wantEditText.getText();
        int start = 0, end = 0;
        if (focused != null) {
            start = focused.getSelectionStart();
            end = focused.getSelectionEnd();
        }

        wantEditText.setText(haveEditText.getText());
        haveEditText.setText(e);
        if (unfocused != null) {
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
            return managedQuery(UsageEntry.CONTENT_URI, childProjection, UsageEntry._FACTOR_FPRINT + "=?", selectionArgs, UnitUsageDBHelper.USAGE_SORT);
        }
    }

    private final ButtonEventListener buttonListener = new ButtonEventListener();

    private class ButtonEventListener implements OnClickListener, OnLongClickListener {

        @SuppressLint("NonConstantResourceId")
        public void onClick(View v) {

            switch (v.getId()) {
                case R.id.backspace: {
                    dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
                    dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL));

                }
                break;

                case R.id.equal:
                    go();
                    break;

                case R.id.unit_entry: {
                    final View currentFocus = getCurrentFocus();
                    if (currentFocus instanceof MultiAutoCompleteTextView) {
                        // 2000 is just a magic number that is less than the total number of units,
                        // but greater than the number of possibly conforming units. Discovered empirically.
                        if (((MultiAutoCompleteTextView) currentFocus).getAdapter().getCount() > 2000) {
                            onSearchRequested();
                        } else {
                            ((MultiAutoCompleteTextView) currentFocus).setError(null);
                            ((MultiAutoCompleteTextView) currentFocus).showDropDown();
                        }
                    }

                }
                break;

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
                    sendTextAsSoftKeyboard(((Button) v).getText().toString() + "( ", true);
                    break;

                // constants
                case R.id.pi:
                case R.id.light:
                case R.id.energy:
                    sendTextAsSoftKeyboard(((Button) v).getText().toString() + " ", true);
                    break;

                case R.id.square:
                    sendTextAsSoftKeyboard("² ", true);
                    break;

                case R.id.cube:
                    sendTextAsSoftKeyboard("³ ", true);
                    break;

                case R.id.milli:
                case R.id.kilo:
                case R.id.centi: {
                    String prefix = ((Button) v).getText().toString();
                    prefix = prefix.substring(0, prefix.length() - 1);
                    sendTextAsSoftKeyboard(prefix, false);
                }
                break;


                default:
                    sendTextAsSoftKeyboard(((Button) v).getText().toString(), true);
            }
        }

        @SuppressLint("NonConstantResourceId")
        public boolean onLongClick(View v) {
            switch (v.getId()) {
                case R.id.unit_entry: {
                    showDialog(DIALOG_ALL_UNITS);
                    return true;
                }
                case R.id.power: {
                    sendTextAsSoftKeyboard("E");
                    return true;
                }
                case R.id.div: {
                    sendTextAsSoftKeyboard("|");
                    return true;
                }
                case R.id.dot: {
                    sendTextAsSoftKeyboard(";");
                    return true;
                }
                case R.id.backspace: {
                    final View currentFocus = getCurrentFocus();
                    if (currentFocus instanceof EditText) {
                        ((EditText) currentFocus).getEditableText().clear();
                        ((EditText) currentFocus).setError(null);
                    }
                    return true;
                }
                case R.id.equal: {
                    openOptionsMenu();
                }
            }
            return false;
        }
    }

    @SuppressLint("NonConstantResourceId")
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        switch (v.getId()) {
            case R.id.want:
                go();
                final InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);


                return true;
        }
        return false;
    }

    private int defaultInputType;
    // make sure to reset the input type when losing focus.
    private final OnFocusChangeListener inputBoxOnFocusChange = new OnFocusChangeListener() {
        public void onFocusChange(View v, boolean hasFocus) {
            if (!hasFocus) {
                ((MultiAutoCompleteTextView) v).setInputType(defaultInputType);
            }
        }
    };

    @SuppressLint("NonConstantResourceId")
    public boolean onTouch(View v, MotionEvent event) {
        switch (v.getId()) {

            // this is used to prevent the first touch on these editors from triggering the IME soft keyboard.
            case R.id.want:
            case R.id.have:
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    final EditText editor = (EditText) v;
                    if (v.hasFocus()) {
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
     *
     * @author steve
     */
    private class AddToUsageTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... params) {
            for (final String param : params) {
                UnitUsageDBHelper.logUnitsInExpression(param, getContentResolver());
            }
            return null;
        }
    }

    /**
     * Prepare the usage data database, that is load the initial usage data on the first run of the
     * application and update the unit classifications as necessary.
     *
     * @author steve
     */
    static class InitialisationTask extends AsyncTask<Void, Void, Void> {
        @SuppressLint("StaticFieldLeak")
        private Units mActivity;

        InitialisationTask(Units activity) {
            attach(activity);
        }

        public void attach(Units activity) {
            mActivity = activity;
        }

        public void detach() {
            mActivity = null;
        }

        @Override
        protected void onPreExecute() {
            mActivity.showDialog(DIALOG_LOADING_UNITS);
        }

        @Override
        protected Void doInBackground(Void... params) {
            ((Application) mActivity.getApplication()).updateUnitsLocaleIfRequired();
            mActivity.unitUsageDBHelper.updateUnitUsage();
            mActivity.unitUsageDBHelper.loadUnitClassifications();

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            try {
                if (mActivity != null) {
                    mActivity.mInitialisationTask = null;
                    mActivity.invalidateUnitsList = true;
                    mActivity.dismissDialog(DIALOG_LOADING_UNITS);
                }
            } catch (final IllegalArgumentException ie) {
                // it's alright if it was dismissed already.
            }
        }
    }
}
