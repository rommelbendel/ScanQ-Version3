package com.rommelbendel.scanQ.onlineQuiz;

public class OnlinePlayer {

    private String playerName;
    private long points;
    private boolean done;
    private int active;

    //innovativer Konstruktoren
    public OnlinePlayer(String playerName, long points, boolean done, int active) {
        this.playerName = playerName;
        this.points = points;
        this.done = done;
        this.active = active;
    }

    //aktueller Konstruktor
    public OnlinePlayer(String playerName, long points) {
        this.playerName = playerName;
        this.points = points;
        this.done = false;
        this.active = 0;
    }

    public String getPlayerName() {
        return playerName;
    }
    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public long getPoints() {
        return points;
    }
    public void setPoints(long points) {
        this.points = points;
    }

    public boolean isDone() {
        return done;
    }
    public void setDone(boolean done) {
        this.done = done;
    }

    public int getActive() {
        return active;
    }
    public void setActive(int active) {
        this.active = active;
    }
}
