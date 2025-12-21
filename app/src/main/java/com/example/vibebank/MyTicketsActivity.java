package com.example.vibebank;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.vibebank.utils.FlightTicketMockService;

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

        // Initialize FlightTicketMockService
        FlightTicketMockService.init(this);

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
        List<FlightTicketMockService.BookedTicket> tickets = FlightTicketMockService.getMyBookedTickets();
        
        if (tickets.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            llTickets.setVisibility(View.GONE);
            return;
        }

        tvEmptyState.setVisibility(View.GONE);
        llTickets.setVisibility(View.VISIBLE);
        llTickets.removeAllViews();

        for (FlightTicketMockService.BookedTicket ticket : tickets) {
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

            tvBookingId.setText("Mã đặt chỗ: " + ticket.getBookingId());
            tvFlightCode.setText(ticket.getFlightCode());
            tvAirline.setText(ticket.getAirline());
            tvRoute.setText(ticket.getDeparture() + " → " + ticket.getDestination());
            tvDepartureDate.setText(ticket.getDepartureDate());
            tvDepartureTime.setText(ticket.getDepartureTime());
            tvArrivalTime.setText(ticket.getArrivalTime());
            tvDuration.setText(ticket.getDuration());
            tvSeatClass.setText(ticket.getSeatClass());
            tvPassengerName.setText(ticket.getPassengerName());
            tvPassengerID.setText(ticket.getPassengerID());
            tvPassengerPhone.setText(ticket.getPassengerPhone());
            tvPrice.setText(formatMoney(ticket.getPrice()) + " VND");
            tvBookingTime.setText("Đặt lúc: " + ticket.getBookingTime());

            llTickets.addView(ticketView);
        }
    }

    private String formatMoney(long amount) {
        return NumberFormat.getNumberInstance(Locale.US).format(amount);
    }
}
