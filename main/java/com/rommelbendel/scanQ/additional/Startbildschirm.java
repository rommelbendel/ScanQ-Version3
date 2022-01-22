package com.rommelbendel.scanQ.additional;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;

import com.rommelbendel.scanQ.NewHome;
import com.rommelbendel.scanQ.R;
import com.rommelbendel.scanQ.Weekday;
import com.rommelbendel.scanQ.WeekdayViewModel;
import com.rommelbendel.scanQ.appIntro.AppIntro;

import org.joda.time.LocalDate;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class Startbildschirm extends AppCompatActivity {
    public final int ladezeit = 10; //Bei Bedarf erhöhen
    public static Startbildschirm instance = null;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.startbildschirm);
        TinyDB tb = new TinyDB(getApplicationContext());
//        TextView text3 = findViewById(R.id.text3);

        Window window = Startbildschirm.this.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.setStatusBarColor(ContextCompat.getColor(Startbildschirm.this, R.color.white));

        WeekdayViewModel weekdayViewModel = new ViewModelProvider(this).get(WeekdayViewModel.class);
        LiveData<List<Weekday>> allWeekdays = weekdayViewModel.getAllDays();
        allWeekdays.observe(Startbildschirm.this, weekdays -> {
            if (weekdays != null) {
                Log.d("check", "run");
                //Log.d("Weekdays", weekdays.toString());
                LocalDate today = LocalDate.now();
                Log.d("Date", today.toDate().toString());
                if (weekdays.size() == 0) {
                    Log.d("decision", "if");
                    final String[] weekdayNames = {"Montag", "Dienstag", "Mittwoch", "Donnerstag",
                            "Freitag", "Samstag", "Sonntag"};
                    Bundle days = new Bundle();
                    for (int i = 0; i < 7; i++) {
                        days.putLong(weekdayNames[today.minusDays(i).getDayOfWeek() - 1],
                                today.minusDays(i).toDate().getTime());
                    }

                    int dayOfWeekCounter = 1;
                    for (String name: weekdayNames) {
                        weekdayViewModel.insertWeekday(new Weekday(dayOfWeekCounter,name, new Date(
                                days.getLong(name)).getTime()));
                        dayOfWeekCounter ++;
                    }
                } else {
                    Log.d("decision", "else");
                    if (weekdays.get(0).getDate() != today.toDate().getTime()) {
                        Log.d("decision", "else");
                        final long timeShift = today.toDate().getTime() - weekdays.get(0).getDate();
                        final int dayShift = (int) (timeShift / 86400000); //conversion of milliseconds to days

                        Log.d("dayShift", String.valueOf(dayShift));
                        for (int i = 0; i < dayShift; i++) {
                            final LocalDate dayToInsert = today.minusDays(i);
                            weekdayViewModel.updateDay(dayToInsert.toDate().getTime(),
                                    dayToInsert.getDayOfWeek());
                        }
                    }
                }
                allWeekdays.removeObservers(Startbildschirm.this);
            }
        });


        if(!Objects.equals(tb.getString("username").trim(), "")) {
            if(tb.getBoolean("hello"))
               // text3.setText("Hey " + tb.getString("username") + "!");

            new Handler().postDelayed(() -> {
                Intent myIntent = new Intent(Startbildschirm.this, NewHome.class);
                Startbildschirm.this.startActivity(myIntent);
                finish();
                overridePendingTransition(R.transition.transition_fade_out, R.transition.transition_fade_in);
            },ladezeit);
        }else {
            new Handler().postDelayed(() -> {
                Intent myIntent = new Intent(Startbildschirm.this, AppIntro.class);
                Startbildschirm.this.startActivity(myIntent);
                finish();
            },ladezeit);
        }

        //FirebaseMessaging.getInstance().subscribeToTopic();

        /*FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w("TAG", "Fetching FCM registration token failed", task.getException());
                        return;
                    }

                    // Get new FCM registration token
                    String token = task.getResult();
                    tb.putString("t", token);
                });*/
        instance = this;
        saveTessData();
    }

    public String getTessDataDirectory() {
        return Objects.requireNonNull(Startbildschirm.instance.getExternalFilesDir(null)).getAbsolutePath();
    }

    private String getTessPath() {
        return Startbildschirm.instance.getExternalFilesDir(null) + "/tessdata/";
    }

    private void saveTessData() {
        Runnable runnable = () -> {
            AssetManager am = Startbildschirm.instance.getAssets();
            OutputStream outEng = null;
            OutputStream outDeu = null;

            try{
                InputStream inEng = am.open("eng.traineddata");
                InputStream inDeu = am.open("deu.traineddata");
                String tesspath = instance.getTessPath();
                File tessFolder = new File(tesspath);

                if(!tessFolder.exists())
                    tessFolder.mkdirs();

                String tessDataEng = tesspath + "/" + "eng.traineddata";
                String tessDataDeu = tesspath + "/" + "deu.traineddata";
                File tessFileEng = new File(tessDataEng);
                File tessFileDeu = new File(tessDataDeu);


                //Todo spalte if in zwei auf, um nach beiden einzeln zu prüfen
                if(!tessFileEng.exists() && !tessFileDeu.exists()) {
                    outEng = new FileOutputStream(tessDataEng);
                    outDeu = new FileOutputStream(tessDataDeu);
                    byte[] bufferEng = new byte[1024];
                    byte[] bufferDeu = new byte[1024];
                    int readEng = inEng.read(bufferEng);
                    int readDeu = inDeu.read(bufferDeu);

                    while (readEng != -1) {
                        outEng.write(bufferEng, 0, readEng);
                        readEng = inEng.read(bufferEng);
                    }

                    while (readDeu != -1) {
                        outDeu.write(bufferDeu, 0, readDeu);
                        readDeu = inDeu.read(bufferDeu);
                    }
                }
            } catch (Exception e) {
                Log.e("tag", Objects.requireNonNull(e.getMessage()));
            } finally {
                try {
                    if(outEng != null)
                        outEng.close();

                    if(outDeu != null)
                        outDeu.close();
                } catch (Exception ignored) {}
            }
        };
        new Thread(runnable).start();
    }
}
