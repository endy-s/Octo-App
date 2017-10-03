package com.br.octo.board.modules.history;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.br.octo.board.R;

/**
 * Created by endysilveira on 30/07/17.
 */

public class HistoryViewHolder extends RecyclerView.ViewHolder {

    TextView historyDistTV, historyTimeTV, historyKcalTV, historyDateTV;

    public HistoryViewHolder(View itemView) {
        super(itemView);
        historyDistTV = (TextView) itemView.findViewById(R.id.historyDistTV);
        historyTimeTV = (TextView) itemView.findViewById(R.id.historyTimeTV);
        historyKcalTV = (TextView) itemView.findViewById(R.id.historyKcalTV);
        historyDateTV = (TextView) itemView.findViewById(R.id.historyDateTV);
    }
}
