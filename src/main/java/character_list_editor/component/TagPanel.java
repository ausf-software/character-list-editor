package character_list_editor.component;

import javax.swing.*;
import java.awt.*;


// компонент отображения тега
public class TagPanel extends JPanel {

    private String text;
    private Color backgroundColor;
    private Color darkerColor;
    private int arcSize = 15;
    private int horizontalPadding = 10;
    private int verticalPadding = 5;

    public TagPanel(Color backgroundColor, String text) {
        this.backgroundColor = backgroundColor;
        this.text = text;
        this.darkerColor = backgroundColor.darker().darker();

        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        g2.setColor(backgroundColor);
        g2.fillRoundRect(0, 0, width, height, arcSize, arcSize);

        g2.setColor(darkerColor);
        g2.setFont(getFont());
        FontMetrics fm = g2.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        int textHeight = fm.getAscent();
        int x = (width - textWidth) / 2;
        int y = (height - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(text, x, y);

        g2.dispose();
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(getFont());
        int textWidth = fm.stringWidth(text);
        int textHeight = fm.getHeight();
        int width = textWidth + 2 * horizontalPadding;
        int height = textHeight + 2 * verticalPadding;
        return new Dimension(width, height);
    }

    public void setText(String text) {
        this.text = text;
        revalidate();
        repaint();
    }

    public void setBackgroundColor(Color backgroundColor) {
        this.backgroundColor = backgroundColor;
        this.darkerColor = backgroundColor.darker().darker();
        repaint();
    }
}