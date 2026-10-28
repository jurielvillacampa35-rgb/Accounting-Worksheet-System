package accountingworksheet;

final class AppTheme {
    private static boolean darkMode = true;

    private AppTheme() {
    }

    static boolean isDarkMode() {
        return darkMode;
    }

    static void setDarkMode(boolean enabled) {
        darkMode = enabled;
    }
}
