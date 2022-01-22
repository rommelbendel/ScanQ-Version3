package com.rommelbendel.scanQ.impaired.visually;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public interface Procedure {
    @NotNull ArrayList<String> getProcedureNames();
    @NotNull ArrayList<String> getQuestions();

    default @NotNull ArrayList<ProcedureInterrupt> getInterrupts() {
        return new ArrayList<>();
    }

    default int getTransitionFromTo() {return 0;}

    boolean redo(); //redo procedure if execution failed
    boolean allowTermination();

    default boolean isAnswerValid(final int index, @NotNull final String question,
                                   @NotNull final String answer) {
        return true;
    }

    void onAnswer(final int index, @NotNull final String question, @NotNull final String answer);
    boolean onAnswersAvailable(@NotNull final ArrayList<String> answers);
}
