package com.rommelbendel.scanQ.onlineQuiz;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static androidx.lifecycle.Lifecycle.State.STARTED;

public class OnlineMultipleChoice extends AppCompatActivity {

    private TextView questionField;
    private MaterialButton buttonAnswer1;
    private MaterialButton buttonAnswer2;
    private MaterialButton buttonAnswer3;
    private MaterialButton buttonAnswer4;
    private ImageButton buttonPreviousQuestion;
    private ImageButton buttonNextQuestion;
    private TextView pointsCounterView;
    private CardView questionCard, resultCard, pointsCard;
    private ConstraintLayout waiting, waitingEnd, playerLeft;
    private TextView resultEvaluation, leftText;
    private TableLayout resultTable;
    private MaterialButton finishButton, newGame;
    private BottomNavigationView bottomNav;
    private LottieAnimationView countdown, resultAmnim;

    private ArrayList<Vokabel> vokabels = new ArrayList<>();

    private int language_from_to;
    private int pointsCounter = 0;
    private int questionPointer = 0;
    private int activeCounter = 0;
    private boolean notFinished = true;
    private boolean isAdmin = false;
    private boolean isEmpty = false;
    private boolean intentBool = true;
    private List<List<String>> questions;
    private Bundle answers = new Bundle();
    private Timer activeTimer = new Timer();

    private Vibrator vibrate;
    private MediaPlayer loss;
    private MediaPlayer win;
    private MediaPlayer rightSound;
    private LottieAnimationView animB1, animB2, animB3, animB4;

    private String id;
    private ArrayList<String> player;
    private String enterCode, username;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz_multiple_choice);
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        Bundle extras = getIntent().getExtras();

        if (extras != null) {
            id = extras.getString("id");
            player = extras.getStringArrayList("player");
            enterCode = extras.getString("enter_code");
            username = extras.getString("playerName");
            isAdmin = extras.getBoolean("admin");
        } else {
            finish();
            Log.e("extras", "extras are null");
        }
        DatabaseReference onlineQuizRef = database.getReference(enterCode);

        onlineQuizRef.child("Language").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                language_from_to = Integer.parseInt(Objects.requireNonNull(snapshot.getValue(Long.class)).toString());

                waiting = findViewById(R.id.waiting);
                waitingEnd = findViewById(R.id.waiting_end);
                questionField = findViewById(R.id.voc);
                questionCard = findViewById(R.id.questionCard1);
                buttonAnswer1 = findViewById(R.id.AnswerButton_1);
                buttonAnswer2 = findViewById(R.id.AnswerButton_2);
                buttonAnswer3 = findViewById(R.id.AnswerButton_3);
                buttonAnswer4 = findViewById(R.id.AnswerButton_4);
                buttonPreviousQuestion = findViewById(R.id.quizleft);
                buttonNextQuestion = findViewById(R.id.quizright);
                pointsCounterView = findViewById(R.id.points);
                bottomNav = findViewById(R.id.bottombar);
                pointsCard = findViewById(R.id.pointscard);
                playerLeft = findViewById(R.id.player_left);
                leftText = findViewById(R.id.player_left_text);
                countdown = findViewById(R.id.left_countdown);
                resultCard = findViewById(R.id.resultCardOnline);
                resultTable = findViewById(R.id.points_table);
                resultEvaluation = findViewById(R.id.succsess);
                resultAmnim = findViewById(R.id.result_anim);
                newGame = findViewById(R.id.new_online_game);
                finishButton = findViewById(R.id.finish_online_game);
                resultCard.setVisibility(View.GONE);

                vibrate = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                loss = MediaPlayer.create(OnlineMultipleChoice.this, R.raw.niederlage);
                win = MediaPlayer.create(OnlineMultipleChoice.this, R.raw.sieg);
                rightSound = MediaPlayer.create(OnlineMultipleChoice.this, R.raw.richtig);

                animB1 = findViewById(R.id.button_anim1);
                animB2 = findViewById(R.id.button_anim2);
                animB3 = findViewById(R.id.button_anim3);
                animB4 = findViewById(R.id.button_anim4);

                activeTimer.scheduleAtFixedRate(new TimerTask() {
                      @Override
                      public void run() {
                          HashMap<String, Object> activeMap = new HashMap<>();
                          activeMap.put(String.valueOf(id), ServerValue.TIMESTAMP);

                          if (OnlineMultipleChoice.this.getLifecycle().getCurrentState().isAtLeast(STARTED))
                              onlineQuizRef.child("active").updateChildren(activeMap);
                      }
                  }, 0, 5000);

                new Timer().scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        if (notFinished) {
                            if (isEmpty) {
                                questionCard.setVisibility(View.GONE);
                                buttonAnswer1.setVisibility(View.GONE);
                                buttonAnswer2.setVisibility(View.GONE);
                                buttonAnswer3.setVisibility(View.GONE);
                                buttonAnswer4.setVisibility(View.GONE);
                                bottomNav.setVisibility(View.GONE);
                                pointsCard.setVisibility(View.GONE);
                                resultCard.setVisibility(View.GONE);

                                playerLeft.setVisibility(View.VISIBLE);
                                if (!isNetworkAvailable())
                                    leftText.setText("Deine Internetverbindung ist abgebrochen");
                                if (!countdown.isAnimating())
                                    countdown.playAnimation();

                                new Handler().postDelayed(() -> finish(), 3000);
                            }
                        }
                    }
                },0, 1500);

                try {
                    if(notFinished) {
                        onlineQuizRef.child("empty").addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                if(notFinished)
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

                    if(notFinished) {
                        onlineQuizRef.child("vocs").addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                if(notFinished) {
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

                                    generateQuestions(onlineQuizRef);
                                    buttonPreviousQuestion.setOnClickListener(v -> previousQuestion(onlineQuizRef));
                                    buttonNextQuestion.setOnClickListener(v -> nextQuestion(onlineQuizRef));
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {}
                        });
                    }
                } catch (Exception exception) {
                    new LiveSmashBar.Builder(OnlineMultipleChoice.this)
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

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    public void generateQuestions(DatabaseReference reference) {
        questions = new ArrayList<>();

        for (int v = 0; v < vokabels.size(); v++) {
            List<Vokabel> vocabs = new ArrayList<>(vokabels);
            vocabs.remove(vokabels.get(v));
            Collections.shuffle(vocabs);
            List<String> question = new ArrayList<>();
            List<String> answers = new ArrayList<>();

            if (language_from_to == QuizSettings.DE_TO_ENG) {
                question.add(vokabels.get(v).getVokabelDE());
                answers.add(vokabels.get(v).getVokabelENG());
                for (int i = 0; i < 3; i++) {
                    answers.add(vocabs.get(0).getVokabelENG());
                    Log.d("answers Englisch add", vocabs.get(0).getVokabelENG());
                    vocabs.remove(0);
                }
            } else {
                question.add(vokabels.get(v).getVokabelENG());
                answers.add(vokabels.get(v).getVokabelDE());
                for (int i = 0; i < 3; i++) {
                    answers.add(vocabs.get(0).getVokabelDE());
                    Log.d("answers german add", vocabs.get(0).getVokabelDE());
                    vocabs.remove(0);
                }
            }

            Collections.shuffle(answers);
            question.addAll(answers);

            questions.add(question);
            loadQuestion(reference);
        }
    }

    public void loadQuestion(DatabaseReference reference) {
        List<String> question = questions.get(questionPointer);

        animB1.setVisibility(GONE);
        animB2.setVisibility(GONE);
        animB3.setVisibility(GONE);
        animB4.setVisibility(GONE);

        questionField.setText(question.get(0));

        buttonAnswer1.setText(question.get(1));
        buttonAnswer2.setText(question.get(2));
        buttonAnswer3.setText(question.get(3));
        buttonAnswer4.setText(question.get(4));

        buttonAnswer1.setBackgroundColor(Color.parseColor("#219a95"));
        buttonAnswer2.setBackgroundColor(Color.parseColor("#219a95"));
        buttonAnswer3.setBackgroundColor(Color.parseColor("#219a95"));
        buttonAnswer4.setBackgroundColor(Color.parseColor("#219a95"));

        if (answers.containsKey(String.valueOf(questionPointer))) {
            buttonAnswer1.setOnClickListener(null);
            buttonAnswer2.setOnClickListener(null);
            buttonAnswer3.setOnClickListener(null);
            buttonAnswer4.setOnClickListener(null);

            buttonAnswer1.setBackgroundColor(Color.parseColor("#FFC1C3C3"));
            buttonAnswer2.setBackgroundColor(Color.parseColor("#FFC1C3C3"));
            buttonAnswer3.setBackgroundColor(Color.parseColor("#FFC1C3C3"));
            buttonAnswer4.setBackgroundColor(Color.parseColor("#FFC1C3C3"));

            int givenAnswer = answers.getInt(String.valueOf(questionPointer));

            MaterialButton answerButton;
            LottieAnimationView anim;
            switch (givenAnswer) {
                case 1:
                    answerButton = buttonAnswer1;
                    anim = animB1;
                    break;
                case 2:
                    answerButton = buttonAnswer2;
                    anim = animB2;
                    break;
                case 3:
                    answerButton = buttonAnswer3;
                    anim = animB3;
                    break;
                case 4:
                    answerButton = buttonAnswer4;
                    anim = animB4;
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + givenAnswer);
            }

            String answerText = question.get(givenAnswer);
            String correctAnswer;
            if (language_from_to == QuizSettings.DE_TO_ENG) {
                correctAnswer = vokabels.get(questionPointer).getVokabelENG();
            } else {
                correctAnswer = vokabels.get(questionPointer).getVokabelDE();
            }

            MaterialButton[] mbs = {buttonAnswer1, buttonAnswer2, buttonAnswer3, buttonAnswer4};
            if (answerText.equals(correctAnswer)) {
                anim.setVisibility(VISIBLE);
                anim.playAnimation();
                anim.setSpeed(2);
                anim.setMaxProgress(1f);

                answerButton.setBackgroundColor(Color.parseColor("#51ad4a"));
            } else {
                answerButton.setBackgroundColor(Color.parseColor("#842c1e"));
                for(Button mb : mbs) {
                    if (mb.getText().equals(correctAnswer))
                        mb.setBackgroundColor(Color.parseColor("#51ad4a"));
                }
            }

        } else {
            buttonAnswer1.setOnClickListener(v -> checkAnswer((MaterialButton) v, reference,1));
            buttonAnswer2.setOnClickListener(v -> checkAnswer((MaterialButton) v, reference,2));
            buttonAnswer3.setOnClickListener(v -> checkAnswer((MaterialButton) v, reference,3));
            buttonAnswer4.setOnClickListener(v -> checkAnswer((MaterialButton) v, reference,4));
        }

    }

    private void previousQuestion(DatabaseReference reference) {
        if (questionPointer > 0) {
            questionPointer --;
        } else {
            questionPointer = questions.size() - 1;
        }
        loadQuestion(reference);
    }

    private void nextQuestion(DatabaseReference reference) {
        if (questionPointer == questions.size() - 1) {
            questionPointer = 0;
        } else {
            questionPointer ++;
        }
        loadQuestion(reference);
    }

    private void checkAnswer(MaterialButton answerButton, DatabaseReference reference, int anim) {
        animB1.setVisibility(VISIBLE);
        animB2.setVisibility(VISIBLE);
        animB3.setVisibility(VISIBLE);
        animB4.setVisibility(VISIBLE);

        buttonAnswer1.setOnClickListener(null);
        buttonAnswer2.setOnClickListener(null);
        buttonAnswer3.setOnClickListener(null);
        buttonAnswer4.setOnClickListener(null);

        buttonAnswer1.setBackgroundColor(Color.parseColor("#FFC1C3C3"));
        buttonAnswer2.setBackgroundColor(Color.parseColor("#FFC1C3C3"));
        buttonAnswer3.setBackgroundColor(Color.parseColor("#FFC1C3C3"));
        buttonAnswer4.setBackgroundColor(Color.parseColor("#FFC1C3C3"));

        MaterialButton[] mbs = {buttonAnswer1, buttonAnswer2, buttonAnswer3, buttonAnswer4};
        List<LottieAnimationView> anims = Arrays.asList(animB1, animB2, animB3, animB4);

        String answerText = (String) answerButton.getText();
        Vokabel quizVocab = vokabels.get(questionPointer);

        String correctAnswer;
        if (language_from_to == QuizSettings.DE_TO_ENG) {
            correctAnswer = quizVocab.getVokabelENG();
        } else {
            correctAnswer = quizVocab.getVokabelDE();
        }

        if (answerText.equals(correctAnswer)) {
            if(rightSound.isPlaying())
                rightSound.stop();
            rightSound.start();

            if(anim > 0 && anim < 5) {
                LottieAnimationView animB = anims.get(anim-1);
                animB.playAnimation();
                animB.setSpeed(2);
                animB.setMaxProgress(1f);

                for(LottieAnimationView notAnim : anims) {
                    if(anims.indexOf(notAnim) != anim-1) {
                        notAnim.setProgress(0f);
                        notAnim.cancelAnimation();
                    }
                }
            } else
                Toast.makeText(this, "unexpected Value", Toast.LENGTH_SHORT).show();

            givePoint();
            answerButton.setBackgroundColor(Color.parseColor("#51ad4a"));
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrate.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrate.vibrate(500);
            }

            animB1.setVisibility(GONE);
            animB2.setVisibility(GONE);
            animB3.setVisibility(GONE);
            animB4.setVisibility(GONE);

            answerButton.setBackgroundColor(Color.parseColor("#842c1e"));

            for(Button mb : mbs) {
                if (mb.getText().equals(correctAnswer))
                    mb.setBackgroundColor(Color.parseColor("#51ad4a"));
            }
        }

        int answerButtonNum;
        if (answerButton.equals(buttonAnswer1)) {
            answerButtonNum = 1;
        } else if (answerButton.equals(buttonAnswer2)) {
            answerButtonNum = 2;
        } else if (answerButton.equals(buttonAnswer3)) {
            answerButtonNum = 3;
        } else {
            answerButtonNum = 4;
        }

        answers.putInt(String.valueOf(questionPointer), answerButtonNum);
        checkIfSolved(reference);
    }

    private void checkIfSolved(DatabaseReference reference) {
        if (answers.size() == questions.size()) {
            reference.child("points").child(id).setValue(pointsCounter);

            buttonPreviousQuestion.setOnClickListener(null);
            buttonNextQuestion.setOnClickListener(null);

            questionCard.setVisibility(View.GONE);
            buttonAnswer1.setVisibility(View.GONE);
            buttonAnswer2.setVisibility(View.GONE);
            buttonAnswer3.setVisibility(View.GONE);
            buttonAnswer4.setVisibility(View.GONE);
            bottomNav.setVisibility(View.GONE);
            pointsCard.setVisibility(View.GONE);
            animB1.setVisibility(GONE);
            animB2.setVisibility(GONE);
            animB3.setVisibility(GONE);
            animB4.setVisibility(GONE);

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

                                                    Intent intentRecreate = new Intent(OnlineMultipleChoice.this, OnlineRecreateNewGame.class);
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
                                                win.start();
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
                                                loss.start();

                                                resultEvaluation.setText(R.string.schade);
                                                resultAmnim.setAnimation(R.raw.looser);
                                                resultAmnim.setRepeatCount(5);
                                            }

                                            new Handler().postDelayed(() -> resultAmnim.playAnimation(), 500);
                                            finishButton.setOnClickListener(v -> finish());
                                        } catch (Exception exception) {
                                            new LiveSmashBar.Builder(OnlineMultipleChoice.this)
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
                                    for (String p : player) {
                                        int index = player.indexOf(p);

                                        onlinePlayers.add(new OnlinePlayer(p, points.get(index)));
                                    }
                                    onlinePlayers = sortPoints(onlinePlayers);

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
                                        ((TextView) playerRow.getChildAt(0)).setText(String.format(" %s.. ", rankPoints.indexOf(op.getPoints())+1));
                                        ((TextView) playerRow.getChildAt(2)).setText(op.getPlayerName());
                                        ((TextView) playerRow.getChildAt(4)).setText(String.format("%s Punkte", op.getPoints()));
                                    }

                                    waitingEnd.setVisibility(View.GONE);
                                    resultCard.setVisibility(View.VISIBLE);

                                    if (Collections.max(points) == pointsCounter) {
                                        if(draw) {
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

                                Intent intent = new Intent(OnlineMultipleChoice.this, OnlineLobbyWaiting.class);
                                intent.putExtras(extras);
                                OnlineMultipleChoice.this.startActivity(intent);
                                finish();

                            } else {
                                waiting.setVisibility(View.GONE);
                                resultCard.setVisibility(View.VISIBLE);

                                new LiveSmashBar.Builder(OnlineMultipleChoice.this)
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

                            new LiveSmashBar.Builder(OnlineMultipleChoice.this)
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
                new LiveSmashBar.Builder(OnlineMultipleChoice.this)
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

    private void givePoint() {
        pointsCounter ++;
        pointsCounterView.setText(String.valueOf(pointsCounter));
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
