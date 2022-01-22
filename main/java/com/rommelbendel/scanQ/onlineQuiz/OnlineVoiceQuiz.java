package com.rommelbendel.scanQ.onlineQuiz;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.rommelbendel.scanQ.R;
import com.rommelbendel.scanQ.Vokabel;
import com.rommelbendel.scanQ.VokabelViewModel;
import com.rommelbendel.scanQ.WeekdayViewModel;
import com.rommelbendel.scanQ.additional.TinyDB;
import com.rommelbendel.scanQ.speechrecognition.RecognitionViewModel;
import com.rommelbendel.scanQ.speechrecognition.VoiceInput;
import com.yuvraj.livesmashbar.enums.GravityView;
import com.yuvraj.livesmashbar.view.LiveSmashBar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import static androidx.lifecycle.Lifecycle.State.STARTED;

public class OnlineVoiceQuiz extends AppCompatActivity {

    private ConstraintLayout waiting;
    private LottieAnimationView button_voice_input_animated;
    private TextView vokabelAnzeige;
    private Button button_ok;
    private ImageButton button_next;
    private ImageButton button_previous;
    private EditText answer_text;
    private TextView points;
    private CardView quizCard;
    private LinearLayout quizLayout;
    private CardView resultCardOfflline;
    private ConstraintLayout waitingEnd, playerLeft, resultCardOnline;
    private TextView resultRichtig, resultFalsch, resultEvaluation, leftText;
    private MaterialButton finishButton, newGame;
    private CardView dialogRight;
    private CardView dialogWrong;
    private TextView dialogSolutionWrong;
    private TextView dialogTextSolutionWrong;
    private TextView dialogSolutionRight;
    private TextView dialogTextSolutionRight;
    private LottieAnimationView countdown, resultAnim;
    private TableLayout resultTable;

    private boolean recordAudio;
    private List<Vokabel> vokabels = new ArrayList<>();
    private Bundle frage_und_antwort;
    private VokabelViewModel vokabelViewModel;
    private WeekdayViewModel weekdayViewModel;
    private LiveData<List<Vokabel>> alleVokabelnLiveData;
    private List<Vokabel> quizFrageVokabeln; //diese Liste wird durchgegangen
    private String categoryName;
    private int vocabNum;
    private boolean onlyUntrained;
    private RecognitionViewModel recognitionResultVM;
    //private SpeechRecognizer speechRecognizer;
    private VoiceInput voiceInput;
    private boolean vrActive = false;

    private boolean notFinished = true;
    private boolean isEmpty = false;
    private boolean isAdmin = false;
    private int frageNummer = 0;
    private final String language_from = "DE";
    private int punkte = 0;
    private final Timer activeTimer = new Timer();
    private String previousAnswer = "";

    private String id;
    private ArrayList<String> player;
    private String enterCode, username;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
                @Override
                public void onActivityResult(Boolean isGranted) {
                    if (isGranted) {
                        recordAudio = true;
                    } else {
                        recordAudio = false;
                        AlertDialog.Builder featureUnaviable = new AlertDialog.Builder(OnlineVoiceQuiz.this);
                        featureUnaviable.setTitle("Mikrofon notwendig");
                        featureUnaviable.setMessage("Dieses Quiz benötigt Zugriff auf dein Mikrofon, um richtig zu funktionieren");
                        featureUnaviable.setPositiveButton("OK", (dialog, which) -> dialog.cancel());
                        featureUnaviable.show();
                    }
                }
            });

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_quiz_voice);

            Bundle extras = getIntent().getExtras();
            FirebaseDatabase database = FirebaseDatabase.getInstance();

            if (extras != null) {
                id = extras.getString("id");
                player = extras.getStringArrayList("player");
                enterCode = extras.getString("enter_code");
                username = extras.getString("playerName");
                isAdmin = extras.getBoolean("admin");
            } else {
                finish();
            }
            DatabaseReference onlineQuizRef = database.getReference(enterCode);

            waiting = findViewById(R.id.waiting);
            button_voice_input_animated = findViewById(R.id.button_voice_record_animated);
            vokabelAnzeige = findViewById(R.id.voc);
            button_ok = findViewById(R.id.button_OK);
            answer_text = findViewById(R.id.input_voice_to_text);
            button_next = findViewById(R.id.quizright);
            button_previous = findViewById(R.id.quizleft);
            points = findViewById(R.id.points);
            resultCardOfflline = findViewById(R.id.resultCard1);
            resultCardOnline = findViewById(R.id.online_result_card);
            quizCard = findViewById(R.id.questionCard1);
            quizLayout = findViewById(R.id.frage_layout);
            resultRichtig = findViewById(R.id.countRight);
            resultFalsch = findViewById(R.id.countWrong);
            resultEvaluation = findViewById(R.id.succsess);
            finishButton = findViewById(R.id.finish_online_game);
            newGame = findViewById(R.id.new_online_game);
            dialogRight = findViewById(R.id.richtig);
            dialogWrong = findViewById(R.id.falsch);
            dialogSolutionRight = findViewById(R.id.solution_right);
            dialogTextSolutionRight = findViewById(R.id.solution_text_right);
            dialogSolutionWrong = findViewById(R.id.solution_wrong);
            dialogTextSolutionWrong = findViewById(R.id.solution_text_wrong);
            waitingEnd = findViewById(R.id.waiting_end);
            playerLeft = findViewById(R.id.player_left);
            countdown = findViewById(R.id.left_countdown);
            leftText = findViewById(R.id.player_left_text);
            resultAnim = findViewById(R.id.result_anim);
            resultTable = findViewById(R.id.points_table);

            resultCardOfflline.setVisibility(View.GONE);
            resultCardOnline.setVisibility(View.GONE);

            activeTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    HashMap<String, Object> activeMap = new HashMap<>();
                    activeMap.put(String.valueOf(id), ServerValue.TIMESTAMP);

                    if (OnlineVoiceQuiz.this.getLifecycle().getCurrentState().isAtLeast(STARTED))
                        onlineQuizRef.child("active").updateChildren(activeMap);
                }
            }, 0, 5000);

            new Timer().scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    if(notFinished) {
                        if (isEmpty) {
                            quizCard.setVisibility(View.GONE);
                            dialogRight.setVisibility(View.GONE);
                            dialogWrong.setVisibility(View.GONE);
                            button_ok.setVisibility(View.GONE);
                            resultCardOfflline.setVisibility(View.GONE);

                            playerLeft.setVisibility(View.VISIBLE);
                            if (!isNetworkAvailable())
                                leftText.setText("Deine Internetverbindung ist abgebrochen");
                            if (!countdown.isAnimating())
                                countdown.playAnimation();

                            new Handler().postDelayed(() -> finish(), 3000);
                        }
                    }
                }
            }, 0, 1500);

            try {
                if(notFinished) {
                    onlineQuizRef.child("empty").addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (notFinished)
                                isEmpty = snapshot.getValue(Boolean.class);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                        }
                    });
                }

                onlineQuizRef.child("restart").addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!notFinished) {
                            boolean restart = snapshot.getValue(Boolean.class);
                            if (!isAdmin) {
                                if (restart) {
                                    newGame.setEnabled(true);
                                    restartForNonAdmin(onlineQuizRef);
                                } else
                                    newGame.setEnabled(false);
                            }
                        }
                    }

                    @Override
                    public void onCancelled (@NonNull DatabaseError error){}
                });

                if (notFinished) {
                    onlineQuizRef.child("vocs").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if(notFinished) {
                                ArrayList<HashMap<String, String>> vokabelHashMaps =
                                        (ArrayList<HashMap<String, String>>) snapshot.getValue();
                                assert vokabelHashMaps != null;
                                for (HashMap<String, String> vocMap : vokabelHashMaps) {
                                    OnlineVoiceQuiz.this.vokabels.add(new Vokabel(
                                            vocMap.get("vokabelENG"),
                                            vocMap.get("vokabelDE"),
                                            vocMap.get("kategorie")
                                    ));
                                }

                                Collections.shuffle(vokabels);
                                startQuiz(onlineQuizRef);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                        }
                    });
                }

            } catch (Exception exception) {
                new LiveSmashBar.Builder(OnlineVoiceQuiz.this)
                        .title("Verbindung zur Datenbank abgebrochen")
                        .titleColor(Color.WHITE)
                        .backgroundColor(Color.parseColor("#541111"))
                        .gravity(GravityView.TOP)
                        .duration(5000)
                        .dismissOnTapOutside()
                        .overlayBlockable()
                        .showOverlay()
                        .show();

                Log.e("error", String.valueOf(exception));
            }

            button_voice_input_animated.setOnClickListener(v -> {
                v.setActivated(false);
                Log.e("Touch vrActive", String.valueOf(vrActive));

                if (vrActive) {
                    Log.e("vrActive", "true");
                    //speechRecognizer.stopListening();
                    //speechRecognizer = null;
                    vrActive = false;
                    button_voice_input_animated.setSpeed(-2f);
                    button_voice_input_animated.playAnimation();
                    ((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(50);
                    //refreshVoiceInputButton();
                } else {
                    if (checkRecordAudioPermission()) {

                        button_voice_input_animated.setSpeed(2f);
                        button_voice_input_animated.playAnimation();
                        ((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(50);

                        Log.e("vrActive", "false");

                        vrActive = true;

                        recognitionResultVM =
                                (RecognitionViewModel) new ViewModelProvider(this).get(
                                        RecognitionViewModel.class);
                        TinyDB tb = new TinyDB(OnlineVoiceQuiz.this);
                        String accent = tb.getString("accent");

                        if (accent.isEmpty()) {
                            tb.putString("accent", "GB");
                            accent = "GB";
                        }

                        Locale locale = new Locale("en", accent);

                        voiceInput = new VoiceInput(OnlineVoiceQuiz.this,
                                (Activity) OnlineVoiceQuiz.this, recognitionResultVM, locale,
                                false);

                        voiceInput.listen();

                        /*
                        speechRecognizer = new SpeechRecognizer(getApplication(),
                                getSupportFragmentManager(), recognitionResultVM, locale,
                                false);

                        speechRecognizer.listen();
                         */

                        Objects.requireNonNull(recognitionResultVM.getCurrentResult()).observe(
                                OnlineVoiceQuiz.this, result -> {
                                    Log.e("Observer", "Step 1");
                                    if (result != null) {
                                        Log.e("Observer", result.toString());
                                        Log.e("Observer", "Step 2");
                                        if (!result.get(0).equals(previousAnswer)
                                                && answer_text.isEnabled()) {
                                            Log.e("Observer", "Step 3");
                                            answer_text.setText(result.get(0));
                                            previousAnswer = result.get(0);
                                        }
                                        //voiceInput.stopListening();
                                        button_voice_input_animated.setSpeed(-2f);
                                        button_voice_input_animated.playAnimation();
                                        Log.e("Observer", "Step 4");

                                        recognitionResultVM.setCurrentResult(null);
                                        recognitionResultVM.getCurrentResult().removeObservers(
                                                OnlineVoiceQuiz.this);
                                    }
                                });

                        /*
                        button_voice_input.setBackground(ContextCompat.getDrawable(
                                QuizVoice.this, R.drawable.ic_cancel));
                         */

                    } else {
                        requestRecordAudioPermission();
                    }
                }

                v.setActivated(true);
            });
            /*button_voice_input.setOnTouchListener((v, event) -> {
                v.onTouchEvent(event);
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (checkRecordAudioPermission()) {

                            RecognitionViewModel recognitionResultVM = new ViewModelProvider(this).get(
                                    RecognitionViewModel.class);
                            TinyDB tb = new TinyDB(OnlineVoiceQuiz.this);
                            String accent = tb.getString("accent");

                            if (accent.isEmpty()) {
                                tb.putString("accent", "UK");
                                accent = "UK";
                            }

                            Locale locale = new Locale("en", accent);

                            com.rommelbendel.scanQ.speechrecognition.SpeechRecognizer speechRecognizer = new com.rommelbendel.scanQ.speechrecognition.SpeechRecognizer(getApplication(),
                                    getSupportFragmentManager(), recognitionResultVM, locale,
                                    false);

                            speechRecognizer.listen();

                            recognitionResultVM.getCurrentResult().observe(OnlineVoiceQuiz.this,
                                    result -> {
                                        if (result != null && !result.equals(previousAnswer)) {
                                            answer_text.setText(result);
                                            previousAnswer = result;
                                        }
                                    });
                        } else {
                            requestRecordAudioPermission();
                        }
                        return true;

                    case MotionEvent.ACTION_UP:
                        v.performClick();
                        speechRecognizer.stopListening();
                        ((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(50);
                        answer_text.setBackgroundColor(Color.parseColor("#ffffff"));
                        answer_text.setTextColor(Color.parseColor("#107575"));
                        Toast.makeText(OnlineVoiceQuiz.this, "Aufnahme beendet",
                                Toast.LENGTH_SHORT).show();
                        return true;
                }
                return false;
            });*/
        } catch (Exception e) {
            Log.e("error", e.getMessage());
        }
    }

    private void startQuiz(DatabaseReference reference) {
        frage_und_antwort = new Bundle();

        button_previous.setOnClickListener(v -> previousQuestion(reference));
        button_next.setOnClickListener(v -> nextQuestion(reference));
        button_ok.setOnClickListener(v -> checkAnswer(reference));

        nextQuestion(reference);
    }

    private void nextQuestion(DatabaseReference reference) {
        try {
            if (frageNummer == vokabels.size()) {
                frageNummer = 1;
            } else {
                frageNummer++;
            }
            prepareQuestion();
            Vokabel vokabel = vokabels.get(frageNummer - 1);
            setQuestion(vokabel);
        } catch (NullPointerException e) {
            AlertDialog.Builder warnung = new AlertDialog.Builder(
                    OnlineVoiceQuiz.this);
            warnung.setTitle("Vokabeln konnten nicht geladen werden.");
            warnung.setMessage("Es sind nicht genug Vokabeln vorhanden.");
            warnung.setPositiveButton("OK", (dialog, which) -> {
                dialog.cancel();
                finish();
            });
            warnung.show();
        }
    }

    private void previousQuestion(DatabaseReference reference) {
        try {
            if (frageNummer == 1) {
                frageNummer = vokabels.size();
            } else {
                frageNummer--;
            }
            prepareQuestion();
            Vokabel vokabel = vokabels.get(frageNummer - 1);
            setQuestion(vokabel);
        } catch (NullPointerException e) {
            AlertDialog.Builder warnung = new AlertDialog.Builder(
                    OnlineVoiceQuiz.this);
            warnung.setTitle("Vokabeln konnten nicht geladen werden.");
            warnung.setMessage("Es sind nicht genug Vokabeln vorhanden.");
            warnung.setPositiveButton("OK", (dialog, which) -> {
                dialog.cancel();
                finish();
            });
            warnung.show();
        }
    }

    private void prepareQuestion() {
        dialogWrong.setVisibility(View.GONE);
        dialogRight.setVisibility(View.GONE);
        if (language_from.equals("DE")) {
            if (frage_und_antwort.containsKey(vokabels.get(frageNummer - 1)
                    .getVokabelDE())) {

                String answer = frage_und_antwort.getString(vokabels.get(frageNummer - 1)
                        .getVokabelDE());
                String question = vokabels.get(frageNummer - 1).getVokabelDE();
                Log.e("question", question);
                answer_text.setText(answer);
                //button_voice_input.setEnabled(false);
                button_ok.setEnabled(false);
                answer_text.setEnabled(false);
                if (vokabels.get(frageNummer - 1).getVokabelENG().equals(answer)) {

                    dialogRight.setVisibility(View.VISIBLE);
                    dialogTextSolutionRight.setText(String.format("%s = %s", question, answer));
                } else {

                    dialogWrong.setVisibility(View.VISIBLE);
                    dialogTextSolutionWrong.setText(String.format("%s = %s", question, answer));
                }
            } else {
                answer_text.setText("");
                //button_voice_input.setEnabled(true);
                button_ok.setEnabled(true);
                answer_text.setEnabled(true);
                quizLayout.setBackgroundColor(Color.WHITE);
            }
        } else {
            if (frage_und_antwort.containsKey(vokabels.get(frageNummer - 1).getVokabelENG())) {
                String answer = frage_und_antwort.getString(vokabels.get(frageNummer - 1)
                        .getVokabelENG());
                String question = vokabels.get(frageNummer - 1).getVokabelENG();

                answer_text.setText(answer);
                //button_voice_input.setEnabled(false);
                button_ok.setEnabled(false);
                answer_text.setEnabled(false);

                if (vokabels.get(frageNummer - 1).getVokabelDE().equals(answer)) {
                    quizLayout.setBackgroundColor(Color.rgb(88, 214, 141));
                    dialogRight.setVisibility(View.VISIBLE);
                    dialogTextSolutionRight.setText(String.format("%s = %s", question, answer));
                } else {
                    quizLayout.setBackgroundColor(Color.rgb(236, 112, 99));
                    dialogWrong.setVisibility(View.VISIBLE);
                    dialogTextSolutionWrong.setText(String.format("%s = %s", question, answer));
                }
            } else {
                answer_text.setText("");
                //button_voice_input.setEnabled(true);
                button_ok.setEnabled(true);
                answer_text.setEnabled(true);
                quizLayout.setBackgroundColor(Color.WHITE);
            }
        }
    }

    private void setQuestion(Vokabel vokabel) {
        String anzeige;
        if (language_from.equals("DE")) {
            anzeige = vokabel.getVokabelDE();
        } else {
            anzeige = vokabel.getVokabelENG();
        }
        vokabelAnzeige.setText(anzeige);

    }

    @SuppressLint("DefaultLocale")
    private void checkAnswer(DatabaseReference reference) {
        //button_voice_input.setEnabled(false);
        button_ok.setEnabled(false);
        answer_text.setEnabled(false);
        String antwort = answer_text.getText().toString().trim();
        String loesung;
        String frage;
        Vokabel frageVokabel = vokabels.get(frageNummer -1);

        if (language_from.equals("DE")) {
            frage_und_antwort.putString(frageVokabel.getVokabelDE(), antwort);
            loesung = frageVokabel.getVokabelENG();
            frage = frageVokabel.getVokabelDE();
        } else {
            frage_und_antwort.putString(frageVokabel.getVokabelENG(), antwort);
            loesung = frageVokabel.getVokabelDE();
            frage = frageVokabel.getVokabelENG();
        }

        if (antwort.equalsIgnoreCase(loesung)) {
            givePoint();
            dialogRight.setVisibility(View.VISIBLE);
            dialogTextSolutionRight.setText(String.format("%s = %s", frage, loesung));
        } else {
            //quizLayout.setBackgroundColor(Color.rgb(236, 112, 99));
            dialogWrong.setVisibility(View.VISIBLE);
            dialogTextSolutionWrong.setText(String.format("%s = %s", frage, loesung));
        }

        boolean solved = true;
        for (int i = 0; i < vokabels.size(); i++) {
            if (language_from.equals("DE")) {
                if (!frage_und_antwort.containsKey(vokabels.get(i).getVokabelDE())) {
                    solved = false;
                    break;
                }
            } else {
                if (!frage_und_antwort.containsKey(vokabels.get(i).getVokabelENG())) {
                    solved = false;
                    break;
                }
            }
        }

        if (solved)
            showResults(reference);
    }

    private void showResults(DatabaseReference reference) {
        reference.child("points").child(id).setValue(punkte);

        button_next.setEnabled(false);
        button_previous.setEnabled(false);

        resultCardOnline.setVisibility(View.GONE);
        waitingEnd.setVisibility(View.VISIBLE);
        quizCard.setVisibility(View.GONE);
        dialogWrong.setVisibility(View.GONE);
        dialogRight.setVisibility(View.GONE);

        reference.child("done").child(String.valueOf(id)).setValue(true);
        reference.child("done").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                try {
                    if (notFinished) {
                        ArrayList<Boolean> done = (ArrayList<Boolean>) snapshot.getValue();
                        if (done != null) {
                            if (!done.contains(false)) {
                                activeTimer.cancel();
                                notFinished = false;

                                reference.child("points").get().addOnCompleteListener(task -> {
                                    try {
                                        boolean draw = false;
                                        final ArrayList<Long> points = (ArrayList<Long>) task.getResult().getValue();
                                        ArrayList<OnlinePlayer> onlinePlayers = new ArrayList<>();

                                        if (isAdmin) {
                                            newGame.setEnabled(true);
                                            newGame.setOnClickListener(v -> {
                                                Bundle extras = new Bundle();
                                                extras.putString("id", "0");
                                                extras.putString("enterCode", enterCode);
                                                extras.putString("playerName", username);

                                                Intent intentRecreate = new Intent(OnlineVoiceQuiz.this, OnlineRecreateNewGame.class);
                                                intentRecreate.putExtras(extras);
                                                startActivity(intentRecreate);
                                                finish();
                                            });
                                        }

                                        assert points != null;
                                        for (String p : player) {
                                            int index = player.indexOf(p);

                                            onlinePlayers.add(new OnlinePlayer(p, points.get(index)));
                                        }
                                        onlinePlayers = sortPoints(onlinePlayers);

                                        final ArrayList<Long> rankPoints = new ArrayList<>();
                                        for (OnlinePlayer op : onlinePlayers) {
                                            final int i = onlinePlayers.indexOf(op);


                                            if (!rankPoints.contains(op.getPoints()))
                                                rankPoints.add(op.getPoints());
                                            else {
                                                if (rankPoints.indexOf(op.getPoints()) == 0)
                                                    draw = true;
                                            }

                                            TableRow playerRow = (TableRow) resultTable.getChildAt(i);
                                            playerRow.setVisibility(View.VISIBLE);
                                            ((TextView) playerRow.getChildAt(0)).setText(String.format(" %s. ", rankPoints.indexOf(op.getPoints()) + 1));
                                            ((TextView) playerRow.getChildAt(2)).setText(op.getPlayerName());
                                            ((TextView) playerRow.getChildAt(4)).setText(String.format("%s Punkte", op.getPoints()));
                                        }

                                        waitingEnd.setVisibility(View.GONE);
                                        resultCardOnline.setVisibility(View.VISIBLE);

                                        if (Collections.max(points) == punkte) {
                                            if (draw) {
                                                resultEvaluation.setText(R.string.unentschieden);
                                                resultEvaluation.setTextSize(20f);
                                                resultAnim.setAnimation(R.raw.draw_handshake);
                                                resultAnim.setRepeatCount(1);
                                            } else {
                                                resultEvaluation.setText(R.string.gratualtion);
                                                resultAnim.setAnimation(R.raw.winner);
                                                resultAnim.setRepeatCount(2);
                                            }
                                        } else {
                                            resultEvaluation.setText(R.string.schade);
                                            resultAnim.setAnimation(R.raw.looser);
                                            resultAnim.setRepeatCount(5);
                                        }

                                        new Handler().postDelayed(() -> resultAnim.playAnimation(), 500);
                                        finishButton.setOnClickListener(v -> finish());
                                    } catch (Exception exception) {
                                        new LiveSmashBar.Builder(OnlineVoiceQuiz.this)
                                                .title("Verbindung zur Datenbank abgebrochen")
                                                .titleColor(Color.WHITE)
                                                .backgroundColor(Color.parseColor("#541111"))
                                                .gravity(GravityView.TOP)
                                                .duration(5000)
                                                .dismissOnTapOutside()
                                                .overlayBlockable()
                                                .showOverlay()
                                                .show();

                                        Log.e("error", String.valueOf(exception));
                                    }
                                });
                            } else {
                                resultCardOnline.setVisibility(View.GONE);
                                waitingEnd.setVisibility(View.VISIBLE);
                            }
                        }
                    }
                } catch (Exception ignored) {
                    HashMap<String, Boolean> done = (HashMap<String, Boolean>) snapshot.getValue();

                    if (done != null) {
                        if (!done.containsValue(false)) {
                            reference.child("points").get().addOnCompleteListener(task -> {
                                ArrayList<Long> points = (ArrayList<Long>) task.getResult().getValue();
                                ArrayList<OnlinePlayer> onlinePlayers = new ArrayList<>();

                                assert points != null;
                                //ArrayList<Object> comparePoints = new ArrayList<>(points);
                                //Collections.sort(points);

                                for (String p : player) {
                                    int index = player.indexOf(p);

                                    onlinePlayers.add(new OnlinePlayer(p, points.get(index)));
                                }
                                sortPoints(onlinePlayers);

                                boolean draw = false;
                                final ArrayList<Long> rankPoints = new ArrayList<>();
                                for (OnlinePlayer op : onlinePlayers) {
                                    final int i = onlinePlayers.indexOf(op);


                                    if(!rankPoints.contains(op.getPoints()))
                                        rankPoints.add(op.getPoints());
                                    else {
                                        if(rankPoints.indexOf(op.getPoints()) == 0)
                                            draw = true;
                                    }

                                    TableRow playerRow = (TableRow) resultTable.getChildAt(i);
                                    playerRow.setVisibility(View.VISIBLE);
                                    ((TextView) playerRow.getChildAt(0)).setText(String.format(" %s. ", rankPoints.indexOf(op.getPoints())+1));
                                    ((TextView) playerRow.getChildAt(2)).setText(op.getPlayerName());
                                    ((TextView) playerRow.getChildAt(4)).setText(String.format("%s Punkte", op.getPoints()));
                                }

                                waitingEnd.setVisibility(View.GONE);
                                resultCardOnline.setVisibility(View.VISIBLE);

                                if (Collections.max(points) == punkte) {
                                    if(draw) {
                                        resultEvaluation.setText(R.string.unentschieden);
                                        resultEvaluation.setTextSize(20f);
                                        resultAnim.setAnimation(R.raw.draw_handshake);
                                        resultAnim.setRepeatCount(1);
                                    } else {
                                        resultEvaluation.setText(R.string.gratualtion);
                                        resultAnim.setAnimation(R.raw.winner);
                                        resultAnim.setRepeatCount(2);
                                    }
                                } else {
                                    resultEvaluation.setText(R.string.schade);
                                    resultAnim.setAnimation(R.raw.looser);
                                    resultAnim.setRepeatCount(5);
                                }

                                new Handler().postDelayed(() -> resultAnim.playAnimation(), 500);
                                finishButton.setOnClickListener(v -> finish());
                            });
                        } else {
                            waitingEnd.setVisibility(View.VISIBLE);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void givePoint() {
        punkte ++;
        points.setText(String.valueOf(punkte));
        Log.d("Punkte", String.valueOf(punkte));
    }

    private void restartForNonAdmin(DatabaseReference reference) {
        newGame.setOnClickListener(v -> {
            if(isNetworkAvailable()) {
                waiting.setVisibility(View.VISIBLE);
                resultCardOnline.setVisibility(View.GONE);

                reference.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.child("isStarted").getValue(Boolean.class)) {
                            ArrayList<String> players = new ArrayList<>();
                            ArrayList<String> rawPlayer = (ArrayList<String>) snapshot.child("player").getValue();
                            assert rawPlayer != null;
                            for(String player : rawPlayer) {
                                if (!player.equals("0")) {
                                    players.add(player);
                                }
                            }

                            if (players.size() < 10 && !players.isEmpty()) {
                                Bundle extras = new Bundle();

                                for (int i = 0; i < 10; i++) {
                                    if (Objects.equals(snapshot.child("player").child(String.valueOf(i)).getValue(String.class), "0")) {
                                        reference.child("player").child(String.valueOf(i)).setValue(username);
                                        reference.child("active").child(String.valueOf(i)).setValue(System.currentTimeMillis());
                                        reference.child("done").child(String.valueOf(i)).setValue(false);
                                        reference.child("points").child(String.valueOf(i)).setValue(0);
                                        extras.putString("id", String.valueOf(i));
                                        break;
                                    }
                                }
                                extras.putString("enter_code", enterCode);
                                extras.putString("playerName", username);

                                //Intent intent = new Intent(OnlineVoiceQuiz.this, OnlineVoiceQuiz.class);
                                Intent intent = new Intent(OnlineVoiceQuiz.this, OnlineLobbyWaiting.class);
                                intent.putExtras(extras);
                                OnlineVoiceQuiz.this.startActivity(intent);
                                finish();

                            } else {
                                waiting.setVisibility(View.GONE);
                                resultCardOnline.setVisibility(View.VISIBLE);

                                new LiveSmashBar.Builder(OnlineVoiceQuiz.this)
                                        .title("Die Lobby ist voll")
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
                            waiting.setVisibility(View.GONE);
                            resultCardOnline.setVisibility(View.VISIBLE);

                            new LiveSmashBar.Builder(OnlineVoiceQuiz.this)
                                    .title("Das Spiel hat bereits angefangen")
                                    .titleColor(Color.WHITE)
                                    .backgroundColor(Color.parseColor("#541111"))
                                    .gravity(GravityView.TOP)
                                    .duration(5000)
                                    .dismissOnTapOutside()
                                    .overlayBlockable()
                                    .showOverlay()
                                    .show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
            } else {
                new LiveSmashBar.Builder(OnlineVoiceQuiz.this)
                        .title("Bitte überprüfe deine Internetverbindung")
                        .titleColor(Color.WHITE)
                        .backgroundColor(Color.parseColor("#541111"))
                        .gravity(GravityView.TOP)
                        .duration(3000)
                        .dismissOnTapOutside()
                        .overlayBlockable()
                        .showOverlay()
                        .show();
            }
        });

    }

    private boolean checkRecordAudioPermission() {
        return ContextCompat.checkSelfPermission(OnlineVoiceQuiz.this,
                Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestRecordAudioPermission() {
        requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
    }

    private ArrayList<OnlinePlayer> sortPoints(ArrayList<OnlinePlayer> op) {

        ArrayList<Long> tempPoints = new ArrayList<>();
        ArrayList<OnlinePlayer> resultOnlinePlayer = new ArrayList<>();

        for(OnlinePlayer onlinePlayer : op) {
            tempPoints.add(onlinePlayer.getPoints());
        }
        Collections.sort(tempPoints, Collections.reverseOrder());

        for(Long tp : tempPoints) {
            for(OnlinePlayer onlinePlayer : op) {
                if(tp == onlinePlayer.getPoints())
                    resultOnlinePlayer.add(onlinePlayer);
            }
        }

        return resultOnlinePlayer;
    }
}

