package com.example.vibebank.utils;

import android.content.Context;
import com.cloudinary.android.MediaManager;
import java.util.HashMap;
import java.util.Map;

public class CloudinaryHelper {

    private static final String CLOUD_NAME = "dfsgrcuya";

    public static void initCloudinary(Context context) {
        try {
            MediaManager.get();
        } catch (IllegalStateException e) {
            Map<String, String> config = new HashMap<>();
            config.put("cloud_name", CLOUD_NAME);
            config.put("secure", "true");
            MediaManager.init(context, config);
        }
    }
}