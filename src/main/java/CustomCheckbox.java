import java.awt.*;
import java.awt.event.ItemListener;

public class CustomCheckbox {

    Checkbox gui_Checkbox;
    String label;
    boolean state;
    boolean enabled;

    public CustomCheckbox(String label, boolean state, CheckboxGroup group) {
        this.gui_Checkbox = new Checkbox(label, state, group);
    }

    public CustomCheckbox(String label, boolean state) {
        this.label = label;
        this.state = state;
        this.enabled = true;
    }

    public void setBounds(int a, int b, int c,int d) {
        if (!SimApp.headless_mode) {
            this.gui_Checkbox.setBounds(a,b,c,d);
        }
    }

    public void setEnabled(boolean b) {
        if (SimApp.headless_mode) {
            this.enabled = b;
        }
        else {
            this.gui_Checkbox.setEnabled(b);
        }
    }

    public boolean isEnabled() {
        if (SimApp.headless_mode) {
            return this.enabled;
        }
        else {
            return this.gui_Checkbox.isEnabled();
        }
    }

    public void setState(boolean b) {
        if (SimApp.headless_mode) {
            this.state = b;
        }
        else {
            this.gui_Checkbox.setState(b);
        }
    }

    public boolean getState() {
        if (SimApp.headless_mode) {
            return this.state;
        }
        else {
            return this.gui_Checkbox.getState();
        }
    }

    public Object getCheckbox() {
        return this.gui_Checkbox;
    }

    public void addItemListener(ItemListener i) {
        if (!SimApp.headless_mode) {
            this.gui_Checkbox.addItemListener(i);
        }
    }

    public void setVisible(boolean visibility_state) {
        if (!SimApp.headless_mode) {
            this.gui_Checkbox.setVisible(visibility_state);
        }
    }
}
