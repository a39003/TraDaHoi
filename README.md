# Trà đá tuần này

Ứng dụng nội bộ để cả nhóm điểm danh uống trà đá, ghi món và chi phí, chốt sổ mỗi thứ Bảy, hiển thị mã QR chuyển khoản cho người đã ứng tiền, và trao đổi trong chat chung.

## Công nghệ

- Frontend: React + Vite
- Backend: Java Spring Boot
- Cơ sở dữ liệu: MySQL 8

## Chạy dự án

1. Khởi động MySQL:

   ```bash
   docker compose up -d mysql
   ```

2. Chạy backend:

   ```bash
   cd backend
   mvn spring-boot:run
   ```

3. Chạy frontend trong một terminal khác:

   ```bash
   cd frontend
   npm install
   npm run dev
   ```

Mở địa chỉ Vite hiển thị trong terminal (thường là `http://localhost:5173`). API Spring Boot chạy tại `http://localhost:8081/api`.

## Cấu hình

Backend mặc định kết nối tới MySQL tại `localhost:3307`, database `tea_attendance`, người dùng `tra_da_app`, mật khẩu `tra_da_secret`. Các giá trị này có thể thay đổi qua biến môi trường `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, và `DB_PASSWORD`.

Đây là cấu hình khởi tạo cho môi trường phát triển. Hãy thay các mật khẩu mặc định trước khi triển khai thật; khi đổi thông tin MySQL trong `.env`, hãy đặt cùng các biến `DB_*` trong terminal chạy backend.
