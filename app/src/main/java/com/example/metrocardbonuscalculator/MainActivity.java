package com.example.metrocardbonuscalculator;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.text.InputFilter;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;

public class MainActivity extends AppCompatActivity {
    public static final String PREF_KEY_VERSION_CODE = "versionCode";
    public static final String PREF_KEY_SPINNER_POS = "spinnerPos";
    public static final String PREF_KEY_BONUS_PCT = "bonusPct";
    public static final String PREF_KEY_BONUS_MIN = "bonusMin";
    public static final String PREF_KEY_INCREMENT = "increment";

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

    private BigDecimal[] fares;
    private BigDecimal bonusPct;
    private BigDecimal bonusMin;
    private BigDecimal increment;

    private EditText editBalance;
    private EditText editRides;
    private Spinner fareSpinner;

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //ActionBar actionBar = getSupportActionBar();
        //actionBar.setLogo(R.mipmap.ic_launcher);
        //actionBar.setDisplayUseLogoEnabled(true);
        //actionBar.setDisplayShowHomeEnabled(true);

        fares = new BigDecimal[keys.length];

        editBalance = findViewById(R.id.edit_balance);
        editBalance.setFilters(new InputFilter[]{new DecimalInputFilter(2)});
        editRides = findViewById(R.id.edit_rides);
        fareSpinner = findViewById(R.id.fare_spinner);

        Button calculateBtn = findViewById(R.id.button_calculate);
        calculateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calculate();
            }
        });

        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        /* Post-update routine called on first run and on updates. */
        int versionCode = AndroidUtilities.getVersionCode(this);
        if (versionCode != prefs.getInt(PREF_KEY_VERSION_CODE, -1)) {
            onUpdate();
            Editor editor = prefs.edit();
            editor.putInt(PREF_KEY_VERSION_CODE, versionCode);
            editor.commit();
        }
    }

    @Override
    protected void onPause() {
        saveSpinnerPos();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        retrieveData();
        populateSpinner();
        restoreSpinnerPos();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void retrieveData() {
        for (int i = 0; i < fares.length; ++i) {
            String defaultFare = getString(defaultIds[i]);
            fares[i] = new BigDecimal(prefs.getString(keys[i], defaultFare));
        }

        String defaultBonusPct = getString(R.string.default_bonus_percentage);
        bonusPct = new BigDecimal(prefs.getString(PREF_KEY_BONUS_PCT, defaultBonusPct));

        String defaultBonusMin = getString(R.string.default_bonus_min);
        bonusMin = new BigDecimal(prefs.getString(PREF_KEY_BONUS_MIN, defaultBonusMin));

        String defaultIncrement = getString(R.string.default_increment);
        increment = new BigDecimal(prefs.getString(PREF_KEY_INCREMENT, defaultIncrement));
    }

    private void populateSpinner() {
        String[] opts = new String[fares.length];
        for (int i = 0; i < opts.length; ++i) {
            String cost = fares[i].toPlainString();
            String name = getString(nameIds[i]);
            opts[i] = getString(R.string.spinner_entry, name, cost);
        }
        fareSpinner.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, opts));
    }

    private void saveSpinnerPos() {
        int spinnerPos = fareSpinner.getSelectedItemPosition();
        Editor editor = prefs.edit();
        editor.putInt(PREF_KEY_SPINNER_POS, spinnerPos);
        editor.commit();
    }

    private void restoreSpinnerPos() {
        int spinnerPos = prefs.getInt(PREF_KEY_SPINNER_POS, 0);
        fareSpinner.setSelection(spinnerPos, true);
    }

    private void calculate() {
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

        MetroCardCalculator calc = new MetroCardCalculator(bonusMin, bonusPct, increment);

        BigDecimal fare = fares[fareSpinner.getSelectedItemPosition()];
        BigDecimal payment = calc.calculatePayment(fare, balance, rides);
        BigDecimal bonus = calc.calculateBonus(payment);
        BigDecimal newBalance = balance.add(payment).add(bonus);
        BigDecimal[] div = newBalance.divideAndRemainder(fare);
        BigInteger ridesOnCard = div[0].toBigInteger();
        BigDecimal remainder = div[1];

        String msg = formatResult(ridesOnCard, payment, newBalance, remainder, bonus);

        new AlertDialog.Builder(this)
                .setTitle(R.string.result_title)
                .setMessage(msg)
                .setNeutralButton(R.string.close, null)
                .show();
    }

    private String formatResult(BigInteger rides,
                                BigDecimal payment,
                                BigDecimal newBalance,
                                BigDecimal remainder,
                                BigDecimal bonus) {

        DecimalFormat df = new DecimalFormat("#,##0.00");

        String paymentStr = getString(R.string.result_cost, df.format(payment));
        String fareStr = getResources().getQuantityString(R.plurals.fare_plurals,
                rides.intValue(),
                rides.intValue());
        String newBalanceStr = getString(R.string.result_new_balance_info,
                df.format(newBalance),
                fareStr,
                remainder.toPlainString());
        String bonusStr = getString(R.string.result_bonus_info, df.format(bonus));

        String lineSeparator = System.getProperty("line.separator");

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

    private void onUpdate() {
        SettingsActivity.restoreDefaultSettings(this);
    }
}
