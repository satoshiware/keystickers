/*
 *      Class to capture entropy from the keyboard.
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

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;

public class KeyboardEntropy {
    public static int SEEDSIZE = 512; // Number of bytes in the seed; must be a multiple of md5.digest().length and OUTPUTLENGTH
    public static int OUTPUTLENGTH = 64; // Number of bytes to display to user per line

    private static MessageDigest md5; // md5 routine used to mix in the captured mouse entropy with the initial seed.

    private static byte[] seed; // Array links to initial entropy passed in the constructor; it is updated for each mouse movement.

    public static byte[] getEntropy(Scanner keyboard) {
        seed = new byte[SEEDSIZE];

        try { md5 = MessageDigest.getInstance("MD5"); } catch(NoSuchAlgorithmException ignored) {}

        System.out.println("Collecting Entropy... Enter a whole lotta random text and numbers:\n");
        byte[] input = keyboard.nextLine().getBytes();

        for(int i = 0; i < input.length; i++)
            updateSeed(i, System.nanoTime() * (long)input[i]);

        byte[] out = new byte[OUTPUTLENGTH];
        System.out.println("\nEntropy Created:");
        for (int i = 0; i < SEEDSIZE / OUTPUTLENGTH; i++) {
            System.arraycopy(seed, i * OUTPUTLENGTH, out, 0, OUTPUTLENGTH);
            System.out.println("\t" + bytesToHex(out));
        }
        System.out.print("\n");

        return seed;
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

    // Captured entropy is mixed in with the original seed
    private static void updateSeed(int shiftMultiplier, long entropy) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(entropy);
        md5.update(buffer.array());
        byte[] digest = md5.digest();

        for(int i = 0; i < digest.length; i++) {
            digest[i] = (byte)(seed[i + ((shiftMultiplier * digest.length) % SEEDSIZE)] ^ digest[i]);
        }

        System.arraycopy(digest, 0, seed, ((shiftMultiplier * digest.length) % SEEDSIZE), digest.length);
    }
}