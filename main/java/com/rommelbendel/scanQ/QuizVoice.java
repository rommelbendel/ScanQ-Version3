package com.rommelbendel.scanQ;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Vibrator;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.button.MaterialButton;
import com.rommelbendel.scanQ.additional.TinyDB;
import com.rommelbendel.scanQ.speechrecognition.RecognitionViewModel;
import com.rommelbendel.scanQ.speechrecognition.VoiceInput;

import org.joda.time.LocalDate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;


public class QuizVoice extends AppCompatActivity{

    //private ImageButton button_voice_input;
    private LottieAnimationView button_voice_input_animated;
    private TextView vokabelAnzeige;
    private Button button_ok;
    private Button button_bearbeiten;
    private ImageButton button_next;
    private ImageButton button_previous;
    private EditText answer_text;
    private TextView points;
    private CardView quizCard;
    private LinearLayout quizLayout;
    private CardView resultCard;
    private TextView resultRichtig, resultFalsch, resultEvaluation;
    private MaterialButton button_restart;
    private CardView dialogRight;
    private CardView dialogWrong;
    private TextView dialogSolutionWrong;
    private TextView dialogTextSolutionWrong;
    private TextView dialogSolutionRight;
    private TextView dialogTextSolutionRight;

    private boolean recordAudio;
    //private SpeechRecognizer speechRecognizer;
    private VokabelViewModel vokabelViewModel;
    private WeekdayViewModel weekdayViewModel;
    private LiveData<List<Vokabel>> alleVokabelnLiveData;
    private List<Vokabel> quizFrageVokabeln; //diese Liste wird durchgegangen
    private Bundle frage_und_antwort; //Speicher für die Antworten
    private int frageNummer = 0;
    private String language_from = "DE";
    private int punkte = 0;
    private String categoryName;
    private int vocabNum;
    private boolean onlyUntrained;
    private RecognitionViewModel recognitionResultVM;
    //private SpeechRecognizer speechRecognizer;
    private VoiceInput voiceInput;
    private String previousAnswer = "";
    private boolean vrActive = false;

    //private float x1,x2;
    //private final int WIPE_MIN = 120;


    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                    new ActivityResultCallback<Boolean>() {
                        @Override
                        public void onActivityResult(Boolean isGranted) {
                            if (isGranted) {
                                recordAudio = true;
                            } else {
                                recordAudio = false;
                                AlertDialog.Builder featureUnaviable = new AlertDialog.Builder(
                                        QuizVoice.this);
                                featureUnaviable.setTitle("nicht verfügbar");
                                featureUnaviable.setMessage("Dieses Quiz benötigt Zugriff auf das " +
                                        "Mikrophon um vollständig zu funktionieren.");
                                featureUnaviable.setPositiveButton("OK",
                                        (dialog, which) -> dialog.cancel());
                                featureUnaviable.show();
                            }
                        }
                    });


    //@SuppressLint("ClickableViewAccessibility")
    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz_voice);

        Bundle extras = getIntent().getExtras();

        if (extras != null) {
            categoryName = extras.getString(QuizSettings.ID_EXTRA_CATEGORY);
            vocabNum = extras.getInt(QuizSettings.ID_EXTRA_VOCAB_NUM);
            onlyUntrained = extras.getBoolean(QuizSettings.ID_EXTRA_ONLY_NEW);
        } else {
            finish();
        }

        //button_voice_input = findViewById(R.id.button_voice_record);
        findViewById(R.id.online_result_card).setVisibility(View.GONE);
        button_voice_input_animated = findViewById(R.id.button_voice_record_animated);
        vokabelAnzeige = findViewById(R.id.voc);
        button_ok = findViewById(R.id.button_OK);
        //button_bearbeiten = findViewById(R.id.button_bearbeiten);
        answer_text = findViewById(R.id.input_voice_to_text);
        button_next = findViewById(R.id.quizright);
        button_previous = findViewById(R.id.quizleft);
        points = findViewById(R.id.points);
        resultCard = findViewById(R.id.resultCard1);
        quizCard = findViewById(R.id.questionCard1);
        quizLayout = findViewById(R.id.frage_layout);
        resultRichtig = findViewById(R.id.countRight);
        resultFalsch = findViewById(R.id.countWrong);
        resultEvaluation = findViewById(R.id.succsess);
        button_restart = findViewById(R.id.restart1);
        dialogRight = findViewById(R.id.richtig);
        dialogWrong = findViewById(R.id.falsch);
        dialogSolutionRight = findViewById(R.id.solution_right);
        dialogTextSolutionRight = findViewById(R.id.solution_text_right);
        dialogSolutionWrong = findViewById(R.id.solution_wrong);
        dialogTextSolutionWrong = findViewById(R.id.solution_text_wrong);

        resultCard.setVisibility(View.INVISIBLE);

        vokabelViewModel = new ViewModelProvider(this).get(VokabelViewModel.class);
        weekdayViewModel = new ViewModelProvider(this).get(WeekdayViewModel.class);

        if (onlyUntrained) {
            alleVokabelnLiveData = vokabelViewModel.getUntrainedCategoryVocabs(categoryName);
        } else {
            alleVokabelnLiveData = vokabelViewModel.getCategoryVocabs(categoryName);
        }

        answer_text.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                button_ok.setEnabled(s.toString().trim().length() != 0);
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void afterTextChanged(Editable s) {}
        });

        alleVokabelnLiveData.observe(this, vokabeln -> {
            if (vokabeln != null) {
                if (vokabeln.size() != 0) {
                    quizFrageVokabeln = alleVokabelnLiveData.getValue();
                    assert quizFrageVokabeln != null;
                    Collections.shuffle(quizFrageVokabeln);
                    if (quizFrageVokabeln.size() > vocabNum) {
                        QuizVoice.this.quizFrageVokabeln = quizFrageVokabeln.subList(0, vocabNum);
                    }
                    startQuiz();
                } else {
                    AlertDialog.Builder warnung = new AlertDialog.Builder(QuizVoice.this);
                    warnung.setTitle("Quiz kann nicht gestartet werden");
                    warnung.setMessage("Es sind keine Vokabeln vorhanden.");
                    warnung.setPositiveButton("OK", (dialog, which) -> {
                        dialog.cancel();
                        Intent backIntent = new Intent(QuizVoice.this,
                                QuizSettings.class);
                        QuizVoice.this.startActivity(backIntent);
                        finish();
                    });
                    warnung.show();
                }
            }
        });

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
                    TinyDB tb = new TinyDB(QuizVoice.this);
                    String accent = tb.getString("accent");

                    if (accent.isEmpty()) {
                        tb.putString("accent", "GB");
                        accent = "GB";
                    }

                    Locale locale = new Locale("en", accent);

                    voiceInput = new VoiceInput(QuizVoice.this,
                            (Activity) QuizVoice.this, recognitionResultVM, locale,
                            false);

                    voiceInput.listen();

                        /*
                        speechRecognizer = new SpeechRecognizer(getApplication(),
                                getSupportFragmentManager(), recognitionResultVM, locale,
                                false);

                        speechRecognizer.listen();
                         */

                    Objects.requireNonNull(recognitionResultVM.getCurrentResult()).observe(
                            QuizVoice.this, result -> {
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
                                            QuizVoice.this);
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
    }

    @Override
    protected void onPause() {
        super.onPause();
        //voiceInput.stopListening();
        vrActive = false;
        button_voice_input_animated.setSpeed(-5f);
        button_voice_input_animated.playAnimation();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //voiceInput.stopListening();
        vrActive = false;
        button_voice_input_animated.setSpeed(-5f);
        button_voice_input_animated.playAnimation();

    }

    private void refreshVoiceInputButton() {
        //button_voice_input.setBackground(ContextCompat.getDrawable(QuizVoice.this,
                //android.R.drawable.ic_btn_speak_now));
        Log.e("refreshVoiceInputButton", "refershed");
        //vrActive = false;
    }

    private void destroyViewModel() {
        recognitionResultVM = null;
    }

    @SuppressLint("ClickableViewAccessibility")
    private void startQuiz() {

        Log.d("vocabNum_real", String.valueOf(quizFrageVokabeln.size()));

        alleVokabelnLiveData.removeObservers(this);

        /*
        alleVokabelnLiveData.observe(this, vokabeln -> {
            Log.d("Data has changed", "alleVokabelnLiveData has changed");
            int changeIndex = -1;
            if (vokabeln.size() == quizFrageVokabeln.size()) {
                Log.d("Observer", "change");
                for (int i = 0; i < quizFrageVokabeln.size(); i++) {
                    if (!vokabeln.contains(quizFrageVokabeln.get(i))) {
                        quizFrageVokabeln.remove(i);
                        changeIndex = i;
                        break;
                    }
                }
                for (int i = 0; i < vokabeln.size(); i++) {
                    if (!quizFrageVokabeln.contains(vokabeln.get(i))) {
                        Vokabel neueVokabel = vokabeln.get(i);
                        if (changeIndex != -1) {
                            quizFrageVokabeln.add(changeIndex, neueVokabel);
                        } else {
                            quizFrageVokabeln.add(neueVokabel);
                        }
                        break;
                    }
                }
            } else if (quizFrageVokabeln.size() < vokabeln.size()){
                for (int i = 0; i < vokabeln.size(); i++) {
                    if (!quizFrageVokabeln.contains(vokabeln.get(i))) {
                        quizFrageVokabeln.add(vokabeln.get(i));
                        Log.d("Observer", "add");
                        break;
                    }
                }
            } else {
                for (int i = 0; i < quizFrageVokabeln.size(); i++) {
                    if (!vokabeln.contains(quizFrageVokabeln.get(i))) {
                        quizFrageVokabeln.remove(i);
                        Log.d("Observer", "remove");
                        break;
                    }
                }
            }
        });
         */

        frage_und_antwort = new Bundle();

        button_next.setOnClickListener(v -> nextQuestion());

        button_previous.setOnClickListener(v -> previousQuestion());

        button_ok.setOnClickListener(v -> checkAnswer());

        /*
        button_bearbeiten.setOnClickListener(v -> {
            final AlertDialog.Builder optionsDialog = new AlertDialog.Builder(
                    QuizVoice.this);
            optionsDialog.setTitle("Was möchtest du machen?");
            String[] options = {"löschen", "markieren", "bearbeiten"};
            optionsDialog.setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0:
                        Toast.makeText(QuizVoice.this, "Vokabel löschen",
                                Toast.LENGTH_SHORT).show();
                        vokabelViewModel.deleteVokabel(quizFrageVokabeln.get(frageNummer - 1));
                        nextQuestion();
                        break;
                    case 1:
                        Toast.makeText(QuizVoice.this, "markieren",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case 2:
                        Toast.makeText(QuizVoice.this, "bearbeiten",
                                Toast.LENGTH_SHORT).show();
                        break;
                }
                dialog.cancel();
            });
            optionsDialog.show();
        });
         */

        /*
        quizLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        x1 = event.getX();
                        break;
                    case MotionEvent.ACTION_UP:
                        x2 = event.getX();
                        if (Math.abs(x2 - x1) >= WIPE_MIN) {
                            if (x1 < x2) {  //nach rechts
                                previousQuestion();
                            } else {    //nach links
                                nextQuestion();
                            }
                        }
                        break;
                }
                return QuizVoice.super.onTouchEvent(event);
            }
        });
         */

        nextQuestion();
    }

    private void nextQuestion() {
        //voiceInput.stopListening();
        vrActive = false;
        button_voice_input_animated.setSpeed(-5f);
        button_voice_input_animated.playAnimation();

        try {
            if (frageNummer == quizFrageVokabeln.size()) {
                frageNummer = 1;
            } else {
                frageNummer++;
            }
            prepareQuestion();
            Vokabel vokabel = quizFrageVokabeln.get(frageNummer - 1);
            setQuestion(vokabel);
        } catch (NullPointerException e) {
            AlertDialog.Builder warnung = new AlertDialog.Builder(
                    QuizVoice.this);
            warnung.setTitle("Vokabeln konnten nicht geladen werden.");
            warnung.setMessage("Es sind nicht genug Vokabeln vorhanden.");
            warnung.setPositiveButton("OK", (dialog, which) -> {
                dialog.cancel();
                finish();
            });
            warnung.show();
        }
    }

    private void previousQuestion() {
        //voiceInput.stopListening();
        vrActive = false;
        button_voice_input_animated.setSpeed(-5f);
        button_voice_input_animated.playAnimation();

        try {
            if (frageNummer == 1) {
                frageNummer = quizFrageVokabeln.size();
            } else {
                frageNummer--;
            }
            prepareQuestion();
            Vokabel vokabel = quizFrageVokabeln.get(frageNummer - 1);
            setQuestion(vokabel);
        } catch (NullPointerException e) {
            AlertDialog.Builder warnung = new AlertDialog.Builder(
                    QuizVoice.this);
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
            if (frage_und_antwort.containsKey(quizFrageVokabeln.get(frageNummer - 1)
                    .getVokabelDE())) {

                String answer = frage_und_antwort.getString(quizFrageVokabeln.get(frageNummer - 1)
                        .getVokabelDE());
                String question = quizFrageVokabeln.get(frageNummer - 1).getVokabelDE();
                Log.e("question", question);
                answer_text.setText(answer);
                //button_voice_input.setEnabled(false);
                button_ok.setEnabled(false);
                answer_text.setEnabled(false);
                if (quizFrageVokabeln.get(frageNummer - 1).getVokabelENG().equals(answer)) {
                    //quizLayout.setBackgroundColor(Color.rgb(88, 214, 141));
                    dialogRight.setVisibility(View.VISIBLE);
                    dialogTextSolutionRight.setText(String.format("%s = %s", question, answer));
                    Log.d("prepareQuestion", "Frage richtig");
                } else {
                    //quizLayout.setBackgroundColor(Color.rgb(236, 112, 99));
                    dialogWrong.setVisibility(View.VISIBLE);
                    dialogTextSolutionWrong.setText(String.format("%s = %s", question, answer));
                    Log.d("prepareQuestion", "Frage falsch");
                }
            } else {
                answer_text.setText("");
                //button_voice_input.setEnabled(true);
                //button_ok.setEnabled(true);
                answer_text.setEnabled(true);
                quizLayout.setBackgroundColor(Color.WHITE);
            }
        } else {
            if (frage_und_antwort.containsKey(quizFrageVokabeln.get(frageNummer - 1)
                    .getVokabelENG())) {
                String answer = frage_und_antwort.getString(quizFrageVokabeln.get(frageNummer - 1)
                        .getVokabelENG());
                String question = quizFrageVokabeln.get(frageNummer - 1).getVokabelENG();
                Log.e("question", question);
                answer_text.setText(answer);
                //button_voice_input.setEnabled(false);
                button_ok.setEnabled(false);
                answer_text.setEnabled(false);
                if (quizFrageVokabeln.get(frageNummer - 1).getVokabelDE().equals(answer)) {
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

        /*
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            TextToSpeech tts = new TextToSpeech(QuizVoice.this, new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int status) {
                    if (status == TextToSpeech.SUCCESS) {
                        Log.d("TTS", "initialisierung erfolgreich");
                    } else {
                        Log.d("TTS", "initialisierung fehlgeschlagen");
                    }
                }
            });
            tts.setLanguage(Locale.GERMAN);
            tts.speak(anzeige, TextToSpeech.QUEUE_ADD, null);
        }
         */

    }

    @SuppressLint("DefaultLocale")
    private void checkAnswer() {
        final int dayOfWeek = LocalDate.now().getDayOfWeek();
        weekdayViewModel.incrementNumberOfTrainedVocabs(dayOfWeek);

        //button_voice_input.setEnabled(false);
        button_ok.setEnabled(false);
        answer_text.setEnabled(false);
        String antwort = answer_text.getText().toString().trim();
        String loesung;
        String frage;
        Vokabel frageVokabel = quizFrageVokabeln.get(frageNummer -1);
        if (language_from.equals("DE")) {
            frage_und_antwort.putString(frageVokabel.getVokabelDE(), antwort);
            loesung = frageVokabel.getVokabelENG();
            frage = frageVokabel.getVokabelDE();
        } else {
            frage_und_antwort.putString(frageVokabel.getVokabelENG(), antwort);
            loesung = frageVokabel.getVokabelDE();
            frage = frageVokabel.getVokabelENG();
        }

        vokabelViewModel.updateAnswered(frageVokabel.getId(),
                frageVokabel.getAnswered() + 1);

        Log.d("checkAnswer", antwort + " == " + loesung);

        if (antwort.equalsIgnoreCase(loesung)) {
            givePoint();
            //quizLayout.setBackgroundColor(Color.rgb(88, 214, 141));
            dialogRight.setVisibility(View.VISIBLE);
            dialogTextSolutionRight.setText(String.format("%s = %s", frage, loesung));
            vokabelViewModel.updateCountRightAnswers(frageVokabel.getId(),
                    frageVokabel.getRichtig() + 1);

            weekdayViewModel.incrementNumberOfRightAnswers(dayOfWeek);
        } else {
            //quizLayout.setBackgroundColor(Color.rgb(236, 112, 99));
            dialogWrong.setVisibility(View.VISIBLE);
            dialogTextSolutionWrong.setText(String.format("%s = %s", frage, loesung));
            vokabelViewModel.updateCountWrongAnswers(frageVokabel.getId(),
                    frageVokabel.getFalsch() + 1);

            weekdayViewModel.incrementNumberOfWrongAnswers(dayOfWeek);
        }

        boolean solved = true;
        for (int i = 0; i < quizFrageVokabeln.size(); i++) {
            if (language_from.equals("DE")) {
                if (!frage_und_antwort.containsKey(quizFrageVokabeln.get(i).getVokabelDE())) {
                    solved = false;
                    break;
                }
            } else {
                if (!frage_und_antwort.containsKey(quizFrageVokabeln.get(i).getVokabelENG())) {
                    solved = false;
                    break;
                }
            }
        }
        if (solved) {
            //button_bearbeiten.setEnabled(false);
            button_next.setEnabled(false);
            button_previous.setEnabled(false);

            resultRichtig.setText(String.format("%d Fragen richtig", punkte));
            resultFalsch.setText(String.format("%d Fragen falsch", quizFrageVokabeln.size() - punkte));

            resultCard.setVisibility(View.VISIBLE);
            quizCard.setVisibility(View.GONE);
            dialogWrong.setVisibility(View.GONE);
            dialogRight.setVisibility(View.GONE);

            if((quizFrageVokabeln.size() - punkte) < Math.ceil(quizFrageVokabeln.size() / 2f)) {
                resultEvaluation.setText(R.string.gratualtion);
            } else {
                resultEvaluation.setText(R.string.schade);
            }

            button_restart.setOnClickListener(v -> {
                //QuizVoice.this.recreate();
                finish();
            });
        }

    }

    private void givePoint() {
        punkte ++;
        points.setText(String.valueOf(punkte));
        Log.d("Punkte", String.valueOf(punkte));
    }

    private boolean checkRecordAudioPermission() {
        return ContextCompat.checkSelfPermission(QuizVoice.this,
                Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestRecordAudioPermission() {
        requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
    }

    //copy this to every activity which uses VoiceInput
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        assert data != null;
        Log.e("onActivityResult", data.toString());
        if (requestCode == VoiceInput.getVoiceRecognitionRequestCode()
                && resultCode == Activity.RESULT_OK) {
            Log.e("onActivityResult", "setting results...");
            // Fill the list view with the strings the recognizer thought it could have heard
            ArrayList<String> matches = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);
            recognitionResultVM.setCurrentResult(matches);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
