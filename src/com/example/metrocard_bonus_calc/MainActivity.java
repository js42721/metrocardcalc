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
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
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
    public static final String PREFERENCES = "preferences";
    
    private static final String USD_FORMAT = "#,##0.00";
    
    /* Fare drop-down menu positions. */
    private static final int REGULAR = 0;
    private static final int REDUCED = 1;
    private static final int EXP_BUS = 2;
    private static final int EXP_BUS_REDUCED = 3;
    
    private SharedPreferences prefs;
    private SharedPreferences defaultPrefs;
    private Editor defaultEditor;
    private Editor prefsEditor;
    private LinearLayout layoutBalance;
    private EditText editBalance;
    private EditText editRides;
    private Spinner fareSpinner;
    private BigDecimal bonusPercentage;
    private BigDecimal bonusMin;
    private BigDecimal increment;
    private BigDecimal regular;
    private BigDecimal reduced;
    private BigDecimal expressBus;
    private BigDecimal expressBusReduced;
    private BigDecimal newCardFee;
    private boolean newCard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        layoutBalance = (LinearLayout)findViewById(R.id.current_balance_layout);
        editBalance = (EditText)findViewById(R.id.edit_balance);
        editBalance.setFilters(new InputFilter[]{ new DecimalInputFilter(2) });
        editRides = (EditText)findViewById(R.id.edit_rides);
        fareSpinner = (Spinner)findViewById(R.id.fare_spinner);
        defaultPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs = getSharedPreferences(PREFERENCES, MODE_PRIVATE);
        defaultEditor = defaultPrefs.edit();
        prefsEditor = prefs.edit();

        RadioGroup cardTypeRadGroup = (RadioGroup)findViewById(R.id.radio_group_card_type);
        cardTypeRadGroup.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                /* 
                 * Hides the current balance section of the layout if the user
                 * is buying a new card.
                 */ 
                switch (checkedId) {
                case R.id.radio_new_card:
                    layoutBalance.setVisibility(View.GONE);
                    newCard = true;
                    break;
                case R.id.radio_existing_card:
                    layoutBalance.setVisibility(View.VISIBLE);
                    editBalance.requestFocus();
                    newCard = false;
                    break;
                }
            }
        });

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
        BigDecimal balance = BigDecimal.ZERO;
        BigInteger rides = null;
        try {
            if (!newCard) {
                String balanceStr = editBalance.getText().toString();
                balance = new BigDecimal(balanceStr);
            }
            String ridesStr = editRides.getText().toString();
            rides = new BigInteger(ridesStr);
        } catch (NumberFormatException e) { // Thrown when a field is blank.
            String msg = getString(R.string.message_all_fields_required);
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            return;
        }

        MetrocardCalculator calc =
                new MetrocardCalculator(bonusPercentage, bonusMin, increment);
        
        BigDecimal payment = calc.computePayment(getSelectedFare(), balance, rides);
        BigDecimal bonus = calc.computeBonus(payment);
        BigDecimal newBalance = balance.add(payment).add(bonus);
        
        if (newCard) {
            /* If a new card is being purchased, a fee is applied. */
            payment = payment.add(newCardFee);
        }
        
        resultDialog(formatResult(payment, newBalance, bonus));
    }

    /**
     * Retrieves fares and other data required for calculations.
     * Default values are from data.xml.
     */
    private void getData() {
        regular = new BigDecimal(defaultPrefs.getString("regular", 
                getString(R.string.default_regular)));
        reduced = new BigDecimal(defaultPrefs.getString("reduced", 
                getString(R.string.default_reduced)));
        expressBus = new BigDecimal(defaultPrefs.getString("expressBus", 
                getString(R.string.default_express_bus)));
        expressBusReduced = new BigDecimal(defaultPrefs.getString("expressBusReduced", 
                getString(R.string.default_express_bus_reduced)));
        bonusPercentage = new BigDecimal(defaultPrefs.getString("bonusPercentage", 
                getString(R.string.default_bonus_percentage)));
        bonusMin = new BigDecimal(defaultPrefs.getString("bonusMin", 
                getString(R.string.default_bonus_min)));
        increment = new BigDecimal(defaultPrefs.getString("increment", 
                getString(R.string.default_increment)));
        newCardFee = new BigDecimal(defaultPrefs.getString("newCardFee", 
                getString(R.string.default_new_card_fee)));
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

    /** resId is the ID of the fare descriptor. */
    private String makeSpinnerEntry(int resId, String cost) {
        return getString(R.string.spinner_entry, getString(resId), cost);
    }
    
    /** Fills the fare drop-down menu. Must be called after getData. */
    private void populateSpinner() {
        String[] opts = new String[4];
        opts[REGULAR] = makeSpinnerEntry(R.string.regular, regular.toPlainString());
        opts[REDUCED] = makeSpinnerEntry(R.string.reduced, reduced.toPlainString());  
        opts[EXP_BUS] = makeSpinnerEntry(R.string.express_bus, expressBus.toPlainString());
        opts[EXP_BUS_REDUCED] = makeSpinnerEntry(R.string.express_bus_reduced, expressBusReduced.toPlainString());
        fareSpinner.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, opts));
    }
    
    /** Returns the fare price selected in the fare drop-down menu. */
    private BigDecimal getSelectedFare() {
        switch (fareSpinner.getSelectedItemPosition()) {
        case REGULAR:
            return regular;
        case REDUCED:
            return reduced;
        case EXP_BUS:
            return expressBus;
        case EXP_BUS_REDUCED:
            return expressBusReduced;
        default:
            return null;
        }
    }
    
    private String formatResult(BigDecimal payment, BigDecimal newBalance, BigDecimal bonus) {
        DecimalFormat df = new DecimalFormat(USD_FORMAT);
        String paymentStr = getString(R.string.result_cost, df.format(payment));
        String newBalanceStr = getString(R.string.result_new_card_info, df.format(newBalance));
        String bonusStr = getString(R.string.result_bonus_info, df.format(bonus));
        return paymentStr + "\n\n" + newBalanceStr + "\n\n" + bonusStr;
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
