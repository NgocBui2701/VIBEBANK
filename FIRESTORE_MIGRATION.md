# Firestore Migration for Tickets

## Tổng Quan
Hệ thống vé đã được chuyển từ SharedPreferences sang Firebase Firestore database.

## Các Loại Vé Đã Migrate

### ✅ 1. Hotel Bookings (Đặt Phòng Khách Sạn)
- **Collection**: `booked_hotel_bookings`
- **Service**: `Ticket DatabaseService.saveHotelBooking()` / `loadHotelBookings()`
- **Activity**: `HotelBookingActivity`, `MyHotelBookingsActivity`
- **Status**: Đã hoàn tất migration

**Cấu trúc dữ liệu**:
```
{
  bookingId: "HT1234567890",
  userId: "firebase_user_id",
  hotelName: "Rex Hotel",
  location: "TP.HCM",
  stars: 5,
  roomTypeName: "Deluxe",
  numberOfRooms: 2,
  numberOfAdults: 2,
  numberOfChildren: 1,
  checkInDate: "25/12/2025",
  checkOutDate: "27/12/2025",
  numberOfNights: 2,
  pricePerNight: 1200000,
  totalPrice: 4800000,
  customerName: "Nguyễn Văn A",
  customerPhone: "0901234567",
  customerEmail: "email@example.com",
  bookingTime: "22/12/2025 14:30",
  timestamp: 1703234567890,
  type: "hotel"
}
```

### 📋 2. Flight Tickets (Vé Máy Bay)
- **Collection**: `booked_flight_tickets`
- **Service**: `TicketDatabaseService.saveFlightTicket()` / `loadFlightTickets()`
- **Activity**: `FlightTicketActivity`, `MyTicketsActivity`
- **Status**: Sẵn sàng để tích hợp

**Cấu trúc dữ liệu**:
```
{
  bookingId: "BK1234567890",
  userId: "firebase_user_id",
  flightCode: "VN201",
  airline: "Vietnam Airlines",
  departure: "SGN",
  destination: "HAN",
  departureDate: "25/12/2025",
  departureTime: "06:00",
  arrivalTime: "08:15",
  seatClass: "Economy",
  price: 1500000,
  duration: "2h 15m",
  passengerName: "Nguyễn Văn A",
  passengerID: "123456789",
  passengerPhone: "0901234567",
  passengerEmail: "email@example.com",
  bookingTime: "22/12/2025 14:30",
  timestamp: 1703234567890,
  type: "flight"
}
```

### 📋 3. Movie Tickets (Vé Xem Phim)
- **Collection**: `booked_movie_tickets`
- **Service**: `TicketDatabaseService.saveMovieTicket()` / `loadMovieTickets()`
- **Activity**: `MovieTicketActivity`, `MyMovieTicketsActivity`
- **Status**: Sẵn sàng để tích hợp

**Cấu trúc dữ liệu**:
```
{
  bookingId: "MV1234567890",
  userId: "firebase_user_id",
  movieTitle: "Avatar 3",
  cinemaName: "CGV Vincom Center",
  date: "25/12/2025",
  time: "19:00",
  seats: ["A1", "A2", "A3"],
  showtimeKey: "M001_C001_25122025_1900",
  totalPrice: 285000,
  customerName: "Nguyễn Văn A",
  customerPhone: "0901234567",
  customerEmail: "email@example.com",
  bookingTime: "22/12/2025 14:30",
  timestamp: 1703234567890,
  type: "movie"
}
```

## Cách Sử Dụng TicketDatabaseService

### 1. Lưu Vé Hotel
```java
Map<String, Object> bookingData = new HashMap<>();
bookingData.put("bookingId", "HT" + System.currentTimeMillis());
bookingData.put("hotelName", "Rex Hotel");
// ... thêm các fields khác

TicketDatabaseService.saveHotelBooking(bookingData, new TicketDatabaseService.OnSaveListener() {
    @Override
    public void onSuccess(String bookingId) {
        Log.d("TAG", "Saved: " + bookingId);
    }
    
    @Override
    public void onError(String error) {
        Log.e("TAG", "Error: " + error);
    }
});
```

### 2. Load Vé Hotel
```java
TicketDatabaseService.loadHotelBookings(data -> {
    for (Map<String, Object> booking : data) {
        String hotelName = (String) booking.get("hotelName");
        // ... xử lý dữ liệu
    }
});
```

### 3. Load Tất Cả Các Loại Vé
```java
TicketDatabaseService.loadAllTickets(new TicketDatabaseService.OnLoadAllTicketsListener() {
    @Override
    public void onAllLoaded(
        List<Map<String, Object>> flightTickets,
        List<Map<String, Object>> movieTickets,
        List<Map<String, Object>> hotelBookings
    ) {
        Log.d("TAG", "Flights: " + flightTickets.size());
        Log.d("TAG", "Movies: " + movieTickets.size());
        Log.d("TAG", "Hotels: " + hotelBookings.size());
    }
});
```

## Firestore Rules

Cần thêm rules vào Firebase Console:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Flight Tickets
    match /booked_flight_tickets/{ticketId} {
      allow read, write: if request.auth != null && 
        request.resource.data.userId == request.auth.uid;
    }
    
    // Movie Tickets
    match /booked_movie_tickets/{ticketId} {
      allow read, write: if request.auth != null && 
        request.resource.data.userId == request.auth.uid;
    }
    
    // Hotel Bookings
    match /booked_hotel_bookings/{bookingId} {
      allow read, write: if request.auth != null && 
        request.resource.data.userId == request.auth.uid;
    }
  }
}
```

## Lợi Ích

1. **Đồng bộ giữa thiết bị**: Vé được lưu trên cloud, truy cập từ bất kỳ thiết bị nào
2. **Bảo mật**: Firestore rules đảm bảo user chỉ truy cập vé của mình
3. **Scalability**: Hỗ trợ hàng triệu records
4. **Real-time**: Có thể bật realtime listeners để update tự động
5. **Backup**: Dữ liệu được backup tự động bởi Firebase

## Migration Checklist

- [x] Tạo `TicketDatabaseService.java`
- [x] Update `HotelBookingMockService` để dùng Firestore
- [x] Update `MyHotelBookingsActivity` với callback async
- [x] Test hotel booking flow hoàn chỉnh
- [ ] Update `FlightTicketMockService` để dùng Firestore
- [ ] Update `MovieTicketMockService` để dùng Firestore
- [ ] Update các Activities liên quan
- [ ] Thêm Firestore rules vào Firebase Console
- [ ] Test tất cả flows
- [ ] Migration dữ liệu cũ từ SharedPreferences (nếu cần)

## Notes

- Tất cả operations là asynchronous (dùng callbacks)
- userId được tự động thêm vào mỗi record
- timestamp được tự động thêm để sort
- Mỗi loại vé có collection riêng để dễ quản lý
