package com.br.octo.board.modules.tutorial;

import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.VideoView;

import com.br.octo.board.R;

import butterknife.BindView;
import butterknife.ButterKnife;

public class TutorialActivity extends AppCompatActivity {

    @BindView(R.id.tutorialVideoView)
    VideoView tutorialVideoView;

    //region lifecycle

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutorial);
        ButterKnife.bind(this);

        this.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                | View.SYSTEM_UI_FLAG_IMMERSIVE);


        loadTutorialVideo();
    }

    //endregion

    //region Private

    private void loadTutorialVideo() {
        tutorialVideoView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (tutorialVideoView.isPlaying()) {
                        tutorialVideoView.pause();
                    } else {
                        tutorialVideoView.start();
                    }
                }
                return true;
            }
        });

        tutorialVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                finish();
            }
        });

        String path = "android.resource://" + getPackageName() + "/" + R.raw.ex;
        tutorialVideoView.setVideoURI(Uri.parse(path));
        tutorialVideoView.start();
    }
}
