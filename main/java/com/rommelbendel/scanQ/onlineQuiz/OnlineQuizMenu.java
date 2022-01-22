package com.rommelbendel.scanQ.onlineQuiz;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.rommelbendel.scanQ.R;
import com.rommelbendel.scanQ.additional.TinyDB;
import com.yuvraj.livesmashbar.enums.GravityView;
import com.yuvraj.livesmashbar.view.LiveSmashBar;

import java.util.ArrayList;
import java.util.Objects;

public class OnlineQuizMenu extends AppCompatActivity {

    private static final Bundle bundle = new Bundle();
    private MaterialButton join, create;
    private TextInputEditText inputCode;
    private CardView content;
    private TextView createText;
    private ConstraintLayout waiting;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_online_menu);

        TinyDB tb = new TinyDB(getApplicationContext());
        FirebaseDatabase database = FirebaseDatabase.getInstance();

        waiting = findViewById(R.id.waiting);
        content = findViewById(R.id.online_menu_content);
        join = findViewById(R.id.join);
        create = findViewById(R.id.create);
        createText = findViewById(R.id.text_view_lobby_erstellen);
        inputCode = findViewById(R.id.input_code);
        inputCode.setTransformationMethod(null);

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        float height = metrics.heightPixels / metrics.ydpi;
        float width = metrics.widthPixels / metrics.xdpi;
        double diagonalInches = Math.sqrt(width*width + height*height);

        if (diagonalInches >= 6.5){
            createText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        }

        join.setOnClickListener(v -> {
            if(isNetworkAvailable()) {
                if (!inputCode.getText().toString().isEmpty()) {
                    waiting.setVisibility(View.VISIBLE);
                    content.setVisibility(View.GONE);

                    final String entercode = inputCode.getText().toString();

                    DatabaseReference online_quiz_ref = database.getReference(entercode);
                    online_quiz_ref.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                boolean started = snapshot.child("isStarted").getValue(Boolean.class);
                                if(!started) {

                                    ArrayList<String> players = new ArrayList<>();
                                    ArrayList<String> rawPlayer = (ArrayList<String>) snapshot.child("player").getValue();
                                    assert rawPlayer != null;
                                    for(String player : rawPlayer) {
                                        if (!player.equals("0")) {
                                            players.add(player);
                                        }
                                    }

                                    if (players.size() < 10 && !players.isEmpty()) {
                                        String username = tb.getString("username");

                                        for(int i = 1; i <= 10; i++) {
                                            if (players.contains(username)) {
                                                if (!players.contains(username + " (" + i + ")")) {
                                                    username = username + " (" + i + ")";
                                                    break;
                                                }
                                            } else {
                                                break;
                                            }
                                        }

                                        for (int i = 0; i < 10; i++) {
                                            if (Objects.equals(snapshot.child("player").child(String.valueOf(i)).getValue(String.class), "0")) {
                                                online_quiz_ref.child("player").child(String.valueOf(i)).setValue(username);
                                                online_quiz_ref.child("active").child(String.valueOf(i)).setValue(System.currentTimeMillis());
                                                online_quiz_ref.child("done").child(String.valueOf(i)).setValue(false);
                                                online_quiz_ref.child("points").child(String.valueOf(i)).setValue(0);
                                                bundle.putString("id", String.valueOf(i));
                                                break;
                                            }
                                        }

                                        bundle.putString("enter_code", inputCode.getText().toString());
                                        bundle.putString("playerName", username);

                                        Intent intent = new Intent(OnlineQuizMenu.this, OnlineLobbyWaiting.class);
                                        intent.putExtras(bundle);
                                        startActivity(intent);

                                        waiting.setVisibility(View.GONE);
                                        content.setVisibility(View.VISIBLE);
                                    } else {
                                        waiting.setVisibility(View.GONE);
                                        content.setVisibility(View.VISIBLE);

                                        new LiveSmashBar.Builder(OnlineQuizMenu.this)
                                                .title("Die Lobby ist voll")
                                                .titleColor(Color.WHITE)
                                                .backgroundColor(Color.parseColor("#541111"))
                                                .gravity(GravityView.TOP)
                                                .duration(5000)
                                                .dismissOnTapOutside()
                                                .overlayBlockable()
                                                .showOverlay()
                                                .show();
                                    }
                                } else {
                                    waiting.setVisibility(View.GONE);
                                    content.setVisibility(View.VISIBLE);

                                    new LiveSmashBar.Builder(OnlineQuizMenu.this)
                                            .title("Das Spiel hat bereits angefangen")
                                            .titleColor(Color.WHITE)
                                            .backgroundColor(Color.parseColor("#541111"))
                                            .gravity(GravityView.TOP)
                                            .duration(5000)
                                            .dismissOnTapOutside()
                                            .overlayBlockable()
                                            .showOverlay()
                                            .show();
                                }
                            } else {
                                waiting.setVisibility(View.GONE);
                                content.setVisibility(View.VISIBLE);

                                new LiveSmashBar.Builder(OnlineQuizMenu.this)
                                        .title("Code nicht gefunden")
                                        .titleColor(Color.WHITE)
                                        .backgroundColor(Color.parseColor("#541111"))
                                        .gravity(GravityView.TOP)
                                        .duration(5000)
                                        .dismissOnTapOutside()
                                        .overlayBlockable()
                                        .showOverlay()
                                        .show();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {}
                    });
                } else {
                    new LiveSmashBar.Builder(this)
                            .title("Bitte gib einen Code ein")
                            .titleColor(Color.WHITE)
                            .backgroundColor(Color.parseColor("#541111"))
                            .gravity(GravityView.TOP)
                            .duration(3000)
                            .dismissOnTapOutside()
                            .overlayBlockable()
                            .showOverlay()
                            .show();
                }
            } else {
                new LiveSmashBar.Builder(this)
                        .title("Bitte überprüfe deine Internetverbindung")
                        .titleColor(Color.WHITE)
                        .backgroundColor(Color.parseColor("#541111"))
                        .gravity(GravityView.TOP)
                        .duration(3000)
                        .dismissOnTapOutside()
                        .overlayBlockable()
                        .showOverlay()
                        .show();
            }
        });

        create.setOnClickListener(v -> {
            Intent intent = new Intent(OnlineQuizMenu.this, OnlineCreateGame.class);
            startActivity(intent);
        });
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
    }
}
