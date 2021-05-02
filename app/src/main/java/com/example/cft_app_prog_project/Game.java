package com.example.cft_app_prog_project;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Game {
    private String user1ID;
    private String user2ID;
    private HashMap<String, Integer> boardState; // 0 = empty, 1 = p1 circle, 2 = p1 star, 3 = p1 square, 4 = p2 circle, 5 = p2 star, 6 = p2 square
    private Boolean p1Turn = true;

    public Game() { }

    public Game(String user1ID, String user2ID) {
        this.user1ID = user1ID;
        this.user2ID = user2ID;
        this.boardState = new HashMap<>();
        for (int i = 10; i <= 50; i = i + 10) {
            for (int j = 1; j <= 5; j++) {
                boardState.put(String.valueOf(i + j), 0);
            }
        }
        p1Turn = true;
    }
    public boolean getP1Turn(){
        return p1Turn;
    }
    public boolean newTurn(){
        return !p1Turn;
    }
    public HashMap<String, Integer> getBoardState(){
        return boardState;
    }

    public String getUser1ID() {
        return user1ID;
    }

    public String getUser2ID() {
        return user2ID;
    }

    public void setUser2ID(String user2ID) {
        this.user2ID = user2ID;
    }
}