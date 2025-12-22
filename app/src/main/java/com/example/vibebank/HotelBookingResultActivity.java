package com.example.vibebank;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class HotelBookingResultActivity extends AppCompatActivity {

    private TextView tvBookingId, tvHotelName, tvLocation, tvRoomType;
    private TextView tvCheckInDate, tvCheckOutDate, tvNumberOfNights;
    private TextView tvNumberOfRooms, tvGuests, tvCustomerName, tvTotalPrice;
    private Button btnBackToHome, btnViewMyBookings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hotel_booking_result);

        initViews();
        loadBookingDetails();
        setupListeners();
    }

    private void initViews() {
        tvBookingId = findViewById(R.id.tvBookingId);
        tvHotelName = findViewById(R.id.tvHotelName);
        tvLocation = findViewById(R.id.tvLocation);
        tvRoomType = findViewById(R.id.tvRoomType);
        tvCheckInDate = findViewById(R.id.tvCheckInDate);
        tvCheckOutDate = findViewById(R.id.tvCheckOutDate);
        tvNumberOfNights = findViewById(R.id.tvNumberOfNights);
        tvNumberOfRooms = findViewById(R.id.tvNumberOfRooms);
        tvGuests = findViewById(R.id.tvGuests);
        tvCustomerName = findViewById(R.id.tvCustomerName);
        tvTotalPrice = findViewById(R.id.tvTotalPrice);
        btnBackToHome = findViewById(R.id.btnBackToHome);
        btnViewMyBookings = findViewById(R.id.btnViewMyBookings);
    }

    private void loadBookingDetails() {
        Intent intent = getIntent();
        
        String bookingId = intent.getStringExtra("bookingId");
        String hotelName = intent.getStringExtra("hotelName");
        String location = intent.getStringExtra("location");
        int stars = intent.getIntExtra("stars", 0);
        String roomTypeName = intent.getStringExtra("roomTypeName");
        String checkInDate = intent.getStringExtra("checkInDate");
        String checkOutDate = intent.getStringExtra("checkOutDate");
        int numberOfNights = intent.getIntExtra("numberOfNights", 0);
        int numberOfRooms = intent.getIntExtra("numberOfRooms", 0);
        int numberOfAdults = intent.getIntExtra("numberOfAdults", 0);
        int numberOfChildren = intent.getIntExtra("numberOfChildren", 0);
        int totalPrice = intent.getIntExtra("totalPrice", 0);
        String customerName = intent.getStringExtra("customerName");

        tvBookingId.setText(bookingId);
        tvHotelName.setText(hotelName + " (" + stars + "★)");
        tvLocation.setText(location);
        tvRoomType.setText(roomTypeName);
        tvCheckInDate.setText(checkInDate);
        tvCheckOutDate.setText(checkOutDate);
        tvNumberOfNights.setText(numberOfNights + " đêm");
        tvNumberOfRooms.setText(numberOfRooms + " phòng");
        
        String guestInfo = numberOfAdults + " người lớn";
        if (numberOfChildren > 0) {
            guestInfo += ", " + numberOfChildren + " trẻ em";
        }
        tvGuests.setText(guestInfo);
        
        tvCustomerName.setText(customerName);
        tvTotalPrice.setText(String.format(Locale.getDefault(), "%,d VNĐ", totalPrice));
    }

    private void setupListeners() {
        btnBackToHome.setOnClickListener(v -> {
            Intent intent = new Intent(HotelBookingResultActivity.this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        btnViewMyBookings.setOnClickListener(v -> {
            Intent intent = new Intent(HotelBookingResultActivity.this, MyHotelBookingsActivity.class);
            startActivity(intent);
            finish();
        });
    }
}
