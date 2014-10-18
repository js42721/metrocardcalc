package com.example.metrocard_bonus_calc;

import android.text.InputFilter;
import android.text.Spanned;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Filters input such that only numbers of a given scale are accepted.
 */
public class DecimalInputFilter implements InputFilter {
    private Pattern pattern;

    /**
     * Constructor.
     * 
     * @param scale the scale of acceptable input numbers
     */
    public DecimalInputFilter(int scale) {
        pattern = Pattern.compile("(0|[1-9]+[0-9]*)?(\\.[0-9]{0," + scale +"})?");
    }

    @Override
    public CharSequence filter(CharSequence source,
                               int start,
                               int end,
                               Spanned dest,
                               int dstart,
                               int dend) {
        
        String result = dest.subSequence(0, dstart) 
                + source.toString()
                + dest.subSequence(dend, dest.length());
        Matcher matcher = pattern.matcher(result);
        if (!matcher.matches()) {
            return dest.subSequence(dstart, dend);
        }
        return null;
    }
}
