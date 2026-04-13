package model;

/**
 * Định nghĩa các trạng thái của một phiên đấu giá theo yêu cầu bài tập lớn.
 */
public enum AuctionStatus {
    OPEN,      // Phiên vừa tạo
    RUNNING,   // Đang diễn ra
    FINISHED,  // Đã kết thúc
    PAID,      // Đã thanh toán
    CANCELED   // Đã hủy
}