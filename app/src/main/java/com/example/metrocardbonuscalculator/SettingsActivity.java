package com.example.metrocardbonuscalculator;

import android.app.AlertDialog;
import android.content.Context;
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
import android.view.MenuItem;
import android.widget.Toast;

import java.math.BigDecimal;
import java.text.DecimalFormat;

public class SettingsActivity extends AppCompatPreferenceActivity {
    public static final String ACTION_FARE_VALUES = "com.example.metrocardbonuscalculator.ACTION_FARE_VALUES";
    public static final String ACTION_OTHER_VALUES = "com.example.metrocardbonuscalculator.ACTION_OTHER_VALUES";

    public static final String PREF_KEY_RESTORE_SETTINGS = "restoreSettings";

    private SharedPreferences prefs;
    private OnSharedPreferenceChangeListener prefListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        prefListener = new OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                Preference pref = findPreference(key);
                if (pref instanceof EditTextPreference) {
                    syncSummary((EditTextPreference) pref);
                }
            }
        };

        String action = getIntent().getAction();
        boolean main = false;

        if (ACTION_FARE_VALUES.equals(action)) {
            addPreferencesFromResource(R.xml.preferences_fare_values);
        } else if (ACTION_OTHER_VALUES.equals(action)) {
            addPreferencesFromResource(R.xml.preferences_other_values);
        } else { // Main settings
            addPreferencesFromResource(R.xml.preferences);
            Preference restore = findPreference(PREF_KEY_RESTORE_SETTINGS);
            restore.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    new AlertDialog.Builder(SettingsActivity.this)
                            .setTitle(R.string.pref_restore_settings)
                            .setMessage(R.string.confirm_restore)
                            .setPositiveButton(R.string.okay, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    restoreDefaultSettings(SettingsActivity.this);
                                    String msg = getString(R.string.restored);
                                    Toast.makeText(SettingsActivity.this, msg, Toast.LENGTH_LONG).show();
                                }
                            })
                            .setNegativeButton(R.string.cancel, null)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();
                    return true;
                }
            });
            main = true;
        }

        if (!main) {
            int prefCount = getPreferenceScreen().getPreferenceCount();
            for (int i = 0; i < prefCount; ++i) {
                EditTextPreference etp = (EditTextPreference) getPreferenceScreen().getPreference(i);
                etp.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
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
                });
                etp.getEditText().setFilters(new InputFilter[]{new DecimalInputFilter(2)});
                syncSummary(etp);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        prefs.registerOnSharedPreferenceChangeListener(prefListener);
    }

    @Override
    protected void onPause() {
        prefs.unregisterOnSharedPreferenceChangeListener(prefListener);
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

    private void syncSummary(EditTextPreference etp) {
        if (MainActivity.PREF_KEY_BONUS_PCT.equals(etp.getKey())) {
            DecimalFormat df = new DecimalFormat("#.##");
            String s = df.format(new BigDecimal(etp.getText()));
            etp.setSummary(s + "%");
            etp.setText(s);
        } else {
            DecimalFormat df = new DecimalFormat("#,##0.00");
            String s = df.format(new BigDecimal(etp.getText()));
            etp.setSummary("$" + s);
            s = s.replaceAll(",", "");
            etp.setText(s);
        }
    }

    public static void restoreDefaultSettings(Context c) {
        Editor editor = PreferenceManager.getDefaultSharedPreferences(c).edit();
        editor.clear();
        editor.commit();
        PreferenceManager.setDefaultValues(c, R.xml.preferences_fare_values, true);
        PreferenceManager.setDefaultValues(c, R.xml.preferences_other_values, true);
    }
}