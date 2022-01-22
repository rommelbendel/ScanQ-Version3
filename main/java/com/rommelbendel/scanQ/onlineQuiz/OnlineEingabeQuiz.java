package com.rommelbendel.scanQ.onlineQuiz;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.rommelbendel.scanQ.QuizSettings;
import com.rommelbendel.scanQ.R;
import com.rommelbendel.scanQ.Vokabel;
import com.yuvraj.livesmashbar.enums.GravityView;
import com.yuvraj.livesmashbar.view.LiveSmashBar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import static androidx.lifecycle.Lifecycle.State.STARTED;

public class OnlineEingabeQuiz extends AppCompatActivity {
    private TextView questionView;
    private TextInputEditText solutionView;
    private MaterialButton buttonCheck;
    private CardView dialogRight;
    private CardView dialogWrong;
    private TextView solutionRight;
    private TextView solutionWrong;
    private ImageButton buttonPreviousQuestion;
    private ImageButton buttonNextQuestion;
    private TextView pointsView, leftText;
    private CardView questionCard, resultCard, pointsCV;
    private ConstraintLayout waitingEnd, playerLeft, waiting;
    private TextView resultEvaluation;
    private MaterialButton finishButton, newGame;
    private LottieAnimationView countdown, resultAmnim;
    private TableLayout resultTable;
    private BottomNavigationView bottomBar;

    private ArrayList<Vokabel> vokabels = new ArrayList<>();

    private int language_from_to;
    private int pointsCounter = 0;
    private int questionPointer = 0;
    private boolean intentBool = true;
    private boolean notFinished = true;
    private boolean isEmpty = false;
    private boolean isAdmin = false;
  //  private List<List<String>> questions;
    private Bundle answers = new Bundle();
    private Timer activeTimer = new Timer();

    private String id;
    private ArrayList<String> player;
    private String enterCode, username;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        Bundle extras = getIntent().getExtras();

        if (extras != null) {
            id = extras.getString("id");
            player = extras.getStringArrayList("player");
            enterCode = extras.getString("enter_code");
            username = extras.getString("playerName");
            isAdmin = extras.getBoolean("admin");
        } else
            finish();

        DatabaseReference onlineQuizRef = database.getReference(enterCode);

        onlineQuizRef.child("Language").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                language_from_to = Integer.parseInt(snapshot.getValue(Long.class).toString());

                if (language_from_to == QuizSettings.DE_TO_ENG)
                    setContentView(R.layout.activity_quiz_eng_deu);
                else
                    setContentView(R.layout.activity_quiz_deu_eng);

                CardView offlineResultCard = findViewById(R.id.resultCard);
                offlineResultCard.setVisibility(View.GONE);

                waiting = findViewById(R.id.waiting);
                questionCard = findViewById(R.id.questionCard1);
                questionView = findViewById(R.id.voc);
                solutionView = findViewById(R.id.loesung);
                buttonCheck = findViewById(R.id.check);
                dialogRight = findViewById(R.id.richtig);
                dialogWrong = findViewById(R.id.falsch);
                solutionRight = findViewById(R.id.solution_text_right);
                solutionWrong = findViewById(R.id.solution_text_wrong);
                buttonPreviousQuestion = findViewById(R.id.quizleft);
                buttonNextQuestion = findViewById(R.id.quizright);
                pointsView = findViewById(R.id.points);
                waitingEnd = findViewById(R.id.waiting_end);
                resultCard = findViewById(R.id.resultCardOnline);
                resultTable = findViewById(R.id.points_table);
                resultEvaluation = findViewById(R.id.succsess);
                finishButton = findViewById(R.id.finish_online_game);
                newGame = findViewById(R.id.new_online_game);
                resultAmnim = findViewById(R.id.result_anim);
                playerLeft = findViewById(R.id.player_left);
                leftText = findViewById(R.id.player_left_text);
                countdown = findViewById(R.id.left_countdown);
                bottomBar = findViewById(R.id.bottombar2);
                pointsCV = findViewById(R.id.points_cv);

                dialogRight.setVisibility(View.GONE);
                dialogWrong.setVisibility(View.GONE);
                resultCard.setVisibility(View.GONE);

                activeTimer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        HashMap<String, Object> activeMap = new HashMap<>();
                        activeMap.put(String.valueOf(id), ServerValue.TIMESTAMP);

                        if (OnlineEingabeQuiz.this.getLifecycle().getCurrentState().isAtLeast(STARTED))
                            onlineQuizRef.child("active").updateChildren(activeMap);
                    }
                }, 0, 5000);

                new Timer().scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        if(notFinished) {
                            if (isEmpty) {
                                questionCard.setVisibility(View.GONE);
                                dialogRight.setVisibility(View.GONE);
                                dialogWrong.setVisibility(View.GONE);
                                buttonCheck.setVisibility(View.GONE);
                                resultCard.setVisibility(View.GONE);

                                playerLeft.setVisibility(View.VISIBLE);
                                if (!isNetworkAvailable())
                                    leftText.setText("Deine Internetverbindung ist abgebrochen");
                                if (!countdown.isAnimating())
                                    countdown.playAnimation();

                                new Handler().postDelayed(OnlineEingabeQuiz.this::finish, 3000);
                            }
                        }
                    }
                }, 0, 1500);

                try {
                    if (notFinished) {
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
                                if (notFinished) {
                                    ArrayList<HashMap<String, String>> vokabelHashMap =
                                            (ArrayList<HashMap<String, String>>) snapshot.getValue();
                                    assert vokabelHashMap != null;
                                    for (HashMap<String, String> vocMap : vokabelHashMap) {
                                        vokabels.add(new Vokabel(
                                                vocMap.get("vokabelENG"),
                                                vocMap.get("vokabelDE"),
                                                vocMap.get("kategorie")
                                        ));
                                    }

                                    Collections.shuffle(vokabels);
                                    loadQuestion(onlineQuizRef);
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {}
                        });
                    }
                } catch (Exception exception) {
                    new LiveSmashBar.Builder(OnlineEingabeQuiz.this)
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

                buttonPreviousQuestion.setOnClickListener(v -> previousQuestion(onlineQuizRef));
                buttonNextQuestion.setOnClickListener(v -> nextQuestion(onlineQuizRef));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void previousQuestion(DatabaseReference reference) {
        try {
            if (questionPointer > 0) {
                questionPointer --;
            } else {
                questionPointer = vokabels.size() - 1;
            }
            loadQuestion(reference);
        } catch (NullPointerException exception) {
            AlertDialog.Builder warnung = new AlertDialog.Builder(OnlineEingabeQuiz.this);
            warnung.setTitle("Vokabeln konnten nicht geladen werden.");
            warnung.setMessage("Es sind nicht genug Vokabeln vorhanden.");
            warnung.setPositiveButton("OK", (dialog, which) -> {
                dialog.cancel();
                finish();
            });
            warnung.show();
        }
    }

    private void nextQuestion(DatabaseReference reference) {
        try {
            if (questionPointer == vokabels.size() - 1) {
                questionPointer = 0;
            } else {
                questionPointer ++;
            }
            loadQuestion(reference);
        } catch (NullPointerException exception) {
            AlertDialog.Builder warnung = new AlertDialog.Builder(OnlineEingabeQuiz.this);
            warnung.setTitle("Vokabeln konnten nicht geladen werden.");
            warnung.setMessage("Es sind nicht genug Vokabeln vorhanden.");
            warnung.setPositiveButton("OK", (dialog, which) -> {
                dialog.cancel();
                finish();
            });
            warnung.show();
        }
    }

    private void loadQuestion(DatabaseReference reference) {
        dialogRight.setVisibility(View.INVISIBLE);
        dialogWrong.setVisibility(View.INVISIBLE);

        final Vokabel questionVocab = vokabels.get(questionPointer);

        if (language_from_to == QuizSettings.DE_TO_ENG) {
            questionView.setText(questionVocab.getVokabelDE());
        } else {
            questionView.setText(questionVocab.getVokabelENG());
        }

        if (answers.containsKey(String.valueOf(questionPointer))) {
            String givenAnswer = answers.getString(String.valueOf(questionPointer));
            solutionView.setText(givenAnswer);
            solutionView.setEnabled(false);
            buttonCheck.setEnabled(false);
            buttonCheck.setOnClickListener(null);

            if (language_from_to == QuizSettings.DE_TO_ENG) {
                if (givenAnswer.equals(questionVocab.getVokabelENG())) {
                    dialogRight.setVisibility(View.VISIBLE);
                    solutionRight.setText(String.format("%s = %s", questionVocab.getVokabelDE(),
                            questionVocab.getVokabelENG()));
                } else {
                    dialogWrong.setVisibility(View.VISIBLE);
                    solutionWrong.setText(String.format("%s = %s", questionVocab.getVokabelDE(),
                            questionVocab.getVokabelENG()));
                }
            } else {
                if (givenAnswer.equals(questionVocab.getVokabelDE())) {
                    dialogRight.setVisibility(View.VISIBLE);
                    solutionRight.setText(String.format("%s = %s", questionVocab.getVokabelENG(),
                            questionVocab.getVokabelDE()));
                } else {
                    dialogWrong.setVisibility(View.VISIBLE);
                    solutionWrong.setText(String.format("%s = %s", questionVocab.getVokabelENG(),
                            questionVocab.getVokabelDE()));
                }
            }

        } else {
            solutionView.setText("");
            solutionView.setEnabled(true);
            buttonCheck.setEnabled(true);

            buttonCheck.setOnClickListener(v -> checkAnswer(reference));
        }
    }

    public void checkAnswer(DatabaseReference reference) {
        final Vokabel questionVocab = vokabels.get(questionPointer);
        final String givenAnswer = Objects.requireNonNull(solutionView.getText()).toString().trim();

        answers.putString(String.valueOf(questionPointer), givenAnswer);
        buttonCheck.setEnabled(false);
        buttonCheck.setOnClickListener(null);
        solutionView.setEnabled(false);

        if (language_from_to == QuizSettings.DE_TO_ENG) {
            if (givenAnswer.equals(questionVocab.getVokabelENG())) {
                givePoint();
                dialogRight.setVisibility(View.VISIBLE);
                solutionRight.setText(String.format("%s = %s", questionVocab.getVokabelDE(),
                        questionVocab.getVokabelENG()));
            } else {
                dialogWrong.setVisibility(View.VISIBLE);
                solutionWrong.setText(String.format("%s = %s", questionVocab.getVokabelDE(),
                        questionVocab.getVokabelENG()));
            }
        } else {
            if (givenAnswer.equals(questionVocab.getVokabelDE())) {
                givePoint();
                dialogRight.setVisibility(View.VISIBLE);
                solutionRight.setText(String.format("%s = %s", questionVocab.getVokabelENG(),
                        questionVocab.getVokabelDE()));
            } else {
                dialogWrong.setVisibility(View.VISIBLE);
                solutionWrong.setText(String.format("%s = %s", questionVocab.getVokabelENG(),
                        questionVocab.getVokabelDE()));
            }
        }
        checkIfSolved(reference);
    }

    private void checkIfSolved(DatabaseReference reference) {
        if (answers.size() == vokabels.size()) {
            reference.child("points").child(id).setValue(pointsCounter);

            buttonPreviousQuestion.setOnClickListener(null);
            buttonNextQuestion.setOnClickListener(null);

            pointsCV.setVisibility(View.GONE);
            bottomBar.setVisibility(View.GONE);
            questionCard.setVisibility(View.GONE);
            waitingEnd.setVisibility(View.VISIBLE);

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
                                            resultCard.setVisibility(View.VISIBLE);

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

                                                    Intent intentRecreate = new Intent(OnlineEingabeQuiz.this, OnlineRecreateNewGame.class);
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
                                            resultCard.setVisibility(View.VISIBLE);

                                            if (Collections.max(points) == pointsCounter) {
                                                if (draw) {
                                                    resultEvaluation.setText(R.string.unentschieden);
                                                    resultEvaluation.setTextSize(30f);
                                                    resultAmnim.setAnimation(R.raw.draw_handshake);
                                                    resultAmnim.setRepeatCount(1);
                                                } else {
                                                    resultEvaluation.setText(R.string.gratualtion);
                                                    resultAmnim.setAnimation(R.raw.winner);
                                                    resultAmnim.setRepeatCount(2);
                                                }
                                            } else {
                                                resultEvaluation.setText(R.string.schade);
                                                resultAmnim.setAnimation(R.raw.looser);
                                                resultAmnim.setRepeatCount(5);
                                            }

                                            new Handler().postDelayed(() -> resultAmnim.playAnimation(), 500);
                                            finishButton.setOnClickListener(v -> finish());
                                        } catch (Exception exception) {
                                            new LiveSmashBar.Builder(OnlineEingabeQuiz.this)
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
                                    resultCard.setVisibility(View.GONE);
                                    waitingEnd.setVisibility(View.VISIBLE);
                                }
                            }
                        }
                    } catch(Exception ignored) {
                        HashMap<String, Boolean> done = (HashMap<String, Boolean>) snapshot.getValue();

                        if (done != null) {
                            if (!done.containsValue(false)) {
                                reference.child("points").get().addOnCompleteListener(task -> {
                                    ArrayList<Long> points = (ArrayList<Long>) task.getResult().getValue();
                                    ArrayList<OnlinePlayer> onlinePlayers = new ArrayList<>();

                                    assert points != null;
                                    for (String p : player) {
                                        int index = player.indexOf(p);
                                        onlinePlayers.add(new OnlinePlayer(p, points.get(index)));
                                    }
                                    sortPoints(onlinePlayers);

                                    boolean draw = false;
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
                                        ((TextView) playerRow.getChildAt(4)).setText(String.format("%s zu", op.getPoints()));
                                    }

                                    waitingEnd.setVisibility(View.GONE);
                                    resultCard.setVisibility(View.VISIBLE);

                                    if (Collections.max(points) == pointsCounter) {
                                        if (draw) {
                                            resultEvaluation.setText(R.string.unentschieden);
                                            resultEvaluation.setTextSize(20f);
                                            resultAmnim.setAnimation(R.raw.draw_handshake);
                                            resultAmnim.setRepeatCount(1);
                                        } else {
                                            resultEvaluation.setText(R.string.gratualtion);
                                            resultAmnim.setAnimation(R.raw.winner);
                                            resultAmnim.setRepeatCount(2);
                                        }
                                    } else {
                                        resultEvaluation.setText(R.string.schade);
                                        resultAmnim.setAnimation(R.raw.looser);
                                        resultAmnim.setRepeatCount(5);
                                    }

                                    new Handler().postDelayed(() -> resultAmnim.playAnimation(), 500);
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
    }

    private void restartForNonAdmin(DatabaseReference reference) {
        newGame.setOnClickListener(v -> {
            if(isNetworkAvailable()) {
                waiting.setVisibility(View.VISIBLE);
                resultCard.setVisibility(View.GONE);

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

                                Intent intent = new Intent(OnlineEingabeQuiz.this, OnlineLobbyWaiting.class);
                                intent.putExtras(extras);
                                OnlineEingabeQuiz.this.startActivity(intent);
                                finish();

                            } else {
                                waiting.setVisibility(View.GONE);
                                resultCard.setVisibility(View.VISIBLE);

                                new LiveSmashBar.Builder(OnlineEingabeQuiz.this)
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
                            resultCard.setVisibility(View.VISIBLE);

                            new LiveSmashBar.Builder(OnlineEingabeQuiz.this)
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
                new LiveSmashBar.Builder(OnlineEingabeQuiz.this)
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

    public void givePoint() {
        pointsCounter ++;
        pointsView.setText(String.valueOf(pointsCounter));
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
