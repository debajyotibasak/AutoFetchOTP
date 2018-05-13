package com.debajyotibasak.autofetchotp;

import android.Manifest;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;
import java.util.Random;

import okhttp3.HttpUrl;

public class MainActivity extends AppCompatActivity {

    private Button mBtnFetchOtp;
    private LinearLayout mLayOtp;
    private EditText mEdtOtp;
    private ProgressBar mProgress;
    private TextView mTxvOtpSuccess;

    private static final int PERMISSION_REQUEST = 1;
    private String[] permissions = {Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS};

    private IncomingSmsReceiver incomingSmsReceiver;
    Handler handler = new Handler(Looper.getMainLooper());

    private void initView() {
        setContentView(R.layout.activity_main);
        mBtnFetchOtp = findViewById(R.id.btn_fetch_otp);
        mLayOtp = findViewById(R.id.lay_otp);
        mEdtOtp = findViewById(R.id.edt_otp);
        mProgress = findViewById(R.id.progress);
        mTxvOtpSuccess = findViewById(R.id.txt_otp_successful);
        incomingSmsReceiver = new IncomingSmsReceiver();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initView();

        mBtnFetchOtp.setOnClickListener(view -> checkPermissions());
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if ((ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED)
                    && (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED)) {
                sendSms(generateOTP());
            } else {
                ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST);
            }
        } else {
            sendSms(generateOTP());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST) {
            if (grantResults.length <= 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Please Enable sms read and recieve permissions for auto OTP", Toast.LENGTH_LONG).show();
            } else {
                sendSms(generateOTP());
            }
        }
    }

    private String generateOTP() {
        int len = 4;
        String numbers = "0123456789";
        Random random = new Random();
        char[] otp = new char[len];
        for (int i = 0; i < len; i++) {
            otp[i] = numbers.charAt(random.nextInt(numbers.length()));
        }
        return String.valueOf(otp);
    }

    private void sendSms(String otp) {
        mBtnFetchOtp.setVisibility(View.GONE);
        mLayOtp.setVisibility(View.VISIBLE);

        showProgress();
        HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse(Constants.SMS_API_URL)).newBuilder();
        urlBuilder.addQueryParameter("Mobile", Constants.WAY2SMS_MOBILE_NUMBER);
        urlBuilder.addQueryParameter("Password", Constants.WAY2SMS_PASSWORD);
        urlBuilder.addQueryParameter("Message", Constants.MESSAGE + otp);
        urlBuilder.addQueryParameter("To", Constants.MOBILE_NUMBER);
        urlBuilder.addQueryParameter("Key", Constants.SMS_API_KEY);
        String url = urlBuilder.build().toString();

        StringRequest jsonRequest = new StringRequest(Request.Method.GET, url,
                response -> {
                    String status = "";
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        status = jsonObject.getString("status");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    if (status.equalsIgnoreCase("success")) {
                        verifyOtp(otp);
                    } else {
                        Toast.makeText(this, "Some Internet Error", Toast.LENGTH_SHORT).show();
                        hideProgress();
                    }
                },

                error -> {
                    if (error instanceof NoConnectionError) {
                        Toast.makeText(this, "No Internet Connection. Please Try again", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                    hideProgress();
                });
        Volley.newRequestQueue(this).add(jsonRequest);
    }

    private void verifyOtp(String otp) {
        incomingSmsReceiver.bindListener(messageText -> {
            if (messageText != null) {
                if (messageText.equalsIgnoreCase(otp)) {
                    mEdtOtp.setText(messageText);
                    handler.postDelayed(() -> {
                        hideProgress();
                        mTxvOtpSuccess.setVisibility(View.VISIBLE);
                    }, 500);
                } else {
                    mEdtOtp.setText("");
                    Toast.makeText(this, "Otp did not match", Toast.LENGTH_SHORT).show();
                    hideProgress();
                }
            }
        });
    }

    private void showProgress() {
        mProgress.setVisibility(View.VISIBLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
    }

    private void hideProgress() {
        mProgress.setVisibility(View.GONE);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.BROADCAST_ACTION);
        registerReceiver(incomingSmsReceiver, intentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(incomingSmsReceiver);
    }
}
