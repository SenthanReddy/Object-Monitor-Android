package com.example.senthan.help;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;

import static com.example.senthan.help.R.styleable.View;

public class MainActivity extends AppCompatActivity {
    Button help = (Button) findViewById(R.id.help);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }
}
