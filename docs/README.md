# Tài liệu nhiệm vụ Nhật TH1–TH7

Đọc theo thứ tự:

| File | Nội dung |
|---|---|
| [BAO_CAO_PHAN_TICH_PTPM_CU.md](BAO_CAO_PHAN_TICH_PTPM_CU.md) | Phân tích dự án PTPM cũ, chọn bản làm cơ sở |
| [BAO_CAO_HIEN_TRANG_NHIEM_VU_NHAT.md](BAO_CAO_HIEN_TRANG_NHIEM_VU_NHAT.md) | Hiện trạng Booking trước khi làm TH1–TH7 |
| [KE_HOACH_TRIEN_KHAI_NHAT_TH1_TH7.md](KE_HOACH_TRIEN_KHAI_NHAT_TH1_TH7.md) | Kế hoạch triển khai |
| [BAO_CAO_HOAN_THANH_NHAT_TH1_TH7.md](BAO_CAO_HOAN_THANH_NHAT_TH1_TH7.md) | Báo cáo nghiệm thu: chức năng, kiểm thử, giới hạn |
| [BAO_CAO_RA_SOAT_TRUOC_KHI_PUSH.md](BAO_CAO_RA_SOAT_TRUOC_KHI_PUSH.md) | Rà soát bảo mật và chất lượng trước khi push |
| [BAO_CAO_DONG_GOI_NHIEM_VU_NHAT.md](BAO_CAO_DONG_GOI_NHIEM_VU_NHAT.md) | Đóng gói phần đóng góp của Nhật (workspace chỉ chứa Booking) |
| [HOP_DONG_API_TICH_HOP.md](HOP_DONG_API_TICH_HOP.md) | Hợp đồng API Booking cần từ Pitch, Auth, MySQL, Eureka |
| [DE_XUAT_CHUAN_HOA_CAU_HINH.md](DE_XUAT_CHUAN_HOA_CAU_HINH.md) | Đề xuất chuẩn hóa cấu hình: `booking_db`, MySQL 8.0, `ddl-auto=validate`, Eureka (chưa áp dụng) |
| [DE_XUAT_HOP_DONG_JWT_AUTH.md](DE_XUAT_HOP_DONG_JWT_AUTH.md) | Đề xuất hợp đồng JWT gửi Hoàng (chờ xác nhận) |
| [DE_XUAT_HOP_DONG_PITCH.md](DE_XUAT_HOP_DONG_PITCH.md) | Đề xuất `ownerId` và danh sách sân theo chủ gửi người phụ trách Pitch (chờ xác nhận) |
| [bang-chung-kiem-thu/](bang-chung-kiem-thu/) | Log kiểm thử thực tế với tiến trình thật |

> **Lưu ý khi đọc:** 5 báo cáo đầu được viết trong workspace đầy đủ của nhóm (`C:\nhom13`, gồm cả Auth, Pitch và script CSDL). Các đoạn nhắc tới `pitchos-auth-service/`, `pitchos-pitch-service/`, `pitchos-database/`, `.env` hay file `application.properties` ở gốc service là **mô tả bối cảnh lúc làm việc**; các thư mục và file đó **không có** trong phần đóng góp này. Khác biệt giữa bản này và workspace ban đầu (bỏ giá trị mặc định của secret, secret riêng cho test, Postman không chứa token) được ghi trong báo cáo đóng gói.
