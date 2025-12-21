package com.example.vibebank;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.vibebank.utils.MovieTicketMockService;

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

        // Initialize MovieTicketMockService
        MovieTicketMockService.init(this);

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
        List<MovieTicketMockService.BookedTicket> tickets = MovieTicketMockService.getMyBookedTickets();
        
        if (tickets.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            llTickets.setVisibility(View.GONE);
            return;
        }

        tvEmptyState.setVisibility(View.GONE);
        llTickets.setVisibility(View.VISIBLE);
        llTickets.removeAllViews();

        for (MovieTicketMockService.BookedTicket ticket : tickets) {
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

            tvBookingId.setText("Mã đặt vé: " + ticket.getBookingId());
            tvMovieTitle.setText(ticket.getMovieTitle());
            tvCinemaName.setText(ticket.getCinemaName());
            tvDate.setText(ticket.getDate());
            tvTime.setText(ticket.getTime());
            
            // Sort and display seats
            List<String> seats = ticket.getSeats();
            seats.sort(String::compareTo);
            tvSeats.setText(String.join(", ", seats));
            
            tvCustomerName.setText(ticket.getCustomerName());
            tvCustomerPhone.setText(ticket.getCustomerPhone());
            tvCustomerEmail.setText(ticket.getCustomerEmail());
            tvTotalPrice.setText(formatMoney(ticket.getTotalPrice()) + " VND");
            tvBookingTime.setText("Đặt lúc: " + ticket.getBookingTime());

            llTickets.addView(ticketView);
        }
    }

    private String formatMoney(long amount) {
        return NumberFormat.getNumberInstance(Locale.US).format(amount);
    }
}
