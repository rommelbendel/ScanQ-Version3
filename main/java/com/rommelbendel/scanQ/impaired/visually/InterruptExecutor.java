package com.rommelbendel.scanQ.impaired.visually;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public class InterruptExecutor {
    private static TextToSpeech tts;

    public static void executeInterruption(@NotNull final ProcedureInterrupt procedureInterrupt,
                                           final int questionContextIndex) {
        procedureInterrupt.onInterrupt(questionContextIndex);
    }

    public static boolean speak(@NotNull final String text, @NotNull final Locale locale, @Nullable final OnTTSCompletedListener ttsCompletedListener, @NotNull final Context context) {
        final boolean[] success = {true};
        final boolean[] finished = {false};
        tts = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(locale);
                tts.speak(text, TextToSpeech.QUEUE_ADD, null,
                        TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID);
                tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onStart(String utteranceId) {

                    }

                    @Override
                    public void onDone(String utteranceId) {
                        finished[0] = true;
                        tts.shutdown();
                    }

                    @Override
                    public void onError(String utteranceId) {
                        finished[0] = true;
                        success[0] = false;
                    }
                });
            }
        });

        //while (!finished[0]) {}
        while (tts.isSpeaking()){
            Log.e("tts.isSpeaking", String.valueOf(tts.isSpeaking()));
        }

        if (ttsCompletedListener != null)
            ttsCompletedListener.onTTSCompleted();

        return success[0];
    }

    public static boolean speak(@NotNull final String text, @NotNull final Locale locale, @NotNull final Context context) {
        return speak(text, locale,null, context);
    }

    public static boolean speak(@NotNull final String text, @NotNull final Context context) {
        return speak(text, Locale.GERMANY, context);
    }
}
