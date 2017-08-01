package com.br.octo.board.modules.history;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.br.octo.board.MainApplication;
import com.br.octo.board.R;
import com.br.octo.board.models.Paddle;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by endysilveira on 30/07/17.
 */

public class HistoryAdapter extends RecyclerView.Adapter<HistoryViewHolder> {

    private final List<Paddle> paddles;
    private final Context context;

    public HistoryAdapter(ArrayList paddles) {
        this.paddles = paddles;
        context = MainApplication.getOCTOContext();
    }

    @Override
    public HistoryViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new HistoryViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.history_list_item, parent, false));
    }

    @Override
    public void onBindViewHolder(HistoryViewHolder holder, int position) {
        holder.historyDistTV.setText(String.format("%.2f %s", paddles.get(position).getDistance(), context.getString(R.string.bt_dist)));
        holder.historyKcalTV.setText(String.format("%d %s", paddles.get(position).getKcal(), context.getString(R.string.bt_kcal)));

        int hour = (int) paddles.get(position).getDuration() / (60 * 60);
        int minutes = (int) (paddles.get(position).getDuration() / 60) % 60;
        holder.historyTimeTV.setText(String.format("%02d:%02d %s", hour, minutes, context.getString(R.string.bt_hour)));

        SimpleDateFormat dateFormatter = new SimpleDateFormat("dd.MM.yyyy", Locale.US);
        holder.historyDateTV.setText(dateFormatter.format(paddles.get(position).getDate()));
    }

    @Override
    public int getItemCount() {
        return paddles != null ? paddles.size() : 0;
    }
}
