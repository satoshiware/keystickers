/*
 *      This class encapsulates multiple random number generators. Each random
 *      byte is a result of XOR'ing bytes from each generator. This class also
 *      provides the interface required for RNG compatibility and a method that
 *      shows a quick visual check of the integrity of each RNG using the swing
 *      classes.
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
package org.satoshiware.keystickers.random;

import org.satoshiware.satoshicoins.SC;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import javax.swing.*;

public class KSGenerator {
    private final ArrayList<RandomInterface> generators;

    private static final int DIAMETER = 10; // The diameter (pixels) of each dot in the QC check.
    private static final int FRAMESIZE = 800; // The size of each frame in the QC check.
    private static final int DOTSCOUNT = 1500; // Total number of dots used for each RNG QC check.

    public KSGenerator() {
        generators = new ArrayList<>();
    }

    // Add an RNG that uses the RaondomInterface to this instance
    public void addGenerator(RandomInterface ri) {
        generators.add(ri);
    }

    // XOR bytes from each RNG and store the result in the "bytes" array
    public void getBytes(byte[] bytes) {
        for(int i = 0; i < bytes.length; i++) {
            bytes[i] = 0;
            for (RandomInterface generator : generators) {
                bytes[i] = (byte) (bytes[i] ^ generator.getByte());
            }
        }
    }

    // This method shows a quick visual check of the integrity of each RNG using the swing classes
    public void runQC() { // Show dots in random locations. Used for QC (Quality Control) on each RNG.
        for(int i = 0; i < generators.size(); i++) {
            final int iFinal = i + 1;
            String name = generators.get(i).getName();
            JFrameGraphics frameGraphics = new JFrameGraphics(i);
            javax.swing.SwingUtilities.invokeLater(() -> {
                JFrame frame = new JFrame(name);

                try { // Load icon in the upper left of this window
                    String iconPath = new File(SC.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath();
                    frame.setIconImage(Toolkit.getDefaultToolkit().getImage(iconPath.substring(0, iconPath.lastIndexOf(File.separator) + 1) + "classes" + File.separator + "satoshiware_icon.png"));
                } catch (Exception ignored) {
                }

                frame.getContentPane().add(frameGraphics);
                frame.setSize(FRAMESIZE, FRAMESIZE);
                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                frame.setResizable(false);
                frame.setLocationRelativeTo(null);
                frame.setLocation(iFinal * 50, iFinal * 50);
                frame.setVisible(true);
            });
        }
    }
    private class JFrameGraphics extends JPanel{
        private final int index; // Holds the index to a RNG from the generators' ArrayList.

        public JFrameGraphics(int selection) {
            super();
            index = selection;
        }

        public void paint(Graphics g){
            for(int i = 0; i < DOTSCOUNT; i++) {
                g.fillOval((int)(((double)(128 + generators.get(index).getByte()) / 255) * FRAMESIZE), (int)(((double)(128 + generators.get(index).getByte()) / 255) * FRAMESIZE), DIAMETER, DIAMETER);
            }
        }
    }

    // This describes the requirements for RNG compatibility within this class
    public interface RandomInterface {
        byte getByte();

        String getName();
    }
}