package com.example.vibebank;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.vibebank.vnpay.VNPayHelper;
import com.example.vibebank.vnpay.VNPayWebViewActivity;

/**
 * Test Activity for VNPay Integration
 */
public class VNPayTestActivity extends AppCompatActivity {
    
    private static final String TAG = "VNPayTestActivity";
    private static final int VNPAY_REQUEST_CODE = 1001;
    
    private ImageView btnBack;
    private EditText edtAmount;
    private EditText edtOrderInfo;
    private Button btnPayWithVNPay;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vnpay_test);
        
        btnBack = findViewById(R.id.btnBack);
        edtAmount = findViewById(R.id.edtTestAmount);
        edtOrderInfo = findViewById(R.id.edtTestOrderInfo);
        btnPayWithVNPay = findViewById(R.id.btnTestPayVNPay);
        
        btnBack.setOnClickListener(v -> finish());
        btnPayWithVNPay.setOnClickListener(v -> initiateVNPayPayment());
    }
    
    private void initiateVNPayPayment() {
        // Get amount
        String amountStr = edtAmount.getText().toString().trim();
        if (amountStr.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập số tiền", Toast.LENGTH_SHORT).show();
            return;
        }
        
        long amount;
        try {
            amount = Long.parseLong(amountStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Số tiền không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (amount < 10000) {
            Toast.makeText(this, "Số tiền tối thiểu 10,000 VND", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Get order info
        String orderInfo = edtOrderInfo.getText().toString().trim();
        if (orderInfo.isEmpty()) {
            orderInfo = "Test thanh toan VIBEBANK";
        }
        
        // Remove Vietnamese accents
        orderInfo = VNPayHelper.removeAccents(orderInfo);
        
        // Generate unique transaction reference
        String txnRef = "TEST" + System.currentTimeMillis();
        
        // Get user IP (simplified - using localhost for test)
        String ipAddr = "127.0.0.1";
        
        // Build payment URL
        String paymentUrl = VNPayHelper.buildPaymentUrl(txnRef, amount, orderInfo, ipAddr);
        
        if (paymentUrl == null) {
            Toast.makeText(this, "Lỗi tạo URL thanh toán", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Log.d(TAG, "✓ Payment URL created, opening in WebView");
        
        // Open payment URL in WebView
        Intent intent = new Intent(this, VNPayWebViewActivity.class);
        intent.putExtra(VNPayWebViewActivity.EXTRA_PAYMENT_URL, paymentUrl);
        startActivityForResult(intent, VNPAY_REQUEST_CODE);
        
        Toast.makeText(this, "Đang chuyển đến VNPay...", Toast.LENGTH_SHORT).show();
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == VNPAY_REQUEST_CODE) {
            if (resultCode == RESULT_OK && data != null) {
                String responseCode = data.getStringExtra("responseCode");
                String txnRef = data.getStringExtra("txnRef");
                String amount = data.getStringExtra("amount");
                
                Toast.makeText(this, "✓ Thanh toán thành công\nMã GD: " + txnRef, Toast.LENGTH_LONG).show();
                Log.d(TAG, "✓✓✓ Payment successful: " + txnRef);
                
            } else if (data != null) {
                String responseCode = data.getStringExtra("responseCode");
                Toast.makeText(this, "✗ Thanh toán thất bại\nMã lỗi: " + responseCode, Toast.LENGTH_LONG).show();
                Log.e(TAG, "✗ Payment failed: " + responseCode);
                
            } else {
                Toast.makeText(this, "Đã hủy thanh toán", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
