package com.example.client_system;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import org.json.JSONObject;

import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class DetailActivity extends AppCompatActivity {
    private ImageView ivDetailImage;
    private TextView tvDetailStatus, tvDetailDate, tvDetailTime, tvDetailLocation;
    private Button btnDelete;
    private ImageButton btnBack;

    private String customerId;
    private String customerType;
    private String imageUrl;
    private String status;
    private String date;
    private String time;
    private String location;

    // MainActivity와 동일한 서버 정보 (공유 상수)
    private static final String SITE_URL = "http://10.0.2.2:8000";
    private static final String API_TOKEN = "bf46b8f9337d1d27b4ef2511514c798be1a954b8";
    private static final String TAG = "DetailActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        // UI 초기화
        initViews();

        // Intent에서 데이터 받기
        getIntentData();

        // 데이터 표시
        displayData();

        // 버튼 이벤트
        setupButtons();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        ivDetailImage = findViewById(R.id.iv_detail_image);
        tvDetailStatus = findViewById(R.id.tv_detail_status);
        tvDetailDate = findViewById(R.id.tv_detail_date);
        tvDetailTime = findViewById(R.id.tv_detail_time);
        tvDetailLocation = findViewById(R.id.tv_detail_location);
        btnDelete = findViewById(R.id.btn_delete);
    }

    private void getIntentData() {
        customerId = getIntent().getStringExtra("customerId");
        customerType = getIntent().getStringExtra("customerType");
        imageUrl = getIntent().getStringExtra("imageUrl");
        status = getIntent().getStringExtra("status");
        date = getIntent().getStringExtra("date");
        time = getIntent().getStringExtra("time");
        location = getIntent().getStringExtra("location");
    }

    private void displayData() {
        // 이미지 로드
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.bg_image)
                    .into(ivDetailImage);
        }

        // 상태
        tvDetailStatus.setText(status);

        // 날짜
        tvDetailDate.setText(date);

        // 시간
        tvDetailTime.setText(time);

        // 위치
        tvDetailLocation.setText(location);
    }

    private void setupButtons() {
        // 뒤로가기
        btnBack.setOnClickListener(v -> finish());

        // 삭제
        btnDelete.setOnClickListener(v -> {
            // 확인 다이얼로그
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("삭제")
                    .setMessage("이 손님을 삭제하시겠습니까?")
                    .setPositiveButton("삭제", (dialog, which) -> {
                        // 중복 클릭 방지
                        btnDelete.setEnabled(false);
                        Toast.makeText(this, "삭제 중입니다...", Toast.LENGTH_SHORT).show();
                        deleteCustomer();
                    })
                    .setNegativeButton("취소", (dialog, which) -> {
                        // 취소 시 아무것도 하지 않음
                    })
                    .show();
        });
    }

    /**
     * 현재 상세 화면의 손님을 서버에서 삭제하는 DELETE API 호출
     * DELETE  /api_root/Post/{id}/
     */
    private void deleteCustomer() {
        if (customerId == null || customerId.isEmpty()) {
            Toast.makeText(this, "삭제 대상 ID가 없습니다.", Toast.LENGTH_LONG).show();
            btnDelete.setEnabled(true);
            return;
        }

        new Thread(() -> {
            int responseCode = -1;
            String errorMessage = "";
            try {
                String urlString = SITE_URL + "/api_root/Post/" + customerId + "/";
                Log.d(TAG, "삭제 API 호출: " + urlString);
                Log.d(TAG, "Customer ID: " + customerId);
                

                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("DELETE");
                conn.setRequestProperty("Authorization", "Token " + API_TOKEN);
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);

                responseCode = conn.getResponseCode();
                Log.d(TAG, "응답 코드: " + responseCode);

                // 에러 응답 읽기
                if (responseCode >= 400) {
                    InputStream errorStream = conn.getErrorStream();
                    if (errorStream != null) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(errorStream));
                        StringBuilder errorResponse = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            errorResponse.append(line);
                        }
                        errorMessage = errorResponse.toString();
                        Log.e(TAG, "에러 응답: " + errorMessage);
                    }
                }
                
                conn.disconnect();
            } catch (Exception e) {
                final String message = e.getMessage();
                Log.e(TAG, "삭제 중 예외 발생: " + message, e);
                runOnUiThread(() -> {
                    Toast.makeText(DetailActivity.this,
                            "삭제 중 오류가 발생했습니다: " + message,
                            Toast.LENGTH_LONG).show();
                    btnDelete.setEnabled(true);
                });
                return;
            }

            final int finalResponseCode = responseCode;
            final String finalErrorMessage = errorMessage;
            runOnUiThread(() -> {
                if (finalResponseCode == HttpURLConnection.HTTP_NO_CONTENT
                        || finalResponseCode == HttpURLConnection.HTTP_OK) {
                    Toast.makeText(DetailActivity.this,
                            "손님이 삭제되었습니다.",
                            Toast.LENGTH_SHORT).show();
                    // 삭제 액션을 Intent에 담아서 MainActivity에 전달
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("action", "delete");
                    resultIntent.putExtra("customerId", customerId);
                    setResult(RESULT_OK, resultIntent);
                    finish();
                } else {
                    String errorMsg = "삭제 실패 (코드 " + finalResponseCode + ")";
                    if (!finalErrorMessage.isEmpty()) {
                        errorMsg += "\n" + finalErrorMessage;
                    }
                    Toast.makeText(DetailActivity.this,
                            errorMsg,
                            Toast.LENGTH_LONG).show();
                    btnDelete.setEnabled(true);
                }
            });
        }).start();
    }

}