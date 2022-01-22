package com.rommelbendel.scanQ.additional;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.rommelbendel.scanQ.NewHome;
import com.rommelbendel.scanQ.R;
import com.rommelbendel.scanQ.appIntro.AppIntro;
import com.rommelbendel.scanQ.impaired.visually.TutorialVIM;
import com.rommelbendel.scanQ.impaired.visually.VoiceControl;
import com.rommelbendel.scanQ.speechrecognition.RecognitionViewModel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

public class SplashActivity extends AppCompatActivity {
    public static SplashActivity instance = null;
    private RecognitionViewModel recognitionResultVM;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TinyDB tb = new TinyDB(getApplicationContext());
        SharedPreferences pref = getSharedPreferences("settings",0);
        SharedPreferences.Editor edit = pref.edit();

        instance = this;
        saveTessData();

        if (tb.getBoolean("visually_impaired") && !tb.getBoolean("vim_tutorial_finished")) {
            Intent startVIMTutorial = new Intent(SplashActivity.this, TutorialVIM.class);
            SplashActivity.this.startActivity(startVIMTutorial);
            finish();
        } else {
            Intent startIntent;
            if (!Objects.equals(tb.getString("username").trim(), ""))
                startIntent = new Intent(SplashActivity.this, NewHome.class);
            else {
                startIntent = new Intent(SplashActivity.this, AppIntro.class);

                edit.putBoolean("personal", false);
                edit.putBoolean("cats", false);
            }
            edit.apply();
            SplashActivity.this.startActivity(startIntent);
            finish();
            overridePendingTransition(R.transition.transition_fade_out, R.transition.transition_fade_in);

            if(tb.getBoolean("visually_impaired")) {
                recognitionResultVM = new ViewModelProvider(SplashActivity.this)
                        .get(RecognitionViewModel.class);
                VoiceControl voiceControl = new VoiceControl(this, recognitionResultVM);
                voiceControl.speak("Hey " + tb.getString("username"));
            }
        }
    }

    public String getTessDataDirectory() {
        return Objects.requireNonNull(SplashActivity.instance.getExternalFilesDir(null)).getAbsolutePath();
    }

    private String getTessPath() {
        return SplashActivity.instance.getExternalFilesDir(null) + "/tessdata/";
    }

    private void saveTessData() {
        Runnable runnable = () -> {
            AssetManager am = SplashActivity.instance.getAssets();
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
