import java.awt.*;
import java.awt.event.TextListener;

public class CustomTextField {

    TextField gui_TextField;
    String headless_TextField;
    boolean editable = false;

    public CustomTextField(String text) {
        if (SimApp.headless_mode){
            this.headless_TextField = text;
        }
        else{
            this.gui_TextField = new TextField(text);
        }
    }

    public void setEditable(boolean b) {
        if (SimApp.headless_mode) {
            editable = b;
        }
        else {
            this.gui_TextField.setEditable(false);
        }
    }

    public void setBounds(int a, int b, int c,int d) {
        if (!SimApp.headless_mode) {
            this.gui_TextField.setBounds(a,b,c,d);
        }
    }

    public void setFocusable(boolean b) {
        if (!SimApp.headless_mode) {
            this.gui_TextField.setFocusable(b);
        }
    }

    public void setEnabled(boolean b) {
        if (!SimApp.headless_mode) {
            this.gui_TextField.setEnabled(b);
        }
    }

    public void addTextListener(TextListener l) {
        if (SimApp.headless_mode) {
            //this.headless_TextField.addTextListener(l); // todo we need to add listener here
        }
        if (!SimApp.headless_mode) {
            this.gui_TextField.addTextListener(l);
        }
    }

    public void setFont(Font f) {
        if (!SimApp.headless_mode) {
            this.gui_TextField.setFont(f);
        }
    }

    public void setText(String s) {
        if (SimApp.headless_mode) {
            this.headless_TextField = s;
        }
        else {
            this.gui_TextField.setText(s);
        }
    }

    public void setCaretPosition(int position){
        this.gui_TextField.setCaretPosition(position);
    }

    public String getText() {
        if (SimApp.headless_mode) {
            return this.headless_TextField;
        }
        else {
            return this.gui_TextField.getText();
        }
    }

    public Object getTextField() {
        if (SimApp.headless_mode) {
            return this.headless_TextField;
        }
        else{
            return this.gui_TextField;
        }
    }
}
