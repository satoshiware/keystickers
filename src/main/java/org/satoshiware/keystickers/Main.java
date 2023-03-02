/*
 *      Main class for generating Keystickers & Satoshi Coins
 *
 *      This program is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU General Public License as published by
 *      the Free Software Foundation, either version 3 of the License, or
 *      (at your option) any later version.
 *
 *      This program is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU General Public License for more details.
 *
 *      You should have received a copy of the GNU General Public License
 *      along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.satoshiware.keystickers;

import org.satoshiware.keystickers.random.*;

import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import java.awt.*;
import java.awt.print.*;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        if (args.length == 0 || (!args[0].equals("-v") && !args[0].equals("-k") && !args[0].equals("-s"))) {
            System.out.println("Program Argument Required:");
            System.out.println("    -v          Verify checksums of external jar files and run QC verification on the RNGs");
            System.out.println("    -k          Print Keystickers");
            System.out.println("    -s          Print Satoshi Coins");
        } else if (args.length == 1) {
            Scanner scanner = new Scanner(System.in);

            KSGenerator generator = new KSGenerator(); // Container for all random sources. Each random source is mixed (XOR'd) together.
            generator.addGenerator(new SecureRandom());
            generator.addGenerator(new FortunaGenerator(KeyboardEntropy.getEntropy(scanner)));

            int count = 10; // Number of Pages
            boolean outline = false; // Keysticker outline present
            float darkness = 100; // Darkness (%)
            String hrp = "bc"; // Human Readable Prefix for each address
            int amount = 1000000; // Amount of satoshis for each Satoshi Coin

            switch (args[0]) {
                case "-v":
                    generator.runQC();
                    JarChecksums.run(System.getProperty("user.dir") + System.getProperty("file.separator") + "lib");
                    break;
                case "-k":
                    System.out.print("Number of Pages (default = 10): ");
                    try {count = Integer.parseInt(scanner.nextLine());} catch (Exception ignored){}
                    System.out.print("Outline (true/false): ");
                    try {outline = Boolean.parseBoolean(scanner.nextLine());} catch (Exception ignored){}
                    System.out.print("Darkness (0 to 100): ");
                    try {darkness = Float.parseFloat(scanner.nextLine());} catch (Exception ignored){}
                    System.out.print("Human Readable Prefix (default = \"bc\"): ");
                    String nextline = scanner.nextLine();
                    if (!nextline.isEmpty())
                        hrp = nextline;

                    print(generator, count, true, hrp.toLowerCase(), outline, darkness, 0);
                    break;
                case "-s":
                    System.out.print("Number of Pages (default = 10): ");
                    try {count = Integer.parseInt(scanner.nextLine());} catch (Exception ignored){}
                    System.out.print("Satoshi Amount: ");
                    try {amount = Integer.parseInt(scanner.nextLine());} catch (Exception ignored){}
                    if (amount != 1000000 && amount != 500000 && amount != 250000 && amount != 100000 && amount != 50000 && amount != 25000 && amount != 10000)
                        amount = 1000000;

                    print(generator, count, false, "bc", false, 100, amount);
                    break;
                default:
                    System.out.println("Error! Invalid program argument!");
                    break;
            }

            scanner.close();
        } else {
            System.out.println("Error! Too many program arguments!");
        }
    }

    private static void print(KSGenerator generator, int pgTotal, boolean keystickers, String hrp, boolean outline, float darkness, int amount) {
        Paper p = new Paper();
        p.setSize(612, 792); // Paper Size: Letter (8.5" x 11"; 1" = 72 points)
        p.setImageableArea(0, 0, 612, 792); // No margins

        PageFormat pf = new PageFormat(); // This is the page format used for each printed sheet.
        pf.setPaper(p);
        pf.setOrientation(PageFormat.LANDSCAPE);

        Book book = new Book(); // Group of sheets (pages) that are prepared and then sent to the printer of choice.
        for (int j = 1; j <= pgTotal; j++) {
            PrintableKeys sheet;
            if (keystickers) { // Keystickers
                sheet = new KSSheet();

                ((KSSheet) sheet).stickerOutlines = outline;
                ((KSSheet) sheet).privateKeyGreyScale = 100 - darkness;
            } else { // Satoshi Coins
                sheet = new SCSheet();

                ((SCSheet) sheet).satoshiAmount = amount;
            }

            for (int i = 1; i <= sheet.getKeyTotal(); i++) {
                byte[] bytes = new byte[32];
                generator.getBytes(bytes);
                KSKey key = new KSKey(bytes);

                if(hrp.equalsIgnoreCase("tb"))
                    sheet.setPrivateKey(key.getWIF(true), i);
                else
                    sheet.setPrivateKey(key.getWIF(false), i);
                sheet.setPublicKey(key.getP2WPKH(hrp, 0), i);
            }

            book.append(sheet, pf);
        }

        Thread thread = new Thread("Printing") {
            public void run() {
                // Set up an attribute set to remove the icon from the print dialog box.
                Frame frame = new Frame();
                PrintRequestAttributeSet aset = new HashPrintRequestAttributeSet();
                aset.add(javax.print.attribute.standard.DialogTypeSelection.NATIVE);
                aset.add(new javax.print.attribute.standard.DialogOwner(frame));

                try {
                    PrinterJob job = PrinterJob.getPrinterJob();
                    job.setPageable(book);
                    if (job.printDialog(aset)) {
                        job.print();
                    }
                } catch (PrinterException ignored) {}
                frame.dispose();
            }
        };
        thread.start();
    }
}