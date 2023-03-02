/*
 *      This class encapsulates multiple random number generators. Each random
 *      byte is a result of XOR'ing bytes from each generator. This class also
 *      provides the interface required for RNG compatibility and a method
 *      runQC() that shows a quick visual check of the integrity of each RNG.
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
package org.satoshiware.keystickers.random;

import java.util.ArrayList;

public class KSGenerator {
    public static int OUTPUTLENGTH = 64; // Number of bytes to display to user per line
    public static int LINECOUNT = 16; // Number of lines to display to user per generator

    private final ArrayList<RandomInterface> generators;

    public KSGenerator() {
        generators = new ArrayList<>();
    }

    // Add an RNG that uses the RandomInterface to this instance
    public void addGenerator(RandomInterface ri) {
        generators.add(ri);
    }

    // XOR bytes from each RNG and store the result in the "bytes" array
    public void getBytes(byte[] bytes) {
        for(int i = 0; i < bytes.length; i++) {
            bytes[i] = 0;
            for (RandomInterface generator : generators) {
                bytes[i] = (byte) (bytes[i] ^ generator.getByte());
            }
        }
    }

    // This method shows a quick visual check of the integrity of each RNG
    public void runQC() {
        System.out.println("Visual RNG Sanity Check:");
        for (RandomInterface generator : generators) {
            System.out.println("\t" + generator.getName() + ":");

            byte[] out = new byte[OUTPUTLENGTH];
            for (int k = 0; k < LINECOUNT; k++) {
                for (int j = 0; j < OUTPUTLENGTH; j++) {
                    out[j] = generator.getByte();
                }
                System.out.println("\t\t" + bytesToHex(out));
            }
            System.out.print("\n");
        }
    }

    private static String bytesToHex(byte[] bytes) {
        char[] hexArray = "0123456789ABCDEF".toCharArray();
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    // This describes the requirements for RNG compatibility within this class
    public interface RandomInterface {
        byte getByte();

        String getName();
    }
}