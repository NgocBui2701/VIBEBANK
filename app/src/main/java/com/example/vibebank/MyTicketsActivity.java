package com.example.vibebank;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.vibebank.utils.FlightTicketMockService;
import com.example.vibebank.utils.TicketDatabaseService;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class MyTicketsActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private LinearLayout llTickets;
    private TextView tvEmptyState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_tickets);

        // Initialize services
        FlightTicketMockService.init(this);
        TicketDatabaseService.init();
        android.util.Log.d("MyTickets", "TicketDatabaseService initialized");

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
        android.util.Log.d("MyTickets", "Loading flight tickets from Firestore...");
        
        // Show loading state
        llTickets.removeAllViews();
        TextView tvLoading = new TextView(this);
        tvLoading.setText("Đang tải vé...");
        tvLoading.setTextSize(16);
        tvLoading.setPadding(16, 16, 16, 16);
        llTickets.addView(tvLoading);
        tvEmptyState.setVisibility(View.GONE);
        
        com.example.vibebank.utils.TicketDatabaseService.loadFlightTickets(data -> {
            android.util.Log.d("MyTickets", "Firestore returned " + data.size() + " tickets");
            
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
                    View ticketView = getLayoutInflater().inflate(R.layout.item_booked_ticket, llTickets, false);
                    
                    TextView tvBookingId = ticketView.findViewById(R.id.tvBookingId);
                    TextView tvFlightCode = ticketView.findViewById(R.id.tvFlightCode);
                    TextView tvAirline = ticketView.findViewById(R.id.tvAirline);
                    TextView tvRoute = ticketView.findViewById(R.id.tvRoute);
                    TextView tvDepartureDate = ticketView.findViewById(R.id.tvDepartureDate);
                    TextView tvDepartureTime = ticketView.findViewById(R.id.tvDepartureTime);
                    TextView tvArrivalTime = ticketView.findViewById(R.id.tvArrivalTime);
                    TextView tvDuration = ticketView.findViewById(R.id.tvDuration);
                    TextView tvSeatClass = ticketView.findViewById(R.id.tvSeatClass);
                    TextView tvPassengerName = ticketView.findViewById(R.id.tvPassengerName);
                    TextView tvPassengerID = ticketView.findViewById(R.id.tvPassengerID);
                    TextView tvPassengerPhone = ticketView.findViewById(R.id.tvPassengerPhone);
                    TextView tvPrice = ticketView.findViewById(R.id.tvPrice);
                    TextView tvBookingTime = ticketView.findViewById(R.id.tvBookingTime);

                    tvBookingId.setText("Mã đặt chỗ: " + map.get("bookingId"));
                    tvFlightCode.setText((String) map.get("flightCode"));
                    tvAirline.setText((String) map.get("airline"));
                    tvRoute.setText(map.get("departure") + " → " + map.get("destination"));
                    tvDepartureDate.setText((String) map.get("departureDate"));
                    tvDepartureTime.setText((String) map.get("departureTime"));
                    tvArrivalTime.setText((String) map.get("arrivalTime"));
                    tvDuration.setText((String) map.get("duration"));
                    tvSeatClass.setText((String) map.get("seatClass"));
                    tvPassengerName.setText((String) map.get("passengerName"));
                    tvPassengerID.setText((String) map.get("passengerID"));
                    tvPassengerPhone.setText((String) map.get("passengerPhone"));
                    
                    long price = map.get("price") instanceof Long ? 
                        (Long) map.get("price") : ((Number) map.get("price")).longValue();
                    tvPrice.setText(String.format(java.util.Locale.getDefault(), "%,d VNĐ", price));
                    tvBookingTime.setText("Đặt lúc: " + map.get("bookingTime"));

                    llTickets.addView(ticketView);
                } catch (Exception e) {
                    android.util.Log.e("MyTickets", "Error displaying ticket", e);
                }
            }
        });
    }

    private String formatMoney(long amount) {
        return NumberFormat.getNumberInstance(Locale.US).format(amount);
    }
}
