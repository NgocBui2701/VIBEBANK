package com.example.vibebank;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.vibebank.utils.MovieTicketMockService;
import com.example.vibebank.utils.TicketDatabaseService;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class MyMovieTicketsActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private LinearLayout llTickets;
    private TextView tvEmptyState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_movie_tickets);

        // Initialize services
        MovieTicketMockService.init(this);
        TicketDatabaseService.init();
        android.util.Log.d("MyMovieTickets", "TicketDatabaseService initialized");

        initViews();
        setupListeners();
        loadBookedTickets();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        llTickets = findViewById(R.id.llTickets);
        tvEmptyState = findViewById(R.id.tvEmptyState);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
    }

    private void loadBookedTickets() {
        android.util.Log.d("MyMovieTickets", "Loading movie tickets from Firestore...");
        
        // Show loading state
        llTickets.removeAllViews();
        TextView tvLoading = new TextView(this);
        tvLoading.setText("Đang tải vé...");
        tvLoading.setTextSize(16);
        tvLoading.setPadding(16, 16, 16, 16);
        llTickets.addView(tvLoading);
        tvEmptyState.setVisibility(View.GONE);
        
        com.example.vibebank.utils.TicketDatabaseService.loadMovieTickets(data -> {
            android.util.Log.d("MyMovieTickets", "Firestore returned " + data.size() + " tickets");
            
            // Remove loading view
            llTickets.removeAllViews();
            
            if (data.isEmpty()) {
                tvEmptyState.setVisibility(View.VISIBLE);
                llTickets.setVisibility(View.GONE);
                return;
            }

            tvEmptyState.setVisibility(View.GONE);
            llTickets.setVisibility(View.VISIBLE);

            for (java.util.Map<String, Object> map : data) {
                try {
                    View ticketView = getLayoutInflater().inflate(R.layout.item_booked_movie_ticket, llTickets, false);
                    
                    TextView tvBookingId = ticketView.findViewById(R.id.tvBookingId);
                    TextView tvMovieTitle = ticketView.findViewById(R.id.tvMovieTitle);
                    TextView tvCinemaName = ticketView.findViewById(R.id.tvCinemaName);
                    TextView tvDate = ticketView.findViewById(R.id.tvDate);
                    TextView tvTime = ticketView.findViewById(R.id.tvTime);
                    TextView tvSeats = ticketView.findViewById(R.id.tvSeats);
                    TextView tvCustomerName = ticketView.findViewById(R.id.tvCustomerName);
                    TextView tvCustomerPhone = ticketView.findViewById(R.id.tvCustomerPhone);
                    TextView tvCustomerEmail = ticketView.findViewById(R.id.tvCustomerEmail);
                    TextView tvTotalPrice = ticketView.findViewById(R.id.tvTotalPrice);
                    TextView tvBookingTime = ticketView.findViewById(R.id.tvBookingTime);

                    tvBookingId.setText("Mã đặt vé: " + map.get("bookingId"));
                    tvMovieTitle.setText((String) map.get("movieTitle"));
                    tvCinemaName.setText((String) map.get("cinemaName"));
                    tvDate.setText((String) map.get("date"));
                    tvTime.setText((String) map.get("time"));
                    
                    // Display seats
                    @SuppressWarnings("unchecked")
                    java.util.List<String> seats = (java.util.List<String>) map.get("seats");
                    if (seats != null) {
                        java.util.Collections.sort(seats);
                        tvSeats.setText(String.join(", ", seats));
                    }
                    
                    tvCustomerName.setText((String) map.get("customerName"));
                    tvCustomerPhone.setText((String) map.get("customerPhone"));
                    tvCustomerEmail.setText((String) map.get("customerEmail"));
                    
                    long price = map.get("totalPrice") instanceof Long ? 
                        (Long) map.get("totalPrice") : ((Number) map.get("totalPrice")).longValue();
                    tvTotalPrice.setText(String.format(java.util.Locale.getDefault(), "%,d VNĐ", price));
                    tvBookingTime.setText("Đặt lúc: " + map.get("bookingTime"));

                    llTickets.addView(ticketView);
                } catch (Exception e) {
                    android.util.Log.e("MyMovieTickets", "Error displaying ticket", e);
                }
            }
        });
    }

    private String formatMoney(long amount) {
        return NumberFormat.getNumberInstance(Locale.US).format(amount);
    }
}
