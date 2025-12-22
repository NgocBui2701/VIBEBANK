package com.example.vibebank;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.vibebank.utils.HotelBookingMockService;

import java.util.List;
import java.util.Locale;

public class MyHotelBookingsActivity extends AppCompatActivity {

    private LinearLayout layoutBookingsList;
    private ImageButton btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_hotel_bookings);

        initViews();
        loadBookings();
        setupListeners();
    }

    private void initViews() {
        layoutBookingsList = findViewById(R.id.layoutBookingsList);
        btnBack = findViewById(R.id.btnBack);
    }

    private void loadBookings() {
        android.util.Log.d("MyHotelBookings", "Loading bookings from Firestore...");
        
        // Show loading state
        layoutBookingsList.removeAllViews();
        TextView tvLoading = new TextView(this);
        tvLoading.setText("Đang tải...");
        tvLoading.setTextSize(16);
        tvLoading.setPadding(16, 16, 16, 16);
        layoutBookingsList.addView(tvLoading);
        
        HotelBookingMockService.getMyBookedHotels(this, bookings -> {
            android.util.Log.d("MyHotelBookings", "Firestore returned " + bookings.size() + " bookings");
            
            // Remove loading view
            layoutBookingsList.removeAllViews();
            
            if (bookings.isEmpty()) {
                android.util.Log.d("MyHotelBookings", "No bookings found");
                TextView tvEmpty = new TextView(this);
                tvEmpty.setText("Chưa có đặt phòng nào.\n\nVé đã đặt sẽ xuất hiện ở đây sau khi thanh toán thành công.");
                tvEmpty.setTextSize(16);
                tvEmpty.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
                tvEmpty.setPadding(32, 64, 32, 32);
                layoutBookingsList.addView(tvEmpty);
                return;
            }

            for (HotelBookingMockService.BookedHotel booking : bookings) {
                View bookingView = LayoutInflater.from(this)
                        .inflate(R.layout.item_booked_hotel, layoutBookingsList, false);

                TextView tvBookingId = bookingView.findViewById(R.id.tvBookingId);
                TextView tvHotelName = bookingView.findViewById(R.id.tvHotelName);
                TextView tvLocation = bookingView.findViewById(R.id.tvLocation);
                TextView tvCheckInDate = bookingView.findViewById(R.id.tvCheckInDate);
                TextView tvCheckOutDate = bookingView.findViewById(R.id.tvCheckOutDate);
                TextView tvRoomInfo = bookingView.findViewById(R.id.tvRoomInfo);
                TextView tvGuestInfo = bookingView.findViewById(R.id.tvGuestInfo);
                TextView tvCustomerInfo = bookingView.findViewById(R.id.tvCustomerInfo);
                TextView tvTotalPrice = bookingView.findViewById(R.id.tvTotalPrice);
                TextView tvBookingTime = bookingView.findViewById(R.id.tvBookingTime);

                tvBookingId.setText(booking.bookingId);
                tvHotelName.setText(booking.hotelName + " (" + booking.stars + "★)");
                tvLocation.setText(booking.location);
                tvCheckInDate.setText(booking.checkInDate);
                tvCheckOutDate.setText(booking.checkOutDate);
                
                String roomInfo = booking.roomTypeName + " - " + booking.numberOfRooms + " phòng - " 
                        + booking.numberOfNights + " đêm";
                tvRoomInfo.setText(roomInfo);
                
                String guestInfo = booking.numberOfAdults + " người lớn";
                if (booking.numberOfChildren > 0) {
                    guestInfo += ", " + booking.numberOfChildren + " trẻ em";
                }
                tvGuestInfo.setText(guestInfo);
                
                String customerInfo = booking.customerName + " | " + booking.customerPhone;
                tvCustomerInfo.setText(customerInfo);
                
                tvTotalPrice.setText(String.format(Locale.getDefault(), "%,d VNĐ", booking.totalPrice));
                tvBookingTime.setText("Đặt lúc: " + booking.bookingTime);

                layoutBookingsList.addView(bookingView);
            }
        });
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
    }
}
