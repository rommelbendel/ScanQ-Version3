package com.rommelbendel.scanQ;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;

import com.yuvraj.livesmashbar.enums.GravityView;
import com.yuvraj.livesmashbar.view.LiveSmashBar;

import java.util.List;

public class CategoryManager extends AppCompatActivity {

    public static final int EDIT_CATEGORY = 1;
    public static final int NEW_CATEGORY = 2;

    private EditText categoryNameInput;
    private Button buttonOK;
    private Button buttonCancel;
    private TextView activityHeading;

    private KategorienViewModel kategorienViewModel;
    private VokabelViewModel vokabelViewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_manager);

        categoryNameInput = findViewById(R.id.inputName);
        buttonOK = findViewById(R.id.button_ok);
        buttonCancel = findViewById(R.id.button_abbrechen);
        activityHeading = findViewById(R.id.category_manager_heading);

        kategorienViewModel = new ViewModelProvider(this).get(KategorienViewModel.class);
        vokabelViewModel = new ViewModelProvider(this).get(VokabelViewModel.class);

        Bundle extras = getIntent().getExtras();

        if (extras != null) {
            int mode = extras.getInt("Mode");
            activityHeading.setText(extras.getString("CategoryManagerHeading"));

            if (mode == EDIT_CATEGORY) {
                String category = extras.getString("Category");
                editCategory(category);
            } else if (mode == NEW_CATEGORY) {
                activityHeading.setText("Kategorie erstellen");
                newCategory();
            } else {
                activityHeading.setText("Kategorie erstellen");
                newCategory();
            }
        } else {
            activityHeading.setText("Kategorie erstellen");
            newCategory();
        }
    }

    private void editCategory(final String categoryNameOld) {
        categoryNameInput.setText(categoryNameOld);

        buttonOK.setOnClickListener(v -> {
            Log.e("click", "buttonOK");
            String categoryNameNew = categoryNameInput.getText().toString().trim();
            if (!categoryNameNew.isEmpty() && !categoryNameNew.equals(categoryNameOld)) {
                LiveData<List<Kategorie>> categoriesWithName = kategorienViewModel
                        .getCategoriesWithName(categoryNameNew);
                categoriesWithName.observe(CategoryManager.this, categories -> {
                    if (categories != null) {
                        if (categories.size() == 0) {
                            kategorienViewModel.updateCategoryName(categoryNameOld, categoryNameNew);
                            vokabelViewModel.updateCategory(categoryNameOld, categoryNameNew);
                            finish();
                        } else {
                            Log.e("Category Name", "exists already");
                            LiveSmashBar.Builder warning = new LiveSmashBar.Builder(this);
                            warning.title("Diese Kategorie existiert bereits.");
                            warning.titleColor(Color.WHITE);
                            warning.backgroundColor(Color.parseColor("#541111"));
                            warning.gravity(GravityView.BOTTOM);
                            warning.dismissOnTapOutside();
                            warning.showOverlay();
                            warning.overlayBlockable();
                            warning.duration(3000);
                            warning.show();
                        }
                        categoriesWithName.removeObservers(CategoryManager.this);
                    }
                });
            } else {
                Log.e("Category Name", "empty");
                LiveSmashBar.Builder warning = new LiveSmashBar.Builder(this);
                warning.title("Bitte gib einen Namen ein");
                warning.titleColor(Color.WHITE);
                warning.backgroundColor(Color.parseColor("#541111"));
                warning.gravity(GravityView.BOTTOM);
                warning.dismissOnTapOutside();
                warning.showOverlay();
                warning.overlayBlockable();
                warning.duration(3000);
                warning.show();
            }
        });

        buttonCancel.setOnClickListener(v -> finish());
    }

    private void newCategory() {

        buttonOK.setOnClickListener(v -> {
            String categoryName = categoryNameInput.getText().toString().trim();
            if (!categoryName.isEmpty()) {
                LiveData<List<Kategorie>> categoriesWithName = kategorienViewModel
                        .getCategoriesWithName(categoryName);
                categoriesWithName.observe(CategoryManager.this, categories -> {
                    if (categories != null) {
                        if (categories.size() == 0) {
                            Kategorie newCategory = new Kategorie(categoryName);
                            kategorienViewModel.insertKategorie(newCategory);
                            finish();
                        } else {
                            Log.e("Category Name", "exists already");
                            LiveSmashBar.Builder warning = new LiveSmashBar.Builder(this);
                            warning.title("Diese Kategorie existiert bereits.");
                            warning.titleColor(Color.WHITE);
                            warning.backgroundColor(Color.parseColor("#541111"));
                            warning.gravity(GravityView.BOTTOM);
                            warning.dismissOnTapOutside();
                            warning.showOverlay();
                            warning.overlayBlockable();
                            warning.duration(3000);
                            warning.show();
                        }
                        categoriesWithName.removeObservers(CategoryManager.this);
                    }
                });
            } else {
                Log.e("Category Name", "empty");
                LiveSmashBar.Builder warning = new LiveSmashBar.Builder(this);
                warning.title("Bitte gib einen Namen ein");
                warning.titleColor(Color.WHITE);
                warning.backgroundColor(Color.parseColor("#541111"));
                warning.gravity(GravityView.BOTTOM);
                warning.dismissOnTapOutside();
                warning.showOverlay();
                warning.overlayBlockable();
                warning.duration(3000);
                warning.show();
            }
        });

        buttonCancel.setOnClickListener(v -> finish());
    }
}
