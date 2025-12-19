package com.example.client_system;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class CustomerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;
    
    private Context context;
    private List<Object> displayList; // Customer 또는 String(날짜 헤더)을 담는 리스트
    private List<Customer> customerList;
    private OnItemClickListener listener;
    private OnCompleteClickListener completeListener;
    
    private static final SimpleDateFormat DATE_FORMAT = 
            new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA);

    public interface OnItemClickListener {
        void onItemClick(Customer customer);
    }

    public interface OnCompleteClickListener {
        void onCompleteClick(Customer customer, int position);
    }

    public CustomerAdapter(Context context, List<Customer> customerList) {
        this.context = context;
        this.customerList = customerList;
        this.displayList = new ArrayList<>();
        buildDisplayList();
    }
    
    private void buildDisplayList() {
        displayList.clear();
        if (customerList == null || customerList.isEmpty()) {
            return;
        }
        
        String currentDateHeader = null;
        Calendar today = Calendar.getInstance(TimeZone.getTimeZone("Asia/Seoul"));
        Calendar yesterday = Calendar.getInstance(TimeZone.getTimeZone("Asia/Seoul"));
        yesterday.add(Calendar.DAY_OF_YEAR, -1);
        
        String todayStr = DATE_FORMAT.format(today.getTime());
        String yesterdayStr = DATE_FORMAT.format(yesterday.getTime());
        
        for (Customer customer : customerList) {
            String customerDate = getDateFromCreatedDate(customer.getCreatedDateRaw());
            // 날짜를 못 찾으면 오늘 날짜로 처리
            if (customerDate == null) {
                customerDate = todayStr;
            }
            
            // 날짜가 바뀌면 헤더 추가
            if (!customerDate.equals(currentDateHeader)) {
                currentDateHeader = customerDate;
                String headerText = formatDateHeader(customerDate, todayStr, yesterdayStr);
                displayList.add(headerText);
            }
            
            displayList.add(customer);
        }
    }
    
    private String getDateFromCreatedDate(String createdDateRaw) {
        if (createdDateRaw == null || createdDateRaw.isEmpty()) {
            return null;
        }
        try {
            if (createdDateRaw.contains("T")) {
                return createdDateRaw.substring(0, 10); // "2025-12-19"
            } else if (createdDateRaw.length() >= 10) {
                // T가 없어도 날짜 형식이면 추출
                return createdDateRaw.substring(0, 10);
            }
        } catch (Exception e) {
            // ignore
        }
        return null;
    }
    
    private String formatDateHeader(String dateStr, String todayStr, String yesterdayStr) {
        if (dateStr.equals(todayStr)) {
            return "오늘";
        } else if (dateStr.equals(yesterdayStr)) {
            return "어제 (" + dateStr + ")";
        } else {
            return dateStr;
        }
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setOnCompleteClickListener(OnCompleteClickListener listener) {
        this.completeListener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        if (position < 0 || position >= displayList.size()) {
            return TYPE_ITEM;
        }
        Object item = displayList.get(position);
        return (item instanceof String) ? TYPE_HEADER : TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_date_header, parent, false);
            return new DateHeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_customer, parent, false);
            return new CustomerViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (position < 0 || position >= displayList.size()) {
            return;
        }
        
        Object item = displayList.get(position);
        
        if (holder instanceof DateHeaderViewHolder) {
            DateHeaderViewHolder headerHolder = (DateHeaderViewHolder) holder;
            if (item instanceof String) {
                headerHolder.tvDateHeader.setText((String) item);
            }
            return;
        }
        
        if (!(holder instanceof CustomerViewHolder) || !(item instanceof Customer)) {
            return;
        }
        
        CustomerViewHolder customerHolder = (CustomerViewHolder) holder;
        Customer customer = (Customer) item;
        
        // 실제 리스트에서의 위치 찾기
        int actualPosition = customerList.indexOf(customer);

        // 카드 배경 설정 (타입별)
        customerHolder.cardView.setBackgroundResource(customer.getCardBackground());

        // 상태 텍스트 (이모지 + 텍스트)
        customerHolder.tvStatus.setText(customer.getEmoji() + " " + customer.getStatus());

        // 시간 + 위치
        customerHolder.tvTime.setText("⏰ " + customer.getRelativeTime() + " · " + customer.getLocation());

        // 이미지 로드
        if (customer.getImageUrl() != null && !customer.getImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(customer.getImageUrl())
                    .placeholder(R.drawable.bg_image)
                    .into(customerHolder.ivCustomer);
        } else {
            customerHolder.ivCustomer.setImageResource(R.drawable.bg_image);
        }

        // 완료 상태에 따른 UI 변경
        boolean isCompleted = customer.isCompleted();
        if (isCompleted) {
            // 완료된 경우: 버튼 숨기고 화살표 숨기기
            customerHolder.btnComplete.setVisibility(View.GONE);
            customerHolder.tvArrow.setVisibility(View.GONE);
        } else {
            // 미완료인 경우: 버튼 표시하고 화살표 표시
            customerHolder.btnComplete.setVisibility(View.VISIBLE);
            customerHolder.tvArrow.setVisibility(View.VISIBLE);
            
            // 응대 완료 버튼 클릭 이벤트
            customerHolder.btnComplete.setOnClickListener(v -> {
                if (completeListener != null) {
                    completeListener.onCompleteClick(customer, actualPosition);
                }
            });
        }

        // 카드 클릭 이벤트 (상세 화면으로 이동) - 완료 여부와 관계없이 항상 활성화
        customerHolder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(customer);
            }
        });
    }

    @Override
    public int getItemCount() {
        return displayList.size();
    }

    // 데이터 업데이트
    public void updateData(List<Customer> newCustomerList) {
        this.customerList = newCustomerList;
        buildDisplayList();
        notifyDataSetChanged();
    }

    static class DateHeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvDateHeader;

        public DateHeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDateHeader = itemView.findViewById(R.id.tv_date_header);
        }
    }

    static class CustomerViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        ImageView ivCustomer;
        TextView tvStatus;
        TextView tvTime;
        Button btnComplete;
        TextView tvArrow;

        public CustomerViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (CardView) itemView;
            ivCustomer = itemView.findViewById(R.id.iv_customer);
            tvStatus = itemView.findViewById(R.id.tv_status);
            tvTime = itemView.findViewById(R.id.tv_time);
            btnComplete = itemView.findViewById(R.id.btn_complete);
            tvArrow = itemView.findViewById(R.id.tv_arrow);
        }
    }
}