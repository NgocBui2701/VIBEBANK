package com.example.vibebank.utils;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HotelBookingMockService {

    private static final String PREFS_NAME = "HotelBookingPrefs";
    private static final String KEY_BOOKED_HOTELS = "booked_hotels_data";

    public static class Hotel {
        public String id;
        public String name;
        public String location;
        public int stars;
        public String image;
        public String description;

        public Hotel(String id, String name, String location, int stars, String description) {
            this.id = id;
            this.name = name;
            this.location = location;
            this.stars = stars;
            this.description = description;
        }

        @Override
        public String toString() {
            return name + " (" + stars + "★) - " + location;
        }
    }

    public static class RoomType {
        public String id;
        public String name;
        public int pricePerNight;
        public int maxGuests;
        public String description;

        public RoomType(String id, String name, int pricePerNight, int maxGuests, String description) {
            this.id = id;
            this.name = name;
            this.pricePerNight = pricePerNight;
            this.maxGuests = maxGuests;
            this.description = description;
        }

        @Override
        public String toString() {
            return name + " - " + formatPrice(pricePerNight) + "/đêm";
        }

        private String formatPrice(int price) {
            return String.format(Locale.getDefault(), "%,d VNĐ", price);
        }
    }

    public static class BookedHotel {
        public String bookingId;
        public String hotelName;
        public String location;
        public int stars;
        public String roomTypeName;
        public String checkInDate;
        public String checkOutDate;
        public int numberOfRooms;
        public int numberOfAdults;
        public int numberOfChildren;
        public int numberOfNights;
        public int pricePerNight;
        public int totalPrice;
        public String customerName;
        public String customerPhone;
        public String customerEmail;
        public String bookingTime;

        public JSONObject toJSON() throws JSONException {
            JSONObject json = new JSONObject();
            json.put("bookingId", bookingId);
            json.put("hotelName", hotelName);
            json.put("location", location);
            json.put("stars", stars);
            json.put("roomTypeName", roomTypeName);
            json.put("checkInDate", checkInDate);
            json.put("checkOutDate", checkOutDate);
            json.put("numberOfRooms", numberOfRooms);
            json.put("numberOfAdults", numberOfAdults);
            json.put("numberOfChildren", numberOfChildren);
            json.put("numberOfNights", numberOfNights);
            json.put("pricePerNight", pricePerNight);
            json.put("totalPrice", totalPrice);
            json.put("customerName", customerName);
            json.put("customerPhone", customerPhone);
            json.put("customerEmail", customerEmail);
            json.put("bookingTime", bookingTime);
            return json;
        }

        public static BookedHotel fromJSON(JSONObject json) throws JSONException {
            BookedHotel booking = new BookedHotel();
            booking.bookingId = json.getString("bookingId");
            booking.hotelName = json.getString("hotelName");
            booking.location = json.getString("location");
            booking.stars = json.getInt("stars");
            booking.roomTypeName = json.getString("roomTypeName");
            booking.checkInDate = json.getString("checkInDate");
            booking.checkOutDate = json.getString("checkOutDate");
            booking.numberOfRooms = json.getInt("numberOfRooms");
            booking.numberOfAdults = json.getInt("numberOfAdults");
            booking.numberOfChildren = json.getInt("numberOfChildren");
            booking.numberOfNights = json.getInt("numberOfNights");
            booking.pricePerNight = json.getInt("pricePerNight");
            booking.totalPrice = json.getInt("totalPrice");
            booking.customerName = json.getString("customerName");
            booking.customerPhone = json.getString("customerPhone");
            booking.customerEmail = json.getString("customerEmail");
            booking.bookingTime = json.getString("bookingTime");
            return booking;
        }
    }

    public static List<String> getLocations() {
        List<String> locations = new ArrayList<>();
        locations.add("Tất cả");
        locations.add("TP.HCM");
        locations.add("Vũng Tàu");
        locations.add("Nha Trang");
        return locations;
    }

    public static List<Hotel> getHotels() {
        List<Hotel> hotels = new ArrayList<>();
        hotels.add(new Hotel("H001", "InterContinental Saigon", "TP.HCM", 5, "Khách sạn 5 sao sang trọng tại trung tâm thành phố"));
        hotels.add(new Hotel("H002", "Sheraton Saigon", "TP.HCM", 5, "Khách sạn 5 sao với tầm nhìn sông Sài Gòn"));
        hotels.add(new Hotel("H003", "Hotel Nikko Saigon", "TP.HCM", 5, "Khách sạn phong cách Nhật Bản"));
        hotels.add(new Hotel("H004", "Liberty Central Saigon Riverside", "TP.HCM", 4, "Khách sạn 4 sao view sông đẹp"));
        hotels.add(new Hotel("H005", "Pullman Vung Tau", "Vũng Tàu", 5, "Resort 5 sao bên bãi biển"));
        hotels.add(new Hotel("H006", "Vinpearl Resort Nha Trang", "Nha Trang", 5, "Resort 5 sao với bãi biển riêng"));
        return hotels;
    }

    public static List<Hotel> getHotelsByLocation(String location) {
        if (location.equals("Tất cả")) {
            return getHotels();
        }
        List<Hotel> filteredHotels = new ArrayList<>();
        for (Hotel hotel : getHotels()) {
            if (hotel.location.equals(location)) {
                filteredHotels.add(hotel);
            }
        }
        return filteredHotels;
    }

    public static List<RoomType> getRoomTypes() {
        List<RoomType> roomTypes = new ArrayList<>();
        roomTypes.add(new RoomType("R001", "Standard", 800000, 2, "Phòng tiêu chuẩn với giường đôi"));
        roomTypes.add(new RoomType("R002", "Deluxe", 1200000, 2, "Phòng cao cấp với view đẹp"));
        roomTypes.add(new RoomType("R003", "Suite", 2000000, 4, "Phòng suite rộng rãi"));
        roomTypes.add(new RoomType("R004", "Family", 1500000, 4, "Phòng gia đình 2 giường đôi"));
        roomTypes.add(new RoomType("R005", "Executive", 3000000, 2, "Phòng hạng sang với phòng khách riêng"));
        return roomTypes;
    }

    public static void bookHotel(Context context, BookedHotel booking) {
        android.util.Log.d("HotelBooking", "========== BOOKING HOTEL ==========");
        android.util.Log.d("HotelBooking", "Hotel: " + booking.hotelName);
        android.util.Log.d("HotelBooking", "Booking ID: " + booking.bookingId);
        android.util.Log.d("HotelBooking", "Customer: " + booking.customerName);
        
        // Convert BookedHotel to Map
        java.util.Map<String, Object> bookingMap = new java.util.HashMap<>();
        bookingMap.put("bookingId", booking.bookingId);
        bookingMap.put("hotelName", booking.hotelName);
        bookingMap.put("location", booking.location);
        bookingMap.put("stars", booking.stars);
        bookingMap.put("roomTypeName", booking.roomTypeName);
        bookingMap.put("numberOfRooms", booking.numberOfRooms);
        bookingMap.put("numberOfAdults", booking.numberOfAdults);
        bookingMap.put("numberOfChildren", booking.numberOfChildren);
        bookingMap.put("checkInDate", booking.checkInDate);
        bookingMap.put("checkOutDate", booking.checkOutDate);
        bookingMap.put("numberOfNights", booking.numberOfNights);
        bookingMap.put("pricePerNight", booking.pricePerNight);
        bookingMap.put("totalPrice", booking.totalPrice);
        bookingMap.put("customerName", booking.customerName);
        bookingMap.put("customerPhone", booking.customerPhone);
        bookingMap.put("customerEmail", booking.customerEmail);
        bookingMap.put("bookingTime", booking.bookingTime);
        
        android.util.Log.d("HotelBooking", "Calling TicketDatabaseService.saveHotelBooking...");
        TicketDatabaseService.saveHotelBooking(bookingMap, new TicketDatabaseService.OnSaveListener() {
            @Override
            public void onSuccess(String bookingId) {
                android.util.Log.d("HotelBooking", "✓ SUCCESS: Booking saved to Firestore: " + bookingId);
            }
            
            @Override
            public void onError(String error) {
                android.util.Log.e("HotelBooking", "✗ ERROR: Failed to save booking: " + error);
            }
        });
    }

    public static void getMyBookedHotels(Context context, OnBookingsLoadedListener listener) {
        android.util.Log.d("HotelBooking", "========== LOADING BOOKINGS ==========");
        android.util.Log.d("HotelBooking", "Calling TicketDatabaseService.loadHotelBookings...");
        
        TicketDatabaseService.loadHotelBookings(data -> {
            android.util.Log.d("HotelBooking", "Firestore returned " + data.size() + " raw records");
            List<BookedHotel> bookings = new ArrayList<>();
            
            for (java.util.Map<String, Object> map : data) {
                try {
                    android.util.Log.d("HotelBooking", "Converting record: " + map.get("bookingId"));
                    BookedHotel booking = new BookedHotel();
                    booking.bookingId = (String) map.get("bookingId");
                    booking.hotelName = (String) map.get("hotelName");
                    booking.location = (String) map.get("location");
                    booking.stars = map.get("stars") instanceof Long ? 
                        ((Long) map.get("stars")).intValue() : (Integer) map.get("stars");
                    booking.roomTypeName = (String) map.get("roomTypeName");
                    booking.numberOfRooms = ((Long) map.get("numberOfRooms")).intValue();
                    booking.numberOfAdults = ((Long) map.get("numberOfAdults")).intValue();
                    booking.numberOfChildren = ((Long) map.get("numberOfChildren")).intValue();
                    booking.checkInDate = (String) map.get("checkInDate");
                    booking.checkOutDate = (String) map.get("checkOutDate");
                    booking.numberOfNights = ((Long) map.get("numberOfNights")).intValue();
                    booking.pricePerNight = ((Long) map.get("pricePerNight")).intValue();
                    booking.totalPrice = ((Long) map.get("totalPrice")).intValue();
                    booking.customerName = (String) map.get("customerName");
                    booking.customerPhone = (String) map.get("customerPhone");
                    booking.customerEmail = (String) map.get("customerEmail");
                    booking.bookingTime = (String) map.get("bookingTime");
                    
                    bookings.add(booking);
                    android.util.Log.d("HotelBooking", "✓ Converted: " + booking.hotelName);
                } catch (Exception e) {
                    android.util.Log.e("HotelBooking", "✗ Error converting booking", e);
                }
            }
            
            android.util.Log.d("HotelBooking", "========== RESULT ==========");
            android.util.Log.d("HotelBooking", "Total bookings loaded: " + bookings.size());
            listener.onBookingsLoaded(bookings);
        });
    }
    
    public interface OnBookingsLoadedListener {
        void onBookingsLoaded(List<BookedHotel> bookings);
    }

    public static String generateBookingId() {
        return "HTL" + System.currentTimeMillis();
    }

    public static String getCurrentDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }
}
