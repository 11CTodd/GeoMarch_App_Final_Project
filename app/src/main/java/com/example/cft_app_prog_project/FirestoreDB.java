package com.example.cft_app_prog_project;
// Might've stolen a lot of this from your ESP game, sorry XD
import android.app.Activity;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class FirestoreDB {
    private static final String TAG = "FirestoreDB";

    public enum RESULT_CODE {
        STARTED, PLAYER1_ACTION, PLAYER2_ACTION, ERROR
    }
    public interface OnAuthenticatedListener {
        void onAuthenticated(boolean success, String status);
    }
    public interface OnGameUpdateListener {
        void onUpdate(RESULT_CODE resultCode, String status, Game game);
    }

    private static FirestoreDB INSTANCE;
    private FirebaseUser user;
    private FirebaseFirestore db;
    private DocumentReference currentGame;
    private ListenerRegistration currentGameListener;

    private OnGameUpdateListener listener;

    private FirestoreDB() {}

    public String getUser(){ return user.getUid(); }

    public static synchronized FirestoreDB getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new FirestoreDB();
        }
        return INSTANCE;
    }

    public void authenticate(Activity activity, final OnAuthenticatedListener listener) {
        if (user == null) {
            db = FirebaseFirestore.getInstance();

            final FirebaseAuth auth = FirebaseAuth.getInstance();
            auth.signInAnonymously()
                    .addOnCompleteListener(activity, task -> {
                        if (task.isSuccessful()) {
                            user = auth.getCurrentUser();
                            listener.onAuthenticated(true, "Logged in with id: " + user.getUid());
                        } else {
                            listener.onAuthenticated(false, null);
                        }
                    });
        }
        else {
            listener.onAuthenticated(true, "Already logged in with id: " + user.getUid());
        }
    }

    public void setOnGameUpdateListener(OnGameUpdateListener listener) {
        this.listener = listener;
    }

    // ********************************************************************
    // *     Create a new game: This whole thing needs to be changed!     *
    // ********************************************************************
    public void startGame() {
        Log.d(TAG, "Starting a new game...");
        Game game = new Game(user.getUid(),"");
        addGameToDB(game);
    }

    private void addGameToDB(Game game) {
        Log.d(TAG, "Adding a new game to the firestore database");
        final Map<String, Object> document = new HashMap<>();
        document.put("status", "open");
        document.put("game", game);
        document.put("start_timestamp", FieldValue.serverTimestamp());
        db.collection("games")
                .add(document)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "New game to the firestore database with ID: " + documentReference.getId());
                    setCurrentGame(documentReference);
                    listener.onUpdate(RESULT_CODE.STARTED, "", game);
                })
                .addOnFailureListener( (@NonNull Exception e) -> {
                    Log.d(TAG, "Adding a new game to the firestore database cause exception: " + e.getMessage());
                    listener.onUpdate(RESULT_CODE.ERROR, "Error adding game to firestore: " + e.getMessage(), null);
                });
    }

    private void setCurrentGame(DocumentReference document) {
        currentGame = document;
        if (currentGameListener != null) currentGameListener.remove();
        currentGameListener =
                document.addSnapshotListener((@Nullable DocumentSnapshot snapshot,
                                              @Nullable FirebaseFirestoreException e) -> {
                    if (e != null) {
                        Log.d(TAG, "Listening for updates to game caused an exception: " + e.getMessage());
                        listener.onUpdate(RESULT_CODE.ERROR, "Failed to register listener for game changes: " + e.getMessage(), null);
                        return;
                    }

                    if (snapshot != null && snapshot.exists()) {
                        Log.d(TAG, "Update to game occurred: " + snapshot.getData());
                        Game game = snapshot.get("game",Game.class);
                        if (game.getP1Turn()) listener.onUpdate(RESULT_CODE.PLAYER1_ACTION, "", game);
                        else listener.onUpdate(RESULT_CODE.PLAYER2_ACTION, "", game);
                    } else {
                        Log.d(TAG, "Update to game occurred, but sent a null update.");
                        listener.onUpdate(RESULT_CODE.ERROR, "Game ended", null);
                    }
                });

    }

    // Attempt to join an existing game
    public void findGame() {
        if (user == null) {
            Log.d(TAG, "Firestore user is null when trying to find game");
            listener.onUpdate(RESULT_CODE.ERROR, "No user logged in.", null);
            return;
        }

        Log.d(TAG, "Attempting to find open game");
        db.collection("games")
                .whereEqualTo("status", "open")
                .orderBy("start_timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult() != null && task.getResult().size() > 0) {
                            Log.d(TAG, "Game found.  Attempting to join game.");
                            DocumentSnapshot snapshot = task.getResult().getDocuments().get(0);
                            joinGame(snapshot.getReference());
                        }
                        else {
                            Log.d(TAG, "No open games found.");
                            listener.onUpdate(RESULT_CODE.ERROR, "No open games found.", null);
                        }
                    }
                });
    }

    // Attempt to join the game described by document
    private void joinGame(final DocumentReference document) {
        db.runTransaction( transaction -> {
            Log.d(TAG, "Starting transaction to join game.");
            DocumentSnapshot snapshot = transaction.get(document);
            Game game = snapshot.get("game",Game.class);
            if (snapshot.get("status").equals("open")) {
                transaction.update(document, "status", "started");
                game.setUser2ID(user.getUid());
                transaction.update(document, "game", game);
            }
            else {
                Log.d(TAG, "Status field is no longer equal to 'open'.  Cannot join this game.");
                throw new FirebaseFirestoreException("Game no longer open",FirebaseFirestoreException.Code.ABORTED);
            }
            return game;
        }).addOnSuccessListener(game-> {
            Log.d(TAG, "Successfully joined game.");
            setCurrentGame(document);
            listener.onUpdate(RESULT_CODE.STARTED, "", game);
        }).addOnFailureListener((@NonNull Exception e) -> {
            Log.d(TAG, "Transaction failed - could not join game.");
            listener.onUpdate(RESULT_CODE.ERROR, "Game state changed while transaction was executing", null);
        });
    }

    public void quitCurrentGame() {
        if (currentGame != null) {
            Log.d(TAG, "Deleting current game");
            currentGame.delete();
            currentGameListener.remove();
        }
        currentGame = null;
        currentGameListener = null;
    }

    // *********************************
    // *   This needs to be changed!   *
    // *********************************
    public void update(HashMap<String, Integer> newBoard) {
        if (currentGame == null) {
            Log.d(TAG, "Attempted to update game with no game in progress");
            listener.onUpdate(RESULT_CODE.ERROR, "No game in progress", null);
        }
        else {
            db.runTransaction( transaction -> {
                Log.d(TAG, "Starting a transaction to update the game board");
                DocumentSnapshot snapshot = transaction.get(currentGame);
                Game game = snapshot.get("game",Game.class);
                GameUtils.updateBoard(game, user.getUid(), newBoard);
                transaction.update(currentGame, "game", game);
                return null;
            }).addOnSuccessListener( aVoid -> {
                Log.d(TAG, "Successfully updated board");
                // Do nothing on success
            }).addOnFailureListener((@NonNull Exception e) -> {
                Log.d(TAG, "Unable to submit an update due to concurrent modification");
                listener.onUpdate(RESULT_CODE.ERROR, "Unable to update, maybe due to concurrent modification, but more likely because of a non-robust way of handling deleted games. To make this robust, you should try again.", null);
            });
        }
    }
}
