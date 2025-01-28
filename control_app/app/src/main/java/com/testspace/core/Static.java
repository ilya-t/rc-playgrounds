package com.testspace.core;

import android.content.Context;
import android.content.res.Resources;
import android.widget.TextView;

/**
 * Here you may access app's basic components despite leaks and code smells.
 * At experiments anything is permitted!
 *
 * Note that those fields injected only by {@link ExperimentActivity}
 */
@SuppressWarnings("ALL")
public class Static {
    public static ExperimentActivity a;
    /**
     * Application context.
     */
    public static Context c;
    public static Resources r;
    static TextView tvOutput;
    static Experiment experiment;

    public static <T extends Experiment> T experiment() {
        return (T) experiment;
    };

    public static void output(String text) {
        tvOutput.setText(text);
    }
}
