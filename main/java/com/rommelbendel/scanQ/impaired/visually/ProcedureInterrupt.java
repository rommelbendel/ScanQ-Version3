package com.rommelbendel.scanQ.impaired.visually;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public interface ProcedureInterrupt {
    @NotNull ArrayList<String> getInterruptNames();
    void onInterrupt(final int questionContextIndex);
}
