# Accounting Worksheet

A Java desktop application for signing in and preparing trial balance worksheets. It uses Swing for the interface and SQLite for local data storage.

## Requirements

- Java 25 or later
- Apache Maven

The desktop interface uses antialiased Segoe UI text and Java's native high-DPI scaling, so
fonts and controls stay clear on high-resolution displays. Use the operating system's display
scaling setting if you want the interface to appear larger or smaller.

## Run

From this folder, run:

```powershell
mvn clean compile exec:java
```

On first launch, create the initial account. Later launches show the sign-in screen; select **Create an account** to add another user. Each account has its own worksheets. Passwords must have at least 8 characters and are stored as salted PBKDF2 hashes.

### Create an administrator account

Create administrators from a terminal, not from the sign-up form. From the project folder, run:

```powershell
mvn exec:java "-Dexec.args=--create-admin admin@example.com"
```

Enter and confirm the password at the hidden terminal prompts. Use a password with at least 8 characters. The application stores the account with the `ADMIN` role; accounts created through the sign-up form always have the regular `USER` role. Sign in normally, then open **Admin console** from the dashboard to view registered users and all trial-balance and 12-column worksheets. The command uses the database in the current working folder.

After signing in, the dashboard offers trial balance and 12-column adjustment worksheets. In the adjustment worksheet, enter account titles, account types (asset, liability, owner's capital, owner's drawing, revenue, or expense), trial balance amounts, and adjustment amounts. The six debit/credit pairs are trial balance, adjustments, adjusted trial balance, income statement, owner's equity, and balance sheet. The last four pairs are calculated, including net income or loss. Trial balance and adjustment debits must each equal their credits before saving. Worksheets can be saved and reopened from the dashboard.

The SQLite database file `accounting_worksheet.db` is created in the folder where the application is launched. The schema is in [`src/main/resources/schema.sql`](src/main/resources/schema.sql).

## Worksheet

Add account rows and enter a debit or credit amount for each account (not both). Amounts accept up to two decimal places. Save is enabled for worksheets with account names and equal, nonzero debit and credit totals. Saved worksheets can be reopened from the list in the worksheet window.
