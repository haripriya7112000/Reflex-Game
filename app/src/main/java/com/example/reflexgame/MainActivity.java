package com.example.reflexgame;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.widget.RelativeLayout;

import com.example.reflexgame.view.Reflexview;

public class MainActivity extends AppCompatActivity {
   private Reflexview gameView;
    @RequiresApi(api = VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        RelativeLayout layout=findViewById(R.id.relativeLayout);

        gameView= new Reflexview(this,getPreferences(Context.MODE_PRIVATE),layout);
        layout.addView(gameView,0);
    }

    @RequiresApi(api = VERSION_CODES.LOLLIPOP)
    @Override
    protected void onPause() {
        super.onPause();
        gameView.pause();
    }

    @RequiresApi(api = VERSION_CODES.LOLLIPOP)
    @Override
    protected void onResume() {
        super.onResume();
        gameView.resume(this);
    }
}