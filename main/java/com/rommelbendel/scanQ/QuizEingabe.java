package com.rommelbendel.scanQ;

import android.app.AlertDialog;
import android.content.Context;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.rommelbendel.scanQ.additional.TinyDB;

import org.joda.time.LocalDate;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class QuizEingabe extends AppCompatActivity {

    private String categoryName;
    private int vocabNum;
    private boolean onlyUntrained;
    private int language_from_to;

    private VokabelViewModel vokabelViewModel;
    private WeekdayViewModel weekdayViewModel;
    private LiveData<List<Vokabel>> quizVocabsLiveData;
    private List<Vokabel> quizVocabs;

    private TextView questionView;
    private TextInputEditText solutionView;
    private MaterialButton buttonCheck;
    private CardView dialogRight;
    private CardView dialogWrong;
    private TextView solutionRight;
    private TextView solutionWrong;
    private ImageButton buttonPreviousQuestion;
    private ImageButton buttonNextQuestion;
    private TextView pointsView;
    private CardView questionCard, resultCard;
    private TextView resultCountRight, resultCountWrong, resultEvaluation;
    private MaterialButton restartButton;
    private LottieAnimationView animQuiz;

    private Vibrator vibrate;
    private MediaPlayer loss;
    private MediaPlayer win;
    private MediaPlayer rightSound;

    private int questionPointer = 0;
    private int pointsCounter = 0;
    private Bundle answers = new Bundle();


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();

        if (extras != null) {
            vocabNum = extras.getInt(QuizSettings.ID_EXTRA_VOCAB_NUM);
            categoryName = extras.getString(QuizSettings.ID_EXTRA_CATEGORY);
            onlyUntrained = extras.getBoolean(QuizSettings.ID_EXTRA_ONLY_NEW);
            language_from_to = extras.getInt(QuizSettings.ID_EXTRA_FROM_TO);
        } else {
            finish();
        }

        if (language_from_to == QuizSettings.DE_TO_ENG) {
            setContentView(R.layout.activity_quiz_eng_deu);
        } else {
            setContentView(R.layout.activity_quiz_deu_eng);
        }

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
        resultCard = findViewById(R.id.resultCard);
        resultCountRight = findViewById(R.id.countRight);
        resultCountWrong = findViewById(R.id.countWrong);
        restartButton = findViewById(R.id.restart1);
        resultEvaluation = findViewById(R.id.succsessOrLoss);
        animQuiz = findViewById(R.id.quiz_anim);

        vibrate = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        loss = MediaPlayer.create(this, R.raw.niederlage);
        win = MediaPlayer.create(this, R.raw.sieg);
        rightSound = MediaPlayer.create(this, R.raw.richtig);

        dialogRight.setVisibility(View.GONE);
        dialogWrong.setVisibility(View.GONE);
        resultCard.setVisibility(View.GONE);

        vokabelViewModel = new ViewModelProvider(this).get(VokabelViewModel.class);
        weekdayViewModel = new ViewModelProvider(this).get(WeekdayViewModel.class);

        if (onlyUntrained) {
            quizVocabsLiveData = vokabelViewModel.getUntrainedCategoryVocabs(categoryName);
        } else {
            quizVocabsLiveData = vokabelViewModel.getCategoryVocabs(categoryName);
        }

        quizVocabsLiveData.observe(this, vokabeln -> {
            if (vokabeln != null) {
                if (vokabeln.size() != 0) {
                    quizVocabs = vokabeln;
                    Collections.shuffle(quizVocabs);
                    if (quizVocabs.size() > vocabNum) {
                        QuizEingabe.this.quizVocabs = quizVocabs.subList(0, vocabNum);
                    }
                    loadQuestion();
                } else {
                    AlertDialog.Builder warnung = new AlertDialog.Builder(
                            QuizEingabe.this);
                    warnung.setTitle("Quiz kann nicht gestartet werden");
                    warnung.setMessage("Es sind keine Vokabeln vorhanden.");
                    warnung.setPositiveButton("OK", (dialog, which) -> {
                        dialog.cancel();
                        finish();
                    });
                    warnung.show();
                }
                quizVocabsLiveData.removeObservers(QuizEingabe.this);
            }
        });

        solutionView.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                buttonCheck.setEnabled(s.toString().trim().length() != 0);
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void afterTextChanged(Editable s) {}
        });

        buttonPreviousQuestion.setOnClickListener(v -> previousQuestion());
        buttonNextQuestion.setOnClickListener(v -> nextQuestion());
    }

    private void previousQuestion() {
        try {
            if (questionPointer > 0) {
                questionPointer --;
            } else {
                questionPointer = quizVocabs.size() - 1;
            }
            loadQuestion();
        } catch (NullPointerException exception) {
            AlertDialog.Builder warnung = new AlertDialog.Builder(
                    QuizEingabe.this);
            warnung.setTitle("Vokabeln konnten nicht geladen werden.");
            warnung.setMessage("Es sind nicht genug Vokabeln vorhanden.");
            warnung.setPositiveButton("OK", (dialog, which) -> {
                dialog.cancel();
                finish();
            });
            warnung.show();
        }
    }

    private void nextQuestion() {
        try {
            if (questionPointer == quizVocabs.size() - 1) {
                questionPointer = 0;
            } else {
                questionPointer ++;
            }
            loadQuestion();
        } catch (NullPointerException exception) {
            AlertDialog.Builder warnung = new AlertDialog.Builder(
                    QuizEingabe.this);
            warnung.setTitle("Vokabeln konnten nicht geladen werden.");
            warnung.setMessage("Es sind nicht genug Vokabeln vorhanden.");
            warnung.setPositiveButton("OK", (dialog, which) -> {
                dialog.cancel();
                finish();
            });
            warnung.show();
        }
    }

    private void loadQuestion() {
        dialogRight.setVisibility(View.INVISIBLE);
        dialogWrong.setVisibility(View.INVISIBLE);
        animQuiz.setVisibility(GONE);

        final Vokabel questionVocab = quizVocabs.get(questionPointer);

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
                    dialogRight.setVisibility(VISIBLE);
                    solutionRight.setText(String.format("%s = %s", questionVocab.getVokabelDE(),
                            questionVocab.getVokabelENG()));
                } else {
                    dialogWrong.setVisibility(VISIBLE);
                    solutionWrong.setText(String.format("%s = %s", questionVocab.getVokabelDE(),
                            questionVocab.getVokabelENG()));
                }
            } else {
                if (givenAnswer.equals(questionVocab.getVokabelDE())) {
                    dialogRight.setVisibility(VISIBLE);
                    solutionRight.setText(String.format("%s = %s", questionVocab.getVokabelENG(),
                            questionVocab.getVokabelDE()));
                } else {
                    dialogWrong.setVisibility(VISIBLE);
                    solutionWrong.setText(String.format("%s = %s", questionVocab.getVokabelENG(),
                            questionVocab.getVokabelDE()));
                }
            }

        } else {
            solutionView.setText("");
            solutionView.setEnabled(true);
            buttonCheck.setEnabled(false);

            solutionView.addTextChangedListener(new TextWatcher() {
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    buttonCheck.setEnabled(s.toString().trim().length() != 0);
                }

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void afterTextChanged(Editable s) {}
            });


            buttonCheck.setOnClickListener(v -> checkAnswer());
        }
    }

    public void checkAnswer() {
        final TinyDB tb = new TinyDB(getApplicationContext());
        final int dayOfWeek = LocalDate.now().getDayOfWeek();
        weekdayViewModel.incrementNumberOfTrainedVocabs(dayOfWeek);

        final Vokabel questionVocab = quizVocabs.get(questionPointer);
        final String givenAnswer = Objects.requireNonNull(solutionView.getText()).toString().trim();

        vokabelViewModel.updateAnswered(questionVocab.getId(),
                questionVocab.getAnswered() + 1);

        answers.putString(String.valueOf(questionPointer), givenAnswer);
        buttonCheck.setEnabled(false);
        buttonCheck.setOnClickListener(null);
        solutionView.setEnabled(false);
        animQuiz.setVisibility(VISIBLE);

        if (language_from_to == QuizSettings.DE_TO_ENG) {
            if (givenAnswer.equals(questionVocab.getVokabelENG())) {
                givePoint();
                animQuiz.playAnimation();
                animQuiz.setSpeed(1);
                animQuiz.setMaxProgress(1f);

                if(tb.getBoolean("quiz_sounds")) {
                    rightSound.start();
                    rightSound.setVolume(0.5f, 0.5f);
                }

                dialogRight.setVisibility(VISIBLE);
                solutionRight.setText(String.format("%s = %s", questionVocab.getVokabelDE(),
                        questionVocab.getVokabelENG()));
                vokabelViewModel.updateCountRightAnswers(questionVocab.getId(),
                        questionVocab.getRichtig() + 1);

                weekdayViewModel.incrementNumberOfRightAnswers(dayOfWeek);
            } else {
                dialogWrong.setVisibility(VISIBLE);
                animQuiz.setVisibility(GONE);

                if(tb.getBoolean("quiz_sounds")) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrate.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
                    } else {
                        vibrate.vibrate(500);
                    }
                }

                solutionWrong.setText(String.format("%s = %s", questionVocab.getVokabelDE(),
                        questionVocab.getVokabelENG()));
                vokabelViewModel.updateCountWrongAnswers(questionVocab.getId(),
                        questionVocab.getFalsch() + 1);

                weekdayViewModel.incrementNumberOfWrongAnswers(dayOfWeek);
            }
        } else {
            if (givenAnswer.equals(questionVocab.getVokabelDE())) {
                givePoint();
                animQuiz.playAnimation();
                animQuiz.setSpeed(1);
                animQuiz.setMaxProgress(1f);

                if(tb.getBoolean("quiz_sounds")) {
                    rightSound.start();
                    rightSound.setVolume(0.5f, 0.5f);
                }

                dialogRight.setVisibility(VISIBLE);
                solutionRight.setText(String.format("%s = %s", questionVocab.getVokabelENG(),
                        questionVocab.getVokabelDE()));
                vokabelViewModel.updateCountRightAnswers(questionVocab.getId(),
                        questionVocab.getRichtig() + 1);

                weekdayViewModel.incrementNumberOfWrongAnswers(dayOfWeek);
            } else {
                dialogWrong.setVisibility(VISIBLE);
                animQuiz.setVisibility(GONE);

                if(tb.getBoolean("quiz_sounds")) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrate.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
                    } else {
                        vibrate.vibrate(500);
                    }
                }

                solutionWrong.setText(String.format("%s = %s", questionVocab.getVokabelENG(),
                        questionVocab.getVokabelDE()));
                vokabelViewModel.updateCountWrongAnswers(questionVocab.getId(),
                        questionVocab.getFalsch() + 1);

                weekdayViewModel.incrementNumberOfRightAnswers(dayOfWeek);
            }
        }
        checkIfSolved();
    }

    private void checkIfSolved() {
        final TinyDB tb = new TinyDB(getApplicationContext());

        if (answers.size() == quizVocabs.size()) {
            final Handler handler = new Handler();
            handler.postDelayed(() -> {
                buttonPreviousQuestion.setOnClickListener(null);
                buttonNextQuestion.setOnClickListener(null);

                resultCard.setVisibility(VISIBLE);
                questionCard.setVisibility(View.GONE);
                animQuiz.setVisibility(View.GONE);

                resultCountRight.setText(String.format("%s Fragen richtig", pointsCounter));
                resultCountWrong.setText(String.format("%s Fragen falsch",
                        (quizVocabs.size() - pointsCounter)));

                if ((quizVocabs.size() - pointsCounter) < Math.ceil(quizVocabs.size() / 2f)) {
                    resultEvaluation.setText(R.string.gratualtion);
                    if(tb.getBoolean("quiz_sounds")) {
                        win.start();
                        win.setVolume(0.4f, 0.4f);
                    }
                } else {
                    resultEvaluation.setText(R.string.schade);
                    if(tb.getBoolean("quiz_sounds")) {
                        loss.start();
                        loss.setVolume(0.5f, 0.5f);
                    }
                }

                restartButton.setOnClickListener(v -> finish());
            }, 700);
        }
    }

    public void givePoint() {
        pointsCounter ++;
        pointsView.setText(String.valueOf(pointsCounter));
    }
}
