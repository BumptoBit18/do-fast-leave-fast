package shared.socket;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

public class SocketRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String action;
    private String actorUsername;
    private String username;
    private String password;
    private String role;
    private String fullName;
    private String keyword;
    private String category;
    private String auctionId;
    private String title;
    private String description;
    private String imageHint;
    private String bankName;
    private String accountName;
    private String accountNumber;
    private String requestId;
    private double amount;
    private double maxAmount;
    private double incrementStep;
    private double startPrice;
    private int durationHours;

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getActorUsername() {
        return actorUsername;
    }

    public void setActorUsername(String actorUsername) {
        this.actorUsername = actorUsername;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(String auctionId) {
        this.auctionId = auctionId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageHint() {
        return imageHint;
    }

    public void setImageHint(String imageHint) {
        this.imageHint = imageHint;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public double getMaxAmount() {
        return maxAmount;
    }

    public void setMaxAmount(double maxAmount) {
        this.maxAmount = maxAmount;
    }

    public double getIncrementStep() {
        return incrementStep;
    }

    public void setIncrementStep(double incrementStep) {
        this.incrementStep = incrementStep;
    }

    public double getStartPrice() {
        return startPrice;
    }

    public void setStartPrice(double startPrice) {
        this.startPrice = startPrice;
    }

    public int getDurationHours() {
        return durationHours;
    }

    public void setDurationHours(int durationHours) {
        this.durationHours = durationHours;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("action", action);
        values.put("actorUsername", actorUsername);
        values.put("username", username);
        values.put("password", password);
        values.put("role", role);
        values.put("fullName", fullName);
        values.put("keyword", keyword);
        values.put("category", category);
        values.put("auctionId", auctionId);
        values.put("title", title);
        values.put("description", description);
        values.put("imageHint", imageHint);
        values.put("bankName", bankName);
        values.put("accountName", accountName);
        values.put("accountNumber", accountNumber);
        values.put("requestId", requestId);
        values.put("amount", amount);
        values.put("maxAmount", maxAmount);
        values.put("incrementStep", incrementStep);
        values.put("startPrice", startPrice);
        values.put("durationHours", durationHours);
        return values;
    }

    @SuppressWarnings("unchecked")
    public static SocketRequest fromMap(Map<String, Object> values) {
        SocketRequest request = new SocketRequest();
        request.setAction((String) values.get("action"));
        request.setActorUsername((String) values.get("actorUsername"));
        request.setUsername((String) values.get("username"));
        request.setPassword((String) values.get("password"));
        request.setRole((String) values.get("role"));
        request.setFullName((String) values.get("fullName"));
        request.setKeyword((String) values.get("keyword"));
        request.setCategory((String) values.get("category"));
        request.setAuctionId((String) values.get("auctionId"));
        request.setTitle((String) values.get("title"));
        request.setDescription((String) values.get("description"));
        request.setImageHint((String) values.get("imageHint"));
        request.setBankName((String) values.get("bankName"));
        request.setAccountName((String) values.get("accountName"));
        request.setAccountNumber((String) values.get("accountNumber"));
        request.setRequestId((String) values.get("requestId"));
        request.setAmount(asDouble(values.get("amount")));
        request.setMaxAmount(asDouble(values.get("maxAmount")));
        request.setIncrementStep(asDouble(values.get("incrementStep")));
        request.setStartPrice(asDouble(values.get("startPrice")));
        request.setDurationHours(asInt(values.get("durationHours")));
        return request;
    }

    private static double asDouble(Object value) {
        return value instanceof Number number ? number.doubleValue() : 0;
    }

    private static int asInt(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }
}
