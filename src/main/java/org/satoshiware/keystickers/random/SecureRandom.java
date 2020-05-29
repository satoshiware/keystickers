/*
 *      This class wraps java.security.SecureRandom implementing the
 *      RandomInterface
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

public class SecureRandom extends java.security.SecureRandom implements KSGenerator.RandomInterface {
    private static final int BUFFERSIZE = 4096;

    private ByteBuffer buffer;

    public SecureRandom() {
        super();

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

    public String getName() {
        return "java.security.SecureRandom";
    }
}