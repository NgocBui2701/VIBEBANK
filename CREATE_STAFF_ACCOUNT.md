# Hướng dẫn tạo tài khoản nhân viên (Staff Account)

## Cách 1: Tạo thủ công trên Firebase Console

1. Mở Firebase Console → Authentication → Users
2. Add user với email: `staff@vibebank.com` / password: `Staff@123456`
3. Copy UID của user vừa tạo

4. Mở Firestore Database → Collection `users`
5. Tạo document với ID = UID vừa copy:
```json
{
  "full_name": "Nhân viên Ngân hàng",
  "phone_number": "0900000001",
  "email": "staff@vibebank.com",
  "role": "staff",
  "created_at": [Timestamp hiện tại],
  "avatar_url": "",
  "address": "VIBEBANK - Chi nhánh Đông Du"
}
```

6. Tạo document trong collection `accounts` với ID = UID:
```json
{
  "account_number": "0900000001",
  "account_type": "staff",
  "balance": 0,
  "created_at": [Timestamp hiện tại]
}
```

## Cách 2: Đăng ký thông thường rồi đổi role

1. Đăng ký tài khoản mới qua app (email bất kỳ)
2. Vào Firestore → Collection `users` → Tìm user vừa tạo
3. Edit document, thêm/sửa field:
   - `role`: `"staff"` (thay vì `"customer"`)

## Đăng nhập

- **Email**: `staff@vibebank.com`
- **Password**: `Staff@123456`

Sau khi đăng nhập thành công, hệ thống sẽ tự động chuyển đến **StaffHomeActivity** thay vì HomeActivity thông thường.

## Phân biệt role

- `role: "customer"` → HomeActivity (giao diện khách hàng)
- `role: "staff"` → StaffHomeActivity (giao diện nhân viên)

