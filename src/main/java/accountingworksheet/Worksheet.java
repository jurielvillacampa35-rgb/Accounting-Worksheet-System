package accountingworksheet;

import java.math.BigDecimal;
import java.util.List;

public record Worksheet(long id, String title, List<Entry> entries) {
    public Worksheet {
        entries = List.copyOf(entries);
    }

    public record Entry(String date, String accountName, BigDecimal debit, BigDecimal credit) {
    }
}
