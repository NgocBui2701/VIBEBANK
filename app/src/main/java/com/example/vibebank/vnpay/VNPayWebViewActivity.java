package com.example.vibebank.vnpay;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.vibebank.R;

import java.util.HashMap;
import java.util.Map;

/**
 * WebView Activity for VNPay Payment
 * Handles payment in-app without opening browser
 */
public class VNPayWebViewActivity extends AppCompatActivity {
    
    private static final String TAG = "VNPayWebViewActivity";
    public static final String EXTRA_PAYMENT_URL = "payment_url";
    
    private WebView webView;
    private ProgressBar progressBar;
    private TextView tvTitle;
    private ImageView btnBack;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vnpay_webview);
        
        initViews();
        setupWebView();
        loadPaymentUrl();
    }
    
    private void initViews() {
        webView = findViewById(R.id.webViewVNPay);
        progressBar = findViewById(R.id.progressBarVNPay);
        tvTitle = findViewById(R.id.tvVNPayWebViewTitle);
        btnBack = findViewById(R.id.btnVNPayWebViewBack);
        
        btnBack.setOnClickListener(v -> onBackPressed());
    }
    
    private void setupWebView() {
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setBuiltInZoomControls(false);
        webView.getSettings().setSupportZoom(false);
        
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                progressBar.setVisibility(View.VISIBLE);
                Log.d(TAG, "Loading URL: " + url);
                
                // Check if this is the return URL
                if (url.startsWith("vibebank://vnpay-return")) {
                    Log.d(TAG, "✓ Detected return URL, processing payment result");
                    handlePaymentReturn(url);
                    return;
                }
            }
            
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                progressBar.setVisibility(View.GONE);
            }
            
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                Log.d(TAG, "shouldOverrideUrlLoading: " + url);
                
                // Intercept return URL
                if (url.startsWith("vibebank://vnpay-return")) {
                    Log.d(TAG, "✓ Intercepted return URL");
                    handlePaymentReturn(url);
                    return true;
                }
                
                // Load other URLs normally in WebView
                return false;
            }
        });
    }
    
    private void loadPaymentUrl() {
        String paymentUrl = getIntent().getStringExtra(EXTRA_PAYMENT_URL);
        
        if (paymentUrl == null || paymentUrl.isEmpty()) {
            Log.e(TAG, "✗ No payment URL provided");
            finish();
            return;
        }
        
        Log.d(TAG, "✓ Loading payment URL in WebView");
        webView.loadUrl(paymentUrl);
    }
    
    private void handlePaymentReturn(String returnUrl) {
        try {
            Log.d(TAG, "📥 Handling payment return: " + returnUrl);
            
            // Parse return URL using VNPayHelper (KHÔNG decode values)
            Map<String, String> params = VNPayHelper.parseCallbackUrl(returnUrl);
            
            if (params.isEmpty()) {
                Log.e(TAG, "✗ No parameters found in return URL");
                setResult(RESULT_CANCELED);
                finish();
                return;
            }
            
            // Verify signature
            if (!VNPayHelper.verifySignature(params)) {
                Log.e(TAG, "✗ Invalid signature");
                setResult(RESULT_CANCELED);
                finish();
                return;
            }
            
            // Get response code
            String responseCode = params.get("vnp_ResponseCode");
            String transactionStatus = params.get("vnp_TransactionStatus");
            
            Log.d(TAG, "Response Code: " + responseCode);
            Log.d(TAG, "Transaction Status: " + transactionStatus);
            
            // Check if payment successful
            if ("00".equals(responseCode) && "00".equals(transactionStatus)) {
                Log.d(TAG, "✓✓✓ Payment successful, preparing result intent");
                
                String txnRef = params.get("vnp_TxnRef");
                String amount = params.get("vnp_Amount");
                String bankCode = params.get("vnp_BankCode");
                String transactionNo = params.get("vnp_TransactionNo");
                String payDate = params.get("vnp_PayDate");
                
                Log.d(TAG, "TxnRef: " + txnRef);
                Log.d(TAG, "Amount: " + amount);
                Log.d(TAG, "BankCode: " + bankCode);
                
                // Create result intent with payment data
                Intent resultIntent = new Intent();
                resultIntent.putExtra("responseCode", responseCode);
                resultIntent.putExtra("transactionStatus", transactionStatus);
                resultIntent.putExtra("txnRef", txnRef);
                resultIntent.putExtra("amount", amount);
                resultIntent.putExtra("bankCode", bankCode);
                resultIntent.putExtra("transactionNo", transactionNo);
                resultIntent.putExtra("payDate", payDate);
                
                Log.d(TAG, "✓ Setting RESULT_OK and finishing");
                setResult(RESULT_OK, resultIntent);
            } else {
                Log.e(TAG, "✗ Payment failed: ResponseCode=" + responseCode + ", Status=" + transactionStatus);
                
                Intent resultIntent = new Intent();
                resultIntent.putExtra("responseCode", responseCode);
                resultIntent.putExtra("transactionStatus", transactionStatus);
                
                setResult(RESULT_CANCELED, resultIntent);
            }
            
            finish();
            
        } catch (Exception e) {
            Log.e(TAG, "✗ Error handling payment return: " + e.getMessage());
            setResult(RESULT_CANCELED);
            finish();
        }
    }
    
    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
