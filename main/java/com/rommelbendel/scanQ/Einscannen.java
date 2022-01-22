package com.rommelbendel.scanQ;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.Text;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.google.android.material.button.MaterialButton;
import com.googlecode.tesseract.android.TessBaseAPI;
import com.rommelbendel.scanQ.additional.SplashActivity;
import com.rommelbendel.scanQ.additional.TinyDB;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;
import com.yuvraj.livesmashbar.enums.BarStyle;
import com.yuvraj.livesmashbar.enums.GravityView;
import com.yuvraj.livesmashbar.view.LiveSmashBar;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

public class Einscannen extends AppCompatActivity /*implements SpellCheckerSession.SpellCheckerSessionListener*/{

    private static final int DE = 2;
    private static final int ENG = 1;

     public class Tesseract extends AsyncTask<String, Boolean, Void> {

         private final ArrayList<Bitmap> bits;
         ProgressDialog progressDialog;

        public Tesseract(ArrayList<Bitmap> bitmaps) {
            this.bits = bitmaps;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(Einscannen.this);
            progressDialog.setMessage("Vokabeln werden erkannt…");
            progressDialog.setIndeterminate(false);
            progressDialog.setCancelable(false);
            progressDialog.setProgress(0);
            progressDialog.show();
        }

        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        protected Void doInBackground(String... params) {
            doOCR();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
        }

        private void doOCR() {
            try {
                final ArrayList<String> vocList = new ArrayList<>();
                for (Bitmap bitmap : bits) {
                    TinyDB tb = new TinyDB(Einscannen.this);
                    String DATA_PATH = SplashActivity.instance.getTessDataDirectory();
                    bitmap.setConfig(Bitmap.Config.ARGB_8888);

                    if (tb.getInt("languageKey") == ENG) {
                        TessBaseAPI tessBaseAPI = new TessBaseAPI();
                        tessBaseAPI.init(DATA_PATH, "eng");

                        tessBaseAPI.setVariable(TessBaseAPI.VAR_CHAR_BLACKLIST, "@#%*<>_+=\"");
                        tessBaseAPI.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, ".•'-:;&()[]!?$,0123456789" +
                                "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz");

                        tessBaseAPI.setImage(bitmap);
                        String res = tessBaseAPI.getUTF8Text()
                                .replaceAll("\\d ", "")
                                .replaceAll("\\n\\n", "\n")
                                .replaceAll("\\n\\n\\n", "\n")
                                .replaceAll("\\n\\n\\n\\n", "\n")
                                .replaceAll("5th", "sth")
                                .replaceAll("\\n[^a-zA-Z0-9]", "\n");

                        if (tb.getBoolean("lowercase"))
                            res = res.toLowerCase();

                        vocList.addAll(Arrays.asList(res.split("\\n")));
                        vocList.removeAll(Collections.singletonList("WORD BANK"));

                        tb.putBoolean("EnglishDone", true);
                        tb.putListString("vocListE", vocList);

                    } else if (tb.getInt("languageKey") == DE) {
                        TessBaseAPI tessBaseAPI = new TessBaseAPI();
                        tessBaseAPI.init(DATA_PATH, "deu");

                        tessBaseAPI.setVariable(TessBaseAPI.VAR_CHAR_BLACKLIST, "@#%*<>_+=\"");
                        tessBaseAPI.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, ".:;'-&()[]!?$,0123456789" +
                                "ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÜabcdefghijklmnopqrstuvwxyzäöü");

                        tessBaseAPI.setImage(bitmap);
                        final String res = tessBaseAPI.getUTF8Text()
                                .replaceAll("\\n\\n", "\n")
                                .replaceAll("\\n\\n\\n", "\n")
                                .replaceAll("\\n\\n\\n\\n", "\n")
                                .replaceAll("\\n[^a-zA-Z0-9]", "\n");
                        vocList.addAll(Arrays.asList(res.split("\\n")));

                        tb.putBoolean("Done", true);
                        tb.putListString("vocListD", vocList);
                    }
                }
                runOnUiThread(Einscannen.this::recreate);
            } catch (Exception e) {
                Log.e("error English voc", e.toString());
            }
            //ToDo: detect Words below
        }
    }

    private ArrayList<RectF> rectFS;
    private ArrayList<RectF> rect2nds;
    private final ArrayList<int[]> dimList = new ArrayList<>();
    private final ArrayList<int[]> dimListFor2nds = new ArrayList<>();

    private TextView kategorie;
    private EditText topic;
    private Spinner spinner;
    private SwitchCompat switchMode;
    private NestedScrollView tableScroll;
    private RecyclerView tableVoc;
    private ImageView bildVorschau;
    private TableLayout finalButtons;
    private LinearLayout llForSpinner, llForET;
    private Button speichern1, speichern2, loeschen;
    private MaterialButton scanE, scanD, saveTopic;
    private CardView cv, note1, note2;
    private ConstraintLayout navMethod, navMethodD, navMethodTopic;

    private static final int CAMERA_REQUEST_CODE = 200;
    private static final int STORAGE_REQUEST_CODE = 400;
    private static final int IMAGE_START_SCAN_CODE = 1001;
    private static final int IMG_CROP_CODE = 101;

    private String[] cameraPermission;
    private String[] storagePermission;
    private Uri image_uri;

    private final BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    try {
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback);
        } else {
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstaceState) {
        super.onCreate(savedInstaceState);
        setContentView(R.layout.activity_einscannen);
        final TinyDB tb = new TinyDB(getApplicationContext());

        navMethod = findViewById(R.id.navMethod);
        navMethodD = findViewById(R.id.navMethodD);
        navMethodTopic = findViewById(R.id.navMethodTopic);

        scanE = findViewById(R.id.navScanWeiter);
        scanD = findViewById(R.id.navScanD);

        bildVorschau = findViewById(R.id.bildVorschau);
        note1 = findViewById(R.id.note_remove);
        speichern1 = findViewById(R.id.speichern1);

        finalButtons = findViewById(R.id.tableButtons);
        speichern2 = findViewById(R.id.speichern);
        loeschen = findViewById(R.id.loeschen);
        tableVoc = findViewById(R.id.tabelleVoc);
        tableScroll = findViewById(R.id.tabelleVocScroll);
        note2 = findViewById(R.id.hinweis_tabelle);
        cv = findViewById(R.id.tableCardView);

        topic = findViewById(R.id.topicEt);
        switchMode = findViewById(R.id.topicMode);
        spinner = findViewById(R.id.spinner);
        llForET = findViewById(R.id.llForET);
        llForSpinner = findViewById(R.id.llForSpinner);
        saveTopic = findViewById(R.id.saveTopic);

        cameraPermission = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermission = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

        Intent getintent = getIntent();
        int cam_id = getintent.getIntExtra("cam_key", 0);

        if (cam_id == 221122) {
            chooseScanMethod();
        }

        if(tb.getBoolean("Done")) {
            try {
                speichern1.setVisibility(View.GONE);
                note1.setVisibility(View.GONE);
                cv.setVisibility(View.VISIBLE);
                finalButtons.setVisibility(View.VISIBLE);
                note2.setVisibility(View.VISIBLE);

                final TabelleVokabelAdapter stringVokabelAdapter = new TabelleVokabelAdapter(this);
                tableVoc.setAdapter(stringVokabelAdapter);
                tableVoc.setLayoutManager(new LinearLayoutManager(this));

                ArrayList<Vokabel> resultVoc = new ArrayList<>();
                ArrayList<String> vocListE = tb.getListString("vocListE");
                ArrayList<String> vocListD = tb.getListString("vocListD");

                /*for(String voc : vocListE) {
                    correctMisspelledWords(voc, ENG);
                }
                for(String voc : vocListD) {
                    correctMisspelledWords(voc, DE);
                }*/

                if(vocListE.size() == vocListD.size()) {
                    for (int i = 0; i < vocListE.size(); i++) {
                        resultVoc.add(new Vokabel(vocListE.get(i), vocListD.get(i), "0"));
                    }
                } else {
                    int diff = Math.abs(vocListD.size() - vocListE.size());
                    for(int i = 0; i < diff; i++) {
                        if(vocListE.size() > vocListD.size()) {
                            vocListD.add("");
                        } else {
                            vocListE.add("");
                        }
                    }
                    for (int i = 0; i < vocListE.size(); i++) {
                        resultVoc.add(new Vokabel(vocListE.get(i), vocListD.get(i), "0"));
                    }

                    new LiveSmashBar.Builder(Einscannen.this)
                            .description("unterschiedliche Anzahl von Vokabeln und Übersetzungen")
                            .descriptionColor(Color.WHITE)
                            .backgroundColor(Color.parseColor("#541111"))
                            .gravity(GravityView.BOTTOM)
                            .duration(10000)
                            .dismissOnTapOutside()
                            .showOverlay()
                            .overlayBlockable()
                            .show();
                }

                stringVokabelAdapter.setVokabelCache(resultVoc);
            } catch (Exception e) {
                Log.e("FinalScreen error",e.toString());
            }
        } else {
            if(!tb.getBoolean("EnglishDone")) {
                showImageImportDialog();
            } else {
                vocDeutsch();
            }
        }

        if(bildVorschau.getDrawable() != null) {
            note1.setVisibility(View.VISIBLE);
            speichern1.setVisibility(View.VISIBLE);
        }

        loeschen.setOnClickListener(v -> new LiveSmashBar.Builder(Einscannen.this)
                .title("Willst du diese Vokabeln wirklich nicht speichern?")
                .titleColor(Color.WHITE)
                .titleSizeInSp(19)
                .backgroundColor(Color.parseColor("#2f3030"))
                .setBarStyle(BarStyle.DIALOG)
                .positiveActionText("abbrechen")
                .positiveActionTextSizeInSp(16)
                .positiveActionTextColor(Color.WHITE)
                .positiveActionEventListener(LiveSmashBar::dismiss)
                .negativeActionText("löschen")
                .negativeActionTextSizeInSp(16)
                .negativeActionTextColor(Color.WHITE)
                .negativeActionEventListener(liveSmashBar -> loeschenClicked())
                .showOverlay()
                .overlayBlockable()
                .duration(20000)
                .show());

        speichern1.setOnClickListener(view -> {
            try {
                BitmapDrawable bitmapDrawable = (BitmapDrawable) bildVorschau.getDrawable();
                Bitmap bitmap = prepareEditedBitmap(bitmapDrawable.getBitmap());

                int rows;
                if (!tb.getBoolean("EnglishDone")) {
                    rows = tb.getInt("rowsEn");
                } else {
                    rows = tb.getInt("rowsDe");
                }

                rows = (int) Math.ceil(rows / 4.0);

                ArrayList<Bitmap> bitmapList = new ArrayList<>(Arrays.asList(splitImage(bitmap, detectWordRects(bitmap), rows)));
                new Tesseract(bitmapList).execute();
            } catch (Exception exception) {
                Log.e("Einscann Fehler", exception.toString());

                new LiveSmashBar.Builder(Einscannen.this)
                    .title("ein unerwarteter Fehler ist aufgetreten")
                    .titleColor(Color.WHITE)
                    .backgroundColor(Color.parseColor("#541111"))
                    .gravity(GravityView.BOTTOM)
                    .primaryActionText("Ok")
                    .primaryActionEventListener(LiveSmashBar::dismiss)
                    .duration(10000)
                    .showOverlay()
                    .overlayBlockable()
                    .show();}
        });

        speichern2.setOnClickListener(view -> {
            navMethodTopic.setVisibility(View.VISIBLE);
            tableScroll.setVisibility(View.GONE);
            note2.setVisibility(View.GONE);

            ArrayList<String> vocListD = new ArrayList<>();
            ArrayList<String> vocListE = new ArrayList<>();

            final int rowCount = Objects.requireNonNull(tableVoc.getLayoutManager()).getChildCount(); // requrieNonNull
            for (int i = 0; i < rowCount - 1; i++) {
                TableLayout tableLayout = (TableLayout) tableVoc.getLayoutManager().findViewByPosition(i);
                assert tableLayout != null; // changed -> remove if needed
                TableRow row = (TableRow) tableLayout.getChildAt(0);
                String vocENG = ((EditText)((CardView) row.getChildAt(0)).getChildAt(0))
                        .getText().toString().trim();
                String vocDE = ((EditText)((CardView) row.getChildAt(1)).getChildAt(0))
                        .getText().toString().trim();
                vocListD.add(vocDE);
                vocListE.add(vocENG);
            }

            VokabelViewModel vokabelViewModel = new ViewModelProvider(Einscannen.this)
                    .get(VokabelViewModel.class);

            KategorienViewModel kategorienViewModel = new ViewModelProvider(Einscannen.this)
                    .get(KategorienViewModel.class);

            LiveData<List<Kategorie>> allCategoriesLiveData = kategorienViewModel
                    .getAlleKategorien();

            allCategoriesLiveData.observe(Einscannen.this, categories -> {
                if (categories != null) {
                    if (categories.size() > 0) {
                        List<String> allCategories = new ArrayList<>();
                        for (Kategorie category: categories) {
                            allCategories.add(category.getName());
                        }

                        ArrayAdapter<String> adp2 = new ArrayAdapter<>(Einscannen.this,
                                android.R.layout.simple_spinner_item, allCategories);
                        adp2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinner.setAdapter(adp2);

                        switchMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
                            if(isChecked) {
                                llForET.setVisibility(View.GONE);
                                llForSpinner.setVisibility(View.VISIBLE);
                            } else {
                                llForET.setVisibility(View.VISIBLE);
                                llForSpinner.setVisibility(View.GONE);
                            }
                        });
                    } else {
                        switchMode.setVisibility(View.INVISIBLE);
                    }

                    saveTopic.setOnClickListener(v -> {
                        if (llForET.getVisibility() == View.VISIBLE) {
                            String topicName = topic.getText().toString();

                            if(topicName.trim().length() != 0) {
                                Kategorie kategoryNew = new Kategorie(topicName.trim());
                                kategorienViewModel.insertKategorie(kategoryNew);

                                for (int i = 0; i < vocListD.size(); i++) {
                                    Vokabel vocabToInsert = new Vokabel(vocListE.get(i),
                                            vocListD.get(i), topicName);
                                    vokabelViewModel.insertVokabel(vocabToInsert);
                                }

                                tb.putBoolean("Done", false);
                                tb.putBoolean("EnglishDone", false);
                                finish();
                            } else {
                                new LiveSmashBar.Builder(Einscannen.this)
                                        .title("Bitte gib eine Kategorie ein")
                                        .titleColor(Color.WHITE)
                                        .backgroundColor(Color.parseColor("#541111"))
                                        .gravity(GravityView.BOTTOM)
                                        .primaryActionText("Ok")
                                        .primaryActionEventListener(LiveSmashBar::dismiss)
                                        .duration(3000)
                                        .showOverlay()
                                        .overlayBlockable()
                                        .show();
                            }
                        }

                        if (llForSpinner.getVisibility() == View.VISIBLE) {
                            if (spinner.getSelectedItemPosition() != 0) {
                                String topicName = spinner.getSelectedItem().toString();

                                for (int i = 0; i < vocListD.size(); i++) {
                                    Vokabel vocabToInsert = new Vokabel(vocListE.get(i),
                                            vocListD.get(i), topicName);
                                    vokabelViewModel.insertVokabel(vocabToInsert);
                                }

                                tb.putBoolean("Done", false);
                                tb.putBoolean("EnglishDone", false);
                                finish();
                            } else {
                                new LiveSmashBar.Builder(Einscannen.this)
                                        .title("Bitte wähle eine Kategorie aus")
                                        .titleColor(Color.WHITE)
                                        .backgroundColor(Color.parseColor("#541111"))
                                        .gravity(GravityView.BOTTOM)
                                        .primaryActionText("Ok")
                                        .primaryActionEventListener(LiveSmashBar::dismiss)
                                        .duration(3000)
                                        .showOverlay()
                                        .overlayBlockable()
                                        .show();
                            }
                        }
                    });
                }
            });
        });
    }

    public void loeschenClicked() {
        TinyDB tb = new TinyDB(getApplicationContext());
        tb.putBoolean("Done", false);
        tb.putBoolean("EnglishDone", false);
        tb.remove("vocListD");
        tb.remove("vocListE");
        bildVorschau.setImageURI(null);
        bildVorschau.setImageBitmap(null);

        Einscannen.this.recreate();
    }

    private void showImageImportDialog() {
        navMethod.setVisibility(View.VISIBLE);
        bildVorschau.setImageDrawable(null);
        final TinyDB tb = new TinyDB(getApplicationContext());
        tb.putInt("languageKey", 1);

        scanE.setOnClickListener(view -> {
            if (!checkCameraPermissions()) { //Prüfen ob Berechtigung Kamera erlaubt
                requestCameraPermissions(); // Berechtigung erteilen
            } else {
                chooseScanMethod();
            }

           navMethod.setVisibility(View.GONE);
        });

        /*weiterMitGal.setOnClickListener(view -> {
            if (!checkStoragePermissions()) { //Prüfen ob Berechtigung Galerie erlaubt
                requestStoragePermissions();
            } else {
                pickGallery();
            }

            navMethod.setVisibility(View.GONE);
        });*/
    }

    private void vocDeutsch() {
        navMethodD.setVisibility(View.VISIBLE);
        bildVorschau.setImageDrawable(null);
        final TinyDB tb = new TinyDB(getApplicationContext());
        tb.putInt("languageKey", 2);

        scanD.setOnClickListener(view -> {
            if (!checkCameraPermissions()) {
                requestCameraPermissions();
            } else {
                chooseScanMethod();
            }

            navMethodD.setVisibility(View.GONE);
        });

        /*galleryD.setOnClickListener(view -> {
            if (!checkStoragePermissions()) {
                requestStoragePermissions();
            } else {
                pickGallery();
            }

            navMethodD.setVisibility(View.GONE);
        });*/
    }

    /*private void pickGallery() {
        /*Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, IMAGE_PICK_GALLERY_CODE);
        intentForCropResult(null, FIRST_CROP);
    }*/

    /*private void requestStoragePermissions() {
        ActivityCompat.requestPermissions(this, storagePermission, STORAGE_REQUEST_CODE);
    }

    private boolean checkStoragePermissions() {
        return ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
    }*/

    private void chooseScanMethod() {
        /*ContentValues values = new ContentValues();
        /*values.put(MediaStore.Images.Media.TITLE, "neues Bild");
        values.put(MediaStore.Images.Media.DESCRIPTION, "Texterkennung");

        image_uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri);
        startActivityForResult(cameraIntent, IMAGE_PICK_CAMERA_CODE);*/
        intentForCropResult(null, IMG_CROP_CODE);
    }


    private void requestCameraPermissions() {
        ActivityCompat.requestPermissions(this, cameraPermission, CAMERA_REQUEST_CODE);
    }

    private boolean checkCameraPermissions() {
        boolean ergebnis = ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) == (PackageManager.PERMISSION_GRANTED);
        boolean ergebnis1 = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
        return ergebnis && ergebnis1;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (grantResults.length > 0) {
                boolean cameraAccepted = grantResults[0] ==
                        PackageManager.PERMISSION_GRANTED;
                if (cameraAccepted) {
                    chooseScanMethod();
                } else {
                    Toast.makeText(this, "Bitte gib ScanQ Zugang zur Kamera", Toast.LENGTH_LONG).show();
                }
            }

            /*case STORAGE_REQUEST_CODE:
                if (grantResults.length > 0) {
                    boolean writeStorageAccepted = grantResults[0] ==
                            PackageManager.PERMISSION_GRANTED;
                    if (writeStorageAccepted) {
                        pickGallery();
                    } else {
                        Toast.makeText(this, "Bitte gib ScanQ Zugang zur Galerie", Toast.LENGTH_LONG).show();
                    }
                }
                break;*/
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        TinyDB tb = new TinyDB(getApplicationContext());

        if(resultCode == RESULT_CANCELED) {
            if (tb.getBoolean("Done")) {
                speichern1.setVisibility(View.GONE);
                note1.setVisibility(View.GONE);
                note2.setVisibility(View.VISIBLE);
                finalButtons.setVisibility(View.VISIBLE);
                cv.setVisibility(View.VISIBLE);

                RecyclerView vokabelAusgabe = findViewById(R.id.tabelleVoc);
                final TabelleVokabelAdapter stringVokabelAdapter = new TabelleVokabelAdapter(this);
                vokabelAusgabe.setAdapter(stringVokabelAdapter);
                vokabelAusgabe.setLayoutManager(new LinearLayoutManager(this));

                ArrayList<Vokabel> resultVoc = new ArrayList<>();
                ArrayList<String> vocListE = tb.getListString("vocListE");

                //TODO: try replacing unused characters
                /*for(String voc : vocListE) {
                    //voc.replaceAll();
                }*/
                ArrayList<String> vocListD = tb.getListString("vocListD");

                for (int i = 0; i < vocListE.size(); i++) {
                    resultVoc.add(new Vokabel(vocListE.get(i), vocListD.get(i), "0"));
                }

                stringVokabelAdapter.setVokabelCache(resultVoc);
            } else {
                if (!tb.getBoolean("EnglishDone")) {
                    //vocabAmount();
                    showImageImportDialog();
                } else {
                    vocDeutsch();
                }
            }
        }

        if (resultCode == RESULT_OK) {
            if (requestCode == IMAGE_START_SCAN_CODE) {
                if (data != null && data.getExtras() != null)
                    image_uri = data.getData();
                    intentForCropResult(image_uri, IMG_CROP_CODE);
            }
        }
        if (requestCode == IMG_CROP_CODE) {
            CropImage.ActivityResult ergebnis = CropImage.getActivityResult(data);

            if (resultCode == RESULT_OK) {
                assert ergebnis != null;
                Uri ergebnisUri = ergebnis.getUri();
                bildVorschau.setImageURI(ergebnisUri);

                if(bildVorschau.getDrawable() != null ){
                    note1.setVisibility(View.VISIBLE);
                    speichern1.setVisibility(View.VISIBLE);
                }

                BitmapDrawable bd = (BitmapDrawable) bildVorschau.getDrawable();
                assert bd != null;
                final Bitmap newBit = bd.getBitmap();

                DisplayMetrics displayMetrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                float width = displayMetrics.widthPixels;
                float factor = (float) (width*0.87) / newBit.getWidth();
                Bitmap bitmap = Bitmap.createScaledBitmap(newBit, (int) ((int) width*0.87),
                        (int) (newBit.getHeight() * factor), true);

                rectFS = detectWordRects(bitmap).get(0);
                rect2nds = detectWordRects(bitmap).get(1);

                Mat img = new Mat();
                Utils.bitmapToMat(bitmap, img);
                Imgproc.cvtColor(img, img, Imgproc.COLOR_BGR2GRAY);
                Imgproc.GaussianBlur(img, img, new Size(7, 7), 0);
                Imgproc.adaptiveThreshold(img, img, 255,
                        Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 5, 4);
                Utils.matToBitmap(img, bitmap);

                Bitmap tempBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(tempBitmap);
                canvas.drawBitmap(bitmap, 0, 0, null);

                int color = Color.WHITE;
                color = Color.argb(205,
                        Color.red(color),
                        Color.green(color),
                        Color.blue(color));
                Paint paint = new Paint();
                Paint recPaint = new Paint();
                paint.setColor(color);

                for (int i = 0; i < rectFS.size(); i++) {
                    int[] dims = {
                            (int) rectFS.get(i).left - 10,
                            (int) rectFS.get(i).top - 10,
                            (int) rectFS.get(i).right + 30,
                            (int) rectFS.get(i).bottom + 10
                    };
                    dimList.add(dims);
                }

                for (int i = 0; i < rect2nds.size(); i++) {
                    int[] dims = {
                            (int) rect2nds.get(i).left - 10,
                            (int) rect2nds.get(i).top - 10,
                            (int) rect2nds.get(i).right + 30,
                            (int) rect2nds.get(i).bottom + 10
                    };
                    dimListFor2nds.add(dims);
                }

                for(int i = 0; i < rect2nds.size(); i++) {
                    RectEdit rectEdit = new RectEdit(getApplicationContext(),
                            canvas, dimListFor2nds, recPaint, null);

                    rectEdit.add(color);
                }
                bildVorschau.setImageBitmap(tempBitmap);

                final ArrayList<Integer> pos = new ArrayList<>();
                int finalColor = color;
                bildVorschau.setOnTouchListener((view, motionEvent) -> {
                    if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                        canvas.drawBitmap(bitmap, 0, 0, null);

                        for(int i = 0; i < rect2nds.size(); i++) {
                            RectEdit rectEdit = new RectEdit(getApplicationContext(),
                                    canvas, dimListFor2nds, recPaint, null);

                            rectEdit.add(finalColor);
                        }

                        for (int i = 0; i < rectFS.size(); i++) {
                            if (rectFS.get(i).contains(motionEvent.getX(), motionEvent.getY())) {
                                if (!pos.contains(i)) {
                                    pos.add(i);
                                    RectEdit rectEdit = new RectEdit(getApplicationContext(),
                                            canvas, dimList, recPaint, pos);

                                    rectEdit.add(finalColor);
                                } else {
                                    pos.removeAll(Collections.singletonList(i));
                                    RectEdit rectEdit = new RectEdit(getApplicationContext(),
                                            canvas, dimList, recPaint, pos);

                                    rectEdit.add(finalColor);
                                }
                                bildVorschau.setImageBitmap(tempBitmap);
                            }
                        }
                        tb.putListInt("posList", pos);
                    }
                    return false;
                });
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                assert ergebnis != null;
                Exception error = ergebnis.getError();
                Toast.makeText(this, "Fehler beim Zuschneiden den Bildes" + error, Toast.LENGTH_SHORT).show();
            }
        }
    }

    public ArrayList<ArrayList<RectF>> detectWordRects(Bitmap bitmap) {

        Logger log = Logger.getLogger(Einscannen.class.getName());
        TinyDB tb = new TinyDB(getApplicationContext());
        TextRecognizer recognizer = new TextRecognizer.Builder(this).build();

        ArrayList<RectF> rects0 = new ArrayList<>();
        ArrayList<RectF> rects1 = new ArrayList<>();
        TextBlock myItem;
        int rows;

        Frame frame = new Frame.Builder().setBitmap(bitmap).build();
        SparseArray<TextBlock> items = recognizer.detect(frame);
        List<TextBlock> blocks = new ArrayList<>();

        Paint rectPaint = new Paint();
        rectPaint.setStyle(Paint.Style.STROKE);
        rectPaint.setStrokeWidth(4);

        Paint delPaint = new Paint();
        delPaint.setStyle(Paint.Style.FILL);

        Bitmap tempBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(tempBitmap);
        canvas.drawBitmap(bitmap, 0, 0, null);

        tb.putInt("rows", 0);
        tb.putInt("lefts", 0);
        for (int i = 0; i < items.size(); i++) {
            myItem = items.valueAt(i);
            blocks.add(myItem);
        }

        for (int i = 0; i < blocks.size(); i++) {
            List<? extends Text> textLines = blocks.get(i).getComponents();
            int lefts;

            for (int z = 0; z < textLines.size(); z++) {
                List<? extends Text> words = textLines.get(z).getComponents();

                lefts = tb.getInt("lefts") + textLines.get(z).getBoundingBox().left;
                tb.putInt("lefts",lefts);

                rows = tb.getInt("rows") + 1;
                tb.putInt("rows", rows);

                log.warning("left: " + textLines.get(z).getBoundingBox().left);
                try {
                    for (int u = 0; u < words.size(); u++) {
                        int[] dims = {
                                words.get(u).getBoundingBox().left,
                                words.get(u).getBoundingBox().top,
                                words.get(u).getBoundingBox().right,
                                words.get(u).getBoundingBox().bottom
                        };

                        int median = tb.getInt("lefts") / tb.getInt("rows");

                        if(textLines.get(z).getBoundingBox().left <= median*1.2) {

                            RectEdit rectEdit = new RectEdit(getApplicationContext(),
                                    canvas, dims, rectPaint);
                            RectF rect = rectEdit.draw();
                            rects0.add(rect);

                        } else {
                            int color = Color.parseColor("#6C0418");
                            color = Color.argb(185,
                                    Color.red(color),
                                    Color.green(color),
                                    Color.blue(color));

                            RectEdit rectEdit = new RectEdit(getApplicationContext(),
                                    canvas, dims, delPaint);
                            RectF rect = rectEdit.remove2nds(color);

                            rects1.add(rect);
                        }

                        if (!tb.getBoolean("EnglishDone")) {
                            tb.putInt("rowsEn", tb.getInt("rows"));
                        } else {
                            tb.putInt("rowsDe", tb.getInt("rows"));
                        }

                        bildVorschau.setImageBitmap(tempBitmap);
                    }
                } catch (Exception e) {
                    log.warning(e.toString() + " : detect words error");
                }
            }
        }

        ArrayList<ArrayList<RectF>> rects = new ArrayList<>();
        rects.add(rects0);
        rects.add(rects1);
        return rects;
    }

    public Bitmap prepareEditedBitmap(Bitmap bitmap) {
        TinyDB tb = new TinyDB(getApplicationContext());
        final ArrayList<Integer> pos = tb.getListInt("posList");

        Bitmap tempBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(tempBitmap);
        canvas.drawBitmap(bitmap, 0, 0, null);

        Paint paint = new Paint();
        Paint recPaint = new Paint();
        paint.setColor(Color.WHITE);

        for (int i = 0; i < rectFS.size(); i++) {
            int[] dims = {
                    (int) rectFS.get(i).left - 10,
                    (int) rectFS.get(i).top - 10,
                    (int) rectFS.get(i).right + 30,
                    (int) rectFS.get(i).bottom + 10
            };
            dimList.add(dims);
        }

        for (int i = 0; i < rectFS.size(); i++) {
            RectEdit rectEdit = new RectEdit(getApplicationContext(),
                    canvas, dimList, recPaint, pos);
            rectEdit.add(Color.WHITE);
        }

        return tempBitmap;
    }

    public void intentForCropResult(Uri uri, int crop_code) {
        Intent intent = CropImage.activity(uri)
                .setGuidelines(CropImageView.Guidelines.ON_TOUCH)
                .getIntent(getApplicationContext());
        Einscannen.this.startActivityForResult(intent, crop_code);
    }

    public Bitmap[] splitImage(Bitmap bitImg, ArrayList<ArrayList<RectF>> rfs, int medianVocs) {
        OpenCVLoader.initDebug();

        Mat img = new Mat();
        Utils.bitmapToMat(bitImg, img);

        ArrayList<int[]> blockedSpaces = new ArrayList<>();
        ArrayList<RectF> rf = rfs.get(0);
        rf.addAll(rfs.get(1));

        for (int i = 0; i < rf.size(); i++) {
            int[] dims = {
                    (int) rf.get(i).top - 10,
                    (int) rf.get(i).bottom + 10
            };
            blockedSpaces.add(dims);
        }

        /*if (tb.getInt("vocAmount") == 0) {
            bit = Bitmap.createBitmap(img.cols(), img.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(img, bit);
        }

        if (tb.getInt("vocAmount") == 1) {
            Rect rect = new Rect(0, 0, img.width(), (img.height() / 2));
            Mat resized = new Mat(img, rect);

            Rect rect2 = new Rect(0, (img.height() / 2), img.width(), (img.height() / 2));
            Mat resized2 = new Mat(img, rect2);

            bit = Bitmap.createBitmap(resized.cols(), resized.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(resized, bit);

            bit2 = Bitmap.createBitmap(resized2.cols(), resized2.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(resized2, bit2);
        }*/

        //if (2 == 2) {
            int[] crops = new int[medianVocs+1];
            Bitmap[] bitmaps = new Bitmap[medianVocs];
            Mat[] mats = new Mat[medianVocs];

            for(int v = 0; v < medianVocs+1; v++) {
                int crop = img.height() / (medianVocs) * (v);

                for (int i = 0; i < blockedSpaces.size(); i++) {
                    int top = blockedSpaces.get(i)[0];
                    int bottom = blockedSpaces.get(i)[1];

                    if (crop > top && crop < bottom) {
                        if (crop - top < bottom - crop) {
                            crop = (int) (top * 0.99);
                        } else {
                            crop = (int) (bottom * 1.01);
                        }
                    }
                    crops[v] = crop;
                }
            }

            for(int c = 0; c < crops.length-1; c++) {
                Rect rect = new Rect(0, crops[c], img.width(), crops[c+1]-crops[c]);
                Mat resized = new Mat(img, rect);
                Bitmap bit = Bitmap.createBitmap(resized.cols(), resized.rows(), Bitmap.Config.ARGB_8888);

                bitmaps[c] = bit;
                mats[c] = resized;
            }

            for (int i = 0; i < bitmaps.length; i++) {
                Utils.matToBitmap(mats[i], bitmaps[i]);
            }
        //}
        return bitmaps;
    }

    /*private void correctMisspelledWords(String vocab, int languageKey){
        Locale language = Locale.ENGLISH;
        if(languageKey == DE)
            language = Locale.GERMAN;

        TextServicesManager tsm = (TextServicesManager) getSystemService(TEXT_SERVICES_MANAGER_SERVICE);
        SpellCheckerSession session = tsm.newSpellCheckerSession(null, language, this, true);

        if (session != null) {
            session.getSentenceSuggestions(new TextInfo[]{new TextInfo(vocab)}, 3);
        } else {
            Toast.makeText(this, "Bitte schalte den SpellChecker ein.", Toast.LENGTH_LONG).show();
            ComponentName componentToLaunch = new ComponentName("com.android.settings",
                    "com.android.settings.Settings$SpellCheckersSettingsActivity");
            Intent intent = new Intent();
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.setComponent(componentToLaunch);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                this.startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Log.e("SpellCheckerSettings", e.toString());
            }
        }
    }

    @Override
    public void onGetSuggestions(SuggestionsInfo[] results) {}

    @Override
    public void onGetSentenceSuggestions(SentenceSuggestionsInfo[] results) {
        for(SentenceSuggestionsInfo result:results) {
            for(int i=0; i < result.getSuggestionsCount(); i++){
                for(int k=0; k < result.getSuggestionsInfoAt(i).getSuggestionsCount(); k++) {
                    Log.e("spellCheck", result.getSuggestionsInfoAt(i).getSuggestionAt(k));
                }

            }
        }
    }*/
}