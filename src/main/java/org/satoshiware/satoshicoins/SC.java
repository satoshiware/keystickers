/*
 *      Main class (GUI) for "Satoshi Coins".
 *      Used to load "Satoshi Coins" with $atoshis (bitcoins).
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

package org.satoshiware.satoshicoins;

import org.satoshiware.keystickers.Bech32;

import wf.bitcoin.javabitcoindrpcclient.BitcoinJSONRPCClient;
import wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient;
import wf.bitcoin.javabitcoindrpcclient.SignatureHashType;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;

import javax.swing.*;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;

public class SC extends JFrame {
    private JPanel mainPanel;
    private JTextArea txtAreaCoinInfo;
    private JScrollPane scrollPaneCoinInfo;

    private JCheckBox chkBxFirstTime;

    private JTextField txtName;
    private JTextField txtTXID;
    private JTextField txtPrivateKey;
    private JTextField txtAddressVout0;

    private JLabel lblName;
    private JLabel lblTXID;
    private JLabel lblPrivateKey;
    private JLabel lblVout0;

    private JComboBox<ComboItem> comboFee;

    private JButton btnInitiate;
    private JLabel lblCoinInfo;

    public final boolean testnet; // Set true if this tx is for testnet

    private static final String[] PREFERREDFONTS = {"Liberation Mono", "Courier New"}; // Fonts ordered by priority.

    private final BitcoinJSONRPCClient rpcClient; // Facilitates communication with Bitcoin Core
    private String payAddress; // This address will serve as the primary source of funds for loading all the coin outputs
    private BigDecimal feeRate; // The desired fee rate for this TX ($ATS/BYTE)
    private BigDecimal txid0Amount; // The amount of BTC associated with output 0 in the previous Satoshi Coins' tx
    private final List<BitcoindRpcClient.BasicTxOutput> coins; // List of all the Satoshi Coins outputs
    private String payload; // For the bank's first transaction, there is an OP_RETURN payload that includes its name

    private boolean exitFlag; // Control flag set during the transaction process; it will cause a pop-up "Transaction Incomplete" warning upon exiting

    public SC(String title, boolean testnet) {
        super(title);
        this.setContentPane(mainPanel);
        this.testnet = testnet;

        //region Configure Fonts
        String thisFont = "defaultFont"; // Font used for this program
        String[] fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        for (int j = 0; j < PREFERREDFONTS.length; j++) {
            for (int i = 0; i < fonts.length; i++) {
                if (fonts[i].equals(PREFERREDFONTS[j])) {
                    thisFont = PREFERREDFONTS[j];
                    j = PREFERREDFONTS.length;
                    i = fonts.length;
                }
            }
        }

        txtAreaCoinInfo.setFont(new Font(thisFont, Font.PLAIN, 12));
        txtName.setFont(new Font(thisFont, Font.PLAIN, 12));
        txtTXID.setFont(new Font(thisFont, Font.PLAIN, 12));
        txtPrivateKey.setFont(new Font(thisFont, Font.PLAIN, 12));
        txtAddressVout0.setFont(new Font(thisFont, Font.PLAIN, 12));
        comboFee.setFont(new Font(thisFont, Font.PLAIN, 12)); //endregion

        rpcClient = new BitcoinJSONRPCClient(testnet);
        try {
            payAddress = rpcClient.getNewAddress("Pay Address", "bech32");
        } catch (Exception ignored) {
            JOptionPane.showMessageDialog(mainPanel, "Could not establish communication with Bitcoin Core", "Communication Error", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }

        feeRate = BigDecimal.valueOf(0);
        txid0Amount = BigDecimal.valueOf(0);
        coins = new ArrayList<>();
        payload = "";

        exitFlag = false;

        //region Configure Combo Box (used to select the tx fee rate). This custom calculator will increase fee market diversity :-)
        BigDecimal[] fees = new BigDecimal[240]; // There is no bump fee option in this application; therefore, the maximum number of blocks for tx inclusion is decreased
        for (int i = 1; i < fees.length; i++)
            fees[i] = rpcClient.estimateSmartFee(i).feeRate();

        int dIndex = fees.length - 1; // Find the block count (for tx inclusion) where the fee rate begins to change
        while (dIndex != 1 && (fees[dIndex].equals(fees[dIndex - 1])))
            dIndex--;

        BigDecimal avg = new BigDecimal(0); // Calculate average fee rate (from most expensive to the first "cheapskate" value)
        for (int i = 1; i <= dIndex; i++)
            avg = avg.add(fees[i]);
        avg = avg.divide(new BigDecimal(dIndex), 8, RoundingMode.HALF_UP);

        int avgBlock = -1; // Find the number of blocks for tx inclusion for the average fee
        for (int i = 1; i < fees.length; i++) {
            if (avg.compareTo(fees[i]) > -1) {
                avgBlock = i;
                break;
            }
        }

        BigDecimal cheapskate = fees[fees.length - 1].multiply(new BigDecimal(100000));
        BigDecimal economy = avg.add(fees[fees.length - 1]).multiply(new BigDecimal(50000));
        BigDecimal average = avg.multiply(new BigDecimal(100000));
        BigDecimal fast = avg.add(fees[1]).multiply(new BigDecimal(50000));
        BigDecimal expedite = fees[1].multiply(new BigDecimal(100000));

        if (cheapskate.compareTo(BigDecimal.valueOf(1.025)) < 0)
            cheapskate = BigDecimal.valueOf(1.025); // Increase the fee rates (if necessary) to make sure minimum fee requirements are met
        if (economy.compareTo(BigDecimal.valueOf(1.050)) < 0) economy = BigDecimal.valueOf(1.050);
        if (average.compareTo(BigDecimal.valueOf(1.075)) < 0) average = BigDecimal.valueOf(1.075);
        if (fast.compareTo(BigDecimal.valueOf(1.100)) < 0) fast = BigDecimal.valueOf(1.100);
        if (expedite.compareTo(BigDecimal.valueOf(1.125)) < 0) expedite = BigDecimal.valueOf(1.125);

        DecimalFormat df = new DecimalFormat("0.000");
        comboFee.addItem(new ComboItem("Cheapskate:  " + df.format(cheapskate) + "  $ATS/BYTE  (" + dIndex + " Block" + (dIndex > 1 ? "s" : "") + ")", cheapskate));
        comboFee.addItem(new ComboItem("Economy:     " + df.format(economy) + "  $ATS/BYTE", economy));
        comboFee.addItem(new ComboItem("Average:     " + df.format(average) + "  $ATS/BYTE  (" + avgBlock + " Block" + (avgBlock > 1 ? "s" : "") + ")", average));
        comboFee.addItem(new ComboItem("Fast:        " + df.format(fast) + "  $ATS/BYTE", fast));
        comboFee.addItem(new ComboItem("Expedite:    " + df.format(expedite) + "  $ATS/BYTE  (1 Block)", expedite));
        comboFee.setSelectedIndex(2); //endregion

        chkBxFirstTime.addActionListener(e -> {
            lblName.setEnabled(chkBxFirstTime.isSelected());
            txtName.setEnabled(chkBxFirstTime.isSelected());
            lblTXID.setEnabled(!chkBxFirstTime.isSelected());
            txtTXID.setEnabled(!chkBxFirstTime.isSelected());
            lblPrivateKey.setEnabled(!chkBxFirstTime.isSelected());
            txtPrivateKey.setEnabled(!chkBxFirstTime.isSelected());
        }); // "First Time" check box action listener

        btnInitiate.addActionListener(e -> {
            coins.clear();
            feeRate = comboFee.getItemAt(comboFee.getSelectedIndex()).getValue();

            //region Verify Vout 0 address
            txtAddressVout0.setText(txtAddressVout0.getText().trim().toLowerCase());
            if ((testnet && !txtAddressVout0.getText().startsWith("tb1q")) || (!testnet && !txtAddressVout0.getText().startsWith("bc1q")) || !validChecksum(txtAddressVout0.getText())) {
                JOptionPane.showMessageDialog(mainPanel, "Vout 0 address is invalid. Only bech32" + (testnet ? " testnet" : "") + " address with correct checksum is accepted", "Invalid Address", JOptionPane.ERROR_MESSAGE);
                return;
            } //endregion

            //region Extract coins from the text area
            String[] coinsArray = txtAreaCoinInfo.getText().split("\n");
            boolean valid = false;
            for (String coinStr : coinsArray)
                valid |= addCoin(coinStr);
            if (!valid) {
                JOptionPane.showMessageDialog(mainPanel, "There are no valid outputs (Coin Address with Amount)\nNote: addresses must be P2WPKH (Bech32 Native Segwit) Version 0", "Invalid Outputs", JOptionPane.ERROR_MESSAGE);
                lblCoinInfo.setText("Coin Address and Amount");
                txtAreaCoinInfo.setText("");
                return;
            } else { // Update txtAreaCoinInfo with all valid coins (no duplicates) while adding the (Vout) numbers at the beginning of each line
                lblCoinInfo.setText("(Vout)  Coin Address and Amount");
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < coins.size(); i++)
                    sb.append("(").append(i + 1).append(")\t").append(coins.get(i).address()).append("\t").append((new DecimalFormat("0")).format(coins.get(i).amount.multiply(BigDecimal.valueOf(100000000)))).append("\n");
                txtAreaCoinInfo.setText(sb.toString());

                if (coins.size() > 500) { // Enforce transaction size limit to around 16K Bytes; Very large transactions may sit in the memory pool much longer than anticipated without an expedited fee rate.
                    JOptionPane.showMessageDialog(mainPanel, "Let's keep the number of coins to 500 or less", "Transaction Too Large", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            } //endregion

            //region Adds payload to the transaction if its the "First Time" or it verifies the TXID, its Key, and extracts the TXID's address & value
            txtName.setText(txtName.getText().replaceAll("\\s+", " ").trim()); // Forces uppercase; removes spaces from the beginning & end; removes double spacing
            txtTXID.setText(txtTXID.getText().toLowerCase().trim());
            txtPrivateKey.setText(txtPrivateKey.getText().trim());
            if (chkBxFirstTime.isSelected()) {
                if (txtName.getText().equals("")) {
                    JOptionPane.showMessageDialog(mainPanel, "Please enter your name and the name of your bank\nExample: \"BANK OF GREENFIELD, SC (S.NAKAMOTO)\"", "Input Missing", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                payload = "SATOSHI COINS: " + txtName.getText();
                if (payload.getBytes(StandardCharsets.UTF_8).length > 80) {
                    JOptionPane.showMessageDialog(mainPanel, "The OP_RETURN payload is too big\nIt cannot exceed 80 Bytes\nMake your name shorter", "Name Exceeds Maximum Length", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                txtTXID.setText("");
                txtPrivateKey.setText("");
            } else {
                txtName.setText("");
                if (txtTXID.getText().matches("^[0-9a-f]{64}$") && txtPrivateKey.getText().matches("[" + (testnet ? "c" : "LK") + "][1-9A-HJ-NP-Za-km-z]{51}$")) {
                    BitcoindRpcClient.TxOut txout;
                    try {
                        txout = rpcClient.getTxOut(txtTXID.getText(), 0, true);
                    } catch (Exception ignored) {
                        JOptionPane.showMessageDialog(mainPanel, "Could not establish communication with Bitcoin Core", "Communication Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    String[] keysValues;
                    try { // Make sure TXID, 0 is a valid and unspent output
                        keysValues = txout.mapStr("scriptPubKey").split(",");
                    } catch (NullPointerException ex) {
                        JOptionPane.showMessageDialog(mainPanel, "TXID, 0 is not a valid or spendable output", "Invalid Input", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    String address = keysValues[4].trim().substring(11, keysValues[4].length() - 3);
                    if (!address.matches("^(" + (testnet ? "tb1q" : "bc1q") + ")[02-9a-hj-np-z]{38}$") || !validChecksum(address)) {
                        JOptionPane.showMessageDialog(mainPanel, "TXID, 0 is not a P2WPKH (Bech32 Native Segwit version 0) output", "Invalid Input", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    rpcClient.importPrivKey(txtPrivateKey.getText(), "", false);
                    String key = rpcClient.dumpPrivKey(address);
                    if (!txtPrivateKey.getText().equals(key)) {
                        JOptionPane.showMessageDialog(mainPanel, "The Key does not unlock the first output of the TXID", "Invalid Input", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    txid0Amount = txout.value();
                } else {
                    JOptionPane.showMessageDialog(mainPanel, "TXID, 0 or its key is invalid", "Invalid Input", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            } //endregion

            txtAreaCoinInfo.append(getReport()); // Add additional information regarding this transaction below the list of coins

            //region Finalize Transactions: disabled controls, initiate timer task, and show "Loading Coins" window
            chkBxFirstTime.setEnabled(false);
            txtName.setEnabled(false);
            txtTXID.setEnabled(false);
            txtPrivateKey.setEnabled(false);
            txtAddressVout0.setEnabled(false);
            comboFee.setEnabled(false);
            btnInitiate.setEnabled(false);

            exitFlag = true;

            TXInfo txInfo = new TXInfo(); // Setup a timer task to periodically check for transactions and to give progress updates
            new Timer().scheduleAtFixedRate(new TXControl(txInfo), 0, TXControl.DELAY);

            SwingUtilities.invokeLater(() -> { // Show window with "Pay Address" QR code, instructions, and the up-to-date progress
                JFrame frame = new JFrame("Loading Coins");

                try { // Load icon in the upper left of this window
                    frame.setIconImage((new ImageIcon(Objects.requireNonNull(this.getClass().getClassLoader().getResource("satoshiware_icon.png")))).getImage());
                } catch (Exception ignored) {
                }

                frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
                frame.setContentPane(txInfo);
                frame.pack();
                frame.setLocation(this.getLocation().x + this.getWidth(), this.getLocation().y);
                frame.setResizable(false);
                frame.setVisible(true);
            }); //endregion
        }); // "Initiate Transaction" button action listener

        //region Gives a warning before exiting during the transaction process
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent we) {
                if (exitFlag) {
                    String[] ObjButtons = {"Yes", "No"};
                    int PromptResult = JOptionPane.showOptionDialog(null, "Are you sure you want to exit?", "Transaction Incomplete", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, ObjButtons, ObjButtons[1]);
                    if (PromptResult == JOptionPane.YES_OPTION)
                        System.exit(0);
                } else {
                    System.exit(0);
                }
            }
        }); //endregion
    }

    // Runs this Satoshi Coins' App
    public static void run(boolean testnet) {
        SwingUtilities.invokeLater(() -> {
            SC sc = new SC("Satoshi Coins" + (testnet ? " (TESTNET)" : ""), testnet);

            try { // Load icon in the upper left of this window
                sc.setIconImage((new ImageIcon(Objects.requireNonNull(sc.getClass().getClassLoader().getResource("satoshiware_icon.png")))).getImage());
            } catch (Exception ignored) {
            }

            sc.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            sc.pack();
            sc.setResizable(false);

            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize(); // Set this frame location in the middle of the screen
            sc.setLocation(screenSize.width / 2 - 320, 50);

            sc.setVisible(true);
        });
    }

    // Adds a coin to the "coins" array list while checking for errors and avoiding duplicates
    private boolean addCoin(String coin) {
        if (coin.indexOf(testnet ? 't' : 'b') == -1)
            return false; // Avoids potential "String Index Out Of Bounds" exception on the next line
        coin = coin.substring(coin.indexOf(testnet ? 't' : 'b')).trim(); // Removes the Vout # from the beginning of the string if it exists

        if (coin.matches("^(" + (testnet ? "tb1q" : "bc1q") + ")[02-9a-hj-np-z]{38}\\s+[0-9]{5,7}$")) {
            final String address = coin.substring(0, 42);
            final BigDecimal amount = new BigDecimal(coin.substring(43).trim());
            if (validChecksum(address) && validAmount(amount)) {
                for (BitcoindRpcClient.BasicTxOutput c : coins) {
                    if (c.address.equals(address))
                        return false;
                }
                coins.add(new BitcoindRpcClient.BasicTxOutput(address, amount.divide(BigDecimal.valueOf(100000000), 8, RoundingMode.UNNECESSARY)));
                return true;
            }
        }
        return false;
    }

    // Creates an informational report containing BTC amounts (Coins, Vout0, Fee, TXID0, and the Total), TX sizes, and the OP_RETURN payload
    private String getReport() {
        //region Convert OP_RETURN payload into a hex string formatted for easy viewing
        StringBuilder hexString = new StringBuilder();
        if (chkBxFirstTime.isSelected()) {
            byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);
            final char[] HEX_ARRAY = "0123456789abcdef".toCharArray();
            char[] hexChars = new char[bytes.length * 2];
            for (int i = 0; i < bytes.length; i++) {
                int v = bytes[i] & 0xFF;
                hexChars[i * 2] = HEX_ARRAY[v >>> 4];
                hexChars[i * 2 + 1] = HEX_ARRAY[v & 0x0F];
            }
            String hex = new String(hexChars);

            hexString = new StringBuilder();
            final int maxLength = 75;
            final int minLength = 20;
            if (hex.length() <= maxLength) {
                hexString.append("    ").append(hex).append("\n");
            } else {
                int tLength = maxLength;
                while (hex.length() % tLength < minLength)
                    tLength--;
                hexString.append("    ").append(hex, 0, tLength).append("\n");
                for (int i = tLength * 2; i < hex.length(); i += tLength)
                    hexString.append("    ").append(hex, i - tLength, i).append("\n");
                hexString.append("    ").append(hex, hex.length() - (hex.length() % tLength), hex.length()).append("\n");
            }
        } //endregion

        DecimalFormat df = new DecimalFormat("#0.000");
        return "\n************************************************************************************\n" +
                "Coins:            " + getAmount("Coin") + " BTC\n" +
                "Vout 0:           " + getAmount("Vout0") + " BTC\n" +
                "Fee:              " + getAmount("Fee") + " BTC\n\n" +

                "TXID 0:          " + ((txid0Amount.equals((BigDecimal.valueOf(0)))) ? (" NOT APPLICABLE") : ("(" + txid0Amount + " BTC)")) + "\n" +
                "************************************************************************************\n" +
                "Total:            " + getAmount("Total") + " BTC\n\n" +

                "Pay Address:      " + payAddress + "\n\n" +

                "TX Size:          " + df.format(getTXSize("Total") / 1000.0) + " kB (" + df.format(getTXSize("Legacy") / 1000.0) + " kB Legacy, " + df.format(getTXSize("Witness") / 1000.0) + " kB Witness)\n" +
                "Virtual:          " + df.format(getTXSize("Virtual") / 1000.0) + " kB\n\n" +

                (chkBxFirstTime.isSelected() ? ("Name:             " + txtName.getText() + "\n") : "") +
                (!chkBxFirstTime.isSelected() ? ("TXID (Previous):  " + txtTXID.getText() + "\n") : "") +
                (!chkBxFirstTime.isSelected() ? ("Key:              " + txtPrivateKey.getText() + "\n") : "") +
                "Vout 0:           " + txtAddressVout0.getText() + "\n" +
                "TX Fee:           \"" + comboFee.getItemAt(comboFee.getSelectedIndex()).toString() + "\"\n\n" +
                "************************************************************************************\n" +
                "OP_RETURN " + (chkBxFirstTime.isSelected() ? "(UTF-8):\n" : "        NOT APPLICABLE\n") +
                (chkBxFirstTime.isSelected() ? ("    \"" + payload + "\"\n") : "") +
                (chkBxFirstTime.isSelected() ? ("    " + payload.getBytes(StandardCharsets.UTF_8).length + " Bytes\n") : "") +
                hexString;
    }

    // Gets BTC amount for "Vout0", "Coin", "Fee", and "Total"
    private BigDecimal getAmount(String type) {
        switch (type) {
            case "Vout0": // The amount of $ATS on output 0
                return BigDecimal.valueOf(10000).divide(BigDecimal.valueOf(100000000), 8, RoundingMode.UNNECESSARY);
            case "Coin": // The total bitcoin amount to be loaded into coins
                BigDecimal amount = BigDecimal.valueOf(0);
                for (BitcoindRpcClient.BasicTxOutput coin : coins)
                    amount = amount.add(coin.amount);
                return amount;
            case "Fee": // The fee for this transaction
                return feeRate.multiply(BigDecimal.valueOf(getTXSize("Virtual"))).divide(BigDecimal.valueOf(100000000), 8, RoundingMode.UP);
            case "Total": // The necessary bitcoin amount to make the transaction possible
                return getAmount("Vout0").add(getAmount("Coin")).add(getAmount("Fee")).subtract(txid0Amount);
            default:
                return BigDecimal.valueOf(0);
        }
    }

    // Calculates the total number of bytes (including signatures) in this transaction. Parameter Options: "Legacy", "Witness", "Total", "Virtual", and "Weighted"
    private int getTXSize(String type) {
        switch (type) {
            case "Legacy":
                if (!chkBxFirstTime.isSelected())
                    return ((coins.size() + 1) * 31) + 92;
                else
                    return ((coins.size() + 1) * 31) + payload.getBytes(StandardCharsets.UTF_8).length + 62;
            case "Witness":
                if (!chkBxFirstTime.isSelected())
                    return 109 + 107;
                else
                    return 109;
            case "Total":
                return getTXSize("Legacy") + getTXSize("Witness");
            case "Virtual":
                return getTXSize("Legacy") + (int) Math.ceil((double) getTXSize("Witness") / 4.0);
            case "Weighted":
                return (getTXSize("Legacy") * 4) + getTXSize("Witness");
            default:
                return -1;
        }
    }

    // Creates, signs, and sends the transaction. Parameters: TXID & Vout from the payAddress (primary source of funds). Returns the TXID of this transaction
    private String txFinale(String payTXID, int payVout) {
        List<BitcoindRpcClient.TxInput> inputs = new ArrayList<>();
        List<BitcoindRpcClient.TxOutput> outputs = new ArrayList<>();
        List<String> keys = new ArrayList<>();

        outputs.add(new BitcoindRpcClient.BasicTxOutput(txtAddressVout0.getText(), getAmount("Vout0")));
        outputs.addAll(coins);

        if (chkBxFirstTime.isSelected()) {
            outputs.remove(outputs.size() - 1); // Remove and then reinsert the last output with the "Name" payload included
            outputs.add(new BitcoindRpcClient.BasicTxOutput(coins.get(coins.size() - 1).address(), coins.get(coins.size() - 1).amount(), payload.getBytes(StandardCharsets.UTF_8)));
        } else {
            inputs.add(new BitcoindRpcClient.BasicTxInput(txtTXID.getText(), 0));
            keys.add(txtPrivateKey.getText());
        }

        inputs.add(new BitcoindRpcClient.BasicTxInput(payTXID, payVout));
        keys.add(rpcClient.dumpPrivKey(payAddress));

        try {
            return rpcClient.sendRawTransaction(rpcClient.signRawTransactionWithKey(rpcClient.createRawTransaction(inputs, outputs), keys, null, SignatureHashType.ALL).hex());
        } catch (Exception ignored) {
            return "NOPE";
        }
    }

    // Is the address checksum valid?
    private static boolean validChecksum(String address) {
        byte[] values = new byte[address.length() - address.lastIndexOf('1') - 1];

        for (int i = 0; i < values.length; ++i)
            values[i] = Bech32.CHARSET_REV[address.charAt(i + address.lastIndexOf('1') + 1)];

        return Bech32.verifyChecksum(address.substring(0, address.lastIndexOf('1')), values);
    }

    // Is the coin amount valid?
    private static boolean validAmount(BigDecimal amount) {
        final long amountSATS = amount.longValue();
        final long amountBTC = amount.multiply(new BigDecimal(100000000)).longValue();
        return amountSATS == 1000000 || amountBTC == 1000000 ||
                amountSATS == 500000 || amountBTC == 500000 ||
                amountSATS == 250000 || amountBTC == 250000 ||
                amountSATS == 100000 || amountBTC == 100000 ||
                amountSATS == 50000 || amountBTC == 50000 ||
                amountSATS == 25000 || amountBTC == 25000 ||
                amountSATS == 10000 || amountBTC == 10000;
    }

    // Shows the QR code for the "Pay Address" and provides a method for up-to-date information to the user while finalizing the transaction
    private class TXInfo extends JPanel {
        JLabel lblInfo; // Holds the progress report

        public TXInfo() {
            super(new GridLayout(17, 1));
            this.setPreferredSize(new Dimension(340, 340));

            JLabel lblSend = new JLabel();
            lblSend.setText(" Send EXACTLY " + getAmount("Total") + " BTC to");
            this.add(lblSend);

            JLabel lblAddress = new JLabel();
            lblAddress.setText("     " + payAddress);
            this.add(lblAddress);

            JLabel lblFeeRate = new JLabel();
            lblFeeRate.setText(" With a Fee Rate of " + (new DecimalFormat("0.000")).format(feeRate) + "+ $ATS/BYTE");
            this.add(lblFeeRate);

            for (int i = 4; i <= 15; i++)
                this.add(new JLabel()); // Dummy Spacers

            lblInfo = new JLabel();
            lblInfo.setText("");
            this.add(lblInfo);
        }

        public void update(String info) { // Updates the progress report
            lblInfo.setText(" " + info);
            this.revalidate();
        }

        @Override // Override paint routine to show the QR code
        public void paint(Graphics g) {
            super.paint(g);

            final Map<EncodeHintType, Object> qrEncoding = new HashMap<>();
            qrEncoding.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M); // L = ~7%, M = ~15%, Q = ~25%, H = ~30%
            qrEncoding.put(EncodeHintType.MARGIN, 0);

            BitMatrix m = null;
            try {
                m = (new QRCodeWriter()).encode(payAddress, BarcodeFormat.QR_CODE, 0, 0, qrEncoding);
            } catch (WriterException e) {
                e.printStackTrace();
            }

            assert m != null;
            int dotSize = 200 / m.getWidth(); // Integer division: the result is an integer and is truncated (fractional part thrown away). Not rounded to the closest integer.
            double r = (double) (200 % m.getWidth()) / (double) m.getWidth(); // Decimal remainder: used to help make the QR code the exact size.

            int xPixelCount = 0;
            int xPixel;
            for (int _x = 0; _x < m.getWidth(); _x++) {
                xPixel = (int) (r * (double) (_x + 1)) - (int) (r * (double) _x);
                int yPixelCount = 0;
                int yPixel;
                for (int _y = 0; _y < m.getWidth(); _y++) {
                    yPixel = (int) (r * (double) (_y + 1)) - (int) (r * (double) _y);
                    if (m.get(_x, _y)) {
                        g.fillRect(70 + (_x * dotSize) + xPixelCount, 80 + (_y * dotSize) + yPixelCount, dotSize + xPixel, dotSize + yPixel);
                    }
                    yPixelCount = yPixelCount + yPixel;
                }
                xPixelCount = xPixelCount + xPixel;
            }
        }
    }

    // Supervises the transaction process while loading the coins
    private class TXControl extends TimerTask {
        public static final long DELAY = 500L; // Recommended Timer delay and period (milliseconds)

        private final TXInfo txInfo; // The handle on the TXInfo instance to provide the user up-to-date information
        private int periods; // For counting periods ('', '.', '..', '...', '....') to indicate activity

        private String txid; // The txid for these newly loaded coins :-)

        public TXControl(TXInfo txInfo) {
            this.txInfo = txInfo;
            periods = 0;

            txid = "";
        }

        @Override
        public void run() {
            if (periods == 4)
                periods = 0;
            else
                periods++;
            txInfo.update("Waiting for The Money" + (periods == 0 ? "" : (periods == 1 ? "." : (periods == 2 ? ".." : (periods == 3 ? "..." : "....")))));

            List<BitcoindRpcClient.Transaction> transactions; // Get the most recent transactions
            try {
                transactions = rpcClient.listTransactions("*", 10);
            } catch (Exception ignored) {
                txInfo.update("Communication Error");
                exitFlag = false; // Clear control flag that causes an exit warning
                this.cancel();
                return;
            }

            for (BitcoindRpcClient.Transaction tx : transactions) { // Searching through all recent transaction for one containing the "Pay Address"
                if (tx.category().equals("receive") && tx.address().equals(payAddress)) {
                    exitFlag = false; // Clear control flag that causes an exit warning
                    if (tx.amount().equals(getAmount("Total"))) { // Houston, we have our transaction
                        txid = txFinale(tx.txId(), Integer.parseInt(tx.mapStr("vout")));

                        if (!txid.matches("^[0-9a-f]{64}$")) {
                            txInfo.update("Transaction Failure");
                            this.cancel();
                            return;
                        }

                        txtAreaCoinInfo.append("\n****************************** TRANSACTION SUCCESS!!! ******************************\n");
                        txtAreaCoinInfo.append("TXID (New):       " + txid + "\n");
                        txtAreaCoinInfo.setCaretPosition(txtAreaCoinInfo.getDocument().getLength()); // Advance the cursor position to the bottom

                        txInfo.update("Transaction Success");
                    } else {
                        txInfo.update("Wrong Amount Sent (" + tx.amount() + " BTC)");
                    }
                    this.cancel();
                }
            }
        }
    }

    // Class used to support the Combo Box (comboFee) configuration
    private static class ComboItem {
        private final String text;
        private final BigDecimal value;

        public ComboItem(String text, BigDecimal value) {
            this.text = text;
            this.value = value;
        }

        public BigDecimal getValue() {
            return value;
        }

        @Override
        public String toString() {
            return text;
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
        mainPanel.setLayout(new GridLayoutManager(9, 2, new Insets(0, 10, 10, 10), -1, -1));
        btnInitiate = new JButton();
        btnInitiate.setText("Initiate Transaction");
        mainPanel.add(btnInitiate, new GridConstraints(8, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(620, 20), new Dimension(620, 20), new Dimension(620, 20), 0, false));
        scrollPaneCoinInfo = new JScrollPane();
        scrollPaneCoinInfo.setHorizontalScrollBarPolicy(31);
        scrollPaneCoinInfo.setVerticalScrollBarPolicy(20);
        mainPanel.add(scrollPaneCoinInfo, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(620, 500), new Dimension(620, 500), new Dimension(620, 500), 0, false));
        txtAreaCoinInfo = new JTextArea();
        txtAreaCoinInfo.setColumns(0);
        txtAreaCoinInfo.setToolTipText("Enter the address for each coin followed by the amount, in $ATOSHIS, to be loaded (space delimited)");
        scrollPaneCoinInfo.setViewportView(txtAreaCoinInfo);
        txtTXID = new JTextField();
        txtTXID.setEnabled(true);
        txtTXID.setToolTipText("The TXID of the previous Satoshi Coins transaction");
        mainPanel.add(txtTXID, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(560, 20), new Dimension(560, 20), new Dimension(560, 20), 0, false));
        lblTXID = new JLabel();
        lblTXID.setEnabled(true);
        lblTXID.setText("TXID");
        lblTXID.setToolTipText("The TXID of the previous Satoshi Coins transaction");
        mainPanel.add(lblTXID, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(50, 20), new Dimension(50, 20), new Dimension(50, 20), 0, false));
        lblPrivateKey = new JLabel();
        lblPrivateKey.setEnabled(true);
        lblPrivateKey.setText("Key");
        lblPrivateKey.setToolTipText("The unlock key to output 0 of the previous Satoshi Coins transaction");
        mainPanel.add(lblPrivateKey, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(50, 20), new Dimension(50, 20), new Dimension(50, 20), 0, false));
        lblVout0 = new JLabel();
        lblVout0.setEnabled(true);
        lblVout0.setText("Vout 0");
        lblVout0.setToolTipText("Output 0 of this transaction; used to extend this chain of Satoshi Coins");
        mainPanel.add(lblVout0, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(50, 20), new Dimension(50, 20), new Dimension(50, 20), 0, false));
        txtPrivateKey = new JTextField();
        txtPrivateKey.setEnabled(true);
        txtPrivateKey.setToolTipText("The unlock key to output 0 of the previous Satoshi Coins transaction");
        mainPanel.add(txtPrivateKey, new GridConstraints(5, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(560, 20), new Dimension(560, 20), new Dimension(560, 20), 0, false));
        txtAddressVout0 = new JTextField();
        txtAddressVout0.setToolTipText("Output 0 of this transaction; used to extend this chain of Satoshi Coins");
        mainPanel.add(txtAddressVout0, new GridConstraints(6, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(560, 20), new Dimension(560, 20), new Dimension(560, 20), 0, false));
        lblName = new JLabel();
        lblName.setEnabled(false);
        lblName.setText("Name");
        lblName.setToolTipText("Enter the name of the bank followed by your name");
        mainPanel.add(lblName, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(50, 20), new Dimension(50, 20), new Dimension(50, 20), 0, false));
        txtName = new JTextField();
        txtName.setEnabled(false);
        txtName.setToolTipText("Enter the name of the bank followed by your name");
        mainPanel.add(txtName, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(560, 20), new Dimension(560, 20), new Dimension(560, 20), 0, false));
        comboFee = new JComboBox();
        mainPanel.add(comboFee, new GridConstraints(7, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(560, 20), new Dimension(560, 20), new Dimension(560, 20), 0, false));
        lblCoinInfo = new JLabel();
        lblCoinInfo.setHorizontalAlignment(0);
        lblCoinInfo.setHorizontalTextPosition(0);
        lblCoinInfo.setText("Coin Address and Amount");
        lblCoinInfo.setToolTipText("Enter the address for each coin followed by the amount, in $ATOSHIS, to be loaded (space delimited)");
        mainPanel.add(lblCoinInfo, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(620, 20), new Dimension(620, 20), new Dimension(620, 20), 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("Tx Fee");
        mainPanel.add(label1, new GridConstraints(7, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(50, 20), new Dimension(50, 20), new Dimension(50, 20), 0, false));
        chkBxFirstTime = new JCheckBox();
        chkBxFirstTime.setSelected(false);
        chkBxFirstTime.setText("First Time");
        chkBxFirstTime.setToolTipText("Check here to begin a new chain of Satoshi Coins");
        mainPanel.add(chkBxFirstTime, new GridConstraints(2, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(620, 20), new Dimension(620, 20), new Dimension(620, 20), 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }

    //endregion
}