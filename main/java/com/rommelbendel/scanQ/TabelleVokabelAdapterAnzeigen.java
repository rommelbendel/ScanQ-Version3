package com.rommelbendel.scanQ;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.yuvraj.livesmashbar.enums.GravityView;
import com.yuvraj.livesmashbar.view.LiveSmashBar;

import java.util.List;

public class TabelleVokabelAdapterAnzeigen extends RecyclerView.Adapter<TabelleVokabelAdapterAnzeigen.VokabelViewHolder> {

    public static final int OUTPUT_MODE_EDITABLE = 1;
    public static final int OUTPUT_MODE_VIEWABLE = 2;

    public final int outputMode;
    public final boolean longClickMode;
    private final Context context;
    private final List<Vokabel> vocabs;
    private final VokabelViewModel vokabelViewModel;

    class VokabelViewHolder extends RecyclerView.ViewHolder {
        private final EditText vokabelEnEdit;
        private final EditText vokabelDeEdit;
        private final TextView vokabelEnView;
        private final TextView vokabelDeView;
        private final CardView viewZeile;
        private final CardView viewZeile2;

        private VokabelViewHolder(View itemView) {
            super(itemView);
            viewZeile = itemView.findViewById(R.id.viewZeile);
            viewZeile2 = itemView.findViewById(R.id.viewZeile2);
            if (outputMode == OUTPUT_MODE_EDITABLE) {
                vokabelEnEdit = itemView.findViewById(R.id.zeile);
                vokabelDeEdit = itemView.findViewById(R.id.zeile2);
                vokabelDeView = null;
                vokabelEnView = null;
            } else {
                vokabelEnView = itemView.findViewById(R.id.zeile);
                vokabelDeView = itemView.findViewById(R.id.zeile2);
                vokabelDeEdit = null;
                vokabelEnEdit = null;
            }
        }
    }

    private final LayoutInflater layoutInflater;
    private List<Vokabel> vokabelCache;

    TabelleVokabelAdapterAnzeigen(Context context, int outputMode, List<Vokabel> vocabs, VokabelViewModel vm) {
        this.context = context;
        layoutInflater = LayoutInflater.from(context);
        this.outputMode = outputMode;
        this.vocabs = vocabs;
        this.vokabelViewModel = vm;
        this.longClickMode = true;
    }

    TabelleVokabelAdapterAnzeigen(Context context, int outputMode, List<Vokabel> vocabs, VokabelViewModel vm, boolean longClickMode) {
        this.context = context;
        layoutInflater = LayoutInflater.from(context);
        this.outputMode = outputMode;
        this.vocabs = vocabs;
        this.vokabelViewModel = vm;
        this.longClickMode = longClickMode;
    }

    @Override
    public VokabelViewHolder onCreateViewHolder(ViewGroup viewGroup, int art) {
        View zeilenView;
        if (outputMode == OUTPUT_MODE_EDITABLE) {
            zeilenView = layoutInflater.inflate(R.layout.dataset, viewGroup, false);
        } else {
            zeilenView = layoutInflater.inflate(R.layout.dataset_view, viewGroup, false);
        }
        return new VokabelViewHolder(zeilenView);
    }

    @Override
    public void onBindViewHolder(VokabelViewHolder vokabelViewHolder, int position) {

        if (vokabelViewHolder.vokabelEnView != null) {
            vokabelViewHolder.vokabelEnView.setOnLongClickListener(v -> {
                TextView vocText = (TextView) v;

                final String vocENG = (String) vocText.getText();
                Log.e("Vokabel", vocENG);
                Vokabel selectedVocab = null;

                for (Vokabel vocab: vocabs) {
                    if (vocab.getVokabelENG().equals(vocENG)) {
                        selectedVocab = vocab;
                    }
                }

                if (selectedVocab != null) {
                    AlertDialog.Builder markingDialog = new AlertDialog.Builder(
                            context);
                    markingDialog.setTitle(vocENG);

                    if (selectedVocab.isMarkiert()) {
                        markingDialog.setMessage("Markierung entfernen?");
                        Vokabel finalSelectedVocab = selectedVocab;
                        markingDialog.setPositiveButton("ja", (dialog, which) ->
                                vokabelViewModel.updateMarking(
                                        finalSelectedVocab.getVokabelDE(), false));
                    } else {
                        markingDialog.setMessage("Vokabel markieren?");
                        Vokabel finalSelectedVocab = selectedVocab;
                        markingDialog.setPositiveButton("ja", (dialog, which) ->
                                vokabelViewModel.updateMarking(
                                        finalSelectedVocab.getVokabelDE(), true));
                    }

                    markingDialog.setNegativeButton("nein", (dialog, which) ->
                            dialog.dismiss());
                    markingDialog.show();
                } else {
                    LiveSmashBar.Builder warning = new LiveSmashBar.Builder(
                            (Activity) context);
                    warning.title("Vokabel wurde nicht gefunden");
                    warning.titleColor(Color.WHITE);
                    warning.backgroundColor(Color.parseColor("#541111"));
                    warning.gravity(GravityView.BOTTOM);
                    warning.primaryActionText("OK");
                    warning.primaryActionEventListener(LiveSmashBar::dismiss);
                    warning.duration(3000);
                    warning.show();
                }

                return false;
            });
        }

        if(longClickMode) {
            if (vokabelViewHolder.vokabelDeView != null) {
                vokabelViewHolder.vokabelDeView.setOnLongClickListener(v -> {
                    TextView vocText = (TextView) v;

                    final String vocDe = (String) vocText.getText();
                    Log.e("Vokabel", vocDe);
                    Vokabel selectedVocab = null;

                    for (Vokabel vocab : vocabs) {
                        if (vocab.getVokabelDE().equals(vocDe)) {
                            selectedVocab = vocab;
                        }
                    }

                    if (selectedVocab != null) {
                        AlertDialog.Builder markingDialog = new AlertDialog.Builder(
                                context);
                        markingDialog.setTitle(vocDe);

                        if (selectedVocab.isMarkiert()) {
                            markingDialog.setMessage("Markierung entfernen?");
                            Vokabel finalSelectedVocab = selectedVocab;
                            markingDialog.setPositiveButton("ja", (dialog, which) ->
                                    vokabelViewModel.updateMarking(
                                            finalSelectedVocab.getVokabelDE(), false));
                        } else {
                            markingDialog.setMessage("Vokabel markieren?");
                            Vokabel finalSelectedVocab = selectedVocab;
                            markingDialog.setPositiveButton("ja", (dialog, which) ->
                                    vokabelViewModel.updateMarking(
                                            finalSelectedVocab.getVokabelDE(), true));
                        }

                        markingDialog.setNegativeButton("nein", (dialog, which) ->
                                dialog.dismiss());
                        markingDialog.show();
                    } else {
                        LiveSmashBar.Builder warning = new LiveSmashBar.Builder(
                                (Activity) context);
                        warning.title("Vokabel wurde nicht gefunden");
                        warning.titleColor(Color.WHITE);
                        warning.backgroundColor(Color.parseColor("#541111"));
                        warning.gravity(GravityView.BOTTOM);
                        warning.primaryActionText("OK");
                        warning.primaryActionEventListener(LiveSmashBar::dismiss);
                        warning.duration(3000);
                        warning.show();
                    }

                    return false;
                });
            }
        }

        if (vokabelCache != null) {
            Vokabel nextVokabel = vokabelCache.get(position);
            String engl = nextVokabel.getVokabelENG();
            String de = nextVokabel.getVokabelDE();

            int bgColor;
            if (nextVokabel.isMarkiert()) {
                bgColor = ContextCompat.getColor(context, R.color.colorMarkierung);
            } else {
                bgColor = ContextCompat.getColor(context, R.color.colorAccent);
            }

            if (outputMode == OUTPUT_MODE_EDITABLE) {
                vokabelViewHolder.vokabelEnEdit.setText(engl);
                vokabelViewHolder.vokabelDeEdit.setText(de);
                vokabelViewHolder.vokabelEnEdit.setTextSize(18);
                vokabelViewHolder.vokabelDeEdit.setTextSize(18);
                vokabelViewHolder.vokabelEnEdit.setBackgroundColor(bgColor);
                vokabelViewHolder.vokabelDeEdit.setBackgroundColor(bgColor);
                vokabelViewHolder.viewZeile.setBackgroundColor(bgColor);
                vokabelViewHolder.viewZeile2.setBackgroundColor(bgColor);
            } else {
                vokabelViewHolder.vokabelEnView.setText(engl);
                vokabelViewHolder.vokabelDeView.setText(de);
                vokabelViewHolder.vokabelEnView.setTextSize(18);
                vokabelViewHolder.vokabelDeView.setTextSize(18);
                vokabelViewHolder.vokabelEnView.setBackgroundColor(bgColor);
                vokabelViewHolder.vokabelDeView.setBackgroundColor(bgColor);
                vokabelViewHolder.viewZeile.setBackgroundColor(bgColor);
                vokabelViewHolder.viewZeile2.setBackgroundColor(bgColor);
            }

        }
    }

    @Override
    public int getItemCount() {
        if (vokabelCache != null) {
            return vokabelCache.size();
        }
        else {
            return 0;
        }
    }

    public void setVokabelCache(List<Vokabel> vokabelCache) {
        this.vokabelCache = vokabelCache;
        notifyDataSetChanged();
    }
}