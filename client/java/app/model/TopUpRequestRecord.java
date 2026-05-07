package app.model;

import java.time.LocalDateTime;

public class TopUpRequestRecord {
    private final String id;
    private final String username;
    private final double amount;
    private final String bankName;
    private final String accountName;
    private final String accountNumber;
    private final LocalDateTime requestedAt;
    private final String status;
    private final LocalDateTime approvedAt;
    private final String approvedBy;
    private final LocalDateTime creditedAt;

    public TopUpRequestRecord(
            String id,
            String username,
            double amount,
            String bankName,
            String accountName,
            String accountNumber,
            LocalDateTime requestedAt,
            String status,
            LocalDateTime approvedAt,
            String approvedBy,
            LocalDateTime creditedAt
    ) {
        this.id = id;
        this.username = username;
        this.amount = amount;
        this.bankName = bankName;
        this.accountName = accountName;
        this.accountNumber = accountNumber;
        this.requestedAt = requestedAt;
        this.status = status;
        this.approvedAt = approvedAt;
        this.approvedBy = approvedBy;
        this.creditedAt = creditedAt;
    }

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public double getAmount() {
        return amount;
    }

    public String getBankName() {
        return bankName;
    }

    public String getAccountName() {
        return accountName;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public LocalDateTime getCreditedAt() {
        return creditedAt;
    }
}
