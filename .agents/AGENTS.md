# Context Dự án: Publication Trend Tracking System (SWP391)

## 1. Thông tin chung
- **Dự án:** Publication Trend Tracking System
- **Môn học:** Đồ án chuyên ngành (SWP391)
- **Vai trò của AI (Cowork):** Hỗ trợ code, review, fix bug và quản lý Git an toàn, tuân thủ chặt chẽ quy trình làm việc nhóm.

## 2. Tech Stack & Architecture
- **Backend Framework:** Spring Boot (Java)
- **Cấu trúc thư mục (N-Tier):** `controller` -> `service` (interface) -> `serviceImpl` -> `repository`
- **Database:** SQL Server (có các script `schema.sql` và `real_data.sql` / `scipt_final.sql` trong thư mục `database/`)

## 3. Coding Conventions (Bắt buộc tuân thủ)
- **Dependency Injection:** KHÔNG dùng `@Autowired`. Bắt buộc dùng `final` field kết hợp với `@RequiredArgsConstructor` của Lombok.
- **Response Format:** Mọi API Endpoint đều phải bọc dữ liệu trả về bằng class `ApiResponse<T>`.
- **Exception Handling:** Không ném Exception thô. Bắt buộc sử dụng `throw new AppException(ErrorCode.XXX)`. `ErrorCode` được tập trung tại một file duy nhất và chia dải ID (VD: 1000-1099 cho Auth, 1300-1399 cho Paper...).
- **Lombok:** Tích cực sử dụng `@Builder` cho DTOs, `@Data` hoặc `@Getter`/`@Setter` chuẩn mực.
- **Security & Swagger:** Các API cần bảo mật phải gắn `@SecurityRequirement(name = "api")` trên Controller để Swagger hiển thị ổ khóa.
- **Transaction:** Thao tác đọc (SELECT) dùng `@Transactional(readOnly = true)`. Thao tác ghi (INSERT/UPDATE/DELETE) dùng `@Transactional`.

## 4. Git & Branching Workflow (Quy tắc sống còn)
- Dự án áp dụng mô hình Feature Branch. Nhánh trung tâm là `develop`.
- **Tuyệt đối tuân thủ Scope Isolation (Cô lập phạm vi):** 
  - Chỉ tạo mới/chỉnh sửa các class thuộc đúng tính năng của nhánh đang làm.
  - **VÙNG CẤM (No-touch zone):** Khi code các tính năng thông thường, TUYỆT ĐỐI KHÔNG tự ý sửa file của tính năng khác như `EmailService`, cấu hình `SecurityConfig`, phân quyền (Authorization/JWT), hoặc module `admin`.
- **Xử lý Conflict:** Luôn đối chiếu (`git fetch` và `git merge develop`) trước khi tạo Pull Request để đảm bảo code không dẫm chân lên code của đồng đội.
- Mọi nhánh rác hoặc commit rác đều phải được dọn dẹp bằng `git rebase` hoặc reset trước khi push.

## 5. Tình trạng các nhánh (Cập nhật gần nhất)
- `feature/topic` & `feature/research-paper`: Đã hoàn thiện và merge vào `develop`.
- `feature/publication-trend`: Đã merge (có filter tìm kiếm nâng cao và phân trang).
- `feature/sync`: Phụ trách đồng bộ dữ liệu từ API ngoài (OpenAlex, Semantic Scholar) bằng Scheduler và trigger thủ công. Đã dọn dẹp conflict.
