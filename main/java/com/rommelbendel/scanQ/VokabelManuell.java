package com.rommelbendel.scanQ;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;

import com.rommelbendel.scanQ.additional.TinyDB;

import java.util.ArrayList;
import java.util.List;

public class VokabelManuell extends AppCompatActivity {

    private VokabelViewModel vokabelViewModel;
    private KategorienViewModel kategorienViewModel;

    private LiveData<List<Kategorie>> alleKategorien;
    private List<Kategorie> kategorienAktuell;

    private EditText inputEnglisch;
    private EditText inputDeutsch;
    private Spinner inputKategorie;
    private RadioButton radioButtonMarkierung;
    private ImageButton addCategorieButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vokabel_manuell);

        vokabelViewModel = new ViewModelProvider(this).get(VokabelViewModel.class);
        kategorienViewModel = new ViewModelProvider(this).get(KategorienViewModel.class);

        inputEnglisch = findViewById(R.id.inputEnglisch);
        inputDeutsch = findViewById(R.id.inputDeutsch);
        inputKategorie = findViewById(R.id.categorySelection);
        radioButtonMarkierung = findViewById(R.id.radioButton_markierung);
        addCategorieButton = findViewById(R.id.addCategoryButton);
        Button buttonOk = findViewById(R.id.button_ok);
        Button buttonAbbrechen = findViewById(R.id.button_abbrechen);

        alleKategorien = kategorienViewModel.getAlleKategorien();
        alleKategorien.observe(this, categories -> {
            if (categories != null) {
                if (categories.size() > 0) {
                    List<String> categoryNames = new ArrayList<>();
                    for (Kategorie category: categories) {
                        categoryNames.add(category.getName());
                    }
                    ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(VokabelManuell.this,
                            android.R.layout.simple_spinner_item, categoryNames);
                    categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    inputKategorie.setAdapter(categoryAdapter);
                    kategorienAktuell = categories;
                    restartIfUpdated();
                } else {
                    inputKategorie.setVisibility(View.INVISIBLE);
                    Intent createCategoryIntent = new Intent(VokabelManuell.this, CategoryManager.class);
                    createCategoryIntent.putExtra("CategoryManagerHeading", "neue Kategorie erstellen");
                    startActivity(createCategoryIntent);
                    finish();
                }
            }
        });

        buttonOk.setOnClickListener(v -> {
            final String englisch = inputEnglisch.getText().toString().trim();
            final String deutsch = inputDeutsch.getText().toString().trim();
            final String kategorie = inputKategorie.getSelectedItem().toString();
            final boolean markiert = radioButtonMarkierung.isChecked();

            TinyDB tb = new TinyDB(getApplicationContext());
            if (tb.getBoolean("lowercase"))
                englisch.toLowerCase();

            if (!englisch.isEmpty()) {
                if (!deutsch.isEmpty()){
                    if (!kategorie.isEmpty()) {
                        Vokabel neueVokabel = new Vokabel(englisch, deutsch, kategorie);
                        if (markiert) {
                            neueVokabel.setMarkiert(true);
                        }
                        vokabelViewModel.insertVokabel(neueVokabel);

                        inputKategorie.setBackgroundColor(Color.WHITE);
                        inputEnglisch.setText("");
                        inputEnglisch.setBackgroundColor(Color.WHITE);
                        inputDeutsch.setText("");
                        inputDeutsch.setBackgroundColor(Color.WHITE);
                        radioButtonMarkierung.setChecked(false);

                        Toast toast = Toast.makeText(VokabelManuell.this, "Vokabel wurde gespeichert", Toast.LENGTH_SHORT);
                        toast.show();

                    } else {
                        inputKategorie.setBackgroundColor(Color.RED);
                    }
                } else {
                    inputDeutsch.setBackgroundColor(Color.RED);
                }
            } else {
                inputEnglisch.setBackgroundColor(Color.RED);
            }
        });

        buttonAbbrechen.setOnClickListener(v -> back());

        addCategorieButton.setOnClickListener(v -> {
            Intent createCategoryIntent = new Intent(VokabelManuell.this, CategoryManager.class);
            createCategoryIntent.putExtra("Mode", CategoryManager.NEW_CATEGORY);
            VokabelManuell.this.startActivity(createCategoryIntent);
        });
    }

    private void restartIfUpdated() {
        alleKategorien.removeObservers(VokabelManuell.this);
        alleKategorien.observe(VokabelManuell.this, categories -> {
            if (categories != null) {
                if (categories != kategorienAktuell) {
                    VokabelManuell.this.recreate();
                }
            }
        });
    }

    private void back() {
        Intent intentBack = new Intent(VokabelManuell.this, AlleVokabelnAnzeigen.class);
        VokabelManuell.this.startActivity(intentBack);
        finish();
    }
}
