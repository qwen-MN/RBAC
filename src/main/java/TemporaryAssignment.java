import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class TemporaryAssignment extends AbstractRoleAssignment {
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private String expiresAt;
    private boolean autoRenew;

    public TemporaryAssignment(User user, Role role, AssignmentMetadata metadata,
                               String expiresAt, boolean autoRenew) {
        super(user, role, metadata);
        setExpiresAt(expiresAt);
        this.autoRenew = autoRenew;
    }

    private void setExpiresAt(String expiresAt) {
        Objects.requireNonNull(expiresAt, "ExpiresAt cannot be null");
        try {
            LocalDateTime.parse(expiresAt, FORMATTER);
            this.expiresAt = expiresAt;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid date format. Use 'yyyy-MM-dd HH:mm'");
        }
    }

    @Override
    public boolean isActive() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiry = LocalDateTime.parse(expiresAt, FORMATTER);
        return !now.isAfter(expiry);
    }

    @Override
    public String assignmentType() {
        return "TEMPORARY";
    }

    public void extend(String newExpirationDate) {
        setExpiresAt(newExpirationDate);
    }

    public boolean isExpired() {
        return !isActive();
    }

    public String getTimeRemaining() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiry = LocalDateTime.parse(expiresAt, FORMATTER);

        if (now.isAfter(expiry)) {
            return "Expired";
        }

        long minutes = java.time.Duration.between(now, expiry).toMinutes();
        long hours = minutes / 60;
        minutes = minutes % 60;
        long days = hours / 24;
        hours = hours % 24;

        if (days > 0) {
            return String.format("%d days %d hours", days, hours);
        } else if (hours > 0) {
            return String.format("%d hours %d minutes", hours, minutes);
        } else {
            return String.format("%d minutes", minutes);
        }
    }

    @Override
    public String summary() {
        String base = super.summary();
        String expiryInfo = String.format("Expires at: %s | Auto-renew: %s | Remaining: %s",
                expiresAt, autoRenew ? "YES" : "NO", getTimeRemaining());
        return base + "\n" + expiryInfo;
    }
}