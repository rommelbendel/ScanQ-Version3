package com.rommelbendel.scanQ;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GestureDetectorCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;

import com.rommelbendel.scanQ.additional.TinyDB;
import com.rommelbendel.scanQ.impaired.visually.InterruptExecutor;
import com.rommelbendel.scanQ.impaired.visually.OnCommandListener;
import com.rommelbendel.scanQ.impaired.visually.Procedure;
import com.rommelbendel.scanQ.impaired.visually.ProcedureInterrupt;
import com.rommelbendel.scanQ.impaired.visually.QuizVI;
import com.rommelbendel.scanQ.impaired.visually.VoiceControl;
import com.rommelbendel.scanQ.speechrecognition.RecognitionViewModel;
import com.rommelbendel.scanQ.speechrecognition.VoiceInput;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QuizSettings extends AppCompatActivity {

    public static final String ID_EXTRA_CATEGORY = "CATEGORY";
    public static final String ID_EXTRA_VOCAB_NUM = "VOCAB_NUM";
    public static final String ID_EXTRA_ONLY_NEW = "ONLY_NEW";
    public static final String ID_EXTRA_FROM_TO = "FROM_TO";

    public static final int DE_TO_ENG = 1;
    public static final int ENG_TO_DE = 2;

    private Spinner categorySpinner;
    private ImageView vorschau;
    private CheckBox onlyNewVocabs;

    private VoiceControl voiceControl;
    private RecognitionViewModel recognitionResultVM;
    private List<Kategorie> categories;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TinyDB tb = new TinyDB(getApplicationContext());
        KategorienViewModel kategorienViewModel = new ViewModelProvider(this).get(KategorienViewModel.class);

        kategorienViewModel.getAlleKategorien().observe(QuizSettings.this, allCategories -> {
            if (allCategories != null) {
                categories = allCategories;
                kategorienViewModel.getAlleKategorien().removeObservers(QuizSettings.this);


                if (tb.getBoolean("visually_impaired")) {
                    recognitionResultVM = new ViewModelProvider(QuizSettings.this)
                            .get(RecognitionViewModel.class);
                    voiceControl = new VoiceControl(this, recognitionResultVM);

                    GestureDetectorCompat gestureDetectorCompat = new GestureDetectorCompat(
                            this, voiceControl);
                    gestureDetectorCompat.setOnDoubleTapListener(voiceControl);
                    gestureDetectorCompat.setIsLongpressEnabled(true);

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
                            return new ArrayList<>(Collections.singletonList("ein Quiz"));
                        }

                        @NonNull
                        @Override
                        public ArrayList<String> getQuestions() {
                            ArrayList<String> questions = new ArrayList<>();

                            questions.add("Mit den Vokablen welcher Kategorie möchtest Du spielen? " +
                                    "Um Dir alle aufzählen zu lassen, frage einfach: Welche gibt es?");
                            questions.add("Mit wie vielen Vokabeln möchtest Du spielen? " +
                                    "Bitte wähle eine Zahl zwischen 4 und 25.");
                            questions.add("Möchtest Du nur mit ungelernten Vokabeln spielen? " +
                                    "Bitte antworte mit ja oder nein.");
                            questions.add("In welche Sprache möchtest Du die Vokabeln übersetzen? " +
                                    "Bitte antworte Englisch oder Deutsch.");

                            return questions;
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
                                        kategorienViewModel.getAlleKategorien().observe(
                                                QuizSettings.this, categories -> {
                                                    if (categories != null) {
                                                        if (categories.size() > 0) {
                                                            StringBuilder readOut = new StringBuilder(
                                                                    "Die möglichen Kategorien lauten ");
                                                            for (Kategorie category : categories) {
                                                                readOut.append(category.getName()).append(", ");
                                                            }
                                                            readOut.replace(readOut.length() - 2,
                                                                    readOut.length(), ".");
                                                            Log.e("speaking", readOut.toString());
                                                            InterruptExecutor.speak(readOut.toString(),
                                                                    QuizSettings.this);
                                                        } else {
                                                            InterruptExecutor.speak("Es sind keine Kategorien " +
                                                                    "vorhanden. Füge zuerst Vokabeln hinzu um " +
                                                                    "ein Quiz zu starten.", QuizSettings.this);
                                                        }
                                                        kategorienViewModel.getAlleKategorien()
                                                                .removeObservers(QuizSettings.this);
                                                    }
                                                });
                                    }
                                }
                            });

                            return interrupts;
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
                        public void onAnswer(int index, @NonNull String question, @NonNull String answer) {

                        }

                        @Override
                        public boolean isAnswerValid(final int index, @NotNull final String question,
                                                     @NotNull final String answer) {
                            boolean isValid = false;
                            switch (index) {
                                case 0:
                                    ArrayList<String> categoryNames = new ArrayList<>();
                                    for (Kategorie category : categories) {
                                        categoryNames.add(category.getName());
                                    }
                                    isValid = VoiceControl.containsIgnoreCase(categoryNames, answer);
                                    break;
                                case 1:
                                    String answerCorrected = answer.replace(" Uhr", "").trim();
                                    if (isInteger(answerCorrected)) {
                                        final int answerInt = Integer.parseInt(answerCorrected);
                                        isValid = answerInt >= 4 && answerInt <= 25;
                                    }
                                    break;
                                case 2:
                                    isValid = (answer.equalsIgnoreCase("ja") ||
                                            answer.equalsIgnoreCase("nein"));
                                    break;
                                case 3:
                                    isValid = (answer.equalsIgnoreCase("Englisch") ||
                                            answer.equalsIgnoreCase("Deutsch"));
                                    break;
                            }
                            return isValid;
                        }

                        @Override
                        public boolean onAnswersAvailable(@NonNull ArrayList<String> answers) {
                            final Intent startQuizIntent = new Intent(QuizSettings.this,
                                    QuizVI.class);
                            String realCategoryName = null;
                            for (Kategorie category : categories) {
                                if (category.getName().equalsIgnoreCase(answers.get(0)))
                                    realCategoryName = category.getName();
                            }
                            startQuizIntent.putExtra(ID_EXTRA_CATEGORY, realCategoryName);

                            startQuizIntent.putExtra(ID_EXTRA_VOCAB_NUM, Integer.parseInt(
                                    answers.get(1).replace(" Uhr", "")));

                            startQuizIntent.putExtra(ID_EXTRA_ONLY_NEW,
                                    answers.get(2).equalsIgnoreCase("ja"));

                            if (answers.get(3).equalsIgnoreCase("Englisch"))
                                startQuizIntent.putExtra(ID_EXTRA_FROM_TO, DE_TO_ENG);
                            else startQuizIntent.putExtra(ID_EXTRA_FROM_TO, ENG_TO_DE);

                            QuizSettings.this.startActivity(startQuizIntent);

                            return true;
                        }
                    });

                    voiceControl.executeProcedureWithName("ein Quiz");
                } else {
                    setContentView(R.layout.activity_quiz_settings);

                    Spinner quizTypeSpinner = findViewById(R.id.quizModeSelection);
                    categorySpinner = findViewById(R.id.categorySelection);
                    vorschau = findViewById(R.id.vorschau_quiz_iv);
                    TextView vocabNumText = findViewById(R.id.vocabNum);
                    SeekBar vocabAmountSeekBar = findViewById(R.id.amountOfVocabs);
                    ImageButton startQuizButton = findViewById(R.id.startQuizButton);
                    onlyNewVocabs = findViewById(R.id.onlyNewVocabs);

                    int[] imgs = {R.drawable.eingabe_quiz_de,
                            R.drawable.eingabe_eng,
                            R.drawable.multiple_choice,
                            R.drawable.multiple_choice,
                            R.drawable.voice_quiz};

                    quizTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            vorschau.setImageResource(imgs[position]);
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {
                            vorschau.setImageResource(imgs[0]);
                        }
                    });

                    ArrayAdapter<CharSequence> quizTypeAdapter = ArrayAdapter.createFromResource(this,
                            R.array.QuizModeDropDown_Array, android.R.layout.simple_spinner_item);
                    quizTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    quizTypeSpinner.setAdapter(quizTypeAdapter);

                    LiveData<List<Kategorie>> alleKategorien = kategorienViewModel.getAlleKategorien();
                    alleKategorien.observe(this, categories -> {
                        if (categories != null) {
                            if (!categories.isEmpty()) {
                                List<String> categoryNames = new ArrayList<>();
                                for (Kategorie category : categories) {
                                    categoryNames.add(category.getName());
                                }
                                ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                                        QuizSettings.this,
                                        android.R.layout.simple_spinner_item, categoryNames);
                                categoryAdapter.setDropDownViewResource(
                                        android.R.layout.simple_spinner_dropdown_item);
                                categorySpinner.setAdapter(categoryAdapter);
                            } else {
                                AlertDialog.Builder warning = new AlertDialog.Builder(QuizSettings.this);
                                warning.setTitle("Keine Vokabeln vorhanden");
                                warning.setMessage("Möchtest du Vokabeln hinzufügen?");
                                warning.setPositiveButton("einscannen", (dialog, which) -> {
                                    dialog.dismiss();
                                    Intent scannIntent = new Intent(QuizSettings.this,
                                            Einscannen.class);
                                    QuizSettings.this.startActivity(scannIntent);
                                    finish();
                                });
                                warning.setNegativeButton("zurück", (dialog, which) -> {
                                    dialog.dismiss();
                                    finish();
                                });
                                warning.show();
                            }
                        }
                    });

                    final TextView vocabNumTxt = vocabNumText;
                    vocabAmountSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                        @Override
                        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                            vocabNumTxt.setText(String.valueOf(progress));
                        }

                        @Override
                        public void onStartTrackingTouch(SeekBar seekBar) {
                            //seekBar.setBackgroundColor(Color.parseColor("#ffffff"));
                        }

                        @Override
                        public void onStopTrackingTouch(SeekBar seekBar) {
                            //seekBar.setBackgroundColor(Color.parseColor("#ffffff"));
                        }
                    });
                    vocabAmountSeekBar.setProgress(12);

                    startQuizButton.setOnClickListener(v -> {
                        final String category = categorySpinner.getSelectedItem().toString();
                        final String quizType = quizTypeSpinner.getSelectedItem().toString();
                        final int vocabNum = vocabAmountSeekBar.getProgress();
                        final boolean onlyUntrained = onlyNewVocabs.isChecked();

                        Intent startQuizIntent;
                        switch (quizType) {
                            case "Eingabe DE":
                                startQuizIntent = new Intent(QuizSettings.this, QuizEingabe.class);
                                startQuizIntent.putExtra(ID_EXTRA_FROM_TO, ENG_TO_DE);
                                break;
                            case "Eingabe ENG":
                                startQuizIntent = new Intent(QuizSettings.this, QuizEingabe.class);
                                startQuizIntent.putExtra(ID_EXTRA_FROM_TO, DE_TO_ENG);
                                break;
                            case "Multiple-Choice DE":
                                startQuizIntent = new Intent(QuizSettings.this, QuizMultipleChoice.class);
                                startQuizIntent.putExtra(ID_EXTRA_FROM_TO, ENG_TO_DE);
                                break;
                            case "Multiple-Choice ENG":
                                startQuizIntent = new Intent(QuizSettings.this, QuizMultipleChoice.class);
                                startQuizIntent.putExtra(ID_EXTRA_FROM_TO, DE_TO_ENG);
                                break;
                            case "Spracheingabe":
                                startQuizIntent = new Intent(QuizSettings.this, QuizVoice.class);
                                break;
                            default:
                                throw new IllegalStateException("Unexpected value: " + quizType);
                        }

                        startQuizIntent.putExtra(ID_EXTRA_CATEGORY, category);
                        startQuizIntent.putExtra(ID_EXTRA_VOCAB_NUM, vocabNum);
                        startQuizIntent.putExtra(ID_EXTRA_ONLY_NEW, onlyUntrained);

                        QuizSettings.this.startActivity(startQuizIntent);
                    });

                }
            }
        });
    }

    public static boolean isInteger(String s) {
        return isInteger(s,10);
    }

    public static boolean isInteger(@NonNull String s, int radix) {
        if(s.isEmpty()) return false;
        for(int i = 0; i < s.length(); i++) {
            if(i == 0 && s.charAt(i) == '-') {
                if(s.length() == 1) return false;
                else continue;
            }
            if(Character.digit(s.charAt(i),radix) < 0) return false;
        }
        return true;
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
