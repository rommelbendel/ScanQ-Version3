package com.rommelbendel.scanQ.impaired.visually;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.GestureDetectorCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;

import com.rommelbendel.scanQ.CategoryOverview;
import com.rommelbendel.scanQ.Kategorie;
import com.rommelbendel.scanQ.KategorienViewModel;
import com.rommelbendel.scanQ.NewHome;
import com.rommelbendel.scanQ.QuizSettings;
import com.rommelbendel.scanQ.R;
import com.rommelbendel.scanQ.Weekday;
import com.rommelbendel.scanQ.WeekdayViewModel;
import com.rommelbendel.scanQ.additional.TinyDB;
import com.rommelbendel.scanQ.VokabelViewModel;
import com.rommelbendel.scanQ.speechrecognition.RecognitionViewModel;
import com.rommelbendel.scanQ.speechrecognition.VoiceInput;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joda.time.LocalDate;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;


public class VoiceControl implements GestureDetector.OnGestureListener,
        GestureDetector.OnDoubleTapListener {

    private final Context owner;
    private OnCommandListener commandListener;
    private final Activity ownerActivity;
    private final int originalLayoutID;
    private final ConstraintLayout rootLayout;
    private TextToSpeech textToSpeech;
    private HashMap<String, String> commandAndMethod;
    private ArrayList<Procedure> procedures = new ArrayList<>();
    private Procedure activeProcedure;
    private boolean procedureRunning = false;
    private int runningIndex;
    private final String keyword;
    private RecognitionViewModel recognitionResultVM;
    //private final VoiceInput voiceInput;
    private ProcedureViewModel procedureVM;
    private SpeechSynthesisViewModel speechSynthesisVM;
    private GestureDetector gestureDetector;
    private final HashMap<String, Class> startableActivities = new HashMap<>();


    private final OnCommandListener defaultMethods = new OnCommandListener() {
        @Override
        public ArrayList<ArrayList<String>> getAvailableCommands() {
            ArrayList<ArrayList<String>> availableCommands = new ArrayList<>();

            ArrayList<String> vocabNumber = new ArrayList<>();
            vocabNumber.add("wie viele vokabeln habe ich");
            availableCommands.add(vocabNumber);

            ArrayList<String> categoryNumber = new ArrayList<>();
            categoryNumber.add("wie viele kategorien habe ich");
            availableCommands.add(categoryNumber);

            ArrayList<String> home = new ArrayList<>();
            home.add("zurück");
            home.add("home");
            home.add("start");
            availableCommands.add(home);

            availableCommands.add(new ArrayList<>(Collections.singletonList(
                    "wie viele vokabeln habe ich heute gelernt")));

            availableCommands.add(new ArrayList<>(Collections.singletonList(
                    "welche kategorien habe ich")));

            ArrayList<String> info = new ArrayList<>();
            info.add("welche kommandos gibt es");
            info.add("welche befehle gibt es");
            info.add("was kann ich machen");
            availableCommands.add(info);

            return availableCommands;
        }

        @Override
        public boolean onCommand(String command) {
            if (command.equalsIgnoreCase("wie viele vokabeln habe ich")) {
                final String text = "Du hast %s Vokabeln insgesamt. " +
                        "Davon sind %s ungelernt.";
                VokabelViewModel vokabelViewModel = new ViewModelProvider(
                        (ViewModelStoreOwner) owner).get(VokabelViewModel.class);
                vokabelViewModel.getAlleVokabeln().observe((LifecycleOwner) owner, vocabs -> {
                    if (vocabs != null) {
                        final int vocabCount = vocabs.size();
                        vokabelViewModel.getUntrainedVocabs().observe(
                                (LifecycleOwner) owner, vocsUntrained -> {
                                    if (vocsUntrained != null) {
                                        String textToSpeak = String.format(text, vocabCount,
                                                vocsUntrained.size());
                                        speak(textToSpeak);
                                        vokabelViewModel.getUntrainedVocabs().removeObservers(
                                                (LifecycleOwner) owner);
                                    }
                                });
                        vokabelViewModel.getAlleVokabeln().removeObservers(
                                (LifecycleOwner) owner);

                    }
                });
                return true;
            } else if (command.equalsIgnoreCase("wie viele kategorien habe ich")) {
                final String text = "Du hast %s Kategorien.";
                KategorienViewModel categoryVM = new ViewModelProvider((ViewModelStoreOwner) owner)
                        .get(KategorienViewModel.class);
                categoryVM.getNumberOfCategories().observe((LifecycleOwner) owner, number -> {
                    if (number != null) {
                        speak(String.format(text, number));
                        categoryVM.getNumberOfCategories().removeObservers((LifecycleOwner) owner);
                    }
                });

                return true;
            } else if (command.equalsIgnoreCase("zurück") ||
                    command.equalsIgnoreCase("home") ||
                    command.equalsIgnoreCase("start")) {
                final Intent homeIntent = new Intent(ownerActivity, NewHome.class);
                ownerActivity.startActivity(homeIntent);

                return true;
            } else if (command.equalsIgnoreCase("wie viele vokabeln habe ich heute gelernt")) {
                final String text = "Du hast heute schon %s Vokabeln gelernt." +
                        "Davon hast Du %s falsch und %s richtig beantwortet.";
                final LocalDate today = LocalDate.now();
                WeekdayViewModel weekdayViewModel = new ViewModelProvider((ViewModelStoreOwner) owner)
                        .get(WeekdayViewModel.class);
                LiveData<Weekday> weekday = weekdayViewModel.getDay(today.getDayOfWeek());
                weekday.observe((LifecycleOwner) owner, day -> {
                    if (day != null) {
                        speak(String.format(text, day.getNumberOfTrainedVocabs(),
                                day.getNumberOfWrongAnswers(), day.getNumberOfRightAnswers()));
                        weekday.removeObservers((LifecycleOwner) owner);
                    }
                });
                return true;
            } else if (command.equalsIgnoreCase("welche kategorien habe ich")) {
                final StringBuilder text = new StringBuilder("Deine Kategorien sind: ");
                final String textEmpty = "Du hast bisher keine Kategorien";
                final StringBuilder oneCategory = new StringBuilder("Deine Kategorie heißt ");
                final KategorienViewModel categoryVM = new ViewModelProvider((ViewModelStoreOwner) owner)
                        .get(KategorienViewModel.class);
                LiveData<List<Kategorie>> categories = categoryVM.getAlleKategorien();
                categories.observe((LifecycleOwner) owner, allCategories -> {
                    if (allCategories != null) {
                        if (allCategories.size() == 1) {
                            oneCategory.append(allCategories.get(0).getName());
                            oneCategory.append(".");
                            speak(oneCategory.toString());
                        } else if (allCategories.size() == 0) {
                            speak(textEmpty);
                        } else {
                            for (Kategorie category: allCategories) {
                                text.append(category.getName());
                                text.append(", ");
                            }
                            String textOut = text.toString();
                            textOut = textOut.substring(0, text.length() - 2);
                            final int pos = textOut.lastIndexOf(",");
                            textOut = textOut.substring(0, pos) + " und" +
                                    textOut.substring(pos + 1);
                            speak(textOut);
                        }
                        categories.removeObservers((LifecycleOwner) owner);
                    }
                });
                return true;
            } else if (command.equalsIgnoreCase("was kann ich machen") ||
                    command.equalsIgnoreCase("welche befehle gibt es") ||
                    command.equalsIgnoreCase("welche kommandos gibt es")) {
                final StringBuilder text = new StringBuilder("Die möglichen Kommandos lauten: ");
                for (ArrayList<String> names: commandListener.getAvailableCommands()) {
                    if (!isAnonymousCommand(names.get(0))) {
                        text.append(names.get(0));
                        text.append(", ");
                    }
                }
                for (ArrayList<String> names: defaultMethods.getAvailableCommands()) {
                    if (!isAnonymousCommand(names.get(0))) {
                        text.append(names.get(0));
                        text.append(", ");
                    }
                }
                for (Procedure procedure: procedures) {
                    if (!isAnonymousCommand(procedure.getProcedureNames().get(0))) {
                        text.append("starte ");
                        text.append(procedure.getProcedureNames().get(0));
                        text.append(", ");
                    }
                }

                String textOut = text.toString();
                textOut = textOut.substring(0, text.length() - 2);
                final int pos = textOut.lastIndexOf(",");
                textOut = textOut.substring(0, pos) + " und" +
                        textOut.substring(pos + 1);
                speak(textOut);

                return true;
            }
            return false;
        }

        @Override
        public void onCommandNotFound(String mostLikelyCommand, List<String> possibleCommands) {
            //if this is empty the method given by the calling class applies
        }
    };


    @SuppressLint("ClickableViewAccessibility")
    public VoiceControl (@NotNull final Context owner,
                         @NotNull RecognitionViewModel recognitionResultVM) {
        this.owner = owner;
        this.ownerActivity = (Activity) owner;
        this.recognitionResultVM = recognitionResultVM;
        /*
        final Locale vILocale = new Locale("de", "DE");

        voiceInput = new VoiceInput(ownerActivity, ownerActivity,
                recognitionResultVM, vILocale, false);
         */

        TinyDB tb = new TinyDB(ownerActivity);

        keyword = tb.getString("keyword");
        Log.e("keyword", keyword);

        MediaPlayer mediaPlayer = new MediaPlayer();
        mediaPlayer.setVolume(50, 50);

        startableActivities.put("ein quiz", QuizSettings.class);
        startableActivities.put("die übersicht", CategoryOverview.class);

        this.originalLayoutID = this.ownerActivity.findViewById(android.R.id.content).getRootView().getId();
        this.ownerActivity.setContentView(R.layout.visually_impaired_mode_screen);

        this.rootLayout = ownerActivity.findViewById(R.id.visually_impaired_mode_root);
        final Context dialogContext = this.owner;
        this.rootLayout.setOnLongClickListener(v -> {
            AlertDialog.Builder exitModeDialog = new AlertDialog.Builder(dialogContext);
            exitModeDialog.setTitle("Hinweis");
            exitModeDialog.setMessage("Wollen Sie den Blindenmodus wirklich beenden?");
            exitModeDialog.setPositiveButton("OK", (dialog, which) -> {
                setOnCommandListener(null);
                tb.putBoolean("visually_impaired", false);
                //dialogOwnerActivity.setContentView(dialogOriginalLayoutID);
                ownerActivity.recreate();
                dialog.cancel();
            });
            exitModeDialog.setNegativeButton("Abbrechen", (dialog, which) -> dialog.cancel());
            exitModeDialog.show();
            return true;
        });

        Objects.requireNonNull(this.recognitionResultVM.getCurrentResult()).observeForever(
                result -> {
                    if (result != null) {
                        if (!procedureRunning) {
                            recognitionResultVM.setCurrentResult(null);
                            Log.e("result", result.toString());
                            boolean executed = false;

                            //check for activation word if this mods is set in the settings
                            if (tb.getBoolean("")) result = checkForCommands(result);

                            if (!result.isEmpty()) {
                                for (String possibility: result) {
                                    Log.e("checking", possibility);

                                    if (possibility.toLowerCase().startsWith("starte ") ||
                                        possibility.toLowerCase().startsWith("öffne ")) {

                                        final String name = possibility.toLowerCase()
                                                .replace("starte ", "")
                                                .replace("öffne ", "");

                                        if (startableActivities.containsKey(name)) {
                                            speak(String.format("%s wird geöffnet.", name));
                                            Intent startIntent = new Intent(owner,
                                                    startableActivities.get(name));
                                            ownerActivity.startActivity(startIntent);
                                            executed = true;
                                        } else {
                                            Log.e("searching procedure", name);
                                            for (Procedure procedure : procedures) {
                                                if (procedure.getProcedureNames().contains(name)) {
                                                    executeProcedure(procedure);
                                                    executed = true;
                                                }
                                            }
                                        }
                                    } else {
                                        if (commandListener.onCommand(possibility.toLowerCase())) {
                                            executed = true;
                                            break;
                                        } else if (defaultMethods.onCommand(possibility.toLowerCase())) {
                                            executed = true;
                                            break;
                                        }
                                    }
                                }
                            }

                            if (!executed) {
                                commandListener.onCommandNotFound(result.get(0), result);
                            }
                        } else {
                            recognitionResultVM.setCurrentResult(null);
                            final ArrayList<String> questions = activeProcedure.getQuestions();
                            String questionToReplace = questions.get(runningIndex)
                                    .replace("?", "");
                            char[] chars = questionToReplace.toCharArray();
                            chars[0] = Character.toLowerCase(chars[0]);
                            questionToReplace = new String(chars);
                            final String answer = result.get(0).replace(
                                    questionToReplace,
                                    "");
                            Log.e("procedure answer " + runningIndex, answer);
                            boolean terminated = false;

                            if (isInterruptionCalled(activeProcedure.getInterrupts(), answer)) {
                                Log.e("interruption", "called");
                                ProcedureInterrupt interruptionToExecute = null;
                                for (ProcedureInterrupt interruption: activeProcedure.getInterrupts()) {
                                    Log.e("check for call", interruption.getInterruptNames().toString());
                                    if (containsIgnoreCase(interruption.getInterruptNames(), answer))
                                        Log.e("found interruption", interruption.getInterruptNames().toString());
                                        interruptionToExecute = interruption;
                                }
                                assert interruptionToExecute != null;
                                InterruptExecutor.executeInterruption(interruptionToExecute, runningIndex);
                                ask(runningIndex, activeProcedure);
                            } else {
                                if (activeProcedure.allowTermination() && (result.contains("Abbruch")
                                        || result.contains("abbrechen") || result.contains("beenden"))) {
                                    terminated = true;
                                    Log.e("procedure", "terminated");
                                }

                                //final String answer = String.valueOf(result.get(0).toLowerCase().charAt(0));
                                if (activeProcedure.isAnswerValid(runningIndex,
                                        questions.get(runningIndex), answer) || terminated) {
                                    procedureVM.addAnswer(answer);
                                    activeProcedure.onAnswer(runningIndex, questions.get(runningIndex),
                                            answer);

                                    Log.e("procedure", String.valueOf(terminated));

                                    if (terminated) {
                                        procedureVM.setTerminated(true);
                                    } else {
                                        Log.e("procedure", "not terminated call index "
                                                + runningIndex);
                                        if (runningIndex < questions.size() - 1) {
                                            ask(runningIndex + 1, activeProcedure);
                                        }
                                        else {
                                            Log.e("procedure", "compleated");
                                            procedureVM.setCompleted(true);
                                            procedureRunning = false;
                                            activeProcedure = null;
                                        }
                                    }
                                } else {
                                    speak("Die gegebene Antwort ist ungültig.",
                                            () -> VoiceControl.this.ask(runningIndex, activeProcedure));
                                }
                            }
                        }
                    }
                }
        );

        gestureDetector = new GestureDetector(owner, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                Log.e("TEST", "onDoubleTap");
                listen();
                return super.onDoubleTap(e);
            }
        });

        this.rootLayout.setOnTouchListener((v, event) -> {
            v.performClick();
            gestureDetector.onTouchEvent(event);
            return false;
        });

        List<HashMap<Integer, String>> textsToRead = findTextToRead();
        Log.e("textToRead", textsToRead.toString());

    }

    public void setOnCommandListener(@Nullable OnCommandListener commandListener) {
        this.commandListener = commandListener;
        //TODO: If necessary:
        //TODO: override default methods by deleting them from the HashMap if there is an equivalent
    }

    public void addProcedure(@NotNull final Procedure procedure) {
        procedures.add(procedure);
        for (Procedure procd: this.procedures) {
            Log.e("Procedure", procd.getProcedureNames().get(0));
        }
    }

    public void listen() {
        final Locale locale = new Locale("de", "DE");
        listen(locale);
    }

    public void listen(@NotNull final Locale locale) {
        if (this.commandListener != null) {
            Log.e("VoiceControl", "listen");

            final VoiceInput voiceInput = new VoiceInput(ownerActivity, ownerActivity,
                    recognitionResultVM, locale, false);

            voiceInput.listen();

        }
    }

    @SuppressLint("ClickableViewAccessibility")
    public void detectGestures(@NotNull final GestureDetectorCompat gestureDetectorCompat) {
        gestureDetectorCompat.setOnDoubleTapListener(this);
    }

    @Nullable
    private String checkForCommand(@NotNull final String result) {
        final String lowerResult = result.toLowerCase();
        if (lowerResult.split(" ")[0].toLowerCase().equals(keyword.toLowerCase()))
            return lowerResult.replace(String.format("%s ", keyword.toLowerCase()), "");
        else return null;
    }

    //TODO: This method is depreciated if it's still useless, delete it.
    @NotNull
    private List<String> checkForCommands(@NotNull List<String> results) {
        List<String> possibleCommands = new ArrayList<>();
        for (int i = 0; i < results.size(); i++) {
            String result = results.get(i);
            if (result.split(" ")[0].equalsIgnoreCase(keyword)) {
                possibleCommands.add(result.replace(
                        String.format("%s ", keyword.toLowerCase()), ""));
            }
        }
        return possibleCommands;
    }

    //TODO: Use this method to detect annotated commands.
    @NotNull
    private OnCommandListener findCommandMethods() {
        this.commandAndMethod = new HashMap<>();
        OnCommandListener onCommandListener;
        Method[] methods = this.owner.getClass().getMethods();
        for (Method method: methods) {
            if (method.isAnnotationPresent(DoOnVoiceCommand.class)) {
                DoOnVoiceCommand doOnVoiceCommand = method.getAnnotation(DoOnVoiceCommand.class);
                assert doOnVoiceCommand != null;
                String command = doOnVoiceCommand.commands()[0];
                if (command.equals("")) {
                    command = method.getName();
                }
                this.commandAndMethod.put(command, method.getName());
            }
        }

        onCommandListener = new OnCommandListener() {
            @Override
            public ArrayList<ArrayList<String>> getAvailableCommands() {
                //TODO: Add code to provide the names and the description for the available commands
                //TODO: The description should be provided within the annotation
                return null;
            }

            @Override
            public boolean onCommand(String command) {
                if (VoiceControl.this.commandAndMethod.containsKey(command)) {
                    try {
                        Method method = VoiceControl.this.owner.getClass().getMethod(
                                Objects.requireNonNull(VoiceControl.this.commandAndMethod.get(
                                        command.toLowerCase())));
                        method.invoke(VoiceControl.this.owner);
                    } catch (NoSuchMethodException | IllegalAccessException |
                            InvocationTargetException | SecurityException e) {
                        e.printStackTrace();
                    }
                    return true;
                } else {
                    return false;
                }
            }

            @Override
            public void onCommandNotFound(String mostLikelyCommand, List<String> possibleCommands) {
                VoiceControl.this.informCommandNotFound();
            }
        };

        return onCommandListener;
    }

    //TODO: Use this method to detect annotated texts to read out.
    @NotNull
    private List<HashMap<Integer, String>> findTextToRead() throws IllegalArgumentException {
        HashMap<Integer, String> textsGerman = new HashMap<>();
        HashMap<Integer, String> textsEnglish = new HashMap<>();

        Field[] fields = this.owner.getClass().getFields();
        Log.e("field count", String.valueOf(fields.length));
        for (Field field : fields) {
            Log.e("filed", field.toString());
            if (field.isAnnotationPresent(ReadOut.class)) {
                Log.e("annotated", field.toString());
                ReadOut readOut = field.getAnnotation(ReadOut.class);
                assert readOut != null;
                if (readOut.language().equals("de-DE")) {
                    try {
                        textsGerman.put(readOut.position(), getTextFromField(field));
                    } catch (IllegalAccessException e) {
                        textsGerman.put(readOut.position(), "");
                        e.printStackTrace();
                    }
                } else if (readOut.language().equals("en-EN")) {
                        try {
                            textsEnglish.put(readOut.position(), getTextFromField(field));
                        } catch (IllegalAccessException e) {
                            textsEnglish.put(readOut.position(), "");
                            e.printStackTrace();
                        }
                } else {
                    throw new IllegalArgumentException(
                            "An illegal argument was found for the language");
                }
            }
        }
        List<HashMap<Integer, String>> texts = new ArrayList<>();
        texts.add(textsGerman);
        texts.add(textsEnglish);
        return texts;
    }

    private String getTextFromField(@NotNull Field field) throws IllegalAccessException {
        String text;
        if (field.getType().isAssignableFrom(String.class)) {
            text = (String) field.get(field);

        } else if (field.getType().isAssignableFrom(TextView.class)) {
            text = (String) ((TextView) Objects.requireNonNull(field.get(field))).getText();
        } else {
            text = "";
        }
        return text;
    }

    private void informError() {
        textToSpeech = new TextToSpeech(owner, status -> {
            if (status == TextToSpeech.SUCCESS) {
                Log.e("TTS", "initialized");
                textToSpeech.setLanguage(Locale.GERMANY);
                textToSpeech.speak("Es ist ein Fehler aufgetreten.",
                        TextToSpeech.QUEUE_FLUSH, null,
                        TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID);
                textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onStart(String utteranceId) {
                        Log.e("utterance","synthesis started");
                    }

                    @Override
                    public void onDone(String utteranceId) {
                        Log.e("utterance","synthesis done");
                        textToSpeech.shutdown();
                    }

                    @Override
                    public void onError(String utteranceId) {
                        Log.e("utterance","synthesis error");
                    }
                });
            } else {
                Log.e("TTS", "error");
            }
        });


    }

    private void ask(final int index, @NotNull final Procedure procedure) {
        final ArrayList<String> questions = procedure.getQuestions();

        Locale localeSpeak;

        if (procedure.getTransitionFromTo() == QuizSettings.ENG_TO_DE) {
            localeSpeak = new Locale("en", "US");
        } else {
            localeSpeak = new Locale("de", "DE");
        }
        speak(questions.get(index), () -> {
            Log.e("speaking", "ended");

            Locale locale;

            if (procedure.getTransitionFromTo() == QuizSettings.DE_TO_ENG) {
                locale = new Locale("en", "US");
            } else {
                locale = new Locale("de", "DE");
            }
            final VoiceInput voiceInput = new VoiceInput(ownerActivity, ownerActivity,
                    recognitionResultVM, locale, false);
            voiceInput.listen();
        }, localeSpeak);

        runningIndex = index;
        Log.e("asking", questions.get(index));

        /*
        final VoiceInput voiceInput = new VoiceInput(ownerActivity, ownerActivity,
                recognitionResultVM, locale, false);
        voiceInput.listen();
         */

        /*
        this.speechSynthesisVM = new ViewModelProvider(
                (ViewModelStoreOwner) owner).get(SpeechSynthesisViewModel.class);
        Objects.requireNonNull(this.speechSynthesisVM.isDone()).observeForever(done -> {
            Log.e("observer", "called");
            if (done != null) {
                Log.e("observer", "synthesis" + done);
                if (done) {
                    final VoiceInput voiceInput = new VoiceInput(ownerActivity, ownerActivity,
                            recognitionResultVM, locale, false);
                    voiceInput.listen();
                    Objects.requireNonNull(speechSynthesisVM.isDone()).removeObservers((
                            LifecycleOwner) ownerActivity);
                }
            }
        });
        speak(questions.get(index), true);
         */

        /*
        textToSpeech = new TextToSpeech(owner, status -> {
            if (status == TextToSpeech.SUCCESS) {
                Log.e("TTS", "initialized");

                textToSpeech.setLanguage(Locale.GERMANY);
                textToSpeech.speak(questions.get(index),
                        TextToSpeech.QUEUE_FLUSH, null,
                        TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID);
                textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onStart(String utteranceId) {
                        Log.e("utterance","synthesis started");
                    }

                    @Override
                    public void onDone(String utteranceId) {
                        Log.e("utterance","synthesis done");
                        textToSpeech.shutdown();
                        final VoiceInput voiceInput = new VoiceInput(ownerActivity, ownerActivity,
                                recognitionResultVM, locale, false);
                        voiceInput.listen();
                    }

                    @Override
                    public void onError(String utteranceId) {
                        Log.e("utterance","synthesis error");
                    }
                });
            } else {
                Log.e("TTS", "error");
            }
        });
         */

        /*
        Objects.requireNonNull(recognitionResultVM.getCurrentResult())
                .removeObservers((LifecycleOwner) ownerActivity);

         */


        /*
        Objects.requireNonNull(recognitionResultVM.getCurrentResult()).observeForever(
                result -> {
                    Log.e("Observer called", "3");
                    if (result != null) {
                        recognitionResultVM.setCurrentResult(null);
                        Log.e("procedure answer " + index, result.get(0));
                        boolean terminated = false;


                        if (procedure.allowTermination() && (result.contains("abbruch")
                                || result.contains("abbrechen") || result.contains("beenden"))) {
                                    terminated = true;
                                    Log.e("procedure", "terminated");
                        }


                        //final String answer = String.valueOf(result.get(0).toLowerCase().charAt(0));

                        procedureVM.addAnswer(result.get(0));
                        procedure.onAnswer(index, questions.get(index), result.get(0));

                        Log.e("procedure", String.valueOf(terminated));

                        if (terminated) {
                            procedureVM.setTerminated(true);
                        } else {
                            Log.e("procedure", "not terminated call index " + index);
                            if (index < questions.size()) ask(index + 1, procedure);
                            else {
                                Log.e("procedure", "compleated");
                                procedureVM.setCompleted(true);
                            }
                        }
                    }
                });
         */


    }

    private void executeProcedure(@NotNull final Procedure procedure) {
        Log.e("executing", procedure.getProcedureNames().get(0));
        activeProcedure = procedure;
        procedureRunning = true;
        procedureVM = new ViewModelProvider((ViewModelStoreOwner) owner)
                .get(ProcedureViewModel.class);

        ask(0, procedure);

        Objects.requireNonNull(procedureVM.isCompleted()).observe((LifecycleOwner) ownerActivity,
                completed -> {
            if (completed != null) {
                if (completed) {
                    final ArrayList<String> answersToQuestions = Objects.requireNonNull(
                            procedureVM.getAnswers()).getValue();
                    assert answersToQuestions != null;
                    final boolean success = procedure.onAnswersAvailable(answersToQuestions);
                    if (!success && procedure.redo()) {
                        informError();
                        executeProcedure(procedure);
                    }
                    procedureVM.clearVM();
                    Objects.requireNonNull(procedureVM.isCompleted()).removeObservers(
                            (LifecycleOwner) ownerActivity);
                }
            }

        });
        Objects.requireNonNull(procedureVM.isTerminated()).observe((LifecycleOwner) ownerActivity,
                terminated -> {
            if (terminated != null) {
                if (terminated) {
                    Objects.requireNonNull(procedureVM.isCompleted()).removeObservers(
                            (LifecycleOwner) ownerActivity);
                    procedureVM.clearVM();
                }
            }
        });
    }

    public boolean executeProcedureWithName(@NotNull final String procedureName) {
        Procedure procedureToExecute = null;
        for (Procedure procedure : procedures) {
            if (containsIgnoreCase(procedure.getProcedureNames(), procedureName))
                procedureToExecute = procedure;
        }

        if (procedureToExecute != null) {
            executeProcedure(procedureToExecute);
            return true;
        }
        else
            return false;
    }

    private boolean isInterruptionCalled(@NotNull final ArrayList<ProcedureInterrupt> interruptions,
                                      @NotNull final String recognitionResult) {
        for (ProcedureInterrupt interruption: interruptions) {
            if(containsIgnoreCase(interruption.getInterruptNames(), recognitionResult)) return true;
        }

        return false;
    }

    public static boolean containsIgnoreCase(@NotNull final ArrayList<String> items,
                                      @NotNull final String target) {
        for (String item : items) {
            if (item.equalsIgnoreCase(target)) return true;
        }
        return false;
    }

    public void informCommandNotFound() {
        textToSpeech = new TextToSpeech(owner, status -> {
            if (status == TextToSpeech.SUCCESS) {
                Log.e("TTS", "initialized");
                textToSpeech.setLanguage(Locale.GERMANY);
                textToSpeech.speak("Kommando nicht gefunden",
                        TextToSpeech.QUEUE_FLUSH, null,
                        TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID);
                textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onStart(String utteranceId) {
                        Log.e("utterance","synthesis started");
                    }

                    @Override
                    public void onDone(String utteranceId) {
                        Log.e("utterance","synthesis done");
                        textToSpeech.shutdown();
                    }

                    @Override
                    public void onError(String utteranceId) {
                        Log.e("utterance","synthesis error");
                    }
                });
            } else {
                Log.e("TTS", "error");
            }
        });


    }

    public boolean isAnonymousCommand(@NonNull String command) {
        return command.startsWith("<!--");
    }

    public void informHelpNeeded() {
        textToSpeech = new TextToSpeech(owner, status -> {
            if (status == TextToSpeech.SUCCESS) {
                Log.e("TTS", "initialized");
                textToSpeech.setLanguage(Locale.GERMANY);
                textToSpeech.speak("Bitte lassen Sie sich von einem Sehenden helfen.",
                        TextToSpeech.QUEUE_FLUSH, null,
                        TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID);
                textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onStart(String utteranceId) {
                        Log.e("utterance","synthesis started");
                    }

                    @Override
                    public void onDone(String utteranceId) {
                        Log.e("utterance","synthesis done");
                        textToSpeech.shutdown();
                    }

                    @Override
                    public void onError(String utteranceId) {
                        Log.e("utterance","synthesis error");
                    }
                });
            } else {
                Log.e("TTS", "error");
            }
        });


    }

    public void informVoiceControlActive() {
        Toast.makeText(owner, "Sprachsteuerung aktiv", Toast.LENGTH_SHORT).show();

        textToSpeech = new TextToSpeech(owner, status -> {
            if (status == TextToSpeech.SUCCESS) {
                Log.e("TTS", "initialized");
                textToSpeech.setLanguage(Locale.GERMANY);
                textToSpeech.speak("Sprachsteuerung aktiv",
                        TextToSpeech.QUEUE_ADD, null,
                        TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID);
                textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onStart(String utteranceId) {
                        Log.e("utterance","synthesis started");
                    }

                    @Override
                    public void onDone(String utteranceId) {
                        Log.e("utterance","synthesis done");
                        textToSpeech.shutdown();
                    }

                    @Override
                    public void onError(String utteranceId) {
                        Log.e("utterance","synthesis error");
                    }
                });
            } else {
                Log.e("TTS", "error");
            }
        });

    }

    public void speak(@NotNull final String text) {
        speak(text, null);
    }

    public void speak(@NotNull final String text,
                      @Nullable final OnTTSCompletedListener onTTSCompletedListener) {
        speak(text, onTTSCompletedListener, Locale.GERMANY);
    }

    public void speak(@NotNull final String text,
                      @Nullable final OnTTSCompletedListener onTTSCompletedListener,
                      @NotNull final Locale locale) {
        textToSpeech = new TextToSpeech(owner, status -> {
            if (status == TextToSpeech.SUCCESS) {
                Log.e("TTS", "initialized");
                textToSpeech.setLanguage(locale);
                textToSpeech.speak(text,
                        TextToSpeech.QUEUE_ADD, null,
                        TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID);
                textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onStart(String utteranceId) {
                        Log.e("utterance","synthesis started");
                    }

                    @Override
                    public void onDone(String utteranceId) {
                        Log.e("utterance","synthesis done");

                        if (onTTSCompletedListener != null) {

                            //vmCtx.speechSynthesisVM.setDone(true);
                            Log.e("executing", "Listener methode");
                            onTTSCompletedListener.onTTSCompleted();
                        }
                        textToSpeech.shutdown();
                    }

                    @Override
                    public void onError(String utteranceId) {
                        Log.e("utterance","synthesis error");
                    }
                });
            } else {
                Log.e("TTS", "error");
            }
        });
    }

    public void setProcedureRunning() {
        procedureRunning = true;
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {

    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        return false;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        Log.e("motion", "singleTap");
        return false;
    }

    @Override
    public boolean onDoubleTap(MotionEvent e) {
        Log.e("motion", "doubleTap");
        return false;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent e) {
        Log.e("motion", "doubleTapEvent");
        return false;
    }

}
