package accountingworksheet;

import java.math.BigDecimal;
import java.util.List;

public record AdjustmentWorksheet(long id, String title, List<Entry> entries) {
    public AdjustmentWorksheet {
        entries = List.copyOf(entries);
    }

    public record Entry(
            String accountName,
            AccountType accountType,
            BigDecimal trialBalanceDebit,
            BigDecimal trialBalanceCredit,
            BigDecimal adjustmentDebit,
            BigDecimal adjustmentCredit) {
    }

    public enum AccountType {
        ASSET("Asset"),
        LIABILITY("Liability"),
        CAPITAL("Owner's capital"),
        DRAWING("Owner's drawing"),
        REVENUE("Revenue"),
        EXPENSE("Expense");

        private final String label;

        AccountType(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }

        @Override
        public String toString() {
            return label;
        }

        public static AccountType fromLabel(String label) {
            for (AccountType type : values()) {
                if (type.label.equals(label)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Unknown account type: " + label);
        }
    }
}
