package com.br.octo.board.modules.base;

import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;

public class BaseActivity extends AppCompatActivity {

    public static boolean keepScreen = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (keepScreen) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    public AlertDialog.Builder createDialog(int titleID, int messageID) {
        return new AlertDialog.Builder(this)
                .setTitle(getString(titleID))
                .setMessage(getString(messageID));
    }

}
