package com.rommelbendel.scanQ.onlineQuiz;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.rommelbendel.scanQ.Einscannen;
import com.rommelbendel.scanQ.Kategorie;
import com.rommelbendel.scanQ.KategorienViewModel;
import com.rommelbendel.scanQ.R;
import com.rommelbendel.scanQ.Vokabel;
import com.rommelbendel.scanQ.VokabelViewModel;
import com.rommelbendel.scanQ.additional.TinyDB;
import com.yuvraj.livesmashbar.enums.GravityView;
import com.yuvraj.livesmashbar.view.LiveSmashBar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OnlineCreateGame extends AppCompatActivity {
    public static final int DE_TO_ENG = 1;
    public static final int ENG_TO_DE = 2;

    private VokabelViewModel vokabelViewModel;
    private LiveData<List<Vokabel>> quizVocabsLiveData;
    private ArrayList<Vokabel> quizVocabs = new ArrayList<>();

    private CardView content;
    private ConstraintLayout waiting;
    private TextInputEditText code;
    private Spinner category, quizType;
    private SeekBar amountSlider;
    private TextView amount, codeText;
    private TableLayout settingsLayout;
    private MaterialButton go;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_online_create);

        TinyDB tb = new TinyDB(getApplicationContext());
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        KategorienViewModel kategorienViewModel = new ViewModelProvider(this).get(KategorienViewModel.class);

        waiting = findViewById(R.id.waiting_create);
        content = findViewById(R.id.content_create);
        code = findViewById(R.id.input_code_create);
        code.setTransformationMethod(null);
        codeText = findViewById(R.id.text_select_code);
        category = findViewById(R.id.spinner_online_category);
        quizType = findViewById(R.id.spinner_online_quiz);
        amountSlider = findViewById(R.id.slider_online_voc_amount);
        amount = findViewById(R.id.online_voc_amount);
        settingsLayout = findViewById(R.id.table_settings_online_create);
        go = findViewById(R.id.go);

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        float height= metrics.heightPixels/metrics.ydpi;
        float width= metrics.widthPixels/metrics.xdpi;
        double diagonalInches = Math.sqrt(width*width + height*height);

        if (diagonalInches >= 6.5) {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
            params.gravity = Gravity.CENTER_HORIZONTAL;
            params.setMargins(dp(30), dp(50), dp(10), 0);

            TableRow.LayoutParams paramsCatQuiz = new TableRow.LayoutParams(dp(150), TableRow.LayoutParams.MATCH_PARENT);
            paramsCatQuiz.setMargins(dp(160), 0, dp(10), 0);

            TableRow.LayoutParams paramsSlide = new TableRow.LayoutParams(dp(130), TableRow.LayoutParams.MATCH_PARENT);
            paramsSlide.setMargins(dp(160), 0, dp(00), 0);

            codeText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            settingsLayout.setLayoutParams(params);
            category.setLayoutParams(paramsCatQuiz);
            quizType.setLayoutParams(paramsCatQuiz);
            amountSlider.setLayoutParams(paramsSlide);
        }

        ArrayAdapter<CharSequence> quizTypeAdapter = ArrayAdapter.createFromResource(this,
                R.array.QuizModeDropDown_Array, android.R.layout.simple_spinner_item);
        quizTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        quizType.setAdapter(quizTypeAdapter);

        amountSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                amount.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        amountSlider.setProgress(12);

        LiveData<List<Kategorie>> alleKategorien = kategorienViewModel.getAlleKategorien();
        alleKategorien.observe(this, categories -> {
            if (categories != null) {
                if (!categories.isEmpty()) {
                    List<String> categoryNames = new ArrayList<>();
                    for (Kategorie category: categories) {
                        categoryNames.add(category.getName());
                    }
                    ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                            OnlineCreateGame.this,
                            android.R.layout.simple_spinner_item, categoryNames);
                    categoryAdapter.setDropDownViewResource(
                            android.R.layout.simple_spinner_dropdown_item);
                    category.setAdapter(categoryAdapter);
                } else {
                    AlertDialog.Builder warning = new AlertDialog.Builder(OnlineCreateGame.this);
                    warning.setTitle("Keine Vokabeln vorhanden");
                    warning.setMessage("M??chtest du Vokabeln hinzuf??gen?");
                    warning.setPositiveButton("einscannen", (dialog, which) -> {
                        dialog.dismiss();
                        Intent scannIntent = new Intent(OnlineCreateGame.this,
                                Einscannen.class);
                        OnlineCreateGame.this.startActivity(scannIntent);
                        finish();
                    });
                    warning.setNegativeButton("zur??ck", (dialog, which) -> {
                        dialog.dismiss();
                        finish();
                    });
                    warning.show();
                }
            }
        });

        go.setOnClickListener(view -> {
            if(isNetworkAvailable()) {
                if (!code.getText().toString().isEmpty()) {
                    waiting.setVisibility(View.VISIBLE);
                    content.setVisibility(View.GONE);

                    final String cat = category.getSelectedItem().toString();
                    final String type = quizType.getSelectedItem().toString();
                    final int vocabNum = amountSlider.getProgress();
                    final String enterCode = code.getText().toString();

                    DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
                    rootRef.get().addOnCompleteListener(task -> {
                        if (task.getResult().hasChild(enterCode)) {
                            new LiveSmashBar.Builder(OnlineCreateGame.this)
                                .title("Code bereits vergeben")
                                .titleColor(Color.WHITE)
                                .backgroundColor(Color.parseColor("#541111"))
                                .gravity(GravityView.TOP)
                                .duration(5000)
                                .dismissOnTapOutside()
                                .overlayBlockable()
                                .showOverlay()
                                .show();
                        } else {
                            vokabelViewModel = new ViewModelProvider(OnlineCreateGame.this).get(VokabelViewModel.class);
                            quizVocabsLiveData = vokabelViewModel.getCategoryVocabs(cat);
                            quizVocabsLiveData.observe(OnlineCreateGame.this, vokabeln -> {
                                getVocs(vokabeln, vocabNum);

                                if (quizVocabs.size() < 4) {
                                    new LiveSmashBar.Builder(OnlineCreateGame.this)
                                            .title("Du hast zu wenig Vokabeln ausgew??hlt")
                                            .titleColor(Color.WHITE)
                                            .backgroundColor(Color.parseColor("#541111"))
                                            .gravity(GravityView.TOP)
                                            .duration(5000)
                                            .dismissOnTapOutside()
                                            .overlayBlockable()
                                            .showOverlay()
                                            .show();
                                } else {

                                    ArrayList<Integer> points = new ArrayList<>(Collections.nCopies(10, 0));
                                    ArrayList<Boolean> finished = new ArrayList<>(Collections.nCopies(10, false));
                                    ArrayList<String> players = new ArrayList<String>();
                                    players.add(tb.getString("username").trim());
                                    players.addAll(Collections.nCopies(9, "0"));

                                    DatabaseReference online_quiz_ref = database.getReference(enterCode);

                                    online_quiz_ref.child("vocs").setValue(quizVocabs);
                                    online_quiz_ref.child("timestamp").setValue(System.currentTimeMillis());
                                    online_quiz_ref.child("isStarted").setValue(false);
                                    online_quiz_ref.child("empty").setValue(false);

                                    online_quiz_ref.child("player").setValue(players);
                                    online_quiz_ref.child("active").child("0").setValue(System.currentTimeMillis());
                                    online_quiz_ref.child("points").child("0").setValue(0);
                                    online_quiz_ref.child("done").child("0").setValue(false);


                                    Bundle bundle = new Bundle();
                                    bundle.putString("enter_code", enterCode);
                                    bundle.putString("id", "0");
                                    bundle.putString("playerName", tb.getString("username"));

                                switch (type) {
                                    case "Eingabe DE":
                                        online_quiz_ref.child("Quiz Type").setValue("Eingabe Deu");
                                        online_quiz_ref.child("Language").setValue(ENG_TO_DE);
                                        break;
                                    case "Eingabe ENG":
                                        online_quiz_ref.child("Quiz Type").setValue("Eingabe Eng");
                                        online_quiz_ref.child("Language").setValue(DE_TO_ENG);
                                        break;
                                    case "Multiple-Choice DE":
                                        online_quiz_ref.child("Quiz Type").setValue("Multiple Choice Deu");
                                        online_quiz_ref.child("Language").setValue(ENG_TO_DE);
                                        break;
                                    case "Multiple-Choice ENG":
                                        online_quiz_ref.child("Quiz Type").setValue("Multiple Choice Eng");
                                        online_quiz_ref.child("Language").setValue(DE_TO_ENG);
                                        break;
                                    case "Spracheingabe":
                                        online_quiz_ref.child("Quiz Type").setValue("Sprachquiz");
                                        break;
                                    default:
                                        throw new IllegalStateException("Unexpected value: " + quizType);
                                }
                                    Intent startQuizIntent = new Intent(OnlineCreateGame.this, OnlineLobbyWaiting.class);
                                    startQuizIntent.putExtras(bundle);
                                    startActivity(startQuizIntent);
                                    finish();

                                    waiting.setVisibility(View.GONE);
                                    content.setVisibility(View.VISIBLE);
                                }
                            });
                        }
                    });
                } else {
                    new LiveSmashBar.Builder(this)
                            .title("Bitte gib einen Code ein")
                            .titleColor(Color.WHITE)
                            .backgroundColor(Color.parseColor("#541111"))
                            .gravity(GravityView.TOP)
                            .duration(5000)
                            .dismissOnTapOutside()
                            .overlayBlockable()
                            .showOverlay()
                            .show();
                }
            } else {
                new LiveSmashBar.Builder(this)
                        .title("Bitte ??berpr??fe deine Internetverbindung")
                        .titleColor(Color.WHITE)
                        .backgroundColor(Color.parseColor("#541111"))
                        .gravity(GravityView.TOP)
                        .duration(5000)
                        .dismissOnTapOutside()
                        .overlayBlockable()
                        .showOverlay()
                        .show();
            }
        });
    }

    private void getVocs(List<Vokabel> vokabeln, int vocabNum) {
        if (vokabeln != null) {
            quizVocabs = (ArrayList<Vokabel>) quizVocabsLiveData.getValue();
            assert quizVocabs != null;
            Collections.shuffle(quizVocabs);
            if (quizVocabs.size() > vocabNum) {
                OnlineCreateGame.this.quizVocabs = new ArrayList<>(quizVocabs.subList(0, vocabNum));
            }
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
    }

    private int dp(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }
}
