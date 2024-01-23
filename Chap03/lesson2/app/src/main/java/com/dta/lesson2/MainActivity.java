package com.dta.lesson2;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MainActivity extends AppCompatActivity {

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    private TextView tv = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Example of a call to a native method
        tv = findViewById(R.id.sample_text);
        tv.setText(stringFromJNI());
        findViewById(R.id.btn_md5).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tv.setText(md52("dta".getBytes()));
            }
        });

    }

    private String md5Java(byte[] data) {
        try {

            MessageDigest digest = MessageDigest.getInstance("md5");
            digest.update(data);
            byte[] digest1 = digest.digest();
            return byte2Hex(digest1);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String byte2Hex(byte[] data){
        StringBuilder sb = new StringBuilder();
        for (byte b : data){
            String s = Integer.toHexString(b & 0xFF);
            if (s.length() < 2){
                sb.append("0");
            }
            sb.append(s);
        }
        return sb.toString();
    }



    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();

    public native String md5(String data);

    public native String md52(byte[] data);
}