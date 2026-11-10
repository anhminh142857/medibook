# React + Vite

This template provides a minimal setup to get React working in Vite with HMR and some Oxlint rules.

Currently, two official plugins are available:

- [@vitejs/plugin-react](https://github.com/vitejs/vite-plugin-react/blob/main/packages/plugin-react) uses [Oxc](https://oxc.rs)
- [@vitejs/plugin-react-swc](https://github.com/vitejs/vite-plugin-react/blob/main/packages/plugin-react-swc) uses [SWC](https://swc.rs/)

## React Compiler

The React Compiler is not enabled on this template because of its impact on dev & build performances. To add it, see [this documentation](https://react.dev/learn/react-compiler/installation).

## Expanding the Oxlint configuration

If you are developing a production application, we recommend using TypeScript with type-aware lint rules enabled. Check out the [TS template](https://github.com/vitejs/vite/tree/main/packages/create-vite/template-react-ts) for information on how to integrate TypeScript and Oxlint's TypeScript related rules in your project.


# MediBook — Website đặt lịch khám bệnh

MediBook là đồ án chuyên đề J2EE xây dựng website đặt lịch khám bệnh trực tuyến.

Hệ thống phục vụ ba nhóm người dùng:

- Bệnh nhân: tìm chuyên khoa, bác sĩ, đặt lịch và theo dõi lịch hẹn.
- Bác sĩ: xem, xác nhận và hoàn thành các lịch khám thuộc hồ sơ của mình.
- Quản trị viên: quản lý chuyên khoa, bác sĩ, khung giờ và liên kết tài khoản bác sĩ.

Backend và frontend phát triển riêng, giao tiếp thông qua REST API.

> Cập nhật tài liệu: 10/11/2026, khoảng 02:43.
>
> Dự án đang phát triển, phục vụ học tập và trình bày đồ án.
>
> VNPAY đang tạm hoãn để chờ trao đổi với giảng viên.
> Chưa sử dụng hệ thống để thu tiền thật.

---

## 1. Công nghệ sử dụng

### Backend

- Spring Boot 4.1.2.
- Java target trong POM: 17.
- Java runtime đã sử dụng: 22.0.2.
- Maven và Maven Wrapper.
- Spring Web MVC.
- Spring Security.
- JWT.
- Spring Data JPA / Hibernate.
- MySQL.
- Bean Validation.
- Lombok.
- Springdoc OpenAPI / Swagger UI 3.2.0.
- Spring Mail.

### Frontend

- React.
- Vite.
- React Router.
- Bootstrap.
- Fetch API.

### Đăng nhập Google

- Google Identity Services trên frontend.
- Backend xác minh Google ID token.
- Backend cấp JWT riêng của MediBook.

### Email thông báo

- Gmail SMTP.
- Gmail App Password.
- Gửi thông báo cho bệnh nhân theo sự kiện lịch hẹn.

### Thanh toán dự kiến

- VNPAY Sandbox.
- Phí dự kiến: 100.000 VNĐ/lịch.
- Đặt lịch trước, thanh toán sau.
- Đang tạm hoãn tích hợp.
- Không phát triển song song MoMo.
- Chưa thanh toán thật hoặc hoàn tiền tự động.

---

## 2. Trạng thái dự án

Phân biệt ba mức:

- Đã triển khai: đã có code trong quá trình phát triển.
- Đã xác nhận: người phát triển đã báo kiểm thử thành công.
- Chờ kiểm tra: vừa được cung cấp code hoặc chưa có xác nhận kiểm thử đầy đủ.

Không coi có code là đã kiểm thử thành công.

### 2.1. Chức năng đã triển khai

- [x] Backend kết nối MySQL.
- [x] Frontend React/Vite kết nối backend.
- [x] Đăng ký bệnh nhân.
- [x] Đăng nhập email/mật khẩu.
- [x] Xác thực JWT.
- [x] Phân quyền PATIENT, DOCTOR, ADMIN.
- [x] Khởi tạo vai trò.
- [x] Khởi tạo admin qua biến môi trường.
- [x] Quản lý chuyên khoa.
- [x] Quản lý hồ sơ bác sĩ.
- [x] Quản lý khung giờ khám.
- [x] Liên kết tài khoản với hồ sơ bác sĩ.
- [x] Bệnh nhân đặt lịch.
- [x] Bệnh nhân xem lịch đã đặt.
- [x] Bệnh nhân hủy lịch theo điều kiện.
- [x] Giữ lịch sử hủy.
- [x] Cho phép đặt lại khung giờ sau khi hủy hợp lệ.
- [x] Bác sĩ xem lịch thuộc hồ sơ của mình.
- [x] Bác sĩ xác nhận lịch.
- [x] Có luồng hoàn thành lịch sau giờ kết thúc.
- [x] Trang chủ sử dụng dữ liệu thật.
- [x] Danh sách và tìm kiếm chuyên khoa.
- [x] Danh sách bác sĩ, tìm theo tên và lọc chuyên khoa.
- [x] Chọn bác sĩ từ danh sách để chuyển sang Booking.
- [x] Tích hợp đăng nhập Google.
- [x] Có service gửi email thông báo lịch hẹn.
- [x] Đã tạo bảng `payment_transactions`.

### 2.2. Kiểm thử đã ghi nhận

- Đã liên kết tài khoản bác sĩ và có ROLE_DOCTOR.
- Đã kiểm thử đặt, hủy và đặt lại lịch.
- Đã kiểm thử bác sĩ xác nhận lịch.
- Các trang công khai và luồng chọn bác sĩ → đặt lịch đã có xác nhận hoạt động.
- Có xác nhận kiểm thử chung sau tích hợp Google.
- Ngày 10/11/2026: đã nhận được Gmail thông báo từ MediBook sau khi cấu hình đúng biến môi trường SMTP.

Lưu ý về email:

- Đã xác nhận nhận được email thông báo.
- Chưa có xác nhận riêng đủ cả bốn loại email:
  đặt lịch, hủy lịch, xác nhận và hoàn thành.
- Chưa có xác nhận kiểm thử rollback, SMTP lỗi và tình huống gửi lặp đầy đủ.

### 2.3. Frontend đã được cung cấp bản hoàn thiện

```text
frontend/src/pages/MyAppointments.jsx
frontend/src/pages/DoctorAppointments.jsx
frontend/src/components/Navbar.jsx
```

Các cải tiến:

- Nút đặt lịch mới.
- Trạng thái đang tải.
- Thông báo khi chưa có lịch.
- Thông báo thành công.
- Hiển thị số lượng lịch.
- Bộ lọc ngày và trạng thái cho bác sĩ.
- Nút xác nhận/hoàn thành theo trạng thái.
- Menu trên màn hình nhỏ.
- Nhãn vai trò dưới tên người dùng.

Các thay đổi này vẫn cần kiểm tra lại sau khi áp dụng bản phân quyền mới nhất.

### 2.4. Thay đổi mới nhất đang chờ xác nhận

Đã cung cấp bản cập nhật nguyên file:

```text
frontend/src/components/ProtectedRoute.jsx
frontend/src/App.jsx
frontend/src/components/Navbar.jsx
backend/src/main/java/com/medibook/backend/service/AppointmentService.java
```

Mục tiêu:

- Bác sĩ/admin không sử dụng trang đặt lịch bệnh nhân.
- Bác sĩ/admin không mở trang “Lịch hẹn của tôi”.
- Bác sĩ/admin không tạo lịch mới qua backend.
- Không xóa ROLE_PATIENT đang tồn tại.
- Không xóa lịch cũ.
- Giữ email đặt/hủy lịch.

Chưa có xác nhận sau đợt cập nhật này về:

- Đã dán và lưu đầy đủ bốn file.
- Backend compile và khởi động thành công.
- Frontend hoạt động đúng.
- Bác sĩ/admin bị chặn tạo lịch mới.
- Bệnh nhân vẫn đặt/hủy lịch và nhận email.

### 2.5. Việc còn lại

- [ ] Kiểm tra bản phân quyền mới nhất.
- [ ] Kiểm thử đủ bốn loại email.
- [ ] Kiểm thử hoàn thành lịch sau giờ kết thúc.
- [ ] Rà bảo mật và quyền sở hữu dữ liệu.
- [ ] Kiểm thử hai người đặt cùng khung giờ.
- [ ] Hoàn thiện frontend Booking và các trang công khai.
- [ ] Kiểm tra giao diện trên màn hình nhỏ.
- [ ] Chuẩn bị dữ liệu demo.
- [ ] Hoàn thiện báo cáo và sơ đồ.
- [ ] Lập bảng kiểm thử.
- [ ] Chuẩn bị slide và kịch bản demo.
- [ ] Trao đổi với giảng viên về VNPAY.
- [ ] Deploy/AI nếu được yêu cầu hoặc còn thời gian.

Danh sách này là phạm vi phát triển đã thống nhất trong quá trình làm.
Cần đối chiếu đề bài và tiêu chí chấm chính thức trước khi kết luận đủ yêu cầu môn học.

---

## 3. Cấu trúc dự án

```text
medibook/
├── backend/
│   ├── src/main/
│   │   ├── java/com/medibook/backend/
│   │   │   ├── config/
│   │   │   ├── controller/
│   │   │   ├── dto/
│   │   │   ├── entity/
│   │   │   ├── repository/
│   │   │   ├── service/
│   │   │   └── BackendApplication.java
│   │   └── resources/
│   │       └── application.properties
│   ├── pom.xml
│   ├── mvnw
│   └── mvnw.cmd
├── frontend/
│   ├── src/
│   │   ├── api/
│   │   ├── components/
│   │   ├── pages/
│   │   ├── App.jsx
│   │   ├── App.css
│   │   └── index.css
│   └── package.json
└── README.md
```

Package backend:

```text
com.medibook.backend
```

Main class:

```text
BackendApplication
```

Đường dẫn máy phát triển:

```text
C:\Users\142857\Documents\GitHub\medibook
```

Người clone dự án không cần sử dụng cùng đường dẫn.

---

## 4. Chuẩn bị môi trường

Cần cài đặt:

1. Git.
2. JDK phù hợp với dự án.
3. Node.js và npm phù hợp với frontend hiện tại.
4. MySQL.
5. VS Code hoặc IDE Java.

Môi trường đã sử dụng:

| Thành phần | Giá trị |
|---|---|
| Hệ điều hành | Windows |
| Java runtime | 22.0.2 |
| Java target trong POM | 17 |
| MySQL | 8.0.44 |
| Database | medibook |
| Backend | 8080 |
| Frontend | 5173 |

Kiểm tra:

```powershell
java -version
node -v
npm -v
git --version
```

Có Maven Wrapper nên không bắt buộc cài Maven toàn cục.

Không đổi Spring Boot, Java target hoặc dependency để thử sửa lỗi khi chưa xác định nguyên nhân.

---

## 5. Lấy source

```powershell
git clone <URL_REPOSITORY_CUA_BAN>
cd medibook
code .
```

Nếu đã có source, mở thư mục gốc chứa cả:

```text
backend
frontend
```

Không tạo lại project khi chỉ cần sửa code hiện có.

---

## 6. Database

### 6.1. Bật MySQL

Sử dụng đúng MySQL instance mà backend kết nối.

Có thể sử dụng:

- MySQL Windows Service.
- MySQL trong XAMPP/Laragon.

Không chạy nhiều instance cùng cổng.

### 6.2. Tạo database

```sql
CREATE DATABASE IF NOT EXISTS medibook
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;
```

### 6.3. Khi clone sang máy khác

Clone GitHub không sao chép database.

Trên database mới:

- Chưa chắc có tài khoản demo.
- Chưa có dữ liệu chuyên khoa, bác sĩ, khung giờ và lịch hẹn.
- Cần tạo admin, sau đó thêm dữ liệu.
- `ddl-auto=update` không khôi phục dữ liệu của máy cũ.

Không commit database dump chứa thông tin cá nhân hoặc secret.

---

## 7. Cấu hình backend

File:

```text
backend/src/main/resources/application.properties
```

Cấu hình mẫu sử dụng biến môi trường:

```properties
# Database
spring.datasource.url=jdbc:mysql://localhost:3306/medibook
spring.datasource.username=${MEDIBOOK_DB_USERNAME:root}
spring.datasource.password=${MEDIBOOK_DB_PASSWORD}

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

server.port=8080

# JWT
app.jwt.secret=${MEDIBOOK_JWT_SECRET}
app.jwt.expiration-ms=86400000

# Google
app.google.client-id=${GOOGLE_CLIENT_ID:}

# Email
app.mail.enabled=${MEDIBOOK_MAIL_ENABLED:false}

spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=${MEDIBOOK_MAIL_USERNAME:}
spring.mail.password=${MEDIBOOK_MAIL_APP_PASSWORD:}
spring.mail.default-encoding=UTF-8

spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.starttls.required=true
spring.mail.properties.mail.smtp.ssl.checkserveridentity=true

spring.mail.properties.mail.smtp.connectiontimeout=5000
spring.mail.properties.mail.smtp.timeout=5000
spring.mail.properties.mail.smtp.writetimeout=5000

spring.mail.test-connection=false

# VNPAY Sandbox - tam hoan
app.vnpay.tmn-code=${VNPAY_TMN_CODE:}
app.vnpay.hash-secret=${VNPAY_HASH_SECRET:}
app.vnpay.pay-url=[https://sandbox.vnpayment.vn/paymentv2/vpcpay.html](https://sandbox.vnpayment.vn/paymentv2/vpcpay.html)
app.vnpay.return-url=http://localhost:5173/payment/vnpay-return
app.vnpay.amount=100000
app.vnpay.expire-minutes=15
```

Lưu ý:

- Đây là cấu hình mẫu; cần đối chiếu file thực tế trước khi thay.
- Không tạo trùng key cấu hình.
- Không thay mật khẩu local đang chạy bằng placeholder chưa được đặt.
- Mật khẩu MySQL khác mật khẩu đăng nhập MediBook.
- Không đổi `ddl-auto` thành `create` hoặc `create-drop` trên database cần giữ.
- Return URL VNPAY hiện chỉ là cấu hình dự kiến, chưa có trang tương ứng.
- Thiếu khóa VNPAY không được làm hỏng các chức năng đang phát triển.
- Backend không tự đọc `frontend/.env`.

---

## Hướng dẫn cấu hình biến môi trường ENV

Dự án sử dụng hai nhóm cấu hình riêng:

| Thành phần | Cách cấu hình |
|---|---|
| Backend Spring Boot | Biến môi trường trong terminal chạy backend |
| Frontend React/Vite | File `frontend/.env` |

Không đặt secret backend trong `frontend/.env`.

### 1. Danh sách biến môi trường backend

| Biến | Mục đích | Khi nào cần |
|---|---|---|
| `MEDIBOOK_DB_USERNAME` | Tài khoản MySQL | Khi cấu hình datasource đọc biến này |
| `MEDIBOOK_DB_PASSWORD` | Mật khẩu MySQL | Khi cấu hình datasource đọc biến này |
| `MEDIBOOK_JWT_SECRET` | Khóa ký JWT | Khi cấu hình JWT đọc biến này |
| `GOOGLE_CLIENT_ID` | Client ID đăng nhập Google | Khi kiểm thử đăng nhập Google |
| `MEDIBOOK_MAIL_ENABLED` | Bật/tắt gửi email | Đặt `true` để gửi email |
| `MEDIBOOK_MAIL_USERNAME` | Gmail gửi thông báo | Khi bật email |
| `MEDIBOOK_MAIL_APP_PASSWORD` | Gmail App Password | Khi bật email |
| `MEDIBOOK_ADMIN_EMAIL` | Email admin cần khởi tạo | Chỉ khi tạo admin mới |
| `MEDIBOOK_ADMIN_PASSWORD` | Mật khẩu admin cần khởi tạo | Chỉ khi tạo admin mới |
| `VNPAY_TMN_CODE` | Mã website VNPAY | Chưa cần khi đang tạm hoãn |
| `VNPAY_HASH_SECRET` | Khóa ký VNPAY | Chưa cần khi đang tạm hoãn |

Tên biến phải khớp placeholder trong `application.properties`.

Ví dụ:

```properties
spring.datasource.username=${MEDIBOOK_DB_USERNAME:root}
spring.datasource.password=${MEDIBOOK_DB_PASSWORD}

app.jwt.secret=${MEDIBOOK_JWT_SECRET}
app.google.client-id=${GOOGLE_CLIENT_ID:}

app.mail.enabled=${MEDIBOOK_MAIL_ENABLED:false}
spring.mail.username=${MEDIBOOK_MAIL_USERNAME:}
spring.mail.password=${MEDIBOOK_MAIL_APP_PASSWORD:}
```

Nếu file thực tế vẫn ghi trực tiếp giá trị cấu hình thì đặt biến môi trường
chưa chắc thay đổi giá trị đó. Cần kiểm tra placeholder tương ứng.

Không ghi mật khẩu thật vào README.

### 2. Mở đúng terminal backend

Trong VS Code, mở terminal PowerShell.

Từ thư mục gốc:

```powershell
cd backend
```

Đường dẫn máy phát triển:

```text
C:\Users\142857\Documents\GitHub\medibook\backend
```

Tất cả lệnh `$env:...` và lệnh chạy backend bên dưới
phải thực hiện trong cùng terminal này.

Nếu backend đang chạy, nhấn:

```text
Ctrl + C
```

trước khi cấu hình và khởi động lại.

### 3. Cấu hình MySQL

Đặt username:

```powershell
$env:MEDIBOOK_DB_USERNAME="root"
```

Thay `root` nếu sử dụng tài khoản MySQL khác.

Nhập mật khẩu theo cách không hiển thị nội dung:

```powershell
$dbPassword = Read-Host "Nhap mat khau MySQL" -AsSecureString

$env:MEDIBOOK_DB_PASSWORD = (
    New-Object System.Net.NetworkCredential("", $dbPassword)
).Password

Remove-Variable dbPassword
```

Mật khẩu này là mật khẩu MySQL, không phải:

- Mật khẩu tài khoản MediBook.
- Mật khẩu Gmail.
- Gmail App Password.

### 4. Cấu hình JWT secret

Nếu đã có JWT secret riêng, tiếp tục sử dụng giá trị đó.

Nhập secret mà không hiển thị ra terminal:

```powershell
$jwtSecret = Read-Host "Nhap JWT secret dang su dung" -AsSecureString

$env:MEDIBOOK_JWT_SECRET = (
    New-Object System.Net.NetworkCredential("", $jwtSecret)
).Password

Remove-Variable jwtSecret
```

Nếu chưa có secret, có thể tạo ngẫu nhiên cho môi trường local:

```powershell
$bytes = New-Object byte[] 64

$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
$rng.Dispose()

$env:MEDIBOOK_JWT_SECRET = [Convert]::ToBase64String($bytes)

Remove-Variable bytes
Remove-Variable rng
```

Lưu ý:

- Không dùng một secret mẫu công khai.
- Đổi secret làm JWT đã cấp bằng secret cũ không còn hợp lệ.
- Nếu tạo secret mới cho mỗi terminal, cần đăng nhập lại sau khi restart backend.
- Không in secret ra terminal để chụp ảnh hoặc gửi hỗ trợ.

### 5. Cấu hình Google đăng nhập

Backend:

```powershell
$env:GOOGLE_CLIENT_ID="CLIENT_ID_CUA_BAN.apps.googleusercontent.com"
```

Thay bằng Client ID thực tế.

Client ID phải khớp giá trị trong `frontend/.env`.

Origin frontend hiện tại:

```text
http://localhost:5173
```

Không đưa Google Client Secret vào frontend.

Google Client ID chỉ phục vụ đăng nhập Google,
không thay thế cấu hình Gmail SMTP.

### 6. Cấu hình Gmail gửi thông báo

Chuẩn bị trước:

1. Bật xác minh hai bước cho Gmail gửi.
2. Tạo App Password riêng cho MediBook.
3. Không sử dụng mật khẩu Gmail thông thường.

Đặt các biến không nhạy cảm:

```powershell
$env:MEDIBOOK_MAIL_ENABLED="true"
$env:MEDIBOOK_MAIL_USERNAME="GMAIL_GUI_THONG_BAO@gmail.com"
```

Thay email ví dụ bằng Gmail gửi thực tế.

Chạy riêng dòng:

```powershell
$mailPassword = Read-Host "Nhap Gmail App Password" -AsSecureString
```

Khi terminal hiển thị:

```text
Nhap Gmail App Password:
```

mới dán App Password và nhấn Enter.

Không sửa phần `"Nhap Gmail App Password"` thành mật khẩu thật.

Sau đó chạy:

```powershell
$env:MEDIBOOK_MAIL_APP_PASSWORD = (
    New-Object System.Net.NetworkCredential("", $mailPassword)
).Password.Replace(" ", "")

Remove-Variable mailPassword
```

Nếu App Password từng xuất hiện trong chat, ảnh hoặc source công khai,
thu hồi mật khẩu đó và tạo mật khẩu ứng dụng mới.

### 7. Kiểm tra ENV mà không lộ secret

Chạy:

```powershell
Write-Host "DB_USERNAME = $($env:MEDIBOOK_DB_USERNAME)"
Write-Host "MAIL_ENABLED = $($env:MEDIBOOK_MAIL_ENABLED)"
Write-Host "MAIL_USERNAME = $($env:MEDIBOOK_MAIL_USERNAME)"

$secretVariables = @(
    "MEDIBOOK_DB_PASSWORD",
    "MEDIBOOK_JWT_SECRET",
    "MEDIBOOK_MAIL_APP_PASSWORD"
)

foreach ($name in $secretVariables) {
    $value = [Environment]::GetEnvironmentVariable(
        $name,
        "Process"
    )

    if ([string]::IsNullOrWhiteSpace($value)) {
        Write-Host "$name = CHUA CO HOAC DANG RONG"
    } else {
        Write-Host "$name = DA CO"
    }
}

if ([string]::IsNullOrWhiteSpace($env:GOOGLE_CLIENT_ID)) {
    Write-Host "GOOGLE_CLIENT_ID = CHUA CO"
} else {
    Write-Host "GOOGLE_CLIENT_ID = DA CO"
}

Remove-Variable value -ErrorAction SilentlyContinue
```

Khi kiểm thử đầy đủ database, JWT, Google và email, kết quả mong đợi:

```text
DB_USERNAME = root
MAIL_ENABLED = true
MAIL_USERNAME = <Gmail gui thong bao>
MEDIBOOK_DB_PASSWORD = DA CO
MEDIBOOK_JWT_SECRET = DA CO
MEDIBOOK_MAIL_APP_PASSWORD = DA CO
GOOGLE_CLIENT_ID = DA CO
```

Nếu MySQL local dùng mật khẩu rỗng,
dòng kiểm tra password sẽ báo rỗng; cần đối chiếu cấu hình MySQL thực tế.

Lệnh kiểm tra chỉ xác nhận biến đã có giá trị,
không xác nhận mật khẩu đúng hoặc dịch vụ đã kết nối thành công.

Không sử dụng lệnh in toàn bộ biến môi trường để gửi ảnh hỗ trợ,
vì có thể làm lộ các secret.

### 8. Chạy backend sau khi đặt ENV

Trong chính terminal đã cấu hình:

```powershell
.\mvnw.cmd spring-boot:run
```

Chờ:

```text
Started BackendApplication ...
```

Sau đó kiểm thử từng chức năng:

- Đăng nhập mật khẩu để kiểm tra JWT.
- Đăng nhập Google để kiểm tra Client ID.
- Đặt lịch bằng bệnh nhân có email thật để kiểm tra SMTP.

Backend khởi động thành công chưa chứng minh gửi email thành công.

### 9. Cấu hình ENV frontend

Tạo file:

```text
frontend/.env
```

Nội dung:

```dotenv
VITE_GOOGLE_CLIENT_ID=CLIENT_ID_CUA_BAN.apps.googleusercontent.com
```

Không thêm vào file này:

```text
MEDIBOOK_DB_PASSWORD
MEDIBOOK_JWT_SECRET
MEDIBOOK_MAIL_APP_PASSWORD
VNPAY_HASH_SECRET
GOOGLE_CLIENT_SECRET
```

Tạo file mẫu có thể commit:

```text
frontend/.env.example
```

Nội dung:

```dotenv
VITE_GOOGLE_CLIENT_ID=YOUR_GOOGLE_WEB_CLIENT_ID
```

Sau khi sửa `.env`, dừng và chạy lại frontend:

```powershell
npm run dev
```

Lệnh trên thực hiện trong terminal frontend riêng, tại thư mục `frontend`.

### 10. Khởi tạo admin bằng ENV

Chỉ thực hiện khi cần tạo admin mới.

Trong terminal backend đã cấu hình database/JWT:

```powershell
$env:MEDIBOOK_ADMIN_EMAIL="admin@medibook.local"

$adminPassword = Read-Host "Nhap mat khau admin moi" -AsSecureString

$env:MEDIBOOK_ADMIN_PASSWORD = (
    New-Object System.Net.NetworkCredential("", $adminPassword)
).Password

Remove-Variable adminPassword

.\mvnw.cmd spring-boot:run
```

Yêu cầu mật khẩu theo initializer hiện tại:

- Tối thiểu 12 ký tự.
- Không quá 72 byte UTF-8.

Initializer không reset mật khẩu hoặc nâng quyền email đã tồn tại.

Sau khi tạo thành công và dừng backend:

```powershell
Remove-Item Env:MEDIBOOK_ADMIN_EMAIL -ErrorAction SilentlyContinue
Remove-Item Env:MEDIBOOK_ADMIN_PASSWORD -ErrorAction SilentlyContinue
```

Không cần đặt lại hai biến này trong các lần chạy thông thường.

### 11. VNPAY đang tạm hoãn

Hiện tại không bắt buộc đặt:

```text
VNPAY_TMN_CODE
VNPAY_HASH_SECRET
```

Không nhập khóa giả để vượt qua kiểm tra cấu hình.

Giữ phần VNPAY tách biệt với database, JWT, Google và Gmail.
Việc chưa có khóa VNPAY không được cản trở kiểm thử các chức năng khác.

### 12. Khi mở terminal mới hoặc khởi động lại máy

Các biến đặt bằng:

```powershell
$env:TEN_BIEN="gia tri"
```

trong hướng dẫn này thuộc phiên terminal hiện tại.

Khi mở terminal mới:

1. Vào thư mục `backend`.
2. Nạp lại các biến cần dùng.
3. Kiểm tra trạng thái biến.
4. Chạy backend trong terminal đó.

Đóng trình duyệt không đồng nghĩa đóng terminal.
Nhấn `Ctrl + C` để dừng backend thường vẫn giữ terminal và các biến đã đặt.

Không đặt biến ở terminal frontend rồi mong backend đang chạy tự nhận.

### 13. Tạm tắt hoặc xóa cấu hình email

Tạm tắt gửi email:

```powershell
$env:MEDIBOOK_MAIL_ENABLED="false"
```

Sau đó restart backend.

Xóa thông tin email khỏi terminal hiện tại:

```powershell
Remove-Item Env:MEDIBOOK_MAIL_ENABLED -ErrorAction SilentlyContinue
Remove-Item Env:MEDIBOOK_MAIL_USERNAME -ErrorAction SilentlyContinue
Remove-Item Env:MEDIBOOK_MAIL_APP_PASSWORD -ErrorAction SilentlyContinue
```

Xóa biến khỏi terminal không thu hồi App Password trên Google.
Nếu mật khẩu bị lộ, phải thu hồi tại tài khoản Google.

### 14. Checklist bảo mật ENV

- [ ] Không có mật khẩu thật trong README.
- [ ] Không có secret backend trong `frontend/.env`.
- [ ] `frontend/.env` được bỏ qua bởi Git.
- [ ] Có `frontend/.env.example` không chứa secret.
- [ ] `application.properties` dùng placeholder cho secret.
- [ ] Không commit script PowerShell chứa mật khẩu thật.
- [ ] Không chụp terminal đang hiển thị secret.
- [ ] App Password từng bị lộ đã được thu hồi.
- [ ] Kiểm tra `git diff --cached` trước khi commit.

------

## 8. Biến môi trường backend

Các lệnh dưới đây thực hiện trong terminal sẽ chạy backend.

### Database

Nếu `application.properties` đang sử dụng biến môi trường:

```powershell
$env:MEDIBOOK_DB_USERNAME="root"

$dbPassword = Read-Host "Nhap mat khau MySQL" -AsSecureString

$env:MEDIBOOK_DB_PASSWORD = (
    New-Object System.Net.NetworkCredential("", $dbPassword)
).Password

Remove-Variable dbPassword
```

### JWT

Nếu đã có JWT secret riêng, tiếp tục sử dụng secret đó.

Có thể tạo secret ngẫu nhiên cho môi trường local:

```powershell
$bytes = New-Object byte[] 64
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()

$rng.GetBytes($bytes)
$rng.Dispose()

$env:MEDIBOOK_JWT_SECRET = [Convert]::ToBase64String($bytes)

Remove-Variable bytes
Remove-Variable rng
```

Đổi JWT secret làm token đã cấp bằng secret cũ không còn hợp lệ.

### Google

```powershell
$env:GOOGLE_CLIENT_ID="CLIENT_ID.apps.googleusercontent.com"
```

### Phạm vi biến môi trường

Biến đặt bằng `$env:...` chỉ áp dụng cho terminal đó và tiến trình con.

- Đặt biến trước khi chạy backend.
- Backend đang chạy không tự nhận giá trị vừa thay trong một terminal khác.
- Mở terminal mới có thể phải đặt lại biến.
- Terminal frontend không truyền biến ngược sang backend đang chạy.

---

## 9. Cấu hình Gmail gửi thông báo

### 9.1. Chuẩn bị Gmail

- Bật xác minh hai bước cho tài khoản gửi.
- Tạo App Password riêng cho MediBook.
- Không sử dụng mật khẩu đăng nhập Gmail thông thường.
- Không gửi App Password vào chat, ảnh chụp hoặc GitHub.
- Nếu App Password bị lộ, thu hồi và tạo mật khẩu ứng dụng mới.

Trang quản lý:

```text
[https://myaccount.google.com/apppasswords](https://myaccount.google.com/apppasswords)
```

Google Client ID dùng đăng nhập website không thay thế App Password SMTP.

### 9.2. Đặt thông tin gửi thư

Trong terminal backend:

```powershell
$env:MEDIBOOK_MAIL_ENABLED="true"
$env:MEDIBOOK_MAIL_USERNAME="GMAIL_GUI_THONG_BAO@gmail.com"
```

Nhập App Password:

```powershell
$mailPassword = Read-Host "Nhap Gmail App Password" -AsSecureString
```

Khi hiện:

```text
Nhap Gmail App Password:
```

mới dán App Password rồi nhấn Enter.

Không thay chữ `"Nhap Gmail App Password"` trong lệnh bằng mật khẩu thật.

Sau đó chạy:

```powershell
$env:MEDIBOOK_MAIL_APP_PASSWORD = (
    New-Object System.Net.NetworkCredential("", $mailPassword)
).Password.Replace(" ", "")

Remove-Variable mailPassword
```

### 9.3. Kiểm tra mà không in mật khẩu

```powershell
Write-Host "MAIL_ENABLED = $($env:MEDIBOOK_MAIL_ENABLED)"
Write-Host "MAIL_USERNAME = $($env:MEDIBOOK_MAIL_USERNAME)"

if ([string]::IsNullOrWhiteSpace($env:MEDIBOOK_MAIL_APP_PASSWORD)) {
    Write-Host "MAIL_APP_PASSWORD = CHUA CO"
} else {
    Write-Host "MAIL_APP_PASSWORD = DA CO"
}
```

Kết quả mong đợi:

```text
MAIL_ENABLED = true
MAIL_USERNAME = <Gmail gui thong bao>
MAIL_APP_PASSWORD = DA CO
```

Không in trực tiếp giá trị `MEDIBOOK_MAIL_APP_PASSWORD`.

### 9.4. Tạm tắt email

Dừng backend, đặt:

```powershell
$env:MEDIBOOK_MAIL_ENABLED="false"
```

Sau đó chạy lại backend.

### 9.5. Lỗi đã gặp và cách khắc phục

Đã gặp tình huống:

```text
MAIL_ENABLED =
MAIL_USERNAME =
MAIL_APP_PASSWORD = CHUA CO
```

Source đã có code gửi email nhưng terminal chạy backend không có biến môi trường.

Sau khi nạp đủ biến và chạy lại backend trong cùng terminal,
người phát triển xác nhận đã nhận được Gmail thông báo.

---

## 10. Cấu hình frontend

Trong terminal frontend:

```powershell
cd frontend
npm install
```

Tạo file:

```text
frontend/.env
```

Nội dung:

```dotenv
VITE_GOOGLE_CLIENT_ID=CLIENT_ID.apps.googleusercontent.com
```

Client ID phải khớp backend.

Địa chỉ hiện tại:

```text
Backend:  http://localhost:8080
Frontend: http://localhost:5173
```

Dùng nhất quán `localhost`.

Đổi sang IP LAN, `127.0.0.1` hoặc cổng khác có thể cần cập nhật CORS và cấu hình Google.

Sau khi sửa `.env`, khởi động lại Vite.

---

## 11. Chạy dự án

Cần hai terminal.

### Terminal 1 — backend

Từ thư mục gốc:

```powershell
cd backend
```

Đặt các biến môi trường cần thiết, sau đó chạy:

```powershell
.\mvnw.cmd spring-boot:run
```

Chờ:

```text
Started BackendApplication ...
```

Kiểm tra không có lỗi MySQL hoặc schema phía trên.

### Terminal 2 — frontend

Từ thư mục gốc:

```powershell
cd frontend
npm run dev
```

Mở:

```text
http://localhost:5173
```

### Dừng và chạy lại

```text
Ctrl + C
```

- Dừng backend cũ trước khi chạy lại.
- Không chạy nhiều backend cùng cổng 8080.
- Nếu Vite chuyển sang cổng khác, kiểm tra tiến trình đang chiếm 5173.
- Giữ terminal backend đã đặt biến môi trường nếu muốn tiếp tục sử dụng email.

Backend khởi động thành công không tự chứng minh SMTP hoạt động,
vì cấu hình hiện tại không kiểm tra kết nối mail lúc khởi động.

---

## 12. Tài khoản demo

Tài khoản demo là dữ liệu local, không tự xuất hiện đầy đủ sau khi clone repository.

| Vai trò | Email | Mật khẩu |
|---|---|---|
| Bệnh nhân demo | test@example.com | 12345678 |
| Admin | admin@medibook.local | Chủ dự án tự quản lý |
| Bác sĩ | Email đã được admin liên kết | Chủ dự án tự quản lý |
| Bệnh nhân kiểm thử email | Gmail thật do người kiểm thử kiểm soát | Local hoặc đăng nhập Google |

Lưu ý:

- `test@example.com` không dùng để kiểm thử nhận email.
- Không sử dụng mật khẩu cá nhân làm mật khẩu demo công khai.
- Không lưu Gmail App Password trong bảng tài khoản demo.
- User đăng nhập Google không nhất thiết có mật khẩu local.
- Không suy đoán mật khẩu admin hoặc bác sĩ từ tài liệu.

---

## 13. Khởi tạo admin

File:

```text
backend/src/main/java/com/medibook/backend/config/AdminInitializer.java
```

Biến môi trường:

```text
MEDIBOOK_ADMIN_EMAIL
MEDIBOOK_ADMIN_PASSWORD
```

Quy tắc:

- Chỉ tạo nếu email chưa tồn tại.
- Không tự nâng quyền tài khoản đã tồn tại.
- Không reset mật khẩu tài khoản cũ.
- Mật khẩu tối thiểu 12 ký tự.
- Mật khẩu không quá 72 byte UTF-8.

Trong terminal backend:

```powershell
$env:MEDIBOOK_ADMIN_EMAIL="admin@medibook.local"

$adminPassword = Read-Host "Nhap mat khau admin" -AsSecureString

$env:MEDIBOOK_ADMIN_PASSWORD = (
    New-Object System.Net.NetworkCredential("", $adminPassword)
).Password

Remove-Variable adminPassword

.\mvnw.cmd spring-boot:run
```

Sau khi tạo xong và dừng backend, có thể xóa biến khởi tạo:

```powershell
Remove-Item Env:MEDIBOOK_ADMIN_EMAIL -ErrorAction SilentlyContinue
Remove-Item Env:MEDIBOOK_ADMIN_PASSWORD -ErrorAction SilentlyContinue
```

Đổi biến môi trường không đổi mật khẩu admin đã tồn tại.

---

## 14. Chuẩn bị dữ liệu demo

Trên database mới:

1. Tạo admin.
2. Đăng nhập admin.
3. Thêm chuyên khoa.
4. Thêm hồ sơ bác sĩ.
5. Thêm khung giờ trong tương lai.
6. Đăng ký tài khoản bệnh nhân.
7. Đăng ký tài khoản riêng dành cho bác sĩ.
8. Admin liên kết tài khoản với hồ sơ bác sĩ.
9. Đăng xuất và đăng nhập lại tài khoản bác sĩ.
10. Dùng tài khoản bệnh nhân riêng để đặt lịch.

Nên sử dụng tài khoản riêng cho mỗi vai trò khi demo.

---

## 15. Vai trò và phân quyền mới nhất

### 15.1. Vai trò lưu trong database

```text
ROLE_PATIENT
ROLE_DOCTOR
ROLE_ADMIN
```

Liên kết bác sĩ hiện thêm ROLE_DOCTOR và giữ vai trò cũ.
Do đó một tài khoản có thể đồng thời có:

```text
ROLE_PATIENT + ROLE_DOCTOR
```

Không tự xóa ROLE_PATIENT bằng SQL.

### 15.2. Quy tắc mới đã cung cấp code

| Tài khoản | Chức năng |
|---|---|
| Bệnh nhân không có vai trò nhân viên | Đặt, xem và hủy lịch của mình |
| Bác sĩ, kể cả có ROLE_PATIENT | Dùng chức năng bác sĩ, không tạo lịch mới |
| Admin, kể cả có ROLE_PATIENT | Dùng chức năng quản trị, không tạo lịch mới |

Nếu tài khoản có cả ADMIN và DOCTOR:

- Không được tạo lịch bệnh nhân.
- Các chức năng admin/bác sĩ vẫn dựa trên vai trò tương ứng.
- Nhãn Navbar ưu tiên hiển thị “Quản trị viên”.

### 15.3. Frontend

`ProtectedRoute` có thêm prop:

```jsx
patientOnly
```

Hai route áp dụng:

```text
/booking
/my-appointments
```

Kết quả dự kiến:

- Khách chưa đăng nhập → `/login`.
- Bác sĩ → `/doctor/appointments`.
- Admin → `/admin/specialties`.
- Bệnh nhân hợp lệ → vào trang.

Navbar ẩn hai mục bệnh nhân đối với bác sĩ/admin.

### 15.4. Backend

`AppointmentService.createAppointment` đọc vai trò từ database và từ chối:

```text
ROLE_DOCTOR
ROLE_ADMIN
```

Chỉ ẩn menu không đủ để bảo vệ API.

### 15.5. Lịch cũ

- Lịch từng được tài khoản bác sĩ đặt vẫn giữ trong database.
- Route frontend mới không cho bác sĩ mở “Lịch hẹn của tôi”.
- Không thêm lệnh cấm xem/hủy lịch cũ trong service ở đợt sửa này.
- Quyền gọi API xem/hủy thực tế còn phụ thuộc controller và SecurityConfig.
- Nếu có lịch thử nghiệm PENDING cần hủy, nên xử lý trước khi đổi giao diện.
- Chưa có màn hình riêng xử lý lịch bệnh nhân cũ của tài khoản đã chuyển thành bác sĩ.

### 15.6. Giới hạn hiện tại

Nút đặt lịch trên Home/Doctors chưa được cập nhật đồng bộ trong đợt sửa mới nhất.

Nếu bác sĩ bấm nút dẫn đến `/booking`, route mới sẽ chuyển về lịch khám.
Cần sửa tiếp hai trang công khai để không hiển thị lời mời đặt lịch không phù hợp.

---

## 16. Các trang giao diện

| Đường dẫn | Chức năng |
|---|---|
| `/` | Trang chủ |
| `/specialties` | Danh sách chuyên khoa |
| `/doctors` | Danh sách bác sĩ |
| `/doctors?specialtyId=...` | Lọc bác sĩ theo chuyên khoa |
| `/register` | Đăng ký |
| `/login` | Đăng nhập |
| `/booking` | Đặt lịch, yêu cầu tài khoản bệnh nhân hợp lệ theo bản mới |
| `/booking?doctorId=...` | Chọn sẵn bác sĩ |
| `/my-appointments` | Lịch hẹn bệnh nhân |
| `/doctor/appointments` | Lịch khám bác sĩ |
| `/admin/specialties` | Quản lý chuyên khoa |
| `/admin/doctors` | Quản lý bác sĩ và liên kết tài khoản |
| `/admin/appointment-slots` | Quản lý khung giờ |

Trang chưa tạo:

```text
/payment/vnpay-return
```

---

## 17. API chính

### Xác thực

| Method | API | Chức năng |
|---|---|---|
| POST | `/api/auth/register` | Đăng ký |
| POST | `/api/auth/login` | Đăng nhập mật khẩu |
| POST | `/api/auth/google` | Đăng nhập Google |

### Danh mục

| Method | API |
|---|---|
| GET | `/api/specialties` |
| GET | `/api/doctors` |
| GET | `/api/appointment-slots` |

Các thao tác ghi dữ liệu danh mục dành cho ADMIN theo cấu hình backend.

### Bệnh nhân

| Method | API | Chức năng |
|---|---|---|
| GET | `/api/appointments/me` | Lịch của người đăng nhập |
| POST | `/api/appointments` | Đặt lịch |
| POST | `/api/appointments/{id}/cancel` | Hủy lịch |

Body đặt lịch:

```json
{
  "slotId": 1
}
```

### Bác sĩ

| Method | API | Chức năng |
|---|---|---|
| GET | `/api/doctor/appointments` | Xem lịch bác sĩ |
| POST | `/api/doctor/appointments/{id}/confirm` | Xác nhận |
| POST | `/api/doctor/appointments/{id}/complete` | Hoàn thành |

### Liên kết tài khoản bác sĩ

```text
POST /api/admin/doctors/{doctorId}/account
```

Body:

```json
{
  "email": "email-tai-khoan-bac-si@example.com"
}
```

### Thanh toán

Chưa có API thanh toán hoàn chỉnh.

### Swagger / OpenAPI

```text
http://localhost:8080/swagger-ui/index.html
http://localhost:8080/v3/api-docs
```

---

## 18. Nghiệp vụ lịch khám

### 18.1. Trạng thái

```text
PENDING → CONFIRMED → COMPLETED
```

Hoặc:

```text
PENDING → CANCELLED
```

### 18.2. Đặt lịch

- User tồn tại và ACTIVE.
- Có ROLE_PATIENT.
- Theo bản mới: không có ROLE_DOCTOR hoặc ROLE_ADMIN.
- Khung giờ tồn tại.
- Khung giờ AVAILABLE.
- Chưa đến giờ bắt đầu.
- Khóa khung giờ trong transaction.
- Tạo lịch PENDING.
- Đổi khung giờ thành BOOKED.

### 18.3. Hủy lịch

- Chỉ chủ lịch được hủy.
- CANCELLED trả lại dữ liệu hiện có.
- Chỉ chuyển PENDING → CANCELLED.
- Chưa đến giờ bắt đầu.
- Khung giờ phải BOOKED.
- Khóa lịch hẹn trước, sau đó khóa khung giờ.
- Khung giờ chuyển AVAILABLE.
- Giữ lịch sử lịch hẹn.
- Không gửi lại email khi hủy lại lịch đã CANCELLED.

### 18.4. Bác sĩ xác nhận

- Tài khoản liên kết đúng hồ sơ bác sĩ.
- Tài khoản ACTIVE và có ROLE_DOCTOR.
- Lịch thuộc bác sĩ hiện tại.
- Chỉ PENDING → CONFIRMED.
- Khung giờ phải BOOKED.

### 18.5. Hoàn thành khám

- Chỉ CONFIRMED → COMPLETED.
- Chỉ đến hoặc qua giờ kết thúc.
- Khung giờ vẫn BOOKED.
- Không mở lại khung giờ khi hoàn thành.

### 18.6. Thanh toán

Trong thời gian tạm hoãn VNPAY:

- Bác sĩ vẫn xác nhận mà chưa kiểm tra thanh toán.
- Không đánh dấu giao dịch giả.
- Không coi lịch đã trả tiền.

---

## 19. Email thông báo

### 19.1. File liên quan

```text
service/EmailNotificationService.java
service/AppointmentService.java
service/DoctorAppointmentService.java
```

### 19.2. Sự kiện gửi thư

| Sự kiện | Người nhận |
|---|---|
| Đặt lịch thành công | Bệnh nhân |
| Hủy lịch hợp lệ | Bệnh nhân |
| Bác sĩ xác nhận | Bệnh nhân |
| Bác sĩ hoàn thành | Bệnh nhân |

Bản AppointmentService mới nhất sử dụng tiêu đề có dấu cho email đặt/hủy.
Email bác sĩ xác nhận/hoàn thành vẫn theo mẫu đã cung cấp trước đó.

### 19.3. Cách xử lý

- Service nghiệp vụ chuẩn bị nội dung email.
- `sendAfterCommit` đăng ký callback gửi sau commit khi có transaction synchronization.
- Nếu transaction rollback, callback sau commit không chạy.
- Nếu gọi ngoài transaction, implementation hiện tại gửi trực tiếp.
- Lỗi gửi SMTP được bắt và ghi log.
- Không đổi lịch đã lưu thành thất bại vì gửi email lỗi.
- Hủy lại lịch CANCELLED không đăng ký email mới.

Bản AppointmentService mới nhất còn bắt lỗi chuẩn bị nội dung thông báo
để không hủy thao tác nghiệp vụ vì lỗi tạo email.

### 19.4. Giới hạn triển khai

- Gửi SMTP hiện thực hiện đồng bộ trong callback sau commit.
- Chưa có hàng đợi email.
- Chưa có bảng outbox.
- Chưa có cơ chế retry bền vững.
- Không bảo đảm giao email đúng một lần khi tiến trình bị dừng hoặc kết nối có kết quả không rõ.
- Log gửi thành công cho biết thao tác gửi SMTP không báo lỗi; không bảo đảm thư nằm trong Inbox.
- Chưa có email nhắc lịch tự động.
- Chưa có email quên mật khẩu.
- Chưa gửi bệnh án hoặc đơn thuốc.

### 19.5. Kiểm tra email

Dùng Gmail thật do người kiểm thử kiểm soát.

Có thể sử dụng Gmail nhận khác Gmail gửi để dễ phân biệt.

Tìm trong Gmail:

```text
in:anywhere subject:MediBook
```

Kiểm tra Inbox, Spam và các tab phân loại.

### 19.6. Log cần quan sát

Thành công:

```text
Email notification sent to ...
```

Thất bại:

```text
Failed to send email notification ...
```

Nếu không có cả hai:

- Kiểm tra `app.mail.enabled`.
- Kiểm tra biến môi trường terminal backend.
- Kiểm tra source đã lưu.
- Kiểm tra backend đã restart.
- Kiểm tra thao tác có thực sự chuyển trạng thái mới không.

Không kết luận ngay rằng Gmail làm mất thư.

---

## 20. Đăng nhập Google

### Luồng

```text
Frontend hiển thị nút Google
    ↓
Google trả ID token
    ↓
Frontend gọi /api/auth/google
    ↓
Backend xác minh token
    ↓
Tìm hoặc tạo user
    ↓
Cấp JWT MediBook
```

Google ID token không thay thế JWT MediBook khi gọi API nghiệp vụ.

### Cấu hình

- OAuth Client loại Web application.
- Origin hiện tại:

```text
http://localhost:5173
```

- Client ID frontend khớp backend.
- Thêm test users nếu cấu hình Google yêu cầu.
- Không đưa Google Client Secret vào React.

### Quy tắc đã triển khai

- Kiểm tra token Google.
- Yêu cầu email đã xác minh.
- User mới nhận ROLE_PATIENT.
- User chỉ Google không có mật khẩu local.
- User cũ giữ mật khẩu và vai trò.
- User không ACTIVE bị từ chối.
- ADMIN không đăng nhập Google theo quy tắc hiện tại.
- Không âm thầm đổi providerId sang tài khoản Google khác.

### File liên quan

```text
service/GoogleTokenVerifier.java
service/GoogleAuthService.java
controller/AuthController.java
config/SecurityConfig.java

frontend/src/api/googleAuthApi.js
frontend/src/components/GoogleLoginButton.jsx
frontend/src/pages/Login.jsx
```

---

## 21. VNPAY — tạm hoãn

### 21.1. Quyết định hiện tại

Tạm bỏ qua phần tích hợp để tiếp tục hoàn thiện frontend và các chức năng còn lại.

Người phát triển sẽ hỏi giảng viên về cách xử lý,
sau đó cung cấp lại thông tin để tiếp tục.

Không xóa code hoặc database thanh toán đã tạo.

### 21.2. Phần đã có

```text
entity/PaymentStatus.java
entity/PaymentTransaction.java
repository/PaymentTransactionRepository.java
```

Đã được hướng dẫn thêm:

```text
config/VnpayProperties.java
service/VnpayGateway.java
```

Chưa có xác nhận riêng đầy đủ về compile/kiểm thử hai class VNPAY.
Dù tạm hoãn, các file còn trong source phải không làm project lỗi build.

### 21.3. Bảng đã xác nhận

```text
payment_transactions
```

Các cột:

```text
id
appointment_id
txn_ref
amount
currency
provider
status
gateway_transaction_no
bank_code
response_code
gateway_transaction_status
payment_url
expires_at
paid_at
requires_review
review_reason
created_at
updated_at
```

Chỉ mục quan sát qua phpMyAdmin:

```text
PRIMARY: id
UNIQUE: uk_payment_txn_ref
INDEX: idx_payment_appointment_status
```

### 21.4. Thiết kế dự kiến

- 100.000 VNĐ/lịch.
- Lưu amount: `100000`.
- Gửi `vnp_Amount`: `10000000`.
- Giao dịch: PENDING, SUCCESS, FAILED.
- Một lịch có thể có nhiều lần thử.
- Service phải chống nhiều giao dịch chờ không phù hợp.
- Thanh toán SUCCESS vẫn giữ lịch PENDING.
- Bác sĩ xác nhận sau thanh toán khi luồng này được nối đầy đủ.

### 21.5. Chưa triển khai

- API tạo giao dịch.
- API IPN.
- API trạng thái.
- Trang Return URL.
- Nút thanh toán.
- Điều kiện thanh toán trong xác nhận.
- Điều kiện hủy có giao dịch chờ/đã trả tiền.
- Địa chỉ công khai nhận IPN.
- Kiểm thử Sandbox.
- Hoàn tiền tự động.

### 21.6. Nguyên tắc khi làm tiếp

- Không giả SUCCESS bằng SQL.
- Không coi đóng tab hoặc hết hạn là đủ kết luận FAILED.
- Xác minh chữ ký, mã website, mã giao dịch, số tiền và kết quả.
- IPN xử lý idempotent.
- Không tin riêng dữ liệu Return URL.
- Phát hiện tham số VNPAY bị lặp.
- Không giải mã query nhiều lần.
- Trường hợp đến muộn cần đối soát.
- Giữ thứ tự khóa nghiệp vụ thống nhất.

### 21.7. Vướng mắc đã gặp

Trang:

```text
[https://sandbox.vnpayment.vn/devreg/](https://sandbox.vnpayment.vn/devreg/)
```

Đã xuất hiện 404 trên máy phát triển.

Chưa xác định nguyên nhân.
Chưa xác nhận nhận được khóa tích hợp.
Không có đủ bằng chứng để kết luận do nhà mạng.

Không dùng khóa tìm ngẫu nhiên từ GitHub.

---

## 22. Checklist kiểm thử

### 22.1. Phân quyền frontend mới

- [ ] Bác sĩ không thấy menu Đặt lịch.
- [ ] Bác sĩ không thấy menu Lịch hẹn của tôi.
- [ ] Bác sĩ mở `/booking` được chuyển về lịch khám.
- [ ] Bác sĩ mở `/my-appointments` được chuyển về lịch khám.
- [ ] Admin mở trang bệnh nhân được chuyển về trang quản trị.
- [ ] Khách mở `/booking` được chuyển đến đăng nhập.
- [ ] Bệnh nhân vẫn truy cập được Booking.
- [ ] Menu trên màn hình nhỏ mở/đóng đúng.

### 22.2. Backend

- [ ] Bác sĩ gọi API tạo lịch nhận 403.
- [ ] Admin gọi API tạo lịch nhận 403.
- [ ] Bệnh nhân đặt lịch bình thường.
- [ ] Không hủy được lịch của người khác.
- [ ] Bác sĩ không xác nhận lịch của bác sĩ khác.
- [ ] Người dùng thường không gọi được API admin.
- [ ] Tài khoản chưa liên kết bác sĩ bị từ chối API bác sĩ.
- [ ] Hai bệnh nhân đặt cùng slot chỉ một người thành công.
- [ ] Không đặt được slot BOOKED.
- [ ] Không đặt được slot đã bắt đầu.
- [ ] Hủy hợp lệ mở slot.
- [ ] Hoàn thành không mở slot.

### 22.3. Email

- [ ] Nhận email đặt lịch.
- [ ] Nhận email hủy.
- [ ] Nhận email xác nhận.
- [ ] Nhận email hoàn thành.
- [ ] Hủy lại không gửi email lần hai.
- [ ] SMTP lỗi không làm mất lịch đã lưu.
- [ ] Transaction rollback không phát email thành công.
- [ ] Nội dung và người nhận đúng.

### 22.4. Google

- [ ] Đăng nhập lần đầu tạo đúng tài khoản.
- [ ] Đăng nhập lần hai không tạo trùng.
- [ ] Vai trò cũ được giữ.
- [ ] Đăng nhập mật khẩu vẫn chạy.
- [ ] Tài khoản không ACTIVE bị từ chối.

### 22.5. Frontend

- [ ] Loading rõ ràng.
- [ ] Phân biệt lỗi tải dữ liệu và danh sách trống.
- [ ] Không mất bảng dữ liệu không cần thiết khi thao tác lỗi.
- [ ] Chặn bấm lặp trong lúc xử lý.
- [ ] Nhãn trạng thái thống nhất.
- [ ] Số lịch “đang hiệu lực” không tính nhầm COMPLETED.
- [ ] Điều kiện hủy cập nhật khi thời gian trôi qua.
- [ ] Home/Doctors hiển thị lời mời đặt lịch phù hợp vai trò.
- [ ] Không vỡ bố cục trên màn hình nhỏ.

Các điểm cuối là nội dung cần rà, chưa khẳng định đã sửa.

---

## 23. Lỗi thường gặp

### MySQL connection refused

Kiểm tra dịch vụ và cổng:

```powershell
Test-NetConnection -ComputerName localhost -Port 3306
```

### Access denied for user

Kiểm tra tài khoản và mật khẩu MySQL.
Không nhầm với tài khoản MediBook.

### Unable to determine Dialect without JDBC metadata

Nếu có lỗi kết nối phía trên, sửa kết nối trước.
Không thêm dialect để che lỗi MySQL.

### Port 8080 already in use

Dừng backend cũ trước khi chạy lại.

### Failed to fetch

Kiểm tra backend, địa chỉ API, cổng và CORS.

### 401

Token thiếu, hết hạn hoặc không hợp lệ.
Đăng xuất và đăng nhập lại.

### 403

Kiểm tra vai trò và quyền sở hữu dữ liệu.
Bác sĩ/admin đặt lịch theo quy tắc mới phải bị từ chối.

Không mở `permitAll()` toàn bộ API để sửa lỗi.

### Google lỗi origin

Kiểm tra Client ID và origin thực tế.
Khởi động lại Vite sau khi sửa `.env`.

### Không thấy email

Kiểm tra theo thứ tự:

1. Có thao tác nghiệp vụ mới không?
2. Backend có chạy source mới không?
3. MAIL_ENABLED có true không?
4. Username và App Password có trong terminal backend không?
5. Log gửi thư có thành công hoặc lỗi không?
6. Kiểm tra Gmail người nhận.

### Đã đặt biến nhưng backend chưa nhận

Dừng backend và chạy lại trong terminal có biến môi trường.
Không đặt ở terminal khác rồi mong backend đang chạy tự cập nhật.

### Lombok báo thiếu getter/setter

Kiểm tra extension Java, Maven reload và kết quả compile.
Không tự thêm dependency trùng.

---

## 24. Bảo mật và GitHub

Không commit:

- Mật khẩu MySQL.
- JWT secret.
- Gmail App Password.
- VNPAY HashSecret.
- Google Client Secret.
- Token đăng nhập.
- Database dump chứa thông tin cá nhân.
- File ghi tài khoản local nhạy cảm.

Google Client ID khác Google Client Secret.

Nếu secret đã lộ:

- Thu hồi hoặc thay secret.
- Không chỉ xóa ở commit mới rồi tiếp tục dùng secret cũ.

### Gợi ý `.gitignore`

Ghép với file hiện có:

```gitignore
# Backend
backend/target/

# Frontend
frontend/node_modules/
frontend/dist/

# Local environment
.env
.env.*
!.env.example

frontend/.env
frontend/.env.*
!frontend/.env.example

# Local configuration
backend/src/main/resources/application-local.properties
TAIKHOAN-LOCAL.md

# Logs
*.log

# OS
.DS_Store
Thumbs.db
```

Giữ `application.properties` dạng cấu hình mẫu dùng biến môi trường.

### File frontend mẫu

```text
frontend/.env.example
```

```dotenv
VITE_GOOGLE_CLIENT_ID=YOUR_GOOGLE_WEB_CLIENT_ID
```

### Kiểm tra trước khi push

```powershell
git status
git diff
```

Sau khi đã rà secret:

```powershell
git add .
git diff --cached
```

Nếu nội dung staged an toàn:

```powershell
git commit -m "Update email notifications and patient doctor access rules"
git push
```

---

## 25. Thứ tự làm tiếp

1. Lưu bốn file phân quyền mới nhất.
2. Restart backend trong terminal có cấu hình email.
3. Kiểm tra bác sĩ/admin không tạo được lịch mới.
4. Kiểm tra bệnh nhân vẫn đặt/hủy và nhận email.
5. Kiểm thử đủ bốn loại email.
6. Hoàn thiện Home/Doctors/Booking theo vai trò.
7. Sửa các điểm frontend còn chưa nhất quán.
8. Kiểm thử nghiệp vụ và bảo mật.
9. Chuẩn bị dữ liệu demo.
10. Hoàn thiện báo cáo, sơ đồ và bảng kiểm thử.
11. Chuẩn bị slide, kịch bản demo.
12. Tiếp tục VNPAY sau khi có hướng dẫn của giảng viên.
13. Deploy/AI nếu thuộc yêu cầu hoặc còn thời gian.

Không thêm hồ sơ bệnh án, đơn thuốc, chat hoặc dashboard mới
nếu chưa xác nhận chúng thuộc phạm vi đồ án.

---

## 26. Mốc bàn giao để tiếp tục

Cập nhật ngày 10/11/2026, khoảng 02:43.

### Đã xác nhận gần nhất

Người phát triển đã nhận được Gmail thông báo.

Nguyên nhân email chưa gửi trước đó:
terminal backend không có các biến môi trường mail.

### Thay đổi mới nhất đã cung cấp

```text
ProtectedRoute.jsx
App.jsx
Navbar.jsx
AppointmentService.java
```

Mục tiêu:
tách chức năng bệnh nhân khỏi tài khoản bác sĩ/admin.

### Chưa xác nhận

- Đã áp dụng đủ bốn file.
- Backend build/run sau thay đổi.
- Frontend sau thay đổi.
- Chặn tạo lịch bác sĩ/admin bằng API.
- Luồng bệnh nhân và email không bị ảnh hưởng.

### File giữ nguyên trong đợt này

```text
Booking.jsx
MyAppointments.jsx
DoctorAppointments.jsx
DoctorAppointmentService.java
EmailNotificationService.java
SecurityConfig.java
```

### Lưu ý triển khai

- Không xóa ROLE_PATIENT bằng SQL.
- Không xóa lịch cũ.
- Không tạo lại project.
- Không đổi dependency.
- Không yêu cầu gửi lại các file đã có nếu người phát triển chưa sửa thêm.
- VNPAY tiếp tục tạm hoãn.
- Ưu tiên kiểm tra đợt sửa hiện tại trước khi thay đổi thêm frontend.

---

## 27. Mục đích sử dụng

MediBook phục vụ học tập và trình bày đồ án.

Chưa công bố sẵn sàng vận hành cho cơ sở y tế thật.

Trước khi sử dụng thực tế cần rà soát thêm bảo mật, dữ liệu cá nhân,
sao lưu, vận hành, thanh toán và các yêu cầu liên quan.