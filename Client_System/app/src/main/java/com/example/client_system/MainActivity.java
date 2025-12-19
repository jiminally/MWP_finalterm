package com.example.client_system;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int REQUEST_NOTIFICATION_PERMISSION = 100;

    // UI 요소
    RecyclerView recyclerView;
    CustomerAdapter adapter;
    List<Customer> customerList;
    TextView tvWelcome;
    TextView tvCallCount, tvDeliveryCount, tvNewCount;
    ImageButton btnSettings, btnDeleteOld;

    // 서버 정보
    String site_url = "http://10.0.2.2:8000";
    String apiToken = "bf46b8f9337d1d27b4ef2511514c798be1a954b8";

    CloadImage taskDownload;

    // created_date 파싱용 포맷터
    private static final SimpleDateFormat CREATED_AT_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.KOREA);

    // BroadcastReceiver
    private BroadcastReceiver notificationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "🔄 새 알림 수신! 자동 새로고침 시작");
            Toast.makeText(MainActivity.this, "새 알림! 자동 새로고침 중...", Toast.LENGTH_SHORT).show();
            loadCustomers();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // UI 초기화
        initViews();

        // RecyclerView 설정
        setupRecyclerView();

        // LocalBroadcastManager 등록
        LocalBroadcastManager.getInstance(this).registerReceiver(
                notificationReceiver,
                new IntentFilter("FCM_NOTIFICATION_RECEIVED")
        );

        // 알림 권한 요청 (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "알림 권한 요청");
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_NOTIFICATION_PERMISSION);
            } else {
                Log.d(TAG, "알림 권한 이미 허용됨");
            }
        }

        // FCM 토큰 가져오기 및 서버 전송
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "FCM 토큰 가져오기 실패", task.getException());
                        return;
                    }

                    String fcmToken = task.getResult();
                    Log.d(TAG, "========================================");
                    Log.d(TAG, "FCM 토큰: " + fcmToken);
                    Log.d(TAG, "========================================");

                    sendTokenToServer(fcmToken);
                });

        // 초기 데이터 로드
        loadCustomers();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        tvCallCount = findViewById(R.id.tv_call_count);
        tvDeliveryCount = findViewById(R.id.tv_delivery_count);
        tvNewCount = findViewById(R.id.tv_new_count);
        btnSettings = findViewById(R.id.btn_settings);
        btnDeleteOld = findViewById(R.id.btn_delete_old);

        // 설정 버튼 클릭
        btnSettings.setOnClickListener(v -> {
            Toast.makeText(this, "설정 (나중에 구현)", Toast.LENGTH_SHORT).show();
        });

        // 하루 지난 항목 일괄 삭제 버튼 클릭
        btnDeleteOld.setOnClickListener(v -> {
            deleteOldCustomers();
        });
    }

    private void setupRecyclerView() {
        customerList = new ArrayList<>();
        adapter = new CustomerAdapter(this, customerList);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // 아이템 클릭 이벤트 - 상세보기로 이동
        adapter.setOnItemClickListener(customer -> {
            Intent intent = new Intent(MainActivity.this, DetailActivity.class);
            intent.putExtra("customerId", customer.getId());
            intent.putExtra("customerType", customer.getType());
            intent.putExtra("imageUrl", customer.getImageUrl());
            intent.putExtra("status", customer.getEmoji() + " " + customer.getStatus());
            intent.putExtra("date", customer.getDate());
            intent.putExtra("time", customer.getTime());
            intent.putExtra("location", customer.getLocation());
            startActivityForResult(intent, 100);
        });

        // 응대 완료 버튼 클릭 이벤트
        adapter.setOnCompleteClickListener((customer, position) -> {
            // 로컬에서만 완료 상태로 변경
            customer.setCompleted(true);
            
            // 같은 날짜 내에서 정렬: 미완료가 위로, 완료된 항목이 아래로
            Collections.sort(customerList, new Comparator<Customer>() {
                @Override
                public int compare(Customer c1, Customer c2) {
                    try {
                        String date1 = c1.getCreatedDateRaw();
                        String date2 = c2.getCreatedDateRaw();
                        
                        if (date1 == null || date1.isEmpty()) return 1;
                        if (date2 == null || date2.isEmpty()) return -1;
                        
                        if (date1.contains("T") && date2.contains("T")) {
                            // 날짜 부분 추출 (yyyy-MM-dd)
                            String datePart1 = date1.substring(0, 10);
                            String datePart2 = date2.substring(0, 10);
                            
                            // 먼저 날짜로 비교 (최신순)
                            int dateCompare = datePart2.compareTo(datePart1);
                            if (dateCompare != 0) {
                                return dateCompare;
                            }
                            
                            // 같은 날짜면 완료 상태로 비교 (미완료가 위로)
                            boolean completed1 = c1.isCompleted();
                            boolean completed2 = c2.isCompleted();
                            if (completed1 != completed2) {
                                return completed1 ? 1 : -1; // 완료된 항목이 아래로
                            }
                            
                            // 같은 날짜, 같은 완료 상태면 시간으로 비교 (최신이 위로)
                            String base1 = date1.substring(0, 19);
                            String base2 = date2.substring(0, 19);
                            
                            CREATED_AT_FORMAT.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
                            Date d1 = CREATED_AT_FORMAT.parse(base1);
                            Date d2 = CREATED_AT_FORMAT.parse(base2);
                            
                            if (d1 != null && d2 != null) {
                                return d2.compareTo(d1);
                            }
                        }
                    } catch (ParseException e) {
                        Log.e(TAG, "정렬 중 날짜 파싱 오류: " + e.getMessage());
                    }
                    return 0;
                }
            });
            
            // 어댑터 업데이트 (날짜별 정렬 포함)
            adapter.updateData(customerList);
            
            updateCounts();
            Toast.makeText(this, "응대 완료 처리되었습니다.", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadCustomers() {
        if (taskDownload != null && taskDownload.getStatus() == AsyncTask.Status.RUNNING) {
            taskDownload.cancel(true);
        }

        taskDownload = new CloadImage();
        taskDownload.execute(site_url + "/api_root/Post/");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            String action = data.getStringExtra("action");
            String customerId = data.getStringExtra("customerId");
            
            if ("complete".equals(action) && customerId != null) {
                // 응대 완료: 로컬에서만 상태 변경
                for (Customer customer : customerList) {
                    if (customer.getId().equals(customerId)) {
                        customer.setCompleted(true);
                        Log.d(TAG, "응대 완료 처리: Customer ID " + customerId);
                        break;
                    }
                }
                // 어댑터 업데이트 및 카운트 재계산
                adapter.updateData(customerList);
                updateCounts();
                Toast.makeText(this, "응대 완료 처리되었습니다.", Toast.LENGTH_SHORT).show();
            } else if ("delete".equals(action)) {
                // 삭제: 서버에서 새로고침
                Log.d(TAG, "상세 화면에서 삭제 발생, 리스트 새로고침");
                loadCustomers();
            } else {
                // 기타 액션: 서버에서 새로고침
                Log.d(TAG, "상세 화면에서 변경사항 발생, 리스트 새로고침");
                loadCustomers();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(notificationReceiver);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_NOTIFICATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "✅ 알림 권한 허용됨");
                Toast.makeText(this, "알림 권한이 허용되었습니다", Toast.LENGTH_SHORT).show();
            } else {
                Log.d(TAG, "❌ 알림 권한 거부됨");
                Toast.makeText(this, "알림 권한이 필요합니다", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void sendTokenToServer(String token) {
        new Thread(() -> {
            try {
                String urlString = site_url + "/api/fcm-token/";
                Log.d(TAG, "전송 URL: " + urlString);

                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Token " + apiToken);
                conn.setDoOutput(true);

                JSONObject jsonParam = new JSONObject();
                jsonParam.put("token", token);

                OutputStream os = conn.getOutputStream();
                os.write(jsonParam.toString().getBytes("UTF-8"));
                os.close();

                int responseCode = conn.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_OK ||
                        responseCode == HttpURLConnection.HTTP_CREATED) {
                    Log.d(TAG, "✅ FCM 토큰 서버 등록 성공!");
                    runOnUiThread(() ->
                            Toast.makeText(MainActivity.this,
                                    "알림 설정 완료", Toast.LENGTH_SHORT).show()
                    );
                } else {
                    Log.e(TAG, "❌ FCM 토큰 등록 실패: " + responseCode);
                }

                conn.disconnect();

            } catch (Exception e) {
                Log.e(TAG, "FCM 토큰 전송 중 에러: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    private class CloadImage extends AsyncTask<String, Integer, List<Customer>> {

        @Override
        protected List<Customer> doInBackground(String... urls) {
            List<Customer> customers = new ArrayList<>();

            try {
                String apiUrl = urls[0];
                String token = apiToken;

                Log.d(TAG, "API 호출: " + apiUrl);

                URL urlAPI = new URL(apiUrl);
                HttpURLConnection conn = (HttpURLConnection) urlAPI.openConnection();

                conn.setRequestProperty("Authorization", "Token " + token);
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);

                int responseCode = conn.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    InputStream is = conn.getInputStream();
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(is));
                    StringBuilder result = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }
                    is.close();

                    String strJson = result.toString();
                    Log.d(TAG, "받은 JSON: " + strJson);

                    JSONArray aryJson = new JSONArray(strJson);

                    for (int i = 0; i < aryJson.length(); i++) {
                        JSONObject post = (JSONObject) aryJson.get(i);

                        // 안전하게 필드 가져오기
                        String id = post.has("id") ? post.getString("id") : String.valueOf(i);
                        String title = post.has("title") ? post.getString("title") : "person";
                        String text = post.has("text") ? post.getString("text") : "";
                        String imageUrl = post.has("image") ? post.getString("image") : "";
                        // Django 모델의 created_date 필드 사용
                        String createdAt = post.has("created_date") ? post.getString("created_date") : "";

                        // ✨ 이거 추가!
                        String customerType = post.has("customer_type")
                                ? post.getString("customer_type")
                                : "new";

                        // 완료 상태 확인
                        boolean isCompleted = post.has("is_completed") && post.getBoolean("is_completed");

                        Log.d(TAG, "Post #" + i + " - ID: " + id + ", Title: " + title + ", Image: " + imageUrl + ", Completed: " + isCompleted);

                        // 타입에 따라 상태 메시지 결정
                        String status = "";
                        switch (customerType) {
                            case "call":
                                status = "손님 호출!";
                                break;
                            case "delivery":
                                status = "배달원 입장";
                                break;
                            case "new":
                            default:
                                status = "새로운 손님!";
                                break;
                        }

                        // 날짜/시간 포맷팅 (created_at 기반)
                        String displayDate = "";
                        String displayTime = "";
                        String relativeTime = "";
                        if (createdAt != null && !createdAt.isEmpty()) {
                            try {
                                // 예상 포맷: "2025-12-19T03:07:22.114017+09:00" (Django DateTimeField)
                                if (createdAt.contains("T")) {
                                    String[] parts = createdAt.split("T");
                                    String datePart = parts[0]; // 2024-12-13
                                    displayDate = datePart.replace("-", "."); // 2024.12.13

                                    String timePart = parts[1]; // 예: 03:07:22.114017+09:00
                                    if (timePart.length() >= 5) {
                                        String hhmm = timePart.substring(0, 5); // "03:07"
                                        try {
                                            int hour24 = Integer.parseInt(hhmm.substring(0, 2));
                                            String minute = hhmm.substring(3, 5);

                                            String ampm = (hour24 < 12) ? "오전" : "오후";
                                            int hour12 = hour24 % 12;
                                            if (hour12 == 0) hour12 = 12;

                                            displayTime = ampm + " " + hour12 + ":" + minute; // 예: "오전 3:07"
                                        } catch (NumberFormatException e) {
                                            // 혹시 파싱이 안 되면 HH:mm 그대로 사용
                                            displayTime = hhmm;
                                        }
                                    }

                                    // 상대 시간 계산
                                    try {
                                        String base = createdAt.substring(0, 19); // "2025-12-19T03:07:22"
                                        CREATED_AT_FORMAT.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
                                        Date createdDate = CREATED_AT_FORMAT.parse(base);
                                        if (createdDate != null) {
                                            long diffMillis = System.currentTimeMillis() - createdDate.getTime();
                                            long diffMinutes = diffMillis / (60 * 1000);
                                            long diffHours = diffMinutes / 60;
                                            long diffDays = diffHours / 24;

                                            if (diffMinutes < 1) {
                                                relativeTime = "방금 전";
                                            } else if (diffMinutes < 60) {
                                                relativeTime = diffMinutes + "분 전";
                                            } else if (diffHours < 24) {
                                                relativeTime = diffHours + "시간 전";
                                            } else {
                                                relativeTime = diffDays + "일 전";
                                            }
                                        }
                                    } catch (ParseException e) {
                                        relativeTime = "방금 전";
                                    }
                                } else {
                                    // T가 없으면 그냥 전체를 날짜로 사용
                                    displayDate = createdAt;
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "created_at 파싱 오류: " + e.getMessage());
                            }
                        }

                        if (displayDate.isEmpty()) displayDate = "알 수 없음";
                        if (displayTime.isEmpty()) displayTime = "방금 전";
                        if (relativeTime.isEmpty()) relativeTime = "방금 전";

                        // 타입에 따라 위치 결정
                        String location;
                        switch (customerType) {
                            case "call":
                                location = "카운터 앞";
                                break;
                            case "delivery":
                                location = "출입구 바깥쪽";
                                break;
                            case "new":
                            default:
                                location = "출입구 안쪽";
                                break;
                        }

                        Customer customer = new Customer(
                                id, customerType, imageUrl, status, displayDate, displayTime, relativeTime, location, createdAt
                        );
                        
                        // 오늘 날짜가 아니면 자동으로 응대 완료 상태로 설정
                        boolean shouldBeCompleted = isCompleted;
                        if (createdAt != null && !createdAt.isEmpty() && createdAt.contains("T")) {
                            try {
                                String customerDateStr = createdAt.substring(0, 10); // "2025-12-19"
                                Calendar today = Calendar.getInstance(TimeZone.getTimeZone("Asia/Seoul"));
                                String todayStr = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(today.getTime());
                                
                                // 오늘 날짜가 아니면 자동으로 완료 처리
                                if (!customerDateStr.equals(todayStr)) {
                                    shouldBeCompleted = true;
                                }
                            } catch (Exception e) {
                                // 날짜 파싱 실패 시 기존 값 유지
                            }
                        }
                        
                        customer.setCompleted(shouldBeCompleted);

                        customers.add(customer);
                    }
                }

            } catch (IOException | JSONException e) {
                e.printStackTrace();
                Log.e(TAG, "에러 발생: " + e.getMessage());
            }

            return customers;
        }

        @Override
        protected void onPostExecute(List<Customer> customers) {
            if (customers.isEmpty()) {
                Log.d(TAG, "총 0개 손님 로드 완료");
                Toast.makeText(MainActivity.this,
                        "불러올 데이터가 없습니다", Toast.LENGTH_SHORT).show();
            } else {
                // 정렬: 날짜별 최신순, 같은 날짜 내에서는 미완료가 위로, 완료된 항목이 아래로
                Collections.sort(customers, new Comparator<Customer>() {
                    @Override
                    public int compare(Customer c1, Customer c2) {
                        try {
                            String date1 = c1.getCreatedDateRaw();
                            String date2 = c2.getCreatedDateRaw();
                            
                            if (date1 == null || date1.isEmpty()) return 1;
                            if (date2 == null || date2.isEmpty()) return -1;
                            
                            if (date1.contains("T") && date2.contains("T")) {
                                // 날짜 부분 추출 (yyyy-MM-dd)
                                String datePart1 = date1.substring(0, 10);
                                String datePart2 = date2.substring(0, 10);
                                
                                // 먼저 날짜로 비교 (최신순)
                                int dateCompare = datePart2.compareTo(datePart1);
                                if (dateCompare != 0) {
                                    return dateCompare;
                                }
                                
                                // 같은 날짜면 완료 상태로 비교 (미완료가 위로)
                                boolean completed1 = c1.isCompleted();
                                boolean completed2 = c2.isCompleted();
                                if (completed1 != completed2) {
                                    return completed1 ? 1 : -1; // 완료된 항목이 아래로
                                }
                                
                                // 같은 날짜, 같은 완료 상태면 시간으로 비교 (최신이 위로)
                                String base1 = date1.substring(0, 19);
                                String base2 = date2.substring(0, 19);
                                
                                CREATED_AT_FORMAT.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
                                Date d1 = CREATED_AT_FORMAT.parse(base1);
                                Date d2 = CREATED_AT_FORMAT.parse(base2);
                                
                                if (d1 != null && d2 != null) {
                                    return d2.compareTo(d1);
                                }
                            }
                        } catch (ParseException e) {
                            Log.e(TAG, "정렬 중 날짜 파싱 오류: " + e.getMessage());
                        }
                        return 0;
                    }
                });

                // 데이터 업데이트
                customerList.clear();
                customerList.addAll(customers);
                adapter.updateData(customerList);

                // 카운트 업데이트
                updateCounts();

                Log.d(TAG, "총 " + customers.size() + "개 손님 로드 완료");
                Toast.makeText(MainActivity.this,
                        "로드 완료! (" + customers.size() + "개)",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    // 타입별 카운트 업데이트
    private void updateCounts() {
        int callCount = 0;
        int deliveryCount = 0;
        int newCount = 0;

        for (Customer customer : customerList) {
            if (customer.isCompleted()) continue;

            switch (customer.getType()) {
                case "call":
                    callCount++;
                    break;
                case "delivery":
                    deliveryCount++;
                    break;
                case "new":
                    newCount++;
                    break;
            }
        }

        tvCallCount.setText("🔔 " + callCount);
        tvDeliveryCount.setText("📦 " + deliveryCount);
        tvNewCount.setText("⭐ " + newCount);
    }

    // 오늘 날짜가 아닌 항목 일괄 삭제
    private void deleteOldCustomers() {
        // 확인 다이얼로그
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("일괄 삭제")
                .setMessage("오늘 날짜가 아닌 모든 항목을 삭제하시겠습니까?")
                .setPositiveButton("삭제", (dialog, which) -> {
                    btnDeleteOld.setEnabled(false);
                    Toast.makeText(this, "삭제 중...", Toast.LENGTH_SHORT).show();
                    deleteOldPostsFromServer();
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void deleteOldPostsFromServer() {
        new Thread(() -> {
            int responseCode = -1;
            String responseMessage = "";
            int deletedCount = 0;
            
            try {
                String urlString = site_url + "/api/delete-old-posts/";
                Log.d(TAG, "일괄 삭제 API 호출: " + urlString);
                
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("DELETE");
                conn.setRequestProperty("Authorization", "Token " + apiToken);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                responseCode = conn.getResponseCode();
                Log.d(TAG, "응답 코드: " + responseCode);

                // 응답 읽기
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    InputStream is = conn.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    is.close();
                    
                    String responseBody = response.toString();
                    Log.d(TAG, "응답 본문: " + responseBody);
                    
                    // JSON 파싱
                    try {
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        responseMessage = jsonResponse.optString("message", "");
                        deletedCount = jsonResponse.optInt("deleted_count", 0);
                    } catch (JSONException e) {
                        Log.e(TAG, "JSON 파싱 오류: " + e.getMessage());
                        responseMessage = responseBody;
                    }
                } else {
                    // 에러 응답 읽기
                    InputStream errorStream = conn.getErrorStream();
                    if (errorStream != null) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(errorStream));
                        StringBuilder errorResponse = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            errorResponse.append(line);
                        }
                        responseMessage = errorResponse.toString();
                        Log.e(TAG, "에러 응답: " + responseMessage);
                    }
                }
                
                conn.disconnect();
            } catch (Exception e) {
                final String message = e.getMessage();
                Log.e(TAG, "일괄 삭제 중 예외 발생: " + message, e);
                runOnUiThread(() -> {
                    btnDeleteOld.setEnabled(true);
                    Toast.makeText(MainActivity.this,
                            "일괄 삭제 중 오류가 발생했습니다: " + message,
                            Toast.LENGTH_LONG).show();
                });
                return;
            }

            final int finalResponseCode = responseCode;
            final String finalMessage = responseMessage;
            final int finalDeletedCount = deletedCount;
            runOnUiThread(() -> {
                btnDeleteOld.setEnabled(true);
                if (finalResponseCode == HttpURLConnection.HTTP_OK) {
                    String successMsg = finalMessage.isEmpty() 
                            ? finalDeletedCount + "개의 항목이 삭제되었습니다."
                            : finalMessage;
                    Toast.makeText(MainActivity.this,
                            successMsg,
                            Toast.LENGTH_LONG).show();
                    // 리스트 새로고침
                    loadCustomers();
                } else {
                    String errorMsg = "일괄 삭제 실패 (코드 " + finalResponseCode + ")";
                    if (!finalMessage.isEmpty()) {
                        errorMsg += "\n" + finalMessage;
                    }
                    Toast.makeText(MainActivity.this,
                            errorMsg,
                            Toast.LENGTH_LONG).show();
                }
            });
        }).start();
    }
}