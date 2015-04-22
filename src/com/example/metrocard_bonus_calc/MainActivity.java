package com.example.metrocard_bonus_calc;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.InputFilter;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;

public class MainActivity extends SherlockActivity {
    private static final String PREFERENCES = "preferences";
    
    private static final String USD_FORMAT = "#,##0.00";
    
    private static final String[] keys = { 
        "regular",
        "reduced",
        "expressBus",
        "expressBusReduced"
    };
    
    private static final int[] nameIds = {
        R.string.regular,
        R.string.reduced,
        R.string.express_bus,
        R.string.express_bus_reduced
    };
    
    private static final int[] defaultIds = {
        R.string.default_regular,
        R.string.default_reduced,
        R.string.default_express_bus,
        R.string.default_express_bus_reduced
    };
    
    private SharedPreferences prefs;
    private SharedPreferences defaultPrefs;
    private Editor defaultEditor;
    private Editor prefsEditor;
    private EditText editBalance;
    private EditText editRides;
    private Spinner fareSpinner;
    private BigDecimal[] fares;
    private BigDecimal bonusPercentage;
    private BigDecimal bonusMin;
    private BigDecimal increment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        fares = new BigDecimal[keys.length];

        editBalance = (EditText)findViewById(R.id.edit_balance);
        editBalance.setFilters(new InputFilter[]{ new DecimalInputFilter(2) });
        editRides = (EditText)findViewById(R.id.edit_rides);
        fareSpinner = (Spinner)findViewById(R.id.fare_spinner);
        
        defaultPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs = getSharedPreferences(PREFERENCES, MODE_PRIVATE);
        defaultEditor = defaultPrefs.edit();
        prefsEditor = prefs.edit();
        
        /* 
         * Checks if the app has been updated. If so, the fare data is reset.
         * Note: The first run counts as an update.
         */
        int versionCode = AndroidUtilities.getVersionCode(this);
        if (checkIfUpdated(versionCode)) {
            resetData();
            prefsEditor.putInt("versionCode", versionCode);
            prefsEditor.commit();
        }
    }
    
    @Override
    protected void onPause() {
        saveSpinnerPosition();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        /* 
         * This means of refreshing data assumes that values are changed in the
         * settings activity. If that is not the case, use a listener instead.
         */
        getData();
        populateSpinner();
        restoreSpinnerPosition();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.main_activity_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.action_settings:
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    
    /** Called when the "calculate" button is pressed. */
    public void compute(View view) {
        BigDecimal balance;
        BigInteger rides;
        try {
            String balanceStr = editBalance.getText().toString();
            balance = new BigDecimal(balanceStr);
            String ridesStr = editRides.getText().toString();
            rides = new BigInteger(ridesStr);
        } catch (NumberFormatException e) { // Thrown when a field is blank.
            String msg = getString(R.string.message_all_fields_required);
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            return;
        }

        MetrocardCalculator calc = new MetrocardCalculator(bonusMin, bonusPercentage, increment);

        int fareId = fareSpinner.getSelectedItemPosition();
        BigDecimal fare = fares[fareId];
        BigDecimal payment = calc.computePayment(fare, balance, rides);
        BigDecimal bonus = calc.computeBonus(payment);
        BigDecimal newBalance = balance.add(payment).add(bonus);
        BigDecimal[] div = newBalance.divideAndRemainder(fare);
        BigInteger ridesOnCard = div[0].toBigInteger();
        BigDecimal remainder = div[1];
        
        resultDialog(formatResult(ridesOnCard, payment, newBalance, remainder, bonus));
    }

    /**
     * Retrieves fares and other data required for calculations.
     * Default values are from data.xml.
     */
    private void getData() {        
        for (int i = 0; i < fares.length; ++i) {
            String s = defaultPrefs.getString(keys[i], getString(defaultIds[i]));
            fares[i] = new BigDecimal(s);
        }
        
        String s1 = defaultPrefs.getString("bonusPercentage", getString(R.string.default_bonus_percentage));
        bonusPercentage = new BigDecimal(s1);
        
        String s2 = defaultPrefs.getString("bonusMin", getString(R.string.default_bonus_min));
        bonusMin = new BigDecimal(s2);
        
        String s3 = defaultPrefs.getString("increment", getString(R.string.default_increment));
        increment = new BigDecimal(s3);
    }

    /** Persists the position of the fare drop-down menu. */
    private void saveSpinnerPosition() {
        int spinnerPosition = fareSpinner.getSelectedItemPosition();
        prefsEditor.putInt("spinnerPosition", spinnerPosition);
        prefsEditor.commit();
    }

    /** Restores the position of the fare drop-down menu. */
    private void restoreSpinnerPosition() {
        int spinnerPosition = prefs.getInt("spinnerPosition", 0);
        fareSpinner.setSelection(spinnerPosition, true);
    }
    
    private String makeSpinnerEntry(int fareId) {
        String cost = fares[fareId].toPlainString();
        return getString(R.string.spinner_entry, getString(nameIds[fareId]), cost);
    }
    
    /** Fills the fare drop-down menu. Must be called after getData. */
    private void populateSpinner() {
        String[] opts = new String[fares.length];
        for (int i = 0; i < 4; ++i) {
            opts[i] = makeSpinnerEntry(i);
        }
        fareSpinner.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, opts));
    }
    
    private String formatResult(BigInteger rides,
                                BigDecimal payment,
                                BigDecimal newBalance,
                                BigDecimal remainder,
                                BigDecimal bonus) {
        
        DecimalFormat df = new DecimalFormat(USD_FORMAT);
        
        String lineSeparator = System.getProperty("line.separator");
        String paymentStr = getString(R.string.result_cost, df.format(payment));
        String fareStr;
        if (rides.compareTo(BigInteger.ONE) == 0) {
            fareStr = getString(R.string.fare_singular);
        } else {
            fareStr = getString(R.string.fare_plural);
        }
        String newBalanceStr = getString(R.string.result_new_balance_info,
                df.format(newBalance), rides.toString(), fareStr, remainder.toPlainString());
        String bonusStr = getString(R.string.result_bonus_info, df.format(bonus));

        StringBuilder builder = new StringBuilder();
        builder.append(paymentStr);
        builder.append(lineSeparator);
        builder.append(lineSeparator);
        builder.append(newBalanceStr);
        builder.append(lineSeparator);
        builder.append(lineSeparator);
        builder.append(bonusStr);
        
        return builder.toString();
    }

    private void resultDialog(String result) {
        new AlertDialog.Builder(this)
        .setTitle(R.string.result_title)
        .setMessage(result)
        .setNeutralButton(R.string.close, null)
        .show();
    }
    
    /**
     * Returns true on the first run and after an update.
     * After performing whatever update routine is necessary, save the new
     * version code into the preferences file so that this will return false.
     */
    private boolean checkIfUpdated(int versionCode) {
        if (!prefs.contains("versionCode")) {
            return true;
        }
        int storedVersionCode = prefs.getInt("versionCode", 0);
        return storedVersionCode != versionCode;
    }
    
    /** Re-loads all MetroCard-related data from data.xml. */
    private void resetData() {
        defaultEditor.clear();
        defaultEditor.commit();
        PreferenceManager.setDefaultValues(this, R.xml.preferences_fare_values, true);
        PreferenceManager.setDefaultValues(this, R.xml.preferences_other_values, true);
    }
}
