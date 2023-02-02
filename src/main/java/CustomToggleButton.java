import javax.swing.*;
import java.awt.event.ActionListener;

public class CustomToggleButton {

    JToggleButton gui_ToggleButton;
    boolean visible;
    boolean enabled;
    boolean clicked;

    public CustomToggleButton(String text) {
        this.gui_ToggleButton = new JToggleButton(text);
    }

    public CustomToggleButton() {
        this.visible = false;
        this.enabled = false;
    }

    public void setVisible(boolean b) {
        if (SimApp.headless_mode) {
            this.visible = b;
        }
        else {
            this.gui_ToggleButton.setVisible(b);
        }
    }

    public void setBounds(int a, int b, int c,int d) {
        if (!SimApp.headless_mode) {
            this.gui_ToggleButton.setBounds(a,b,c,d);
        }
    }

    public void setEnabled(boolean b) {
        if (SimApp.headless_mode) {
            this.enabled = b;
        }
        else {
            this.gui_ToggleButton.setEnabled(b);
        }
    }

    public boolean isEnabled() {
        if (SimApp.headless_mode) {
            return this.enabled;
        }
        else {
            return this.gui_ToggleButton.isEnabled();
        }
    }

    public void setClicked(boolean b) {
        if (SimApp.headless_mode) {
            this.clicked = b;
        }
        else {
            this.gui_ToggleButton.setSelected(b);
        }
    }

    public boolean isClicked() {
        if (SimApp.headless_mode) {
            return this.clicked;
        }
        else {
            return this.gui_ToggleButton.isSelected();
        }
    }

    public void addActionListener(ActionListener l) {
        if (!SimApp.headless_mode) {
            this.gui_ToggleButton.addActionListener(l);
        }
    }

    public Object getToggleButton() {
        return this.gui_ToggleButton;
    }

    public void setText(String value) {
        if (!SimApp.headless_mode) {
            this.gui_ToggleButton.setText(value);
        }
    }
}
