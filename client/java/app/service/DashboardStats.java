package app.service;

public record DashboardStats(
        long openAuctions,
        long finishedAuctions,
        long sellers,
        long bidders,
        double totalVolume,
        long paidAuctions,
        long autoBidRules,
        long notificationCount
) {
}
