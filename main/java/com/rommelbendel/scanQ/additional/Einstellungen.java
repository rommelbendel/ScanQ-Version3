package com.rommelbendel.scanQ.additional;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.rommelbendel.scanQ.R;

public class Einstellungen extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragment())
                .commit();
    }
}
