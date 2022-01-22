package com.rommelbendel.scanQ;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.transition.Fade;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.view.GestureDetectorCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.button.MaterialButton;
import com.nightonke.boommenu.BoomButtons.TextOutsideCircleButton;
import com.nightonke.boommenu.BoomMenuButton;
import com.nightonke.boommenu.OnBoomListenerAdapter;
import com.rommelbendel.scanQ.additional.Einstellungen;
import com.rommelbendel.scanQ.additional.TinyDB;
import com.rommelbendel.scanQ.appIntro.AppIntro;
import com.rommelbendel.scanQ.impaired.visually.InterruptExecutor;
import com.rommelbendel.scanQ.impaired.visually.OnCommandListener;
import com.rommelbendel.scanQ.impaired.visually.Procedure;
import com.rommelbendel.scanQ.impaired.visually.ProcedureInterrupt;
import com.rommelbendel.scanQ.impaired.visually.VoiceControl;
import com.rommelbendel.scanQ.onlineQuiz.OnlineQuizMenu;
import com.rommelbendel.scanQ.speechrecognition.RecognitionViewModel;
import com.rommelbendel.scanQ.speechrecognition.VoiceInput;
import com.yuvraj.livesmashbar.enums.GravityView;
import com.yuvraj.livesmashbar.view.LiveSmashBar;

import org.jetbrains.annotations.NotNull;
import org.joda.time.LocalDate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class NewHome extends AppCompatActivity {

    private SharedPreferences.OnSharedPreferenceChangeListener change;

    private TextView appname, appnameBottom, trainedYesterday, textYesterday, trainedToday,
            textToday, untrainedVocabs, textUntrained, hello, untrained, messageLess10Vocs;
    private View clickStats;
    private CardView tableCV;
    private BoomMenuButton bmb;
    private LinearLayout ll;
    private HorizontalScrollView hv;
    private TextView tv, noCats;
    private View line;
    private RecyclerView vocabTable;
    private MaterialButton offline, online;
    private PieChart rightWrongPieChart;
    private TableLayout statisticsWrapper;
    //private NestedScrollView contentScrollView;
    private AppBarLayout appBarLayout;

    private VokabelViewModel vokabelViewModel;
    private WeekdayViewModel weekdayViewModel;
    private KategorienViewModel kategorienViewModel;

    private List<Vokabel> vocs;
    private List<Kategorie> allCategories;
    private int tdy;
    private int ystrdy;

    private int untrainedVocs;
    private boolean categoriesExsist = true;

    private VoiceControl voiceControl;
    private RecognitionViewModel recognitionResultVM;

    private TextToSpeech tts;
    private TinyDB tb;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_screen);
        tb = new TinyDB(getApplicationContext());
        SharedPreferences sp = getSharedPreferences("settings",0);

        weekdayViewModel = new ViewModelProvider(this).get(WeekdayViewModel.class);
        LiveData<List<Weekday>> allWeekdays = weekdayViewModel.getAllDays();
        allWeekdays.observe(NewHome.this, weekdays -> {
            if (weekdays != null) {
                Log.e("check", "run");
                //Log.d("Weekdays", weekdays.toString());
                LocalDate today = LocalDate.now();
                Log.d("Date", today.toDate().toString());
                if (weekdays.size() == 0) {
                    Log.e("decision", "if");
                    final String[] weekdayNames = {"Montag", "Dienstag", "Mittwoch", "Donnerstag",
                            "Freitag", "Samstag", "Sonntag"};
                    Bundle days = new Bundle();
                    for (int i = 0; i < 7; i++) {
                        days.putLong(weekdayNames[today.minusDays(i).getDayOfWeek() - 1],
                                today.minusDays(i).toDate().getTime());
                    }

                    int dayOfWeekCounter = 1;
                    for (String name: weekdayNames) {
                        weekdayViewModel.insertWeekday(new Weekday(dayOfWeekCounter,name, new Date(
                                days.getLong(name)).getTime()));
                        dayOfWeekCounter ++;
                    }
                } else {
                    Log.e("decision", "else");
                    if (weekdays.get(0).getDate() != today.toDate().getTime()) {
                        Log.d("decision", "else");
                        final long timeShift = today.toDate().getTime() - weekdays.get(0).getDate();
                        final int dayShift = (int) (timeShift / 86400000); //conversion of milliseconds to days

                        Log.d("dayShift", String.valueOf(dayShift));
                        for (int i = 0; i < dayShift; i++) {
                            final LocalDate dayToInsert = today.minusDays(i);
                            weekdayViewModel.updateDay(dayToInsert.toDate().getTime(),
                                    dayToInsert.getDayOfWeek());
                        }
                    }
                }
                allWeekdays.removeObservers(NewHome.this);
            }
        });

        if (tb.getBoolean("visually_impaired")) {
            recognitionResultVM = new ViewModelProvider(NewHome.this)
                    .get(RecognitionViewModel.class);
            voiceControl = new VoiceControl(this, recognitionResultVM);
            /*
            voiceControl.detectGestures(new GestureDetectorCompat(HomeTest.this,
                    voiceControl));
             */
            kategorienViewModel = new ViewModelProvider(NewHome.this)
                    .get(KategorienViewModel.class);
            kategorienViewModel.getAlleKategorien().observe(NewHome.this, categories -> {
                if (categories != null) {
                    allCategories = categories;
                    kategorienViewModel.getAlleKategorien().removeObservers(NewHome.this);
                }
            });

            vokabelViewModel = new ViewModelProvider(NewHome.this)
                    .get(VokabelViewModel.class);

            GestureDetectorCompat gestureDetectorCompat = new GestureDetectorCompat(
                    this, voiceControl);
            gestureDetectorCompat.setOnDoubleTapListener(voiceControl);
            gestureDetectorCompat.setIsLongpressEnabled(true);

            voiceControl.informVoiceControlActive();

            voiceControl.setOnCommandListener(new OnCommandListener() {
                @Override
                public ArrayList<ArrayList<String>> getAvailableCommands() {
                    ArrayList<ArrayList<String>> availableCommands = new ArrayList<>();

                    ArrayList<String> scanCommand = new ArrayList<>();
                    scanCommand.add("einscannen");
                    scanCommand.add("scannen");
                    scanCommand.add("vokabeln einscannen");
                    availableCommands.add(scanCommand);

                    return availableCommands;
                }

                @Override
                public boolean onCommand(String command) {
                    Log.e("submitted", command);
                    switch (command) {
                        case "beenden":
                        case "aus":
                        case "blindenmodus beenden":
                        case "blinden modus beenden":
                            Log.e("executed", "wird beendet...");
                            return true;
                        case "einscannen":
                            voiceControl.informHelpNeeded();
                            Log.e("executed", "help needed");
                            return true;
                        case "quiz":
                        case "frage mich ab":
                            Log.e("executed", "quiz");
                            Intent intent = new Intent(NewHome.this,
                                    QuizSettings.class);
                            startActivity(intent);
                            return true;
                        case "wie viele vokabeln habe ich":
                            final String text = "Du hast %s Vokabeln insgesamt. " +
                                    "Davon sind %s ungelernt";

                            vokabelViewModel.getAlleVokabeln().observe(NewHome.this,
                                    vocabs -> {
                                        if (vocabs != null) {
                                            final int vocabCount = vocabs.size();
                                            vokabelViewModel.getUntrainedVocabs().observe(
                                                    NewHome.this, vocsUntrained -> {
                                                        if (vocsUntrained != null) {
                                                            String textToSpeak = String.format(
                                                                    text,
                                                                    vocabCount,
                                                                    vocsUntrained.size());
                                                            voiceControl.speak(textToSpeak);
                                                            vokabelViewModel.getUntrainedVocabs()
                                                                    .removeObservers(
                                                                            NewHome.this);
                                                        }
                                                    });
                                            vokabelViewModel.getAlleVokabeln().removeObservers(
                                                    NewHome.this);


                                        }
                                    });
                            return true;
                    }
                    return false;
                }

                @Override
                public void onCommandNotFound(String mostLikelyCommand,
                                              List<String> possibleCommands) {
                    Log.e( "error","command not found");
                    voiceControl.informCommandNotFound();
                }
            });

            /*
            voiceControl.addProcedure(new Procedure() {
                @NotNull
                @Override
                public ArrayList<String> getProcedureNames() {
                    ArrayList<String> procedureNames = new ArrayList<>();
                    procedureNames.add("test");
                    return procedureNames;
                }

                @NotNull
                @Override
                public ArrayList<String> getQuestions() {
                    ArrayList<String> questions = new ArrayList<>();
                    questions.add("Wie ist dein Name?");
                    questions.add("OK, wie alt bist du?");
                    return questions;
                }

                @Override
                public boolean redo() {
                    return false;
                }

                @Override
                public boolean allowTermination() {
                    return true;
                }

                @Override
                public void onAnswer(int index, @NotNull String question, @NotNull String answer) {
                    Log.e("Answer", String.format("%s: %s", question, answer));
                }

                @Override
                public boolean onAnswersAvailable(@NotNull ArrayList<String> answers) {
                    final String output = "Dein Name ist %s und du bist %s Jahre alt.";
                    voiceControl.speak(String.format(output, answers.get(0), answers.get(1)));
                    return true;
                }
            });
             */

            voiceControl.addProcedure(new Procedure() {
                @NotNull
                @Override
                public ArrayList<String> getProcedureNames() {
                    return new ArrayList<>(Collections.singletonList("ein quiz"));
                }

                @NotNull
                @Override
                public ArrayList<String> getQuestions() {
                    return new ArrayList<>(Arrays.asList(
                            "Welche Kategorie möchtest du spielen?",
                            "OK, Mit wie vielen Vokabeln möchtest du spielen?",
                            "Alles klar, Möchtest du nur mit ungelernten Vokabeln spielen?"));
                }

                @Override
                public boolean redo() {
                    return true;
                }

                @Override
                public boolean allowTermination() {
                    return true;
                }

                @Override
                public void onAnswer(int index, @NotNull String question, @NotNull String answer) {

                }

                @Override
                public boolean onAnswersAvailable(@NotNull ArrayList<String> answers) {
                    final String text;
                    if (answers.get(2).equalsIgnoreCase("ja")) text = "Gestartet wird ein " +
                            "Quiz der Kategorie %s mit %s ungelernten Vokabeln";
                    else text = "Gestartet wird ein Quiz der Kategorie %s mit %s Vokabeln";
                    voiceControl.speak(String.format(text, answers.get(0), answers.get(1)));
                    return true;
                }
            });

            voiceControl.addProcedure(new Procedure() {

                @NonNull
                @Override
                public ArrayList<String> getProcedureNames() {
                    return new ArrayList<>(Collections.singletonList("das vokabelbuch"));
                }

                @NonNull
                @Override
                public ArrayList<String> getQuestions() {
                    return new ArrayList<>(Collections.singletonList(
                            "Welche Kategorie soll vorgelesen werden?"));
                }

                @NonNull
                @Override
                public ArrayList<ProcedureInterrupt> getInterrupts() {
                    ArrayList<ProcedureInterrupt> interrupts = new ArrayList<>();
                    interrupts.add(new ProcedureInterrupt() {
                        @NonNull
                        @Override
                        public ArrayList<String> getInterruptNames() {
                            return new ArrayList<>(Collections.singletonList("welche gibt es"));
                        }

                        @Override
                        public void onInterrupt(final int questionContextIndex) {
                            if (questionContextIndex == 0) {
                                if (allCategories.size() > 0) {
                                    StringBuilder readOut = new StringBuilder(
                                            "Die möglichen Kategorien lauten ");
                                    for (Kategorie category : allCategories) {
                                        readOut.append(category.getName()).append(", ");
                                    }
                                    readOut.replace(readOut.length() - 2,
                                            readOut.length(), ".");
                                    Log.e("speaking", readOut.toString());
                                    InterruptExecutor.speak(readOut.toString(),
                                            NewHome.this);
                                } else {
                                    InterruptExecutor.speak("Es sind keine Kategorien " +
                                            "vorhanden. Füge zuerst Vokabeln hinzu um " +
                                            "sie Dir vorlesen zu lassen.", NewHome.this);
                                }
                            }
                        }
                    });

                    return interrupts;
                }

                @Override
                public boolean redo() {
                    return false;
                }

                @Override
                public boolean allowTermination() {
                    return true;
                }

                @Override
                public boolean isAnswerValid(int index, @NonNull String question, @NonNull String answer) {
                    boolean isAnswerValid = false;
                    for (Kategorie category : allCategories) {
                        if (category.getName().equalsIgnoreCase(answer)) {
                            isAnswerValid = true;
                            break;
                        }
                    }
                    Log.e("isAnswerValid", String.valueOf(isAnswerValid));
                    return isAnswerValid;
                }

                @Override
                public void onAnswer(int index, @NonNull String question, @NonNull String answer) {

                }

                @Override
                public boolean onAnswersAvailable(@NonNull ArrayList<String> answers) {
                    String realCategoryName = null;
                    for (Kategorie category : allCategories) {
                        if (category.getName().equalsIgnoreCase(answers.get(0)))
                            realCategoryName = category.getName();
                    }
                    assert realCategoryName != null;
                    vokabelViewModel.getCategoryVocabs(realCategoryName).observe(NewHome.this,
                            vocabs -> {
                                if (vocabs != null) {
                                    Log.e("onAnswersAvailable", "vocabs != null");
                                    if (vocabs.size() >= 1) {
                                        Log.e("out", String.format(
                                                "Die Vokabeln mit Übersetzungen der Kategorie %s lauten: ",
                                                answers.get(0)));

                                        voiceControl.speak(String.format(
                                                "Die Vokabeln mit Übersetzungen der Kategorie %s lauten: ",
                                                answers.get(0)), () -> readVocabs(vocabs));

                                    }
                                    vokabelViewModel.getCategoryVocabs(answers.get(0))
                                            .removeObservers(NewHome.this);
                                }
                            });
                    return true;
                }
            });
            //voiceControl.listen();

        }
        else {

            ll = findViewById(R.id.ll_for_cats);
            tv = findViewById(R.id.tv_for_cats);
            line = findViewById(R.id.line_for_cats);
            hv = findViewById(R.id.hv_for_cats);
            noCats = findViewById(R.id.message_for_cats);
            bmb = findViewById(R.id.bmb_home);
            tableCV = findViewById(R.id.tableCardView);
            messageLess10Vocs = findViewById(R.id.less_10_vocs);
            clickStats = findViewById(R.id.click_statistics);
            trainedYesterday = findViewById(R.id.trainedYesterday);
            textYesterday = findViewById(R.id.textYesterday);
            trainedToday = findViewById(R.id.trainedToday);
            textToday = findViewById(R.id.textToday);
            textUntrained = findViewById(R.id.textUntrainedVocabs);
            untrainedVocabs = findViewById(R.id.numberOfUntrainedVocabs);
            rightWrongPieChart = findViewById(R.id.pieChartRightWrong);
            statisticsWrapper = findViewById(R.id.statisticsWrapper);
            appBarLayout = findViewById(R.id.appBar);

            appname = findViewById(R.id.appname);
            appnameBottom = findViewById(R.id.appname_bottom);
            hello = findViewById(R.id.hello_home);
            untrained = findViewById(R.id.untrained_vocabs);
            vocabTable = findViewById(R.id.tabelleVocView);
            offline = findViewById(R.id.quiz_offline);
            online = findViewById(R.id.quiz_online);

            if (!sp.getBoolean("personal", false))
                hello.setText("Hallo " + sp.getString("username", ""));
            else
                hello.setText("Hallo, viel Spaß");

            if (!sp.getBoolean("cats", false))
                createCategoryBubbles();
            else {
                ll.removeAllViews();
                noCats.setVisibility(View.GONE);
                hv.setVisibility(View.GONE);
                tv.setVisibility(View.GONE);
                line.setVisibility(View.GONE);
            }

            change = (pref, key) -> {
                if (key.equals("personal")) {
                    if (!pref.getBoolean(key, false))
                        hello.setText("Hallo " + sp.getString("username", ""));
                    else
                        hello.setText("Hallo, viel Spaß");
                }

                if (key.equals("username") && !sp.getBoolean("personal", true))
                    hello.setText("Hallo " + pref.getString(key, ""));

                if (key.equals("cats")) {
                    if (!pref.getBoolean("cats", false))
                        createCategoryBubbles();
                    else {
                        ll.removeAllViews();
                        noCats.setVisibility(View.GONE);
                        tv.setVisibility(View.GONE);
                        hv.setVisibility(View.GONE);
                        line.setVisibility(View.GONE);
                    }
                }
            };
            getSharedPreferences("settings", 0).registerOnSharedPreferenceChangeListener(change);

            Typeface face = Typeface.createFromAsset(getAssets(),
                    "scanQ.ttf");
            appnameBottom.setTypeface(face);
            appname.setTypeface(face);

            online.setOnClickListener(v -> {
                Intent intent = new Intent(NewHome.this, OnlineQuizMenu.class);
                startActivity(intent);
            });

            offline.setOnClickListener(v -> {
                Intent intent = new Intent(NewHome.this, QuizSettings.class);
                startActivity(intent);
            });

            // Boom Menu und Intents
            int[] icons = {R.drawable.ic_scan_add_vocs, R.drawable.ic_feather_write_add,
                    R.drawable.ic_vocab_list, R.drawable.ic_quiz_games, R.drawable.settings, R.drawable.tutorial};
            String[] titels = {"Einscannen", "Manuell", "Vokabeln", "Quiz", "Einstellungen", "Tipps"};

            for (int i = 0; i < bmb.getPiecePlaceEnum().pieceNumber(); i++) {
                TextOutsideCircleButton.Builder builder = new TextOutsideCircleButton.Builder()
                        .normalColor(Color.WHITE)
                        .highlightedColor(Color.GRAY)
                        .normalText(titels[i])
                        .imagePadding(new Rect(15, 15, 15, 15))
                        .normalImageDrawable(ContextCompat.getDrawable(this, icons[i]))
                        .listener(index -> {
                            Intent intent;
                            switch (index) {
                                case 0:
                                    intent = new Intent(NewHome.this, Einscannen.class);
                                    break;

                                case 1:
                                    if (categoriesExsist)
                                        intent = new Intent(NewHome.this, VokabelManuell.class);
                                    else {
                                        intent = new Intent(NewHome.this, CategoryManager.class);
                                        intent.putExtra("CategoryManagerHeading", "neue Kategorie erstellen");
                                    }
                                    break;

                                case 2:
                                    intent = new Intent(NewHome.this, CategoryOverview.class);
                                    break;

                                case 3:
                                    intent = new Intent(NewHome.this, QuizSettings.class);
                                    break;

                                case 4:
                                    intent = new Intent(NewHome.this, Einstellungen.class);
                                    break;

                                case 5:
                                    intent = new Intent(NewHome.this, AppIntro.class);
                                    break;

                                default:
                                    throw new IllegalStateException("Unexpected value: " + index);
                            }
                            new Handler().postDelayed(() -> startActivity(intent), 500);
                        });
                bmb.addBuilder(builder);
            }

            final View shadow = findViewById(R.id.shadow);
            bmb.setOnBoomListener(new OnBoomListenerAdapter() {
                @Override
                public void onBoomWillShow() {
                    super.onBoomWillShow();

                    Transition transition = new Fade();
                    transition.setDuration(600);
                    transition.addTarget(shadow);

                    TransitionManager.beginDelayedTransition((ViewGroup) shadow.getRootView(), transition);
                    shadow.setVisibility(View.VISIBLE);
                }

                @Override
                public void onBoomWillHide() {
                    super.onBoomWillHide();

                    Transition transition = new Fade();
                    transition.setDuration(600);
                    transition.addTarget(shadow);

                    TransitionManager.beginDelayedTransition((ViewGroup) shadow.getRootView(), transition);
                    shadow.setVisibility(View.GONE);
                }
            });

            appBarLayout.addOnOffsetChangedListener((appBarLayout, verticalOffset) -> {
                final int fullOffset = -227;
                //trainedYesterday.setAlpha((int) (255 - 255 / fullOffset * verticalOffset));
                final int alpha = verticalOffset > fullOffset ? 255 - 255 / fullOffset * verticalOffset : 0;
                trainedYesterday.setTextColor(trainedYesterday.getTextColors().withAlpha(alpha));
                textYesterday.setTextColor(textYesterday.getTextColors().withAlpha(alpha));
                trainedToday.setTextColor(trainedToday.getTextColors().withAlpha(alpha));
                textToday.setTextColor(textToday.getTextColors().withAlpha(alpha));
                untrainedVocabs.setTextColor(untrainedVocabs.getTextColors().withAlpha(alpha));
                textUntrained.setTextColor(textUntrained.getTextColors().withAlpha(alpha));

                final float holeRaduis = verticalOffset > fullOffset ? 5f * verticalOffset / fullOffset : 5f;
                rightWrongPieChart.setHoleRadius(95 + holeRaduis);
                rightWrongPieChart.invalidate();

                if (fullOffset < verticalOffset) {
                    rightWrongPieChart.setVisibility(View.VISIBLE);
                    clickStats.setVisibility(View.VISIBLE);
                } else {
                    clickStats.setVisibility(View.GONE);
                    rightWrongPieChart.setVisibility(View.INVISIBLE);
                }
            });

            // Tabelle
            vokabelViewModel = new ViewModelProvider(this).get(VokabelViewModel.class);
            //weekdayViewModel = new ViewModelProvider(this).get(WeekdayViewModel.class);
            loadAll();
        }
    }

    private void loadAll() {
        LiveData<List<Vokabel>> vocabsLiveData = vokabelViewModel.getAlleVokabeln();

        vocabsLiveData.observe(NewHome.this, vocabs -> {
            if (vocabs != null) {
                vocs = vocabs;
                statisticsWrapper.setVisibility(View.VISIBLE);

                rightWrongPieChart.setUsePercentValues(true);
                rightWrongPieChart.getDescription().setEnabled(false);
                rightWrongPieChart.getLegend().setEnabled(false);
                rightWrongPieChart.animateY(1500, Easing.EasingOption.EaseInQuad);
                rightWrongPieChart.setBackgroundColor(Color.TRANSPARENT);
                rightWrongPieChart.setHoleRadius(95f);
                rightWrongPieChart.setHoleColor(Color.TRANSPARENT);
                rightWrongPieChart.setDrawEntryLabels(false);

                final String rightWrongLabel = "";
                ArrayList<PieEntry> rightWrongEntries = new ArrayList<>();

                int trained = 0;
                untrainedVocs = 0;

                for (Vokabel vocab: vocabs) {
                    if (vocab.getRichtig() > vocab.getFalsch()) {
                        trained ++;
                    }else {
                        untrainedVocs ++;
                    }
                }

                if(untrainedVocs == 0) {
                    if (vocabs.size() > 0)
                        untrained.setText("Du hast alle Vokabeln gelernt");
                    else
                        untrained.setText("Du hast noch keine Vokabeln");
                } else
                    untrained.setText("noch " + untrainedVocs + " Vokabeln ungelernt");

                Map<String, Integer> dataRightWrong = new HashMap<>();
                dataRightWrong.put("ungelernt", untrainedVocs);
                dataRightWrong.put("definitv_gelernt", trained); //Key muss länger sein als "ungelernt"

                ArrayList<Integer> colors = new ArrayList<>();
                colors.add(ContextCompat.getColor(this, R.color.colorText));
                colors.add(ContextCompat.getColor(this, R.color.colorGreenGray));

                for (String dataName: dataRightWrong.keySet()) {
                    rightWrongEntries.add(new PieEntry(Objects.requireNonNull(dataRightWrong.get(
                            dataName)).floatValue(), dataName));
                }

                PieDataSet dataSetRightWrong = new PieDataSet(rightWrongEntries,
                        rightWrongLabel);
                dataSetRightWrong.setColors(colors);
                dataSetRightWrong.setDrawValues(false);

                PieData chartDataRightWrong = new PieData(dataSetRightWrong);
                chartDataRightWrong.setValueFormatter(new PercentFormatter());
                //chartDataRightWrong.setDrawValues(false);

                rightWrongPieChart.setData(chartDataRightWrong);
                rightWrongPieChart.invalidate();

                untrainedVocabs.setText(String.valueOf(untrainedVocs));

                List<Vokabel> finalVocabs = vocabs;
                clickStats.setOnClickListener(v -> {
                    if(finalVocabs.size() > 0) {
                        Intent intent = new Intent(NewHome.this, Statistics.class);
                        intent.putExtra("mode", Statistics.MODE_LOAD_ALL);
                        startActivity(intent);
                    } else {
                        new LiveSmashBar.Builder(NewHome.this)
                                .title("Füge Vokabeln hinzu, um Statistiken zu nutzen.")
                                .titleColor(Color.WHITE)
                                .titleSizeInSp(18)
                                .backgroundColor(Color.parseColor("#2f3030"))
                                .dismissOnTapOutside()
                                .gravity(GravityView.TOP)
                                .showOverlay()
                                .overlayBlockable()
                                .duration(5000)
                                .show();
                    }
                });

                Collections.shuffle(vocabs);
                if(vocabs.size() > 10) {
                    vocabs = vocabs.subList(0, 10);

                    messageLess10Vocs.setVisibility(View.GONE);
                    tableCV.setVisibility(View.VISIBLE);
                    insertInTable(vocabs);
                } else {
                    messageLess10Vocs.setVisibility(View.VISIBLE);
                    tableCV.setVisibility(View.GONE);
                }
            }
            observerStatistics(vocabsLiveData);
        });

        final int dayOfWeekToday = LocalDate.now().getDayOfWeek();

        LiveData<Weekday> todaysWeekday = weekdayViewModel.getDay(dayOfWeekToday);
        todaysWeekday.observe(NewHome.this, day -> {
            if (day != null) {
                final int tempTDY = day.getNumberOfTrainedVocabs();
                if (tdy != tempTDY) {
                    tdy = tempTDY;
                    trainedToday.setText(String.valueOf(tempTDY));
                }
            }
        });

        LiveData<Weekday> yesterdaysWeekday = weekdayViewModel.getDay(
                dayOfWeekToday > 1 ? dayOfWeekToday - 1 : 7);
        yesterdaysWeekday.observe(NewHome.this, day -> {
            if (day != null) {
                final int tempYSTRDY = day.getNumberOfTrainedVocabs();
                if (ystrdy != tempYSTRDY) {
                    ystrdy = tempYSTRDY;
                    trainedYesterday.setText(String.valueOf(tempYSTRDY));
                }
            }
        });
    }

    private void observerStatistics(LiveData<List<Vokabel>> vocabsLiveData){
        vocabsLiveData.removeObservers(NewHome.this);

        vocabsLiveData.observe(NewHome.this, (vocabs) -> {
            if (vocabs != null) {
                if (!vocabs.isEmpty() && vocabs != vocs) {
                    vocs = vocabs;
                    statisticsWrapper.setVisibility(View.VISIBLE);

                    rightWrongPieChart.setUsePercentValues(true);
                    rightWrongPieChart.getDescription().setEnabled(false);
                    rightWrongPieChart.getLegend().setEnabled(false);
                    rightWrongPieChart.animateY(1500, Easing.EasingOption.EaseInQuad);
                    rightWrongPieChart.setBackgroundColor(Color.TRANSPARENT);
                    rightWrongPieChart.setHoleRadius(95f);
                    rightWrongPieChart.setHoleColor(Color.TRANSPARENT);
                    rightWrongPieChart.setDrawEntryLabels(false);

                    final String rightWrongLabel = "";
                    ArrayList<PieEntry> rightWrongEntries = new ArrayList<>();

                    int trained = 0;
                    untrainedVocs = 0;

                    for (Vokabel vocab: vocabs) {
                        if (vocab.getRichtig() > vocab.getFalsch()) {
                            trained ++;
                        }else {
                            untrainedVocs ++;
                        }
                    }

                    if(untrainedVocs == 0) {
                        if (vocabs.size() > 0)
                            untrained.setText("Du hast alle Vokabeln gelernt");
                        else
                            untrained.setText("Du hast noch keine Vokabeln");
                    } else
                        untrained.setText("noch " + untrainedVocs + " Vokabeln ungelernt");

                    Map<String, Integer> dataRightWrong = new HashMap<>();
                    dataRightWrong.put("ungelernt", untrainedVocs);
                    dataRightWrong.put("definitv_gelernt", trained); //Key muss länger sein als "ungelernt"

                    ArrayList<Integer> colors = new ArrayList<>();
                    colors.add(ContextCompat.getColor(this, R.color.colorText));
                    colors.add(ContextCompat.getColor(this, R.color.colorGreenGray));

                    for (String dataName: dataRightWrong.keySet()) {
                        rightWrongEntries.add(new PieEntry(Objects.requireNonNull(dataRightWrong.get(
                                dataName)).floatValue(), dataName));
                    }

                    PieDataSet dataSetRightWrong = new PieDataSet(rightWrongEntries, rightWrongLabel);
                    dataSetRightWrong.setColors(colors);
                    dataSetRightWrong.setDrawValues(false);

                    PieData chartDataRightWrong = new PieData(dataSetRightWrong);
                    chartDataRightWrong.setValueFormatter(new PercentFormatter());
                    //chartDataRightWrong.setDrawValues(false);

                    rightWrongPieChart.setData(chartDataRightWrong);
                    rightWrongPieChart.invalidate();

                    untrainedVocabs.setText(String.valueOf(untrainedVocs));

                    clickStats.setOnClickListener(v -> {
                        if(vocabs.size() > 0) {
                            Intent intent = new Intent(NewHome.this, Statistics.class);
                            intent.putExtra("mode", Statistics.MODE_LOAD_ALL);
                            startActivity(intent);
                        } else {
                            new LiveSmashBar.Builder(NewHome.this)
                                    .title("Füge Vokabeln hinzu, um Statistiken zu nutzen.")
                                    .titleColor(Color.WHITE)
                                    .titleSizeInSp(18)
                                    .backgroundColor(Color.parseColor("#2f3030"))
                                    .dismissOnTapOutside()
                                    .gravity(GravityView.TOP)
                                    .showOverlay()
                                    .overlayBlockable()
                                    .duration(5000)
                                    .show();
                        }
                    });
                }
            }
        });
    }

    private void createCategoryBubbles() {
        KategorienViewModel kategorienViewModel = new ViewModelProvider(this).get(KategorienViewModel.class);
        LiveData<List<Kategorie>> alleKategorien = kategorienViewModel.getAlleKategorien();

        alleKategorien.observe(this, kategories -> {
            if (kategories != null) {
                if (kategories.size() > 0) {
                    noCats.setVisibility(View.GONE);
                    categoriesExsist = true;
                    int i = 0;
                    hv.setVisibility(View.VISIBLE);
                    line.setVisibility(View.VISIBLE);
                    tv.setVisibility(View.VISIBLE);
                    ll.removeAllViews();
                    Collections.reverse(kategories);
                    for (Kategorie cat : kategories) {
                        if (kategories.indexOf(cat) < 10) {
                            String name;
                            if(cat.getName().length() > 1)
                                name = cat.getName().substring(0, 1).toUpperCase() + cat.getName().substring(1, 2).toLowerCase();
                            else
                                name = cat.getName().toUpperCase();
                            ++i;

                            Button but = new Button(this);
                            but.setText(name);
                            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(55), dp(55));
                            params.setMargins(dp(15), 0, dp(15), 0);
                            but.setLayoutParams(params);
                            but.setTextColor(Color.WHITE);
                            but.setTextSize(20f);
                            but.setClickable(true);
                            but.setFocusable(true);
                            but.setAllCaps(false);
                            but.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

                            switch (i) {
                                case 1:
                                    but.setBackground(ContextCompat.getDrawable(this, R.drawable.fab_1));
                                    break;
                                case 2:
                                    but.setBackground(ContextCompat.getDrawable(this, R.drawable.fab_2));
                                    break;
                                case 3:
                                    but.setBackground(ContextCompat.getDrawable(this, R.drawable.fab_3));
                                    break;
                                case 4:
                                    but.setBackground(ContextCompat.getDrawable(this, R.drawable.fab_4));
                                    break;
                                default:
                                    but.setBackground(ContextCompat.getDrawable(this, R.drawable.fab_1));
                                    i = 1;
                            }
                            but.setOnClickListener(v -> {
                                Intent intent = new Intent(NewHome.this, AlleVokabelnAnzeigen.class);
                                intent.putExtra("Category", cat.getName());
                                startActivity(intent);
                            });
                            ll.addView(but);
                        }
                    }
                } else {
                    ll.removeAllViews();
                    categoriesExsist = false;
                    noCats.setVisibility(View.VISIBLE);
                    hv.setVisibility(View.VISIBLE);
                    line.setVisibility(View.VISIBLE);
                    tv.setVisibility(View.VISIBLE);
                }
            } else {
                categoriesExsist = false;
                noCats.setVisibility(View.VISIBLE);
                hv.setVisibility(View.VISIBLE);
                line.setVisibility(View.VISIBLE);
                tv.setVisibility(View.VISIBLE);
            }
        });
    }

    private void readFieldEng(@NotNull final List<Vokabel> vocabs, final int pointer) {
        String accent = tb.getString("accent");

        if (accent.isEmpty()) {
            tb.putString("accent", "US");
            accent = "US";
        }

        final Locale eng = new Locale("en", accent);

        tts.setLanguage(eng);
        tts.speak(vocabs.get(pointer).getVokabelENG(), TextToSpeech.QUEUE_ADD, null,
                TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID);
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {

            }

            @Override
            public void onDone(String utteranceId) {
                readFieldDe(vocabs, pointer);
            }

            @Override
            public void onError(String utteranceId) {

            }
        });
    }

    private void readFieldDe(@NotNull final List<Vokabel> vocabs, final int pointer) {
        final Locale de = new Locale("de", "de");

        tts.setLanguage(de);
        tts.speak("bedeutet" + vocabs.get(pointer).getVokabelDE(), TextToSpeech.QUEUE_ADD,
                null, TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID);
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {

            }

            @Override
            public void onDone(String utteranceId) {
                if (pointer + 1 < vocabs.size()) {
                    readFieldEng(vocabs, pointer + 1);
                }
            }

            @Override
            public void onError(String utteranceId) {

            }
        });
    }

    private void readVocabs(@NotNull final List<Vokabel> vocabs) {
        tts = new TextToSpeech(NewHome.this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                Log.e("TTS", "SUCCESS");
                readFieldEng(vocabs, 0);
            }
        });

                /*
        voiceControl.speak(vocEng, () -> voiceControl.speak("bedeutet " + vocDe, () -> {
            Log.e("pointer + 1 < vocabs.size()", String.valueOf(pointer + 1 < vocabs.size()));
            if (pointer + 1 < vocabs.size()) readNextVocab(vocabs, pointer + 1);
        }, de), eng);
                 */

    }

    private void insertInTable(List<Vokabel> vocabs) {
        Collections.sort(vocabs, (voc1, voc2) -> voc1.getVokabelENG().compareToIgnoreCase(voc2.getVokabelENG()));

        final TabelleVokabelAdapterAnzeigen stringVokabelAdapterView = new TabelleVokabelAdapterAnzeigen(this,
                TabelleVokabelAdapterAnzeigen.OUTPUT_MODE_VIEWABLE, vocabs, vokabelViewModel, false);
        vocabTable.setAdapter(stringVokabelAdapterView);
        vocabTable.setLayoutManager(new LinearLayoutManager(this));
        stringVokabelAdapterView.setVokabelCache(vocabs);
    }

    private int dp(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data != null) Log.e("onActivityResult", data.toString());
        else Log.e("onActivityResult", "null");
        if (requestCode == VoiceInput.getVoiceRecognitionRequestCode()
                && resultCode == Activity.RESULT_OK) {
            Log.e("onActivityResult", "setting results...");
            // Fill the list view with the strings the recognizer thought it could have heard
            assert data != null;
            ArrayList<String> matches = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);
            recognitionResultVM.setCurrentResult(matches);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
