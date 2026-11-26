# Hướng dẫn chạy toàn bộ đồ án MediBook

Tài liệu dành cho Windows/PowerShell. Thực hiện từ thư mục gốc repository; thay đường dẫn mẫu bằng vị trí thực tế của source. Đây là hướng dẫn thao tác, không phải xác nhận các lệnh đã được chạy trên máy của bạn.

> **Cảnh báo dữ liệu:** MediBook dùng MySQL và cấu hình mẫu có `ddl-auto=update`, nên backend có thể thay đổi schema khi khởi động. Chỉ dùng database thử nghiệm do bạn kiểm soát. Không bật seeder trên database đang dùng bởi ứng dụng khác. SePay chỉ dùng Sandbox, không chuyển tiền thật.

## 1. Thành phần cần có

| Thành phần | Mục đích | Bắt buộc |
|---|---|---|
| JDK 17 trở lên | Chạy backend Java/Spring Boot | Có |
| MySQL Server | Lưu tài khoản, bác sĩ, slot và lịch | Có |
| Node.js và npm | Cài dependency và chạy frontend React/Vite | Có |
| VS Code hoặc editor | Mở source và các terminal | Khuyến nghị |
| Google Identity Services | Đăng nhập Google | Tùy chọn |
| Gmail SMTP | Gửi email nghiệp vụ | Tùy chọn |
| SePay Sandbox và ngrok | Tạo payment thử nghiệm và nhận webhook từ bên ngoài | Tùy chọn |
| Ollama và model `qwen2.5:3b` | Chạy Trợ lý MediBook | Tùy chọn |

Backend dùng Maven Wrapper đi kèm repository nên không cần cài Maven toàn máy. Kiểm tra các công cụ bằng lệnh sau nếu muốn:

```powershell
java -version
node --version
npm --version
```

## 2. Mở source và chia terminal

Mở thư mục gốc `medibook` trong VS Code. Mở hai terminal PowerShell riêng:

- **Backend:** chạy Spring Boot trong `backend`.
- **Frontend:** chạy Vite trong `frontend`.

MySQL là dịch vụ riêng. Chỉ mở thêm terminal ngrok hoặc Ollama nếu bạn chủ động dùng các tích hợp đó. Không khởi động backend lần thứ hai nếu đã có backend đang chạy.

Trong terminal backend và frontend, đặt đường dẫn repository thực tế:

```powershell
$Repo = "C:\DuongDan\den\medibook"
```

## 3. Tạo database thử nghiệm

1. Khởi động MySQL và mở MySQL Workbench hoặc client khác mà bạn quản lý.
2. Tạo schema thử nghiệm mới nếu cần:

```sql
CREATE DATABASE medibook_demo
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

3. Chọn schema dự định dùng và xác nhận đích:

```sql
SELECT DATABASE();
```

Không xóa/reset schema hiện có. Cấu hình mẫu mặc định trỏ tới database `medibook`, còn seeder chỉ cho phép chạy nếu database hiện hành tên chính xác là `medibook_demo`. Không bật seeder cho tới khi đã xác nhận datasource trong cấu hình local trỏ đúng database thử nghiệm.

## 4. Cấu hình backend

### 4.1 Tạo cấu hình local mà không ghi đè

Từ PowerShell ở thư mục gốc repository:

```powershell
if (-not (Test-Path .\backend\src\main\resources\application.properties)) {
    Copy-Item `
      .\backend\src\main\resources\application-example.properties `
      .\backend\src\main\resources\application.properties
}
```

Nếu `application.properties` đã tồn tại, lệnh trên không ghi đè. Mở file cục bộ bằng editor và tự kiểm tra cấu hình; không chia sẻ nội dung hoặc chụp màn hình có secret.

Xác nhận datasource trỏ tới database thử nghiệm, user MySQL có quyền phù hợp, và các placeholder secret đã được thay bằng giá trị riêng của môi trường. Không đưa secret vào source control.

### 4.2 Nhập secret kín vào terminal backend

Mở PowerShell backend riêng rồi định nghĩa helper sau. Mỗi lần gọi, PowerShell sẽ yêu cầu nhập kín và không echo giá trị:

```powershell
function Set-SecretProcessVariable {
    param([Parameter(Mandatory = $true)][string]$Name)
    $secure = Read-Host "Nhap $Name" -AsSecureString
    $bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
    try {
        $plain = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)
        [Environment]::SetEnvironmentVariable(
            $Name,
            $plain,
            [EnvironmentVariableTarget]::Process
        )
    }
    finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
        Remove-Variable secure, plain, bstr -ErrorAction SilentlyContinue
    }
}
```

Với cấu hình MySQL/JWT, nhập các biến tương ứng trong đúng terminal sẽ chạy backend:

```powershell
Set-SecretProcessVariable MEDIBOOK_DB_PASSWORD
Set-SecretProcessVariable MEDIBOOK_JWT_SECRET
```

Các tên cấu hình dùng trong source/mẫu:

| Tên | Dùng cho |
|---|---|
| `MEDIBOOK_DB_USERNAME`, `MEDIBOOK_DB_PASSWORD` | Đăng nhập MySQL |
| `MEDIBOOK_JWT_SECRET` | Ký JWT MediBook |
| `MEDIBOOK_ADMIN_EMAIL`, `MEDIBOOK_ADMIN_PASSWORD` | Tạo admin khi khởi động nếu email chưa tồn tại |
| `MEDIBOOK_SEED_ENABLED`, `MEDIBOOK_SEED_PASSWORD` | Seeder demo, chỉ với database `medibook_demo` |

Không đặt mật khẩu/JWT key bằng literal trong câu lệnh, không in environment variables để kiểm tra, và không gửi các giá trị đó qua chat. Biến đặt trong terminal chỉ có hiệu lực với process chạy từ terminal đó.

## 5. Chạy backend

Trong terminal backend:

```powershell
Set-Location "$Repo\backend"
.\mvnw.cmd spring-boot:run
```

Để terminal chạy. Backend mẫu sử dụng cổng `8080`. Log Spring Boot báo đã khởi động chỉ xác nhận ứng dụng bắt đầu chạy; chưa chứng minh Google, email, SePay hoặc Ollama đã cấu hình đúng.

Nếu backend không khởi động:

- Kiểm tra MySQL đang chạy, JDBC URL/schema/user đúng và database thử nghiệm đã tồn tại.
- Kiểm tra cổng `8080` có đang được process khác sử dụng hay không; không tự dừng process không rõ nguồn gốc.
- Không bật seeder để xử lý lỗi kết nối database.
- Không đổi `ddl-auto` tùy tiện trên database có dữ liệu.

## 6. Cài và chạy frontend

Trong terminal frontend riêng:

```powershell
Set-Location "$Repo\frontend"
npm ci
npm run dev
```

Mở URL được Vite in ra, thường là `http://localhost:5173`. Backend CORS hiện cho phép origin `http://localhost:5173`, vì vậy nếu Vite chuyển sang cổng khác, xử lý nguyên nhân cổng trước khi demo.

API frontend hiện gọi `http://localhost:8080`. Không có cấu hình `VITE_API_URL` được thấy trong source đã rà.

Để kiểm tra frontend thay vì chạy dev server:

```powershell
npm run build
npm run lint
```

Các lệnh build/lint là lựa chọn kiểm tra; chúng không cần thiết để khởi động dev server.

## 7. Chuẩn bị tài khoản và dữ liệu demo

### Seeder (tùy chọn, chỉ database thử nghiệm)

Seeder có thể tạo bệnh nhân, bác sĩ, chuyên khoa, slot và lịch mẫu. Nó mặc định tắt, ghi dữ liệu vào database, không tạo payment và không gửi email. Seeder kiểm tra tên database là `medibook_demo`.

Chỉ sau khi tự xác nhận local datasource trỏ đúng schema thử nghiệm mới bật trong terminal backend:

```powershell
$env:MEDIBOOK_SEED_ENABLED = "true"
Set-SecretProcessVariable MEDIBOOK_SEED_PASSWORD
```

Mật khẩu seed phải đáp ứng giới hạn ứng dụng. Seeder dùng mật khẩu này cho tài khoản mới, không đặt lại mật khẩu tài khoản đã tồn tại. Tắt lại sau lần khởi động seed:

```powershell
$env:MEDIBOOK_SEED_ENABLED = "false"
```

Ngày seed trong source nằm từ 18/11/2026 đến 31/12/2026; dữ liệu thực tế phụ thuộc database và thời điểm chạy. Không phụ thuộc vào ID lịch hoặc payment cũ.

### Không dùng seeder

- Bệnh nhân có thể đăng ký qua `/register`.
- Admin có thể được tạo lúc startup bằng `MEDIBOOK_ADMIN_EMAIL` và `MEDIBOOK_ADMIN_PASSWORD`; chỉ dùng trên database thử nghiệm. Nếu email đã tồn tại, initializer không đổi role/mật khẩu.
- Bác sĩ cần có user và hồ sơ liên kết. Admin thực hiện liên kết tài khoản tại `/admin/doctors`.

Không có mật khẩu demo cố định được đảm bảo trong source. Hãy chuẩn bị thông tin đăng nhập thử nghiệm riêng và không đưa mật khẩu vào tài liệu/ảnh demo.

## 8. Kiểm tra các màn hình chính

Khi cả hai ứng dụng đã chạy, thử các route sau theo quyền tài khoản:

| Route | Nội dung |
|---|---|
| `/` | Trang chủ |
| `/specialties` | Chuyên khoa |
| `/doctors` | Danh sách/tìm bác sĩ |
| `/register`, `/login` | Đăng ký và đăng nhập |
| `/booking` | Bệnh nhân đặt lịch |
| `/my-appointments` | Bệnh nhân xem lịch và thanh toán |
| `/doctor/appointments` | Bác sĩ xem/xác nhận/hoàn thành lịch |
| `/admin/specialties` | Admin quản lý chuyên khoa |
| `/admin/doctors` | Admin quản lý hồ sơ và liên kết bác sĩ |
| `/admin/appointment-slots` | Admin quản lý slot |

Đặt lịch, hủy lịch, tạo payment, xác nhận/hoàn thành lịch và các thao tác quản trị có thể ghi dữ liệu. Chỉ thực hiện với tài khoản và database thử nghiệm. Backend kiểm tra quyền; việc một nút bị ẩn/vô hiệu hóa ở frontend không thay thế kiểm tra quyền server.

## 9. Tích hợp tùy chọn

Tắt các tích hợp này nếu chưa chuẩn bị môi trường. Không cần chúng để mở các trang cơ bản.

### Google login

1. Tạo/cấu hình Google Web Client ID do chủ dự án kiểm soát.
2. Nếu frontend `.env` chưa có, tạo từ mẫu mà không ghi đè:

```powershell
if (-not (Test-Path .\frontend\.env)) {
    Copy-Item .\frontend\.env.example .\frontend\.env
}
```

3. Đặt Web Client ID công khai vào `VITE_GOOGLE_CLIENT_ID` trong `frontend/.env` và `GOOGLE_CLIENT_ID` cho backend (property `app.google.client-id`).
4. Cấu hình JavaScript origin `http://localhost:5173`, tải lại frontend và khởi động lại backend sau khi đổi cấu hình.

Không đưa Google Client Secret vào frontend. Facebook login chưa thấy trong source.

### Email Gmail

Khi chủ động bật gửi mail, đặt `MEDIBOOK_MAIL_ENABLED=true`, `MEDIBOOK_MAIL_USERNAME` và cấu hình `MEDIBOOK_MAIL_APP_PASSWORD` trong terminal backend; mẫu dùng Gmail SMTP `smtp.gmail.com:587`. Chỉ dùng tài khoản gửi/nhận thử nghiệm do bạn kiểm soát. Email có thể được gửi khi đặt, hủy, bác sĩ xác nhận hoặc hoàn thành lịch. Lỗi gửi mail được log; không có retry bền vững.

### SePay Sandbox và webhook

Luồng thanh toán hiện tại là SePay Sandbox, không phải VNPay/MoMo. Tên cổng khác với yêu cầu đồ án đã được cung cấp; cần hỏi giảng viên, không tự coi là đã được chấp thuận.

Các cấu hình liên quan gồm `SEPAY_PAYMENT_ENABLED`, `SEPAY_PAYMENT_AMOUNT`, `SEPAY_PAYMENT_EXPIRE_MINUTES`, `SEPAY_RECEIVER_ACCOUNT`, `SEPAY_RECEIVER_GATEWAY`, `SEPAY_WEBHOOK_ENABLED`, `SEPAY_WEBHOOK_API_KEY`. Chỉ sử dụng receiver và thông tin Sandbox được cấp; không dùng tài khoản thật. Không đặt webhook key ở frontend.

Để nhận webhook từ Sandbox vào backend local cần cấu hình callback đúng, backend đang chạy, và một tunnel công khai như ngrok:

```powershell
ngrok http 8080
```

Callback path trong source là `/api/payments/sepay/webhook`. Hostname tunnel có thể đổi sau khi khởi động lại. Xác minh portal/provider hỗ trợ cách gửi header `Authorization: Apikey ...` trước khi bật webhook. Không tạo payment hoặc gửi webhook nếu chưa sẵn sàng dùng dữ liệu thử nghiệm.

Đọc payment bằng `GET /api/payments/sepay/appointments/{appointmentId}` không tạo giao dịch; thao tác tạo payment là hành động riêng trên giao diện và gọi POST. Trạng thái payment và trạng thái lịch là hai trạng thái độc lập. Không có chức năng hoàn tiền/xóa review.

### Trợ lý AI / Ollama

Theo cấu hình mẫu, backend kết nối Ollama local tại `http://localhost:11434`, model `qwen2.5:3b`; model không tự tải khi backend chạy. Cài/chạy Ollama và tải model theo tài liệu Ollama phiên bản đang dùng. Sau đó bật `MEDIBOOK_AI_ENABLED=true` trong terminal backend và khởi động lại.

Trợ lý chỉ hướng dẫn sử dụng, không đọc lịch cá nhân hoặc database và không thay thế tư vấn y tế. Nếu máy thiếu RAM, để AI tắt. Prompt hiện còn nội dung cũ nói thanh toán đang phát triển.

## 10. Dừng ứng dụng

- Dừng backend và frontend do chính bạn khởi động bằng `Ctrl+C` tại đúng terminal tương ứng.
- Chỉ dừng ngrok/Ollama nếu chính bạn đã khởi động chúng và không còn cần dùng.
- Không kết thúc process thuộc phiên làm việc của người khác.

## 11. Test backend có chọn lọc

Các test service với Mockito là unit test, không chứng minh tích hợp database, transaction/rollback, khóa đồng thời, race condition hoặc tích hợp SePay thật. `BackendApplicationTests` có thể khởi động Spring context; không chạy toàn bộ test nếu chưa xác định datasource test cô lập.

Từ terminal riêng:

```powershell
Set-Location "$Repo\backend"
.\mvnw.cmd "-Dtest=AppointmentServiceTest,DoctorAppointmentServiceTest" test
```

Hai lớp SePay có thể chạy chọn lọc nếu chủ động muốn:

```powershell
.\mvnw.cmd "-Dtest=SepayPaymentServiceTest" test
.\mvnw.cmd "-Dtest=SepayWebhookServiceTest" test
```

Nếu JVM báo lỗi native memory trước khi test chạy, dừng và xem xét nguyên nhân môi trường; không lặp lệnh liên tục, tự tăng heap hoặc sửa cấu hình hệ thống khi chưa hiểu tác động.

## 12. Tham khảo

- [README tổng quan MediBook](./README.md)
- [Hướng dẫn chạy và demo chi tiết](./HUONG_DAN_CHAY_VA_DEMO.md)
- Mẫu cấu hình backend: `backend/src/main/resources/application-example.properties`
- Scripts frontend: `frontend/package.json`

