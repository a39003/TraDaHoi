# Trà Đá Hội – React frontend

Giao diện React/Vite thuần cho nhóm điểm danh uống trà đá: điểm danh và gọi nước theo ngày, thống kê tuần, tự cân đối khoản cần hoàn, QR chuyển khoản và chat nhóm.

## Chạy giao diện

```bash
npm install
npm run dev
```

Mặc định ứng dụng chạy với dữ liệu minh hoạ cục bộ để có thể xem toàn bộ luồng ngay cả khi API Spring Boot chưa khởi động.

## Kết nối Spring Boot

Đặt `VITE_API_URL=http://localhost:8081/api` (đây cũng là giá trị mặc định) và nối các action trong `src/App.jsx` với `src/api.js`.

API adapter đã sẵn các endpoint dự kiến:

- `GET /members`
- `GET /attendances/daily-summary`, `POST /attendances`, `DELETE /expenses/{id}`
- `GET /weeks/{weekStart}/settlement`, `POST /weeks/{weekStart}/settlement/calculate`
- `GET /chat/messages`, `POST /chat/messages`

Các thông tin ngân hàng trong bản demo là dữ liệu mẫu, cần được lấy từ hồ sơ thành viên thật trước khi triển khai.
