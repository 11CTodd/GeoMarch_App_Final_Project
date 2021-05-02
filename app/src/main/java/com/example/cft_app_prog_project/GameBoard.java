package com.example.cft_app_prog_project;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;

import com.google.firebase.auth.FirebaseUser;

import java.util.HashMap;

public class GameBoard extends AppCompatActivity implements FirestoreDB.OnGameUpdateListener, DeleteDialog.ButtonClickListener {
    private static Boolean player1 = false;
    private String uid = FirestoreDB.getInstance().getUser();
    private Boolean p1joined = false;
    private Boolean p2joined = false;
    private int piece;
    private Game thisGame;
    private HashMap<String, Integer> newMap;
    private boolean turn1 = true;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.game_board);
        FirestoreDB.getInstance().setOnGameUpdateListener(this);
    }

    private void showStatusUpdate(String status) {
        Toast.makeText(this, status, Toast.LENGTH_LONG).show();
    }

    private void setupGame() {
        player1 = true;
        ((ImageButton) findViewById(R.id.CircleButton)).setBackgroundColor(Color.RED);
        ((ImageButton) findViewById(R.id.SquareButton)).setBackgroundColor(Color.RED);
        ((ImageButton) findViewById(R.id.StarButton)).setBackgroundColor(Color.RED);
        p1joined = true;
        phase0();
    }
    // Disables buttons before game starts and while it's not the player's turn
    private void phase0(){
        ((ImageButton) findViewById(R.id.CircleButton)).setEnabled(false);
        ((ImageButton) findViewById(R.id.SquareButton)).setEnabled(false);
        ((ImageButton) findViewById(R.id.StarButton)).setEnabled(false);
        ((ImageButton) findViewById(R.id.grid51)).setEnabled(false);
        ((ImageButton) findViewById(R.id.grid52)).setEnabled(false);
        ((ImageButton) findViewById(R.id.grid53)).setEnabled(false);
        ((ImageButton) findViewById(R.id.grid54)).setEnabled(false);
        ((ImageButton) findViewById(R.id.grid55)).setEnabled(false);
        if (player1) ((TextView) findViewById(R.id.GameTicker)).setText(R.string.wait2);
        else ((TextView) findViewById(R.id.GameTicker)).setText(R.string.wait1);
    }
    // Player has to pick a piece to use
    private void phase1(){
        ((ImageButton) findViewById(R.id.CircleButton)).setEnabled(true);
        ((ImageButton) findViewById(R.id.SquareButton)).setEnabled(true);
        ((ImageButton) findViewById(R.id.StarButton)).setEnabled(true);
        ((TextView) findViewById(R.id.GameTicker)).setText(R.string.phase1);
    }
    public void choose(View view){
        int choice;
        if (player1) choice = 1;
        else choice = 4;
        if (view.getId() == R.id.StarButton) choice += 1;
        else if (view.getId() == R.id.SquareButton) choice += 2;
        piece = choice;
        phase2();
    }
    // Player has to pick a spot to put their piece
    private void phase2(){
        ((ImageButton) findViewById(R.id.grid51)).setEnabled(true);
        ((ImageButton) findViewById(R.id.grid52)).setEnabled(true);
        ((ImageButton) findViewById(R.id.grid53)).setEnabled(true);
        ((ImageButton) findViewById(R.id.grid54)).setEnabled(true);
        ((ImageButton) findViewById(R.id.grid55)).setEnabled(true);
        TextView ticker = ((TextView) findViewById(R.id.GameTicker));
        if (piece == 1 | piece == 4) ticker.setText(R.string.placeCircle);
        else if (piece == 2 | piece == 5) ticker.setText(R.string.placeStar);
        else if (piece == 3 | piece == 6) ticker.setText(R.string.placeSquare);
        else ticker.setText(R.string.choiceError);
    }
    public void place(View view){
        int spot;
        if (player1) spot = 50;
        else spot = 10;
        switch(view.getId()){
            case R.id.grid51:
                spot += 1;
                break;
            case R.id.grid52:
                spot += 2;
                break;
            case R.id.grid53:
                spot += 3;
                break;
            case R.id.grid54:
                spot += 4;
                break;
            case R.id.grid55:
                spot += 5;
                break;
            default:
                break;
        }
        Log.d("Game.place", "Placed " + piece + " at " + spot);
        newMap = GameUtils.advance(thisGame, spot, piece);
    }
    public HashMap<String, Integer> flip(){
        HashMap<String, Integer> board = thisGame.getBoardState();
        HashMap<String, Integer> newBoard = new HashMap<>();
        int row = 50;
        for (int i = 10; i <= 50; i = i + 10){
            for (int j = 1; j <= 5; j++){
                String key = String.valueOf(row+j);
                newBoard.put(key, board.get(String.valueOf(i+j)));
            }
            row -= 10;
        }
        return newBoard;
    }
    public void placePieces(HashMap<String, Integer> board){
        for (int i = 10; i <= 50; i = i + 10){
            for (int j = 1; j <= 5; j++){
                int thisPiece = board.get(String.valueOf(i+j));
                Log.d("GameBoard.placePieces", "Piece = " + thisPiece);
                int pic;
                switch(thisPiece){
                    case 1:
                        pic = R.drawable.ic_p1_circle;
                        break;
                    case 2:
                        pic = R.drawable.ic_p1_star;
                        break;
                    case 3:
                        pic = R.drawable.ic_p1_square;
                        break;
                    case 4:
                        pic = R.drawable.ic_p2_circle;
                        break;
                    case 5:
                        pic = R.drawable.ic_p2_star;
                        break;
                    case 6:
                        pic = R.drawable.ic_p2_square;
                        break;
                    case 0:
                    default:
                        pic = android.R.color.background_light;
                        break;
                }
                String id = "grid" + (i + j);
                ImageButton space = ((ImageButton) findViewById(getResources().getIdentifier(id, "id", getPackageName())));
                space.setImageResource(pic);
            }
        }
    }

    public static boolean getPlayer(){
        return player1;
    }

    @Override
    public void onUpdate(FirestoreDB.RESULT_CODE resultCode, String status, Game game) {
        switch (resultCode) {
            case STARTED:
                if (uid.equals(game.getUser1ID()) && !p1joined) {
                    setupGame();
                    thisGame = game;
                    showStatusUpdate("Created game");
                }
                else {
                    showStatusUpdate("Started game");
                    p2joined = true;
                    phase0();
                }
                break;
            case PLAYER1_ACTION:
                Log.d("GameBoard", "PLAYER1_ACTION received.");
                thisGame = game;
                if (!player1 && p2joined) {
                    if (!turn1) {
                        showStatusUpdate("Player 1 made their move.");
                        phase1();
                        HashMap<String, Integer> board = flip();
                        placePieces(board);
                    }
                    else {
                        turn1 = false;
                        FirestoreDB.getInstance().update(thisGame.getBoardState());
                    }
                }
                break;
            case PLAYER2_ACTION:
                Log.d("GameBoard", "PLAYER2_ACTION received.");
                if (player1 && p2joined) {
                    showStatusUpdate("Player 2 made their move.");
                    phase1();
                    placePieces(thisGame.getBoardState());
                }
                else {
                    showStatusUpdate("Game update received");
                    if (p1joined) p2joined = true;
                }
                break;
            case ERROR:
                showStatusUpdate("Failure: " + status);
                finish();
                break;
        }
    }

    @Override
    public void onBackPressed(){
        DialogFragment dialogFragment = new DeleteDialog(this);
        dialogFragment.show(getSupportFragmentManager(), "deleteDialog");
    }

    public void onButtonClick(){

    }
}
