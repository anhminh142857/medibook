# Hướng dẫn chạy và demo MediBook

Tài liệu dành cho người mới mở repository trên Windows. Tất cả lệnh dưới đây là **hướng dẫn để người trình bày tự chạy**; chúng chưa được chạy trong lượt viết tài liệu. Các thao tác đăng ký, đặt/hủy lịch, tạo payment, quản lý admin và seed đều có thể ghi dữ liệu. Chỉ làm trên database thử nghiệm do bạn kiểm soát.

> **Cảnh báo quan trọng:** Không trỏ `ddl-auto=update`, `MEDIBOOK_ADMIN_*` hoặc seeder vào database đang có dữ liệu mà chưa sao lưu. SePay chỉ dùng Sandbox, không chuyển tiền thật. Giảng viên yêu cầu VNPay hoặc MoMo trong thông tin hiện có; cần xác nhận SePay được chấp thuận.

## Mục lục

1. [Chuẩn bị](#1-chuẩn-bị)
2. [Lấy và mở source](#2-lấy-và-mở-source)
3. [Tạo database thử nghiệm và hiểu seeder](#3-tạo-database-thử-nghiệm-và-hiểu-seeder)
4. [Cấu hình ứng dụng](#4-cấu-hình-ứng-dụng)
5. [Chạy phần cơ bản](#5-chạy-phần-cơ-bản)
6. [Bật từng tích hợp](#6-bật-từng-tích-hợp)
7. [Tài khoản demo](#7-tài-khoản-demo)
8. [URL và công cụ khi trình bày](#8-url-và-công-cụ-khi-trình-bày)
9. [Kịch bản demo 10–15 phút](#9-kịch-bản-demo-1015-phút)
10. [API chính](#10-api-chính)
11. [Xem database an toàn](#11-xem-database-an-toàn)
12. [Câu hỏi giảng viên có thể hỏi](#12-câu-hỏi-giảng-viên-có-thể-hỏi)
13. [Test và xử lý lỗi](#13-test-và-xử-lý-lỗi)
14. [Checklist trước ngày demo](#14-checklist-trước-ngày-demo)

## 1. Chuẩn bị

### Công cụ và vai trò của từng dịch vụ

| Công cụ/dịch vụ | Dùng cho | Bắt buộc? | Có thể bỏ qua khi |
|---|---|---:|---|
| JDK 17 trở lên | Biên dịch/chạy backend; `pom.xml` đặt `java.version=17` | Có | Không thể chạy backend nếu thiếu JDK phù hợp |
| Maven Wrapper | Chạy Maven theo cấu hình repo; wrapper tải Maven nếu máy chưa có | Có cho build/run backend | Không cần cài Maven toàn cục |
| Node.js + npm | Frontend React/Vite | Có cho frontend | Không cần chạy nếu chỉ xem source/backend |
| MySQL | Database của backend | Có cho ứng dụng đầy đủ | Không có DB thì không thể kiểm tra luồng thật; không có H2 thay thế trong dependencies |
| Google Identity Services + Google project | Google login | Không | Bỏ qua Google login; email/mật khẩu vẫn có code |
| Gmail SMTP và App Password | Email đặt/hủy/xác nhận/hoàn thành | Không | Mail tắt mặc định |
| SePay Sandbox | Payment và xác nhận lịch theo quy tắc hiện tại | Không cho xem catalog/đặt lịch; có cho luồng xác nhận bác sĩ | Có thể xem trang/thử đặt lịch nhưng không hoàn tất luồng xác nhận qua UI nếu không có payment SePay thành công |
| ngrok | Cho SePay gọi webhook vào backend local | Chỉ cho webhook từ ngoài vào máy | Đọc/tải lại payment không cần ngrok; gọi webhook local trực tiếp không chứng minh kết nối công khai |
| Ollama và model `qwen2.5:3b` | Trợ lý AI local | Không | AI tắt mặc định; các luồng chính khác không cần AI |
| MySQL Workbench hoặc công cụ SQL tương đương | Tạo schema và xem dữ liệu | Công cụ GUI không bắt buộc | Có thể dùng client MySQL khác |

Kiểm tra phiên bản trong terminal khi bạn chuẩn bị máy (các lệnh này **không được chạy trong lượt viết tài liệu**):

```powershell
java -version
node --version
npm --version
git --version
```

`backend/pom.xml` khai báo Java target 17; Maven Wrapper khai báo Maven 3.10.0. `frontend/package.json` có lockfile nhưng không khóa phiên bản Node/npm bằng trường `engines`; chọn Node/npm tương thích với Vite trong lockfile. Phiên bản MySQL server không được pin trong manifest.

## 2. Lấy và mở source

1. Lấy repository từ nguồn do nhóm cung cấp.
2. Mở thư mục gốc `medibook` bằng VS Code.
3. Thay đường dẫn ví dụ sau bằng vị trí thực tế của bạn; không dùng đường dẫn máy của người phát triển:

```powershell
$Repo = "C:\DuongDan\den\medibook"
Set-Location $Repo
Get-Location
```

4. Dùng các terminal riêng:

| Terminal | Thư mục làm việc | Tiến trình |
|---|---|---|
| MySQL | Không phụ thuộc terminal dự án | MySQL service/Workbench |
| Backend | `$Repo\backend` | Spring Boot |
| Frontend | `$Repo\frontend` | Vite dev server |
| ngrok | Bất kỳ thư mục nào có `ngrok` | Chỉ khi nhận webhook SePay từ bên ngoài |
| Ollama | Bất kỳ thư mục nào có Ollama | Chỉ khi bật AI |

Không khởi động backend lần thứ hai nếu một backend đã chạy. Nếu không rõ terminal nào sở hữu process đang dùng cổng, kiểm tra trước; không kết thúc process lạ.

## 3. Tạo database thử nghiệm và hiểu seeder

### 3.1. Chọn database đích trước khi khởi động

1. Dùng MySQL Workbench hoặc công cụ MySQL mà bạn kiểm soát.
2. Nếu cần database sạch, tạo **database thử nghiệm mới**. Ví dụ dưới đây chỉ tạo schema tên `medibook_demo`; không xóa schema cũ:

```sql
CREATE DATABASE medibook_demo
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

3. Kiểm tra schema đang chọn bằng câu lệnh chỉ đọc:

```sql
SELECT DATABASE();
```

Cấu hình mẫu `backend/src/main/resources/application-example.properties` trỏ tới `jdbc:mysql://localhost:3306/medibook`; đây không phải xác nhận database local đang dùng. Nếu muốn dùng seeder, source bắt buộc tên database hiện tại chính xác là `medibook_demo`. Không bật seeder trên schema khác. Không `DROP`, reset hoặc import dữ liệu vào database đang dùng bởi ứng dụng khác.

### 3.2. Tác động của Hibernate và initializer

- `DataInitializer` chạy lúc khởi động, tạo ba role còn thiếu.
- `ddl-auto=update` trong cấu hình mẫu để Hibernate cập nhật schema; nó không phải bộ migration có version và không thay thế sao lưu.
- `AdminInitializer` chỉ tạo admin nếu cả `MEDIBOOK_ADMIN_EMAIL` và `MEDIBOOK_ADMIN_PASSWORD` được đặt. Nó tác động vào datasource hiện tại. Nếu email đã tồn tại, nó không đổi password/role.
- `DataSeeder` mặc định tắt (`MEDIBOOK_SEED_ENABLED=false`). Khi bật, seeder kiểm tra `SELECT DATABASE()` và dừng nếu không phải `medibook_demo`; nó có thể tạo user, chuyên khoa, bác sĩ, slot và lịch mẫu.
- Seeder dùng `MEDIBOOK_SEED_PASSWORD` cho các tài khoản **mới được tạo**; tài khoản đã tồn tại không bị đổi mật khẩu. Password yêu cầu tối thiểu 12 ký tự và tối đa 72 byte UTF-8.
- Seeder không tạo payment và không gửi email. Nó không xóa/reset database, nhưng vẫn ghi thêm dữ liệu vào database đích.
- Bộ ngày seed trong source là 18/11/2026–31/12/2026. Nếu ngày hiện tại sau 31/12/2026, seeder sẽ dừng. Từ ngày 25/11/2026, nó bỏ qua các slot tương lai đã qua và có các lịch lịch sử được dự định để demo; sự tồn tại thực tế còn phụ thuộc database và điều kiện slot.

Chỉ bật seeder trên database thử nghiệm riêng, có thể bỏ đi, sau khi tự kiểm tra database URL và tên schema. Không bật nó chỉ để “xem thử” trên dữ liệu đang chạy.

## 4. Cấu hình ứng dụng

### 4.1. Tạo file cấu hình local mà không ghi đè

`application.properties` có thể chứa cấu hình nhạy cảm. Không gửi file này lên chat/repository và không ghi giá trị của nó vào ảnh demo.

Từ thư mục gốc, kiểm tra file đã tồn tại chưa:

```powershell
Test-Path .\backend\src\main\resources\application.properties
```

- Nếu kết quả là `False`, có thể tạo cấu hình local từ mẫu:

```powershell
Copy-Item `
  .\backend\src\main\resources\application-example.properties `
  .\backend\src\main\resources\application.properties
```

- Nếu kết quả là `True`, **không chạy lệnh copy trên** vì có thể ghi đè cấu hình local. Mở file cục bộ bằng editor, xem xét thủ công và giữ nguyên giá trị riêng tư. Không cần gửi nội dung file cho người khác.

Mẫu đã đặt backend port 8080, database mặc định `medibook`, JPA `update`, mail/payment/AI mặc định tắt. Nếu dùng seeder, cấu hình URL phải trỏ vào database thử nghiệm `medibook_demo`; không sửa cấu hình đang dùng chung.

Frontend có `.env.example` chứa placeholder Google Client ID. Khi cần Google login, chỉ tạo `.env` nếu file chưa tồn tại:

```powershell
if (-not (Test-Path .\frontend\.env)) {
    Copy-Item .\frontend\.env.example .\frontend\.env
}
```

Mở file `.env` local và chỉ thay `VITE_GOOGLE_CLIENT_ID` bằng **Web Client ID công khai** do chủ dự án quản lý. Không đưa Client Secret/API key/JWT/SMTP/SePay secret vào biến `VITE_*`.

Lưu ý: cấu hình mẫu bật `spring.jpa.show-sql=true`, nên backend có thể ghi câu SQL vào log. Không chia sẻ log chứa thông tin môi trường/người dùng khi không cần thiết.

### 4.2. Bảng cấu hình

Các tên dưới đây được đối chiếu với `application-example.properties` và `@Value` trong source. “Ví dụ” cố ý dùng giá trị mẫu hoặc placeholder; không phải thông tin tài khoản thật.

| Property / biến môi trường | Dịch vụ | Bắt buộc khi nào | Ví dụ an toàn / mặc định trong mẫu | Lấy từ đâu / kiểm tra |
|---|---|---|---|---|
| `spring.datasource.url` | MySQL | Mọi backend chạy đầy đủ | `jdbc:mysql://localhost:3306/medibook` | Chọn schema thử nghiệm; có thể xác nhận tên hiện hành bằng `SELECT DATABASE()` |
| `MEDIBOOK_DB_USERNAME` | MySQL | Mọi backend | Mẫu mặc định `root`; thay theo MySQL local | Tên user DB, không phải mật khẩu |
| `MEDIBOOK_DB_PASSWORD` | MySQL | Mọi backend cần đăng nhập MySQL | `<nhập-trong-terminal-backend>` | Cấu hình từ quản trị MySQL; không in ra terminal |
| `MEDIBOOK_JWT_SECRET` → `app.jwt.secret` | Ký JWT | Mọi backend | `<secret-ngẫu-nhiên-riêng-ít-nhất-32-byte>` | Tạo bằng password manager/nguồn ngẫu nhiên tin cậy; không commit/chia sẻ |
| `app.jwt.expiration-ms` | JWT | Mọi backend | Mẫu: `86400000` ms | Giá trị cấu hình không phải secret |
| `GOOGLE_CLIENT_ID` → `app.google.client-id` | Google backend | Google login | `<Google-Web-Client-ID>` | Google Cloud project của chủ dự án |
| `VITE_GOOGLE_CLIENT_ID` | Google frontend | Hiển thị/khởi tạo nút Google | Cùng Web Client ID công khai | `frontend/.env`; không phải Client Secret |
| `MEDIBOOK_MAIL_ENABLED` → `app.mail.enabled` | Spring Mail | Chỉ khi gửi email | Mặc định `false` | Đặt `true` trong process backend sau khi chuẩn bị SMTP |
| `spring.mail.host`, `spring.mail.port` | SMTP | Khi mail bật | Mẫu `smtp.gmail.com`, `587` | Mẫu dùng Gmail SMTP và STARTTLS |
| `MEDIBOOK_MAIL_USERNAME` | Gmail SMTP username | Khi mail bật | `<địa-chỉ-Gmail-gửi>` | Tài khoản Gmail chủ dự án |
| `MEDIBOOK_MAIL_APP_PASSWORD` | Gmail SMTP password | Khi mail bật | `<Gmail-App-Password>` | Cấu hình App Password của tài khoản được phép; giữ riêng |
| `SEPAY_PAYMENT_ENABLED` → `app.sepay.payment-enabled` | Tạo payment Sandbox | Khi muốn tạo thông tin payment | Mặc định `false` | Chỉ bật sau khi đã chọn đúng môi trường thử nghiệm |
| `SEPAY_PAYMENT_AMOUNT` → `app.sepay.payment-amount` | SePay | Khi tạo payment | Mẫu `100000` VND | Giá trị backend dùng; client không quyết định số tiền |
| `SEPAY_PAYMENT_EXPIRE_MINUTES` → `app.sepay.payment-expire-minutes` | SePay | Khi tạo payment | Mẫu `15`; code chấp nhận 1–60 phút | Cấu hình backend |
| `SEPAY_RECEIVER_ACCOUNT` → `app.sepay.receiver-account` | SePay | Tạo payment và kiểm tra webhook | `<tài-khoản-nhận-Sandbox>` | Lấy đúng tài khoản thử nghiệm từ cấu hình nhà cung cấp; không dùng tài khoản nhận tiền thật |
| `SEPAY_RECEIVER_GATEWAY` → `app.sepay.receiver-gateway` | SePay | Tạo payment và kiểm tra webhook | `<mã-ngân-hàng-Sandbox>` | Đối chiếu đúng giá trị provider gửi trong webhook |
| `SEPAY_WEBHOOK_ENABLED` → `app.sepay.webhook-enabled` | Webhook | Khi nhận webhook | Mặc định `false` | Chỉ bật khi đã đặt key và kiểm tra đúng endpoint |
| `SEPAY_WEBHOOK_API_KEY` → `app.sepay.webhook-api-key` | Webhook | Khi nhận webhook | `<khóa-riêng-giữa-provider-và-backend>` | Tạo/đặt theo cấu hình được provider hỗ trợ; gửi trong header `Authorization: Apikey ...` |
| `MEDIBOOK_AI_ENABLED` → `app.ai.enabled` | AI | Khi dùng widget AI | Mặc định `false` | Bật sau khi Ollama/model sẵn sàng |
| `spring.ai.ollama.base-url` | Ollama | Khi AI bật | `http://localhost:11434` | Mẫu local; endpoint thực tế cần xác nhận trên máy |
| `spring.ai.ollama.chat.model` | Ollama | Khi AI bật | `qwen2.5:3b` | Model cấu hình sẵn; source đặt pull strategy `never`, vì vậy không tự tải model |
| `MEDIBOOK_ADMIN_EMAIL`, `MEDIBOOK_ADMIN_PASSWORD` | Tạo admin | Chỉ khi cần tạo admin mới lúc startup | `<email-thử-nghiệm>`, `<password-riêng-tối-thiểu-12-ký-tự>` | Source không đưa các biến này vào application-example; `AdminInitializer` bỏ qua nếu email đã tồn tại |
| `MEDIBOOK_SEED_ENABLED`, `MEDIBOOK_SEED_PASSWORD` | Seed demo | Chỉ trong database `medibook_demo` cô lập | Mặc định `false`, password `<tối-thiểu-12-ký-tự>` | Chỉ bật có chủ đích; không chạy trên database ứng dụng đang dùng |

#### Đặt secret vào đúng PowerShell backend

Environment variable đặt trong một terminal không tự truyền ngược sang terminal khác. Hãy đặt chúng trong đúng PowerShell sẽ chạy `.\mvnw.cmd spring-boot:run`, rồi khởi động backend từ terminal đó. Secret không nên gõ trực tiếp thành literal trong lệnh (vì history) hoặc in ra để kiểm tra.

Có thể dùng helper dưới đây để nhập secret không echo ra màn hình và chỉ đặt cho process/terminal hiện tại:

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

Ví dụ nhập các secret cần cho backend cơ bản; từng lệnh sẽ hỏi giá trị:

```powershell
Set-SecretProcessVariable MEDIBOOK_DB_PASSWORD
Set-SecretProcessVariable MEDIBOOK_JWT_SECRET
```

Chỉ nếu bật các tích hợp tương ứng mới nhập thêm:

```powershell
Set-SecretProcessVariable MEDIBOOK_MAIL_APP_PASSWORD
Set-SecretProcessVariable SEPAY_RECEIVER_ACCOUNT
Set-SecretProcessVariable SEPAY_WEBHOOK_API_KEY
Set-SecretProcessVariable MEDIBOOK_ADMIN_PASSWORD
Set-SecretProcessVariable MEDIBOOK_SEED_PASSWORD
```

Không cần đặt mọi biến trong mọi lần chạy. Đặt biến email/payment/AI enabled thành `true` chỉ khi đã cấu hình cả phần liên quan. Để xác nhận một secret có mặt mà không in giá trị:

```powershell
[pscustomobject]@{
    DbPasswordConfigured = -not [string]::IsNullOrWhiteSpace($env:MEDIBOOK_DB_PASSWORD)
    JwtConfigured = -not [string]::IsNullOrWhiteSpace($env:MEDIBOOK_JWT_SECRET)
    MailPasswordConfigured = -not [string]::IsNullOrWhiteSpace($env:MEDIBOOK_MAIL_APP_PASSWORD)
    SePayKeyConfigured = -not [string]::IsNullOrWhiteSpace($env:SEPAY_WEBHOOK_API_KEY)
}
```

Kết quả chỉ là `True`/`False`; nó không xác minh secret đúng. Không dùng độ dài API key làm bằng chứng provider đã cấu hình cùng key.

### 4.3. Cấu hình legacy

`application-example.properties` còn `VNPAY_TMN_CODE`, `VNPAY_HASH_SECRET` và các `app.vnpay.*`; `VnpayGateway`/`VnpayProperties` cũng còn trong source. Luồng payment hiện tại ở frontend/controller/service là SePay Sandbox. Không cần cấu hình VNPAY cho luồng hiện tại và không được suy ra rằng VNPAY đang được dùng chỉ vì các lớp đó còn tồn tại.

### 4.4. Cấu hình frontend và bảo mật

- Frontend chỉ đọc `VITE_GOOGLE_CLIENT_ID` từ `frontend/.env.example`.
- API frontend hiện hardcode `http://localhost:8080`; không thấy biến `VITE_API_URL` hay cấu hình base URL trong source.
- Backend CORS chỉ cho origin `http://localhost:5173`. Nếu Vite tự chuyển sang cổng khác, trình duyệt có thể báo CORS; không tự đoán rằng API đã chấp nhận origin mới.
- Không bao giờ đưa JWT secret, Gmail App Password, SePay webhook key, Google Client Secret hay database password vào `.env` frontend hoặc bất kỳ biến `VITE_*` nào.

## 5. Chạy phần cơ bản

Các bước này chỉ là hướng dẫn; không có bước nào được thực thi trong phiên viết tài liệu.

### Bước 1 — MySQL

- Mở Windows **Services** hoặc công cụ cài MySQL của bạn; tên service phụ thuộc cách cài.
- Khởi động MySQL và kết nối bằng Workbench/client do bạn quản lý.
- Tạo/chọn database thử nghiệm theo [mục 3](#3-tạo-database-thử-nghiệm-và-hiểu-seeder).
- Thành công khi client kết nối được và `SELECT DATABASE();` trả về schema thử nghiệm dự định.
- Nếu lỗi: xác nhận MySQL đang chạy, host/port trong JDBC URL đúng, user có quyền với schema. Không in password hoặc sửa datasource sang database chưa xác minh.

### Bước 2 — Backend

Mở PowerShell backend riêng; dùng repo path thực tế:

```powershell
Set-Location "C:\DuongDan\den\medibook\backend"
.\mvnw.cmd spring-boot:run
```

Trước đó phải có local `application.properties`, database thử nghiệm đã tạo, và `MEDIBOOK_DB_PASSWORD`/`MEDIBOOK_JWT_SECRET` trong **cùng process environment**. Port trong application-example là `8080`. Dấu hiệu thường thấy là log Spring Boot báo ứng dụng đã `Started`; đây không phải chứng minh mọi tích hợp đã hoạt động.

- MySQL connection error: kiểm tra JDBC URL/schema/user; tránh in password.
- `Unable to determine Dialect` hoặc metadata error: thường datasource không kết nối được; kiểm tra DB trước, không sửa schema/migration tùy tiện.
- Port đã dùng: xác định process/listener trước. Không kill một backend không rõ nguồn gốc; nếu process do bạn khởi động, dừng bằng `Ctrl+C` tại đúng terminal.
- Không chạy `MEDIBOOK_SEED_ENABLED=true` ngoài DB `medibook_demo` cô lập.

### Bước 3 — Frontend

Mở PowerShell frontend riêng:

```powershell
Set-Location "C:\DuongDan\den\medibook\frontend"
npm ci
npm run dev
```

`npm ci` dùng `package-lock.json`; có thể cần Internet để tải package nếu chưa có trong cache. Mở đúng URL được Vite in ra, thường là `http://localhost:5173`. Source backend chỉ cho phép CORS origin `http://localhost:5173`; nếu cổng khác được Vite chọn, hãy dừng và xử lý nguyên nhân cổng trước khi demo thay vì mặc định API sẽ hoạt động.

Các lệnh kiểm tra frontend (chạy khi bạn chủ động muốn kiểm tra):

```powershell
npm run build
npm run lint
```

Build tạo output `frontend/dist`; lint dùng script `oxlint`. Source được rà trong lượt viết tài liệu nhưng không chạy build/lint.

### Thứ tự và dừng ứng dụng

Thứ tự: **MySQL → backend → frontend**. Khi dừng ứng dụng do chính bạn khởi động, nhấn `Ctrl+C` tại terminal tương ứng, mỗi terminal một lần. Không dừng backend/ngrok khác đang được dùng bởi người khác.

## 6. Bật từng tích hợp

### 6a. Google login

1. Dùng Google Web OAuth Client do chủ dự án kiểm soát.
2. Thêm JavaScript origin `http://localhost:5173` trong cấu hình Google. Source không dùng redirect callback nên không tự tạo redirect URI theo suy đoán.
3. Đặt cùng Web Client ID vào `VITE_GOOGLE_CLIENT_ID` trong `frontend/.env` và biến process backend `GOOGLE_CLIENT_ID` (placeholder property là `app.google.client-id`). Đây là ID công khai, không phải secret; không nhập Client Secret.
4. Tải lại frontend sau khi sửa `.env`; khởi động lại backend sau khi đổi env.
5. Trang `/login` tải Google Identity Services từ `https://accounts.google.com/gsi/client`. Cần Internet tới Google.
6. Bấm nút Google và đăng nhập một tài khoản Gmail/Google Workspace có email xác minh. Tài khoản mới được tạo là bệnh nhân. Backend phát JWT MediBook riêng.

Thành công khi UI hoàn tất đăng nhập và tài khoản có thể vào luồng bệnh nhân; kiểm tra role bằng UI/API an toàn. Nếu lỗi, so sánh đúng client ID ở frontend/backend, origin chính xác, trạng thái mạng và lỗi hiển thị. Không dùng Client Secret ở frontend. Facebook chưa có trong source.

### 6b. Email Gmail

1. Chuẩn bị tài khoản gửi Gmail do người trình bày sở hữu và App Password được phép dùng. Cách cấp App Password tùy trạng thái bảo mật/tài khoản Google.
2. Trong terminal backend, đặt `MEDIBOOK_MAIL_USERNAME` bằng địa chỉ Gmail gửi của bạn, rồi nhập `MEDIBOOK_MAIL_APP_PASSWORD` bằng helper nhập kín ở trên.
3. Đặt `$env:MEDIBOOK_MAIL_ENABLED = "true"` trong terminal đó; nếu tắt, để `false` hoặc bỏ qua (mặc định false trong application-example).
4. Cấu hình mẫu dùng `smtp.gmail.com:587`, SMTP auth và STARTTLS.
5. Restart backend. Khi một sự kiện nghiệp vụ phù hợp xảy ra, kiểm tra log và hộp thư nhận, gồm Spam/All Mail.

Sự kiện: đặt lịch, hủy lịch hợp lệ, bác sĩ xác nhận, bác sĩ hoàn thành. Không có email thanh toán trong code đã rà. Gửi mail sau commit khi có transaction synchronization; lỗi mail được log và không rollback nghiệp vụ. Không có retry bền vững/outbox. Log SMTP “sent” không chứng minh thư vào Inbox.

Để tránh tạo dữ liệu thật, dùng tài khoản thử nghiệm và lịch thử nghiệm riêng. Không gửi email demo tới người không đồng ý.

### 6c. SePay Sandbox và ngrok

**Chỉ dùng tài khoản/luồng Sandbox được cung cấp cho dự án; không chuyển tiền thật.**

1. Trong đúng terminal backend, cấu hình các biến `SEPAY_PAYMENT_ENABLED`, `SEPAY_PAYMENT_AMOUNT`, `SEPAY_PAYMENT_EXPIRE_MINUTES`, `SEPAY_RECEIVER_ACCOUNT`, `SEPAY_RECEIVER_GATEWAY`, `SEPAY_WEBHOOK_ENABLED`, `SEPAY_WEBHOOK_API_KEY`. Payment/webhook mặc định tắt trong application-example. Code yêu cầu amount dương, expiry 1–60 phút và receiver account/gateway không rỗng. Ví dụ bật hai cờ sau khi đã chọn đúng môi trường Sandbox:

```powershell
$env:SEPAY_PAYMENT_ENABLED = "true"
$env:SEPAY_WEBHOOK_ENABLED = "true"
$env:SEPAY_PAYMENT_AMOUNT = "100000"
$env:SEPAY_PAYMENT_EXPIRE_MINUTES = "15"
$env:SEPAY_RECEIVER_GATEWAY = "<ma-ngan-hang-Sandbox>"
Set-SecretProcessVariable SEPAY_RECEIVER_ACCOUNT
Set-SecretProcessVariable SEPAY_WEBHOOK_API_KEY
```

Thay placeholder bằng giá trị nhận từ cấu hình Sandbox; không lưu account/key vào history hay repository. Nếu chưa cấu hình webhook public, để `SEPAY_WEBHOOK_ENABLED=false`.
2. Bật `SEPAY_PAYMENT_ENABLED=true` chỉ khi có receiver Sandbox đúng. Bật webhook riêng sau khi key đã được cấu hình hai phía. Không đặt key ở frontend.
3. Khởi động backend trên port cấu hình (mẫu 8080). Khi nhận webhook từ bên ngoài, mở terminal ngrok riêng:

```powershell
ngrok http 8080
```

   Cần cài và xác thực ngrok theo hướng dẫn của phiên bản bạn dùng; yêu cầu tài khoản/token ngrok không được lưu trong repository. Dùng hostname HTTPS do tiến trình ngrok hiện tại cấp. Mỗi lần ngrok restart có thể đổi URL; cập nhật callback ở cấu hình Sandbox.
4. Đặt callback URL theo controller: `https://<ngrok-host>/api/payments/sepay/webhook`.
5. Backend so sánh header nguyên dạng `Authorization: Apikey <SEPAY_WEBHOOK_API_KEY>`. Giao diện cấu hình SePay cần hỗ trợ gửi header này hoặc cơ chế tương đương; **tên menu/trường trên portal chưa được xác minh**, hãy xem tài liệu/portal Sandbox hiện hành. Không đoán tên nút.
6. Trong UI, đăng nhập bệnh nhân, mở lịch của chính tài khoản đó → **Thanh toán / Chi tiết**. Đọc/trạng thái là GET; chỉ nút **Tạo thông tin thanh toán SePay Sandbox** mới gọi POST để yêu cầu backend tạo/trả thông tin payment.
7. UI hiển thị tài khoản nhận, gateway, amount, mã chuyển khoản backend sinh và hạn giao dịch. Thực hiện thao tác thử chỉ theo quy trình Sandbox được nhà cung cấp xác nhận. Không nhập mã BAM cũ, mã lịch cũ hoặc dữ liệu giả làm giao dịch thật.
8. Theo dõi request webhook trong portal/log backend/ngrok inspector nếu môi trường có inspector. Backend chấp nhận các trường payload như `id`, `transferAmount`, `transferType`, `accountNumber`, `gateway`, `code`, `content`, `transactionDate`; timestamp cần dạng `yyyy-MM-dd HH:mm:ss`. Provider có thể có nhiều trường hơn; tuân theo payload thật của Sandbox.
9. Tải lại panel để đọc trạng thái. Nếu payment thành công, lịch vẫn `PENDING` cho đến khi bác sĩ xác nhận. Không có thao tác hoàn tiền/xóa review.

**Phân biệt đường đi:**

- `GET /api/payments/sepay/appointments/{id}` chỉ đọc payment của lịch thuộc chủ tài khoản; không cần ngrok và không tạo giao dịch.
- Gọi webhook local bằng công cụ kiểm thử chỉ chứng minh controller local nhận request, không chứng minh SePay Sandbox kết nối tới máy.
- Để SePay gọi backend local, cần backend đang hoạt động, tunnel HTTPS công khai đúng port, callback path đúng, header đúng và config Sandbox đang dùng hostname hiện hành.

Nếu chưa biết portal Sandbox đang hỗ trợ header hoặc hình thức kích hoạt nào, dừng trước khi tạo event và xác nhận tài liệu nhà cung cấp. Không đưa khóa vào ảnh, lịch sử lệnh, Postman collection chia sẻ hoặc chat.

### 6d. Trợ lý AI / Ollama

Theo cấu hình mẫu, Ollama ở `http://localhost:11434`, model chat là `qwen2.5:3b`, `init.pull-model-strategy=never`; backend không tự tải model. Nếu dùng CLI Ollama đã cài, các lệnh thông thường là:

```powershell
ollama pull qwen2.5:3b
ollama list
ollama serve
```

`ollama serve` chỉ cần nếu chưa có Ollama server chạy. Không khởi động thêm server nếu ứng dụng Ollama GUI đã chạy sẵn. Tải model cần mạng và dung lượng đĩa/RAM; model cỡ 3B vẫn có thể làm máy thiếu tài nguyên. Kiểm tra theo tài liệu/version Ollama trên máy; các lệnh trên không được chạy trong phiên này.

Sau khi model sẵn sàng, đặt `$env:MEDIBOOK_AI_ENABLED = "true"` trong terminal backend và khởi động lại backend. Trên trang bất kỳ, mở nút **Trợ lý MediBook**. Câu hỏi phù hợp: “Làm sao để đặt lịch khám?” hoặc “Tôi có thể hủy lịch khi nào?”.

AI cần đăng nhập, giới hạn câu hỏi 1.000 ký tự, xử lý một request tại một thời điểm, có timeout ở frontend. Nó không đọc lịch/slot/database; không hỏi về lịch cá nhân, chẩn đoán, thuốc hoặc thông tin y tế riêng. Prompt backend còn câu cũ nói thanh toán đang phát triển; nếu AI nhắc đến câu này, đó là nội dung prompt chưa cập nhật, không phải trạng thái giao dịch. Nếu máy thiếu RAM, để AI tắt và đóng Ollama; web cơ bản không cần AI.

## 7. Tài khoản demo

### Tạo dữ liệu demo bằng seeder (chỉ database thử nghiệm)

Source seeder tạo các tài khoản tổng hợp sau nếu chưa tồn tại:

- Admin: `admin.demo@medibook.local`
- Bác sĩ: `bacsi1@medibook.local` đến `bacsi6@medibook.local`
- Bệnh nhân: `benhnhan1@medibook.local` đến `benhnhan3@medibook.local`

Không có mật khẩu mặc định trong source. Mật khẩu chỉ đến từ `MEDIBOOK_SEED_PASSWORD` khi tài khoản được tạo mới; seeder không đổi password của tài khoản đã tồn tại. Không đưa password hoặc hash vào tài liệu/ảnh demo. Bác sĩ thứ sáu được tạo hồ sơ không liên kết để demo thao tác liên kết, nếu dữ liệu đó thực sự được tạo trên database thử nghiệm.

Để seed, phải tự xác nhận datasource đang trỏ đúng `medibook_demo`, đặt `MEDIBOOK_SEED_ENABLED=true` và nhập `MEDIBOOK_SEED_PASSWORD` trong terminal backend; sau khi seed xong, tắt biến này trước lần chạy tiếp theo. Seeder chạy trong quá trình startup và ghi dữ liệu; không thực hiện trên database đang phục vụ người dùng.

Chỉ trên database thử nghiệm mới/riêng, có thể đặt:

```powershell
$env:MEDIBOOK_SEED_ENABLED = "true"
Set-SecretProcessVariable MEDIBOOK_SEED_PASSWORD
```

Trước khi khởi động ứng dụng, phải xác nhận JDBC URL trong local `application.properties` đang trỏ `medibook_demo`. Không dùng lệnh này với cấu hình hiện có mà chưa kiểm tra database đích.

Một số lịch được seed có ngày cố định trong tháng 11/2026, trong đó source có lịch `CONFIRMED` đã qua giờ kết thúc để trình diễn chức năng hoàn thành. Chỉ dùng nếu seed đã chạy an toàn và lịch vẫn xuất hiện đúng trên UI. Không phụ thuộc vào một ID lịch/payment cũ.

### Tạo tài khoản khi không dùng seeder

- Đăng ký qua `/register` tạo patient.
- Admin có thể được tạo lúc startup bằng `MEDIBOOK_ADMIN_EMAIL` và `MEDIBOOK_ADMIN_PASSWORD` trên datasource đã chọn; password tối thiểu 12 ký tự, tối đa 72 byte UTF-8. Nếu email đã tồn tại, initializer không đổi mật khẩu/role. Chỉ đặt các biến này trên database thử nghiệm; nhập password bằng `Set-SecretProcessVariable MEDIBOOK_ADMIN_PASSWORD`.
- Để tạo tài khoản bác sĩ, tạo user trước bằng đăng ký, tạo/chọn hồ sơ bác sĩ, rồi admin mở `/admin/doctors` → **Liên kết tài khoản** và nhập email đó. Service thêm `ROLE_DOCTOR`, giữ role cũ và liên kết một hồ sơ. Bác sĩ đăng xuất/đăng nhập lại sau khi liên kết.
- Không có mật khẩu khôi phục trong code đã rà. Không chia sẻ thông tin đăng nhập qua tài liệu công khai.

## 8. URL và công cụ khi trình bày

Mở frontend tại URL mà Vite in ra, thường `http://localhost:5173`. Backend trong cấu hình mẫu là `http://localhost:8080`. Chỉ dùng `localhost` nhất quán vì CORS cho phép đúng origin `http://localhost:5173`.

| Mục đích | URL/công cụ | Cách mở | Nội dung trình bày | Lưu ý bảo mật |
|---|---|---|---|---|
| Trang chủ | `http://localhost:5173/` | Trình duyệt | Danh mục/bác sĩ và điều hướng | Dữ liệu là từ DB backend |
| Chuyên khoa | `/specialties` | Menu **Chuyên khoa** | Tìm chuyên khoa | Chỉ GET khi xem |
| Bác sĩ | `/doctors` | Menu **Bác sĩ** | Tìm tên/lọc chuyên khoa | Chỉ GET khi xem |
| Đăng ký/đăng nhập | `/register`, `/login` | Menu tương ứng | Patient registration, password/Google login | Không chiếu password/token |
| Đặt lịch | `/booking` | Đăng nhập patient → **Đặt lịch** | Chọn bác sĩ/ngày/slot | Bấm Đặt lịch sẽ ghi DB và có thể gửi mail |
| Lịch patient | `/my-appointments` | Menu **Lịch hẹn của tôi** | Trạng thái lịch/payment, tải lại, chi tiết | Hủy/tạo payment là thao tác ghi |
| Lịch doctor | `/doctor/appointments` | Đăng nhập doctor → **Lịch khám của tôi** | Xác nhận/hoàn thành | Hành động làm thay đổi lịch |
| Admin | `/admin/specialties`, `/admin/doctors`, `/admin/appointment-slots` | Đăng nhập admin | Danh mục, hồ sơ, slot | POST/PUT/DELETE có thể ghi/xóa dữ liệu |
| Swagger UI | `http://localhost:8080/swagger-ui/index.html` | Trình duyệt | Đọc endpoint/schema | Docs public; source không cho thấy Bearer scheme tùy chỉnh. Tránh Execute với POST/PUT/DELETE |
| OpenAPI JSON | `http://localhost:8080/v3/api-docs` | Trình duyệt | Schema OpenAPI | Không có token trong URL |
| API GET công khai | `http://localhost:8080/api/specialties` | Browser/PowerShell | Ví dụ đọc response JSON | GET; không ghi dữ liệu |
| MySQL | MySQL Workbench hoặc client local | Kết nối MySQL bằng cấu hình cá nhân | Xác nhận schema và SELECT giới hạn | Không chụp password/hash/PII |
| DevTools | Browser → F12 → Network | Chọn request frontend | Path, method, status, response | Không mở/chiếu Authorization token |
| ngrok | Hostname HTTPS do ngrok hiện tại in ra | Terminal ngrok; inspector nếu đã cài/cấu hình (thường `http://127.0.0.1:4040`, cần xác nhận) | Request webhook và response | Inspector có thể chứa payload; không chia sẻ công khai |
| SePay Sandbox | Portal Sandbox của chủ dự án | Mở portal do chủ dự án quản lý | Cấu hình endpoint/event thử nghiệm | Tên menu/trường chưa xác minh; không dùng tài khoản thật |
| Gmail | Hộp thư nhận do người demo kiểm soát | Mở Inbox/Spam | Email nghiệp vụ | Che địa chỉ người dùng không liên quan |
| Ollama | `http://localhost:11434` theo cấu hình mẫu | Server local và terminal Ollama | Model local được cấu hình | Không gửi dữ liệu y tế riêng |
| Source | `backend/src/main/java/com/medibook/backend/` và `frontend/src/` | VS Code Explorer | Trace request qua các layer | Không mở local config khi share màn hình |

Không thấy Postman collection trong repository. Swagger/OpenAPI có trong dependency, nhưng không thấy cấu hình Authorization scheme riêng; dùng UI/API client chỉ với tài khoản test của chính bạn, không lưu token vào collection chia sẻ.

## 9. Kịch bản demo 10–15 phút

Chuẩn bị trước giờ trình bày: database thử nghiệm, patient/admin/doctor account, hồ sơ bác sĩ đã liên kết, slot tương lai, Gmail kiểm thử, Google config nếu định trình diễn, SePay Sandbox + webhook/ngrok nếu định trình diễn payment, Ollama/model nếu định trình diễn AI. Chuẩn bị một ảnh/screenshot từ buổi chạy trước cho tích hợp không thể chạy tại phòng; đánh dấu rõ đó là ảnh lịch sử, không phải phiên live.

| Thời gian | Bước và thao tác | Kết quả mong đợi | Bằng chứng có thể chụp | Thay đổi dữ liệu? |
|---:|---|---|---|---|
| 1 phút | Mở trang chủ, chuyên khoa, bác sĩ | Dữ liệu catalog hiển thị; tìm/lọc bác sĩ | Trang và bộ lọc | Không, nếu chỉ xem |
| 1 phút | Đăng nhập patient bằng Google nếu đã cấu hình, nếu không dùng tài khoản test | Login xong, UI hiển thị vai trò/điều hướng phù hợp | Trang lịch sau đăng nhập, không chụp token | Có thể tạo user mới lần đầu Google |
| 2 phút | Chọn bác sĩ, ngày và slot tương lai → **Đặt lịch** | Appointment mới `PENDING`, slot chuyển `BOOKED` | Thông báo thành công, lịch trong MyAppointments | **Có**; phát sinh lịch, có thể gửi email |
| 1 phút | Mở email test của sự kiện đặt lịch | Mail notification tới patient nếu SMTP bật | Subject/nội dung, che email cá nhân | Không tạo thêm ngoài việc đặt ở bước trước |
| 2–3 phút | Mở **Thanh toán / Chi tiết** → **Tạo thông tin thanh toán SePay Sandbox**; thực hiện event theo portal Sandbox đã xác minh; **Tải lại thanh toán** | UI đọc payment; nếu webhook hợp lệ, payment SUCCESS nhưng lịch vẫn chờ bác sĩ | Panel với payment/lịch riêng; che account/reference | **Có**; POST tạo payment và Sandbox event |
| 1–2 phút | Đăng nhập doctor đã liên kết → **Xác nhận** lịch đã trả | Lịch chuyển `CONFIRMED`; bệnh nhân có thể nhận email xác nhận | Doctor list và MyAppointments | **Có**; có thể gửi email |
| 1 phút | Nếu có lịch `CONFIRMED` đã qua endTime hợp lệ, nhấn **Hoàn thành** | Lịch `COMPLETED`; có thể gửi email hoàn thành | Trạng thái và email nếu có | **Có** |
| 1 phút | Đăng nhập admin, xem ba trang quản trị | Danh mục, hồ sơ bác sĩ và slot hiển thị | Mỗi trang ở trạng thái chỉ xem | Không nếu không bấm lưu/xóa |
| 1 phút | Mở **Trợ lý MediBook**, hỏi cách đặt lịch | Trả lời AI theo prompt website; không tra lịch cá nhân | Widget, không hiển thị dữ liệu riêng | Gửi nội dung câu hỏi tới Ollama local |
| 1 phút | Xem Swagger GET hoặc chạy SELECT an toàn | API/database cho thấy trạng thái đã trình bày | Path/status và số liệu tối thiểu | GET/SELECT không chủ đích ghi |

### Nếu không thể hoàn thành lịch trong thời gian demo

- Chỉ dùng một lịch `CONFIRMED` đã đến/qua giờ kết thúc và đã được chuẩn bị hợp lệ trong database demo, ví dụ lịch quá khứ do seeder tạo nếu nó còn tồn tại.
- Xác nhận lại trạng thái trên UI trước buổi trình bày, không dựa vào ID lịch hay ID payment cố định.
- Không chỉnh đồng hồ máy, sửa database để vượt kiểm tra, hoặc xác nhận lịch chưa thanh toán.
- Nếu không có lịch hợp lệ, trình bày nút bị vô hiệu hóa trước giờ kết thúc và giải thích điều kiện backend. Không giả lập thao tác thành công.

## 10. API chính

JWT MediBook được gửi trong header `Authorization: Bearer <JWT-của-tài-khoản-test>` cho endpoint cần đăng nhập. Không ghi token thật vào code, URL, ảnh, tài liệu hoặc history được chia sẻ. Swagger docs public; không mặc định đã có nút `Authorize` vì chưa thấy security scheme cấu hình.

| Method | Path | Quyền theo source | Input/output khái quát | An toàn khi chỉ xem? |
|---|---|---|---|---|
| `POST` | `/api/auth/register` | Công khai | `fullName`, `email`, `password`, `phone?`; response user/roles, chưa cấp token từ endpoint đăng ký | **Không** – tạo user |
| `POST` | `/api/auth/login` | Công khai | Email/password → JWT và roles | Không thay đổi dữ liệu nghiệp vụ; không đưa password/token vào demo log |
| `POST` | `/api/auth/google` | Công khai | Google `credential` → JWT MediBook | Tạo user lần đầu có thể ghi DB |
| `GET` | `/api/auth/me` | Đã đăng nhập | User hiện hành | Có |
| `GET` | `/api/specialties` | Công khai | Danh sách specialty | Có |
| `POST`, `PUT`, `DELETE` | `/api/specialties`, `/api/specialties/{id}` | Admin | Tạo/sửa/xóa specialty | **Không** |
| `GET` | `/api/doctors` | Công khai | Danh sách hồ sơ bác sĩ | Có |
| `POST`, `PUT` | `/api/doctors`, `/api/doctors/{id}` | Admin | Tạo/sửa hồ sơ doctor | **Không** |
| `GET` | `/api/appointment-slots` | Công khai | Danh sách slot | Có |
| `POST` | `/api/appointment-slots` | Admin | `doctorId`, `slotDate`, `startTime`, `endTime` | **Không** – tạo slot |
| `POST` | `/api/admin/doctors/{doctorId}/account` | Admin | `email` tài khoản đã đăng ký | **Không** – thay đổi liên kết/role |
| `GET` | `/api/appointments/me` | Patient | Danh sách lịch của chủ token | Có |
| `POST` | `/api/appointments` | Patient | `slotId` → appointment | **Không** – đặt lịch |
| `POST` | `/api/appointments/{appointmentId}/cancel` | Patient sở hữu lịch | Hủy lịch theo điều kiện | **Không** |
| `GET` | `/api/doctor/appointments` | Doctor | Lịch thuộc hồ sơ đã liên kết | Có |
| `POST` | `/api/doctor/appointments/{appointmentId}/confirm` | Doctor sở hữu lịch | Xác nhận appointment đủ điều kiện | **Không** |
| `POST` | `/api/doctor/appointments/{appointmentId}/complete` | Doctor sở hữu lịch | Hoàn thành sau thời điểm kết thúc | **Không** |
| `POST` | `/api/payments/sepay/appointments/{appointmentId}` | Patient sở hữu lịch | Tạo/trả payment từ cấu hình server | **Không** – có thể tạo payment |
| `GET` | `/api/payments/sepay/appointments/{appointmentId}` | Patient sở hữu lịch | Danh sách payments theo thứ tự mới nhất trước | Có |
| `GET` | `/api/payments/sepay/transactions/{paymentId}` | Patient sở hữu lịch | Một payment SePay | Có |
| `POST` | `/api/payments/sepay/webhook` | Permit ở filter; controller đòi `Authorization: Apikey ...` | Payload do provider gửi | **Không** – xử lý payment/event |
| `POST` | `/api/ai/chat` | Đã đăng nhập | Câu hỏi tối đa 1.000 ký tự → answer | Gửi câu hỏi cho model; tránh dữ liệu nhạy cảm |

Để gọi API chỉ đọc trong PowerShell:

```powershell
Invoke-RestMethod `
  -Method Get `
  -Uri "http://localhost:8080/api/specialties"
```

Các GET yêu cầu đăng nhập có thể xem trực tiếp trên UI. Nếu dùng API client, gắn token của chính user test trong bộ nhớ của phiên; không dùng token người khác hoặc lưu vào collection dùng chung. `GET /api/payments/sepay/appointments/{id}` chỉ đọc danh sách, không tạo payment; dùng ID lịch vừa thấy trên UI và thuộc chính user đang đăng nhập, không hardcode một lịch cũ.

## 11. Xem database an toàn

Mở MySQL Workbench/client cục bộ, kết nối đúng schema thử nghiệm, rồi xác nhận:

```sql
SELECT DATABASE();
SELECT VERSION();

SELECT table_name
FROM information_schema.tables
WHERE table_schema = DATABASE()
ORDER BY table_name;
```

Các câu lệnh sau chỉ chọn cột cần thiết và không đọc password hash/token:

```sql
SELECT id, status, provider
FROM users
ORDER BY id
LIMIT 20;

SELECT id, user_id, role_id
FROM user_roles
ORDER BY user_id, role_id
LIMIT 30;

SELECT id, doctor_id, slot_date, start_time, end_time, status
FROM appointment_slots
ORDER BY slot_date, start_time
LIMIT 30;

SELECT id, patient_id, doctor_id, slot_id, status, created_at
FROM appointments
ORDER BY id DESC
LIMIT 30;

SELECT id, appointment_id, provider, amount, currency, status,
       expires_at, paid_at, requires_review
FROM payment_transactions
ORDER BY id DESC
LIMIT 30;

SELECT outcome, COUNT(*) AS event_count
FROM sepay_webhook_events
GROUP BY outcome
ORDER BY outcome;
```

Không dùng `SELECT * FROM users`, không đọc `password_hash`, không chụp danh sách email/số điện thoại nếu không cần. Không sửa dữ liệu bằng SQL để demo. So sánh appointment status với slot status/payment status; `payment_transactions` có thể có nhiều giao dịch trên một appointment.

Unit test mock không xác minh transaction commit/rollback, dirty checking, unique constraint ở DB, locking hoặc race condition thật. Không tuyên bố các thuộc tính đó đã được kiểm chứng chỉ từ test service.

## 12. Câu hỏi giảng viên có thể hỏi

| Câu hỏi | Trả lời ngắn dựa trên source |
|---|---|
| 1. MediBook giải quyết bài toán gì? | Tra cứu danh mục, bác sĩ, khung giờ và theo dõi lịch khám; xem `frontend/src/pages/Booking.jsx` và `backend/.../service/AppointmentService.java`. |
| 2. Vì sao frontend/backend tách riêng? | React/Vite giao tiếp REST JSON với Spring MVC; xem `frontend/src/api/` và `backend/.../controller/`. |
| 3. Backend đi qua những lớp nào? | Controller nhận request, service áp nghiệp vụ, repository dùng Spring Data JPA, Hibernate kết nối MySQL. |
| 4. User/role được lưu ở đâu? | `User`, `Role` và join table `user_roles`; xem `backend/.../entity/User.java`, `Role.java`. |
| 5. Patient có bảng riêng không? | Không; đó là user có `ROLE_PATIENT`. |
| 6. Google login có phải redirect OAuth không? | Frontend dùng Google Identity Services lấy credential/ID token; backend xác minh token tại `GoogleTokenVerifier` rồi cấp JWT MediBook. |
| 7. Facebook login có chưa? | Không thấy Facebook integration trong source đã rà. |
| 8. Vì sao frontend guard chưa đủ bảo mật? | Người dùng có thể gọi API ngoài UI; quyền được kiểm tra thêm ở `SecurityConfig`, filter và controller/service. |
| 9. Admin liên kết bác sĩ như thế nào? | Tài khoản phải tồn tại; `DoctorAccountService` thêm `ROLE_DOCTOR` và liên kết hồ sơ; bác sĩ cần đăng nhập lại. |
| 10. Chống đặt cùng slot bằng gì? | Service khóa slot `PESSIMISTIC_WRITE` trong transaction và kiểm tra `AVAILABLE`; source/test mock chưa chứng minh concurrent requests trên MySQL. |
| 11. Slot trùng giờ do admin xử lý ra sao? | `AppointmentSlotService` khóa doctor và đếm slot overlap cùng ngày trước khi lưu. Cần integration test/DB demo để xác nhận đồng thời thật. |
| 12. Đặt lịch tạo trạng thái gì? | Appointment `PENDING`, slot `BOOKED`; xem `AppointmentService.createAppointment`. |
| 13. Có API đổi/reschedule không? | Không thấy; hủy lịch đủ điều kiện rồi đặt lịch mới ở slot khác. |
| 14. Vì sao payment success chưa làm appointment confirmed? | Payment và lịch là hai trạng thái tách rời; doctor cần xác nhận riêng sau payment SePay success. |
| 15. Bác sĩ cần điều kiện nào để xác nhận? | Đúng doctor, lịch `PENDING`, slot `BOOKED`, có SePay `SUCCESS` sạch và không có payment nào review; xem `DoctorAppointmentService.confirmAppointment`. |
| 16. Khi nào hoàn thành lịch? | Chỉ appointment `CONFIRMED`, slot `BOOKED`, thời gian hiện tại đã đến/qua slot endTime. |
| 17. Webhook xác thực thế nào? | Controller so sánh `Authorization: Apikey ...`; service kiểm tra receiver, chiều tiền, amount, mã tham chiếu và thời điểm. |
| 18. Chống webhook lặp thế nào? | `sepay_webhook_events` lưu event key và fingerprint; lặp cùng ID/fingerprint không ghi xử lý mới; ID trùng payload khác bị conflict. |
| 19. Hết hạn được lưu ra sao? | Enum `PaymentStatus` chỉ có `PENDING/SUCCESS/FAILED`; expiry suy ra từ `expires_at`, không có trạng thái DB `EXPIRED`. |
| 20. Nếu giao dịch cần review thì sao? | Không tự xóa review/hoàn tiền; xác nhận lịch và tạo payment mới bị chặn theo service, bệnh nhân liên hệ phòng khám. |
| 21. Tại sao có ngrok? | Public tunnel để provider gọi webhook vào backend local; không phải triển khai toàn app lên cloud. |
| 22. Khi nào gửi email? | Khi đặt/hủy/xác nhận/hoàn thành; không thấy email thanh toán. `EmailNotificationService` ghi log nếu gửi lỗi, không retry bền vững. |
| 23. AI dùng gì và truy cập dữ liệu nào? | Spring AI `ChatClient` tới Ollama/model cấu hình; service chỉ gửi prompt + câu hỏi, không tra DB/lịch cá nhân. |
| 24. Vì sao dùng SePay dù đề ghi VNPay/MoMo? | “Source hiện triển khai luồng SePay Sandbox; đề bài được cung cấp ghi VNPay hoặc MoMo. Nhóm chưa có xác nhận SePay được chấp thuận.” Không bịa lý do/chấp thuận. |
| 25. Phần nào đã được demo? | Trình bày đúng phần bạn đã tự demo. Code không tự chứng minh integration; test mock cũng không chứng minh database/webhook/email thật. |

## 13. Test và xử lý lỗi

### Test có sẵn và lệnh chọn lọc

Các lớp service test được rà dùng `@ExtendWith(MockitoExtension.class)` và mock repository/service dependencies; không tự khởi động Spring hoặc kết nối DB trong các lớp này. `BackendApplicationTests` có `@SpringBootTest` nên có thể khởi động context và datasource. Tránh `.\mvnw.cmd test` toàn bộ nếu chưa cấu hình database test cô lập.

Các lệnh dưới đây chỉ chạy đúng test đã chọn khi bạn chủ động kiểm tra; **không được chạy trong phiên viết tài liệu**:

```powershell
Set-Location "C:\DuongDan\den\medibook\backend"
.\mvnw.cmd "-Dtest=AppointmentServiceTest,DoctorAppointmentServiceTest" test
```

```powershell
.\mvnw.cmd "-Dtest=SepayPaymentServiceTest" test
.\mvnw.cmd "-Dtest=SepayWebhookServiceTest" test
```

Maven Surefire có cấu hình Mockito agent trong `backend/pom.xml`:

```xml
<argLine>-javaagent:"${settings.localRepository}/org/mockito/mockito-core/${mockito.version}/mockito-core-${mockito.version}.jar"</argLine>
```

Đây là agent JVM cho test (cần static mock thời gian trong một test); không xóa/đổi mock maker/đổi dependency để xử lý lỗi môi trường.

**Kết quả lịch sử đã được cung cấp, không phải kiểm tra hiện tại:**

- `AppointmentServiceTest` + `DoctorAppointmentServiceTest`: 19 test pass trong lần chạy đã được duyệt trước đây.
- `SepayWebhookServiceTest`: trước đây có báo cáo 14 test pass.
- Lần chạy SePay riêng gần đây dừng vì JVM fork lỗi native memory trước khi có kết quả test; lớp payment dừng ở bước đầu, lớp webhook không chạy trong lần thử đó. Không được báo PASS dựa trên lần lỗi này.

Unit test với mock chỉ kiểm tra logic service theo mock setup; không chứng minh API thực, MySQL, transaction/rollback, khóa đồng thời, race condition hoặc tích hợp SePay. Frontend có `npm run build` và `npm run lint`, nhưng tài liệu này không chạy chúng.

### Lỗi thường gặp

| Hiện tượng | Phân biệt/kiểm tra an toàn |
|---|---|
| `8080` đang dùng | Xem terminal backend của chính bạn; có thể kiểm tra listener bằng `Get-NetTCPConnection -LocalPort 8080 -State Listen`. Không kết thúc process lạ. |
| Frontend không tải được API | Backend có chạy không; API URL frontend hiện hardcode `localhost:8080`; xem Network status và CORS. Vite đổi sang 5174 sẽ không khớp origin CORS hiện tại. |
| MySQL connection refused | Kiểm tra MySQL service, host/port/schema. Không hiển thị password trong log/chụp ảnh. |
| Access denied / datasource lỗi | Kiểm tra username/secret trong đúng process backend; file application-example dùng env placeholders. Không chuyển sang database khác chưa xác minh. |
| HTTP 401 | Token thiếu/hết hạn/sai hoặc tài khoản không `ACTIVE`; đăng nhập lại bằng tài khoản test. Không chia sẻ token. |
| HTTP 403 | Tài khoản không có role/không sở hữu resource/không thuộc doctor; kiểm tra user test và liên kết hồ sơ. |
| Google không hiện/nút báo cấu hình | Kiểm tra `frontend/.env` có `VITE_GOOGLE_CLIENT_ID`, backend có `GOOGLE_CLIENT_ID`, origin đúng và có mạng tới Google. Không nhầm ID với client secret. |
| Email không đến | Xác nhận mail enabled và cấu hình username/App Password ở terminal backend, restart backend; kiểm tra log, Spam/All Mail. Gửi SMTP không đồng nghĩa mail vào Inbox. |
| SePay webhook 401/503 | 503 thường do webhook tắt/thiếu cấu hình; 401 do header/key mismatch. Không in key; xác nhận header scheme chính xác là `Apikey`. |
| Không thấy webhook từ SePay | Kiểm tra backend port, hostname ngrok mới nhất, path `/api/payments/sepay/webhook`, provider settings và ngrok request inspector nếu có. Local POST không chứng minh public route. |
| Payment `PENDING` nhưng hết hạn | Status DB có thể vẫn `PENDING`; UI suy ra expiry từ `expiresAt`. Tải lại; chỉ bấm tạo payment mới khi có chủ đích. |
| Payment review | UI hiển thị liên hệ phòng khám; không có nút xóa review/hoàn tiền. Không chỉnh DB để demo. |
| Bác sĩ không xác nhận được | Backend yêu cầu lịch `PENDING`, slot `BOOKED`, bác sĩ sở hữu, payment SePay `SUCCESS` sạch, không có review. |
| Bác sĩ chưa thể hoàn thành | Chỉ lịch `CONFIRMED` và thời điểm đã đến/qua endTime mới được hoàn thành. Không chỉnh giờ máy/DB. |
| AI báo 503/time out | Kiểm tra AI enabled, Ollama server và model `qwen2.5:3b`; tắt AI nếu máy thiếu tài nguyên. |
| JVM test native memory | Lỗi cấp phát native memory trước test không tự chứng minh lỗi nghiệp vụ. Không lặp test liên tục, không tự tăng heap/tắt bảo vệ Windows/đổi cấu hình hệ thống. Nếu cần chẩn đoán, chỉ đọc phần lỗi cần thiết của `backend\hs_err_pid*.log`, không chia sẻ toàn file/environment. |
| Test toàn bộ nạp context | `BackendApplicationTests` có `@SpringBootTest`; dùng test selector chỉ định lớp unit đã xác nhận, tránh suite đầy đủ trên database đang chạy. |

## 14. Checklist trước ngày demo

- [ ] Đã hỏi giảng viên về sai khác SePay so với VNPay/MoMo; không trình bày là đã được chấp thuận nếu chưa có xác nhận.
- [ ] Đã xác nhận backend trỏ tới database thử nghiệm, không phải DB của ứng dụng/người khác.
- [ ] Đã kiểm tra quyền truy cập DB, `ddl-auto=update`, seeder đang tắt nếu không dùng.
- [ ] Đã kiểm tra các tài khoản patient/admin/doctor và role; doctor đã được liên kết hồ sơ.
- [ ] Đã chuẩn bị slot còn trống trong tương lai; không phụ thuộc lịch/payment ID cũ.
- [ ] Đã xác nhận Google origin/client ID nếu demo Google; có phương án đăng nhập thường nếu mạng/OAuth lỗi.
- [ ] Đã xác nhận SMTP ở tài khoản test nếu demo email; chuẩn bị ảnh lịch sử dự phòng và ghi rõ nếu không phải live.
- [ ] Đã xác nhận đúng SePay Sandbox, receiver, header, URL tunnel và hostname ngrok hiện tại; không dùng tiền thật.
- [ ] Đã tải model Ollama trước buổi demo hoặc để AI tắt nếu máy thiếu RAM.
- [ ] Đã mở thử route và Swagger/DB tool cần trình bày; chỉ dùng GET/SELECT trong phần đọc.
- [ ] Đã ẩn password, API key, JWT, app password, thông tin cá nhân khỏi cửa sổ/ảnh.
- [ ] Đã chuẩn bị nhánh giải thích vì sao không thể hoàn thành lịch trước endTime; không thay đồng hồ hoặc sửa DB.
- [ ] Đã biết terminal nào khởi chạy ứng dụng; dừng đúng terminal bằng `Ctrl+C`.
- [ ] Nếu khởi động lại ngrok, cập nhật URL callback ở cấu hình Sandbox vì URL public có thể đổi.
- [ ] Không chạy toàn bộ test nếu chưa xác định rõ `BackendApplicationTests`/datasource và database test cô lập.

