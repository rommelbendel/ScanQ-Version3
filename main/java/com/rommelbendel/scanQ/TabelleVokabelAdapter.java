package com.rommelbendel.scanQ;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TabelleVokabelAdapter extends RecyclerView.Adapter<TabelleVokabelAdapter.VokabelViewHolder> {

    class VokabelViewHolder extends RecyclerView.ViewHolder {
        private final EditText vokabelEnEdit;
        private final EditText vokabelDeEdit;

        private VokabelViewHolder(View itemView) {
            super(itemView);
            vokabelEnEdit = itemView.findViewById(R.id.zeile);
            vokabelDeEdit = itemView.findViewById(R.id.zeile2);
        }
    }

    private final LayoutInflater layoutInflater;
    private List<Vokabel> vokabelCache;

    TabelleVokabelAdapter(Context context) {
        layoutInflater = LayoutInflater.from(context);
    }

    @Override
    public VokabelViewHolder onCreateViewHolder(ViewGroup viewGroup, int art) {
        View zeilenView = layoutInflater.inflate(R.layout.dataset, viewGroup, false);
        return new VokabelViewHolder(zeilenView);
    }

    @Override
    public void onBindViewHolder(VokabelViewHolder vokabelViewHolder, int position) {
        if (vokabelCache != null) {
            Vokabel nextVokabel = vokabelCache.get(position);
            String engl = nextVokabel.getVokabelENG();
            String de = nextVokabel.getVokabelDE();
            vokabelViewHolder.vokabelEnEdit.setText(engl);
            vokabelViewHolder.vokabelDeEdit.setText(de);
        }
        else {
            vokabelViewHolder.vokabelEnEdit.setText(R.string.leere_datenbank_fehlermeldung);
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
