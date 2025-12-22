package com.example.vibebank;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.vibebank.utils.MovieTicketMockService;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MovieTicketActivity extends AppCompatActivity {

    private ImageView btnBack;
    private ImageButton btnMyMovieTickets;
    private Spinner spnMovie, spnCinema, spnShowtime;
    private EditText edtDate;
    private TextView tvCurrentBalance, tvSelectedSeats, tvTotalPrice, tvCinemaAddress;
    private GridLayout gridSeats;
    private LinearLayout llCustomerInfo, llShowtimeSelection, llSeatSelection;
    private EditText edtCustomerName, edtCustomerPhone, edtCustomerEmail;
    private MaterialButton btnSelectShowtime, btnContinue;

    private FirebaseFirestore db;
    private String currentUserId;
    private double currentBalance = 0;
    private Calendar selectedDate;
    private Set<String> selectedSeats = new HashSet<>();
    private Set<String> bookedSeats = new HashSet<>();
    private MovieTicketMockService.Showtime selectedShowtime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_ticket);

        // Initialize
        db = FirebaseFirestore.getInstance();
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            currentUserId = auth.getCurrentUser().getUid();
        }

        MovieTicketMockService.init(this);
        selectedDate = Calendar.getInstance();

        initViews();
        setupSpinners();
        setupListeners();
        loadAccountBalance();
        loadUserInfo();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnMyMovieTickets = findViewById(R.id.btnMyMovieTickets);
        spnMovie = findViewById(R.id.spnMovie);
        spnCinema = findViewById(R.id.spnCinema);
        spnShowtime = findViewById(R.id.spnShowtime);
        edtDate = findViewById(R.id.edtDate);
        tvCurrentBalance = findViewById(R.id.tvCurrentBalance);
        tvSelectedSeats = findViewById(R.id.tvSelectedSeats);
        tvTotalPrice = findViewById(R.id.tvTotalPrice);
        tvCinemaAddress = findViewById(R.id.tvCinemaAddress);
        gridSeats = findViewById(R.id.gridSeats);
        llCustomerInfo = findViewById(R.id.llCustomerInfo);
        llShowtimeSelection = findViewById(R.id.llShowtimeSelection);
        llSeatSelection = findViewById(R.id.llSeatSelection);
        edtCustomerName = findViewById(R.id.edtCustomerName);
        edtCustomerPhone = findViewById(R.id.edtCustomerPhone);
        edtCustomerEmail = findViewById(R.id.edtCustomerEmail);
        btnSelectShowtime = findViewById(R.id.btnSelectShowtime);
        btnContinue = findViewById(R.id.btnContinue);

        // Set default date
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        edtDate.setText(sdf.format(selectedDate.getTime()));
    }

    private void setupSpinners() {
        // Movies
        List<MovieTicketMockService.Movie> movies = MovieTicketMockService.getAvailableMovies();
        ArrayAdapter<MovieTicketMockService.Movie> movieAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, movies);
        movieAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnMovie.setAdapter(movieAdapter);

        // Cinemas
        List<MovieTicketMockService.Cinema> cinemas = MovieTicketMockService.getAvailableCinemas();
        ArrayAdapter<MovieTicketMockService.Cinema> cinemaAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, cinemas);
        cinemaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnCinema.setAdapter(cinemaAdapter);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnMyMovieTickets.setOnClickListener(v -> {
            Intent intent = new Intent(MovieTicketActivity.this, MyMovieTicketsActivity.class);
            startActivity(intent);
        });
        edtDate.setOnClickListener(v -> showDatePicker());
        btnSelectShowtime.setOnClickListener(v -> selectShowtime());
        btnContinue.setOnClickListener(v -> proceedToPayment());
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    selectedDate.set(year, month, dayOfMonth);
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    edtDate.setText(sdf.format(selectedDate.getTime()));
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private void selectShowtime() {
        MovieTicketMockService.Movie movie = (MovieTicketMockService.Movie) spnMovie.getSelectedItem();
        MovieTicketMockService.Cinema cinema = (MovieTicketMockService.Cinema) spnCinema.getSelectedItem();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String date = sdf.format(selectedDate.getTime());

        if (movie == null || cinema == null) {
            Toast.makeText(this, "Vui lòng chọn phim và rạp", Toast.LENGTH_SHORT).show();
            return;
        }

        // Load showtimes
        List<MovieTicketMockService.Showtime> showtimes = MovieTicketMockService.getShowtimes(
                movie.getId(), cinema.getId(), date);

        ArrayAdapter<MovieTicketMockService.Showtime> showtimeAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, showtimes);
        showtimeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnShowtime.setAdapter(showtimeAdapter);

        llShowtimeSelection.setVisibility(View.VISIBLE);
        
        // Set cinema address
        tvCinemaAddress.setText(cinema.getAddress());

        // Setup showtime selection listener
        spnShowtime.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                selectedShowtime = (MovieTicketMockService.Showtime) parent.getItemAtPosition(position);
                loadSeats();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    private void loadSeats() {
        if (selectedShowtime == null) return;

        llSeatSelection.setVisibility(View.VISIBLE);
        gridSeats.removeAllViews();
        selectedSeats.clear();
        updateSelectedSeatsDisplay();

        // Get booked seats
        bookedSeats = MovieTicketMockService.getBookedSeats(selectedShowtime.getShowtimeKey());

        // Create seat grid (8 rows x 12 columns)
        gridSeats.setColumnCount(12);
        gridSeats.setRowCount(8);

        String[] rows = {"A", "B", "C", "D", "E", "F", "G", "H"};
        
        for (int row = 0; row < 8; row++) {
            for (int col = 1; col <= 12; col++) {
                String seatId = rows[row] + col;
                CardView seatView = createSeatView(seatId);
                gridSeats.addView(seatView);
            }
        }
    }

    private CardView createSeatView(String seatId) {
        CardView cardView = new CardView(this);
        
        // Calculate seat size to fit 12 columns on screen
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int totalMargin = (int) (16 * getResources().getDisplayMetrics().density); // margins for grid container
        int availableWidth = screenWidth - totalMargin;
        int margin = (int) (2 * getResources().getDisplayMetrics().density);
        int seatSize = (availableWidth / 12) - (margin * 2);
        
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = seatSize;
        params.height = seatSize;
        params.setMargins(margin, margin, margin, margin);
        cardView.setLayoutParams(params);
        cardView.setCardElevation(4f);
        cardView.setRadius(8f);
        cardView.setClickable(true);
        cardView.setFocusable(true);
        cardView.setForeground(getDrawable(android.R.drawable.list_selector_background));

        TextView textView = new TextView(this);
        textView.setText(seatId);
        textView.setTextSize(10);
        textView.setGravity(Gravity.CENTER);
        textView.setPadding(8, 16, 8, 16);
        textView.setTextColor(Color.WHITE);

        // Set seat status
        boolean isBooked = bookedSeats.contains(seatId);
        if (isBooked) {
            cardView.setCardBackgroundColor(Color.parseColor("#9E9E9E")); // Gray - booked
            cardView.setClickable(false);
        } else {
            cardView.setCardBackgroundColor(Color.parseColor("#4CAF50")); // Green - available
            cardView.setOnClickListener(v -> toggleSeat(seatId, cardView));
        }

        cardView.addView(textView);
        return cardView;
    }

    private void toggleSeat(String seatId, CardView cardView) {
        if (selectedSeats.contains(seatId)) {
            // Deselect
            selectedSeats.remove(seatId);
            cardView.setCardBackgroundColor(Color.parseColor("#4CAF50")); // Green - available
        } else {
            // Select
            selectedSeats.add(seatId);
            cardView.setCardBackgroundColor(Color.parseColor("#FF9800")); // Orange - selected
        }
        updateSelectedSeatsDisplay();
        
        // Show customer info if seats selected
        if (!selectedSeats.isEmpty()) {
            llCustomerInfo.setVisibility(View.VISIBLE);
        } else {
            llCustomerInfo.setVisibility(View.GONE);
        }
    }

    private void updateSelectedSeatsDisplay() {
        if (selectedSeats.isEmpty()) {
            tvSelectedSeats.setText("Chưa chọn ghế");
            tvTotalPrice.setText("0 VND");
        } else {
            List<String> sortedSeats = new ArrayList<>(selectedSeats);
            sortedSeats.sort(String::compareTo);
            tvSelectedSeats.setText(String.join(", ", sortedSeats));
            
            long totalPrice = selectedSeats.size() * selectedShowtime.getPricePerSeat();
            tvTotalPrice.setText(formatMoney(totalPrice) + " VND");
        }
    }

    private void proceedToPayment() {
        if (selectedSeats.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn ghế", Toast.LENGTH_SHORT).show();
            return;
        }

        String customerName = edtCustomerName.getText().toString().trim();
        String customerPhone = edtCustomerPhone.getText().toString().trim();
        String customerEmail = edtCustomerEmail.getText().toString().trim();

        if (customerName.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập họ tên", Toast.LENGTH_SHORT).show();
            return;
        }

        if (customerPhone.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập số điện thoại", Toast.LENGTH_SHORT).show();
            return;
        }

        if (customerEmail.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập email", Toast.LENGTH_SHORT).show();
            return;
        }

        MovieTicketMockService.Movie movie = (MovieTicketMockService.Movie) spnMovie.getSelectedItem();
        MovieTicketMockService.Cinema cinema = (MovieTicketMockService.Cinema) spnCinema.getSelectedItem();
        
        long totalPrice = selectedSeats.size() * selectedShowtime.getPricePerSeat();

        Intent intent = new Intent(this, TransferDetailsActivity.class);
        intent.putExtra("amount", String.valueOf(totalPrice));
        intent.putExtra("receiverName", cinema.getName());
        intent.putExtra("receiverUserId", "EXTERNAL_BANK");
        intent.putExtra("receiverAccountNumber", "MOVIE_TICKET");
        intent.putExtra("receiverBank", "Cinema");
        intent.putExtra("isMovieTicketPayment", true);
        
        // Movie ticket details
        intent.putExtra("movieTitle", movie.getTitle());
        intent.putExtra("cinemaName", cinema.getName());
        intent.putExtra("date", selectedShowtime.getDate());
        intent.putExtra("time", selectedShowtime.getTime());
        intent.putExtra("showtimeKey", selectedShowtime.getShowtimeKey());
        intent.putExtra("seats", new ArrayList<>(selectedSeats));
        intent.putExtra("customerName", customerName);
        intent.putExtra("customerPhone", customerPhone);
        intent.putExtra("customerEmail", customerEmail);

        startActivityForResult(intent, 1005);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1005 && resultCode == RESULT_OK) {
            // Payment successful
            Intent intent = new Intent(this, MovieTicketResultActivity.class);
            intent.putExtra("movieTitle", ((MovieTicketMockService.Movie) spnMovie.getSelectedItem()).getTitle());
            intent.putExtra("cinemaName", ((MovieTicketMockService.Cinema) spnCinema.getSelectedItem()).getName());
            intent.putExtra("date", selectedShowtime.getDate());
            intent.putExtra("time", selectedShowtime.getTime());
            intent.putExtra("seats", new ArrayList<>(selectedSeats));
            intent.putExtra("totalPrice", selectedSeats.size() * selectedShowtime.getPricePerSeat());
            intent.putExtra("customerName", edtCustomerName.getText().toString());
            startActivity(intent);
            finish();
        }
    }

    private void loadAccountBalance() {
        if (currentUserId == null) return;

        db.collection("accounts").document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentBalance = documentSnapshot.getDouble("balance");
                        tvCurrentBalance.setText(formatMoney((long) currentBalance) + " VND");
                    }
                });
    }

    private void loadUserInfo() {
        if (currentUserId == null) return;

        db.collection("accounts").document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String fullName = documentSnapshot.getString("fullName");
                        String phone = documentSnapshot.getString("phone");
                        
                        if (fullName != null && !fullName.isEmpty()) {
                            edtCustomerName.setText(fullName);
                        }
                        if (phone != null && !phone.isEmpty()) {
                            edtCustomerPhone.setText(phone);
                        }
                    }
                });
    }

    private String formatMoney(long amount) {
        return NumberFormat.getNumberInstance(Locale.US).format(amount);
    }
}
