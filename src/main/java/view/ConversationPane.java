package view;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.net.URI;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JOptionPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

/** A plain-text conversation with styled, actionable web addresses. */
final class ConversationPane extends JTextPane {
    private static final Object LINK_TARGET = new Object();
    private static final Color LINK_COLOR = new Color(0x1A5FB4);
    private static final Pattern WEB_ADDRESS = Pattern.compile(
            "(?i)\\bhttps?://[^\\s\\p{Z}<>\"`，。！？；：、）】》“”‘’]+");
    private static final Pattern TRAILING_PUNCTUATION = Pattern.compile("[.,!?;:*]+$");

    ConversationPane() {
        this(ConversationPane::openInBrowser);
    }

    ConversationPane(BiConsumer<Component, URI> openLink) {
        setEditable(false);
        setFont(UIManager.getFont("TextArea.font"));
        if (getCaret() instanceof DefaultCaret) {
            ((DefaultCaret) getCaret()).setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
        }
        addLinkActions(openLink);
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    void append(String text) {
        StyledDocument document = getStyledDocument();
        try {
            document.insertString(document.getLength(), text, SimpleAttributeSet.EMPTY);
        }
        catch (BadLocationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    void appendWithLinks(String text) {
        StyledDocument document = getStyledDocument();
        int start = document.getLength();
        append(text);
        Matcher matcher = WEB_ADDRESS.matcher(text);
        while (matcher.find()) {
            String address = webAddress(matcher.group());
            URI target;
            try {
                target = URI.create(address);
            }
            catch (IllegalArgumentException exception) {
                continue;
            }
            if (target.getHost() != null) {
                SimpleAttributeSet link = new SimpleAttributeSet();
                StyleConstants.setUnderline(link, true);
                StyleConstants.setForeground(link, LINK_COLOR);
                link.addAttribute(LINK_TARGET, target);
                document.setCharacterAttributes(start + matcher.start(), address.length(), link, false);
            }
        }
    }

    void truncate(int start) {
        StyledDocument document = getStyledDocument();
        try {
            document.remove(start, document.getLength() - start);
        }
        catch (BadLocationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static String webAddress(String match) {
        String address = TRAILING_PUNCTUATION.matcher(match).replaceFirst("");
        int parentheses = 0;
        int brackets = 0;
        for (int i = 0; i < address.length(); i++) {
            switch (address.charAt(i)) {
                case '(': parentheses++; break;
                case ')': parentheses--; break;
                case '[': brackets++; break;
                case ']': brackets--; break;
                default: break;
            }
        }
        int end = address.length();
        while (end > 0) {
            char last = address.charAt(end - 1);
            if (last == ')' && parentheses < 0) {
                parentheses++;
            }
            else if (last == ']' && brackets < 0) {
                brackets++;
            }
            else {
                break;
            }
            end--;
        }
        return address.substring(0, end);
    }

    private void addLinkActions(BiConsumer<Component, URI> openLink) {
        MouseAdapter links = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent event) {
                setCursor(Cursor.getPredefinedCursor(linkAt(event.getPoint()) == null
                        ? Cursor.TEXT_CURSOR : Cursor.HAND_CURSOR));
            }

            @Override
            public void mouseClicked(MouseEvent event) {
                if (SwingUtilities.isLeftMouseButton(event) && event.getClickCount() == 1
                        && getSelectionStart() == getSelectionEnd()) {
                    URI target = linkAt(event.getPoint());
                    if (target != null) {
                        openLink.accept(ConversationPane.this, target);
                    }
                }
            }
        };
        addMouseListener(links);
        addMouseMotionListener(links);
    }

    private URI linkAt(Point point) {
        int nearest = viewToModel2D(point);
        URI target = linkAt(point, nearest);
        return target != null ? target : linkAt(point, nearest - 1);
    }

    private URI linkAt(Point point, int offset) {
        StyledDocument document = getStyledDocument();
        if (offset < 0 || offset >= document.getLength()) {
            return null;
        }
        Object target = document.getCharacterElement(offset).getAttributes().getAttribute(LINK_TARGET);
        if (target == null) {
            return null;
        }
        try {
            Rectangle2D character = modelToView2D(offset);
            if (character == null) {
                return null;
            }
            double width = getFontMetrics(getFont()).stringWidth(document.getText(offset, 1));
            return point.x >= character.getX() && point.x < character.getX() + width
                    && point.y >= character.getY() && point.y < character.getMaxY() ? (URI) target : null;
        }
        catch (BadLocationException exception) {
            return null;
        }
    }

    private static void openInBrowser(Component parent, URI target) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(target);
                return;
            }
        }
        catch (IOException | RuntimeException exception) {
            // Keep the address visible if the default browser cannot be opened.
        }
        JOptionPane.showMessageDialog(parent, "Could not open the browser. Copy the link from the conversation.",
                "Open link", JOptionPane.WARNING_MESSAGE);
    }
}
