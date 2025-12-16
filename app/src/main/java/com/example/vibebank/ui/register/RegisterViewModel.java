package com.example.vibebank.ui.register;

import androidx.lifecycle.ViewModel;
import com.example.vibebank.data.manager.RegisterDataManager;

public class RegisterViewModel extends ViewModel {
    private final RegisterDataManager dataManager;

    public RegisterViewModel() {
        this.dataManager = RegisterDataManager.getInstance();
    }

    public void saveStep1(String email, String phone, String cccd) {
        dataManager.setEmail(email);
        dataManager.setPhone(phone);
        dataManager.setCccd(cccd);
    }

    public void saveStep2(String fullName, String birthDate, String gender, String address, String issueDate) {
        dataManager.setFullName(fullName);
        dataManager.setBirthDate(birthDate);
        dataManager.setGender(gender);
        dataManager.setAddress(address);
        dataManager.setIssueDate(issueDate);
    }

    public void saveStep3(String frontPath, String backPath) {
        dataManager.setFrontIdCardPath(frontPath);
        dataManager.setBackIdCardPath(backPath);
    }

    public String getFrontImage() { return dataManager.getFrontIdCardPath(); }
    public String getBackImage() { return dataManager.getBackIdCardPath(); }
}