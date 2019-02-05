/*
 *      This class is used to run Quality Control (QC) on some or all of the
 *      Keystickers.
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

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class QCApp {
    private JPanel mainPanel;

    private JTextField textKeyField;
    private JLabel lblType;
    private JLabel lblKeyAddressEntry;
    private JLabel lblKeyField;
    private JLabel lblQCNumber;
    private JLabel lblKeyPair;

    private String previousQCWIF; // Previously scanned WIF
    private String previousQCP2PKH; // Previously scanned P2PKH
    private String previousQCNP2WPKH; // Previously scanned Native P2WPKH
    private Color defaultBG; // Default background of the frame
    private static Color keyPairFoundBG = Color.PINK; // Background of the frame to indicate a valid Key Pair is found.

    public QCApp() {
        previousQCWIF = "?????";
        previousQCP2PKH = "?????";
        previousQCNP2WPKH = "?????";
        defaultBG = mainPanel.getBackground();

        /* Routine verifies the scanned key (public or private) for validity
            and compares it with previously scanned key (private or public)
            in search for a valid Keypair. */
        textKeyField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(KSKey.keyCheck(textKeyField.getText()) == KSKey.WIF || KSKey.keyCheck(textKeyField.getText()) == KSKey.TESTWIF) {
                    if(KSKey.keyCheck(textKeyField.getText()) == KSKey.WIF) {
                        lblType.setText("Key Type: Private Key");
                    }else {
                        lblType.setText("Key Type: Private Key (Testnet)");
                    }

                    KSKey key = new KSKey(textKeyField.getText());
                    if(previousQCP2PKH.equals(key.getQCP2PKH()) && previousQCWIF.equals("?????")) {
                        lblQCNumber.setText("QC: " + key.getQCWIF() + " - " + previousQCP2PKH);
                        lblKeyPair.setText("Valid Key Pair");
                        mainPanel.setBackground(keyPairFoundBG);
                    }else if(previousQCNP2WPKH.equals(key.getQCP2WPKH(0)) && previousQCWIF.equals("?????")) {
                        lblQCNumber.setText("QC: " + key.getQCWIF() + " - " + previousQCNP2WPKH);
                        lblKeyPair.setText("Valid Key Pair");
                        mainPanel.setBackground(keyPairFoundBG);
                    }else {
                        lblQCNumber.setText("QC: " + key.getQCWIF() + " - ?????");
                        previousQCP2PKH = key.getQCP2PKH();
                        previousQCNP2WPKH = key.getQCP2WPKH(0);
                        lblKeyPair.setText("");
                        mainPanel.setBackground(defaultBG);
                    }

                    previousQCWIF = key.getQCWIF();
                }else if(KSKey.keyCheck(textKeyField.getText()) == KSKey.P2PKH || KSKey.keyCheck(textKeyField.getText()) == KSKey.TESTP2PKH) {
                    if(KSKey.keyCheck(textKeyField.getText()) == KSKey.P2PKH) {
                        lblType.setText("Key Type: Public Address");
                    }else {
                        lblType.setText("Key Type: Public Address (Testnet)");
                    }

                    if(previousQCP2PKH.equals(KSKey.getQCnumber(textKeyField.getText()))) {
                        lblQCNumber.setText("QC: " + previousQCWIF + " - " + KSKey.getQCnumber(textKeyField.getText()));
                        previousQCNP2WPKH = "?????";
                        if(previousQCWIF.equals("?????")) {
                            lblKeyPair.setText("");
                            mainPanel.setBackground(defaultBG);
                        }else {
                            lblKeyPair.setText("Valid Key Pair");
                            mainPanel.setBackground(keyPairFoundBG);
                        }
                    }else {
                        lblQCNumber.setText("QC: ????? - " + KSKey.getQCnumber(textKeyField.getText()));
                        previousQCP2PKH = KSKey.getQCnumber(textKeyField.getText());
                        previousQCNP2WPKH = "?????";
                        lblKeyPair.setText("");
                        mainPanel.setBackground(defaultBG);
                    }
                    previousQCWIF = "?????";
                }else if(KSKey.keyCheck(textKeyField.getText()) == KSKey.NP2WPKH || KSKey.keyCheck(textKeyField.getText()) == KSKey.TESTNP2WPKH) {
                    if(KSKey.keyCheck(textKeyField.getText()) == KSKey.NP2WPKH) {
                        lblType.setText("Key Type: SEGWIT Address");
                    }else {
                        lblType.setText("Key Type: SEGWIT Address (Testnet)");
                    }

                    if(previousQCNP2WPKH.equals(KSKey.getQCnumber(textKeyField.getText()))) {
                        lblQCNumber.setText("QC: " + previousQCWIF + " - " + KSKey.getQCnumber(textKeyField.getText()));
                        previousQCP2PKH = "?????";
                        if(previousQCWIF.equals("?????")) {
                            lblKeyPair.setText("");
                            mainPanel.setBackground(defaultBG);
                        }else {
                            lblKeyPair.setText("Valid Key Pair");
                            mainPanel.setBackground(keyPairFoundBG);
                        }
                    }else {
                        lblQCNumber.setText("QC: ????? - " + KSKey.getQCnumber(textKeyField.getText()));
                        previousQCP2PKH = "?????";
                        previousQCNP2WPKH = KSKey.getQCnumber(textKeyField.getText());
                        lblKeyPair.setText("");
                        mainPanel.setBackground(defaultBG);
                    }
                    previousQCWIF = "?????";
                }else {
                    lblType.setText("Key Type: Not Valid");
                    lblQCNumber.setText("QC: ????? - ?????");

                    previousQCWIF = "?????";
                    previousQCP2PKH = "?????";
                    previousQCNP2WPKH = "?????";
                    lblKeyPair.setText("");
                    mainPanel.setBackground(defaultBG);
                }

                lblKeyAddressEntry.setText("Key Entered: " + textKeyField.getText());
                textKeyField.setText("");
            }
        });
    }

    public static void runQCApp() {
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JFrame frame = new JFrame("QC Validation");
                frame.setContentPane((new QCApp()).mainPanel);
                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                frame.pack();
                frame.setVisible(true);
                frame.setResizable(false);
            }
        });
    }
}
