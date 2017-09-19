package com.br.octo.board.modules.history;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.MenuItem;
import android.widget.TextView;

import com.br.octo.board.R;
import com.br.octo.board.models.Paddle;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.realm.Realm;
import io.realm.RealmConfiguration;

public class HistoryActivity extends AppCompatActivity {

    private HistoryAdapter historyAdapter;

    @BindView(R.id.historyLabel)
    TextView historyLabel;
    @BindView(R.id.historyRecyclerView)
    RecyclerView historyRecyclerView;

    //region lifecycle

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        ButterKnife.bind(this);
        setupActionBar();
        loadHistoryInfo();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    //endregion

    //region Private

    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    private void loadHistoryInfo() {
        RealmConfiguration realmConfiguration = new RealmConfiguration.Builder().build();
        Realm realm = Realm.getInstance(realmConfiguration);
        if (realm.where(Paddle.class).findAllSorted("id").size() > 0) {
            int nrOfPaddles = realm.where(Paddle.class).findAllSorted("id").size();
            int nrToShow = nrOfPaddles > 20 ? 20 : nrOfPaddles;
            ArrayList<Paddle> historyPaddles = new ArrayList<>();

            String nrString = "<font color=\"#21B24B\"> " + String.valueOf(nrToShow) + " </font>";
            historyLabel.setText(Html.fromHtml(getString(R.string.history_prefix) + nrString + getString(R.string.history_suffix)));

            for (int i = 1; i <= nrToShow; i++) {
                historyPaddles.add(realm.copyFromRealm(realm.where(Paddle.class).findAllSorted("id").get(nrOfPaddles - i)));
            }

            historyAdapter = new HistoryAdapter(historyPaddles);

            LinearLayoutManager layoutManager = new LinearLayoutManager(this);
            historyRecyclerView.setLayoutManager(layoutManager);
            historyRecyclerView.setAdapter(historyAdapter);
        } else {
            historyLabel.setText(getString(R.string.history_empty));
        }
        realm.close();
    }
}
