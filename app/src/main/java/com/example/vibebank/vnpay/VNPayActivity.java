package com.example.vibebank.vnpay;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.vibebank.R;

import java.util.HashMap;
import java.util.Map;

/**
 * VNPay Return Activity - Handle payment result from VNPay
 */
public class VNPayActivity extends AppCompatActivity {
    
    private static final String TAG = "VNPayActivity";
    
    private ImageView imgResult;
    private TextView tvTitle;
    private TextView tvAmount;
    private TextView tvTxnRef;
    private TextView tvBankCode;
    private TextView tvTransactionNo;
    private TextView tvPayDate;
    private TextView tvResponseCode;
    private TextView tvMessage;
    private Button btnClose;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vnpay_result);
        
        initViews();
        handlePaymentResult();
    }
    
    private void initViews() {
        imgResult = findViewById(R.id.imgVnpayResult);
        tvTitle = findViewById(R.id.tvVnpayTitle);
        tvAmount = findViewById(R.id.tvVnpayAmount);
        tvTxnRef = findViewById(R.id.tvVnpayTxnRef);
        tvBankCode = findViewById(R.id.tvVnpayBankCode);
        tvTransactionNo = findViewById(R.id.tvVnpayTransactionNo);
        tvPayDate = findViewById(R.id.tvVnpayPayDate);
        tvResponseCode = findViewById(R.id.tvVnpayResponseCode);
        tvMessage = findViewById(R.id.tvVnpayMessage);
        btnClose = findViewById(R.id.btnVnpayClose);
        
        btnClose.setOnClickListener(v -> finish());
    }
    
    private void handlePaymentResult() {
        // Get return URL from intent
        Intent intent = getIntent();
        Uri data = intent.getData();
        
        if (data == null) {
            Log.e(TAG, "✗ No data received from VNPay");
            showError("Không nhận được dữ liệu từ VNPay");
            return;
        }
        
        Log.d(TAG, "VNPay return URL: " + data.toString());
        
        // Parse query parameters
        Map<String, String> params = new HashMap<>();
        for (String param : data.getQueryParameterNames()) {
            params.put(param, data.getQueryParameter(param));
            Log.d(TAG, param + " = " + data.getQueryParameter(param));
        }
        
        // Verify signature
        if (!VNPayHelper.verifySignature(params)) {
            Log.e(TAG, "✗ Invalid signature from VNPay");
            showError("Chữ ký không hợp lệ. Giao dịch có thể bị giả mạo.");
            return;
        }
        
        // Get response code
        String responseCode = params.get("vnp_ResponseCode");
        String transactionStatus = params.get("vnp_TransactionStatus");
        
        // Display result
        displayResult(params, responseCode, transactionStatus);
    }
    
    private void displayResult(Map<String, String> params, String responseCode, String transactionStatus) {
        try {
            // Get amount (divide by 100 to get VND)
            long amount = Long.parseLong(params.get("vnp_Amount")) / 100;
            String amountStr = String.format("%,d VND", amount);
            
            String txnRef = params.get("vnp_TxnRef");
            String bankCode = params.get("vnp_BankCode");
            String transactionNo = params.get("vnp_TransactionNo");
            String payDate = params.get("vnp_PayDate");
            String orderInfo = params.get("vnp_OrderInfo");
            
            // Set common info
            tvAmount.setText("Số tiền: " + amountStr);
            tvTxnRef.setText("Mã giao dịch: " + (txnRef != null ? txnRef : "N/A"));
            tvBankCode.setText("Ngân hàng: " + (bankCode != null ? bankCode : "N/A"));
            tvTransactionNo.setText("Mã GD VNPAY: " + (transactionNo != null ? transactionNo : "N/A"));
            tvPayDate.setText("Thời gian: " + formatPayDate(payDate));
            tvResponseCode.setText("Mã phản hồi: " + responseCode);
            
            // Check transaction status
            if ("00".equals(responseCode) && "00".equals(transactionStatus)) {
                // Success
                imgResult.setImageResource(R.drawable.ic_check_green);
                tvTitle.setText("THANH TOÁN THÀNH CÔNG");
                tvTitle.setTextColor(getResources().getColor(R.color.success));
                tvMessage.setText("Giao dịch của bạn đã được xử lý thành công.");
                tvMessage.setTextColor(getResources().getColor(R.color.success));
                
                Log.d(TAG, "✓✓✓ Payment successful: " + txnRef);
                
                // Set result OK to notify calling activity
                setResult(RESULT_OK);
                
            } else {
                // Failed
                imgResult.setImageResource(R.drawable.ic_close);
                tvTitle.setText("THANH TOÁN THẤT BẠI");
                tvTitle.setTextColor(getResources().getColor(R.color.error));
                
                String errorMessage = getErrorMessage(responseCode);
                tvMessage.setText(errorMessage);
                tvMessage.setTextColor(getResources().getColor(R.color.error));
                
                Log.e(TAG, "✗✗✗ Payment failed: " + txnRef + " - Code: " + responseCode);
                
                // Set result CANCELED
                setResult(RESULT_CANCELED);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "✗ Error displaying result: " + e.getMessage());
            showError("Lỗi hiển thị kết quả thanh toán");
        }
    }
    
    private void showError(String message) {
        imgResult.setImageResource(R.drawable.ic_close);
        tvTitle.setText("LỖI");
        tvTitle.setTextColor(getResources().getColor(R.color.error));
        tvMessage.setText(message);
        tvMessage.setTextColor(getResources().getColor(R.color.error));
        
        // Hide detail fields
        tvAmount.setVisibility(View.GONE);
        tvTxnRef.setVisibility(View.GONE);
        tvBankCode.setVisibility(View.GONE);
        tvTransactionNo.setVisibility(View.GONE);
        tvPayDate.setVisibility(View.GONE);
        tvResponseCode.setVisibility(View.GONE);
        
        setResult(RESULT_CANCELED);
    }
    
    private String formatPayDate(String payDate) {
        if (payDate == null || payDate.length() != 14) {
            return "N/A";
        }
        try {
            // Format: yyyyMMddHHmmss -> dd/MM/yyyy HH:mm:ss
            String year = payDate.substring(0, 4);
            String month = payDate.substring(4, 6);
            String day = payDate.substring(6, 8);
            String hour = payDate.substring(8, 10);
            String minute = payDate.substring(10, 12);
            String second = payDate.substring(12, 14);
            return day + "/" + month + "/" + year + " " + hour + ":" + minute + ":" + second;
        } catch (Exception e) {
            return payDate;
        }
    }
    
    private String getErrorMessage(String responseCode) {
        switch (responseCode) {
            case "07":
                return "Trừ tiền thành công. Giao dịch bị nghi ngờ (liên quan tới lừa đảo, giao dịch bất thường).";
            case "09":
                return "Thẻ/Tài khoản chưa đăng ký dịch vụ InternetBanking tại ngân hàng.";
            case "10":
                return "Khách hàng xác thực thông tin thẻ/tài khoản không đúng quá 3 lần.";
            case "11":
                return "Đã hết hạn chờ thanh toán. Xin vui lòng thực hiện lại giao dịch.";
            case "12":
                return "Thẻ/Tài khoản bị khóa.";
            case "13":
                return "Nhập sai mật khẩu xác thực giao dịch (OTP). Xin vui lòng thực hiện lại giao dịch.";
            case "24":
                return "Khách hàng hủy giao dịch.";
            case "51":
                return "Tài khoản không đủ số dư để thực hiện giao dịch.";
            case "65":
                return "Tài khoản đã vượt quá hạn mức giao dịch trong ngày.";
            case "75":
                return "Ngân hàng thanh toán đang bảo trì.";
            case "79":
                return "Nhập sai mật khẩu thanh toán quá số lần quy định. Xin vui lòng thực hiện lại giao dịch.";
            case "99":
                return "Lỗi không xác định.";
            default:
                return "Giao dịch thất bại. Mã lỗi: " + responseCode;
        }
    }
}
