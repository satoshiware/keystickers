/*
 *      Java swing app used to collect entropy from mouse movements.
 *
 *      This program is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU General Public License as published by
 *      the Free Software Foundation, either version 3 of the License, or
 *      (at your option) any later version.

 *      This program is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU General Public License for more details.

 *      You should have received a copy of the GNU General Public License
 *      along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.satoshiware.keystickers;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowEvent;

import java.nio.ByteBuffer;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.util.ArrayList;

import javax.swing.*;

public class MouseEntropy extends JPanel implements MouseMotionListener {
    public static int SEEDSIZE = 512; // Number of bytes in the seed; must be a multiple of both ROWSIZE and md5.digest().length

    private static Color BGCOLOR = Color.BLUE; // Background color of the mousePanel
    private static Color DOTCOLOR = Color.RED; // Color of the dots added to the mousePanel
    private static int POINTSIZE = 8; // Diameter (pixels) of the dots added to the mousePanel
    private static int FONTSIZE = 13; // Size of textArea font
    private static int FRAMESIZE = 550; // Overall size of the mouseEntropy window
    private static int ROWSIZE = 32; // Number of bytes for each row in the textArea
    private static int SAMPLESSIZE = 512; // Number of entropy samples to take.
    private static long SAMPLETIME = 50000000; // Minimum time (in nano seconds) between mouse entropy samples.
    private static String[] PREFERREDFONTS = {"Liberation Mono", "Courier New"}; // Fonts ordered by priority.

    private MousePanel mousePanel; // Custom JPanel instance that updates (paints) each captured mouse movement with a dot
    private JTextArea textArea; // Text area shows the entropy collection in real time and overall progress
    private JFrame frame; // Links to the mouseEntropy window; used to repaint and update after each mouse movement

    private ArrayList<Point> mousePoints; // List of all the locations where mouse movements were captured
    private MessageDigest md5; // md5 routine used to mix in the captured mouse entropy with the initial seed

    private long timeStamp; // Additional entropy captured by measuring the "timeStamp" between mouse movement captures
    private int sampleNumber; // Counts the number of mouse movements; helps determine when enough has been captured
    private byte[] seed; // Array links to initial entropy passed in the constructor; it is updated for each mouse movement

    private MouseEntropyCloseEvent closeEvent;

    public MouseEntropy(byte[] entropy, MouseEntropyCloseEvent closeEvent, JFrame frame) {
        super(new GridLayout(0, 1));
        this.setPreferredSize(new Dimension(FRAMESIZE, FRAMESIZE));

        // Creates the portion of the window that captures mouse movements
        mousePanel = new MousePanel();
        mousePanel.addMouseMotionListener(this); // Register mouse events on the mousePanel.
        add(mousePanel);

        // textArea is used to create the scrollPane component; it shows an up-to-date "seed" (entropy) and the overall progress
        textArea = new JTextArea();
        textArea.setEditable(false);
        // Set the best monospaced font that is currently available (see PREFERREDFONTS)
        String[] fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        for(int j = 0; j < PREFERREDFONTS.length; j++) {
            for(int i = 0; i < fonts.length; i++) {
                if (fonts[i].equals(PREFERREDFONTS[j])) {
                    textArea.setFont(new Font(PREFERREDFONTS[j], Font.PLAIN, FONTSIZE));
                    j = PREFERREDFONTS.length;
                    i = fonts.length;
                }
            }
        }
        JScrollPane scrollPane = new JScrollPane(textArea, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(scrollPane);

        this.frame = frame; // Save the link to the overall frame (window) containing THIS panel and textArea component. Used to repaint itself after each mouse capture

        mousePoints = new ArrayList();
        try { md5 = MessageDigest.getInstance("MD5"); } catch(NoSuchAlgorithmException e) {}

        timeStamp = System.nanoTime();
        sampleNumber = 0;

        seed = entropy;
        updateTextArea();

        this.closeEvent = closeEvent;
    }

    // Runs this app to collect mouse entropy and stores it in the "bytes" array
    public static void run(byte[] bytes, MouseEntropyCloseEvent onClose) {
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JFrame frame = new JFrame("Collect Mouse Entropy");
                frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
                JComponent component = new MouseEntropy(bytes, onClose, frame);
                frame.setContentPane(component);
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setLocation(100, 100); // Open up the frame in the middle of the screen
                frame.setVisible(true);
                frame.setResizable(false);
            }
        });
    }

    // MouseMotionListener interface implementation; Routine runs for each captured mouse movement in the mousePanel
    public void mouseMoved(MouseEvent e) {
        if (sampleNumber < SAMPLESSIZE) {
            if (((System.nanoTime() - timeStamp) > SAMPLETIME)) {
                updateSeed(e.getX() * e.getY() * (System.nanoTime() - timeStamp));
                sampleNumber++;
                updateTextArea();

                timeStamp = System.nanoTime();

                mousePoints.add(new Point(e.getX(), e.getY()));
                mousePanel.repaint();
            }
        }else {
            closeEvent.onClose(); // Runs the callback routine passed to the constructor
            this.frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            this.frame.dispatchEvent(new WindowEvent(this.frame, WindowEvent.WINDOW_CLOSING));
        }
    }

    // MouseMotionListener interface implementation; Does nothing
    public void mouseDragged(MouseEvent e) {
    }

    // Custom JPanel that updates (paints) each captured mouse movement with a dot
    private class MousePanel extends JPanel {
        public void paint(Graphics g) {
            g.setColor(BGCOLOR);
            g.fillRect(0, 0, FRAMESIZE + 10, FRAMESIZE + 10);

            g.setColor(DOTCOLOR);
            for (int i = 0; i < mousePoints.size(); i++) {
                g.fillOval(mousePoints.get(i).x, mousePoints.get(i).y, POINTSIZE, POINTSIZE);
            }
        }
    }

    // Routine called from the constructor and mouseMoved events to update the textArea with current seed and overall progress
    private void updateTextArea() {
        textArea.setText(""); // Clear the textArea
        byte[] bytes = new byte[ROWSIZE];
        for(int i = 0; i < SEEDSIZE; i += ROWSIZE) {
            System.arraycopy(seed, i, bytes, 0, ROWSIZE);
            textArea.append(bytesToHex(bytes) + "\n");
        }
        textArea.append("\nPercent Complete: " + Integer.toString(sampleNumber * 100 / SAMPLESSIZE) + "%");
    }
    private static String bytesToHex(byte[] bytes) {
        char[] hexArray = "0123456789ABCDEF".toCharArray();
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    // Captured entropy is mixed in with the original seed
    private void updateSeed(long entropy) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(entropy);
        md5.update(buffer.array());
        byte[] digest = md5.digest();

        for(int i = 0; i < digest.length; i++) {
            digest[i] = (byte)(seed[i + ((sampleNumber * digest.length) % SEEDSIZE)] ^ digest[i]);
        }

        System.arraycopy(digest, 0, seed, ((sampleNumber * digest.length) % SEEDSIZE), digest.length);
    }

    // Interface used to define the callback routine that is called after this app has finished collecting mouse entropy
    public interface MouseEntropyCloseEvent {
        void onClose();
    }
}