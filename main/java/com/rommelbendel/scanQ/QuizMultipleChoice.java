package com.rommelbendel.scanQ;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.button.MaterialButton;
import com.rommelbendel.scanQ.additional.TinyDB;

import org.joda.time.LocalDate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class QuizMultipleChoice extends AppCompatActivity {

    private TextView questionField;
    private MaterialButton buttonAnswer1;
    private MaterialButton buttonAnswer2;
    private MaterialButton buttonAnswer3;
    private MaterialButton buttonAnswer4;
    private ImageButton buttonPreviousQuestion;
    private ImageButton buttonNextQuestion;
    private TextView pointsCounterView;
    private CardView questionCard;
    private ConstraintLayout resultCard;
    private TextView resultCountRight, resultCountWrong, resultEvaluation;
    private MaterialButton restartButton;
    private LottieAnimationView animB1, animB2, animB3, animB4;

    private VokabelViewModel vokabelViewModel;
    private WeekdayViewModel weekdayViewModel;
    private LiveData<List<Vokabel>> quizVocabsLiveData;
    private List<Vokabel> quizVocabs;
    private LiveData<List<Vokabel>> allVocabsLiveData;

    private String categoryName;
    private int vocabNum;
    private boolean onlyUntrained;
    private int language_from_to;

    private int pointsCounter = 0;
    private int questionPointer = 0;
    private List<List<String>> questions;
    private Bundle answers = new Bundle();

    private Vibrator vibrate;
    private MediaPlayer loss;
    private MediaPlayer win;
    private MediaPlayer rightSound;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz_multiple_choice);

        ConstraintLayout onlineResultCard = findViewById(R.id.online_result_card);
        onlineResultCard.setVisibility(GONE);

        vibrate = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        loss = MediaPlayer.create(this, R.raw.niederlage);
        win = MediaPlayer.create(this, R.raw.sieg);
        rightSound = MediaPlayer.create(this, R.raw.richtig);


        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            vocabNum = extras.getInt(QuizSettings.ID_EXTRA_VOCAB_NUM);
            categoryName = extras.getString(QuizSettings.ID_EXTRA_CATEGORY);
            onlyUntrained = extras.getBoolean(QuizSettings.ID_EXTRA_ONLY_NEW);
            language_from_to = extras.getInt(QuizSettings.ID_EXTRA_FROM_TO);
        } else {
            finish();
        }

        if (vocabNum < 4) {
            resultCard = findViewById(R.id.resultCard1);
            resultCard.setVisibility(GONE);

            AlertDialog.Builder warnung = new AlertDialog.Builder(
                    QuizMultipleChoice.this);
            warnung.setTitle("Quiz kann nicht gestartet werden");
            warnung.setMessage("Bitte wähle mindestens 4 Vokabeln aus.");
            warnung.setPositiveButton("OK", (dialog, which) -> {
                dialog.cancel();
                finish();
            });
            warnung.show();
        } else {
            questionField = findViewById(R.id.voc);
            questionCard = findViewById(R.id.questionCard1);
            buttonAnswer1 = findViewById(R.id.AnswerButton_1);
            buttonAnswer2 = findViewById(R.id.AnswerButton_2);
            buttonAnswer3 = findViewById(R.id.AnswerButton_3);
            buttonAnswer4 = findViewById(R.id.AnswerButton_4);
            buttonPreviousQuestion = findViewById(R.id.quizleft);
            buttonNextQuestion = findViewById(R.id.quizright);
            pointsCounterView = findViewById(R.id.points);
            resultCard = findViewById(R.id.resultCard1);
            resultCountRight = findViewById(R.id.countRight);
            resultCountWrong = findViewById(R.id.countWrong);
            resultEvaluation = findViewById(R.id.succsessOrLoss);
            restartButton = findViewById(R.id.restart1);
            resultCard.setVisibility(GONE);
            animB1 = findViewById(R.id.button_anim1);
            animB2 = findViewById(R.id.button_anim2);
            animB3 = findViewById(R.id.button_anim3);
            animB4 = findViewById(R.id.button_anim4);

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
                        quizVocabs = quizVocabsLiveData.getValue();
                        assert quizVocabs != null;
                        Collections.shuffle(quizVocabs);
                        if (quizVocabs.size() > vocabNum) {
                            QuizMultipleChoice.this.quizVocabs = quizVocabs.subList(0, vocabNum);
                        }
                        generateQuestions();
                    } else {
                        AlertDialog.Builder warnung = new AlertDialog.Builder(
                                QuizMultipleChoice.this);
                        warnung.setTitle("Quiz kann nicht gestartet werden");
                        warnung.setMessage("Es sind keine Vokabeln vorhanden.");
                        warnung.setPositiveButton("OK", (dialog, which) -> {
                            dialog.cancel();
                            finish();
                        });
                        warnung.show();
                    }
                    quizVocabsLiveData.removeObservers(QuizMultipleChoice.this);
                }
            });

            buttonPreviousQuestion.setOnClickListener(v -> previousQuestion());
            buttonNextQuestion.setOnClickListener(v -> nextQuestion());
        }

    }

    public void generateQuestions() {
        questions = new ArrayList<>();

        allVocabsLiveData = vokabelViewModel.getAlleVokabeln();
        allVocabsLiveData.observe(this, allVocabs -> {
            if (allVocabs != null) {
                if (allVocabs.size() > 4) {
                    for (Vokabel quizVocab: quizVocabs) {
                        List<Vokabel> vocabs = new ArrayList<>(quizVocabs); //essential not redundant!!!
                        vocabs.remove(quizVocab);
                        Collections.shuffle(vocabs);
                        List<String> question = new ArrayList<>();
                        List<String> answers = new ArrayList<>();
                        if (language_from_to == QuizSettings.DE_TO_ENG) {
                            question.add(quizVocab.getVokabelDE());
                            answers.add(quizVocab.getVokabelENG());
                            for (int i = 0; i < 3; i++) {
                                answers.add(vocabs.get(0).getVokabelENG());
                                Log.d("answers Englisch add", vocabs.get(0).getVokabelENG());
                                vocabs.remove(0);
                            }
                        } else {
                            question.add(quizVocab.getVokabelENG());
                            answers.add(quizVocab.getVokabelDE());
                            for (int i = 0; i < 3; i++) {
                                answers.add(vocabs.get(0).getVokabelDE());
                                Log.d("answers german add", vocabs.get(0).getVokabelDE());
                                vocabs.remove(0);
                            }
                        }
                        Collections.shuffle(answers);
                        question.addAll(answers);

                        questions.add(question);
                        loadQuestion();
                        allVocabsLiveData.removeObservers(QuizMultipleChoice.this);
                    }
                } else {
                    AlertDialog.Builder warnung = new AlertDialog.Builder(
                            QuizMultipleChoice.this);
                    warnung.setTitle("Quiz kann nicht gestartet werden");
                    warnung.setMessage("Es sind nicht genug Vokabeln vorhanden.");
                    warnung.setPositiveButton("OK", (dialog, which) -> {
                        dialog.cancel();
                        Intent backIntent = new Intent(QuizMultipleChoice.this,
                                QuizSettings.class);
                        QuizMultipleChoice.this.startActivity(backIntent);
                        finish();
                    });
                    warnung.show();
                    allVocabsLiveData.removeObservers(QuizMultipleChoice.this);
                }
            }
        });
    }

    public void loadQuestion() {
        List<String> question = questions.get(questionPointer);

        questionField.setText(question.get(0));

        buttonAnswer1.setText(question.get(1));
        buttonAnswer2.setText(question.get(2));
        buttonAnswer3.setText(question.get(3));
        buttonAnswer4.setText(question.get(4));

        animB1.setVisibility(GONE);
        animB2.setVisibility(GONE);
        animB3.setVisibility(GONE);
        animB4.setVisibility(GONE);

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

            MaterialButton[] mbs = {buttonAnswer1, buttonAnswer2, buttonAnswer3, buttonAnswer4};

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
                correctAnswer = quizVocabs.get(questionPointer).getVokabelENG();
            } else {
                correctAnswer = quizVocabs.get(questionPointer).getVokabelDE();
            }

            if (answerText.equals(correctAnswer)) {
                answerButton.setBackgroundColor(Color.parseColor("#51ad4a"));

                anim.setVisibility(VISIBLE);
                anim.playAnimation();
                anim.setSpeed(2);
                anim.setMaxProgress(1f);
            } else {
                answerButton.setBackgroundColor(Color.parseColor("#842c1e"));
                for(Button mb : mbs) {
                    if (mb.getText().equals(correctAnswer))
                        mb.setBackgroundColor(Color.parseColor("#51ad4a"));
                }
            }

        } else {
            buttonAnswer1.setOnClickListener(v -> checkAnswer((MaterialButton) v, 1));
            buttonAnswer2.setOnClickListener(v -> checkAnswer((MaterialButton) v, 2));
            buttonAnswer3.setOnClickListener(v -> checkAnswer((MaterialButton) v, 3));
            buttonAnswer4.setOnClickListener(v -> checkAnswer((MaterialButton) v, 4));
        }

    }

    private void previousQuestion() {
        try {
            if (questionPointer > 0) {
                questionPointer --;
            } else {
                questionPointer = questions.size() - 1;
            }
            loadQuestion();
        } catch (NullPointerException exception) {
            AlertDialog.Builder warnung = new AlertDialog.Builder(
                    QuizMultipleChoice.this);
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
            if (questionPointer == questions.size() - 1) {
                questionPointer = 0;
            } else {
                questionPointer ++;
            }
            loadQuestion();
        } catch (NullPointerException exception) {
            AlertDialog.Builder warnung = new AlertDialog.Builder(
                    QuizMultipleChoice.this);
            warnung.setTitle("Vokabeln konnten nicht geladen werden.");
            warnung.setMessage("Es sind nicht genug Vokabeln vorhanden.");
            warnung.setPositiveButton("OK", (dialog, which) -> {
                dialog.cancel();
                finish();
            });
            warnung.show();
        }
    }

    private void checkAnswer(MaterialButton answerButton, int anim) {
        final TinyDB tb = new TinyDB(getApplicationContext());
        final int dayOfWeek = LocalDate.now().getDayOfWeek();
        weekdayViewModel.incrementNumberOfTrainedVocabs(dayOfWeek);

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
        String correctAnswer;
        Vokabel quizVocab = quizVocabs.get(questionPointer);

        vokabelViewModel.updateAnswered(quizVocab.getId(), quizVocab.getAnswered() + 1);

        if (language_from_to == QuizSettings.DE_TO_ENG) {
            correctAnswer = quizVocab.getVokabelENG();
        } else {
            correctAnswer = quizVocab.getVokabelDE();
        }

        if (answerText.equals(correctAnswer)) {
            givePoint();
            answerButton.setBackgroundColor(Color.parseColor("#51ad4a"));

            if(tb.getBoolean("quiz_sounds")) {
                if (rightSound.isPlaying())
                    rightSound.stop();
                rightSound.start();
                rightSound.setVolume(0.5f, 0.5f);
            }

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

            vokabelViewModel.updateCountRightAnswers(quizVocab.getId(),
                    quizVocab.getRichtig() + 1);
            weekdayViewModel.incrementNumberOfRightAnswers(dayOfWeek);
        } else {
            answerButton.setBackgroundColor(Color.parseColor("#842c1e"));

            animB1.setVisibility(GONE);
            animB2.setVisibility(GONE);
            animB3.setVisibility(GONE);
            animB4.setVisibility(GONE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrate.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrate.vibrate(500);
            }

            if(tb.getBoolean("quiz_sounds")) {
                vokabelViewModel.updateCountWrongAnswers(quizVocab.getId(),
                        quizVocab.getFalsch() + 1);
                weekdayViewModel.incrementNumberOfWrongAnswers(dayOfWeek);
            }

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

        checkIfSolved();
    }

    private void checkIfSolved() {
        final TinyDB tb = new TinyDB(getApplicationContext());
        if (answers.size() == questions.size()) {
            final Handler handler = new Handler();
            handler.postDelayed(() -> {
                buttonPreviousQuestion.setOnClickListener(null);
                buttonNextQuestion.setOnClickListener(null);

                resultCard.setVisibility(VISIBLE);
                questionCard.setVisibility(GONE);
                buttonAnswer1.setVisibility(GONE);
                buttonAnswer2.setVisibility(GONE);
                buttonAnswer3.setVisibility(GONE);
                buttonAnswer4.setVisibility(GONE);

                resultCountRight.setText(String.format("%s Fragen richtig", pointsCounter));
                resultCountWrong.setText(String.format("%s Fragen falsch", (questions.size() - pointsCounter)));

                if((quizVocabs.size() - pointsCounter) < Math.ceil(quizVocabs.size() / 2f)) {
                    resultEvaluation.setText(R.string.gratualtion);
                    if(tb.getBoolean("quiz_sounds")) {
                        win.start();
                        win.setVolume(0.4f, 0.4f);
                    }
                } else {
                    resultEvaluation.setText(R.string.schade);
                    if(tb.getBoolean("quiz_sounds")) {
                        loss.start();
                        loss.setVolume(0.4f, 0.4f);
                    }
                }
                animB1.setVisibility(GONE);
                animB2.setVisibility(GONE);
                animB3.setVisibility(GONE);
                animB4.setVisibility(GONE);

                restartButton.setOnClickListener(v -> finish());
            }, 800);
        }
    }

    private void givePoint() {
        pointsCounter ++;
        pointsCounterView.setText(String.valueOf(pointsCounter));
    }
}
