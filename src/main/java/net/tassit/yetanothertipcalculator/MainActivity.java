/**
 Copyright 2014 Cody Huzarski (chuzarski@gmail.com)

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.

 */

package net.tassit.yetanothertipcalculator;

import android.app.Activity;
import android.app.Service;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import net.tassit.yetanothertipcalculator.Exceptions.InvalidUserValueException;

import static android.view.View.OnClickListener;
import static android.widget.SeekBar.OnSeekBarChangeListener;


public class MainActivity extends Activity {


    private final String logtag = "MainActivity";

    private TextView resultTextView;
    private TextView tipPercentTextView;
    private SeekBar percentSeekBar;
    private EditText billEditText;
    private EditText splitEditText;
    private Button calculateButton;

    private EventHandler eHandler;
    private InputMethodManager imm;

    //numbers for the application
    private int seekbarPercent = 15; //change this to change the default tip percentage
    private int tipSplit;
    private double billTotal;
    private double tipAmount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Do not want to support screen rotation - it is useless for this app
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        //initialize UI components
        resultTextView = (TextView) findViewById(R.id.result_textview);
        tipPercentTextView = (TextView) findViewById(R.id.tip_percent_textview);
        percentSeekBar = (SeekBar) findViewById(R.id.percent_seekbar);
        billEditText = (EditText) findViewById(R.id.bill_textedit);
        splitEditText = (EditText) findViewById(R.id.party_split_textedit);
        calculateButton = (Button) findViewById(R.id.calculate_button);

        //setup SeekBar and set default tip percent
        percentSeekBar.setMax(80);
        percentSeekBar.setProgress(seekbarPercent);
        uiUpdateTipPercentView(seekbarPercent);

        //set default split
        splitEditText.setText("1"); //change this value to modify the default split

        //initialize the result textview
        resultTextView.setText("Enter the bill total, split and percentage.");

        //initialize the Event Handler
        eHandler = new EventHandler();
        percentSeekBar.setOnSeekBarChangeListener(eHandler);
        calculateButton.setOnClickListener(eHandler);

        //setup input method manager so that the keyboard can be manipulated
        imm = (InputMethodManager) this.getSystemService(Service.INPUT_METHOD_SERVICE);

        //app should be ready
        Log.v(logtag, "Application Ready");

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_about) {

            uiHideAllKeyboards();
            resultTextView.setText(getResources().getString(R.string.about_app));
        }
        return super.onOptionsItemSelected(item);
    }

    //UI related methods

    private void uiHideAllKeyboards() {
        //hide the keyboards
        imm.hideSoftInputFromWindow(splitEditText.getWindowToken(), 0);
        imm.hideSoftInputFromWindow(billEditText.getWindowToken(), 0);
    }

    /**
     * Updates the tip % string
     * @param progress the % to set
     */
    private void uiUpdateTipPercentView(int progress) {
        String txt;
        txt = getResources().getString(R.string.tip_percent_label, progress);
        tipPercentTextView.setText(txt + "%");
        Log.d(logtag, String.format("Updated percent seekbar to %d", progress));
    }

    /**
     * Updates the results TextView
     * This shows the final result of the calculation
     */
    private void uiDisplayCalculationResults() {
        String resultString;
        if(tipSplit > 1) {
            //multiple split, use plural string
            resultString = getResources().getString(R.string.result_plural, tipAmount, billTotal + tipAmount,
                    tipAmount / tipSplit, (billTotal + tipAmount) / tipSplit);
        } else {
            //use singular string
            resultString = getResources().getString(R.string.result_singular, tipAmount, billTotal + tipAmount);
        }

        resultTextView.setText(resultString);
    }

    //Calculation Methods

    /**
     * This method calls other methods to calculate the tip instead of using
     * one giant method.
     */
    private void doTipCalculation() {
        //try to retrieve flawless user input
        try {
            getAndValidateUserInput();
            tipAmount = calculateTip(billTotal, seekbarPercent);
            //log the tip
            Log.d(logtag, String.format("Tip set to %f", tipAmount));
            uiDisplayCalculationResults();
            //cannot see results with keyboards in the way
            uiHideAllKeyboards();
        } catch (InvalidUserValueException e) {
            //flash try again
            resultTextView.setText(getResources().getString(R.string.tryagain_message));
        }
    }

    /**
     * This method fetches the numbers that the user supplies, but also checks to ensure that the
     * numbers are correct (non-zero)
     * @exception java.lang.IllegalArgumentException
     */
    private void getAndValidateUserInput() throws InvalidUserValueException {
        int split;
        double bill;
        split = 0;
        bill = 0;
        try {
            split = Integer.parseInt(splitEditText.getText().toString());
            bill = Double.parseDouble(billEditText.getText().toString());
        } catch (IllegalArgumentException e) {
            Log.e(logtag, "String for bill total or split is not a valid double or integer - while validating input");
            //flash try again
            resultTextView.setText(getResources().getString(R.string.tryagain_message));
        } finally {
            //check that these values are good
            if(split > 0) {
                tipSplit = split;
            } else {
                //not going to work
                Toast.makeText(this, getResources().getString(R.string.invalid_split_val), Toast.LENGTH_SHORT).show();
                imm.showSoftInputFromInputMethod(splitEditText.getWindowToken(), 0);
                splitEditText.requestFocus();
                throw new InvalidUserValueException();
            }

            if(bill > 0) {
                billTotal = bill;
            } else {
                Toast.makeText(this, getResources().getString(R.string.invalid_bill_val), Toast.LENGTH_SHORT).show();
                imm.showSoftInputFromInputMethod(billEditText.getWindowToken(), 0);
                billEditText.requestFocus();
                throw new InvalidUserValueException();
            }
        }

    }

    private double calculateTip(double bill, int percent) {
        return tipAmount = (bill * ((float) percent / 100));
    }

    private class EventHandler implements OnSeekBarChangeListener, OnClickListener {

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            //no use checking for which seekbar, we only have one.
            //update seekbarPercent
            seekbarPercent = progress;
            //update the textview
            uiUpdateTipPercentView(seekbarPercent);
        }

        @Override
        public void onClick(View v) {
            if(v == calculateButton) {
                //fire off a calculation
                doTipCalculation();
            }
        }

        //not required
        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    }

}
