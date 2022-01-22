package com.rommelbendel.scanQ;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.transition.Fade;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;
import com.nightonke.boommenu.BoomButtons.HamButton;
import com.nightonke.boommenu.BoomMenuButton;
import com.nightonke.boommenu.OnBoomListenerAdapter;
import com.yuvraj.livesmashbar.enums.GravityView;
import com.yuvraj.livesmashbar.view.LiveSmashBar;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;

public class AlleVokabelnAnzeigen extends AppCompatActivity {

    private VokabelViewModel vokabelViewModel;
    private LiveData<List<Vokabel>> vocabsLiveData;

    private View shadow;
    private TextView header;
    private ViewSwitcher toolbar;
    private ImageButton saveButton;
    private ImageButton cancelButton;
    private RecyclerView vocabTable;
    private RecyclerView vocabTableEdit;
    private ViewSwitcher tableSwitcher;
    private BoomMenuButton bmb;
    private CardView vocabTableCV;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.datenausgabe);

        final LottieAnimationView addButton = findViewById(R.id.addButton);
        final LottieAnimationView editButton = findViewById(R.id.editButton);

        shadow =findViewById(R.id.shadow_datenausgabe);
        header = findViewById(R.id.header);
        vocabTable = findViewById(R.id.tabelleVocView);
        vocabTableEdit = findViewById(R.id.tabelleVoc);
        toolbar = findViewById(R.id.toolbar);
        saveButton = findViewById(R.id.saveButton);
        cancelButton = findViewById(R.id.cancelButton);
        tableSwitcher = findViewById(R.id.tableSwitcher);
        bmb = findViewById(R.id.bmb_voc_tabelle);
        vocabTableCV = findViewById(R.id.tableCardView);

        int[] icons = {R.drawable.ic_scan_add_vocs, R.drawable.ic_feather_write_add};
        String[] titels = {"Einscannen", "Manuell"};
        String[] descriptions = {"Vokabeln automatisch eiscannen & spiechern", "Vokabeln einzeln eingeben & abspeichern"};

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
                                categoryIntent = new Intent(AlleVokabelnAnzeigen.this, Einscannen.class);
                                break;

                            case 1:
                                categoryIntent = new Intent(AlleVokabelnAnzeigen.this, VokabelManuell.class);
                                break;

                            default:
                                throw new IllegalStateException("Unexpected value: " + index);
                        }
                        new Handler().postDelayed(() -> AlleVokabelnAnzeigen.this.startActivity(categoryIntent), 500);


                    });
            bmb.addBuilder(builder);
        }

        bmb.setOnBoomListener(new OnBoomListenerAdapter() {
            @Override
            public void onBoomWillShow() {
                super.onBoomWillShow();
                addButton.playAnimation();
                addButton.setSpeed(1.2f);

                Transition transition = new Fade();
                transition.setDuration(600);
                transition.addTarget(shadow);

                TransitionManager.beginDelayedTransition((ViewGroup) shadow.getRootView(),transition);
                shadow.setVisibility(View.VISIBLE);
            }

            @Override
            public void onBoomWillHide() {
                super.onBoomWillHide();

                Transition transition = new Fade();
                transition.setDuration(600);
                transition.addTarget(shadow);

                TransitionManager.beginDelayedTransition((ViewGroup) shadow.getRootView(),transition);
                shadow.setVisibility(View.GONE);
            }
        });

        vokabelViewModel = new ViewModelProvider(this).get(VokabelViewModel.class);

        Bundle extras = getIntent().getExtras();

        if (extras != null) {
            String category = extras.getString("Category");
            int statsDetails = extras.getInt("statsDetails");

            if(statsDetails == 0) {
                if (!category.isEmpty())
                    loadCategory(category);
                else
                    loadAll();
            } else {
                loadStatsDetails(statsDetails, category);
            }
        } else {
            loadAll();
        }

        NestedScrollView scrollView = findViewById(R.id.ScrollView);
        scrollView. setOnScrollChangeListener((View.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            if (oldScrollY > scrollY) {
                bmb.setVisibility(View.VISIBLE);
                addButton.setVisibility(View.VISIBLE);
                editButton.setVisibility(View.VISIBLE);
                toolbar.setVisibility(View.VISIBLE);
            }
            else {
                bmb.setVisibility(View.INVISIBLE);
                addButton.setVisibility(View.INVISIBLE);
                editButton.setVisibility(View.INVISIBLE);
                toolbar.setVisibility(View.INVISIBLE);
            }
        });

        /*addButton.setOnClickListener(v -> {
            addButton.playAnimation();

            final AlertDialog.Builder neueVokabelDialogBuilder = new AlertDialog.Builder(
                    AlleVokabelnAnzeigen.this);
            neueVokabelDialogBuilder.setTitle("Vokabel hinzufügen");
            neueVokabelDialogBuilder.setMessage("Wie möchtest du fortfahren?");

            neueVokabelDialogBuilder.setPositiveButton("manuell", (dialog, which) -> {
                dialog.cancel();

                Intent intentManuell = new Intent(AlleVokabelnAnzeigen.this,
                        VokabelManuell.class);
                AlleVokabelnAnzeigen.this.startActivity(intentManuell);
                finish();
            });

            neueVokabelDialogBuilder.setNeutralButton("scannen", (dialog, which) -> {
                Toast toast = Toast.makeText(AlleVokabelnAnzeigen.this, "scannen",
                        Toast.LENGTH_SHORT);
                toast.show();
                dialog.cancel();
            });

            neueVokabelDialogBuilder.setNegativeButton("abbrechen",
                    (dialog, which) -> dialog.cancel());

            neueVokabelDialogBuilder.show();
        });*/

        editButton.setOnClickListener(v -> {
            //editButton.playAnimation();
            toolbar.setDisplayedChild(1);
            tableSwitcher.setDisplayedChild(1);

            saveButton.setOnClickListener(v1 -> {
                int rowNum = vocabTable.getChildCount();
                for (int i = 0; i < rowNum; i++){
                    TableLayout tableLayoutOriginal = (TableLayout) vocabTable.getChildAt(i);
                    TableRow rowOriginal = (TableRow) tableLayoutOriginal.getChildAt(0);

                    CardView cardViewOrigENG = (CardView) rowOriginal.getChildAt(0);
                    TextView textViewOrigENG = (TextView) cardViewOrigENG.getChildAt(0);
                    String engoriginal = (String) textViewOrigENG.getText();

                    CardView cardViewOrigDE = (CardView) rowOriginal.getChildAt(1);
                    TextView textViewOrigDE = (TextView) cardViewOrigDE.getChildAt(0);
                    String deoriginal = (String) textViewOrigDE.getText();


                    TableLayout tableLayoutUpdated = (TableLayout) vocabTableEdit.getChildAt(i);
                    TableRow rowUpdated = (TableRow) tableLayoutUpdated.getChildAt(0);

                    CardView cardViewUpdatedENG = (CardView) rowUpdated.getChildAt(0);
                    TextView textViewUpdatedENG = (TextView) cardViewUpdatedENG.getChildAt(0);
                    String engupdated = textViewUpdatedENG.getText().toString().trim();

                    CardView cardViewUpdatedDE = (CardView) rowUpdated.getChildAt(1);
                    TextView textViewUpdatedDE = (TextView) cardViewUpdatedDE.getChildAt(0);
                    String deupdated = textViewUpdatedDE.getText().toString().trim();

                    if (!engupdated.equals(engoriginal) && !engupdated.isEmpty()) {
                        vokabelViewModel.updateVokabelENG(engoriginal, engupdated);
                    }

                    if (!deupdated.equals(deoriginal) && !deupdated.isEmpty()) {
                        vokabelViewModel.updateVokabelDE(deoriginal, deupdated);
                    }
                    toolbar.setDisplayedChild(0);
                    tableSwitcher.setDisplayedChild(0);
                }

                AlleVokabelnAnzeigen.this.recreate();
            });

            cancelButton.setOnClickListener(v12 -> {
                toolbar.setDisplayedChild(0);
                tableSwitcher.setDisplayedChild(0);
            });
        });

    }

    private void loadCategory(String categoryName) {
        header.setText(categoryName);
        vocabsLiveData = vokabelViewModel.getCategoryVocabs(categoryName);
        vocabsLiveData.observe(this, vocabs -> {
            if (vocabs != null) {
                if (!vocabs.isEmpty()) {
                    insertInTable(vocabs);
                } else {
                    vocabTableCV.setVisibility(View.GONE);
                    new LiveSmashBar.Builder(AlleVokabelnAnzeigen.this)
                            .title("Keine Vokabeln vorhanden")
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
                }
            }
        });
    }

    private void loadAll() {
        header.setText(R.string.alle_vokabeln);
        vocabsLiveData = vokabelViewModel.getAlleVokabeln();
        vocabsLiveData.observe(this, vocabs -> {
            if (vocabs != null) {
                if (!vocabs.isEmpty()) {
                    insertInTable(vocabs);
                } else {
                    vocabTableCV.setVisibility(View.GONE);
                    new LiveSmashBar.Builder(AlleVokabelnAnzeigen.this)
                            .title("Keine Vokabeln vorhanden")
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
                }
            }
        });
    }

    private void loadStatsDetails(int i, String category) {
        if(category.equals("alle")) {
            switch (i) {
                case 1:
                    header.setText(R.string.good);
                    vocabsLiveData = vokabelViewModel.getTrainedVocabs();
                    break;
                case 2:
                    header.setText(R.string.bad);
                    vocabsLiveData = vokabelViewModel.getBadVocabs();
                    break;
                case 3:
                    header.setText(R.string.unlearned);
                    vocabsLiveData = vokabelViewModel.getUntrainedVocabs();
                    break;
                default:
                    finish();
            }
        } else {
            switch (i) {
                case 1:
                    header.setText(MessageFormat.format("{0}\naus \"{1}\"", getString(R.string.good), category));
                    vocabsLiveData = vokabelViewModel.getTrainedCategoryVocabs(category);
                    break;
                case 2:header.setText(MessageFormat.format("{0}\naus \"{1}\"", getString(R.string.bad), category));
                    vocabsLiveData = vokabelViewModel.getBadCategoryVocabs(category);
                    break;
                case 3:
                    header.setText(MessageFormat.format("{0}\naus \"{1}\"", getString(R.string.unlearned), category));
                    vocabsLiveData = vokabelViewModel.getUntrainedCategoryVocabs(category);
                    break;
                default:
                    finish();
            }
        }

        vocabsLiveData.observe(this, vocabs -> {
            if (vocabs != null) {
                if (!vocabs.isEmpty()) {
                    insertInTable(vocabs);
                } else {
                    vocabTableCV.setVisibility(View.GONE);
                    new LiveSmashBar.Builder(AlleVokabelnAnzeigen.this)
                            .title("Keine Vokabeln vorhanden")
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
                }
            }
        });
    }

    private void insertInTable(List<Vokabel> vocabs) {
        Collections.sort(vocabs, (voc1, voc2) -> voc1.getVokabelENG()
                .compareToIgnoreCase(voc2.getVokabelENG()));

        final TabelleVokabelAdapterAnzeigen stringVokabelAdapter = new TabelleVokabelAdapterAnzeigen(this,
                TabelleVokabelAdapterAnzeigen.OUTPUT_MODE_EDITABLE, vocabs, vokabelViewModel);
        vocabTableEdit.setAdapter(stringVokabelAdapter);
        vocabTableEdit.setLayoutManager(new LinearLayoutManager(this));
        stringVokabelAdapter.setVokabelCache(vocabs);

        final TabelleVokabelAdapterAnzeigen stringVokabelAdapterView = new TabelleVokabelAdapterAnzeigen(this,
                TabelleVokabelAdapterAnzeigen.OUTPUT_MODE_VIEWABLE, vocabs, vokabelViewModel);
        vocabTable.setAdapter(stringVokabelAdapterView);
        vocabTable.setLayoutManager(new LinearLayoutManager(this));
        stringVokabelAdapterView.setVokabelCache(vocabs);

    }

}