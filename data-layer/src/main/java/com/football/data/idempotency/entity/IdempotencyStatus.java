package com.football.data.idempotency.entity;

/**
 * Trạng thái của khóa bất biến (Idempotency Key).
 * Giúp kiểm soát vòng đời của yêu cầu tích hợp liên dịch vụ.
 * 
 * Thành viên thiết kế: HÙNG (feat/hung-data) - Tuần 6 (TH6)
 */
public enum IdempotencyStatus {

    /** Yêu cầu đang được xử lý trong hệ thống */
    PROCESSING,

    /** Yêu cầu đã hoàn tất thành công và kết quả đã được lưu cache */
    COMPLETED,

    /** Yêu cầu gặp lỗi trong quá trình thực thi */
    FAILED
}
