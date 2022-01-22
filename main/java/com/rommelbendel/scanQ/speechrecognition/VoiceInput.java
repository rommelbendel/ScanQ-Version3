package com.rommelbendel.scanQ.speechrecognition;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

public class VoiceInput extends BroadcastReceiver {
    private final Context context;
    private final Activity activity;
    private final RecognitionViewModel recognitionViewModel;
    private final Locale locale;
    private final boolean continuous;
    private static final int VOICE_RECOGNITION_REQUEST_CODE = 1234;


    public VoiceInput(@NotNull final Context context,
                      @NotNull final Activity activity,
                      @NotNull final RecognitionViewModel recognitionViewModel,
                      @NotNull final Locale locale, final boolean continuous) {
        this.context = context;
        this.activity = activity;
        this.recognitionViewModel = recognitionViewModel;
        this.locale = locale;
        this.continuous = continuous;
    }

    public void listen() {
        Log.e("VoiceInput", "listen");
        Intent languagesIntent = new Intent(RecognizerIntent.ACTION_GET_LANGUAGE_DETAILS);
        languagesIntent.setPackage("com.google.android.googlequicksearchbox");
        activity.sendOrderedBroadcast(languagesIntent, null,
                this, null, Activity.RESULT_OK, null,
                null);
        /*
        Objects.requireNonNull(recognitionViewModel.getSupportedLanguages()).observe(
                (LifecycleOwner) context, supportedLanguages -> {
            Log.e("languages available", supportedLanguages.toString());
            Log.e("language required", locale.toString().replace("_", "-"));
            if (supportedLanguages.contains(locale.toString().replace("_", "-"))) {
                Log.e("language check", "supported");
                recognitionViewModel.getSupportedLanguages().removeObservers(
                        (LifecycleOwner) context);
                Intent recognitionIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                recognitionIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,
                        locale.toString().replace("_", "-"));
                activity.startActivityForResult(recognitionIntent, VOICE_RECOGNITION_REQUEST_CODE);
                if (continuous) {
                    Objects.requireNonNull(recognitionViewModel.getCurrentResult()).observe(
                            (LifecycleOwner) context, results -> {
                                if (results != null) {
                                    listen();
                                    recognitionViewModel.getCurrentResult().removeObservers(
                                            (LifecycleOwner) context);
                                }
                            });
                }
            }
        });
         */

    }

    public static int getVoiceRecognitionRequestCode() {
        return VOICE_RECOGNITION_REQUEST_CODE;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.e("VoiceInput", "onReceive");
        Log.e("onReceive: intent", intent.toString());
        Bundle results = getResultExtras(true);
        Log.e("onReceive: results", results.toString());
        if (results.containsKey(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES)) {
            Log.e("onReceive", "contains SUPPORTED_LANGUAGES");
            /*
            recognitionViewModel.setSupportedLanguages(results.getStringArrayList(
                    RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES));
             */

            final List<String> supportedLanguages = results.getStringArrayList(
                    RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES);

            Log.e("Lokal", locale.toString());
            Log.e("supportedLanguages", supportedLanguages.toString());
            if (supportedLanguages.contains(locale.toString().replace("_", "-"))) {
                Log.e("language check", "supported");
                /*
                recognitionViewModel.getSupportedLanguages().removeObservers(
                        (LifecycleOwner) context);
                 */
                Intent recognitionIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                recognitionIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,
                        locale.toString().replace("_", "-"));
                activity.startActivityForResult(recognitionIntent, VOICE_RECOGNITION_REQUEST_CODE);
                /*
                if (continuous) {
                    Objects.requireNonNull(recognitionViewModel.getCurrentResult()).observe(
                            (LifecycleOwner) context, results -> {
                                if (results != null) {
                                    listen();
                                    recognitionViewModel.getCurrentResult().removeObservers(
                                            (LifecycleOwner) context);
                                }
                            });
                }
                 */
            }
        }
        if (results.containsKey(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE)) {
            Log.e("onReceive", "contains LANGUAGE_PREFERENCE");
        }
    }
}
