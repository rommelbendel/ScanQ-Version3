package com.rommelbendel.scanQ;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;

import com.airbnb.lottie.LottieAnimationView;
import com.nightonke.boommenu.BoomButtons.HamButton;
import com.nightonke.boommenu.BoomMenuButton;
import com.nightonke.boommenu.OnBoomListenerAdapter;
import com.rommelbendel.scanQ.additional.TinyDB;
import com.yuvraj.livesmashbar.enums.GravityView;
import com.yuvraj.livesmashbar.view.LiveSmashBar;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class CategoryOverview extends AppCompatActivity implements AdapterView.OnItemClickListener{

    private ListView listView;
    private ArrayAdapter<String> listViewAdapter;
    ArrayList<String> categoriesList = new ArrayList<>();
    private TextView loadingText;
    private ProgressBar loadingSymbol;

    private VokabelViewModel vokabelViewModel;
    private KategorienViewModel kategorienViewModel;
    private LiveData<List<Kategorie>> alleKategorien;
    private List<Kategorie> kategorienAktuell;
    private static LottieAnimationView lottieAdd;
    private static BoomMenuButton bmb;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_overview);

        TinyDB tb = new TinyDB(getApplication().getApplicationContext());

        loadingText = findViewById(R.id.loadingText);
        loadingSymbol = findViewById(R.id.loadingSymbol);
        lottieAdd = findViewById(R.id.addButton);
        bmb = findViewById(R.id.bmb);

        int[] icons = {R.drawable.ic_category_add, R.drawable.ic_scan_add_vocs, R.drawable.ic_feather_write_add};
        String[] titels = {"Kategorie","Einscannen", "Manuell"};
        String[] descriptions = {"Füge eine neue Kategorie hinzu","Vokabeln automatisch einscannen & speichern", "Vokabeln einzeln eingeben & abspeichern"};

        for (int i = 0; i < bmb.getPiecePlaceEnum().pieceNumber(); i++) {
            HamButton.Builder builder = new HamButton.Builder()
                    .normalImageDrawable(ContextCompat.getDrawable(this, icons[i]))
                    .imagePadding(new Rect(25,25,25,25))
                    .normalText(titels[i])
                    .subNormalText(descriptions[i])
                    .normalTextColor(getResources().getColor(R.color.colorPrimaryDark))
                    .subNormalTextColor(getResources().getColor(R.color.colorPrimaryDark))
                    .highlightedColor(Color.LTGRAY)
                    .normalColor(Color.WHITE)
                    .listener(index -> {
                        Intent categoryIntent;
                        switch (index) {
                            case 0:
                                categoryIntent = new Intent(CategoryOverview.this, CategoryManager.class);
                                categoryIntent.putExtra("Mode", CategoryManager.NEW_CATEGORY);
                                break;

                            case 1:
                                categoryIntent = new Intent(CategoryOverview.this, Einscannen.class);
                                break;

                            case 2:
                                categoryIntent = new Intent(CategoryOverview.this, VokabelManuell.class);
                                break;

                            default:
                                throw new IllegalStateException("Unexpected value: " + index);
                        }
                        new Handler().postDelayed(() -> CategoryOverview.this.startActivity(categoryIntent), 500);


                    });
            bmb.addBuilder(builder);
        }

        bmb.setOnBoomListener(new OnBoomListenerAdapter() {
            @Override
            public void onBoomWillShow() {
                super.onBoomWillShow();
                lottieAdd.playAnimation();
                lottieAdd.setSpeed(1.2f);
            }
        });

        vokabelViewModel = new ViewModelProvider(this).get(VokabelViewModel.class);
        kategorienViewModel = new ViewModelProvider(this).get(KategorienViewModel.class);

        alleKategorien = kategorienViewModel.getAlleKategorien();

        alleKategorien.observe(this, kategories -> {
            if (kategories != null) {
                if (kategories.size() > 0) {
                    kategorienAktuell = kategories;
                    displayCategoryList(kategories);
                } else {
                    new LiveSmashBar.Builder(CategoryOverview.this)
                            .title("Keine Kategorien vorhanden")
                            .titleColor(Color.WHITE)
                            .titleSizeInSp(19)
                            .backgroundColor(Color.parseColor("#2f3030"))
                            .gravity(GravityView.BOTTOM)
                            .primaryActionText("ok")
                            .primaryActionEventListener(LiveSmashBar::dismiss)
                            .showOverlay()
                            .overlayBlockable()
                            .duration(10000)
                            .show();
                    loadingText.setText(R.string.keine_kategorien_vorhanden);
                    loadingSymbol.setVisibility(View.GONE);
                }

                alleKategorien.removeObservers(CategoryOverview.this);
                alleKategorien.observe(CategoryOverview.this, kategories1 -> {
                    if (kategories1 != null) {
                        if (kategories1 != kategorienAktuell && kategories1.size() > 0) {
                            CategoryOverview.this.recreate();
                            tb.putBoolean("CatOverviewRecreate", true);
                        }
                        else if(kategories1 != kategorienAktuell && tb.getBoolean("CatOverviewRecreate")) {
                            tb.putBoolean("CatOverviewRecreate", false);
                            finish();
                        }
                    }
                });
            }
        });
    }

    private void displayCategoryList(@NotNull List<Kategorie> categories) {
        loadingText.setVisibility(View.GONE);
        loadingSymbol.setVisibility(View.GONE);

        listView = findViewById(R.id.listViewCategory);

        for (Kategorie kategorie : categories) {
           categoriesList.add(kategorie.getName());
        }

        this.listViewAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_2,
                android.R.id.text1, categoriesList) {
            @SuppressLint("SetTextI18n")
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text1 = view.findViewById(android.R.id.text1);
                TextView text2 = view.findViewById(android.R.id.text2);

                LiveData<List<Vokabel>> vokabelList = vokabelViewModel.getCategoryVocabs(categories.get(position).getName());
                vokabelList.observe(CategoryOverview.this, vokabels -> {
                    LiveData<List<Vokabel>> unlearnedVocabs = vokabelViewModel.getUntrainedCategoryVocabs(categories.get(position).getName());
                    unlearnedVocabs.observe(CategoryOverview.this, vokabels1 -> {
                        text1.setTextSize(27);
                        text1.setTextColor(ContextCompat.getColor(CategoryOverview.this, R.color.colorPrimary));
                        text1.setPadding(25,0,0,45);
                        text2.setTextSize(15);
                        text2.setPadding(0,0,0,35);
                        text2.setText(vokabels.size() - vokabels1.size() + " von " + vokabels.size() + " Vokabeln gelernt");
                    });
                });
                return view;
            }
        };

        this.listView.setAdapter(this.listViewAdapter);

        this.listView.setOnItemClickListener(this);
        registerForContextMenu(this.listView);
    }

    public void onItemClick(AdapterView<?> l, View v, int position, long id) {
        Intent showVocabsIntent = new Intent(CategoryOverview.this,
                AlleVokabelnAnzeigen.class);
        showVocabsIntent.putExtra("Category", categoriesList.get(position));
        CategoryOverview.this.startActivity(showVocabsIntent);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, view, menuInfo);
        menu.setHeaderTitle("Optionen");

        menu.add(0,13, 2,  "bearbeiten");
        menu.add(0, 14, 4, "löschen");
        menu.add(0, 15, 4, "abbrechen");
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo
                info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
        final String ausgewaehlteKategorie= (String) this.listView.getItemAtPosition(info.position);

        if(item.getItemId() == 14){
            AlertDialog.Builder deletitionAlert = new AlertDialog.Builder(this);
            deletitionAlert.setTitle("Kategorie löschen?");
            deletitionAlert.setMessage("Möchtest du die Kategorie und die dazugehörigen " +
                    "Vokabeln wirklich löschen?");
            deletitionAlert.setPositiveButton("ja", (dialog14, which1) -> {
                dialog14.dismiss();

                kategorienViewModel.deleteWithName(ausgewaehlteKategorie);
                vokabelViewModel.deleteCategory(ausgewaehlteKategorie);
            });
            deletitionAlert.setNegativeButton("nein", (dialog15, which12) -> {
                dialog15.dismiss();
            });
            deletitionAlert.show();
        }
        else if(item.getItemId() == 13) {
            Intent addCategoryIntent = new Intent(CategoryOverview.this,
                    CategoryManager.class);
            addCategoryIntent.putExtra("Mode", CategoryManager.EDIT_CATEGORY);
            addCategoryIntent.putExtra("CategoryManagerHeading", "\"" + ausgewaehlteKategorie + "\" bearbeiten");
            addCategoryIntent.putExtra("Category", ausgewaehlteKategorie);
            CategoryOverview.this.startActivity(addCategoryIntent);
        }
        else if (item.getItemId() == 15) {
            closeContextMenu();
        } else {
            return false;
        }
        return false;
    }
}
