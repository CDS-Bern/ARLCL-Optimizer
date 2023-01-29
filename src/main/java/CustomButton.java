import java.awt.*;
import java.awt.event.ActionListener;

public class CustomButton {

    Button gui_Button;
    boolean visible;
    boolean enabled;

    public CustomButton(String text) {
        this.gui_Button = new Button(text);
    }

    public CustomButton() {
        this.visible = false;
        this.enabled = false;
    }

    public void setVisible(boolean b) {
        if (SimApp.headless_mode) {
            this.visible = b;
        }
        else {
            this.gui_Button.setVisible(b);
        }
    }

    public void setBounds(int a, int b, int c,int d) {
        if (!SimApp.headless_mode) {
            this.gui_Button.setBounds(a,b,c,d);
        }
    }

    public void setEnabled(boolean b) {
        if (SimApp.headless_mode) {
            this.enabled = b;
        }
        else {
            this.gui_Button.setEnabled(b);
        }
    }

    public boolean isEnabled() {
        if (SimApp.headless_mode) {
            return this.enabled;
        }
        else {
            return this.gui_Button.isEnabled();
        }
    }

    public void addActionListener(ActionListener l) {
        if (!SimApp.headless_mode) {
            this.gui_Button.addActionListener(l);
        }
    }

    public Object getButton() {
        return this.gui_Button;
    }
}
