# Triển khai miễn phí: Aiven MySQL + Render

Dự án đã được chuẩn bị để dùng Aiven làm cơ sở dữ liệu MySQL, còn Render chạy giao diện React và API Spring Boot bằng gói miễn phí.

## 1. Tạo cơ sở dữ liệu trên Aiven

1. Tạo tài khoản Aiven, sau đó tạo dịch vụ **Aiven for MySQL** với gói **Free**.
2. Trong trang **Overview** của dịch vụ, tại phần **Connection information**, sao chép các thông tin sau:
   - Host
   - Port
   - User
   - Password
   - Database name
3. Nếu muốn dùng cơ sở dữ liệu tên `tea_attendance`, hãy tạo trước trong mục **Connect → Databases**. Không nên để ứng dụng tự tạo cơ sở dữ liệu khi triển khai.

## 2. Triển khai bằng Render Blueprint

Đẩy mã nguồn lên GitHub. Trong Render, chọn **New → Blueprint** rồi chọn repository đó. File `render.yaml` ở thư mục gốc sẽ tạo hai dịch vụ miễn phí:

- `tra-da-api-free`: backend Spring Boot.
- `tra-da-web-free`: giao diện React.

Khi Render yêu cầu nhập biến môi trường cho backend, điền các giá trị lấy từ Aiven:

| Biến trên Render | Giá trị lấy từ Aiven |
| --- | --- |
| `DB_HOST` | Host |
| `DB_PORT` | Port (không mặc định là 3306) |
| `DB_NAME` | Database name |
| `DB_USERNAME` | User |
| `DB_PASSWORD` | Password |
| `CORS_ALLOWED_ORIGINS` | Đường dẫn giao diện trên Render, ví dụ `https://tra-da-web-free.onrender.com` |

Với biến `VITE_API_URL` của frontend, nhập đường dẫn backend kèm `/api`, ví dụ:

```text
https://tra-da-api-free.onrender.com/api
```

Không thêm dấu `/` ở cuối `CORS_ALLOWED_ORIGINS`.

Cấu hình Render đã đặt sẵn `DB_SSL_MODE=REQUIRED`, nên kết nối từ backend đến Aiven sẽ được mã hóa. Mật khẩu cơ sở dữ liệu không được lưu trong Git.

## Giới hạn của gói miễn phí

- Aiven Free MySQL có 1 GB dung lượng, 1 GB RAM, mỗi tổ chức chỉ có một dịch vụ miễn phí và có thể tự tắt khi không hoạt động lâu.
- Backend Render Free sẽ ngủ sau 15 phút không có truy cập. Lần truy cập đầu tiên sau khi ngủ có thể mất khoảng một phút.
- Render Free không có ổ đĩa lưu trữ cố định. Ảnh gửi trong chat có thể mất khi backend khởi động lại, deploy lại hoặc ngủ. Tin nhắn chữ, điểm danh và các dữ liệu khác vẫn nằm trong MySQL trên Aiven.
- Chức năng tổng kết tự động hằng tuần có thể chạy chậm khi backend đang ngủ. Nên mở ứng dụng gần thời gian tổng kết đã đặt, hoặc nâng cấp backend sau này nếu cần chạy đúng giờ tuyệt đối.

Nếu sau này cần MySQL và ảnh được lưu ổn định ngay trên Render, dùng `render-paid.yaml` thay cho `render.yaml` ở thư mục gốc.
