package com.rommelbendel.scanQ.onlineQuiz;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.rommelbendel.scanQ.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import static androidx.lifecycle.Lifecycle.State.STARTED;

public class OnlineLobbyWaiting extends AppCompatActivity {

    private MaterialButton start;
    CardView content;
    ConstraintLayout allLeft;
    LottieAnimationView countdown;
    private TableLayout players, info;
    private TableRow p1, p2, p3, p4, p5, p6, p7, p8, p9, p10;
    private TextView lobby, leftText;

    private Bundle extras;
    private String enterCode, quizType;
    private String id;
    private boolean isAdmin = false;

    private String username;
    private List<TableRow> pList;
    private ArrayList<String> playerList = new ArrayList<>();

    private boolean started = false;
    private boolean lobbyEmpty = false;
    private String quizName = " lädt...";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_online_lobby);
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        extras = getIntent().getExtras();

        content = findViewById(R.id.contet_online_lobby);
        allLeft = findViewById(R.id.players_left_lobby);
        leftText = findViewById(R.id.player_left_text);
        countdown = findViewById(R.id.left_countdown);
        info = findViewById(R.id.info_online_lobby);
        lobby = findViewById(R.id.lobby_name);
        players = findViewById(R.id.players);
        start = findViewById(R.id.start_online_quiz);
        start.setVisibility(View.GONE);
        p1 = findViewById(R.id.p1);
        p2 = findViewById(R.id.p2);
        p3 = findViewById(R.id.p3);
        p4 = findViewById(R.id.p4);
        p5 = findViewById(R.id.p5);
        p6 = findViewById(R.id.p6);
        p7 = findViewById(R.id.p7);
        p8 = findViewById(R.id.p8);
        p9 = findViewById(R.id.p9);
        p10 = findViewById(R.id.p10);
        p5.setVisibility(View.GONE);
        p6.setVisibility(View.GONE);
        p7.setVisibility(View.GONE);
        p8.setVisibility(View.GONE);
        p9.setVisibility(View.GONE);
        p10.setVisibility(View.GONE);
        pList = Arrays.asList(p1,p2,p3,p4,p5,p6,p7,p8,p9,p10);

        if (extras != null) {
            enterCode = extras.getString("enter_code");
            username = extras.getString("playerName");
            id = extras.getString("id");
        } else {
            finish();
        }
        DatabaseReference onlineQuizRef = database.getReference(enterCode);

        final Intent[] intent = new Intent[1];
        onlineQuizRef.child("Quiz Type").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                quizType = snapshot.getValue(String.class);

                switch (Objects.requireNonNull(quizType)) {
                    case "Eingabe Deu":
                    case "Eingabe Eng":
                        intent[0] = new Intent(OnlineLobbyWaiting.this, OnlineEingabeQuiz.class);
                        //quizName = "Eingabe Quiz";
                        break;

                    case "Multiple Choice Deu":
                    case "Multiple Choice Eng":
                        intent[0] =  new Intent(OnlineLobbyWaiting.this, OnlineMultipleChoice.class);
                        //quizName = "Multiple Choice";
                        break;

                    case "Sprachquiz":
                        intent[0] =  new Intent(OnlineLobbyWaiting.this, OnlineVoiceQuiz.class);
                        //quizName = "Voice Quiz";
                        break;

                    default:
                        throw new IllegalStateException("Unexpected value: " + quizType);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        onlineQuizRef.child("isStarted").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                started = snapshot.getValue(Boolean.class);

                if(started) {
                    for(int i=0; i<10; i++)  {
                        onlineQuizRef.child("done").child(String.valueOf(i)).setValue(false);
                    }
                    onlineQuizRef.child("restart").setValue(false);

                    extras.putStringArrayList("player", playerList);
                    extras.putBoolean("admin", isAdmin);

                    intent[0].putExtras(extras);
                    OnlineLobbyWaiting.this.startActivity(intent[0]);
                    OnlineLobbyWaiting.this.finish();
                }
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        onlineQuizRef.child("empty").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                lobbyEmpty = snapshot.getValue(Boolean.class);
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        onlineQuizRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int amount = ((ArrayList<String>) Objects.requireNonNull(snapshot.child("vocs").getValue())).size();

                ((TextView) ((TableRow) info.getChildAt(0)).getChildAt(1)).setText(String.format(" %s", enterCode));
                ((TextView) ((TableRow) info.getChildAt(1)).getChildAt(1)).setText(String.format(" %s Vokabeln", amount));
                ((TextView) ((TableRow) info.getChildAt(2)).getChildAt(1)).setText(String.format(" %s", quizType));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                HashMap<String, Object> activeMap = new HashMap<>();
                activeMap.put(String.valueOf(id), ServerValue.TIMESTAMP);

                if (OnlineLobbyWaiting.this.getLifecycle().getCurrentState().isAtLeast(STARTED))
                    onlineQuizRef.child("active").updateChildren(activeMap);

                if (lobbyEmpty) {
                    runOnUiThread(() -> {
                        allLeft.setVisibility(View.VISIBLE);
                        content.setVisibility(View.GONE);

                        if (!countdown.isAnimating())
                            countdown.playAnimation();
                        leftText.setText("Alle Spieler haben die Lobby verlassen");

                        new Handler().postDelayed(() -> {
                            OnlineLobbyWaiting.this.finish();
                            System.exit(0);
                        }, 2500);
                    });
                }

                if (!isNetworkAvailable()) {
                    runOnUiThread(() -> {
                        allLeft.setVisibility(View.VISIBLE);
                        content.setVisibility(View.GONE);
                        leftText.setText("Du hast kein Internet mehr");

                        if (!countdown.isAnimating())
                            countdown.playAnimation();

                        new Handler().postDelayed(() -> {
                            OnlineLobbyWaiting.this.finish();
                            System.exit(0);
                        }, 2500);
                        timer.cancel();
                    });
                }
            }
        }, 0, 5000);

        onlineQuizRef.child("player").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                //GenericTypeIndicator<ArrayList<String>> t = new GenericTypeIndicator<ArrayList<String>>() {};
                ArrayList<String> rawPlayer = (ArrayList<String>) snapshot.getValue();
                playerList.clear();
                for(String player : rawPlayer) {
                    if (!player.equals("0")) {
                        playerList.add(player);
                    }
                }

                //assert playerList != null;
                if(!playerList.isEmpty()) {
                    if (playerList.get(0).matches(".*(s|ß|z|x|ce)")) {
                        lobby.setText(String.format("%s' Lobby", playerList.get(0)));
                    } else {
                        lobby.setText(String.format("%ss Lobby", playerList.get(0)));
                    }
                }

                for(TableRow p : pList) {
                    if (playerList.size() > pList.indexOf(p)) {
                        int i = pList.indexOf(p);
                        p.setVisibility(View.VISIBLE);

                        if (playerList.get(i).equals(username))
                            ((TextView) p.getChildAt(1)).setTextColor(ContextCompat.getColor(OnlineLobbyWaiting.this, R.color.colorPrimary));
                        else
                            ((TextView) p.getChildAt(1)).setTextColor(Color.GRAY);

                        ((TextView) p.getChildAt(1)).setText(String.format(" %s", playerList.get(i)));
                        ((TextView) p.getChildAt(0)).setTextColor(Color.GRAY);

                    } else {
                        ((TextView) p.getChildAt(1)).setText(" leer");
                        ((TextView) p.getChildAt(1)).setTextColor(Color.parseColor("#E3E6E6"));
                        ((TextView) p.getChildAt(0)).setTextColor(Color.parseColor("#E3E6E6"));
                    }
                }
                if(playerList.indexOf(username) == 0)
                    isAdmin = true;
                else
                    isAdmin = false;

                if(playerList.size() > 1 && isAdmin) {
                    start.setVisibility(View.VISIBLE);
                } else {
                    start.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(OnlineLobbyWaiting.this, "Verbindungsfehler", Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        start.setOnClickListener(v -> {
            onlineQuizRef.child("isStarted").setValue(true);
            onlineQuizRef.child("restart").setValue(false);
        });
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
    }
}
