# Context Dự Án: Publication Trend Tracking System (SWP391)

## 1. Thông tin chung
- **Dự Án:** Publication Trend Tracking System
- **Môn học:** Đồ Án chuyên ngành (SWP391)
- **Vai trò của AI (Cowork):** Hỗ trợ code, review, fix bug và quản lý Git an toàn, tuân thủ chặt chẽ quy trình làm việc nhóm.

## 2. Tech Stack & Architecture
- **Backend Framework:** Spring Boot (Java)
- **Cấu trúc thư mục (N-Tier):** controller -> service (interface) -> serviceImpl -> repository
- **Database:** SQL Server (có các script schema.sql và real_data.sql / scipt_final.sql trong thư mục database/)

## 3. Coding Conventions (Bắt buộc tuân thủ)
- **Dependency Injection:** KHÔNG dùng @Autowired. Bắt buộc dùng final field kết hợp với @RequiredArgsConstructor của Lombok.
- **Response Format:** Mọi API Endpoint đều phải bọc dữ liệu trả về bằng class ApiResponse<T>.
- **Exception Handling:** Không ném Exception thô. Bắt buộc sử dụng throw new AppException(ErrorCode.XXX). ErrorCode được tập trung tại một file duy nhất và chia dải ID (VD: 1000-1099 cho Auth, 1300-1399 cho Paper...).
- **Lombok:** Tích cực sử dụng @Builder cho DTOs, @Data hoặc @Getter/@Setter chuẩn mực.
- **Security & Swagger:** Các API có bảo mật phải gắn @SecurityRequirement(name = "api") trên Controller để Swagger hiển thị ổ khóa.
- **Transaction:** Thao tác đọc (SELECT) dùng @Transactional(readOnly = true). Thao tác ghi (INSERT/UPDATE/DELETE) dùng @Transactional.

## 4. Git & Branching Workflow (Quy tắc Sống còn - Strict Isolation)
- **Pre-Flight Checklist (BẮT BUỘC):** Trước khi bắt tay vào code bất kỳ tính năng mới nào, AI LUÔN LUÔN phải chạy git branch hoặc git status để xác nhận nhánh hiện tại.
- **Strict Branch Boundary (Ranh giới tuyệt đối):** Chức năng nào thuộc nhánh đó. Code về chủ đề A (vd: Dashboard) KHÔNG BAO GIỜ được phép commit vào nhánh chủ đề B (vd: research-paper). Nếu phát hiện sai nhánh, phải DỪNG code và xin ý kiến chuyển nhánh.
- **No Cross-branch Contamination (Chống nhiễm chéo):** Khi chuyển nhánh (git checkout), tuyệt đối không được mang theo code đang sửa dở (unstaged changes) sang nhánh mới. Phải luôn dùng git stash hoặc commit gọn gàng trước khi đổi nhánh.
- DỰ ÁN ÁP DỤNG FEATURE BRANCH: Nhánh trung tâm là develop. Luôn git pull origin develop để xử lý conflict trước khi Merge/PR.

## 5. Tình trạng các nhánh (Cập nhật gần nhất)
- feature/topic & feature/research-paper: Code đã chuẩn hoá.
- feature/publication-trend: Phụ trách thống kê biểu đồ.
- feature/sync: Phụ trách đồng bộ dữ liệu.
- feature/dashboard: Phụ trách giao diện dashboard.
