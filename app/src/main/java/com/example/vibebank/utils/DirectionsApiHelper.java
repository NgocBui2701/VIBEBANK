package com.example.vibebank.utils;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DirectionsApiHelper {
    private static final String TAG = "DirectionsApiHelper";
    private static final String API_KEY = "AIzaSyAOVYRIgupAurZup5y1PRh8Ismb1A3lLao"; // Same as in AndroidManifest
    private static final String DIRECTIONS_API_URL = "https://maps.googleapis.com/maps/api/directions/json";

    private final ExecutorService executorService;
    private final Handler mainHandler;

    public interface DirectionsCallback {
        void onDirectionsReceived(List<LatLng> polylinePoints, String distance, String duration);
        void onError(String error);
    }

    public DirectionsApiHelper() {
        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
    }

    public void getDirections(LatLng origin, LatLng destination, DirectionsCallback callback) {
        executorService.execute(() -> {
            try {
                String urlString = DIRECTIONS_API_URL +
                        "?origin=" + origin.latitude + "," + origin.longitude +
                        "&destination=" + destination.latitude + "," + destination.longitude +
                        "&mode=driving" +
                        "&key=" + API_KEY;

                Log.d(TAG, "Requesting directions: " + urlString);

                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.connect();

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    parseDirectionsResponse(response.toString(), callback);
                } else {
                    String error = "HTTP Error: " + responseCode;
                    Log.e(TAG, error);
                    mainHandler.post(() -> callback.onError(error));
                }

                connection.disconnect();

            } catch (Exception e) {
                Log.e(TAG, "Error fetching directions", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    private void parseDirectionsResponse(String jsonResponse, DirectionsCallback callback) {
        try {
            JSONObject json = new JSONObject(jsonResponse);
            String status = json.getString("status");

            if ("OK".equals(status)) {
                JSONArray routes = json.getJSONArray("routes");
                if (routes.length() > 0) {
                    JSONObject route = routes.getJSONObject(0);
                    
                    // Get overview polyline
                    JSONObject overviewPolyline = route.getJSONObject("overview_polyline");
                    String encodedPolyline = overviewPolyline.getString("points");
                    List<LatLng> polylinePoints = decodePolyline(encodedPolyline);

                    // Get distance and duration
                    JSONArray legs = route.getJSONArray("legs");
                    if (legs.length() > 0) {
                        JSONObject leg = legs.getJSONObject(0);
                        JSONObject distanceObj = leg.getJSONObject("distance");
                        JSONObject durationObj = leg.getJSONObject("duration");

                        String distance = distanceObj.getString("text");
                        String duration = durationObj.getString("text");

                        Log.d(TAG, "Directions received: " + distance + ", " + duration);

                        mainHandler.post(() -> callback.onDirectionsReceived(polylinePoints, distance, duration));
                    }
                }
            } else {
                String error = "Directions API status: " + status;
                Log.e(TAG, error);
                mainHandler.post(() -> callback.onError(error));
            }

        } catch (Exception e) {
            Log.e(TAG, "Error parsing directions response", e);
            mainHandler.post(() -> callback.onError(e.getMessage()));
        }
    }

    // Decode polyline encoded string to List<LatLng>
    // Source: https://developers.google.com/maps/documentation/utilities/polylinealgorithm
    private List<LatLng> decodePolyline(String encoded) {
        List<LatLng> polyline = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)), (((double) lng / 1E5)));
            polyline.add(p);
        }

        return polyline;
    }

    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}

