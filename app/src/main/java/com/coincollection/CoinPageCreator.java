/*
 * Coin Collection, an Android app that helps users track the coins that they've collected
 * Copyright (C) 2010-2016 Andrew Williams
 *
 * This file is part of Coin Collection.
 *
 * Coin Collection is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Coin Collection is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Coin Collection.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.coincollection;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.Spinner;
import android.widget.Toast;

import com.spencerpages.BuildConfig;
import com.spencerpages.MainApplication;
import com.spencerpages.R;

import java.util.ArrayList;
import java.util.HashMap;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import static com.coincollection.MainActivity.createAndShowHelpDialog;
import static com.spencerpages.MainApplication.APP_NAME;

/**
 * Activity responsible for managing the collection creation page
 */
public class CoinPageCreator extends AppCompatActivity {

    /** mCoinTypeIndex The index of the currently selected coin type in the
     *                 MainApplication.COLLECTION_TYPES list. */
    private int mCoinTypeIndex;

    /** mCollectionObj The CollectionInfo object associated with this index. */
    private CollectionInfo mCollectionObj;

    /** mParameters The HashMap that is used to keep track of the changes that
     *              the user has requested (via the UI) to the default
     *              collection settings. */
    private HashMap<String, Object> mParameters;

    /** mDefaults The default parameters provided from a call to the current
     *             mCollectionObj getCreationParameters method. */
    private HashMap<String, Object> mDefaults;

    /** mCoinList Upon selecting to create a collection, gets populated with coin identifiers
     *            and mint marks */
    private final ArrayList<CoinSlot> mCoinList = new ArrayList<>();

    /** mTask Holds the AsyncTask that we use to interact with the database (so that our database
     *        activity doesn't run on the main thread and trigger an Application Not Responding
     *        error.)  If we change screen orientation and our activity is going to be destroyed,
     *        we have to save this off and pass it to the new activity. */
    private AsyncProgressTask mTask = null;

    /** mProgressDialog Holds the dialog that we display when the AsyncTask is running to update
     *                  the database.
     *                  TODO There are some issues with how we handle this dialog during the screen
     *                  orientation change case. See other TODOs below and get this working
     *                  correctly if it is indeed broken! */
    private ProgressDialog mProgressDialog = null;

    /* Internal keys to use for passing data via saved instance state */
    private final static String _COIN_TYPE_INDEX = "CoinTypeIndex";
    private final static String _PARAMETERS = "Parameters";

    /** These are the options supported in the parameter HashMaps.  In general,
     *  this is how they work:
     *  - If an option is present in the parameters HashMap after the call to
     *    getCreationParameters, the associated UI element will be displayed to
     *    the user
     *  - The UI element's default value will be set to the value specified in
     *    the HashMap after a call to getCreationParameters, and the associated
     *    value in the HashMap passed to populateCollectionLists will change
     *    based on changes made to the UI element.
     *  The specifics for the various options are as follows:
     *
     *  OPT_SHOW_MINT_MARKS
     *  - Associated UI Element: 'Show Mint Marks' checkbox
     *  - Associated Value Type: Boolean
     *  - This option MUST be used in conjunction with at least one of the
     *    specific show mint mark options. (Ex: OPT_SHOW_MINT_MARK_1)
     *
     *  OPT_SHOW_MINT_MARK_# (where # is a number between 1 and 5)
     *  - Associated UI Element: Checkboxes that can be used for mint markers
     *  - Associated Value Type: Boolean
     *  - These checkboxes will get hidden and displayed depending on the 'Show Mint Marks'
     *    checkbox.  The text associated with the checkbox must be specified via the
     *    associated OPT_SHOW_MINT_MARK_#_STRING_ID.  There are currently five of these
     *    that can be used per collection (if more are needed, minor changes to the core
     *    code will be necessary)
     *  - These options MUST be used in conjunction with OPT_SHOW_MINT_MARKS, and MUST
     *    be accompanied by the respective OPT_SHOW_MINT_MARK_#_STRING_ID
     *
     *  OPT_SHOW_MINT_MARK_#_STRING_ID (where # is a number between 1 and 5)
     *  - Associated UI Element: Special - see above
     *  - Associated Value Type: Integer
     *  - This option is special - it is used in conjunction with the option above
     *    to indicate the resource ID associated with a String to display next
     *    to the checkbox (Ex: R.string.show_p in the U.S. Coin Collection app)
     *
     *  OPT_EDIT_DATE_RANGE
     *  - Associated UI Element: 'Edit Date Range' checkbox
     *  - Associated Value Type: Boolean
     *  - This option MUST be used in conjunction with OPT_START_YEAR and
     *    OPT_STOP_YEAR
     *
     *  OPT_START_YEAR
     *  - Associated UI Element: 'Edit Start Year' EditText
     *  - Associated Value Type: Integer
     *  - This option MUST be used in conjunction with OPT_EDIT_DATE_RANGE
     *
     *  OPT_STOP_YEAR
     *  - Associated UI Element: 'Edit Stop Year' EditText
     *  - Associated Value Type: Integer
     *  - This option MUST be used in conjunction with OPT_EDIT_DATE_RANGE
     *
     *  OPT_CHECKBOX_# (where # is a number between 1 and 5)
     *  - Associated UI Element: a standalone checkbox
     *  - Associated Value Type: Boolean
     *  - The text associated with the checkbox must be specified via the
     *    associated OPT_SHOW_MINT_MARK_#_STRING_ID.  There are currently five
     *    of these that can be used per collection (if more are needed, minor
     *    changes to the core code will be necessary.)
     *  - These options MUST be accompanied by the respective
     *    OPT_CHECKBOX_#_STRING_ID
     *
     *  OPT_CHECKBOX_#_STRING_ID
     *  - Associated UI Element: Special - see above
     *  - Associated Value Type: Integer
     *  - This option is special - it is used in conjunction with the option above
     *    to indicate the resource ID associated with a String to display next
     *    to the checkbox (Ex: R.string.show_territories in the U.S. Coin
     *    Collection app)
     */
    public final static String OPT_SHOW_MINT_MARKS = "ShowMintMarks";
    public final static String OPT_SHOW_MINT_MARK_1 = "ShowMintMark1";
    public final static String OPT_SHOW_MINT_MARK_2 = "ShowMintMark2";
    public final static String OPT_SHOW_MINT_MARK_3 = "ShowMintMark3";
    public final static String OPT_SHOW_MINT_MARK_4 = "ShowMintMark4";
    public final static String OPT_SHOW_MINT_MARK_5 = "ShowMintMark5";
    public final static String OPT_EDIT_DATE_RANGE = "EditDateRange";
    public final static String OPT_START_YEAR = "StartYear";
    public final static String OPT_STOP_YEAR = "StopYear";
    public final static String OPT_CHECKBOX_1 = "ShowCheckbox1";
    public final static String OPT_CHECKBOX_2 = "ShowCheckbox2";
    private final static String OPT_CHECKBOX_3 = "ShowCheckbox3";
    private final static String OPT_CHECKBOX_4 = "ShowCheckbox4";
    private final static String OPT_CHECKBOX_5 = "ShowCheckbox5";

    // TODO Is there a better way to pass this info?  Maybe we can
    // store default values in each app's MainApplication and use
    // those if not specified?
    public final static String OPT_SHOW_MINT_MARK_1_STRING_ID = "ShowMintMark1StringId";
    public final static String OPT_SHOW_MINT_MARK_2_STRING_ID = "ShowMintMark2StringId";
    public final static String OPT_SHOW_MINT_MARK_3_STRING_ID = "ShowMintMark3StringId";
    public final static String OPT_SHOW_MINT_MARK_4_STRING_ID = "ShowMintMark4StringId";
    public final static String OPT_SHOW_MINT_MARK_5_STRING_ID = "ShowMintMark5StringId";

    public final static String OPT_CHECKBOX_1_STRING_ID = "ShowCheckbox1StringId";
    public final static String OPT_CHECKBOX_2_STRING_ID = "ShowCheckbox2StringId";
    private final static String OPT_CHECKBOX_3_STRING_ID = "ShowCheckbox3StringId";
    private final static String OPT_CHECKBOX_4_STRING_ID = "ShowCheckbox4StringId";
    private final static String OPT_CHECKBOX_5_STRING_ID = "ShowCheckbox5StringId";

    /** This flag should be used by collections whose year of most recent
     *  production should track the current year.
     *
     * TODO Make this easier to maintain, but make sure it doesn't break database
     *      upgrade functionality */
    public final static Integer OPTVAL_STILL_IN_PRODUCTION = 2020;


    private final static HashMap<String,String> SHOW_MINT_MARK_CHECKBOX_STRING_ID_OPT_MAP = new HashMap<>();

    static {
        SHOW_MINT_MARK_CHECKBOX_STRING_ID_OPT_MAP.put(OPT_SHOW_MINT_MARK_1, OPT_SHOW_MINT_MARK_1_STRING_ID);
        SHOW_MINT_MARK_CHECKBOX_STRING_ID_OPT_MAP.put(OPT_SHOW_MINT_MARK_2, OPT_SHOW_MINT_MARK_2_STRING_ID);
        SHOW_MINT_MARK_CHECKBOX_STRING_ID_OPT_MAP.put(OPT_SHOW_MINT_MARK_3, OPT_SHOW_MINT_MARK_3_STRING_ID);
        SHOW_MINT_MARK_CHECKBOX_STRING_ID_OPT_MAP.put(OPT_SHOW_MINT_MARK_4, OPT_SHOW_MINT_MARK_4_STRING_ID);
        SHOW_MINT_MARK_CHECKBOX_STRING_ID_OPT_MAP.put(OPT_SHOW_MINT_MARK_5, OPT_SHOW_MINT_MARK_5_STRING_ID);
    }

    private final static HashMap<String,String> CUSTOMIZABLE_CHECKBOX_STRING_ID_OPT_MAP = new HashMap<>();

    static {
        CUSTOMIZABLE_CHECKBOX_STRING_ID_OPT_MAP.put(OPT_CHECKBOX_1, OPT_CHECKBOX_1_STRING_ID);
        CUSTOMIZABLE_CHECKBOX_STRING_ID_OPT_MAP.put(OPT_CHECKBOX_2, OPT_CHECKBOX_2_STRING_ID);
        CUSTOMIZABLE_CHECKBOX_STRING_ID_OPT_MAP.put(OPT_CHECKBOX_3, OPT_CHECKBOX_3_STRING_ID);
        CUSTOMIZABLE_CHECKBOX_STRING_ID_OPT_MAP.put(OPT_CHECKBOX_4, OPT_CHECKBOX_4_STRING_ID);
        CUSTOMIZABLE_CHECKBOX_STRING_ID_OPT_MAP.put(OPT_CHECKBOX_5, OPT_CHECKBOX_5_STRING_ID);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the actionbar so that clicking the icon takes you back (SO 1010877)
        ActionBar actionBar = this.getSupportActionBar();
        if (actionBar != null) {
        actionBar.setDisplayHomeAsUpEnabled(true);
        }

        setContentView(R.layout.collection_creation_page);

        // NOTE: the UI is not fully inflated at this point (specifically, some
        // of the checkboxes which are added programmatically have not been
        // instantiated yet.)

        // Initialize our instance variables
        if(savedInstanceState != null)
        {
            // Pull in enough of the saved state to initialize the UI
            setInternalStateFromCollectionIndex(
                    savedInstanceState.getInt(_COIN_TYPE_INDEX),
                    (HashMap<String, Object>) savedInstanceState.getSerializable(_PARAMETERS));

        } else {
            // Initialize mCoinTypeIndex and related internal state to index 0
            setInternalStateFromCollectionIndex(0, null);
        }

        // If we have an AsyncProgressTask already running, inherit it
        AsyncProgressTask check = (AsyncProgressTask) getLastCustomNonConfigurationInstance();

        // TODO If there is a screen orientation change, it looks like a mProgressDialog gets leaked. :(
        if(check != null){
            mTask = check;
            // Change the task's listener to be the new activity. See note above AsyncTask
            // definition for more info.
            mTask.mListener = new AsyncProgressInterface() {
                @Override
                public void asyncProgressDoInBackground() {
                    // Not needed here
        }
                @Override
                public void asyncProgressOnPreExecute() {
                    // Not needed here
                }
                @Override
                public void asyncProgressOnPostExecute() {
                    completeProgressDialogAndFinishActivity();
                }
            };
            createProgressDialog();
        }

        // Next, we will finish setting up the various UI elements (creating
        // adapters, listeners, etc..  We won't set any of the values yet -
        // we will do that at the end.

        // Prepare the Spinner that gets what type of collection they want to make
        ArrayAdapter<CharSequence> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);

        for(int i = 0; i < MainApplication.COLLECTION_TYPES.length; i++)
        {
            spinnerAdapter.add(MainApplication.COLLECTION_TYPES[i].getCoinType());
        }

        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        Spinner coinTypeSelector = findViewById(R.id.coin_selector);
        coinTypeSelector.setAdapter(spinnerAdapter);
        coinTypeSelector.setOnItemSelectedListener(new OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent,
                    View view, int pos, long id) {

                // No need to do anything if onItemSelected was called but the selected index hasn't
                // changed since:
                //  - first activity initialization, or
                //  - activity initialization from SavedInstanceState
                if(mCoinTypeIndex == pos) {
                    return;
                }

                // When an item is selected, switch our internal state based on the collection type
                setInternalStateFromCollectionIndex(pos, null);

                // Reset the view for the new coin type
                updateViewFromState();

            }
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        // Create an OnKeyListener that can be used to hide the soft keyboard when the enter key
        // (or a few others) are pressed.
        //
        // TODO OnKeyListeners aren't guaranteed to work with software keyboards... find a better way
        // From https://developer.android.com/reference/android/view/View.OnKeyListener.html:
        // Interface definition for a callback to be invoked when a hardware key event is dispatched
        // to this view. The callback will be invoked before the key event is given to the view.
        // This is only useful for hardware keyboards; a software input method has no obligation to
        // trigger this listener.
        //
        // Has worked on all the devices I've tested on (which are all Samsung devices)

        OnKeyListener hideKeyboardListener = new OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                // If the event is a key-down event on the "enter" button
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_DPAD_DOWN || keyCode == KeyEvent.KEYCODE_DPAD_UP)) {
                    // This should hide the keyboard
                    // Thanks! http://stackoverflow.com/questions/1109022/how-to-close-hide-the-android-soft-keyboard
                    InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    // Returning true prevents the action that would ordinarily have happened from taking place
                    return keyCode == KeyEvent.KEYCODE_ENTER;
                }
                return false;
            }
        };

        // Set the OnKeyListener for the EditText
        final EditText nameEditText = findViewById(R.id.edit_enter_collection_name);
        nameEditText.setOnKeyListener(hideKeyboardListener);

        // Make a filter to block out bad characters
        InputFilter nameFilter = getCollectionNameFilter();
        nameEditText.setFilters(new InputFilter[]{nameFilter});

        // Set the listener for the show mint mark checkbox
        final CheckBox showMintMarkCheckBox = findViewById(R.id.check_show_mint_mark);
        showMintMarkCheckBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked){

                // Don't take any action if the value isn't changing - needed to prevent
                // loops that would get created by the call to updateViewFromState()
                Boolean optMintMarks = (Boolean) mParameters.get(OPT_SHOW_MINT_MARKS);
                if(optMintMarks != null && optMintMarks == isChecked){
                    return;
                }

                mParameters.put(OPT_SHOW_MINT_MARKS, isChecked);

                // Restore defaults for all of the mint mark checkboxes when this is unchecked
                if(!isChecked) {
                    for (String key : SHOW_MINT_MARK_CHECKBOX_STRING_ID_OPT_MAP.keySet()) {
                        if (mParameters.containsKey(key)) {
                            mParameters.put(key, mDefaults.get(key));
                        }
                    }
                }

                // Refresh the UI so that the individual mint mark checkboxes are either
                // hidden or displayed
                updateViewFromState();
            }
        });

        // Set the listener for the edit date range
        final CheckBox editDateRangeCheckBox = findViewById(R.id.check_edit_date_range);
        editDateRangeCheckBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked){

                // Don't take any action if the value isn't changing - needed to prevent
                // loops that would get created by the call to updateViewFromState()
                Boolean optEditDateRange = (Boolean)mParameters.get(OPT_EDIT_DATE_RANGE);
                if(optEditDateRange != null && optEditDateRange == isChecked){
                    return;
                }

                mParameters.put(OPT_EDIT_DATE_RANGE, isChecked);

                // Reset the start/stop year when the field is unchecked
                if(!isChecked) {
                    mParameters.put(OPT_START_YEAR, mDefaults.get(OPT_START_YEAR));
                    mParameters.put(OPT_STOP_YEAR, mDefaults.get(OPT_STOP_YEAR));
                }

                // Refresh the UI so that the start/stop year EditTexts are hidden or displayed
                updateViewFromState();
            }
        });

        // Instantiate an onCheckedChangeListener for use by all the simple checkboxes
        OnCheckedChangeListener checkboxChangeListener = new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                // The tag store the OPT_NAME associated with the button
                String optName = (String) compoundButton.getTag();
                mParameters.put(optName, isChecked);
            }
        };

        // Instantiate a LayoutParams for the simple checkboxes
        LayoutParams layoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

        LinearLayout showMintMarksContainer = findViewById(R.id.show_mint_mark_checkbox_container);

        // Create the ShowMintMark Checkboxes (even if they aren't needed right now)
        for (String optName : SHOW_MINT_MARK_CHECKBOX_STRING_ID_OPT_MAP.keySet()) {
            // Instantiate a checkbox in the UI for this option
            CheckBox box = showMintMarksContainer.findViewWithTag(optName);
            box.setOnCheckedChangeListener(checkboxChangeListener);
        }

        // Add any stand-alone, customizable checkboxes
        LinearLayout customizableCheckboxContainer = findViewById(R.id.customizable_checkbox_container);

        for(String optName : CUSTOMIZABLE_CHECKBOX_STRING_ID_OPT_MAP.keySet()){
            // Instantiate a checkbox in the UI for this option
            CheckBox box = customizableCheckboxContainer.findViewWithTag(optName);
            box.setOnCheckedChangeListener(checkboxChangeListener);
        }

        // Make a filter to block out non-numeric characters
        InputFilter digitFilter = new InputFilter() {
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                for (int i = start; i < end; i++) {
                    if (source.charAt(i) < '0' || source.charAt(i) > '9') {
                        // Don't allow these characters
                        return "";
                    }
                }
                return null;
            }
        };

        // Make a filter limiting the year text fields to 4 characters
        InputFilter yearLengthFilter = new InputFilter.LengthFilter(4);

        InputFilter[] yearEditTextFilters = new InputFilter[]{digitFilter, yearLengthFilter};

        // Set the OnKeyListener and InputFilters for the EditText
        final EditText startYearEditText = findViewById(R.id.edit_start_year);
        startYearEditText.setOnKeyListener(hideKeyboardListener);
        startYearEditText.setFilters(yearEditTextFilters);

        startYearEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override
            public void afterTextChanged(Editable s) {
                try {
                    mParameters.put(OPT_START_YEAR, Integer.valueOf(s.toString()));
                } catch (NumberFormatException e) {
                    // The only case that should trigger this is the empty string case, so set
                    // mStartYear to the default
                    mParameters.put(OPT_START_YEAR, mDefaults.get(OPT_START_YEAR));
                }
            }
        });

        // Set the OnKeyListener and InputFilters for the EditText
        final EditText stopYearEditText = findViewById(R.id.edit_stop_year);
        stopYearEditText.setOnKeyListener(hideKeyboardListener);
        stopYearEditText.setFilters(yearEditTextFilters);

        stopYearEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override
            public void afterTextChanged(Editable s) {
                try {
                    mParameters.put(OPT_STOP_YEAR, Integer.valueOf(s.toString()));
                } catch (NumberFormatException e) {
                    // The only case that should trigger this is the empty string case, so set
                    // mStopYear to the default
                    mParameters.put(OPT_STOP_YEAR, mDefaults.get(OPT_STOP_YEAR));
                }
            }
        });

        final Button makeCollectionButton = findViewById(R.id.create_page);
        makeCollectionButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //Go ahead and grab what is in the EditText
                final String collectionName = nameEditText.getText().toString();

                // Perform action on click
                if(collectionName.equals("")){
                    Toast.makeText(CoinPageCreator.this, "Please enter a name for the collection", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Validate the last year in the collection, if necessary
                if(mParameters.containsKey(OPT_EDIT_DATE_RANGE) &&
                        mParameters.get(OPT_EDIT_DATE_RANGE) == Boolean.TRUE){

                    boolean result = validateStartAndStopYears();
                    if(!result){
                        // The function will have already displayed a toast, so return
                        return;
                    }
                }

                // Ensure that at least one mint mark is selected
                if(mParameters.containsKey(OPT_SHOW_MINT_MARKS) &&
                        mParameters.get(OPT_SHOW_MINT_MARKS) == Boolean.TRUE){

                    boolean atLeastOneMintMarkSelected = false;
                    for(String optName : SHOW_MINT_MARK_CHECKBOX_STRING_ID_OPT_MAP.keySet()) {
                        if (mParameters.containsKey(optName) && mParameters.get(optName) == Boolean.TRUE) {
                            atLeastOneMintMarkSelected = true;
                            break;
                        }
                    }

                    if(!atLeastOneMintMarkSelected){

                        Toast.makeText(CoinPageCreator.this, "Please select at least one mint to collect coins from", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // Otherwise, good to go
                }

                //Get a list of all the database tables

                // TODO Move this into the AsyncTask so that we don't have to do two calls
                // to open the db
                // Open it again.  This one shouldn't take long
                DatabaseAdapter dbAdapter = new DatabaseAdapter(CoinPageCreator.this);
                dbAdapter.open();
                String checkNameResult = dbAdapter.checkCollectionName(collectionName);
                if(!checkNameResult.equals("")){
                    Toast.makeText(CoinPageCreator.this, checkNameResult, Toast.LENGTH_SHORT).show();
                    return;
                }
                final int newDisplayOrder = dbAdapter.getNextDisplayOrder();
                dbAdapter.close();

                //Now actually set up the mIdentifierList and mMintList
                populateCollectionArrays();
                mTask = new AsyncProgressTask(new AsyncProgressInterface() {
                    @Override
                    public void asyncProgressDoInBackground() {
                        // Create the table in the database
                        final String aCoinType = mCollectionObj.getCoinType();
                        createNewTable(collectionName, aCoinType, mCoinList, newDisplayOrder);
                    }
                    @Override
                    public void asyncProgressOnPreExecute() {
                        createProgressDialog();
                    }
                    @Override
                    public void asyncProgressOnPostExecute() {
                        completeProgressDialogAndFinishActivity();
                    }
                });
                mTask.execute();

                // Wait for it to finish and trigger the callback method
            }
        });

        // Create help dialog to create a new collection
        createAndShowHelpDialog("first_Time_screen2", R.string.tutorial_select_coin_and_create, this);

        // Finally, update the UI element values and display state
        // (VISIBLE vs. GONE) of the UI from the internal state.
        updateViewFromState();

        if(BuildConfig.DEBUG) {
            Log.d(APP_NAME, "Finished in onCreate");
        }

    }

    /** Returns an input filter for sanitizing collection names
     * @return The input filter
     */
    static InputFilter getCollectionNameFilter() {
        return new InputFilter() {
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                for (int i = start; i < end; i++) {
                    if (source.charAt(i) == '[' || source.charAt(i) == ']') {
                        // Don't allow these characters as they break the sql queries
                        return "";
                    }
                }
                return null;
            }
        };
    }

    /**
     * Updates the internal state based on a new coin type index
     * @param index The index of this coin type in the list of all collection types
     * @param parameters If not null, set mParameters to parameters.  Otherwise,
     *                   create a new HashMap for mParameters and assign it default
     *                   values based on the new collection type.
     *
     */
    private void setInternalStateFromCollectionIndex(int index, HashMap<String, Object> parameters){

        mCoinTypeIndex = index;

        mCollectionObj = MainApplication.COLLECTION_TYPES[mCoinTypeIndex];

        // Get the defaults for the parameters that this new collection type cares about
        mDefaults = new HashMap<>();
        mCollectionObj.getCreationParameters(mDefaults);

        if (parameters == null) {
            mParameters = new HashMap<>();
            mCollectionObj.getCreationParameters(mParameters);

        } else {
            // Allow the parameters to be passed in for things like testing and on screen rotation
            mParameters = parameters;
        }

        // TODO Validate mParameters
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {

        savedInstanceState.putInt(_COIN_TYPE_INDEX, mCoinTypeIndex);
        savedInstanceState.putSerializable(_PARAMETERS, mParameters);

        super.onSaveInstanceState(savedInstanceState);
    }

    /**
     *  Updates the UI from the internal state.  This allows us to easily
     *  reset the state of the UI when a big change has occurred (Ex: the
     *  individual showMintMark checkboxes should be shown because the
     *  showMintMarks checkbox was set to True)
     */
    private void updateViewFromState(){

        Spinner coinTypeSelector = findViewById(R.id.coin_selector);
        CheckBox showMintMarkCheckBox = findViewById(R.id.check_show_mint_mark);
        LinearLayout showMintMarkCheckboxContainer = findViewById(R.id.show_mint_mark_checkbox_container);

        CheckBox editDateRangeCheckBox = findViewById(R.id.check_edit_date_range);
        LinearLayout editStartYearLayout = findViewById(R.id.start_year_layout);
        LinearLayout editStopYearLayout = findViewById(R.id.stop_year_layout);
        EditText editStartYear = findViewById(R.id.edit_start_year);
        EditText editStopYear = findViewById(R.id.edit_stop_year);

        LinearLayout customizableCheckboxContainer = findViewById(R.id.customizable_checkbox_container);

        // Start with the Collection Type list index
        coinTypeSelector.setSelection(mCoinTypeIndex, false);

        // Handle the showMintMarks checkbox
        Boolean showMintMarks = (Boolean) mParameters.get(OPT_SHOW_MINT_MARKS);
        if(showMintMarks != null) {
            showMintMarkCheckBox.setChecked(showMintMarks);
            showMintMarkCheckBox.setVisibility(View.VISIBLE);
        } else {
            showMintMarks = false;
            showMintMarkCheckBox.setVisibility(View.GONE);
        }

        // Now handle the individual showMintMark checkboxes
        for(String optName : SHOW_MINT_MARK_CHECKBOX_STRING_ID_OPT_MAP.keySet()){
            CheckBox uiElement = showMintMarkCheckboxContainer.findViewWithTag(optName);
            Boolean paramOptValue = (Boolean) mParameters.get(optName);
            if(paramOptValue != null && showMintMarks){
                String stringIdOptName = SHOW_MINT_MARK_CHECKBOX_STRING_ID_OPT_MAP.get(optName);
                Integer optStringId = (Integer)mParameters.get(stringIdOptName);
                if(optStringId != null){
                    uiElement.setText(optStringId);
                    uiElement.setChecked(paramOptValue);
                    uiElement.setVisibility(View.VISIBLE);
                } else {
                    // Should never reach this
                    uiElement.setVisibility(View.GONE);
                }
            } else {
                uiElement.setVisibility(View.GONE);
            }
        }

        // Update the UI of the editDateRange checkbox and the associated
        // start/stop year EditTexts
        Boolean editDateRange = (Boolean) mParameters.get(OPT_EDIT_DATE_RANGE);
        if(editDateRange != null){
            Integer startYear = (Integer) mParameters.get(OPT_START_YEAR);
            Integer stopYear = (Integer) mParameters.get(OPT_STOP_YEAR);

            editDateRangeCheckBox.setChecked(editDateRange);
            editDateRangeCheckBox.setVisibility(View.VISIBLE);

            if(editDateRange && startYear != null && stopYear != null) {
                editStartYearLayout.setVisibility(View.VISIBLE);
                editStartYear.setText(String.valueOf(startYear));
                editStopYearLayout.setVisibility(View.VISIBLE);
                editStopYear.setText(String.valueOf(stopYear));
            } else {
                editStartYearLayout.setVisibility(View.GONE);
                editStopYearLayout.setVisibility(View.GONE);
            }
        } else {
            editDateRangeCheckBox.setVisibility(View.GONE);
            editStartYearLayout.setVisibility(View.GONE);
            editStopYearLayout.setVisibility(View.GONE);
        }

        // Handle the customizable checkboxes
        for(String optName : CUSTOMIZABLE_CHECKBOX_STRING_ID_OPT_MAP.keySet()){
            CheckBox uiElement = customizableCheckboxContainer.findViewWithTag(optName);
            Boolean paramOptValue = (Boolean) mParameters.get(optName);
            if(paramOptValue != null){
                String stringIdOptName = CUSTOMIZABLE_CHECKBOX_STRING_ID_OPT_MAP.get(optName);
                Integer optStringId = (Integer)mParameters.get(stringIdOptName);
                if(optStringId != null){
                    uiElement.setText(optStringId);
                    uiElement.setChecked(paramOptValue);
                    uiElement.setVisibility(View.VISIBLE);
                } else {
                    // Should never reach this
                    uiElement.setVisibility(View.GONE);
                }
            } else {
                uiElement.setVisibility(View.GONE);
            }
        }
    }

    /**
     *  Helper function to validate the collection start and stop years
     *
     *  NOTE: This doesn't rely on the UI elements having listeners that update the internal state
     *        vars so that we can use this before the listeners have been created (or if no
     *        listeners will be created, in the case of testing.)
     */
    private boolean validateStartAndStopYears(){

        EditText editStartYear = findViewById(R.id.edit_start_year);
        EditText editStopYear = findViewById(R.id.edit_stop_year);

        Integer startYear = (Integer) mParameters.get(OPT_START_YEAR);
        Integer stopYear = (Integer) mParameters.get(OPT_STOP_YEAR);

        Integer minStartYear = (Integer) mDefaults.get(OPT_START_YEAR);
        Integer maxStartYear = (Integer) mDefaults.get(OPT_STOP_YEAR);

        if(startYear == null || stopYear == null || minStartYear == null || maxStartYear == null){
            // Shouldn't reach this as all collections should have start/end dates
            return true;
        }

        if(stopYear > maxStartYear){

            Toast.makeText(CoinPageCreator.this,
                "Highest possible ending year is " + maxStartYear +
                        ".  Note, new years will automatically be added as they come.",
                Toast.LENGTH_LONG).show();

            mParameters.put(OPT_STOP_YEAR, maxStartYear);
            editStopYear.setText(String.valueOf(maxStartYear));
            return false;
        }
        if(stopYear < minStartYear){

            Toast.makeText(CoinPageCreator.this,
                "Ending year can't be less than the collection starting year (" + minStartYear +
                        ")",
                Toast.LENGTH_SHORT).show();

            mParameters.put(OPT_STOP_YEAR, maxStartYear);
            editStopYear.setText(String.valueOf(maxStartYear));
            return false;
        }

        if(startYear < minStartYear){

            Toast.makeText(CoinPageCreator.this,
                "Lowest possible starting year is " + minStartYear,
                Toast.LENGTH_LONG).show();

            mParameters.put(OPT_START_YEAR, minStartYear);
            editStartYear.setText(String.valueOf(minStartYear));
            return false;

        } else if(startYear > maxStartYear){

            Toast.makeText(CoinPageCreator.this,
                "Starting year can't be greater than the collection ending year (" + maxStartYear +
                        ")",
                Toast.LENGTH_SHORT).show();

            mParameters.put(OPT_START_YEAR, minStartYear);
            editStartYear.setText(String.valueOf(minStartYear));
            return false;
        }

        // Finally, validate them with respect to each other
        if(startYear > stopYear){
            Toast.makeText(CoinPageCreator.this, "Starting year can't be greater than the ending year", Toast.LENGTH_SHORT).show();

            mParameters.put(OPT_START_YEAR, minStartYear);
            editStartYear.setText(String.valueOf(minStartYear));
            mParameters.put(OPT_STOP_YEAR, maxStartYear);
            editStopYear.setText(String.valueOf(maxStartYear));
            return false;
        }

        // Yay, validation succeeded
        return true;
    }

    /**
     *  Helper function to call the make collection method corresponding to the creation parameters
     *  NOTE: This is public so we can use it with our current test bench
     */
    private void populateCollectionArrays(){

        mCollectionObj.populateCollectionLists(mParameters, mCoinList);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        if(item.getItemId() == android.R.id.home) {
            this.onBackPressed();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }
    
    @Override
    public Object onRetainCustomNonConfigurationInstance(){

        if(mProgressDialog != null && mProgressDialog.isShowing()){
            mProgressDialog.dismiss();
            return mTask;
        } else {
            // No dialog showing, do nothing
            return null;
        }
    }

    @Override
    public void onDestroy(){

        // TODO Not a perfect solution, but assuming this gets called, we should cut down on the
        // race condition inherent in how we do our AsyncTask
        if(mTask != null) {
            mTask.mListener = null;
        }
        super.onDestroy();

    }

    /**
     * Create a database table for a new collection
     * @param tableName Name of the table
     * @param coinType Type of coin
     * @param coinList List of coin slots
     * @param displayOrder Display order of the collection
     */
    public void createNewTable(String tableName, String coinType, ArrayList<CoinSlot> coinList,
                                      int displayOrder){
        // Open it again.  This one shouldn't take long
        DatabaseAdapter dbAdapter = new DatabaseAdapter(this);
        dbAdapter.open();
        dbAdapter.createNewTable(tableName, coinType, coinList, displayOrder);
        dbAdapter.close();
    }

    /**
     * Create a new progress dialog for initial collection creation
     */
    private void createProgressDialog(){
        if (mProgressDialog != null){
            // Progress bar already being displayed
            return;
        }
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setMessage("Creating Collection...");
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setProgress(0);
        mProgressDialog.show();
    }

    /**
     * Hide the dialog and finish the activity
     */
    private void completeProgressDialogAndFinishActivity(){
        if(mProgressDialog != null) {
            if (mProgressDialog.isShowing()) {
                mProgressDialog.dismiss();
            }
            mProgressDialog = null;
        }
        this.finish();
    }
}