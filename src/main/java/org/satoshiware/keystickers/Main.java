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

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;

import org.satoshiware.keystickers.random.*;
import org.satoshiware.satoshicoins.SC;

import java.awt.*;
import java.awt.event.*;
import java.awt.print.*;
import java.io.File;

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
<<<<<<< Updated upstream
    private JButton btnQCVerify;

    private JLabel lblTotalPages;
    private JLabel lblHorizontalOffset;
    private JLabel lblVerticalOffset;
    private JLabel lblRotation;
    private JLabel lblFGGrayscale;
    private JLabel lblBGGrayscale;
    private JLabel lblKeyType;
    private JLabel lblQCDestFile;
=======

    private JRadioButton rBtnKeystickers;
    private JRadioButton rBtnSatoshiCoins;

    private JLabel lblTotalPages;
    private JLabel lblAmount;
    private JLabel lblKeyDarkness;
>>>>>>> Stashed changes
    private JLabel lblRandomInfo;
    private JLabel lblRandomInfo2;
    private JLabel lblRandomInfo3;

    private JCheckBox checkReserved;
    private JCheckBox checkTestnet;
    private JCheckBox checkOutline;
    private JCheckBox checkGrayScaleTest;

<<<<<<< Updated upstream
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
=======
    private JTextField txtAmount;
    private JTextField txtKeyDarkness;
    private JTextField txtTotalPages;

    private final KSGenerator generator; // Container for all random sources. Each random source is mixed (XOR'd) together.
    private final byte[] entropy; // Entropy generated from mouse movements.
    private final MouseEntropyClosingHandle entropyHandle; // Called after Mouse Entropy has successfully closed.
    private final RPiHWRNG HWRNG; // Raspberry Pi's HWRNG.
>>>>>>> Stashed changes

    public Main(String title) {
        super(title);
        this.setContentPane(mainPanel);

<<<<<<< Updated upstream
        // Configure Combo Box (comboKeyType) used to select Bitcoin address format
        comboKeyType.addItem(new ComboItem("Native Segwit (Bech32)", "Segwit"));
        comboKeyType.addItem(new ComboItem("Legacy Address", "Legacy"));

=======
>>>>>>> Stashed changes
        generator = new KSGenerator();
        generator.addGenerator(new SecureRandom());
        lblRandomInfo.setText("RNG #1: java.security.SecureRandom");

        entropy = new byte[MouseEntropy.SEEDSIZE];
        generator.getBytes(entropy); // Initial seeding of mouse entropy pool
        entropyHandle = new MouseEntropyClosingHandle();
        MouseEntropy.run(entropy, entropyHandle); // Get mouse entropy
        lblRandomInfo2.setText("RNG #2: Collecting Mouse Entropy");

<<<<<<< Updated upstream
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
=======
        HWRNG = new RPiHWRNG(); // Open a stream to the Raspberry Pi's HWRNG if available.
        if (HWRNG.open() && HWRNG.fillStack()) {
            lblRandomInfo3.setText("RNG #3: Raspberry Pi's HWRNG");
            generator.addGenerator(HWRNG);
        } else {
            lblRandomInfo3.setText("");
        }

        rBtnKeystickers.addActionListener(e -> { // Enable controls for Keystickers. Disable controls for Satoshi Coins.
            txtAmount.setEnabled(false);
            txtKeyDarkness.setEnabled(true);
            checkTestnet.setEnabled(true);
            checkOutline.setEnabled(true);
            lblAmount.setEnabled(false);
            lblKeyDarkness.setEnabled(true);
            lblTotalPages.setText("Number of Pages (12 Keys / Sheet)");
        });
        rBtnSatoshiCoins.addActionListener(e -> { // Disable controls for Keystickers. Enable controls for Satoshi Coins
            txtAmount.setEnabled(true);
            txtKeyDarkness.setEnabled(false);
            checkOutline.setEnabled(false);
            lblAmount.setEnabled(true);
            lblKeyDarkness.setEnabled(false);
            lblTotalPages.setText("Number of Pages (24 Keys / Sheet)");
        });

        btnRndQCVisual.addActionListener(e -> generator.runQC());

        btnPrint.addActionListener(e -> {
            boolean exceptionThrown = false;
            try { // Verify inputs
                Integer.parseInt(txtTotalPages.getText());
                if (rBtnKeystickers.isSelected()) {
                    Float.parseFloat(txtKeyDarkness.getText());
                } else if (rBtnSatoshiCoins.isSelected()) {
                    long amount = Long.parseLong(txtAmount.getText().trim().replace(",", ""));
                    if (!(amount == 1000000 || amount == 500000 || amount == 250000 || amount == 100000 || amount == 50000 || amount == 25000 || amount == 10000)) {
                        exceptionThrown = true;
                        JOptionPane.showMessageDialog(mainPanel, "Invalid $atoshi amount.\n\n Valid Amounts:\n    1,000,000\n    500,000\n    250,000\n    100,000\n   50,000\n    25,000\n    10,000", "Invalid Amount", JOptionPane.ERROR_MESSAGE);
                    }
                }
            } catch (Exception ex) { // Execute this exception if all inputs are not valid.
                exceptionThrown = true;
                JOptionPane.showMessageDialog(mainPanel, "One or more inputs are invalid.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
>>>>>>> Stashed changes
            }
        });
        btnRndQCVisual.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                generator.runQC();
            }
        });

<<<<<<< Updated upstream
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
=======
            if (!exceptionThrown) { // True if all inputs are valid.
                if (!entropyHandle.isMouseEntropyCollected) {
                    int dialogResult = JOptionPane.showConfirmDialog(null, "Mouse Entropy is not fully collected. Continue?", "Warning", JOptionPane.YES_NO_OPTION);
                    if (dialogResult == JOptionPane.YES_OPTION)
>>>>>>> Stashed changes
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

<<<<<<< Updated upstream
        // This makes sure any external RNG stream is properly closed upon exit.
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if(extRNG != null) {
                    extRNG.close();
                }
=======
        // This is used to make sure the Raspberry Pi's HWRNG stream is properly closed upon exit.
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (!lblRandomInfo3.getText().equals(""))
                    HWRNG.close();

>>>>>>> Stashed changes
                e.getWindow().dispose();
            }
        });
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Program Argument Required:");
<<<<<<< Updated upstream
            System.out.println("    -v  Verify checksums of external jar files");
            System.out.println("    -s  Skip checksum verification");
            filepath = null;
        }else if(args.length == 1) {
            if(args[0].equals("-v")) {
            }else if(args[0].equals("-s")) {
                skip = true;
            }else {
                System.out.println("Error! Invalid program argument!");
                filepath = null;
=======
            System.out.println("    -v          Verify checksums of external jar files");
            System.out.println("    -k          Launch Keystickers");
            System.out.println("    -s          Launch Satoshi Coins");
            System.out.println("    -t          Launch Satoshi Coins (Testnet)");
        } else if (args.length == 1) {
            switch (args[0]) {
                case "-s":
                    SC.run(false);
                    break;
                case "-t":
                    SC.run(true);
                    break;
                case "-k":
                    SwingUtilities.invokeLater(() -> {
                        Main main = new Main("Keystickers");

                        try { // Load icon in the upper left of this window
                            String iconPath = new File(SC.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath();
                            main.setIconImage(Toolkit.getDefaultToolkit().getImage(iconPath.substring(0, iconPath.lastIndexOf(File.separator) + 1) + "classes" + File.separator + "satoshiware_icon.png"));
                        } catch (Exception ignored) {
                        }

                        main.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                        main.pack();

                        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                        main.setLocationRelativeTo(null);
                        main.setLocation(screenSize.width / 2 - 320, screenSize.height / 2 - 200); // Open up the frame in the middle of the screen

                        main.setVisible(true);
                        main.setResizable(false);
                    });
                    break;
                case "-v":
                    JarChecksums.run(System.getProperty("user.dir") + System.getProperty("file.separator") + "lib");
                    break;
                default:
                    System.out.println("Error! Invalid program argument!");
                    break;
>>>>>>> Stashed changes
            }
        } else {
            System.out.println("Error! Too many program arguments!");
<<<<<<< Updated upstream
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
=======
>>>>>>> Stashed changes
        }
    }

    private void print() {
        Paper p = new Paper();
        p.setSize(612, 792); // Paper Size: Letter (8.5" x 11"; 1" = 72 points)
        p.setImageableArea(0, 0, 612, 792); // No margins

        PageFormat pf = new PageFormat(); // This is the page format used for each printed sheet.
        pf.setPaper(p);
        pf.setOrientation(PageFormat.LANDSCAPE);

<<<<<<< Updated upstream
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
=======
        Book book = new Book(); // Group of sheets (pages) that are prepared and then sent to the printer of choice.
        for (int j = 1; j <= Integer.parseInt(txtTotalPages.getText()); j++) {
            PrintableKeys sheet;
            if (rBtnKeystickers.isSelected()) { // Keystickers
                sheet = new KSSheet();

                ((KSSheet) sheet).stickerOutlines = checkOutline.isSelected();
                ((KSSheet) sheet).privateKeyGreyScale = 100 - Float.parseFloat(txtKeyDarkness.getText());
            } else { // Satoshi Coins
                sheet = new SCSheet();

                ((SCSheet) sheet).satoshiAmount = Integer.parseInt(txtAmount.getText().replace(",", ""));
            }

            for (int i = 1; i <= sheet.getKeyTotal(); i++) {
>>>>>>> Stashed changes
                byte[] bytes = new byte[32];
                generator.getBytes(bytes);
                KSKey key = new KSKey(bytes, checkTestnet.isSelected());

                sheet.setPrivateKey(key.getWIF(), i);
<<<<<<< Updated upstream
                if(((ComboItem)comboKeyType.getSelectedItem()).getValue().equals("Segwit")) {
                    sheet.setPublicKey(key.getP2WPKH(0), i);
                    sheet.setQCNumber(key.getQCWIF() + " - " + key.getQCP2WPKH(0), i);
                    qcFile.write(j, i, key.getQCWIF() + " - " + key.getQCP2WPKH(0));
                }else if(((ComboItem)comboKeyType.getSelectedItem()).getValue().equals("Legacy")) {
                    sheet.setPublicKey(key.getP2PKH(), i);
                    sheet.setQCNumber(key.getQCWIF() + " - " + key.getQCP2PKH(), i);
                    qcFile.write(j, i, key.getQCWIF() + " - " + key.getQCP2PKH());
                }
=======
                sheet.setPublicKey(key.getP2WPKH(0), i);
>>>>>>> Stashed changes
            }

            book.append(sheet, pf);
        }

<<<<<<< Updated upstream
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
=======
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
                } catch (PrinterException ignored) {
                }
                btnPrint.setEnabled(true);
                btnPrint.setText("Print");
            }
        };
        thread.start();
>>>>>>> Stashed changes
    }

    // Routine run when mouse entropy collection has finished
    private class MouseEntropyClosingHandle implements MouseEntropy.MouseEntropyCloseEvent {
        public boolean isMouseEntropyCollected;

        public MouseEntropyClosingHandle() {
            isMouseEntropyCollected = false;
        }

        public void onClose() {
            isMouseEntropyCollected = true;
            generator.addGenerator(new FortunaGenerator(entropy));
            lblRandomInfo2.setText("RNG #2: org.jitsi.bccontrib.prng.FortunaGenerator");
        }
    }

<<<<<<< Updated upstream
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
=======
    //region The following code has been generated by IntelliJ
>>>>>>> Stashed changes

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }
<<<<<<< Updated upstream
=======

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayoutManager(11, 2, new Insets(10, 10, 10, 10), 0, 0));
        mainPanel.setAlignmentX(0.0f);
        mainPanel.setAlignmentY(0.0f);
        mainPanel.setMaximumSize(new Dimension(510, 390));
        mainPanel.setMinimumSize(new Dimension(510, 390));
        mainPanel.setName("");
        mainPanel.setPreferredSize(new Dimension(510, 390));
        btnPrint = new JButton();
        btnPrint.setAlignmentX(0.0f);
        btnPrint.setAlignmentY(0.0f);
        btnPrint.setText("Print");
        btnPrint.setToolTipText("Generates keys and opens the Print Job dialog box");
        mainPanel.add(btnPrint, new GridConstraints(10, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(490, 30), new Dimension(490, 30), new Dimension(490, 30), 0, false));
        lblTotalPages = new JLabel();
        lblTotalPages.setText("Number of Pages (12 Keys / Sheet)");
        lblTotalPages.setToolTipText("Number of pages that will be generated and printed");
        lblTotalPages.setVerifyInputWhenFocusTarget(true);
        mainPanel.add(lblTotalPages, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(280, 30), new Dimension(280, 30), new Dimension(280, 30), 0, false));
        txtTotalPages = new JTextField();
        txtTotalPages.setText("1");
        txtTotalPages.setToolTipText("Number of pages that will be generated and printed");
        mainPanel.add(txtTotalPages, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(280, 30), new Dimension(280, 30), new Dimension(280, 30), 0, false));
        lblRandomInfo3 = new JLabel();
        lblRandomInfo3.setRequestFocusEnabled(false);
        lblRandomInfo3.setText("lblRandomInfo3");
        lblRandomInfo3.setToolTipText("Raspberry Pi's HWRNG (True Random Number Generator)");
        mainPanel.add(lblRandomInfo3, new GridConstraints(8, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(420, 30), new Dimension(420, 30), new Dimension(420, 30), 0, false));
        lblRandomInfo2 = new JLabel();
        lblRandomInfo2.setText("lblRandomInfo2");
        lblRandomInfo2.setToolTipText("Cryptographically Secure Pseudo-Random Number Generator (CSPRNG) native to this Keysticker application. Seeded with entropy from the mouse. Each Bitcoin key is created by XOR’ing numbers from each RNG source.");
        mainPanel.add(lblRandomInfo2, new GridConstraints(7, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(420, 30), new Dimension(420, 30), new Dimension(420, 30), 0, false));
        lblRandomInfo = new JLabel();
        lblRandomInfo.setText("lblRandomInfo");
        lblRandomInfo.setToolTipText("Cryptographically Secure Pseudo-Random Number Generator (CSPRNG) native to the Java Platform. Seeded with entropy from the OS kernel. Each Bitcoin key is created by XOR’ing numbers from each RNG source.");
        mainPanel.add(lblRandomInfo, new GridConstraints(6, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(420, 30), new Dimension(420, 30), new Dimension(420, 30), 0, false));
        txtKeyDarkness = new JTextField();
        txtKeyDarkness.setText("100");
        txtKeyDarkness.setToolTipText("Keystickers: Configures the grayscale for the Private Key (Default = 100% / Black)");
        mainPanel.add(txtKeyDarkness, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(280, 30), new Dimension(280, 30), new Dimension(280, 30), 0, false));
        btnRndQCVisual = new JButton();
        btnRndQCVisual.setAlignmentX(0.0f);
        btnRndQCVisual.setAlignmentY(0.0f);
        btnRndQCVisual.setText("RNG Visual Check");
        btnRndQCVisual.setToolTipText("Provides a visual sanity check for each RNG source. Look for plenty of dots without any patterns");
        mainPanel.add(btnRndQCVisual, new GridConstraints(9, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(490, 30), new Dimension(490, 30), new Dimension(490, 30), 0, false));
        lblKeyDarkness = new JLabel();
        lblKeyDarkness.setText("Private Key Darkness (%)");
        lblKeyDarkness.setToolTipText("Keystickers: Configures the grayscale for the Private Key (Default = 100% / Black)");
        mainPanel.add(lblKeyDarkness, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(210, 30), new Dimension(210, 30), new Dimension(210, 30), 0, false));
        rBtnSatoshiCoins = new JRadioButton();
        rBtnSatoshiCoins.setText("Satoshi Coins");
        rBtnSatoshiCoins.setToolTipText("Select to generate Satoshi Coins");
        mainPanel.add(rBtnSatoshiCoins, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(210, 30), new Dimension(210, 30), new Dimension(210, 30), 2, false));
        rBtnKeystickers = new JRadioButton();
        rBtnKeystickers.setSelected(true);
        rBtnKeystickers.setText("Keystickers");
        rBtnKeystickers.setToolTipText("Select to generate Keystickers");
        mainPanel.add(rBtnKeystickers, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(210, 30), new Dimension(210, 30), new Dimension(210, 30), 2, false));
        lblAmount = new JLabel();
        lblAmount.setEnabled(false);
        lblAmount.setOpaque(false);
        lblAmount.setText("Amount ($atoshis)");
        lblAmount.setToolTipText("Satoshi Coins: Enter the amount of $atoshis for this batch");
        mainPanel.add(lblAmount, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(280, 30), new Dimension(280, 30), new Dimension(280, 30), 0, false));
        txtAmount = new JTextField();
        txtAmount.setEnabled(false);
        txtAmount.setText("1,000,000");
        txtAmount.setToolTipText("Satoshi Coins: Enter the amount of $atoshis for this batch");
        mainPanel.add(txtAmount, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(280, 30), new Dimension(280, 30), new Dimension(280, 30), 0, false));
        checkOutline = new JCheckBox();
        checkOutline.setText("Keysticker Outline");
        checkOutline.setToolTipText("Check box for Keysticker outlines to show on each printed sheet (Not for production)");
        mainPanel.add(checkOutline, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(210, 30), new Dimension(210, 30), new Dimension(210, 30), 2, false));
        checkTestnet = new JCheckBox();
        checkTestnet.setSelected(false);
        checkTestnet.setText("Testnet");
        checkTestnet.setToolTipText("Check box for Testnet Bitcoin addresses");
        mainPanel.add(checkTestnet, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(210, 30), new Dimension(210, 30), new Dimension(210, 30), 2, false));
        ButtonGroup buttonGroup;
        buttonGroup = new ButtonGroup();
        buttonGroup.add(rBtnKeystickers);
        buttonGroup.add(rBtnSatoshiCoins);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }

    //endregion
>>>>>>> Stashed changes
}