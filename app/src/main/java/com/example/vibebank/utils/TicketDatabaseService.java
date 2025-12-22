package com.example.vibebank.utils;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Unified service for managing all types of tickets in Firebase Firestore
 * Supports: Flight Tickets, Movie Tickets, Hotel Bookings
 */
public class TicketDatabaseService {
    
    private static final String TAG = "TicketDatabaseService";
    private static final String COLLECTION_FLIGHT_TICKETS = "booked_flight_tickets";
    private static final String COLLECTION_MOVIE_TICKETS = "booked_movie_tickets";
    private static final String COLLECTION_HOTEL_BOOKINGS = "booked_hotel_bookings";
    
    private static FirebaseFirestore firestore;
    private static FirebaseAuth auth;
    
    public static void init() {
        if (firestore == null) {
            firestore = FirebaseFirestore.getInstance();
            auth = FirebaseAuth.getInstance();
        }
    }
    
    // ============ FLIGHT TICKETS ============
    
    public static void saveFlightTicket(Map<String, Object> ticketData, OnSaveListener listener) {
        Log.d(TAG, "========== SAVE FLIGHT TICKET ==========");
        Log.d(TAG, "Data received: " + ticketData.toString());
        
        init();
        if (auth.getCurrentUser() == null) {
            Log.e(TAG, "✗✗✗ User not authenticated!");
            if (listener != null) listener.onError("User not authenticated");
            return;
        }
        
        Log.d(TAG, "✓ User authenticated: " + auth.getCurrentUser().getUid());
        
        ticketData.put("userId", auth.getCurrentUser().getUid());
        ticketData.put("timestamp", System.currentTimeMillis());
        ticketData.put("type", "flight");
        
        String bookingId = (String) ticketData.get("bookingId");
        if (bookingId == null) {
            bookingId = "BK" + System.currentTimeMillis();
            ticketData.put("bookingId", bookingId);
        }
        
        final String finalBookingId = bookingId;
        Log.d(TAG, "Saving to Firestore with ID: " + finalBookingId);
        
        firestore.collection(COLLECTION_FLIGHT_TICKETS)
                .document(finalBookingId)
                .set(ticketData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✓✓✓ Flight ticket saved successfully: " + finalBookingId);
                    if (listener != null) listener.onSuccess(finalBookingId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "✗✗✗ Error saving flight ticket: " + e.getMessage(), e);
                    if (listener != null) listener.onError(e.getMessage());
                });
    }
    
    public static void loadFlightTickets(OnLoadListener listener) {
        Log.d(TAG, "========== LOAD FLIGHT TICKETS ==========");
        init();
        if (auth.getCurrentUser() == null) {
            Log.e(TAG, "✗✗✗ User not authenticated!");
            if (listener != null) listener.onLoaded(new ArrayList<>());
            return;
        }
        
        String userId = auth.getCurrentUser().getUid();
        Log.d(TAG, "Loading tickets for user: " + userId);
        
        firestore.collection(COLLECTION_FLIGHT_TICKETS)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Map<String, Object>> tickets = new ArrayList<>();
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        tickets.add(document.getData());
                    }
                    // Sort by timestamp in memory (newest first)
                    tickets.sort((a, b) -> {
                        Long t1 = (Long) a.getOrDefault("timestamp", 0L);
                        Long t2 = (Long) b.getOrDefault("timestamp", 0L);
                        return t2.compareTo(t1);
                    });
                    Log.d(TAG, "✓✓✓ Loaded " + tickets.size() + " flight tickets");
                    if (listener != null) listener.onLoaded(tickets);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "✗✗✗ Error loading flight tickets: " + e.getMessage(), e);
                    if (listener != null) listener.onLoaded(new ArrayList<>());
                });
    }
    
    public static void checkFlightTicketBooked(String flightCode, String departureDate, OnCheckListener listener) {
        init();
        if (auth.getCurrentUser() == null) {
            if (listener != null) listener.onChecked(false);
            return;
        }
        
        String userId = auth.getCurrentUser().getUid();
        firestore.collection(COLLECTION_FLIGHT_TICKETS)
                .whereEqualTo("userId", userId)
                .whereEqualTo("flightCode", flightCode)
                .whereEqualTo("departureDate", departureDate)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (listener != null) listener.onChecked(!querySnapshot.isEmpty());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking flight ticket", e);
                    if (listener != null) listener.onChecked(false);
                });
    }
    
    // ============ MOVIE TICKETS ============
    
    public static void saveMovieTicket(Map<String, Object> ticketData, OnSaveListener listener) {
        init();
        if (auth.getCurrentUser() == null) {
            if (listener != null) listener.onError("User not authenticated");
            return;
        }
        
        ticketData.put("userId", auth.getCurrentUser().getUid());
        ticketData.put("timestamp", System.currentTimeMillis());
        ticketData.put("type", "movie");
        
        String bookingId = (String) ticketData.get("bookingId");
        if (bookingId == null) {
            bookingId = "MV" + System.currentTimeMillis();
            ticketData.put("bookingId", bookingId);
        }
        
        final String finalBookingId = bookingId;
        firestore.collection(COLLECTION_MOVIE_TICKETS)
                .document(finalBookingId)
                .set(ticketData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Movie ticket saved: " + finalBookingId);
                    if (listener != null) listener.onSuccess(finalBookingId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving movie ticket", e);
                    if (listener != null) listener.onError(e.getMessage());
                });
    }
    
    public static void loadMovieTickets(OnLoadListener listener) {
        init();
        if (auth.getCurrentUser() == null) {
            if (listener != null) listener.onLoaded(new ArrayList<>());
            return;
        }
        
        String userId = auth.getCurrentUser().getUid();
        firestore.collection(COLLECTION_MOVIE_TICKETS)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Map<String, Object>> tickets = new ArrayList<>();
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        tickets.add(document.getData());
                    }
                    // Sort by timestamp in memory (newest first)
                    tickets.sort((a, b) -> {
                        Long t1 = (Long) a.getOrDefault("timestamp", 0L);
                        Long t2 = (Long) b.getOrDefault("timestamp", 0L);
                        return t2.compareTo(t1);
                    });
                    Log.d(TAG, "Loaded " + tickets.size() + " movie tickets");
                    if (listener != null) listener.onLoaded(tickets);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading movie tickets", e);
                    if (listener != null) listener.onLoaded(new ArrayList<>());
                });
    }
    
    public static void checkMovieSeatsBooked(String showtimeKey, OnLoadListener listener) {
        init();
        if (auth.getCurrentUser() == null) {
            if (listener != null) listener.onLoaded(new ArrayList<>());
            return;
        }
        
        firestore.collection(COLLECTION_MOVIE_TICKETS)
                .whereEqualTo("showtimeKey", showtimeKey)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Map<String, Object>> tickets = new ArrayList<>();
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        tickets.add(document.getData());
                    }
                    if (listener != null) listener.onLoaded(tickets);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking movie seats", e);
                    if (listener != null) listener.onLoaded(new ArrayList<>());
                });
    }
    
    // ============ HOTEL BOOKINGS ============
    
    public static void saveHotelBooking(Map<String, Object> bookingData, OnSaveListener listener) {
        init();
        if (auth.getCurrentUser() == null) {
            Log.e(TAG, "Cannot save hotel booking: User not authenticated");
            if (listener != null) listener.onError("User not authenticated");
            return;
        }
        
        String userId = auth.getCurrentUser().getUid();
        Log.d(TAG, "Saving hotel booking for user: " + userId);
        
        bookingData.put("userId", auth.getCurrentUser().getUid());
        bookingData.put("timestamp", System.currentTimeMillis());
        bookingData.put("type", "hotel");
        
        String bookingId = (String) bookingData.get("bookingId");
        if (bookingId == null) {
            bookingId = "HT" + System.currentTimeMillis();
            bookingData.put("bookingId", bookingId);
        }
        
        final String finalBookingId = bookingId;
        Log.d(TAG, "Writing to Firestore collection: " + COLLECTION_HOTEL_BOOKINGS);
        Log.d(TAG, "Document ID: " + finalBookingId);
        
        firestore.collection(COLLECTION_HOTEL_BOOKINGS)
                .document(finalBookingId)
                .set(bookingData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✓✓✓ Hotel booking saved successfully: " + finalBookingId);
                    if (listener != null) listener.onSuccess(finalBookingId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "✗✗✗ Error saving hotel booking: " + e.getMessage(), e);
                    if (listener != null) listener.onError(e.getMessage());
                });
    }
    
    public static void loadHotelBookings(OnLoadListener listener) {
        init();
        if (auth.getCurrentUser() == null) {
            Log.e(TAG, "Cannot load hotel bookings: User not authenticated");
            if (listener != null) listener.onLoaded(new ArrayList<>());
            return;
        }
        
        String userId = auth.getCurrentUser().getUid();
        Log.d(TAG, "Loading hotel bookings for user: " + userId);
        Log.d(TAG, "Querying collection: " + COLLECTION_HOTEL_BOOKINGS);
        
        firestore.collection(COLLECTION_HOTEL_BOOKINGS)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Map<String, Object>> bookings = new ArrayList<>();
                    Log.d(TAG, "Query returned " + querySnapshot.size() + " documents");
                    
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        Log.d(TAG, "Document: " + document.getId());
                        bookings.add(document.getData());
                    }
                    // Sort by timestamp in memory (newest first)
                    bookings.sort((a, b) -> {
                        Long t1 = (Long) a.getOrDefault("timestamp", 0L);
                        Long t2 = (Long) b.getOrDefault("timestamp", 0L);
                        return t2.compareTo(t1);
                    });
                    Log.d(TAG, "✓✓✓ Successfully loaded " + bookings.size() + " hotel bookings");
                    if (listener != null) listener.onLoaded(bookings);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "✗✗✗ Error loading hotel bookings: " + e.getMessage(), e);
                    if (listener != null) listener.onLoaded(new ArrayList<>());
                });
    }
    
    // ============ UNIFIED METHODS ============
    
    /**
     * Load all tickets (flight, movie, hotel) for the current user
     */
    public static void loadAllTickets(OnLoadAllTicketsListener listener) {
        init();
        if (auth.getCurrentUser() == null) {
            if (listener != null) listener.onAllLoaded(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
            return;
        }
        
        final List<Map<String, Object>> flightTickets = new ArrayList<>();
        final List<Map<String, Object>> movieTickets = new ArrayList<>();
        final List<Map<String, Object>> hotelBookings = new ArrayList<>();
        final int[] completedCount = {0};
        
        Runnable checkComplete = () -> {
            if (completedCount[0] == 3) {
                if (listener != null) {
                    listener.onAllLoaded(flightTickets, movieTickets, hotelBookings);
                }
            }
        };
        
        loadFlightTickets(tickets -> {
            flightTickets.addAll(tickets);
            completedCount[0]++;
            checkComplete.run();
        });
        
        loadMovieTickets(tickets -> {
            movieTickets.addAll(tickets);
            completedCount[0]++;
            checkComplete.run();
        });
        
        loadHotelBookings(bookings -> {
            hotelBookings.addAll(bookings);
            completedCount[0]++;
            checkComplete.run();
        });
    }
    
    // ============ CALLBACK INTERFACES ============
    
    public interface OnSaveListener {
        void onSuccess(String bookingId);
        void onError(String error);
    }
    
    public interface OnLoadListener {
        void onLoaded(List<Map<String, Object>> data);
    }
    
    public interface OnCheckListener {
        void onChecked(boolean result);
    }
    
    public interface OnLoadAllTicketsListener {
        void onAllLoaded(
            List<Map<String, Object>> flightTickets,
            List<Map<String, Object>> movieTickets,
            List<Map<String, Object>> hotelBookings
        );
    }
}
