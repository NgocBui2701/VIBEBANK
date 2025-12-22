package com.example.vibebank;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

public class MovieTicketResultActivity extends AppCompatActivity {

    private TextView tvMovieTitle, tvCinemaName, tvDate, tvTime, tvSeats, tvTotalPrice, tvCustomerName;
    private MaterialButton btnBackToHome, btnViewMyTickets;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_ticket_result);

        initViews();
        displayResult();
        setupListeners();
    }

    private void initViews() {
        tvMovieTitle = findViewById(R.id.tvMovieTitle);
        tvCinemaName = findViewById(R.id.tvCinemaName);
        tvDate = findViewById(R.id.tvDate);
        tvTime = findViewById(R.id.tvTime);
        tvSeats = findViewById(R.id.tvSeats);
        tvTotalPrice = findViewById(R.id.tvTotalPrice);
        tvCustomerName = findViewById(R.id.tvCustomerName);
        btnBackToHome = findViewById(R.id.btnBackToHome);
        btnViewMyTickets = findViewById(R.id.btnViewMyTickets);
    }

    private void displayResult() {
        String movieTitle = getIntent().getStringExtra("movieTitle");
        String cinemaName = getIntent().getStringExtra("cinemaName");
        String date = getIntent().getStringExtra("date");
        String time = getIntent().getStringExtra("time");
        ArrayList<String> seats = getIntent().getStringArrayListExtra("seats");
        long totalPrice = getIntent().getLongExtra("totalPrice", 0);
        String customerName = getIntent().getStringExtra("customerName");

        tvMovieTitle.setText(movieTitle);
        tvCinemaName.setText(cinemaName);
        tvDate.setText(date);
        tvTime.setText(time);
        
        if (seats != null && !seats.isEmpty()) {
            seats.sort(String::compareTo);
            tvSeats.setText(String.join(", ", seats));
        }
        
        tvTotalPrice.setText(formatMoney(totalPrice) + " VND");
        tvCustomerName.setText(customerName);
    }

    private void setupListeners() {
        btnBackToHome.setOnClickListener(v -> {
            Intent intent = new Intent(this, com.example.vibebank.ui.home.HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        btnViewMyTickets.setOnClickListener(v -> {
            Intent intent = new Intent(this, MyMovieTicketsActivity.class);
            startActivity(intent);
        });
    }

    private String formatMoney(long amount) {
        return NumberFormat.getNumberInstance(Locale.US).format(amount);
    }
}
