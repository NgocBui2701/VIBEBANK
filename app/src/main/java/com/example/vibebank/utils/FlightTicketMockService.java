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

public class FlightTicketMockService {

    private static final String PREFS_NAME = "FlightTicketPrefs";
    private static final String KEY_BOOKED_TICKETS = "booked_tickets";
    private static Context appContext;

    public static void init(Context context) {
        if (appContext == null) {
            appContext = context.getApplicationContext();
        }
    }

    public static class FlightTicket {
        private String flightCode;
        private String airline;
        private String departure;
        private String destination;
        private String departureDate;
        private String departureTime;
        private String arrivalTime;
        private String seatClass;
        private long price;
        private String duration;
        private boolean isBooked;

        public FlightTicket(String flightCode, String airline, String departure, String destination,
                          String departureDate, String departureTime, String arrivalTime, 
                          String seatClass, long price, String duration) {
            this.flightCode = flightCode;
            this.airline = airline;
            this.departure = departure;
            this.destination = destination;
            this.departureDate = departureDate;
            this.departureTime = departureTime;
            this.arrivalTime = arrivalTime;
            this.seatClass = seatClass;
            this.price = price;
            this.duration = duration;
        }

        public String getFlightCode() { return flightCode; }
        public String getAirline() { return airline; }
        public String getDeparture() { return departure; }
        public String getDestination() { return destination; }
        public String getDepartureDate() { return departureDate; }
        public String getDepartureTime() { return departureTime; }
        public String getArrivalTime() { return arrivalTime; }
        public String getSeatClass() { return seatClass; }
        public long getPrice() { return price; }
        public String getDuration() { return duration; }
        public boolean isBooked() { return isBooked; }
        public void setBooked(boolean booked) { isBooked = booked; }

        public String getBookingKey() {
            return flightCode + "_" + departureDate;
        }
    }

    public static class BookedTicket {
        private String bookingId;
        private String flightCode;
        private String airline;
        private String departure;
        private String destination;
        private String departureDate;
        private String departureTime;
        private String arrivalTime;
        private String seatClass;
        private long price;
        private String duration;
        private String passengerName;
        private String passengerID;
        private String passengerPhone;
        private String passengerEmail;
        private String bookingTime;

        public BookedTicket(String bookingId, String flightCode, String airline, String departure, 
                          String destination, String departureDate, String departureTime, 
                          String arrivalTime, String seatClass, long price, String duration,
                          String passengerName, String passengerID, String passengerPhone, 
                          String passengerEmail, String bookingTime) {
            this.bookingId = bookingId;
            this.flightCode = flightCode;
            this.airline = airline;
            this.departure = departure;
            this.destination = destination;
            this.departureDate = departureDate;
            this.departureTime = departureTime;
            this.arrivalTime = arrivalTime;
            this.seatClass = seatClass;
            this.price = price;
            this.duration = duration;
            this.passengerName = passengerName;
            this.passengerID = passengerID;
            this.passengerPhone = passengerPhone;
            this.passengerEmail = passengerEmail;
            this.bookingTime = bookingTime;
        }

        // Getters
        public String getBookingId() { return bookingId; }
        public String getFlightCode() { return flightCode; }
        public String getAirline() { return airline; }
        public String getDeparture() { return departure; }
        public String getDestination() { return destination; }
        public String getDepartureDate() { return departureDate; }
        public String getDepartureTime() { return departureTime; }
        public String getArrivalTime() { return arrivalTime; }
        public String getSeatClass() { return seatClass; }
        public long getPrice() { return price; }
        public String getDuration() { return duration; }
        public String getPassengerName() { return passengerName; }
        public String getPassengerID() { return passengerID; }
        public String getPassengerPhone() { return passengerPhone; }
        public String getPassengerEmail() { return passengerEmail; }
        public String getBookingTime() { return bookingTime; }

        public JSONObject toJSON() {
            try {
                JSONObject json = new JSONObject();
                json.put("bookingId", bookingId);
                json.put("flightCode", flightCode);
                json.put("airline", airline);
                json.put("departure", departure);
                json.put("destination", destination);
                json.put("departureDate", departureDate);
                json.put("departureTime", departureTime);
                json.put("arrivalTime", arrivalTime);
                json.put("seatClass", seatClass);
                json.put("price", price);
                json.put("duration", duration);
                json.put("passengerName", passengerName);
                json.put("passengerID", passengerID);
                json.put("passengerPhone", passengerPhone);
                json.put("passengerEmail", passengerEmail);
                json.put("bookingTime", bookingTime);
                return json;
            } catch (JSONException e) {
                return null;
            }
        }

        public static BookedTicket fromJSON(JSONObject json) {
            try {
                return new BookedTicket(
                    json.getString("bookingId"),
                    json.getString("flightCode"),
                    json.getString("airline"),
                    json.getString("departure"),
                    json.getString("destination"),
                    json.getString("departureDate"),
                    json.getString("departureTime"),
                    json.getString("arrivalTime"),
                    json.getString("seatClass"),
                    json.getLong("price"),
                    json.getString("duration"),
                    json.getString("passengerName"),
                    json.getString("passengerID"),
                    json.getString("passengerPhone"),
                    json.optString("passengerEmail", ""),
                    json.getString("bookingTime")
                );
            } catch (JSONException e) {
                return null;
            }
        }
    }

    public static class Route {
        private String code;
        private String name;

        public Route(String code, String name) {
            this.code = code;
            this.name = name;
        }

        public String getCode() { return code; }
        public String getName() { return name; }

        @Override
        public String toString() {
            return name + " (" + code + ")";
        }
    }

    public static List<Route> getAvailableRoutes() {
        List<Route> routes = new ArrayList<>();
        routes.add(new Route("SGN", "TP. Hồ Chí Minh"));
        routes.add(new Route("HAN", "Hà Nội"));
        routes.add(new Route("DAD", "Đà Nẵng"));
        routes.add(new Route("PQC", "Phú Quốc"));
        routes.add(new Route("VCA", "Nha Trang"));
        routes.add(new Route("DLI", "Đà Lạt"));
        return routes;
    }

    public static List<FlightTicket> searchFlights(String departureCode, String destinationCode, String date) {
        List<FlightTicket> flights = new ArrayList<>();
        Set<String> bookedTickets = getBookedTickets();
        
        // SGN to HAN
        if (departureCode.equals("SGN") && destinationCode.equals("HAN")) {
            FlightTicket vn201 = new FlightTicket("VN201", "Vietnam Airlines", "SGN", "HAN", date, "06:00", "08:15", "Economy", 1500000, "2h 15m");
            vn201.setBooked(bookedTickets.contains(vn201.getBookingKey()));
            flights.add(vn201);
            
            FlightTicket vj101 = new FlightTicket("VJ101", "VietJet Air", "SGN", "HAN", date, "07:30", "09:45", "Economy", 1200000, "2h 15m");
            vj101.setBooked(bookedTickets.contains(vj101.getBookingKey()));
            flights.add(vj101);
            
            FlightTicket bl201 = new FlightTicket("BL201", "Bamboo Airways", "SGN", "HAN", date, "09:00", "11:15", "Economy", 1300000, "2h 15m");
            bl201.setBooked(bookedTickets.contains(bl201.getBookingKey()));
            flights.add(bl201);
            
            FlightTicket vn211 = new FlightTicket("VN211", "Vietnam Airlines", "SGN", "HAN", date, "12:00", "14:15", "Business", 3500000, "2h 15m");
            vn211.setBooked(bookedTickets.contains(vn211.getBookingKey()));
            flights.add(vn211);
        }
        
        // HAN to SGN
        if (departureCode.equals("HAN") && destinationCode.equals("SGN")) {
            FlightTicket vn202 = new FlightTicket("VN202", "Vietnam Airlines", "HAN", "SGN", date, "06:30", "08:45", "Economy", 1500000, "2h 15m");
            vn202.setBooked(bookedTickets.contains(vn202.getBookingKey()));
            flights.add(vn202);
            
            FlightTicket vj102 = new FlightTicket("VJ102", "VietJet Air", "HAN", "SGN", date, "08:00", "10:15", "Economy", 1200000, "2h 15m");
            vj102.setBooked(bookedTickets.contains(vj102.getBookingKey()));
            flights.add(vj102);
            
            FlightTicket bl202 = new FlightTicket("BL202", "Bamboo Airways", "HAN", "SGN", date, "10:30", "12:45", "Economy", 1300000, "2h 15m");
            bl202.setBooked(bookedTickets.contains(bl202.getBookingKey()));
            flights.add(bl202);
            
            FlightTicket vn212 = new FlightTicket("VN212", "Vietnam Airlines", "HAN", "SGN", date, "15:00", "17:15", "Business", 3500000, "2h 15m");
            vn212.setBooked(bookedTickets.contains(vn212.getBookingKey()));
            flights.add(vn212);
        }
        
        // SGN to DAD
        if (departureCode.equals("SGN") && destinationCode.equals("DAD")) {
            FlightTicket vn301 = new FlightTicket("VN301", "Vietnam Airlines", "SGN", "DAD", date, "07:00", "08:20", "Economy", 900000, "1h 20m");
            vn301.setBooked(bookedTickets.contains(vn301.getBookingKey()));
            flights.add(vn301);
            
            FlightTicket vj201 = new FlightTicket("VJ201", "VietJet Air", "SGN", "DAD", date, "09:30", "10:50", "Economy", 700000, "1h 20m");
            vj201.setBooked(bookedTickets.contains(vj201.getBookingKey()));
            flights.add(vj201);
            
            FlightTicket bl301 = new FlightTicket("BL301", "Bamboo Airways", "SGN", "DAD", date, "13:00", "14:20", "Economy", 800000, "1h 20m");
            bl301.setBooked(bookedTickets.contains(bl301.getBookingKey()));
            flights.add(bl301);
        }
        
        // SGN to PQC
        if (departureCode.equals("SGN") && destinationCode.equals("PQC")) {
            FlightTicket vn401 = new FlightTicket("VN401", "Vietnam Airlines", "SGN", "PQC", date, "08:00", "09:00", "Economy", 1100000, "1h");
            vn401.setBooked(bookedTickets.contains(vn401.getBookingKey()));
            flights.add(vn401);
            
            FlightTicket vj301 = new FlightTicket("VJ301", "VietJet Air", "SGN", "PQC", date, "10:30", "11:30", "Economy", 900000, "1h");
            vj301.setBooked(bookedTickets.contains(vj301.getBookingKey()));
            flights.add(vj301);
            
            FlightTicket bl401 = new FlightTicket("BL401", "Bamboo Airways", "SGN", "PQC", date, "14:00", "15:00", "Economy", 1000000, "1h");
            bl401.setBooked(bookedTickets.contains(bl401.getBookingKey()));
            flights.add(bl401);
        }
        
        // HAN to DAD
        if (departureCode.equals("HAN") && destinationCode.equals("DAD")) {
            FlightTicket vn501 = new FlightTicket("VN501", "Vietnam Airlines", "HAN", "DAD", date, "07:30", "09:00", "Economy", 1000000, "1h 30m");
            vn501.setBooked(bookedTickets.contains(vn501.getBookingKey()));
            flights.add(vn501);
            
            FlightTicket vj401 = new FlightTicket("VJ401", "VietJet Air", "HAN", "DAD", date, "11:00", "12:30", "Economy", 800000, "1h 30m");
            vj401.setBooked(bookedTickets.contains(vj401.getBookingKey()));
            flights.add(vj401);
            
            FlightTicket bl501 = new FlightTicket("BL501", "Bamboo Airways", "HAN", "DAD", date, "15:30", "17:00", "Economy", 900000, "1h 30m");
            bl501.setBooked(bookedTickets.contains(bl501.getBookingKey()));
            flights.add(bl501);
        }
        
        return flights;
    }

    public static String getAirlineLogo(String airline) {
        if (airline.contains("Vietnam Airlines")) {
            return "VN";
        } else if (airline.contains("VietJet")) {
            return "VJ";
        } else if (airline.contains("Bamboo")) {
            return "BB";
        }
        return "??";
    }

    // Get set of booked tickets from SharedPreferences
    private static Set<String> getBookedTickets() {
        if (appContext == null) {
            return new HashSet<>();
        }
        SharedPreferences prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getStringSet(KEY_BOOKED_TICKETS, new HashSet<>());
    }

    // Book a ticket - save complete booking information
    public static void bookTicket(String flightCode, String airline, String departure, String destination,
                                 String departureDate, String departureTime, String arrivalTime,
                                 String seatClass, long price, String duration,
                                 String passengerName, String passengerID, String passengerPhone,
                                 String passengerEmail) {
        if (appContext == null) {
            return;
        }
        
        // Generate booking ID
        String bookingId = "BK" + System.currentTimeMillis();
        String bookingTime = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
                .format(new java.util.Date());
        
        // Create booked ticket
        BookedTicket ticket = new BookedTicket(bookingId, flightCode, airline, departure, destination,
                departureDate, departureTime, arrivalTime, seatClass, price, duration,
                passengerName, passengerID, passengerPhone, passengerEmail, bookingTime);
        
        // Save to booked tickets list
        SharedPreferences prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String bookedTicketsJson = prefs.getString("booked_tickets_data", "[]");
        
        try {
            JSONArray ticketsArray = new JSONArray(bookedTicketsJson);
            ticketsArray.put(ticket.toJSON());
            prefs.edit().putString("booked_tickets_data", ticketsArray.toString()).apply();
            
            // Also add to simple set for quick lookup
            Set<String> bookedKeys = new HashSet<>(getBookedTickets());
            bookedKeys.add(flightCode + "_" + departureDate);
            prefs.edit().putStringSet(KEY_BOOKED_TICKETS, bookedKeys).apply();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // Check if a ticket is already booked
    public static boolean isTicketBooked(String flightCode, String departureDate) {
        String bookingKey = flightCode + "_" + departureDate;
        return getBookedTickets().contains(bookingKey);
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
