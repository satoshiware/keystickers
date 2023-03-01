/*
 *      Each instance of this class represents a "Satoshi Coins" sheet. The class
 *      implements the "PrintableKeys" interface for easier key management
 *      before printing.
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

import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.EncodeHintType;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.google.zxing.WriterException;

import java.awt.*;
import java.awt.Font;
import java.awt.font.LineMetrics;
import java.awt.font.TextAttribute;
import java.awt.geom.AffineTransform;
import java.awt.print.PageFormat;
import java.awt.print.PrinterException;
import java.text.AttributedString;

import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;

import java.util.*;

public class SCSheet implements PrintableKeys {
    private static final int PPL = 8; // Stroke width: Controls line thickness for frames and borders.
    private static final String[] PREFERREDFONTS = {"Liberation Serif", "Times New Roman"}; // Fonts ordered by priority.
    private static final int FStyle = Font.PLAIN; // Style of the font (Font.PLAIN, Font.BOLD, Font.ITALIC, (Font.BOLD + Font.ITALIC))

    private static final int PPI = 2400; // Minimum 600 PPI to maintain good quality
    private static final double MARGIN = 0.35; // Printing margin (Inches)
    private static final int KEYTOTAL = 24; // Number of keys per Sheet

    public int satoshiAmount; // The amount of $atoshis that will be on the intended coins (initialized externally).

    private String thisFont; // Font used for this sheet

    private final String[] fullPubKeyStr; // Stores the public keys for this sheet
    private final String[] fullPrivKeyStr; // Stores the private keys for this sheet

    private boolean tstFlag; // If any public or private key added is not for mainnet, this flag is set
                             // A Private Key QR will be included with each set of keys and they will be marked "Testing"
    public SCSheet() {
        String[] fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        for(int j = 0; j < PREFERREDFONTS.length; j++) {
            for(int i = 0; i < fonts.length; i++) {
                if (fonts[i].equals(PREFERREDFONTS[j])) {
                    thisFont = PREFERREDFONTS[j];
                    j = PREFERREDFONTS.length;
                    i = fonts.length;
                }
            }
        }

        fullPubKeyStr = new String[KEYTOTAL];
        fullPrivKeyStr = new String[KEYTOTAL];
        for (int i = 0; i < KEYTOTAL; i++) {
            fullPrivKeyStr[i] = "";
            fullPubKeyStr[i] = "";
        }

        tstFlag = false;
    }

    public int print(Graphics _g, PageFormat pf, int page) throws PrinterException {
        Graphics2D g = (Graphics2D)_g;

        if(pf.getOrientation() != PageFormat.LANDSCAPE) {
            throw new PrinterException("SCSheet class only supports LANDSCAPE");
        }else if(!(pf.getHeight() > (8.4 * 72) && pf.getHeight() < (8.6 * 72) && pf.getWidth() > (10.9 * 72) && pf.getWidth() < (11.1 * 72))) {
            throw new PrinterException("SCSheet class only supports (8.5\" x 11\") paper");
        }else if((pf.getImageableX() / 72) >= MARGIN || (pf.getImageableY() / 72) >= MARGIN || (11.0 - ((pf.getImageableX() / 72) + (pf.getImageableWidth() / 72))) >= MARGIN || (8.5 - ((pf.getImageableY() / 72) + (pf.getImageableHeight() / 72))) > MARGIN) {
            throw new PrinterException("SCSheet Java Class: All Printing Margins must be less than " + MARGIN + "\"");
        }

        g.scale ((double) 72 / PPI, (double) 72 / PPI); // Cheap hack to change graphic resolution. java.awt.print is fixed at 72 PPI.

        // Draw frames around each Satoshi Coin key
        drawFrame(g);

        // Write sheet details: "SATOSHI COINS", Page #, Date, and Time
        String sheetDetails = "           SATOSHI COINS                    PAGE: " + (page + 1) + "                     " + DateTimeFormatter.ofPattern("MM/dd/yyyy                     HH:mm:ss").format(LocalDateTime.now());
        drawString(g, sheetDetails, (int)(0.5375 * PPI), (int)(MARGIN * PPI), 90, -(int)getAscent(g, sheetDetails, (int)((0.5375 - MARGIN) * PPI)),0, (int) ((0.5375 - MARGIN) * PPI));

        // Draw QR codes
        try {
            for (int i = 1; i <= 4; i++) {
                for (int j = 0; j < 6; j++) {
                    drawQR(g, new Point((int)((0.25 + (2.625 * i) + (i == 4 ? (0.25 - MARGIN) : 0) - 0.4) * PPI), (int)(((j == 0 ? MARGIN : 0.25) + (1.33 * j) + 0.4) * PPI)), (int)(0.6 * PPI), fullPubKeyStr[(i - 1) + (j * 4)] + " " + satoshiAmount);
                }
            }
        } catch (WriterException ignored) {
        }

        // Draw the address circumscribed within a circle and between two black dots.
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 6; j++) {
                final double offsetAddress = (0.93 + (2.625 * i) + (i == 0 ? 0.14 : 0)) * PPI;
                circumscribeAddress(g, fullPubKeyStr[i + (j * 4)].substring(3, 12), (int) offsetAddress, (int)((0.44 + (1.33 * j)) * PPI));
                g.drawOval((int) offsetAddress, (int)((0.44 + (1.33 * j)) * PPI), (int) (0.47 * PPI), (int)(0.47 * PPI));

                final double offsetAddressMarkers = (1.105 + (2.625 * i) + (i == 0 ? 0.14 : 0)) * PPI;
                g.fillOval((int) offsetAddressMarkers, (int)((0.44 + (1.33 * j)) * PPI), (int)(0.12 * PPI), (int)(0.12 * PPI));
                g.fillOval((int) offsetAddressMarkers, (int)((0.79 + (1.33 * j)) * PPI), (int)(0.12 * PPI), (int)(0.12 * PPI));

                if(tstFlag) { // Draw a QR of the private key if in test mode
                    try { drawQR(g, new Point((int)((1.66 + (2.625 * i) + (i == 0 ? 0.14 : 0)) * PPI), (int)((0.675 + (1.33 * j)) * PPI)), (int)(0.4 * PPI), fullPrivKeyStr[i + (j * 4)]);} catch (WriterException ignored) {}
                }
            }
        }

        // Split the private key into two and draw them circumscribed within two circles.
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 6; j++) {
                String[] s = splitPrivKeyString(g, fullPrivKeyStr[i + (j * 4)]);

                final double offsetPrivateKey1 = (0.57 + (2.625 * i) + (i == 0 ? 0.14 : 0)) * PPI;
                circumscribeKey12mmD0p08inTxt(g, "1•" + s[0], (int) offsetPrivateKey1, (int)((0.94 + (1.33 * j)) * PPI));
                g.drawOval((int) offsetPrivateKey1, (int)((0.94 + (1.33 * j)) * PPI), (int)(0.47 * PPI), (int)(0.47 * PPI));

                final double offsetPrivateKey2 = (1.29 + (2.625 * i) + (i == 0 ? 0.14 : 0)) * PPI;
                circumscribeKey12mmD0p08inTxt(g, "2•" + s[1], (int) offsetPrivateKey2, (int)((0.94 + (1.33 * j)) * PPI));
                g.drawOval((int) offsetPrivateKey2, (int)((0.94 + (1.33 * j)) * PPI), (int)(0.47 * PPI), (int)(0.47 * PPI));
            }
        }

        // Write verifying information for each key: truncated address, and $atoshi amount.
        for (int i = 1; i <= 4; i++) {
            for (int j = 0; j < 6; j++) {
                final double textX = (0.25 + (2.625 * i) + (i == 4 ? (0.25 - MARGIN) : 0) - 0.7) * PPI;
                final double textY = ((j == 0 ? MARGIN : 0.25) + (1.33 * j) + 0.8) * PPI;
                drawString(g, fullPubKeyStr[(i - 1) + (j * 4)].substring(3, 12), (int) textX, (int) textY, 0, 0, (int)(0.075 * PPI), (int)(0.12 * PPI));
                drawString(g, String.format("%,d", satoshiAmount) + " $", (int) textX, (int) textY, 0, 0, (int)(0.225 * PPI), (int)(0.12 * PPI));
                if(tstFlag) // Write "TESTNET" near the keys if in test mode
                    drawString(g, "TESTING", (int) textX, (int) textY, 0, 0, (int)(0.375 * PPI), (int)(0.12 * PPI));
            }
        }

        // Number the keys for each Satoshi Coins
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 6; j++) {
                drawString(g, "#" + (i + (j * 4) + 1), (int)(((i == 0 ? 0.6375 : 0.35) + (2.625 * i)) * PPI), (int)(((j == 0 ? MARGIN : 0.25) + 0.1 + (1.33 * j)) * PPI), 0, (int)(0.02 * PPI), (int)(0.15 * PPI), (int) (0.2 * PPI));
            }
        }

        return PAGE_EXISTS;
    }

    public int getKeyTotal() {
        return KEYTOTAL;
    }

    public void setPublicKey(String text, int keyIndex) {
        if (text == null) {
            throw new NullPointerException();
        }
        fullPubKeyStr[keyIndex - 1] = text;

        if(!text.startsWith("bc1q")) {
            tstFlag = true;
        }
    }

    public void setPrivateKey(String text, int keyIndex) {
        if (text == null) {
            throw new NullPointerException();
        }
        fullPrivKeyStr[keyIndex - 1] = text;

        if(!(text.startsWith("L") || text.startsWith("K"))) {
            tstFlag = true;
        }
    }

    public String getPrivateKey(int keyIndex) {
        return fullPrivKeyStr[keyIndex - 1];
    }

    public String getPublicKey(int keyIndex) {
        return fullPubKeyStr[keyIndex - 1];
    }

    @Override
    public KSSheet clone() {
        try {
            KSSheet nSheet = (KSSheet)super.clone();

            for (int i = 1; i <= KEYTOTAL; i++) {
                nSheet.setPublicKey(this.getPublicKey(i), i);
                nSheet.setPrivateKey(this.getPrivateKey(i), i);
            }

            return nSheet;
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

    private void drawFrame(Graphics2D g) {
        g.setStroke(new BasicStroke(PPL));

        // Draw Horizontal Lines
        for (int i = 1; i < 6; i++) {
            g.drawLine((int) (PPI * 0.5375), (int) ((0.25 + (1.33 * i)) * PPI), (int) (PPI * (11.0 - MARGIN)), (int) ((0.25 + (1.33 * i)) * PPI));
        }
        g.drawLine((int) (PPI * MARGIN), (int) (PPI * MARGIN), (int) (PPI * (11.0 - MARGIN)), (int) (MARGIN * PPI));
        g.drawLine((int) (PPI * MARGIN), (int) (PPI * (8.5 - MARGIN)), (int) (PPI * (11.0 - MARGIN)), (int) ((8.5 - MARGIN) * PPI));

        // Draw Vertical Lines
        for (int i = 1; i < 4; i++) {
            g.drawLine((int) ((0.25 + (2.625 * i)) * PPI), (int) (PPI * MARGIN), (int) ((0.25 + (2.625 * i)) * PPI), (int) (PPI * (8.5 - MARGIN)));
        }
        g.drawLine((int) (MARGIN * PPI), (int) (PPI * MARGIN), (int) (PPI * MARGIN), (int) (PPI * (8.5 - MARGIN)));
        g.drawLine((int) (0.5375 * PPI), (int) (PPI * MARGIN), (int) (PPI * 0.5375), (int) (PPI * (8.5 - MARGIN)));

        g.drawLine((int) ((11.0 - MARGIN) * PPI), (int) (PPI * MARGIN), (int) (PPI * (11.0 - MARGIN)), (int) (PPI * (8.5 - MARGIN)));

        // Draw separation markers. These show where to cut in order to isolate the QR, $atoshi amount, and verification address.
        for (int i = 1; i <= 4; i++) {
            final double offsetMarkers = (0.25 + (2.625 * i) + (i == 4 ? (0.25 - MARGIN) : 0) - 0.8) * PPI;
            for (int j = 0; j < 6; j++) {
                g.drawLine((int) offsetMarkers, (int) (((j == 0 ? MARGIN : 0.125) + (1.33 * j)) * PPI), (int) offsetMarkers, (int) (((j == 0 ? MARGIN + 0.125 : 0.375) + (1.33 * j)) * PPI));
            }

            g.drawLine((int) offsetMarkers, (int) ((8.5 - MARGIN) * PPI), (int) offsetMarkers, (int) ((8.5 - MARGIN - 0.125) * PPI));
        }

    }

    private void drawQR(Graphics2D g, Point center, int size, String qrStr) throws WriterException {
        final Map<EncodeHintType, Object> qrEncoding = new HashMap<>();
        qrEncoding.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M); // L = ~7%, M = ~15%, Q = ~25%, H = ~30%
        qrEncoding.put(EncodeHintType.MARGIN, 0);

        BitMatrix m = (new QRCodeWriter()).encode(qrStr, BarcodeFormat.QR_CODE, 0, 0, qrEncoding);

        int dotSize = size / m.getWidth(); // Integer division: the result is an integer and is truncated (fractional part thrown away). Not rounded to the closest integer.
        double r = (double) (size % m.getWidth()) / (double) m.getWidth(); // Decimal remainder: used to help make the QR code the exact size.
        Point start = new Point(center.x - (size / 2), center.y - (size / 2));

        int xPixelCount = 0;
        int xPixel;
        for (int _x = 0; _x < m.getWidth(); _x++) {
            xPixel = (int) (r * (double) (_x + 1)) - (int) (r * (double) _x);
            int yPixelCount = 0;
            int yPixel;
            for (int _y = 0; _y < m.getWidth(); _y++) {
                yPixel = (int) (r * (double) (_y + 1)) - (int) (r * (double) _y);
                if (m.get(_x, _y)) {
                    g.fillRect(start.x + (_x * dotSize) + xPixelCount, start.y + (_y * dotSize) + yPixelCount, dotSize + xPixel, dotSize + yPixel);
                }
                yPixelCount = yPixelCount + yPixel;
            }
            xPixelCount = xPixelCount + xPixel;
        }
    }

    // Adds text to the sheet at the desired location, orientation, and size
    private void drawString(Graphics2D g, String text, int x, int y, double angle, int xOffset, int yOffset, int size) {
        Font f = new Font(thisFont, FStyle, size);

        AffineTransform affineTransform = new AffineTransform();
        affineTransform.rotate(Math.toRadians(angle), 0, 0);
        Font rF = f.deriveFont(affineTransform);

        AttributedString as = new AttributedString(text);
        as.addAttribute(TextAttribute.FONT, rF);
        g.drawString(as.getIterator(), x + xOffset, y + yOffset);
    }

    // Gets the ascent from the font and its size; The recommended distance above the baseline for singled spaced text
    private float getAscent(Graphics2D g, String text, int size) { // The distance from the baseline to the ascender line.
        Font f = new Font(thisFont, FStyle, size);
        LineMetrics lineMetrics = f.getLineMetrics(text, g.getFontRenderContext());
        return lineMetrics.getAscent();
        // The ascent usually represents the the height of the capital letters of the text. Some characters can extend above the ascender line.
    }

    // Write the truncated address (tAddress) within the given circle.
    private void circumscribeAddress(Graphics2D g, String tAddress, int x, int y) {
        final int tAddressWidth = g.getFontMetrics(new Font(thisFont, FStyle, 192)).stringWidth(tAddress);
        final float ascent = getAscent(g, tAddress, 192);

        drawString(g, tAddress, x, y, 0, (1128 - tAddressWidth) / 2, (1128 / 2) + (int)(ascent / 4) - 192, 192);
        drawString(g, tAddress, x, y, 0, (1128 - tAddressWidth) / 2, (1128 / 2) + (int)(ascent / 4), 192);
        drawString(g, tAddress, x, y, 0, (1128 - tAddressWidth) / 2, (1128 / 2) + (int)(ascent / 4) + 192, 192);
    }

    // Divides the private key string (by text length not character length) into two parts.
    private String[] splitPrivKeyString(Graphics2D g, String key) {
        String[] strArray = new String[2];

        int targetWidth = g.getFontMetrics(new Font(thisFont, FStyle, PPI)).stringWidth(key) / 2;

        int index = 0;
        while (g.getFontMetrics(new Font(thisFont, FStyle, PPI)).stringWidth(key.substring(index)) > targetWidth)
            index++;

        if(index != 0) {
            strArray[0] = key.substring(0, index);
            strArray[1] = key.substring(index);
        } else {
            strArray[0] = key;
            strArray[1] = "";
        }

        return strArray;
    }

    // Write the key (0.08" text) within a 12mm circle.
    private void circumscribeKey12mmD0p08inTxt(Graphics2D g, String key, int x, int y) {
        final int diameter = (int)(0.47 * PPI);
        final int txtSize = (int)(0.08 * PPI);

        final float ascent = getAscent(g, key, txtSize);

        int[] widths = new int[5]; // Maximum widths for each of the 5 lines of text
        widths[0] = (int)(diameter * 0.60) - txtSize;
        widths[1] = (int)(diameter * 0.85) - txtSize;
        widths[2] = (int)(diameter * 1.00) - txtSize;
        widths[3] = (int)(diameter * 0.85) - txtSize;
        widths[4] = (int)(diameter * 0.60) - txtSize;

        for(int i = 0; i < 5; i++) {
            String s = "";
            while (key.length() != 0 && g.getFontMetrics(new Font(thisFont, FStyle, txtSize)).stringWidth(s) <= widths[i]) {
                s += key.substring(0, 1);
                key = key.substring(1);
            }

            if(s.length() != 0)
                drawString(g, s, x, y, 0, (diameter - g.getFontMetrics(new Font(thisFont, FStyle, txtSize)).stringWidth(s)) / 2, (diameter / 2) + (int)(ascent / 4) + (txtSize * (i - 2)), txtSize);
        }
        if(key.length() != 0) // Void the private key if it was not able to fit in the circle.
            drawString(g, "***VOID***", x, y, 0, (diameter - g.getFontMetrics(new Font(thisFont, FStyle, txtSize * 2)).stringWidth("***VOID***")) / 2, (diameter / 2) + (int)(ascent / 2), txtSize * 2);
    }
}