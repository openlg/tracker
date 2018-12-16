package com.qz.tracker.activity;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.qz.tracker.R;
import com.qz.tracker.ui.main.MainFragment;

/**
 * @author lg
 */
public class MainActivity extends AppCompatActivity {

    private final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, MainFragment.newInstance())
                    .commitNow();
        }
    }

}
