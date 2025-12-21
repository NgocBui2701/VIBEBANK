package com.example.vibebank;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import java.text.NumberFormat;
import java.util.Locale;

public class FlightTicketResultActivity extends AppCompatActivity {

    private TextView tvFlightCode, tvAirline, tvRoute, tvDepartureDate, tvDepartureTime, 
                     tvArrivalTime, tvDuration, tvSeatClass, tvPrice,
                     tvPassengerName, tvPassengerID, tvPassengerPhone, tvPassengerEmail;
    private MaterialButton btnBackToHome;
    private ImageView btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flight_ticket_result);

        initViews();
        displayResult();
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tvFlightCode = findViewById(R.id.tvFlightCode);
        tvAirline = findViewById(R.id.tvAirline);
        tvRoute = findViewById(R.id.tvRoute);
        tvDepartureDate = findViewById(R.id.tvDepartureDate);
        tvDepartureTime = findViewById(R.id.tvDepartureTime);
        tvArrivalTime = findViewById(R.id.tvArrivalTime);
        tvDuration = findViewById(R.id.tvDuration);
        tvSeatClass = findViewById(R.id.tvSeatClass);
        tvPrice = findViewById(R.id.tvPrice);
        tvPassengerName = findViewById(R.id.tvPassengerName);
        tvPassengerID = findViewById(R.id.tvPassengerID);
        tvPassengerPhone = findViewById(R.id.tvPassengerPhone);
        tvPassengerEmail = findViewById(R.id.tvPassengerEmail);
        btnBackToHome = findViewById(R.id.btnBackToHome);
    }

    private void displayResult() {
        String flightCode = getIntent().getStringExtra("flightCode");
        String airline = getIntent().getStringExtra("airline");
        String departure = getIntent().getStringExtra("departure");
        String destination = getIntent().getStringExtra("destination");
        String departureDate = getIntent().getStringExtra("departureDate");
        String departureTime = getIntent().getStringExtra("departureTime");
        String arrivalTime = getIntent().getStringExtra("arrivalTime");
        String duration = getIntent().getStringExtra("duration");
        String seatClass = getIntent().getStringExtra("seatClass");
        long price = getIntent().getLongExtra("price", 0);
        String passengerName = getIntent().getStringExtra("passengerName");
        String passengerID = getIntent().getStringExtra("passengerID");
        String passengerPhone = getIntent().getStringExtra("passengerPhone");
        String passengerEmail = getIntent().getStringExtra("passengerEmail");

        tvFlightCode.setText(flightCode);
        tvAirline.setText(airline);
        tvRoute.setText(departure + " → " + destination);
        tvDepartureDate.setText(departureDate);
        tvDepartureTime.setText(departureTime);
        tvArrivalTime.setText(arrivalTime);
        tvDuration.setText(duration);
        tvSeatClass.setText(seatClass);
        tvPrice.setText(formatMoney(price) + " VND");
        tvPassengerName.setText(passengerName);
        tvPassengerID.setText(passengerID);
        tvPassengerPhone.setText(passengerPhone);
        
        if (passengerEmail != null && !passengerEmail.isEmpty()) {
            tvPassengerEmail.setText(passengerEmail);
        } else {
            tvPassengerEmail.setText("Không có");
        }
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> backToHome());
        btnBackToHome.setOnClickListener(v -> backToHome());
    }

    private void backToHome() {
        Intent intent = new Intent(this, com.example.vibebank.ui.home.HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        backToHome();
    }

    private String formatMoney(long amount) {
        return NumberFormat.getInstance(new Locale("vi", "VN")).format(amount);
    }
}
