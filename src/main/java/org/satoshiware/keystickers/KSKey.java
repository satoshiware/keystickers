/*
 *      This class somewhat wraps and extends BitcoinJ.ECKey for ease in
 *      generating and verifying Keystickers.
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

import org.bitcoinj.core.*;

public class KSKey {
<<<<<<< Updated upstream
    // These values (NOTVALID, WIF, TESTWIF, P2PKH, TESTP2PKH, NP2WPKH, and TESTNP2WPKH) are returned in the keyCheck method
    public static int NOTVALID = -1;

    public static int WIF = 0;
    public static int TESTWIF = 1; // Testnet WIF

    public static int P2PKH = 20;
    public static int TESTP2PKH = 21; // Testnet Bitcoin address

    public static int NP2WPKH = 40; // Native segwit address (Bech32)
    public static int TESTNP2WPKH = 41; // Native segwit testnet address (Bech32)

    private ECKey key;
=======
    private final ECKey key;
>>>>>>> Stashed changes
    public boolean testnet;

    // Construct a KSKey with the private key derived from the passed bytes
    public KSKey(byte[] bytes, boolean forTestnet) { // Constructor 1
        if(bytes.length != 32) {
            throw new IllegalArgumentException("KSKey constructor requires exactly 32 bytes of random data");
        }

        key = ECKey.fromPrivate(bytes);

        testnet = forTestnet;
    }

    // Construct a KSKey with the passed WIF
    public KSKey(String WIF) { // Constructor 2
        key = (DumpedPrivateKey.fromBase58(null, WIF)).getKey();

        if(WIF.substring(0, 1).equals("c")) {
            testnet = true; // Testnet Leading Symbol: c
        }else if(WIF.substring(0, 1).equals("K") || WIF.substring(0, 1).equals("L")) {
            testnet = false; // Mainnet Leading Symbol: K or L
        }else {
            throw new IllegalArgumentException("KSKey constructor: Invalid or non-supported WIF");
        }
    }

    public String getP2PKH() {
        byte[] address = new byte[1 + key.getPubKeyHash().length + 4];
        address[0] = 0; // Mainnet Leading Symbol: 1
        if(testnet) address[0] = 111; // Testnet Leading Symbol: m or n
        System.arraycopy(key.getPubKeyHash(), 0, address, 1, key.getPubKeyHash().length);
        byte[] check = Sha256Hash.hashTwice(address, 0, key.getPubKeyHash().length + 1);
        System.arraycopy(check, 0, address, key.getPubKeyHash().length + 1, 4);
        return Base58.encode(address);
    }

    // Returns native segwit public address (bech32) encoded with the "version" number
    public String getP2WPKH(int version) {
        byte[] convertedBytes = Bech32.convertBits(key.getPubKeyHash(), 0, key.getPubKeyHash().length, 8, 5, true);
        byte[] bytes = new byte[1 + convertedBytes.length];
        bytes[0] = (byte) (Bech32.encodeToOpN(version) & 0xff);
        System.arraycopy(convertedBytes, 0, bytes, 1, convertedBytes.length);

        if(testnet){
            return Bech32.encode("tb", bytes).toUpperCase();
        }else {
            return Bech32.encode("bc", bytes).toUpperCase();
        }
    }

    public String getWIF() {
        if (testnet) {
            return key.getPrivateKeyAsWiF(NetworkParameters.fromID(NetworkParameters.ID_TESTNET)); // Testnet Leading Symbol: c
        }else {
            return key.getPrivateKeyAsWiF(NetworkParameters.fromID(NetworkParameters.ID_MAINNET)); // Mainnet Leading Symbol: K or L
        }
    }

    // Calculates and returns the Quality Control (QC) number for the WIF
    public String getQCWIF() {
        return getQCnumber(this.getWIF());
    }

    // Calculates and returns the Quality Control (QC) number for legacy type address
    public String getQCP2PKH() {
        return getQCnumber(this.getP2PKH());
    }

    // Calculates and returns the Quality Control (QC) number for Bech32 type address
    public String getQCP2WPKH(int version) {
        return getQCnumber(this.getP2WPKH(version));
    }

    // Methods support getQC... routines above
    public static String getQCnumber(String s) {
        byte[] hash = Sha256Hash.hashTwice(s.getBytes());
        byte[] bytes = Bech32.convertBits(hash, 0, hash.length, 8, 5, true);
        return Bech32.encode("qc", bytes).substring(3, 8).toUpperCase();
    }

    // Routine used to determine the type of the key passed as a string
    public static int keyCheck(String s) {
        if(s.length() < 3) return NOTVALID;

        if(s.substring(0,1).equals("m") || s.substring(0,1).equals("n")) { // Testnet P2PKH
            if(s.length() != 34) {
                return NOTVALID;
            }
            try {
                Address a = Address.fromBase58(null, s);
            } catch (AddressFormatException e) {
                return NOTVALID;
            }
                return TESTP2PKH;
        }else if(s.substring(0,1).equals("1")) { // P2PKH
            if(s.length() > 34) {
                return NOTVALID;
            }
            try {
                Address a = Address.fromBase58(null, s);
            } catch (AddressFormatException e) {
                return NOTVALID;
            }
            return P2PKH;
        }else if(s.substring(0,3).equals("bc1") || s.substring(0,3).equals("tb1") || s.substring(0,3).equals("BC1") || s.substring(0,3).equals("TB1")) { // Native P2WPKH
            if(s.length() != 42) {
                return NOTVALID;
            }
            try {
                byte[] b = Bech32.decode(s.toLowerCase()).data;
            }catch (Exception e) {
                return NOTVALID;
            }
            if(s.substring(0,3).equals("bc1") || s.substring(0,3).equals("BC1")) {
                return NP2WPKH;
            }else {
                return TESTNP2WPKH;
            }
         }else if(s.substring(0,1).equals("K") || s.substring(0,1).equals("L") || s.substring(0,1).equals("c")) { // WIF
            if(s.length() != 52) {
                return NOTVALID;
            }
            try {
                new KSKey(s);
            }catch (IllegalArgumentException e) {
                return NOTVALID;
            }
            if(s.substring(0,1).equals("c")) {
                return TESTWIF;
            }else {
                return WIF;
            }
        }else {
            return NOTVALID;
        }
    }
}