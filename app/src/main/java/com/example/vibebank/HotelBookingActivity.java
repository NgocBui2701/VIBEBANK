package com.example.vibebank;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.vibebank.utils.HotelBookingMockService;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HotelBookingActivity extends AppCompatActivity {

    private static final int REQUEST_HOTEL_PAYMENT = 1006;

    private TextView tvBalance;
    private Spinner spinnerLocation, spinnerHotel, spinnerRoomType, spinnerNumberOfRooms, spinnerAdults, spinnerChildren;
    private LinearLayout layoutCheckInDate, layoutCheckOutDate;
    private TextView tvCheckInDate, tvCheckOutDate;
    private Button btnCalculatePrice, btnProceedToPayment;
    private ImageButton btnMyHotelBookings;
    private CardView cardBookingDetails, cardCustomerInfo;
    private TextView tvNumberOfNights, tvPricePerNight, tvTotalPrice;
    private TextInputEditText etCustomerName, etCustomerPhone, etCustomerEmail;
    private ImageButton btnBack;

    private List<HotelBookingMockService.Hotel> hotels;
    private List<HotelBookingMockService.RoomType> roomTypes;
    private HotelBookingMockService.Hotel selectedHotel;
    private HotelBookingMockService.RoomType selectedRoomType;
    private String checkInDate = "";
    private String checkOutDate = "";
    private int numberOfNights = 0;
    private int totalPrice = 0;

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hotel_booking);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        initViews();
        loadBalance();
        setupSpinners();
        setupDatePickers();
        setupListeners();
    }

    private void initViews() {
        tvBalance = findViewById(R.id.tvBalance);
        spinnerLocation = findViewById(R.id.spinnerLocation);
        spinnerHotel = findViewById(R.id.spinnerHotel);
        spinnerRoomType = findViewById(R.id.spinnerRoomType);
        spinnerNumberOfRooms = findViewById(R.id.spinnerNumberOfRooms);
        spinnerAdults = findViewById(R.id.spinnerAdults);
        spinnerChildren = findViewById(R.id.spinnerChildren);
        layoutCheckInDate = findViewById(R.id.layoutCheckInDate);
        layoutCheckOutDate = findViewById(R.id.layoutCheckOutDate);
        tvCheckInDate = findViewById(R.id.tvCheckInDate);
        tvCheckOutDate = findViewById(R.id.tvCheckOutDate);
        btnCalculatePrice = findViewById(R.id.btnCalculatePrice);
        btnProceedToPayment = findViewById(R.id.btnProceedToPayment);
        btnMyHotelBookings = findViewById(R.id.btnMyHotelBookings);
        cardBookingDetails = findViewById(R.id.cardBookingDetails);
        cardCustomerInfo = findViewById(R.id.cardCustomerInfo);
        tvNumberOfNights = findViewById(R.id.tvNumberOfNights);
        tvPricePerNight = findViewById(R.id.tvPricePerNight);
        tvTotalPrice = findViewById(R.id.tvTotalPrice);
        etCustomerName = findViewById(R.id.etCustomerName);
        etCustomerPhone = findViewById(R.id.etCustomerPhone);
        etCustomerEmail = findViewById(R.id.etCustomerEmail);
        btnBack = findViewById(R.id.btnBack);
    }

    private void loadBalance() {
        // Get current user ID from multiple sources
        String userId = null;
        
        // Try Firebase Auth first
        if (auth != null && auth.getCurrentUser() != null) {
            userId = auth.getCurrentUser().getUid();
        }
        
        // Try SessionManager
        if (userId == null) {
            com.example.vibebank.utils.SessionManager sessionManager = 
                new com.example.vibebank.utils.SessionManager(this);
            userId = sessionManager.getCurrentUserId();
        }
        
        // Try SharedPreferences
        if (userId == null) {
            android.content.SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            userId = prefs.getString("current_user_id", null);
        }
        
        android.util.Log.d("HotelBooking", "Loading balance for userId: " + userId);
        
        if (userId != null) {
            final String finalUserId = userId;
            firestore.collection("accounts").document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Long balance = documentSnapshot.getLong("balance");
                            android.util.Log.d("HotelBooking", "Balance loaded: " + balance);
                            if (balance != null) {
                                tvBalance.setText(String.format(Locale.getDefault(), "%,d VNĐ", balance));
                            } else {
                                tvBalance.setText("0 VNĐ");
                            }
                        } else {
                            android.util.Log.d("HotelBooking", "Document does not exist for userId: " + finalUserId);
                            tvBalance.setText("0 VNĐ");
                        }
                    })
                    .addOnFailureListener(e -> {
                        android.util.Log.e("HotelBooking", "Error loading balance", e);
                        tvBalance.setText("0 VNĐ");
                    });
        } else {
            android.util.Log.e("HotelBooking", "No userId found");
            tvBalance.setText("0 VNĐ");
        }
    }

    private void setupSpinners() {
        // Setup location spinner
        List<String> locations = HotelBookingMockService.getLocations();
        ArrayAdapter<String> locationAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, locations);
        locationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLocation.setAdapter(locationAdapter);

        // Setup hotel spinner
        hotels = HotelBookingMockService.getHotels();
        ArrayAdapter<HotelBookingMockService.Hotel> hotelAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, hotels);
        hotelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerHotel.setAdapter(hotelAdapter);

        // Filter hotels when location changes
        spinnerLocation.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                String selectedLocation = (String) parent.getItemAtPosition(position);
                hotels = HotelBookingMockService.getHotelsByLocation(selectedLocation);
                ArrayAdapter<HotelBookingMockService.Hotel> newHotelAdapter = new ArrayAdapter<>(
                        HotelBookingActivity.this, android.R.layout.simple_spinner_item, hotels);
                newHotelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerHotel.setAdapter(newHotelAdapter);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });

        roomTypes = HotelBookingMockService.getRoomTypes();
        ArrayAdapter<HotelBookingMockService.RoomType> roomTypeAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, roomTypes);
        roomTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRoomType.setAdapter(roomTypeAdapter);

        List<String> roomNumbers = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            roomNumbers.add(String.valueOf(i));
        }
        ArrayAdapter<String> roomNumberAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, roomNumbers);
        roomNumberAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerNumberOfRooms.setAdapter(roomNumberAdapter);

        List<String> adults = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            adults.add(String.valueOf(i));
        }
        ArrayAdapter<String> adultsAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, adults);
        adultsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAdults.setAdapter(adultsAdapter);

        List<String> children = new ArrayList<>();
        for (int i = 0; i <= 10; i++) {
            children.add(String.valueOf(i));
        }
        ArrayAdapter<String> childrenAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, children);
        childrenAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerChildren.setAdapter(childrenAdapter);
    }

    private void setupDatePickers() {
        Calendar calendar = Calendar.getInstance();

        layoutCheckInDate.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    this,
                    (view, year, month, dayOfMonth) -> {
                        checkInDate = String.format(Locale.getDefault(), "%02d/%02d/%d", dayOfMonth, month + 1, year);
                        tvCheckInDate.setText(checkInDate);
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.getDatePicker().setMinDate(calendar.getTimeInMillis());
            datePickerDialog.show();
        });

        layoutCheckOutDate.setOnClickListener(v -> {
            if (checkInDate.isEmpty()) {
                Toast.makeText(this, "Vui lòng chọn ngày nhận phòng trước", Toast.LENGTH_SHORT).show();
                return;
            }

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    this,
                    (view, year, month, dayOfMonth) -> {
                        checkOutDate = String.format(Locale.getDefault(), "%02d/%02d/%d", dayOfMonth, month + 1, year);
                        tvCheckOutDate.setText(checkOutDate);
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );

            try {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                Date checkIn = sdf.parse(checkInDate);
                if (checkIn != null) {
                    datePickerDialog.getDatePicker().setMinDate(checkIn.getTime() + (24 * 60 * 60 * 1000));
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }

            datePickerDialog.show();
        });
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnMyHotelBookings.setOnClickListener(v -> {
            Intent intent = new Intent(HotelBookingActivity.this, MyHotelBookingsActivity.class);
            startActivity(intent);
        });

        btnCalculatePrice.setOnClickListener(v -> calculatePrice());

        btnProceedToPayment.setOnClickListener(v -> proceedToPayment());
    }

    private void calculatePrice() {
        if (checkInDate.isEmpty() || checkOutDate.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn ngày nhận và trả phòng", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date checkIn = sdf.parse(checkInDate);
            Date checkOut = sdf.parse(checkOutDate);

            if (checkIn != null && checkOut != null) {
                long diffInMillis = checkOut.getTime() - checkIn.getTime();
                numberOfNights = (int) (diffInMillis / (24 * 60 * 60 * 1000));

                if (numberOfNights <= 0) {
                    Toast.makeText(this, "Ngày trả phòng phải sau ngày nhận phòng", Toast.LENGTH_SHORT).show();
                    return;
                }

                selectedHotel = (HotelBookingMockService.Hotel) spinnerHotel.getSelectedItem();
                selectedRoomType = (HotelBookingMockService.RoomType) spinnerRoomType.getSelectedItem();
                int numberOfRooms = Integer.parseInt((String) spinnerNumberOfRooms.getSelectedItem());

                int pricePerNight = selectedRoomType.pricePerNight;
                totalPrice = pricePerNight * numberOfNights * numberOfRooms;

                tvNumberOfNights.setText(String.valueOf(numberOfNights));
                tvPricePerNight.setText(String.format(Locale.getDefault(), "%,d VNĐ", pricePerNight));
                tvTotalPrice.setText(String.format(Locale.getDefault(), "%,d VNĐ", totalPrice));

                cardBookingDetails.setVisibility(View.VISIBLE);
                cardCustomerInfo.setVisibility(View.VISIBLE);
            }
        } catch (ParseException e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi khi tính toán giá", Toast.LENGTH_SHORT).show();
        }
    }

    private void proceedToPayment() {
        String customerName = etCustomerName.getText().toString().trim();
        String customerPhone = etCustomerPhone.getText().toString().trim();
        String customerEmail = etCustomerEmail.getText().toString().trim();

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

        Intent intent = new Intent(this, TransferDetailsActivity.class);
        intent.putExtra("isHotelBookingPayment", true);
        intent.putExtra("amount", String.valueOf(totalPrice));
        intent.putExtra("hotelId", selectedHotel.id);
        intent.putExtra("hotelName", selectedHotel.name);
        intent.putExtra("location", selectedHotel.location);
        intent.putExtra("stars", selectedHotel.stars);
        intent.putExtra("roomTypeId", selectedRoomType.id);
        intent.putExtra("roomTypeName", selectedRoomType.name);
        intent.putExtra("checkInDate", checkInDate);
        intent.putExtra("checkOutDate", checkOutDate);
        intent.putExtra("numberOfRooms", Integer.parseInt((String) spinnerNumberOfRooms.getSelectedItem()));
        intent.putExtra("numberOfAdults", Integer.parseInt((String) spinnerAdults.getSelectedItem()));
        intent.putExtra("numberOfChildren", Integer.parseInt((String) spinnerChildren.getSelectedItem()));
        intent.putExtra("numberOfNights", numberOfNights);
        intent.putExtra("pricePerNight", selectedRoomType.pricePerNight);
        intent.putExtra("totalPrice", totalPrice);
        intent.putExtra("customerName", customerName);
        intent.putExtra("customerPhone", customerPhone);
        intent.putExtra("customerEmail", customerEmail);
        startActivityForResult(intent, REQUEST_HOTEL_PAYMENT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        android.util.Log.d("HotelBooking", "onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode);
        
        if (requestCode == REQUEST_HOTEL_PAYMENT && resultCode == RESULT_OK) {
            android.util.Log.d("HotelBooking", "Payment successful, creating booking...");
            
            HotelBookingMockService.BookedHotel booking = new HotelBookingMockService.BookedHotel();
            booking.bookingId = HotelBookingMockService.generateBookingId();
            booking.hotelName = selectedHotel.name;
            booking.location = selectedHotel.location;
            booking.stars = selectedHotel.stars;
            booking.roomTypeName = selectedRoomType.name;
            booking.checkInDate = checkInDate;
            booking.checkOutDate = checkOutDate;
            booking.numberOfRooms = Integer.parseInt((String) spinnerNumberOfRooms.getSelectedItem());
            booking.numberOfAdults = Integer.parseInt((String) spinnerAdults.getSelectedItem());
            booking.numberOfChildren = Integer.parseInt((String) spinnerChildren.getSelectedItem());
            booking.numberOfNights = numberOfNights;
            booking.pricePerNight = selectedRoomType.pricePerNight;
            booking.totalPrice = totalPrice;
            booking.customerName = etCustomerName.getText().toString().trim();
            booking.customerPhone = etCustomerPhone.getText().toString().trim();
            booking.customerEmail = etCustomerEmail.getText().toString().trim();
            booking.bookingTime = HotelBookingMockService.getCurrentDateTime();

            android.util.Log.d("HotelBooking", "Booking details: " + booking.bookingId + ", " + booking.hotelName);
            
            HotelBookingMockService.bookHotel(this, booking);
            
            android.util.Log.d("HotelBooking", "Booking saved, starting result activity");

            Intent intent = new Intent(this, HotelBookingResultActivity.class);
            intent.putExtra("bookingId", booking.bookingId);
            intent.putExtra("hotelName", booking.hotelName);
            intent.putExtra("location", booking.location);
            intent.putExtra("stars", booking.stars);
            intent.putExtra("roomTypeName", booking.roomTypeName);
            intent.putExtra("checkInDate", booking.checkInDate);
            intent.putExtra("checkOutDate", booking.checkOutDate);
            intent.putExtra("numberOfRooms", booking.numberOfRooms);
            intent.putExtra("numberOfAdults", booking.numberOfAdults);
            intent.putExtra("numberOfChildren", booking.numberOfChildren);
            intent.putExtra("numberOfNights", booking.numberOfNights);
            intent.putExtra("totalPrice", booking.totalPrice);
            intent.putExtra("customerName", booking.customerName);
            startActivity(intent);
            finish();
        }
    }
}
