/*
 *      Main class (GUI) for generating and verifying Keystickers & Satoshi Coins
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
import java.util.Objects;

import javax.swing.*;

public class Main extends JFrame {
    private JPanel mainPanel;

    private JButton btnRndQCVisual;
    private JButton btnPrint;

    private JRadioButton rBtnKeystickers;
    private JRadioButton rBtnSatoshiCoins;

    private JLabel lblTotalPages;
    private JLabel lblAmount;
    private JLabel lblKeyDarkness;
    private JLabel lblRandomInfo;
    private JLabel lblRandomInfo2;
    private JLabel lblRandomInfo3;

    private JCheckBox checkTestnet;
    private JCheckBox checkOutline;

    private JTextField txtAmount;
    private JTextField txtKeyDarkness;
    private JTextField txtTotalPages;

    private final KSGenerator generator; // Container for all random sources. Each random source is mixed (XOR'd) together.
    private final byte[] entropy; // Entropy generated from mouse movements.
    private final MouseEntropyClosingHandle entropyHandle; // Called after Mouse Entropy has successfully closed.
    private final RPiHWRNG HWRNG; // Raspberry Pi's HWRNG.

    public Main(String title) {
        super(title);
        this.setContentPane(mainPanel);

        generator = new KSGenerator();
        generator.addGenerator(new SecureRandom());
        lblRandomInfo.setText("RNG #1: java.security.SecureRandom");

        entropy = new byte[MouseEntropy.SEEDSIZE];
        generator.getBytes(entropy); // Initial seeding of mouse entropy pool
        entropyHandle = new MouseEntropyClosingHandle();
        MouseEntropy.run(entropy, entropyHandle); // Get mouse entropy
        lblRandomInfo2.setText("RNG #2: Collecting Mouse Entropy");

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
            }

            if (!exceptionThrown) { // True if all inputs are valid.
                if (!entropyHandle.isMouseEntropyCollected) {
                    int dialogResult = JOptionPane.showConfirmDialog(null, "Mouse Entropy is not fully collected. Continue?", "Warning", JOptionPane.YES_NO_OPTION);
                    if (dialogResult == JOptionPane.YES_OPTION)
                        print();
                } else {
                    print();
                }
            }
        });

        // This is used to make sure the Raspberry Pi's HWRNG stream is properly closed upon exit.
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (!lblRandomInfo3.getText().equals(""))
                    HWRNG.close();

                e.getWindow().dispose();
            }
        });
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Program Argument Required:");
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
                            main.setIconImage((new ImageIcon(Objects.requireNonNull(main.getClass().getClassLoader().getResource("satoshiware_icon.png")))).getImage());
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
            }
        } else {
            System.out.println("Error! Too many program arguments!");
        }
    }

    private void print() {
        Paper p = new Paper();
        p.setSize(612, 792); // Paper Size: Letter (8.5" x 11"; 1" = 72 points)
        p.setImageableArea(0, 0, 612, 792); // No margins

        PageFormat pf = new PageFormat(); // This is the page format used for each printed sheet.
        pf.setPaper(p);
        pf.setOrientation(PageFormat.LANDSCAPE);

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
                byte[] bytes = new byte[32];
                generator.getBytes(bytes);
                KSKey key = new KSKey(bytes, checkTestnet.isSelected());

                sheet.setPrivateKey(key.getWIF(), i);
                sheet.setPublicKey(key.getP2WPKH(0), i);
            }

            book.append(sheet, pf);
        }

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

    //region The following code has been generated by IntelliJ

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

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
}