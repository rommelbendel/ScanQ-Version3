package com.rommelbendel.scanQ;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.DefaultValueFormatter;
import com.github.mikephil.charting.formatter.IValueFormatter;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.ViewPortHandler;
import com.yuvraj.livesmashbar.enums.GravityView;
import com.yuvraj.livesmashbar.view.LiveSmashBar;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Statistics extends AppCompatActivity {

    public class CustomPercentFormatter implements IValueFormatter {

        private DecimalFormat mFormat;
        public CustomPercentFormatter() {
            mFormat = new DecimalFormat("###,###,##0.0");
        }

        public CustomPercentFormatter(DecimalFormat format) {
            this.mFormat = format;
        }

        @Override
        public String getFormattedValue(float value, Entry entry, int dataSetIndex, ViewPortHandler viewPortHandler) {
            if (value < 10f) {
                return "";
            }
            return mFormat.format(value) + " %";
        }
    }

    public static final int MODE_LOAD_CATEGORY = 1;
    public static final int MODE_LOAD_ALL = 2;
    //public static final int REQUEST = 112;

    private KategorienViewModel categoryViewModel;
    private VokabelViewModel vocabViewModel;
    private WeekdayViewModel weekdayViewModel;
    private LiveData<List<Kategorie>> allCategoriesLiveData;
    private List<Kategorie> allCategories;
    private List<Vokabel> vocabsToAnalyze;
    private List<String> categoryNames;

    private Spinner categorySelection;
    private TextView numberOfVocabs;
    private PieChart rightWrongPieChart;
    private BarChart rightWrongBarChart;
    private TextView trainedLast7Days;
    private TextView trainingAverage7Days;
    private LineChart trainingHistory;
    private TextView header7Days;
    private View contentSeparator2;

    private String categoryTOLoad;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        categorySelection = findViewById(R.id.categorySelection);
        numberOfVocabs = findViewById(R.id.numberOfVocabs);
        rightWrongPieChart = findViewById(R.id.pieChartRightWrong);
        rightWrongBarChart = findViewById(R.id.barChartRightWrong);
        trainedLast7Days = findViewById(R.id.trained7Days);
        trainingAverage7Days = findViewById(R.id.averageTraining7Days);
        trainingHistory = findViewById(R.id.lineChartTrainingHistory);
        header7Days = findViewById(R.id.header7Days);
        contentSeparator2 = findViewById(R.id.contentSeparator2);

        categoryViewModel = new ViewModelProvider(this).get(KategorienViewModel.class);
        vocabViewModel = new ViewModelProvider(this).get(VokabelViewModel.class);
        weekdayViewModel = new ViewModelProvider(this).get(WeekdayViewModel.class);

        final Bundle extras = getIntent().getExtras();
        assert extras != null;
        final int mode = extras.getInt("mode");

        allCategoriesLiveData = categoryViewModel.getAlleKategorien();
        allCategoriesLiveData.observe(this, categories -> {
            if (categories != null) {
                if (categories.size() != 0) {
                    allCategories = categories;

                    categoryNames = new ArrayList<>();
                    categoryNames.add("alle");
                    for (Kategorie category: allCategories) {
                        categoryNames.add(category.getName());
                    }
                    ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                            Statistics.this,
                            android.R.layout.simple_spinner_item, categoryNames);
                    categoryAdapter.setDropDownViewResource(
                            android.R.layout.simple_spinner_dropdown_item);
                    categorySelection.setAdapter(categoryAdapter);
                    categorySelection.setOnItemSelectedListener(
                            new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            categoryTOLoad = categoryNames.get(position);
                            LiveData<List<Vokabel>> vocabsToAnalyze = loadCategory(categoryTOLoad);
                            int actualMode = categoryTOLoad.equals("alle")
                                    ? MODE_LOAD_ALL : MODE_LOAD_CATEGORY;
                            analyzeVocabs(vocabsToAnalyze, actualMode);
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {}
                    });

                    LiveData<List<Vokabel>> vocabsToAnalyze;
                    if (mode == MODE_LOAD_CATEGORY) {
                        categoryTOLoad = extras.getString("category");
                        assert categoryTOLoad != null;
                        vocabsToAnalyze = loadCategory(categoryTOLoad);
                    } else {
                        vocabsToAnalyze = loadAll();
                    }
                    analyzeVocabs(vocabsToAnalyze, mode);
                } else {
                    new LiveSmashBar.Builder(Statistics.this)
                            .title("Es sind keine Vokabeln vorhanden.")
                            .titleColor(Color.WHITE)
                            .titleSizeInSp(17)
                            .backgroundColor(Color.parseColor("#541111"))
                            .gravity(GravityView.BOTTOM)
                            .primaryActionText("ok")
                            .primaryActionEventListener(l -> {
                                l.dismiss();
                                finish();
                            })
                            .showOverlay()
                            .overlayBlockable()
                            .duration(60000)
                            .show();
                }
                allCategoriesLiveData.removeObservers(Statistics.this);
            }
        });
    }

    private LiveData<List<Vokabel>> loadCategory(String categoryName) {
        return categoryName.equals("alle") ? loadAll() : vocabViewModel.getCategoryVocabs(categoryName);
    }

    private LiveData<List<Vokabel>> loadAll() {
        return vocabViewModel.getAlleVokabeln();
    }

    private void analyzeVocabs(LiveData<List<Vokabel>> vocabsLiveData, final int mode) {
        vocabsLiveData.observe(Statistics.this, vocabs -> {
            if (vocabs != null) {
                if (vocabs.size() != 0) {
                    numberOfVocabs.setVisibility(View.VISIBLE);
                    rightWrongPieChart.setVisibility(View.VISIBLE);
                    rightWrongBarChart.setVisibility(View.VISIBLE);

                    //Number of vocabs
                    numberOfVocabs.setText(String.format("%s Vokabeln in dieser Kategorie",
                            vocabs.size()));

                    //Pie chart training
                    rightWrongPieChart.setUsePercentValues(true);
                    rightWrongPieChart.getDescription().setEnabled(false);
                    rightWrongPieChart.animateY(1500, Easing.EasingOption.EaseInQuad);

                    //final String rightWrongLabel = "Lernstand";
                    ArrayList<PieEntry> rightWrongEntries = new ArrayList<>();

                    int goodTrained = 0;
                    int badTrained = 0;
                    int untrained = 0;

                    for (Vokabel vocab: vocabs) {
                        if (vocab.getRichtig() == 0 && vocab.getFalsch() == 0) {
                            untrained ++;
                        }else if (vocab.getRichtig() > vocab.getFalsch()) {
                            goodTrained ++;
                        } else {
                            badTrained ++;
                        }
                    }

                    Map<String, Integer> dataRightWrong = new HashMap<>();
                    if(goodTrained > 0)
                        dataRightWrong.put("gut", goodTrained);
                    if(badTrained > 0)
                        dataRightWrong.put("schlecht", badTrained);
                    if(untrained > 0)
                        dataRightWrong.put("ungespielt", untrained);

                    ArrayList<Integer> colors = new ArrayList<>();
                    colors.add(ContextCompat.getColor(this, R.color.colorAccent));
                    colors.add(ContextCompat.getColor(this, R.color.colorPrimary));
                    colors.add(ContextCompat.getColor(this, R.color.colorPrimary4));

                    for (String dataName: dataRightWrong.keySet()) {
                        rightWrongEntries.add(new PieEntry(Objects.requireNonNull(dataRightWrong.get(
                                dataName)).floatValue(), dataName));
                    }

                    PieDataSet dataSetRightWrong = new PieDataSet(rightWrongEntries,
                            "");
                    dataSetRightWrong.setColors(colors);
                    dataSetRightWrong.setValueTextSize(14);
                    dataSetRightWrong.setValueTextColor(Color.parseColor("#f3fefe"));

                    PieData chartDataRightWrong = new PieData(dataSetRightWrong);
                    chartDataRightWrong.setValueFormatter(new CustomPercentFormatter());
                    chartDataRightWrong.setDrawValues(true);

                    rightWrongPieChart.setData(chartDataRightWrong);
                    int finalGoodTrained = goodTrained;
                    int finalBadTrained = badTrained;
                    rightWrongPieChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
                        @Override
                        public void onValueSelected(Entry e, Highlight h) {
                            Intent detailedStatisticsIntent = new Intent(Statistics.this, AlleVokabelnAnzeigen.class);;
                            switch ((int) h.getX()) {
                                case 0:
                                    if(finalGoodTrained > 0)
                                        detailedStatisticsIntent.putExtra("statsDetails", 1);
                                    else if(finalBadTrained > 0)
                                        detailedStatisticsIntent.putExtra("statsDetails", 2);
                                    else
                                        detailedStatisticsIntent.putExtra("statsDetails", 3);
                                    break;

                                case 1:
                                    if(finalBadTrained > 0)
                                        detailedStatisticsIntent.putExtra("statsDetails", 2);
                                    else
                                        detailedStatisticsIntent.putExtra("statsDetails", 3);
                                    break;

                                case 2:
                                    detailedStatisticsIntent.putExtra("statsDetails", 3);
                                    break;

                                default:
                                    throw new IllegalStateException("Unexpected value: " + h.getDrawX());
                            }
                            detailedStatisticsIntent.putExtra("Category", categoryTOLoad);
                            startActivity(detailedStatisticsIntent);
                        }

                        @Override
                        public void onNothingSelected() {}
                    });
                    rightWrongPieChart.invalidate();

                    //Bar chart training
                    rightWrongBarChart.getDescription().setEnabled(false);
                    rightWrongBarChart.setTouchEnabled(true);
                    rightWrongBarChart.animateY(1500, Easing.EasingOption.EaseInBounce);
                    rightWrongBarChart.getXAxis().setEnabled(false);
                    rightWrongBarChart.getAxisLeft().setAxisMinimum(0f);
                    //rightWrongBarChart.getAxisLeft().setValueFormatter(new DefaultAxisValueFormatter(0));
                    rightWrongBarChart.getAxisLeft().setGranularity(1f);
                    rightWrongBarChart.getAxisLeft().setGranularityEnabled(true);
                    rightWrongBarChart.getAxisRight().setEnabled(false);
                    rightWrongBarChart.getLegend().setEnabled(false);

                    ArrayList<Double> rightWrongValues = new ArrayList<>();
                    ArrayList<BarEntry> rightWrongBarEntries = new ArrayList<>();

                    rightWrongValues.add((double) goodTrained);
                    rightWrongValues.add((double) badTrained);
                    rightWrongValues.add((double) untrained);

                    for (int i = 0; i < rightWrongValues.size(); i++) {
                        rightWrongBarEntries.add(new BarEntry(i,
                                rightWrongValues.get(i).floatValue()));
                    }

                    BarDataSet barDataSet = new BarDataSet(rightWrongBarEntries, "");
                    barDataSet.setColors(colors);
                    barDataSet.setValueTextSize(14);

                    BarData barData = new BarData(barDataSet);
                    barData.setValueFormatter(new DefaultValueFormatter(0));

                    rightWrongBarChart.setData(barData);
                    rightWrongBarChart.invalidate();

                    if (mode == MODE_LOAD_ALL) {
                        numberOfVocabs.setVisibility(View.VISIBLE);
                        rightWrongPieChart.setVisibility(View.VISIBLE);
                        rightWrongBarChart.setVisibility(View.VISIBLE);
                        trainedLast7Days.setVisibility(View.VISIBLE);
                        trainingAverage7Days.setVisibility(View.VISIBLE);
                        trainingHistory.setVisibility(View.VISIBLE);
                        header7Days.setVisibility(View.VISIBLE);
                        contentSeparator2.setVisibility(View.VISIBLE);

                        numberOfVocabs.setText(String.format("Du hast %s Vokabeln insgesamt",
                                vocabs.size()));

                        //Number of trained vocabs in last 7 days
                        LiveData<Integer> sumOfTrainedVocabs = weekdayViewModel
                                .getSumOfTrainedVocabs();
                        sumOfTrainedVocabs.observe(Statistics.this, (numberOfTrainedVocabs) -> {
                            if (numberOfTrainedVocabs != null) {
                                trainedLast7Days.setText(String.format(
                                        "Du hast %s Vokabeln in den letzten 7 Tagen gelernt.",
                                        numberOfTrainedVocabs));
                                sumOfTrainedVocabs.removeObservers(Statistics.this);
                            }
                        });

                        //Number of trained vocabs per day
                        LiveData<Float> averageVocabsPerDay = weekdayViewModel
                                .getAverageOfTrainedVocabs();
                        averageVocabsPerDay.observe(Statistics.this, (averagePerDay) -> {
                            if (averagePerDay != null) {
                                trainingAverage7Days.setText(String.format(
                                        "Durchschnittlich hast Du %s Vokabeln pro Tag gelernt.",
                                        Math.round(averagePerDay * 100f) / 100f));
                                averageVocabsPerDay.removeObservers(Statistics.this);
                            }
                        });

                        //LineChart
                        trainingHistory.setTouchEnabled(true);
                        trainingHistory.setPinchZoom(true);
                        trainingHistory.animateY(1500, Easing.EasingOption.EaseInQuad);
                        trainingHistory.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
                        trainingHistory.getAxisRight().setEnabled(false);
                        trainingHistory.getAxisLeft().setAxisMinimum(0f);
                        trainingHistory.getDescription().setEnabled(false);

                        LiveData<List<Weekday>> allWeekdays = weekdayViewModel.getAllDays();
                        allWeekdays.observe(Statistics.this, weekdays -> {
                            if (weekdays != null) {
                                Collections.reverse(weekdays);
                                ArrayList<Entry> historyEntries = new ArrayList<>();
                                for (int i = 0; i < 7; i++) {
                                    historyEntries.add(new Entry(i,
                                            weekdays.get(i).getNumberOfTrainedVocabs()));
                                }

                                LineDataSet lineDataSet = new LineDataSet(historyEntries, "Verlauf");
                                lineDataSet.setDrawIcons(false);
                                lineDataSet.enableDashedLine(10f, 5f, 0f);
                                lineDataSet.setColor(ContextCompat.getColor(this,
                                        R.color.colorPrimary4));
                                lineDataSet.setDrawCircles(false);
                                lineDataSet.setLineWidth(1.5f);
                                lineDataSet.setDrawFilled(true);
                                lineDataSet.setFillColor(ContextCompat.getColor(this,
                                        R.color.colorAccent));
                                lineDataSet.setDrawValues(false);
                                //lineDataSet.setValueTextSize(18f);
                                //lineDataSet.setValueTextColor(Color.BLACK);
                                lineDataSet.setFormLineWidth(1f);
                                lineDataSet.setFormLineDashEffect(
                                        new DashPathEffect(new float[]{10f, 5f}, 0f));
                                lineDataSet.setFormSize(15f);

                                //ArrayList<LineDataSet> lineDataSets = new ArrayList<>();
                                //lineDataSets.add(lineDataSet);
                                LineData lineData = new LineData(lineDataSet);
                                lineData.setValueFormatter(new DefaultValueFormatter(0));
                                List<String> xAxisValues = new ArrayList<>();
                                Log.d("weekdays", weekdays.toString());
                                for (Weekday weekday : weekdays) {
                                    xAxisValues.add(weekday.getNameOfDay().substring(0, 2));
                                }
                                Log.d("x-Axis", xAxisValues.toString());
                                trainingHistory.getXAxis().setValueFormatter(new IndexAxisValueFormatter(xAxisValues));
                                //trainingHistory.getAxisLeft().setValueFormatter(new DefaultAxisValueFormatter(0));
                                trainingHistory.getAxisLeft().setGranularity(1f);
                                trainingHistory.getAxisLeft().setGranularityEnabled(true);
                                trainingHistory.setData(lineData);
                            }
                        });
                    } else {
                        trainedLast7Days.setVisibility(View.GONE);
                        trainingAverage7Days.setVisibility(View.GONE);
                        trainingHistory.setVisibility(View.GONE);
                        header7Days.setVisibility(View.GONE);
                        contentSeparator2.setVisibility(View.GONE);
                    }
                } else {
                    numberOfVocabs.setVisibility(View.INVISIBLE);
                    rightWrongPieChart.setVisibility(View.INVISIBLE);
                    rightWrongBarChart.setVisibility(View.INVISIBLE);
                    trainedLast7Days.setVisibility(View.GONE);
                    trainingAverage7Days.setVisibility(View.GONE);
                    trainingHistory.setVisibility(View.GONE);
                    header7Days.setVisibility(View.GONE);
                    contentSeparator2.setVisibility(View.GONE);
                    LiveSmashBar.Builder warning = new LiveSmashBar.Builder(this);
                    warning.title("Es sind keine Vokabeln in dieser Kategorie vorhanden.");
                    warning.titleColor(Color.WHITE);
                    warning.backgroundColor(Color.parseColor("#541111"));
                    warning.gravity(GravityView.BOTTOM);
                    warning.dismissOnTapOutside();
                    warning.showOverlay();
                    warning.overlayBlockable();
                    warning.duration(3000);
                    warning.show();
                }
                vocabsLiveData.removeObservers(Statistics.this);
            }
        });
    }
    /*
    private void share(final View view) {
        viewToShare = view;

        if (Build.VERSION.SDK_INT >= 23) {
            String[] PERMISSIONS = {android.Manifest.permission.WRITE_EXTERNAL_STORAGE};
            if (!hasPermissions(Statistics.this, PERMISSIONS)) {
                ActivityCompat.requestPermissions(Statistics.this, PERMISSIONS, REQUEST );
            } else {
                String state = Environment.getExternalStorageState();
                File path;
                if (Environment.MEDIA_MOUNTED.equals(state)) {
                    path = new File(Environment.getExternalStorageDirectory() +
                            File.separator + "ScanQ" + File.separator + "shared_images" +
                            File.separator + "shared_chart" + System.currentTimeMillis() + ".jpeg");

                    //path = new File(Environment.getDataDirectory() + File.separator + "shared_images");
                } else {
                    path = new File(getFilesDir() + "ScanQ" +
                            File.separator + "shared_images" + File.separator
                            + "shared_chart" + System.currentTimeMillis() + ".jpeg");

                    //path = new File(Environment.getDataDirectory() + File.separator + "shared_images");
                }
                //final boolean success = Objects.requireNonNull(path.getParentFile()).mkdirs();
                final boolean success = Objects.requireNonNull(path).mkdirs();
                try {
                    path.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                Log.d("success", String.valueOf(success));

                Log.d("imgPath", path.getAbsolutePath());

                Bitmap imageToShare;
                View rootView = view.getRootView();
                view.setDrawingCacheEnabled(true);
                imageToShare = view.getDrawingCache();

                OutputStream fout = null;

                try {
                    fout = new FileOutputStream(path);
                    imageToShare.compress(Bitmap.CompressFormat.JPEG, 90, fout);
                    fout.flush();
                    fout.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }



                ((PieChart) view).saveToPath("shared_chart_" + System.currentTimeMillis(),
                        File.separator + "ScanQ" + File.separator + "shared_images" + File.separator);


            }
        } else {
            //TODO
        }



        Bitmap imageToShare;
        View rootView = view.getRootView();
        view.setDrawingCacheEnabled(true);
        imageToShare = view.getDrawingCache();

        OutputStream fout = null;
        File imageFile = new File(imgPath);

        try {
            fout = new FileOutputStream(imageFile);
            imageToShare.compress(Bitmap.CompressFormat.JPEG, 90, fout);
            fout.flush();
            fout.close();
        } catch (IOException e) {
            e.printStackTrace();
        }



        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM, imgPath);
        shareIntent.setType("image/jpeg");
        startActivity(Intent.createChooser(shareIntent, getResources().getText(R.string.send_to)));


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                share(viewToShare);
            } else {
                Toast.makeText(Statistics.this, "ScanQ hat keine Berechtigung" +
                        " auf den Speicher zuzugreifen.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private static boolean hasPermissions(Context context, String... permissions) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null &&
                permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) !=
                        PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }
    */
}

