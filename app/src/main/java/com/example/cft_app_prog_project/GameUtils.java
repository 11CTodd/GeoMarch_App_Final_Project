package com.example.cft_app_prog_project;

import android.util.Log;

import java.util.HashMap;

public class GameUtils {

    public static  HashMap<String, Integer> advance(Game game, int spot, int choice) throws NullPointerException {
        HashMap<String, Integer> boardState = game.getBoardState();
        try {
            for (int i = 10; i <= 40; i = i + 10) {
                for (int j = 1; j <= 5; j++) {
                    if (boardState.get(String.valueOf(i+j)) == 1 | boardState.get(String.valueOf(i+j)) == 2 | boardState.get(String.valueOf(i+j)) == 3) {
                        int piece = boardState.get(String.valueOf(i+j));
                        int next = (i + j) - 10;
                        if (next < 20) {
                            win(game, 1);
                        }
                        int atNext = boardState.get(String.valueOf(next));
                        if (atNext != 0) {
                            Log.d("GameUtil.advance", "Collision at " + next);
                            fight(game, next, piece, atNext);
                        } else {
                            boardState.put(String.valueOf(next), piece);
                            boardState.put(String.valueOf(i + j), 0);
                        }
                    } else if (boardState.get(String.valueOf(i+j)) == 4 | boardState.get(String.valueOf(i+j)) == 5 | boardState.get(String.valueOf(i+j)) == 6) {
                        int piece = boardState.get(String.valueOf(i+j));
                        int next = (i + j) + 10;
                        if (next > 50) {
                            win(game, 2);
                        }
                        int atNext = boardState.get(String.valueOf(next));
                        if (atNext != 0) {
                            Log.d("GameUtil.advance", "Collision at " + next);
                            fight(game, next, atNext, piece);
                        } else {
                            boardState.put(String.valueOf(next), piece);
                            boardState.put(String.valueOf(i+j), 0);
                        }
                    }
                }
            }
            Log.d("GameUtil.advance", "Placed " + choice + " at " + spot);
            boardState.put(String.valueOf(spot), choice);
            game.getBoardState().putAll(boardState);
        } catch (Exception e){
            Log.d("Game", "Failure while advancing pieces");
        }
        FirestoreDB.getInstance().update(boardState);
        return game.getBoardState();
    }
    public static void fight(Game game, int spot, int p1, int p2){
        int winner = 0;
        switch (p1){
            case 1:
                if (p2 == 4) winner = 0;
                else if (p2 == 5) winner = 1;
                else winner = 6;
                break;
            case 2:
                if (p2 == 4) winner = 4;
                else if (p2 == 5) winner = 0;
                else winner = 2;
                break;
            case 3:
                if (p2 == 4) winner = 3;
                else if (p2 == 5) winner = 5;
                else winner = 0;
            default:
                break;
        }
        Log.d("GameUtil.fight", winner + " won fight");
        game.getBoardState().put(String.valueOf(spot), winner);
    }
    public static void win(Game game, int winner){

    }
    public static void updateBoard(Game game, String uid, HashMap<String, Integer> newBoard){
        String user2ID = game.getUser2ID();
        if (uid.equals(user2ID)) {
            int row = 50;
            for (int i = 10; i <= 50; i = i + 10) {
                for (int j = 1; j <= 5; j++) {
                    game.getBoardState().put(String.valueOf(row + j), newBoard.get(String.valueOf(i+j)));
                }
                row -= 10;
            }
        }
        else game.getBoardState().putAll(newBoard);
        game.newTurn();
    }
}
