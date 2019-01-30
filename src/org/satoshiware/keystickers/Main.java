/*
 *      Main class (GUI) for generating and verifying Keystickers
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

import java.awt.*;
import java.awt.event.*;
import java.awt.print.*;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.StringTokenizer;

import javax.swing.*;

public class Main extends JFrame {
    private JPanel mainPanel;

    private JButton btnSelectQCFileDest;
    private JButton btnSelectExtSrc;
    private JButton btnRndQCVisual;
    private JButton btnPrint;
    private JButton btnQCVerify;

    private JLabel lblTotalPages;
    private JLabel lblHorizontalOffset;
    private JLabel lblVerticalOffset;
    private JLabel lblRotation;
    private JLabel lblFGGrayscale;
    private JLabel lblBGGrayscale;
    private JLabel lblKeyType;
    private JLabel lblQCDestFile;
    private JLabel lblRandomInfo;
    private JLabel lblRandomInfo2;
    private JLabel lblRandomInfo3;

    private JCheckBox checkReserved;
    private JCheckBox checkTestnet;
    private JCheckBox checkOutline;
    private JCheckBox checkGrayScaleTest;

    private JTextField txtHorizontalOffset;
    private JTextField txtVerticalOffset;
    private JTextField txtRotation;
    private JTextField txtBGGrayscale;
    private JTextField txtFGGrayscale;
    private JTextField txtTotalPages;

    private JComboBox comboKeyType;

    private KSGenerator generator; // Container for all random sources. Each random source is mixed (XOR'd) together.
    private byte[] entropy; // Entropy generated from mouse movements.
    private File destQCFile; // Destination file where QC and job information is written.
    private MouseEntropyClosingHandle entropyHandle; // Called after Mouse Entropy has successfully closed.
    private ExtRNG extRNG; // RNG that streams from a HWRNG (True Random Number Generator) or ".dat" file with plenty of raw random data.

    public Main(String title) {
        super(title);
        this.setContentPane(mainPanel);

        // Configure Combo Box (comboKeyType) used to select Bitcoin address format
        comboKeyType.addItem(new ComboItem("Native Segwit (Bech32)", "Segwit"));
        comboKeyType.addItem(new ComboItem("Legacy Address", "Legacy"));

        generator = new KSGenerator();
        generator.addGenerator(new SecureRandom());
        lblRandomInfo.setText("RNG #1: java.security.SecureRandom");

        entropy = new byte[MouseEntropy.SEEDSIZE];
        generator.getBytes(entropy); // Initial seeding of mouse entropy pool
        entropyHandle = new MouseEntropyClosingHandle();
        MouseEntropy.run(entropy, entropyHandle); // Get mouse entropy
        lblRandomInfo2.setText("RNG #2: Collecting Mouse Entropy");

        extRNG = null;
        lblRandomInfo3.setText("RNG #3: External RNG Source Not Selected");

        destQCFile = null;
        btnSelectQCFileDest.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fc = new JFileChooser();
                if(destQCFile != null)
                    fc.setCurrentDirectory(new java.io.File(destQCFile.getPath()));
                else
                    fc.setCurrentDirectory(new java.io.File("."));
                fc.setDialogTitle("Select Folder: QC File Destination");
                fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                fc.setAcceptAllFileFilterUsed(false);
                if (fc.showOpenDialog(mainPanel) == JFileChooser.APPROVE_OPTION) {
                    destQCFile = fc.getSelectedFile();
                    lblQCDestFile.setText("QC Destination: " + destQCFile.getName());
                }
            }
        });

        checkGrayScaleTest.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(checkGrayScaleTest.isSelected()) {
                    txtBGGrayscale.setText("NA");
                    txtBGGrayscale.setEditable(false);
                    txtFGGrayscale.setText("NA");
                    txtFGGrayscale.setEditable(false);
                    txtTotalPages.setText("1");
                }else {
                    txtBGGrayscale.setEditable(true);
                    txtBGGrayscale.setText("0");
                    txtFGGrayscale.setEditable(true);
                    txtFGGrayscale.setText("100");
                }
            }
        });
        checkOutline.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(checkOutline.isSelected()) {
                    txtTotalPages.setText("1");
                }
            }
        });

        btnSelectExtSrc.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fc = new JFileChooser();
                fc.setCurrentDirectory(new java.io.File("."));

                fc.setDialogTitle("Select External RNG Source");
                fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
                if (fc.showOpenDialog(mainPanel) == JFileChooser.APPROVE_OPTION) {
                    extRNG = new ExtRNG(fc.getSelectedFile());
                    if(extRNG.open() && (extRNG.isFile() ? true : extRNG.baudrate()) && extRNG.fillStack()) {
                        if(fc.getSelectedFile().getPath().length() > 45) {
                            lblBGGrayscale.setText("RNG #3: ..." + fc.getSelectedFile().getPath().substring(fc.getSelectedFile().getPath().length() - 42));
                        }else {
                            lblRandomInfo3.setText("RNG #3: " + fc.getSelectedFile());
                        }

                        generator.addGenerator(extRNG);
                        btnSelectExtSrc.setEnabled(false);
                    }
                }
            }
        });
        btnRndQCVisual.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                generator.runQC();
            }
        });

        btnPrint.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean exceptionThrown = false;
                try {
                    Double.parseDouble(txtRotation.getText());
                    Double.parseDouble(txtVerticalOffset.getText());
                    Double.parseDouble(txtHorizontalOffset.getText());
                    if(!checkGrayScaleTest.isSelected()) {
                        Float.parseFloat(txtBGGrayscale.getText());
                        Float.parseFloat(txtFGGrayscale.getText());
                    }
                    Integer.parseInt(txtTotalPages.getText());
                }catch (Exception ex) {
                    exceptionThrown = true;
                    JOptionPane.showMessageDialog(mainPanel, "One or more inputs are invalid.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
                }
                if (!exceptionThrown &&
                        ((checkGrayScaleTest.isSelected() ? false : (Float.parseFloat(txtBGGrayscale.getText()) > 100)) ||
                        (checkGrayScaleTest.isSelected() ? false : (Float.parseFloat(txtBGGrayscale.getText()) < 0)) ||
                        (checkGrayScaleTest.isSelected() ? false : (Float.parseFloat(txtFGGrayscale.getText()) > 100)) ||
                        (checkGrayScaleTest.isSelected() ? false : (Float.parseFloat(txtFGGrayscale.getText()) < 0)) ||
                        Double.parseDouble(txtRotation.getText()) < -30 ||
                        Double.parseDouble(txtRotation.getText()) > 30 ||
                        Double.parseDouble(txtVerticalOffset.getText()) < -1 ||
                        Double.parseDouble(txtVerticalOffset.getText()) > 1 ||
                        Double.parseDouble(txtHorizontalOffset.getText()) < -1 ||
                        Double.parseDouble(txtHorizontalOffset.getText()) > 1 ||
                        Integer.parseInt(txtTotalPages.getText()) > 500 ||
                        Integer.parseInt(txtTotalPages.getText()) <= 0)) {
                    exceptionThrown = true;
                    JOptionPane.showMessageDialog(mainPanel, "One or more inputs are invalid or too extreme.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
                }

                if(!exceptionThrown) {
                    if(checkOutline.isSelected() || checkGrayScaleTest.isSelected()) {
                        print();
                    } else if(!entropyHandle.isMouseEntropyCollected || (destQCFile == null)) {
                        String str;
                        if(!entropyHandle.isMouseEntropyCollected && (destQCFile == null))
                            str = "Mouse Entropy is not fully collected and destination for QC file is not set. Continue?";
                        else if(destQCFile == null)
                            str = "Destination for QC file is not set. Continue?";
                        else
                            str = "Mouse Entropy is not fully collected. Continue?";

                        int dialogResult = JOptionPane.showConfirmDialog (null, str,"Warning", JOptionPane.YES_NO_OPTION);
                        if(dialogResult == JOptionPane.YES_OPTION){
                            print();
                        }
                    } else {
                        print();
                    }
                }
            }
        });
        btnQCVerify.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                QCApp.runQCApp();
            }
        });

        // This makes sure any external RNG stream is properly closed upon exit.
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if(extRNG != null) {
                    extRNG.close();
                }
                e.getWindow().dispose();
            }
        });
    }

    public static void main(String[] args) {
        String filepath = System.getProperty("user.dir");
        boolean skip = false;
        if(args.length == 0) {
            System.out.println("Program Argument required:");
            System.out.println("    -v  Verify checksums of external jar files");
            System.out.println("    -s  Skip checksum verification");
            System.out.println("    -i  Use this flag instead of \"-v\" when running/debugging within IntelliJ");
            filepath = null;
        }else if(args.length == 1) {
            if(args[0].equals("-v")) {
            }else if(args[0].equals("-s")) {
                skip = true;
            }else if(args[0].equals("-i")) { // Filepath needs altered when running within IntelliJ
                filepath += System.getProperty("file.separator") + "out" + System.getProperty("file.separator") + "artifacts";
            }else {
                System.out.println("Error! Invalid program argument!");
                filepath = null;
            }
        }else {
            System.out.println("Error! Too many program arguments!");
            filepath = null;
        }

        if(skip || JarChecksums.run(filepath)) {
            javax.swing.SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    Main main = new Main("Keystickers by Satoshiware");

                    main.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                    main.pack();

                    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                    main.setLocationRelativeTo(null);
                    main.setLocation(screenSize.width/2 - 320, screenSize.height/2 - 200); // Open up the frame in the middle of the screen

                    main.setVisible(true);
                    main.setResizable(false);
                }
            });
        }
    }

    private void print() {
        Paper p = new Paper();
        p.setSize(612, 792); // Paper Size: Letter (8.5" x 11"; 1" = 72 points)
        p.setImageableArea(0,0, 612, 792); // No margins

        PageFormat pf = new PageFormat();
        pf.setPaper(p);
        pf.setOrientation(PageFormat.LANDSCAPE);

        Book book = new Book();
        writeQCFile qcFile = new writeQCFile();
        for(int j = 1; j <= Integer.parseInt(txtTotalPages.getText()); j++) {
            KSSheet sheet = new KSSheet();

            sheet.stickerOutlines = checkOutline.isSelected();
            sheet.grayScaleTest = checkGrayScaleTest.isSelected();
            sheet.rotationalShift = Double.parseDouble(txtRotation.getText());
            sheet.verticalShift = Double.parseDouble(txtVerticalOffset.getText());
            sheet.horizontalShift = Double.parseDouble(txtHorizontalOffset.getText());
            if(!checkGrayScaleTest.isSelected()) {
                sheet.privateQRBackground = 100 - Float.parseFloat(txtBGGrayscale.getText());
                sheet.privateQRGreyScale = 100 - Float.parseFloat(txtFGGrayscale.getText());
            }

            for(int i = 1; i <= 12; i++) {
                byte[] bytes = new byte[32];
                generator.getBytes(bytes);
                KSKey key = new KSKey(bytes, checkTestnet.isSelected());

                sheet.setPrivateKey(key.getWIF(), i);
                if(((ComboItem)comboKeyType.getSelectedItem()).getValue().equals("Segwit")) {
                    sheet.setPublicKey(key.getP2WPKH(0), i);
                    sheet.setQCNumber(key.getQCWIF() + " - " + key.getQCP2WPKH(0), i);
                    qcFile.write(j, i, key.getQCWIF() + " - " + key.getQCP2WPKH(0));
                }else if(((ComboItem)comboKeyType.getSelectedItem()).getValue().equals("Legacy")) {
                    sheet.setPublicKey(key.getP2PKH(), i);
                    sheet.setQCNumber(key.getQCWIF() + " - " + key.getQCP2PKH(), i);
                    qcFile.write(j, i, key.getQCWIF() + " - " + key.getQCP2PKH());
                }
            }

            book.append(sheet, pf);
        }

        qcFile.writeGenInfo();
        if(qcFile.writeSuccessful() || destQCFile == null) {
            Thread thread = new Thread("Printing") {
                public void run() {
                    try {
                        PrinterJob job = PrinterJob.getPrinterJob();
                        job.setPageable(book);
                        if (job.printDialog()) {
                            btnPrint.setEnabled(false);
                            btnPrint.setText("Printing");
                            job.print();
                        }
                    } catch (PrinterException ex) {}
                    btnPrint.setEnabled(true);
                    btnPrint.setText("Print");
                }
            };
            thread.start();
            qcFile.close();
        }else {
            JOptionPane.showMessageDialog(mainPanel, "Problem writing to QC file", "Write Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Routine run when mouse entropy collection has finished
    private class MouseEntropyClosingHandle implements MouseEntropy.MouseEntropyCloseEvent {
        public boolean isMouseEntropyCollected;

        public MouseEntropyClosingHandle() {
            isMouseEntropyCollected = false;
        }

        public  void onClose() {
            isMouseEntropyCollected = true;
            generator.addGenerator(new FortunaGenerator(entropy));
            lblRandomInfo2.setText("RNG #2: org.jitsi.bccontrib.prng.FortunaGenerator");
        }
    }

    private class writeQCFile {
        private FileOutputStream fos;
        private BufferedWriter bw;

        private boolean writeSuccessful;

        public writeQCFile() {
            writeSuccessful = true;
            if(destQCFile != null) {
                String s = "QC" + (new SimpleDateFormat("MMddyyyyHHmmss")).format(new Date()) + ".txt";
                try {
                    fos = new FileOutputStream(new File(destQCFile, s));
                    bw = new BufferedWriter(new OutputStreamWriter(fos));
                } catch (IOException e) {
                    fos = null;
                    bw = null;
                    writeSuccessful = false;
                }
            }else {
                writeSuccessful = false;
            }
        }

        public boolean write(int page, int sticker, String qc) {
            if(bw != null) {
                try {
                    bw.write("Page# " + Integer.toString(page) + "  " + "\tSticker# " + Integer.toString(sticker) + "\tQC# " + qc);
                    bw.newLine();
                } catch (IOException e) {
                    fos = null;
                    bw = null;
                    writeSuccessful = false;
                    return false;
                }
                return true;
            }
            return false;
        }

        public boolean writeGenInfo() {
            if(bw != null) {
                try {
                    bw.newLine();

                    StringTokenizer st = new StringTokenizer(generator.getInfo(), "\n");
                    while (st.hasMoreTokens()) {
                        bw.write(st.nextToken());
                        bw.newLine();
                    }
                } catch (IOException e) {
                    fos = null;
                    bw = null;
                    writeSuccessful = false;
                    return false;
                }
                return true;
            }
            return false;
        }

        public boolean close() {
            if(bw != null) {
                try {
                    bw.close();
                } catch (IOException e) {
                    fos = null;
                    bw = null;
                    writeSuccessful = false;
                    return false;
                }
                return true;
            }
            return false;
        }

        public boolean writeSuccessful() {
            return writeSuccessful;
        }
    }

    // Class used to support the Combo Box (comboKeyType) configuration
    private class ComboItem {
        private String key;
        private String value;

        public ComboItem(String key, String value)
        {
            this.key = key;
            this.value = value;
        }

        @Override
        public String toString()
        {
            return key;
        }

        public String getKey()
        {
            return key;
        }

        public String getValue()
        {
            return value;
        }
    }
}