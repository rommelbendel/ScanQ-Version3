package com.rommelbendel.scanQ.speechrecognition;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class SpeechRecognitionException extends RuntimeException {
    @NonNull
    @Override
    public synchronized Throwable initCause(@Nullable Throwable cause) {
        return super.initCause(cause);
    }

    @Nullable
    @Override
    public String getMessage() {
        return super.getMessage();
    }
}
