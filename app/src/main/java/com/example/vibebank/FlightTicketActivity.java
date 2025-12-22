package com.example.vibebank;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.vibebank.utils.FlightTicketMockService;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class FlightTicketActivity extends AppCompatActivity {

    private ImageView btnBack;
    private ImageButton btnMyTickets;
    private Spinner spnDeparture, spnDestination;
    private EditText edtDepartureDate;
    private MaterialButton btnSearchFlights, btnContinue;
    private TextView tvCurrentBalance;
    private LinearLayout llFlights, llPassengerInfo;
    private EditText edtPassengerName, edtPassengerID, edtPassengerPhone, edtPassengerEmail;

    private FirebaseFirestore db;
    private String currentUserId;
    private double currentBalance = 0;
    private FlightTicketMockService.FlightTicket selectedFlight;
    private Calendar selectedDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flight_ticket);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            currentUserId = auth.getCurrentUser().getUid();
        }

        // Initialize FlightTicketMockService
        FlightTicketMockService.init(this);

        selectedDate = Calendar.getInstance();

        initViews();
        setupSpinners();
        setupListeners();
        loadAccountBalance();
        loadUserInfo();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnMyTickets = findViewById(R.id.btnMyTickets);
        spnDeparture = findViewById(R.id.spnDeparture);
        spnDestination = findViewById(R.id.spnDestination);
        edtDepartureDate = findViewById(R.id.edtDepartureDate);
        btnSearchFlights = findViewById(R.id.btnSearchFlights);
        tvCurrentBalance = findViewById(R.id.tvCurrentBalance);
        llFlights = findViewById(R.id.llFlights);
        llPassengerInfo = findViewById(R.id.llPassengerInfo);
        btnContinue = findViewById(R.id.btnContinue);
        edtPassengerName = findViewById(R.id.edtPassengerName);
        edtPassengerID = findViewById(R.id.edtPassengerID);
        edtPassengerPhone = findViewById(R.id.edtPassengerPhone);
        edtPassengerEmail = findViewById(R.id.edtPassengerEmail);

        // Set default date to today
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        edtDepartureDate.setText(sdf.format(selectedDate.getTime()));
    }

    private void setupSpinners() {
        List<FlightTicketMockService.Route> routes = FlightTicketMockService.getAvailableRoutes();
        
        ArrayAdapter<FlightTicketMockService.Route> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, routes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        
        spnDeparture.setAdapter(adapter);
        spnDestination.setAdapter(adapter);
        
        // Set default: SGN to HAN
        spnDeparture.setSelection(0); // SGN
        spnDestination.setSelection(1); // HAN
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnMyTickets.setOnClickListener(v -> {
            Intent intent = new Intent(FlightTicketActivity.this, MyTicketsActivity.class);
            startActivity(intent);
        });
        btnSearchFlights.setOnClickListener(v -> searchFlights());
        btnContinue.setOnClickListener(v -> proceedToTransfer());
        
        edtDepartureDate.setOnClickListener(v -> showDatePicker());
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    selectedDate.set(year, month, dayOfMonth);
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    edtDepartureDate.setText(sdf.format(selectedDate.getTime()));
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)
        );
        
        // Set minimum date to today
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private void loadAccountBalance() {
        if (currentUserId == null) {
            return;
        }

        db.collection("accounts").document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Double balance = documentSnapshot.getDouble("balance");
                        if (balance != null) {
                            currentBalance = balance;
                            tvCurrentBalance.setText("Số dư hiện tại: " + formatMoney((long)currentBalance) + " VND");
                        }
                    }
                });
    }

    private void loadUserInfo() {
        if (currentUserId == null) {
            return;
        }

        db.collection("accounts").document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String fullName = documentSnapshot.getString("fullName");
                        String phone = documentSnapshot.getString("phone");
                        
                        if (fullName != null) {
                            edtPassengerName.setText(fullName);
                        }
                        if (phone != null) {
                            edtPassengerPhone.setText(phone);
                        }
                    }
                });
    }

    private void searchFlights() {
        FlightTicketMockService.Route departure = (FlightTicketMockService.Route) spnDeparture.getSelectedItem();
        FlightTicketMockService.Route destination = (FlightTicketMockService.Route) spnDestination.getSelectedItem();

        if (departure.getCode().equals(destination.getCode())) {
            Toast.makeText(this, "Điểm đi và điểm đến không được trùng nhau", Toast.LENGTH_SHORT).show();
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String dateStr = sdf.format(selectedDate.getTime());

        List<FlightTicketMockService.FlightTicket> flights = FlightTicketMockService.searchFlights(
                departure.getCode(), destination.getCode(), dateStr);

        displayFlights(flights);
    }

    private void displayFlights(List<FlightTicketMockService.FlightTicket> flights) {
        llFlights.removeAllViews();
        llPassengerInfo.setVisibility(View.GONE);
        selectedFlight = null;

        if (flights.isEmpty()) {
            TextView tvNoFlights = new TextView(this);
            tvNoFlights.setText("Không tìm thấy chuyến bay phù hợp");
            tvNoFlights.setTextColor(getResources().getColor(R.color.text_secondary));
            tvNoFlights.setPadding(16, 32, 16, 16);
            llFlights.addView(tvNoFlights);
            return;
        }

        for (FlightTicketMockService.FlightTicket flight : flights) {
            View flightView = getLayoutInflater().inflate(R.layout.item_flight_ticket, llFlights, false);

            TextView tvFlightCode = flightView.findViewById(R.id.tvFlightCode);
            TextView tvAirline = flightView.findViewById(R.id.tvAirline);
            TextView tvDepartureTime = flightView.findViewById(R.id.tvDepartureTime);
            TextView tvArrivalTime = flightView.findViewById(R.id.tvArrivalTime);
            TextView tvDuration = flightView.findViewById(R.id.tvDuration);
            TextView tvSeatClass = flightView.findViewById(R.id.tvSeatClass);
            TextView tvPrice = flightView.findViewById(R.id.tvPrice);
            TextView tvBookedBadge = flightView.findViewById(R.id.tvBookedBadge);
            CardView cardFlight = flightView.findViewById(R.id.cardFlight);

            tvFlightCode.setText(flight.getFlightCode());
            tvAirline.setText(flight.getAirline());
            tvDepartureTime.setText(flight.getDepartureTime());
            tvArrivalTime.setText(flight.getArrivalTime());
            tvDuration.setText(flight.getDuration());
            tvSeatClass.setText(flight.getSeatClass());
            tvPrice.setText(formatMoney(flight.getPrice()) + " VND");

            // Show "Đã đặt" badge if flight is already booked
            if (flight.isBooked()) {
                tvBookedBadge.setVisibility(View.VISIBLE);
                cardFlight.setAlpha(0.6f);
                cardFlight.setEnabled(false);
            } else {
                tvBookedBadge.setVisibility(View.GONE);
                cardFlight.setAlpha(1.0f);
                cardFlight.setEnabled(true);
                cardFlight.setOnClickListener(v -> selectFlight(flight, cardFlight));
            }

            llFlights.addView(flightView);
        }
    }

    private void selectFlight(FlightTicketMockService.FlightTicket flight, CardView selectedCard) {
        // Don't allow selecting already booked flight
        if (flight.isBooked()) {
            Toast.makeText(this, "Vé này đã được đặt", Toast.LENGTH_SHORT).show();
            return;
        }

        // Reset all flight cards
        for (int i = 0; i < llFlights.getChildCount(); i++) {
            View child = llFlights.getChildAt(i);
            CardView card = child.findViewById(R.id.cardFlight);
            if (card != null) {
                card.setCardBackgroundColor(getResources().getColor(android.R.color.white));
                card.setCardElevation(4.0f);
            }
        }
        
        // Highlight selected flight
        selectedCard.setCardBackgroundColor(getResources().getColor(R.color.background));
        selectedCard.setCardElevation(8.0f);
        
        selectedFlight = flight;
        llPassengerInfo.setVisibility(View.VISIBLE);
        
        // Scroll to passenger info
        llPassengerInfo.post(() -> {
            llPassengerInfo.requestFocus();
        });
    }

    private void proceedToTransfer() {
        if (selectedFlight == null) {
            Toast.makeText(this, "Vui lòng chọn chuyến bay", Toast.LENGTH_SHORT).show();
            return;
        }

        String passengerName = edtPassengerName.getText().toString().trim();
        String passengerID = edtPassengerID.getText().toString().trim();
        String passengerPhone = edtPassengerPhone.getText().toString().trim();
        String passengerEmail = edtPassengerEmail.getText().toString().trim();

        if (passengerName.isEmpty()) {
            edtPassengerName.setError("Vui lòng nhập họ tên");
            edtPassengerName.requestFocus();
            return;
        }

        if (passengerID.isEmpty()) {
            edtPassengerID.setError("Vui lòng nhập CMND/CCCD");
            edtPassengerID.requestFocus();
            return;
        }

        if (passengerPhone.isEmpty()) {
            edtPassengerPhone.setError("Vui lòng nhập số điện thoại");
            edtPassengerPhone.requestFocus();
            return;
        }

        // Navigate to TransferDetailsActivity
        Intent intent = new Intent(this, TransferDetailsActivity.class);
        intent.putExtra("receiverAccountNumber", selectedFlight.getFlightCode());
        intent.putExtra("receiverName", selectedFlight.getAirline());
        intent.putExtra("receiverUserId", "EXTERNAL_BANK");
        intent.putExtra("bankName", "Vibebank");
        intent.putExtra("amount", String.valueOf(selectedFlight.getPrice()));
        intent.putExtra("isFlightTicketPayment", true);
        intent.putExtra("flightCode", selectedFlight.getFlightCode());
        intent.putExtra("airline", selectedFlight.getAirline());
        intent.putExtra("departure", selectedFlight.getDeparture());
        intent.putExtra("destination", selectedFlight.getDestination());
        intent.putExtra("departureDate", selectedFlight.getDepartureDate());
        intent.putExtra("departureTime", selectedFlight.getDepartureTime());
        intent.putExtra("arrivalTime", selectedFlight.getArrivalTime());
        intent.putExtra("seatClass", selectedFlight.getSeatClass());
        intent.putExtra("duration", selectedFlight.getDuration());
        intent.putExtra("passengerName", passengerName);
        intent.putExtra("passengerID", passengerID);
        intent.putExtra("passengerPhone", passengerPhone);
        intent.putExtra("passengerEmail", passengerEmail);

        startActivityForResult(intent, 1004);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1004) {
            if (resultCode == RESULT_OK) {
                // Transfer successful - show success screen
                Intent resultIntent = new Intent(this, FlightTicketResultActivity.class);
                resultIntent.putExtra("flightCode", selectedFlight.getFlightCode());
                resultIntent.putExtra("airline", selectedFlight.getAirline());
                resultIntent.putExtra("departure", selectedFlight.getDeparture());
                resultIntent.putExtra("destination", selectedFlight.getDestination());
                resultIntent.putExtra("departureDate", selectedFlight.getDepartureDate());
                resultIntent.putExtra("departureTime", selectedFlight.getDepartureTime());
                resultIntent.putExtra("arrivalTime", selectedFlight.getArrivalTime());
                resultIntent.putExtra("seatClass", selectedFlight.getSeatClass());
                resultIntent.putExtra("duration", selectedFlight.getDuration());
                resultIntent.putExtra("price", selectedFlight.getPrice());
                resultIntent.putExtra("passengerName", edtPassengerName.getText().toString().trim());
                resultIntent.putExtra("passengerID", edtPassengerID.getText().toString().trim());
                resultIntent.putExtra("passengerPhone", edtPassengerPhone.getText().toString().trim());
                resultIntent.putExtra("passengerEmail", edtPassengerEmail.getText().toString().trim());
                startActivity(resultIntent);
                finish();
            } else {
                // Transfer failed or cancelled
                Toast.makeText(this, "Đặt vé thất bại hoặc đã hủy", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String formatMoney(long amount) {
        return NumberFormat.getInstance(new Locale("vi", "VN")).format(amount);
    }
}
