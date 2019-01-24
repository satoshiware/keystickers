/*
 *      This class wraps org.jitsi.bccontrib.prng.FortunaGenerator implementing
 *      the RandomInterface. The Fortuna Generator is a well known well proven CSPRNG
 *      (Cryptographically Secure Pseudo-Random Number Generator).
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

import java.nio.ByteBuffer;

public class FortunaGenerator extends org.jitsi.bccontrib.prng.FortunaGenerator implements KSGenerator.RandomInterface {
    private static int BUFFERSIZE = 4096;

    private ByteBuffer buffer;

    public FortunaGenerator(byte[] seed) {
        super(seed);

        if(seed.length < 32) {
            throw new IllegalArgumentException("Minimum seed length is 32 bytes.");
        }

        byte[] bytes = new byte[BUFFERSIZE];
        this.nextBytes(bytes);
        buffer = ByteBuffer.wrap(bytes);
    }

    public byte getByte() {
        if(!buffer.hasRemaining()) {
            byte[] bytes = new byte[BUFFERSIZE];
            this.nextBytes(bytes);
            buffer = ByteBuffer.wrap(bytes);
        }

        return buffer.get();
    }

    public String getInfo() {
        String s = "Provider: Keystickers\n";
        s += "Version: 1.0\n";
        s += "Algorithm: Fortuna CSPRNG";

        return s;
    }

    public String getName() {
        return "org.jitsi.bccontrib.prng.FortunaGenerator";
    }
}