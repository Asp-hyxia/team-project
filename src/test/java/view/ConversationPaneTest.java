package view;

import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyleConstants;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConversationPaneTest {
    @Test
    void sendsOnlyTheClickedWebAddressToTheBrowserOpener() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            List<URI> opened = new ArrayList<>();
            ConversationPane pane = new ConversationPane((parent, address) -> opened.add(address));
            pane.setSize(900, 200);
            String address = "https://example.edu/Page_(course)?item=1#milestone";
            String text = "See [the Page](" + address + ").\nAlso shown: file:///tmp/test";
            pane.appendWithLinks(text);

            int start = text.indexOf(address);
            assertTrue(StyleConstants.isUnderline(pane.getStyledDocument()
                    .getCharacterElement(start).getAttributes()));
            assertFalse(StyleConstants.isUnderline(pane.getStyledDocument()
                    .getCharacterElement(start + address.length()).getAttributes()));
            int fileOffset = text.indexOf("file:");
            clickCharacter(pane, fileOffset);
            clickCharacter(pane, start + address.length());
            clickCharacter(pane, start + 7);

            assertEquals(List.of(URI.create(address)), opened);
            assertEquals(text, pane.getText());
        });
    }

    private static void clickCharacter(ConversationPane pane, int offset) {
        try {
            Rectangle2D character = pane.modelToView2D(offset);
            pane.dispatchEvent(new MouseEvent(pane, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0,
                    (int) Math.ceil(character.getX()) + 1, (int) character.getCenterY(), 1, false, MouseEvent.BUTTON1));
        }
        catch (BadLocationException exception) {
            throw new AssertionError(exception);
        }
    }
}
