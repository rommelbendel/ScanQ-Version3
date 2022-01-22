package com.rommelbendel.scanQ.impaired.visually;

import java.util.ArrayList;
import java.util.List;

public interface OnCommandListener {
    ArrayList<ArrayList<String>> getAvailableCommands();
    boolean onCommand(String command);
    void onCommandNotFound(String mostLikelyCommand, List<String> possibleCommands);
}
