# Triển khai dự án lên Render

Repository này đã có `render.yaml`. Sau khi đẩy code lên GitHub, trong Render chọn **New → Blueprint** và chọn repository đó.

Render sẽ tạo ba dịch vụ tại khu vực Singapore:

- `tra-da-mysql`: MySQL nội bộ, có disk tại `/var/lib/mysql`.
- `tra-da-api`: Spring Boot API, có disk tại `/var/data` để giữ ảnh chat.
- `tra-da-web`: React/Vite static site.

Trong lúc tạo Blueprint, Render sẽ yêu cầu nhập các biến `sync: false`:

| Biến | Giá trị cần nhập |
| --- | --- |
| `MYSQL_PASSWORD` | Mật khẩu mạnh cho tài khoản MySQL `tra_da_app` |
| `MYSQL_ROOT_PASSWORD` | Mật khẩu mạnh khác cho MySQL root |
| `DB_PASSWORD` | Nhập **đúng bằng** `MYSQL_PASSWORD` |
| `CORS_ALLOWED_ORIGINS` | URL frontend sau khi Render tạo, ví dụ `https://tra-da-web.onrender.com` |
| `VITE_API_URL` | URL API, ví dụ `https://tra-da-api.onrender.com/api` |

Không thêm dấu `/` ở cuối hai URL. Nếu Render thêm hậu tố vào tên dịch vụ vì trùng tên, hãy lấy đúng URL mà Render cấp và dùng lại ở hai biến tương ứng, rồi deploy lại frontend và backend.

## Lưu ý vận hành

- Không chọn gói free cho backend nếu cần lịch tự động chốt tuần. Gói free có thể ngủ khi không có truy cập, làm lịch chạy không đúng giờ.
- Disk MySQL và disk ảnh chat là dữ liệu thật. Không xóa service hoặc disk khi cần giữ lịch sử.
- Sao lưu MySQL định kỳ bằng `mysqldump`; không nên chỉ dựa vào disk snapshot để khôi phục database.
