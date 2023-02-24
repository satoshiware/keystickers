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
    private final ECKey key;

    // Construct a KSKey with the private key derived from the passed bytes
    public KSKey(byte[] bytes) { // Constructor 1
        if(bytes.length != 32) {
            throw new IllegalArgumentException("KSKey constructor requires exactly 32 bytes of random data");
        }

        key = ECKey.fromPrivate(bytes);
    }

    // Returns native segwit public address (bech32) encoded with the "version" number
    public String getP2WPKH(String hrp, int version) {
        byte[] convertedBytes = Bech32.convertBits(key.getPubKeyHash(), 0, key.getPubKeyHash().length, 8, 5, true);
        byte[] bytes = new byte[1 + convertedBytes.length];
        bytes[0] = (byte) (Bech32.encodeToOpN(version) & 0xff);
        System.arraycopy(convertedBytes, 0, bytes, 1, convertedBytes.length);
        return Bech32.encode(hrp, bytes);
    }

    public String getWIF(boolean testnet) {
        if (testnet) {
            return key.getPrivateKeyAsWiF(NetworkParameters.fromID(NetworkParameters.ID_TESTNET)); // Testnet Leading Symbol: c
        }else {
            return key.getPrivateKeyAsWiF(NetworkParameters.fromID(NetworkParameters.ID_MAINNET)); // Mainnet Leading Symbol: K or L
        }
    }
}