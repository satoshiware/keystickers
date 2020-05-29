/*
 *      Each instance of this class represents a Keysticker sheet. The class
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

public class KSSheet implements PrintableKeys {
    private static final int PPL = 8; // Stroke width: Controls line thickness for frames and borders.
    private static final String[] PREFERREDFONTS = {"Liberation Serif", "Times New Roman"}; // Fonts ordered by priority.
    private static final int FStyle = Font.PLAIN; // Style of the font (Font.PLAIN, Font.BOLD, Font.ITALIC, (Font.BOLD + Font.ITALIC))

    private static final int PPI = 2400; // Minimum 600 PPI to maintain good quality
    private static final double MARGIN = 0.35; // Printing margin (Inches)
    private static final int KEYTOTAL = 12; // Number of keys per Sheet

    public boolean stickerOutlines; // If true, the Keysticker Borders are drawn. Facilitates debug on regular paper.
    public float privateKeyGreyScale; // The grey scale color for the Private Key (0% - 100%).

    private final String[] fullPubKeyStr = new String[KEYTOTAL]; // Stores the public keys for this sheet
    private final String[] fullPrivKeyStr = new String[KEYTOTAL]; // Stores the private keys for this sheet

    private String thisFont; // Font used for this sheet

    public KSSheet() {
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

        stickerOutlines = false;

        privateKeyGreyScale = 0; // 0% is black

        for (int i = 0; i < KEYTOTAL; i++) {
            fullPrivKeyStr[i] = "";
            fullPubKeyStr[i] = "";
        }
    }

    public int print(Graphics _g, PageFormat pf, int page) throws PrinterException {
        Graphics2D g = (Graphics2D)_g;

        if(pf.getOrientation() != PageFormat.LANDSCAPE) {
            throw new PrinterException("KSSheet class only supports LANDSCAPE");
        }else if(!(pf.getHeight() > (8.4 * 72) && pf.getHeight() < (8.6 * 72) && pf.getWidth() > (10.9 * 72) && pf.getWidth() < (11.1 * 72))) {
            throw new PrinterException("KSSheet class only supports (8.5\" x 11\") paper");
        }else if((pf.getImageableX() / 72) >= MARGIN || (pf.getImageableY() / 72) >= MARGIN || (11.0 - ((pf.getImageableX() / 72) + (pf.getImageableWidth() / 72))) >= MARGIN || (8.5 - ((pf.getImageableY() / 72) + (pf.getImageableHeight() / 72))) > MARGIN) {
            throw new PrinterException("KSSheet Java Class: All Printing Margins must be less than " + MARGIN + "\"");
        }

        g.scale ((double) 72 / PPI, (double) 72 / PPI); // Cheap hack to change graphic resolution. java.awt.print is fixed at 72 PPI.

        // Draw frames around each Keystickers
        drawFrame(g);

        // Draw Keysticker borders if enabled
        if(stickerOutlines) {
            drawKSOutline(g);
        }

        // Write sheet details: "KEYSTICKERS", Page #, Date, and Time
        String sheetDetails = "            KEYSTICKERS                     PAGE: " + (page + 1) + "                     " + DateTimeFormatter.ofPattern("MM/dd/yyyy                     HH:mm:ss").format(LocalDateTime.now());
        drawString(g, sheetDetails, (int)(0.5375 * PPI), (int)(MARGIN * PPI), 90, -(int)getAscent(g, sheetDetails, (int)((0.5375 - MARGIN) * PPI)),0, (int) ((0.5375 - MARGIN) * PPI));

        // Draw Public Key QR codes
        try {
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 2; j++) {
                    drawQR(g, new Point((int)((1.1875 + (3.4375 * i)) * PPI), (int)((1.1750 + (2.05 * j)) * PPI)), (int)(0.75 * PPI), fullPubKeyStr[i + (j * 3)], ErrorCorrectionLevel.M);

                    drawQR(g, new Point((int)((2.9375 + (3.4375 * i)) * PPI), (int)((5.2750 + (2.05 * j)) * PPI)), (int)(0.75 * PPI), fullPubKeyStr[6 + i + (j * 3)], ErrorCorrectionLevel.M);
                }
            }
        } catch (WriterException ignored) {
        }

        // Draw Private Key (QR & string) codes
        if(privateKeyGreyScale > 0 && privateKeyGreyScale <= 100){
            g.setColor(new Color(privateKeyGreyScale / 100, privateKeyGreyScale / 100, privateKeyGreyScale / 100));
        }else {
            g.setColor(Color.BLACK);
        }
        try {
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 2; j++) {
                    final double sizeQR = 0.9; // Size of private key QR code in inches.
                    final double sizeText = 0.1; // Size of Private key text in inches.
                    final double distance = 0.05; // Distance between text and QR code (inches).
                    final double offset = 0.05; // Text begins and ends inline with the edge of the QR code +/- offset (inches).
                    drawQR(g, new Point((int)((2.5625 + (3.4375 * i)) * PPI), (int)((1.1750 + (2.05 * j)) * PPI)), (int)(sizeQR * PPI), fullPrivKeyStr[i + (j * 3)], ErrorCorrectionLevel.H);
                    String[] s = splitPrivKeyString(g, "•" + fullPrivKeyStr[i + (j * 3)]);
                    drawString(g, s[0], (int)((2.5625 + (3.4375 * i)) * PPI), (int)((1.1750 + (2.05 * j)) * PPI), 0, -(int)(((sizeQR / 2) - offset) * PPI), -(int)(((sizeQR / 2) + distance) * PPI), (int) (sizeText * PPI));
                    drawString(g, s[1], (int)((2.5625 + (3.4375 * i)) * PPI), (int)((1.1750 + (2.05 * j)) * PPI), 90, (int)(((sizeQR / 2) + distance)  * PPI), -(int)(((sizeQR / 2) - offset) * PPI), (int) (sizeText * PPI));
                    drawString(g, s[2], (int)((2.5625 + (3.4375 * i)) * PPI), (int)((1.1750 + (2.05 * j)) * PPI), 180, (int)(((sizeQR / 2) - offset) * PPI), (int)(((sizeQR / 2) + distance) * PPI), (int) (sizeText * PPI));
                    drawString(g, s[3] + " ", (int)((2.5625 + (3.4375 * i)) * PPI), (int)((1.1750 + (2.05 * j)) * PPI), 270, -(int)(((sizeQR / 2) + distance)  * PPI), (int)(((sizeQR / 2) - offset)  * PPI), (int) (sizeText * PPI));

                    drawQR(g, new Point((int)((1.5625 + (3.4375 * i)) * PPI), (int)((5.2750 + (2.05 * j)) * PPI)), (int)(sizeQR * PPI), fullPrivKeyStr[6 + i + (j * 3)], ErrorCorrectionLevel.H);
                    s = splitPrivKeyString(g, "•" + fullPrivKeyStr[6 + i + (j * 3)]);
                    drawString(g, s[0], (int)((1.5625 + (3.4375 * i)) * PPI), (int)((5.2750 + (2.05 * j)) * PPI), 180, (int)(((sizeQR / 2) - offset) * PPI), (int)(((sizeQR / 2) + distance) * PPI), (int) (sizeText * PPI));
                    drawString(g, s[1], (int)((1.5625 + (3.4375 * i)) * PPI), (int)((5.2750 + (2.05 * j)) * PPI), 270, -(int)(((sizeQR / 2) + distance)  * PPI), (int)(((sizeQR / 2) - offset)  * PPI), (int) (sizeText * PPI));
                    drawString(g, s[2], (int)((1.5625 + (3.4375 * i)) * PPI), (int)((5.2750 + (2.05 * j)) * PPI), 0, -(int)(((sizeQR / 2) - offset) * PPI), -(int)(((sizeQR / 2) + distance) * PPI), (int) (sizeText * PPI));
                    drawString(g, s[3] + " ", (int)((1.5625 + (3.4375 * i)) * PPI), (int)((5.2750 + (2.05 * j)) * PPI), 90, (int)(((sizeQR / 2) + distance)  * PPI), -(int)(((sizeQR / 2) - offset) * PPI), (int) (sizeText * PPI));
                }
            }
        } catch (WriterException ignored) {
        }
        g.setColor(Color.BLACK);

        // Write truncated Public Keys on the Keystickers
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 2; j++) {
                String s = truncateAddress(g, (int)(0.75 * PPI), fullPubKeyStr[i + (j * 3)], (int)(0.1 * PPI));
                drawString(g, s, (int)((1.6375 + (3.4375 * i)) * PPI), (int)((0.8000 + (2.05 * j)) * PPI), 90, 0, 0, (int) (0.1 * PPI));

                s = truncateAddress(g, (int)(0.75 * PPI), fullPubKeyStr[6 + i + (j * 3)], (int)(0.1 * PPI));
                drawString(g, s, (int)((2.4875 + (3.4375 * i)) * PPI), (int)((5.65 + (2.05 * j)) * PPI), 270, 0, 0, (int) (0.1 * PPI));
            }
        }

        // Write Sticker numbers
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 2; j++) {
                int ascent = (int)getAscent(g, "#" + (i + (j * 3) + 1), (int)(0.2 * PPI));
                final int offset = (int)((0.5375 + (i == 2 ? 6.68125 : 3.24375 * i)) * PPI);
                drawString(g, "#" + (i + (j * 3) + 1), offset, (int)((MARGIN + (1.85 * j)) * PPI), 0, (int)(0.0625 * PPI), ascent, (int) (0.2 * PPI));

                drawString(g, "#" + (7 + i + (j * 3)), offset, (int)((4.2500 + (2.05 * j)) * PPI), 0, (int)(0.0625 * PPI), ascent, (int) (0.2 * PPI));
            }
        }

        return PAGE_EXISTS;

    }

    public int getKeyTotal() {
        return KEYTOTAL;
    }

    public void setPublicKey(String text, int stickerNumber) {
        if (text == null) {
            throw new NullPointerException();
        }
        fullPubKeyStr[stickerNumber - 1] = text;
    }

    public void setPrivateKey(String text, int stickerNumber) {
        if (text == null) {
            throw new NullPointerException();
        }
        fullPrivKeyStr[stickerNumber - 1] = text;
    }

    public String getPrivateKey(int stickerNumber) {
        return fullPrivKeyStr[stickerNumber - 1];
    }

    public String getPublicKey(int stickerNumber) {
        return fullPubKeyStr[stickerNumber - 1];
    }

    @Override
    public KSSheet clone() {
        try {
            KSSheet nSheet = (KSSheet)super.clone();

            for (int i = 1; i <= KEYTOTAL; i++) {
                nSheet.setPublicKey(this.getPublicKey(i), i);
                nSheet.setPrivateKey(this.getPrivateKey(i), i);
            }

            nSheet.stickerOutlines = this.stickerOutlines;
            nSheet.privateKeyGreyScale = this.privateKeyGreyScale;

            return nSheet;
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

    // Draws a table on the sheet so that each Keysticker has a section
    private void drawFrame(Graphics2D g) {
        g.setStroke(new BasicStroke(PPL));

        // Draw Horizontal Lines
        for (int i = 1; i < 4; i++) {
            g.drawLine((int) (PPI * 0.5375), (int) ((0.15 + (2.05 * i)) * PPI), (int) (PPI * (11.0 - MARGIN)), (int) ((0.15 + (2.05 * i)) * PPI));
        }
        g.drawLine((int) (PPI * MARGIN), (int) (PPI * MARGIN), (int) (PPI * 1.6594), (int) (MARGIN * PPI));
        g.drawLine((int) (PPI * 3.2806), (int) (PPI * MARGIN), (int) (PPI * 5.0969), (int) (MARGIN * PPI));
        g.drawLine((int) (PPI * 6.7181), (int) (PPI * MARGIN), (int) (PPI * 8.5344), (int) (MARGIN * PPI));
        g.drawLine((int) (PPI * 10.1556), (int) (PPI * MARGIN), (int) (PPI * (11.0 - MARGIN)), (int) (MARGIN * PPI));

        g.drawLine((int) (PPI * MARGIN), (int) (PPI * (8.5 - MARGIN)), (int) (PPI * 0.8444), (int) ((8.5 - MARGIN) * PPI));
        g.drawLine((int) (PPI * 2.4656), (int) (PPI * (8.5 - MARGIN)), (int) (PPI * 4.2819), (int) ((8.5 - MARGIN) * PPI));
        g.drawLine((int) (PPI * 5.9031), (int) (PPI * (8.5 - MARGIN)), (int) (PPI * 7.7194), (int) ((8.5 - MARGIN) * PPI));
        g.drawLine((int) (PPI * 9.3406), (int) (PPI * (8.5 - MARGIN)), (int) (PPI * (11.0 - MARGIN)), (int) ((8.5 - MARGIN) * PPI));

        // Draw Vertical Lines
        for (int i = 1; i < 3; i++) {
            g.drawLine((int) ((0.34375 + (3.4375 * i)) * PPI), (int) (PPI * MARGIN), (int) ((0.34375 + (3.4375 * i)) * PPI), (int) (PPI * (8.5 - MARGIN)));
        }
        g.drawLine((int) (MARGIN * PPI), (int) (PPI * MARGIN), (int) (PPI * MARGIN), (int) (PPI * (8.5 - MARGIN)));
        g.drawLine((int) (0.5375 * PPI), (int) (PPI * MARGIN), (int) (PPI * 0.5375), (int) (PPI * (8.5 - MARGIN)));

        g.drawLine((int) ((11.0 - MARGIN) * PPI), (int) (PPI * MARGIN), (int) (PPI * (11.0 - MARGIN)), (int) (PPI * (8.5 - MARGIN)));
    }

    // Draws the outline of the Keystickers. Helpful for configuring initial setup.
    private void drawKSOutline(Graphics2D g) {
        g.setStroke(new BasicStroke(PPL));
        for (int i = 0; i < 3; i++) { // Draw the outline for the Keystickers
            for (int j = 0; j < 2; j++) {
                g.drawOval((int) ((1.7625 + (3.4375 * i)) * PPI), (int) ((0.3750 + (2.05 * j)) * PPI), (int) (1.6 * PPI), (int) (1.6 * PPI));

                g.drawArc((int) ((1.6875 + (3.4375 * i)) * PPI), (int) ((0.3 + (2.05 * j)) * PPI), (int) (1.75 * PPI), (int) (1.75 * PPI), 225, 270);

                g.drawArc((int) ((0.6875 + (3.4375 * i)) * PPI), (int) ((0.6750 + (2.05 * j)) * PPI), (int) (0.15 * PPI), (int) (0.15 * PPI), 90, 90);
                g.drawArc((int) ((0.6875 + (3.4375 * i)) * PPI), (int) ((1.525 + (2.05 * j)) * PPI), (int) (0.15 * PPI), (int) (0.15 * PPI), 180, 90);


                g.drawArc((int) ((1.2594 + (3.4375 * i)) * PPI), (int) ((-0.125 + (2.05 * j)) * PPI), (int) (0.8 * PPI), (int) (0.8 * PPI), 270, 45);
                g.drawArc((int) ((1.2594 + (3.4375 * i)) * PPI), (int) ((1.6750 + (2.05 * j)) * PPI), (int) (0.8 * PPI), (int) (0.8 * PPI), 45, 45);

                g.drawLine((int) ((0.6875 + (3.4375 * i)) * PPI), (int) ((0.7500 + (2.05 * j)) * PPI), (int) ((0.6875 + (3.4375 * i)) * PPI), (int) ((1.6 + (2.05 * j)) * PPI));
                g.drawLine((int) ((0.7625 + (3.4375 * i)) * PPI), (int) ((0.6750 + (2.05 * j)) * PPI), (int) ((1.6594 + (3.4375 * i)) * PPI), (int) ((0.6750 + (2.05 * j)) * PPI));
                g.drawLine((int) ((0.7625 + (3.4375 * i)) * PPI), (int) ((1.6750 + (2.05 * j)) * PPI), (int) ((1.6594 + (3.4375 * i)) * PPI), (int) ((1.6750 + (2.05 * j)) * PPI));

                g.drawOval((int) ((0.7625 + (3.4375 * i)) * PPI), (int) ((4.4750 + (2.05 * j)) * PPI), (int) (1.6 * PPI), (int) (1.6 * PPI));

                g.drawArc((int) ((0.6875 + (3.4375 * i)) * PPI), (int) ((4.4 + (2.05 * j)) * PPI), (int) (1.75 * PPI), (int) (1.75 * PPI), 45, 270);

                g.drawArc((int) ((3.2875 + (3.4375 * i)) * PPI), (int) ((4.7750 + (2.05 * j)) * PPI), (int) (0.15 * PPI), (int) (0.15 * PPI), 0, 90);
                g.drawArc((int) ((3.2875 + (3.4375 * i)) * PPI), (int) ((5.625 + (2.05 * j)) * PPI), (int) (0.15 * PPI), (int) (0.15 * PPI), 270, 90);

                g.drawArc((int) ((2.0656 + (3.4375 * i)) * PPI), (int) ((3.9750 + (2.05 * j)) * PPI), (int) (0.8 * PPI), (int) (0.8 * PPI), 225, 45);
                g.drawArc((int) ((2.0656 + (3.4375 * i)) * PPI), (int) ((5.7750 + (2.05 * j)) * PPI), (int) (0.8 * PPI), (int) (0.8 * PPI), 90, 45);

                g.drawLine((int) ((3.4375 + (3.4375 * i)) * PPI), (int) ((5.7000 + (2.05 * j)) * PPI), (int) ((3.4375 + (3.4375 * i)) * PPI), (int) ((4.8500 + (2.05 * j)) * PPI));
                g.drawLine((int) ((3.3625 + (3.4375 * i)) * PPI), (int) ((4.7750 + (2.05 * j)) * PPI), (int) ((2.4656 + (3.4375 * i)) * PPI), (int) ((4.7750 + (2.05 * j)) * PPI));
                g.drawLine((int) ((3.3625 + (3.4375 * i)) * PPI), (int) ((5.7750 + (2.05 * j)) * PPI), (int) ((2.4656 + (3.4375 * i)) * PPI), (int) ((5.7750 + (2.05 * j)) * PPI));
            }
        }
    }

    private void drawQR(Graphics2D g, Point center, int size, String qrStr, ErrorCorrectionLevel errLevel) throws WriterException {
        final Map<EncodeHintType, Object> qrEncoding = new HashMap<>();
        qrEncoding.put(EncodeHintType.ERROR_CORRECTION, errLevel); // L = ~7%, M = ~15%, Q = ~25%, H = ~30%
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

    // Shortens the hashed public Key address while preserving sufficient data for verification. The data shown is selected at random for better security.
    private String truncateAddress(Graphics2D g, int width, String address, int size) {
        String trnAddress = address;

        if (g.getFontMetrics(new Font(thisFont, FStyle, size)).stringWidth(trnAddress) < width)
            return trnAddress;

        // Select random portion of the address after the first 4 characters.
        int iRnd = (int)(Math.random() * (trnAddress.length() - 4)) + 4; // 4 <= iRnd < (trnText.length() - 4)
        if (iRnd != 4)
            trnAddress = trnAddress.substring(0, 4) + "..." + trnAddress.substring(iRnd);

        if (g.getFontMetrics(new Font(thisFont, FStyle, size)).stringWidth(trnAddress) >= width) { // String is too long; let's shorten it.
            while (g.getFontMetrics(new Font(thisFont, FStyle, size)).stringWidth(trnAddress + "...") >= width)
                trnAddress = trnAddress.substring(0, trnAddress.length() - 1);

            return trnAddress + "...";
        } else if (g.getFontMetrics(new Font(thisFont, FStyle, size)).stringWidth(trnAddress) <= (int)(width * 0.8)) { // String too short (less than 80% of desired width); let's try again (Recursion).
            return truncateAddress(g, width, address, size);
        } else { // Length of string is about right.
            return trnAddress;
        }
    }

    // Splits the text of a private key (into a 4 element string array) so that it can be wrapped around the private key QR code.
    private String[] splitPrivKeyString(Graphics2D g, String key) {
        String[] strArray = new String[4];
        strArray[0] = "";
        strArray[1] = "";

        for(int i = 0; i < 3; i++) {
            String trnText = key.substring(strArray[0].length() + strArray[1].length());
            while (g.getFontMetrics(new Font(thisFont, FStyle, 240)).stringWidth(trnText) >= 1920)
                trnText = trnText.substring(0, trnText.length() - 1);
            strArray[i] = trnText;
        }
        strArray[3] = key.substring(strArray[0].length() + strArray[1].length() + strArray[2].length());

        return strArray;
    }
}