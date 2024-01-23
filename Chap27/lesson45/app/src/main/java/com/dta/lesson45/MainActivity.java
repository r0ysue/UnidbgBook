package com.dta.lesson45;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Build;
import android.os.Bundle;
import android.system.Os;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    private TextView tv_value;
    private EditText edt_key;
    private Button btn_read;

    @Override
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        try {
            Os.setenv("name","r0ysue",true);
        }catch (Exception ignore){}

        tv_value = findViewById(R.id.tv_value);
        edt_key = findViewById(R.id.edt_key);
        btn_read = findViewById(R.id.btn_read);

        btn_read.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onClick(View v) {
                String key = edt_key.getText().toString();
                try {
                    /*String value = Os.getenv(key);*/
                    String value = readEnv(key);
                    tv_value.setText(value);
                }catch (Exception ignore){}

            }
        });

        tv_value.setText(getSalt());

    }

    public native String readEnv(String key);

    public native String getSalt();
}