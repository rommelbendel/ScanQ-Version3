package com.rommelbendel.scanQ;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class VokabelAusgabeAdapter extends RecyclerView.Adapter<VokabelAusgabeAdapter.VokabelViewHolder> {

    class VokabelViewHolder extends RecyclerView.ViewHolder {
        private final TextView vokabelItemView;

        private VokabelViewHolder(View itemView) {
            super(itemView);
            vokabelItemView = itemView.findViewById(R.id.zeile);

            /*
            vokabelItemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int id = (int) v.getTag(1);
                    Vokabel clickedVokabel =
                }
            });
             */
        }
    }

    private final LayoutInflater layoutInflater;
    private List<Vokabel> vokabelCache;

    VokabelAusgabeAdapter(Context context) {
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
            if (nextVokabel.isMarkiert()) {
                vokabelViewHolder.vokabelItemView.setBackgroundColor(Color.parseColor("#f39C12"));
            }
            String text = nextVokabel.getVokabelENG() + " | " + nextVokabel.getVokabelDE();
            Log.d("Ausgabe", text);
            vokabelViewHolder.vokabelItemView.setText(text);
            //vokabelViewHolder.vokabelItemView.setTag(1, nextVokabel.getId());
        }
        else {
            vokabelViewHolder.vokabelItemView.setText(R.string.leere_datenbank_fehlermeldung);
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
