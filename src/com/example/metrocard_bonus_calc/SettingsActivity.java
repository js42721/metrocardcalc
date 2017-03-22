package com.example.metrocard_bonus_calc;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceManager;
import android.text.InputFilter;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.MenuItem;

import java.math.BigDecimal;
import java.text.DecimalFormat;

@SuppressWarnings("deprecation")
public class SettingsActivity extends SherlockPreferenceActivity {
    private static final String ACTION_FARE_VALUES = "com.example.metrocard_bonus_calc.ACTION_FARE_VALUES";
    private static final String ACTION_OTHER_VALUES = "com.example.metrocard_bonus_calc.ACTION_OTHER_VALUES";

    private static final String USD_FORMAT = "#,##0.00";
    private static final String PERCENT_FORMAT = "#.##";

    private DecimalFormat formatUSD;
    private DecimalFormat formatPercent;
    private OnPreferenceChangeListener prefListener;
    private OnSharedPreferenceChangeListener sharedPrefListener;
    private SharedPreferences defaultPrefs;
    private Editor defaultEditor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        formatUSD = new DecimalFormat(USD_FORMAT);
        formatPercent = new DecimalFormat(PERCENT_FORMAT);
        defaultPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        defaultEditor = defaultPrefs.edit();

        /* Performs input validation. */
        prefListener = new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                /* Ensures that all fields are filled with data. */
                if ("".equals(newValue.toString().trim())
                        || ".".equals(newValue.toString())) {
                    String msg = getString(R.string.message_blank_field);
                    Toast.makeText(SettingsActivity.this, msg, Toast.LENGTH_LONG).show();
                    return false;
                }
                /* Ensures that the increment is valid. */
                if ("increment".equals(preference.getKey())
                        && Double.parseDouble(newValue.toString()) == 0) {
                    String msg = getString(R.string.message_invalid_increment);
                    Toast.makeText(SettingsActivity.this, msg, Toast.LENGTH_LONG).show();
                    return false;
                }
                return true;
            }
        };

        /* Synchronizes fields with their summaries. */
        sharedPrefListener = new OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                Preference pref = findPreference(key);
                if (pref instanceof EditTextPreference) {
                    syncSummary((EditTextPreference)pref);
                }
            }
        };

        String action = getIntent().getAction();
        if (action != null && action.equals(ACTION_FARE_VALUES)) {
            loadFareValuesPreferences();
        } else if (action != null && action.equals(ACTION_OTHER_VALUES)) {
            loadOtherValuesPreferences();
        } else {
            /* Main settings screen. */
            addPreferencesFromResource(R.xml.preferences);
            /* Sets up restore option. */
            Preference restore = findPreference("restoreSettings");
            restore.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    confirmRestoreDialog();
                    return true;
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        defaultPrefs.registerOnSharedPreferenceChangeListener(sharedPrefListener);
    }

    @Override
    protected void onPause() {
        defaultPrefs.unregisterOnSharedPreferenceChangeListener(sharedPrefListener);
        super.onPause();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            finish();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    private void loadFareValuesPreferences() {
        addPreferencesFromResource(R.xml.preferences_fare_values);
        int prefCount = getPreferenceScreen().getPreferenceCount();
        /* Sets up the input filters for the fare fields. */
        for (int i = 0; i < prefCount; ++i) {
            EditTextPreference etp = (EditTextPreference) getPreferenceScreen().getPreference(i);
            etp.setOnPreferenceChangeListener(prefListener);
            etp.getEditText().setFilters(new InputFilter[] { new DecimalInputFilter(2) });
            syncSummary(etp);
        }
    }

    private void loadOtherValuesPreferences() {
        addPreferencesFromResource(R.xml.preferences_other_values);
        int prefCount = getPreferenceScreen().getPreferenceCount();
        /* Sets up the input filters. */
        for (int i = 0; i < prefCount; ++i) {
            EditTextPreference etp = (EditTextPreference) getPreferenceScreen().getPreference(i);
            etp.setOnPreferenceChangeListener(prefListener);
            etp.getEditText().setFilters(new InputFilter[] { new DecimalInputFilter(2) });
            syncSummary(etp);
        }
    }

    /** Sets the summary of a preference to its formatted value. */
    private void syncSummary(EditTextPreference etp) {
        String text = etp.getText();
        if ("bonusPercentage".equals(etp.getKey())) {
            text = formatPercent.format(new BigDecimal((text)));
            etp.setSummary(text + "%");
        } else {
            text = formatUSD.format(new BigDecimal((text)));
            etp.setSummary("$" + text);
            text = text.replaceAll(",", "");
        }
        etp.setText(text);
    }

    private void confirmRestoreDialog() {
        new AlertDialog.Builder(this)
        .setTitle(R.string.pref_restore_settings)
        .setMessage(R.string.confirm_restore)
        .setPositiveButton(R.string.okay, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                restoreDefaultSettings();
                String msg = getString(R.string.restored);
                Toast.makeText(SettingsActivity.this, msg, Toast.LENGTH_LONG).show();
            }
        })
        .setNegativeButton(R.string.cancel, null)
        .setIcon(android.R.drawable.ic_dialog_alert)
        .show();
    }

    private void restoreDefaultSettings() {
        defaultEditor.clear();
        defaultEditor.commit();
        PreferenceManager.setDefaultValues(this, R.xml.preferences_fare_values, true);
        PreferenceManager.setDefaultValues(this, R.xml.preferences_other_values, true);
    }
}
