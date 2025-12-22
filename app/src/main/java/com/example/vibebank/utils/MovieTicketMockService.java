package com.example.vibebank.utils;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MovieTicketMockService {

    private static final String PREFS_NAME = "MovieTicketPrefs";
    private static final String KEY_BOOKED_SEATS = "booked_seats";
    private static Context appContext;

    public static void init(Context context) {
        if (appContext == null) {
            appContext = context.getApplicationContext();
        }
    }

    public static class Movie {
        private String id;
        private String title;
        private String genre;
        private int duration; // minutes
        private String rating;
        private String posterUrl;

        public Movie(String id, String title, String genre, int duration, String rating) {
            this.id = id;
            this.title = title;
            this.genre = genre;
            this.duration = duration;
            this.rating = rating;
        }

        public String getId() { return id; }
        public String getTitle() { return title; }
        public String getGenre() { return genre; }
        public int getDuration() { return duration; }
        public String getRating() { return rating; }

        @Override
        public String toString() {
            return title + " (" + duration + " phút)";
        }
    }

    public static class Cinema {
        private String id;
        private String name;
        private String address;

        public Cinema(String id, String name, String address) {
            this.id = id;
            this.name = name;
            this.address = address;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public String getAddress() { return address; }

        @Override
        public String toString() {
            return name;
        }
    }

    public static class Showtime {
        private String movieId;
        private String cinemaId;
        private String date;
        private String time;
        private long pricePerSeat;

        public Showtime(String movieId, String cinemaId, String date, String time, long pricePerSeat) {
            this.movieId = movieId;
            this.cinemaId = cinemaId;
            this.date = date;
            this.time = time;
            this.pricePerSeat = pricePerSeat;
        }

        public String getMovieId() { return movieId; }
        public String getCinemaId() { return cinemaId; }
        public String getDate() { return date; }
        public String getTime() { return time; }
        public long getPricePerSeat() { return pricePerSeat; }

        public String getShowtimeKey() {
            return movieId + "_" + cinemaId + "_" + date + "_" + time;
        }

        @Override
        public String toString() {
            return time + " - " + pricePerSeat / 1000 + "K";
        }
    }

    public static class BookedTicket {
        private String bookingId;
        private String movieTitle;
        private String cinemaName;
        private String date;
        private String time;
        private List<String> seats;
        private long totalPrice;
        private String customerName;
        private String customerPhone;
        private String customerEmail;
        private String bookingTime;

        public BookedTicket(String bookingId, String movieTitle, String cinemaName, String date, 
                          String time, List<String> seats, long totalPrice, String customerName,
                          String customerPhone, String customerEmail, String bookingTime) {
            this.bookingId = bookingId;
            this.movieTitle = movieTitle;
            this.cinemaName = cinemaName;
            this.date = date;
            this.time = time;
            this.seats = seats;
            this.totalPrice = totalPrice;
            this.customerName = customerName;
            this.customerPhone = customerPhone;
            this.customerEmail = customerEmail;
            this.bookingTime = bookingTime;
        }

        public String getBookingId() { return bookingId; }
        public String getMovieTitle() { return movieTitle; }
        public String getCinemaName() { return cinemaName; }
        public String getDate() { return date; }
        public String getTime() { return time; }
        public List<String> getSeats() { return seats; }
        public long getTotalPrice() { return totalPrice; }
        public String getCustomerName() { return customerName; }
        public String getCustomerPhone() { return customerPhone; }
        public String getCustomerEmail() { return customerEmail; }
        public String getBookingTime() { return bookingTime; }

        public JSONObject toJSON() {
            try {
                JSONObject json = new JSONObject();
                json.put("bookingId", bookingId);
                json.put("movieTitle", movieTitle);
                json.put("cinemaName", cinemaName);
                json.put("date", date);
                json.put("time", time);
                JSONArray seatsArray = new JSONArray(seats);
                json.put("seats", seatsArray);
                json.put("totalPrice", totalPrice);
                json.put("customerName", customerName);
                json.put("customerPhone", customerPhone);
                json.put("customerEmail", customerEmail);
                json.put("bookingTime", bookingTime);
                return json;
            } catch (JSONException e) {
                return null;
            }
        }

        public static BookedTicket fromJSON(JSONObject json) {
            try {
                JSONArray seatsArray = json.getJSONArray("seats");
                List<String> seats = new ArrayList<>();
                for (int i = 0; i < seatsArray.length(); i++) {
                    seats.add(seatsArray.getString(i));
                }
                return new BookedTicket(
                    json.getString("bookingId"),
                    json.getString("movieTitle"),
                    json.getString("cinemaName"),
                    json.getString("date"),
                    json.getString("time"),
                    seats,
                    json.getLong("totalPrice"),
                    json.getString("customerName"),
                    json.getString("customerPhone"),
                    json.optString("customerEmail", ""),
                    json.getString("bookingTime")
                );
            } catch (JSONException e) {
                return null;
            }
        }
    }

    // Mock data
    public static List<Movie> getAvailableMovies() {
        List<Movie> movies = new ArrayList<>();
        movies.add(new Movie("M001", "Avengers: Endgame", "Hành động, Phiêu lưu", 181, "13+"));
        movies.add(new Movie("M002", "The Batman", "Hành động, Tội phạm", 176, "16+"));
        movies.add(new Movie("M003", "Spider-Man: No Way Home", "Hành động, Phiêu lưu", 148, "13+"));
        movies.add(new Movie("M004", "Mai", "Tâm lý, Tình cảm", 131, "16+"));
        movies.add(new Movie("M005", "Doraemon: Nobita và Vùng Đất Lý Tưởng", "Hoạt hình, Gia đình", 108, "P"));
        return movies;
    }

    public static List<Cinema> getAvailableCinemas() {
        List<Cinema> cinemas = new ArrayList<>();
        cinemas.add(new Cinema("C001", "CGV Vincom Center", "72 Lê Thánh Tôn, Q.1, TP.HCM"));
        cinemas.add(new Cinema("C002", "Lotte Cinema Diamond Plaza", "34 Lê Duẩn, Q.1, TP.HCM"));
        cinemas.add(new Cinema("C003", "BHD Star Cineplex", "3/2 Street, Q.10, TP.HCM"));
        return cinemas;
    }

    public static List<Showtime> getShowtimes(String movieId, String cinemaId, String date) {
        List<Showtime> showtimes = new ArrayList<>();
        showtimes.add(new Showtime(movieId, cinemaId, date, "09:00", 75000));
        showtimes.add(new Showtime(movieId, cinemaId, date, "12:30", 85000));
        showtimes.add(new Showtime(movieId, cinemaId, date, "15:45", 85000));
        showtimes.add(new Showtime(movieId, cinemaId, date, "19:00", 95000));
        showtimes.add(new Showtime(movieId, cinemaId, date, "21:30", 95000));
        return showtimes;
    }

    // Seat layout: 8 rows (A-H), 12 columns (1-12)
    public static List<String> getAllSeats() {
        List<String> seats = new ArrayList<>();
        String[] rows = {"A", "B", "C", "D", "E", "F", "G", "H"};
        for (String row : rows) {
            for (int col = 1; col <= 12; col++) {
                seats.add(row + col);
            }
        }
        return seats;
    }

    // Get booked seats for a specific showtime
    public static Set<String> getBookedSeats(String showtimeKey) {
        if (appContext == null) {
            return new HashSet<>();
        }
        SharedPreferences prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String bookedSeatsJson = prefs.getString(KEY_BOOKED_SEATS, "{}");
        
        try {
            JSONObject allBookedSeats = new JSONObject(bookedSeatsJson);
            if (allBookedSeats.has(showtimeKey)) {
                JSONArray seatsArray = allBookedSeats.getJSONArray(showtimeKey);
                Set<String> bookedSeats = new HashSet<>();
                for (int i = 0; i < seatsArray.length(); i++) {
                    bookedSeats.add(seatsArray.getString(i));
                }
                return bookedSeats;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        
        return new HashSet<>();
    }

    // Book seats
    public static void bookSeats(String showtimeKey, List<String> seats, String movieTitle, 
                               String cinemaName, String date, String time, long totalPrice,
                               String customerName, String customerPhone, String customerEmail) {
        if (appContext == null) {
            return;
        }

        // Generate booking ID
        String bookingId = "MV" + System.currentTimeMillis();
        String bookingTime = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
                .format(new java.util.Date());

        // Create booked ticket
        BookedTicket ticket = new BookedTicket(bookingId, movieTitle, cinemaName, date, time,
                seats, totalPrice, customerName, customerPhone, customerEmail, bookingTime);

        SharedPreferences prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Save to booked tickets list
        String bookedTicketsJson = prefs.getString("booked_tickets_data", "[]");
        try {
            JSONArray ticketsArray = new JSONArray(bookedTicketsJson);
            ticketsArray.put(ticket.toJSON());
            prefs.edit().putString("booked_tickets_data", ticketsArray.toString()).apply();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // Save booked seats
        String bookedSeatsJson = prefs.getString(KEY_BOOKED_SEATS, "{}");
        try {
            JSONObject allBookedSeats = new JSONObject(bookedSeatsJson);
            JSONArray existingSeats = allBookedSeats.optJSONArray(showtimeKey);
            if (existingSeats == null) {
                existingSeats = new JSONArray();
            }
            for (String seat : seats) {
                existingSeats.put(seat);
            }
            allBookedSeats.put(showtimeKey, existingSeats);
            prefs.edit().putString(KEY_BOOKED_SEATS, allBookedSeats.toString()).apply();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // Get all booked tickets
    public static List<BookedTicket> getMyBookedTickets() {
        List<BookedTicket> tickets = new ArrayList<>();
        if (appContext == null) {
            return tickets;
        }

        SharedPreferences prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String bookedTicketsJson = prefs.getString("booked_tickets_data", "[]");

        try {
            JSONArray ticketsArray = new JSONArray(bookedTicketsJson);
            for (int i = 0; i < ticketsArray.length(); i++) {
                BookedTicket ticket = BookedTicket.fromJSON(ticketsArray.getJSONObject(i));
                if (ticket != null) {
                    tickets.add(ticket);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // Sort by booking time (newest first)
        tickets.sort((t1, t2) -> t2.getBookingTime().compareTo(t1.getBookingTime()));

        return tickets;
    }
}
