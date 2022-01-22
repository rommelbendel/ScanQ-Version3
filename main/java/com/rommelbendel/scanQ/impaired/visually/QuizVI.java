package com.rommelbendel.scanQ.impaired.visually;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.speech.RecognizerIntent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GestureDetectorCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;

import com.rommelbendel.scanQ.QuizSettings;
import com.rommelbendel.scanQ.R;
import com.rommelbendel.scanQ.Vokabel;
import com.rommelbendel.scanQ.VokabelViewModel;
import com.rommelbendel.scanQ.WeekdayViewModel;
import com.rommelbendel.scanQ.speechrecognition.RecognitionViewModel;
import com.rommelbendel.scanQ.speechrecognition.VoiceInput;

import org.joda.time.LocalDate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QuizVI extends AppCompatActivity {
    private VoiceControl voiceControl;
    private List<Vokabel> vocabulary;
    private VokabelViewModel vokabelViewModel;
    private WeekdayViewModel weekdayViewModel;

    private RecognitionViewModel recognitionViewModel;

    private int index = 0;
    private ArrayList<String> questions;
    private String categoryName;
    private int numberOfVocabs;
    private boolean onlyUntrained;
    private int fromTo;
    private short points = 0;

    private Vibrator vibrate;
    private MediaPlayer loss;
    private MediaPlayer win;
    private MediaPlayer wrongSound;
    private MediaPlayer rightSound;

    private static final String QUESTION_TEXT_DE = "Was ist die Übersetzung für %s";
    private static final String QUESTION_TEXT_ENG = "What's the translation for %s";
    private static final String TEXT_POSITIVE_RESULT = "Herzlichen Glückwunsch! Du hast %s von " +
            "%s Vokablen richtig übersetzt.";
    private static final String TEXT_NEGATIVE_RESULT = "Schade, du hast leider nur %s von %s " +
            "Vokabeln richtig übersetzt.";
    private static final String TEXT_END = "Verwende den Befehl 'wiederholen', um mit den selben " +
            "Einstellungen erneut zu spielen.";
    private static final String ASK_QUESTION_PROCEDURE_NAME = "<!--ask_question-->";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        vibrate = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        loss = MediaPlayer.create(this, R.raw.niederlage);
        win = MediaPlayer.create(this, R.raw.sieg);
        wrongSound = MediaPlayer.create(this, R.raw.falsch);
        rightSound = MediaPlayer.create(this, R.raw.richtig);

        recognitionViewModel = new ViewModelProvider(QuizVI.this)
                .get(RecognitionViewModel.class);
        voiceControl = new VoiceControl(QuizVI.this, recognitionViewModel);
        voiceControl.setOnCommandListener(new OnCommandListener() {
            @Override
            public ArrayList<ArrayList<String>> getAvailableCommands() {
                return new ArrayList<>();
            }

            @Override
            public boolean onCommand(String command) {
                return false;
            }

            @Override
            public void onCommandNotFound(String mostLikelyCommand, List<String> possibleCommands) {

            }
        });
        voiceControl.addProcedure(new Procedure() {
            @NonNull
            @Override
            public ArrayList<String> getProcedureNames() {
                Log.e("QuizVI - Procedure - called", "getProcedureNames");
                return new ArrayList<>(Collections.singletonList(ASK_QUESTION_PROCEDURE_NAME));
            }

            @NonNull
            @Override
            public ArrayList<String> getQuestions() {
                Log.e("QuizVI - Procedure - called", "getQuestions");

                //return new ArrayList<>(Collections.singletonList(getNextQuestion()));
                return questions;
            }

            @Override
            public int getTransitionFromTo() {
                return getCurrentQuestionTranslationDirection();
            }

            @Override
            public boolean redo() {
                return false;
            }

            @Override
            public boolean allowTermination() {
                return false;
            }

            @Override
            public void onAnswer(int index, @NonNull String question, @NonNull String answer) {
                if (isAnswerWright(answer, index)) {
                    points++;

                    if(rightSound.isPlaying())
                        rightSound.stop();
                    rightSound.start();
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrate.vibrate(VibrationEffect.createOneShot(500,
                                VibrationEffect.DEFAULT_AMPLITUDE));
                    } else {
                        vibrate.vibrate(500);
                    }

                    final StringBuilder text = new StringBuilder("Die richtige Antwort wäre ");
                    if (fromTo == QuizSettings.DE_TO_ENG)
                        text.append(vocabulary.get(index).getVokabelENG());
                    else
                        text.append(vocabulary.get(index).getVokabelDE());
                    InterruptExecutor.speak(text.toString(), QuizVI.this);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public boolean onAnswersAvailable(@NonNull ArrayList<String> answers) {
                Log.e("QuizVI - Procedure - called", "onAnswersAvailable");
                presentResults();
                return true;
            }
        });

        vokabelViewModel = new ViewModelProvider(this).get(VokabelViewModel.class);
        weekdayViewModel = new ViewModelProvider(this).get(WeekdayViewModel.class);

        final Bundle extras = getIntent().getExtras();

        categoryName = extras.getString(QuizSettings.ID_EXTRA_CATEGORY);
        Log.e("received", "categoryName " + categoryName);
        numberOfVocabs = extras.getInt(QuizSettings.ID_EXTRA_VOCAB_NUM);
        Log.e("received", "numberOfVocabs " + numberOfVocabs);
        onlyUntrained = extras.getBoolean(QuizSettings.ID_EXTRA_ONLY_NEW);
        Log.e("received", "onlyUntrained " + onlyUntrained);
        fromTo = extras.getInt(QuizSettings.ID_EXTRA_FROM_TO);
        Log.e("received", "fromTo " + fromTo);

        VokabelViewModel vocabularyVM = new ViewModelProvider(QuizVI.this).get(VokabelViewModel.class);
        final LiveData<List<Vokabel>> vocabularyLiveData;
        if (onlyUntrained)
            vocabularyLiveData = vocabularyVM.getUntrainedCategoryVocabs(categoryName);
        else
            vocabularyLiveData = vocabularyVM.getCategoryVocabs(categoryName);
        vocabularyLiveData.observe(QuizVI.this, vocabs -> {
            if (vocabs != null) {
                Log.e("QuizVI - observer", "vocabs received: " + vocabs.toString());
                vocabulary = vocabs;
                Collections.shuffle(vocabulary);
                if (numberOfVocabs < vocabulary.size())
                    vocabulary = vocabulary.subList(0, numberOfVocabs - 1);
                Log.e("QuizVI - observer", "final vocabs: " + vocabulary.toString());

                questions = new ArrayList<>();
                for (int i = 0; i < vocabulary.size(); i++) {
                    questions.add(getNextQuestion());
                }
                askNextQuestion();
                vocabularyLiveData.removeObservers(QuizVI.this);
            }
        });
    }

    private void askNextQuestion() {
        Log.e("QuizVI - called", "askNextQuestion");
        //voiceControl.setProcedureRunning();
        voiceControl.executeProcedureWithName(ASK_QUESTION_PROCEDURE_NAME);
    }

    @NonNull
    private String getNextQuestion() {
        Log.e("QuizVI - called", "getNextQuestion");
        String question;
        if (fromTo == QuizSettings.DE_TO_ENG)
            question = String.format(QUESTION_TEXT_DE, vocabulary.get(index).getVokabelDE());
        else
            question = String.format(QUESTION_TEXT_ENG, vocabulary.get(index).getVokabelENG());
        index++;
        return question;
    }

    private int getCurrentQuestionTranslationDirection() {
        return fromTo;
    }

    private boolean isAnswerWright(@NonNull final String answer, final int index) {
        Log.e("QuizVI - called", "isAnswerWright");
        final int dayOfWeek = LocalDate.now().getDayOfWeek();
        weekdayViewModel.incrementNumberOfTrainedVocabs(dayOfWeek);

        StringBuilder vocabsString = new StringBuilder();
        for (Vokabel vocab: vocabulary) {
            vocabsString.append(vocab.getVokabelENG());
            vocabsString.append(" = ");
            vocabsString.append(vocab.getVokabelDE());
            vocabsString.append(", ");
        }
        Log.e("vocabs", vocabsString.toString());
        Log.e("checking index", String.valueOf(index));
        final Vokabel questionVocab = vocabulary.get(index);
        vokabelViewModel.updateAnswered(questionVocab.getId(),
                questionVocab.getAnswered() + 1);

        String wrightAnswer;
        if (fromTo == QuizSettings.DE_TO_ENG)
            wrightAnswer = questionVocab.getVokabelENG();
        else
            wrightAnswer = questionVocab.getVokabelDE();

        final boolean wright = answer.equals(wrightAnswer);
        if (wright) {
            vokabelViewModel.updateCountRightAnswers(questionVocab.getId(),
                    questionVocab.getRichtig() + 1);
            weekdayViewModel.incrementNumberOfRightAnswers(dayOfWeek);
        } else {
            vokabelViewModel.updateCountWrongAnswers(questionVocab.getId(),
                    questionVocab.getFalsch() + 1);
            weekdayViewModel.incrementNumberOfWrongAnswers(dayOfWeek);
        }

        return wright;
    }

    private void presentResults() {
        Log.e("QuizVI - called", "presentResults");
        StringBuilder resultText = new StringBuilder();
        if (points >= vocabulary.size() / 2)
            resultText.append(String.format(TEXT_POSITIVE_RESULT, points, vocabulary.size()));
        else
            resultText.append(String.format(TEXT_NEGATIVE_RESULT, points, vocabulary.size()));
        resultText.append(TEXT_END);
        voiceControl.speak(resultText.toString(), () -> {
            GestureDetectorCompat gestureDetectorCompat = new GestureDetectorCompat(
                    this, voiceControl);
            gestureDetectorCompat.setOnDoubleTapListener(voiceControl);
            gestureDetectorCompat.setIsLongpressEnabled(true);

            voiceControl.setOnCommandListener(new OnCommandListener() {
                @Override
                public ArrayList<ArrayList<String>> getAvailableCommands() {
                    return new ArrayList<>(new ArrayList<>(Collections.singletonList(
                            new ArrayList<>(Collections.singletonList("wiederholen")))));
                }

                @Override
                public boolean onCommand(String command) {
                    if (command.equalsIgnoreCase("wiederholen")) {
                        QuizVI.this.restart();
                        return true;
                    } else
                        return false;
                }

                @Override
                public void onCommandNotFound(String mostLikelyCommand, List<String> possibleCommands) {
                    voiceControl.informCommandNotFound();
                }
            });
        });
    }

    private void restart() {
        Log.e("QuizVI - called", "restart");
        final Intent startQuizIntent = new Intent(QuizVI.this, QuizVI.class);

        startQuizIntent.putExtra(QuizSettings.ID_EXTRA_CATEGORY, categoryName);
        startQuizIntent.putExtra(QuizSettings.ID_EXTRA_VOCAB_NUM, numberOfVocabs);
        startQuizIntent.putExtra(QuizSettings.ID_EXTRA_ONLY_NEW, onlyUntrained);
        startQuizIntent.putExtra(QuizSettings.ID_EXTRA_FROM_TO, QuizSettings.DE_TO_ENG);

        QuizVI.this.startActivity(startQuizIntent);

        finish();
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
            recognitionViewModel.setCurrentResult(matches);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
