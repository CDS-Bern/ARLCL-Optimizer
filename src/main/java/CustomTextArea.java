import java.awt.*;

public class CustomTextArea {

    TextArea gui_TextArea;
    String headless_TextArea;
    boolean editable = false;

    public CustomTextArea(String text, int rows, int columns, int scrollbars) {
        this.gui_TextArea = new TextArea(text, rows, columns, scrollbars);
    }

    public CustomTextArea(String text) {
        this.headless_TextArea = text;
    }

    public void setEditable(boolean b) {
        if (SimApp.headless_mode) {
            editable = b;
        }
        else {
            this.gui_TextArea.setEditable(false);
        }
    }

    public void setBounds(int a, int b, int c,int d) {
        if (!SimApp.headless_mode) {
            this.gui_TextArea.setBounds(a,b,c,d);
        }
    }

    public void setFocusable(boolean b) {
        if (!SimApp.headless_mode) {
            this.gui_TextArea.setFocusable(b);
        }
    }

    public void setEnabled(boolean b) {
        if (!SimApp.headless_mode) {
            this.gui_TextArea.setEnabled(b);
        }
    }

    public void setBackground(Color c) {
        if (!SimApp.headless_mode) {
            this.gui_TextArea.setBackground(c);
        }
    }

    public void setFont(Font f) {
        if (!SimApp.headless_mode) {
            this.gui_TextArea.setFont(f);
        }
    }

    public void setText(String s) {
        if (SimApp.headless_mode) {
            this.headless_TextArea = s;
        }
        else {
            this.gui_TextArea.setText(s);
        }
    }

    public String getText() {
        if (SimApp.headless_mode) {
            return this.headless_TextArea;
        }
        else {
            return this.gui_TextArea.getText();
        }
    }

    public Object getTextArea() {
        if (SimApp.headless_mode) {
            return this.headless_TextArea;
        }
        else{
            return this.gui_TextArea;
        }
    }
}
