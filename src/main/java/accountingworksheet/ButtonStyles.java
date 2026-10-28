package accountingworksheet;

import javax.swing.JButton;
import javax.swing.plaf.basic.BasicButtonUI;

final class ButtonStyles {
    private ButtonStyles() {
    }

    static void useConfiguredColors(JButton button) {
        button.setUI(new BasicButtonUI());
    }
}
