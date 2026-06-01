package model;

/**
 * Dinh nghia cac trang thai cua mot phien dau gia theo yeu cau bai tap lon.
 */
public enum AuctionStatus {
    OPEN,      // Phien vua tao
    RUNNING,   // Dang dien ra
    FINISHED,  // Da ket thuc
    PAID,      // Da thanh toan
    CANCELED   // Da huy
}
