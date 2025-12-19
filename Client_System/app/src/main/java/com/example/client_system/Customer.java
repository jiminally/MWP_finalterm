package com.example.client_system;

public class Customer {
    private String id;
    private String type;        // "new", "call", "delivery"
    private String imageUrl;
    private String status;
    private String date;        // 표시용 날짜 (예: 2024.12.13)
    private String time;        // 표시용 시간 (예: 15:30)
    private String relativeTime; // 목록에서 사용할 상대 시간 (예: 2분 전)
    private String location;
    private String createdDateRaw; // 원본 created_date (필터링용)
    private boolean isCompleted;

    public Customer(String id, String type, String imageUrl, String status,
                    String date, String time, String relativeTime, String location, String createdDateRaw) {
        this.id = id;
        this.type = type;
        this.imageUrl = imageUrl;
        this.status = status;
        this.date = date;
        this.time = time;
        this.relativeTime = relativeTime;
        this.location = location;
        this.createdDateRaw = createdDateRaw;
        this.isCompleted = false;
    }

    // Getters
    public String getId() { return id; }
    public String getType() { return type; }
    public String getImageUrl() { return imageUrl; }
    public String getStatus() { return status; }
    public String getDate() { return date; }
    public String getTime() { return time; }
    public String getRelativeTime() { return relativeTime; }
    public String getLocation() { return location; }
    public String getCreatedDateRaw() { return createdDateRaw; }
    public boolean isCompleted() { return isCompleted; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setType(String type) { this.type = type; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setStatus(String status) { this.status = status; }
    public void setDate(String date) { this.date = date; }
    public void setTime(String time) { this.time = time; }
    public void setRelativeTime(String relativeTime) { this.relativeTime = relativeTime; }
    public void setLocation(String location) { this.location = location; }
    public void setCreatedDateRaw(String createdDateRaw) { this.createdDateRaw = createdDateRaw; }
    public void setCompleted(boolean completed) { isCompleted = completed; }

    // 타입별 이모지 반환
    public String getEmoji() {
        switch (type) {
            case "call":
                return "🔔";
            case "delivery":
                return "📦";
            case "new":
                return "⭐";
            default:
                return "👤";
        }
    }

    // 타입별 색상 반환 (drawable 리소스)
    public int getCardBackground() {
        if (isCompleted) {
            return R.drawable.bg_card_completed;
        }
        switch (type) {
            case "call":
                return R.drawable.bg_card_call;
            case "delivery":
                return R.drawable.bg_card_delivery;
            case "new":
                return R.drawable.bg_card_new;
            default:
                return R.drawable.bg_counter;
        }
    }
}