package com.rommelbendel.scanQ.impaired.visually;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GestureDetectorCompat;
import androidx.lifecycle.ViewModelProvider;

import com.rommelbendel.scanQ.NewHome;
import com.rommelbendel.scanQ.R;
import com.rommelbendel.scanQ.additional.TinyDB;
import com.rommelbendel.scanQ.speechrecognition.RecognitionViewModel;
import com.rommelbendel.scanQ.speechrecognition.VoiceInput;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TutorialVIM extends AppCompatActivity {
    private VoiceControl voiceControl;
    private RecognitionViewModel recognitionViewModel;

    private static final String GREETING = "Hallo, Du hast den Blindenmodus aktiviert. " +
            "Im Anschluss erhälst Du ein kurzes Tutorial. Um das Tutorial zu starten, mache " +
            "einen Doppeltipp und spreche den Befehl starte das Tutorial aus.";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.visually_impaired_mode_screen);

        Log.e("TutorialVIM", "finished");

        final TinyDB tb = new TinyDB(getApplicationContext());
        tb.putBoolean("vim_tutorial_finished", true);

        recognitionViewModel = new ViewModelProvider(TutorialVIM.this)
                .get(RecognitionViewModel.class);
        voiceControl = new VoiceControl(TutorialVIM.this, recognitionViewModel);

        voiceControl.addProcedure(new Procedure() {

            @NonNull
            @Override
            public ArrayList<String> getProcedureNames() {
                return new ArrayList<>(Collections.singletonList("das tutorial"));
            }

            @NonNull
            @Override
            public ArrayList<String> getQuestions() {
                final ArrayList<String> questions = new ArrayList<>();
                questions.add("Sehr gut, Du hast deinen ersten Abfragedialog gestartet." +
                        "Es folgen zwei Fragen, um dir den Ablauf zu erläutern. " +
                        "Hast Du das verstanden? Bitte antworte mit ja oder nein.");
                questions.add("Ok, wie alt bist Du?");
                return questions;
            }

            @Override
            public boolean redo() {
                return true;
            }

            @Override
            public boolean allowTermination() {
                return false;
            }

            @Override
            public boolean isAnswerValid(final int index, @NotNull final String question,
                                         @NotNull final String answer) {
                if (index == 0)
                    return (answer.equalsIgnoreCase("ja") ||
                            answer.equalsIgnoreCase("nein"));
                else return true;

            }

            @Override
            public void onAnswer(int index, @NonNull String question, @NonNull String answer) {

            }

            @Override
            public boolean onAnswersAvailable(@NonNull ArrayList<String> answers) {
                voiceControl.speak("Sehr gut. Du bist also " + answers.get(1) + "Jahre alt" +
                        ". Du bist jetzt bereit die App auszuprobieren. Um Dir alle möglichen" +
                        " Kommandos aufzählen zu lassen, kannst Du jeder Zeit den Befehl: " +
                        "Welche Kommandos gibt es, verwenden. Um fortzufahren mache einen " +
                        "Doppeltipp und führe den Befehl Tutorial beenden aus.");
                return true;
            }
        });

        voiceControl.setOnCommandListener(new OnCommandListener() {
            @Override
            public ArrayList<ArrayList<String>> getAvailableCommands() {
                return new ArrayList<>(new ArrayList<>(Collections.singletonList(
                        new ArrayList<>(Collections.singletonList("tutorial beenden")))));
            }

            @Override
            public boolean onCommand(String command) {
                if (command.equalsIgnoreCase("tutorial beenden")) {
                    final Intent homeIntent = new Intent(TutorialVIM.this, NewHome.class);
                    TutorialVIM.this.startActivity(homeIntent);
                    finish();
                    return true;
                } else
                    return false;
            }

            @Override
            public void onCommandNotFound(String mostLikelyCommand, List<String> possibleCommands) {
                voiceControl.informCommandNotFound();
            }
        });

        GestureDetectorCompat gestureDetectorCompat = new GestureDetectorCompat(
                this, voiceControl);
        gestureDetectorCompat.setOnDoubleTapListener(voiceControl);
        gestureDetectorCompat.setIsLongpressEnabled(true);

        voiceControl.speak(GREETING);

        /*
        Intent myIntent = new Intent(TutorialVIM.this, NewHome.class);
        TutorialVIM.this.startActivity(myIntent);
        finish();
         */
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
