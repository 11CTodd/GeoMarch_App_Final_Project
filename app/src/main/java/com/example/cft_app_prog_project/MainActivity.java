package com.example.cft_app_prog_project;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity implements FirestoreDB.OnGameUpdateListener{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FirestoreDB.getInstance().authenticate(this, (success, status) -> {
            if (success) showStatusUpdate(status);
            else showStatusUpdate("Authentication to firebase failed.");
        });
        FirestoreDB.getInstance().setOnGameUpdateListener(this);
    }

    public void startGame(View view) {
        startActivity(new Intent(getApplicationContext(), GameBoard.class));
        FirestoreDB.getInstance().startGame();
    }

    public void joinGame(View view) {
        startActivity(new Intent(getApplicationContext(), GameBoard.class));
        FirestoreDB.getInstance().findGame();
    }

    public void settings(View view) {
        startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
    }
    //public void deleteGame(View view) {
    //    FirestoreDB.getInstance().quitCurrentGame();
    //}

    private void showStatusUpdate(String status) {
        Toast.makeText(this, status, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onUpdate(FirestoreDB.RESULT_CODE resultCode, String status, Game game) {
        switch (resultCode) {
            case STARTED:
                showStatusUpdate("Started game.");
                //setupGame(game);
                break;
            case PLAYER2_ACTION:

                break;
            case ERROR:
                showStatusUpdate("Failure: " + status);
                break;
        }
    }
}