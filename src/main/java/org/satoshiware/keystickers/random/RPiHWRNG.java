/*
 *      This class creates an input stream from the Raspberry Pi's HWRNG.
 *      Implements Callable to create threads and RandomInterface for
 *      KSGenerator compatibility.
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

import java.io.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.EmptyStackException;

public class RPiHWRNG implements Callable<Object>, KSGenerator.RandomInterface {
    public static final int STREAMREADSIZE = 1024; // How many bytes to read from stream
    public static final int NUMFOLDS = 4; // Number of stream reads XOR'ed together.
    public static final int STACKSIZE = 131072; // (128K) Number of random bytes folded and stored in this instance.
    public static final TimeUnit TIMEUNIT = TimeUnit.MILLISECONDS; // Interpretation of time parameters.
    public static final int OPENTIMEOUT = 2500; // Maximum amount of time to open a stream.
    public static final int CLOSETIMEOUT = 8000; // Maximum amount of time to close a stream.
    public static final int STACKTIMEOUT = 60000; // Maximum amount of time loading the stack with random numbers.
    public static final String rndSrc = "/dev/hwrng"; // Location or source of the Raspberry Pi's HWRNG

    private ExecutorService executor; // Used create threads for opening, closing, and reading from external sources
    private DataInputStream in; // The input stream to an external source (RNG or ".dat" file containing random numbers)

    private final Stack stack; // Holds the folded (XOR'ed) random data read from the source

    private String action; // What action is taken with call(): OPEN, FILLSTACK, CLOSE.

    public RPiHWRNG(){
        stack = new Stack(STACKSIZE);
    }

    // Opens the source; uses the call() routine to make a new thread
    public boolean open() {
        if(in != null) {
            System.out.println("The \"" + rndSrc + "\"" + " is already opened or had initially failed to open.");
            return false;
        }
        if(executor != null) executor.shutdown(); // Shutdown any existing ExecutorService instance before creating a new one.
        executor = Executors.newFixedThreadPool(1);

        action = "OPEN";
        Future<Object> future = executor.submit(this);

        System.out.println("Opening the \"" + rndSrc + "\" stream on the Raspberry Pi.");

        long startTime = System.nanoTime();
        long progressTime = System.nanoTime();
        while (!future.isDone() && (System.nanoTime() - startTime) < TIMEUNIT.toNanos(OPENTIMEOUT)) {
            if (((System.nanoTime() - progressTime) > 250000000)) { // Add a dot to show progress every 250 mS
                progressTime = System.nanoTime();
                System.out.print(".");
            }
        }

        if((System.nanoTime() - startTime) < TIMEUNIT.toNanos(OPENTIMEOUT)) {
            try {
                future.get();
            }catch (ExecutionException | InterruptedException e) {
                System.out.print("The \"" + rndSrc + "\" stream was NOT successfully opened: ");
                if(e.getMessage().contains("Permission"))
                    System.out.println("Permission Denied!");
                else
                    System.out.println("You are not running on a Raspberry Pi with HWRNG support!");

                in = null;
                return false;
            }
            System.out.println(" Success!");
            return true;
        }else {
            System.out.println("The \"" + rndSrc + "\" could NOT be opened in a reasonable amount of time.");
            in = null;
            return false;
        }
    }

    // Reads from the source until the stack is full; uses the call() routine to make a new thread
    public boolean fillStack() {
        if(in == null) {
            System.out.println("The \"" + rndSrc + "\" stream is not available to read.");
            return false;
        }

        action = "FILLSTACK";
        Future<Object> future = executor.submit(this);

        System.out.print("Reading " + (STACKSIZE - stack.remaining()) + " bytes from the \"" + rndSrc + "\" stream.");

        long startTime = System.nanoTime();
        long progressTime = System.nanoTime();
        while (!future.isDone() && (System.nanoTime() - startTime) < TIMEUNIT.toNanos(STACKTIMEOUT)) {
            if (((System.nanoTime() - progressTime) > 2000000000)) { // Add a dot to show progress every 2 seconds
                progressTime = System.nanoTime();
                System.out.print(".");
            }
        }

        if((System.nanoTime() - startTime) < TIMEUNIT.toNanos(STACKTIMEOUT)) {
            try {
                future.get();
            }catch (ExecutionException | InterruptedException e) {
                System.out.println("The \"" + rndSrc + "\" stream was NOT successfully read: " + e.getMessage());
                return false;
            }
            System.out.println(" Success!");
            return true;
        }else {
            System.out.println("The \"" + rndSrc + "\" stream could NOT be read in a reasonable amount of time.");
            return false;
        }
    }

    // Closes the source; uses the call() routine to make a new thread
    public void close() {
        if(in == null) {
            System.out.println("The \"" + rndSrc + "\" stream is not available to close.");
            return;
        }

        action = "CLOSE";
        Future<Object> future = executor.submit(this);

        System.out.print("Closing the \"" + rndSrc + "\" Stream.");

        long startTime = System.nanoTime();
        long progressTime = System.nanoTime();
        while (!future.isDone() && (System.nanoTime() - startTime) < TIMEUNIT.toNanos(CLOSETIMEOUT)) {
            if (((System.nanoTime() - progressTime) > 250000000)) { // Add a dot to show progress every 250 mS
                progressTime = System.nanoTime();
                System.out.print(".");
            }
        }

        if((System.nanoTime() - startTime) < TIMEUNIT.toNanos(CLOSETIMEOUT)) {
            try {
                future.get();
            }catch (ExecutionException | InterruptedException e) {
                System.out.println("The \"" + rndSrc + "\" stream was NOT successfully closed.");
                in = null;
                executor.shutdown();
                return;
            }
            System.out.println(" Success!");
        }else {
            System.out.println("the \"" + rndSrc + "\" stream could NOT be closed in a reasonable amount of time.");
        }
        in = null;
        executor.shutdown();
    }

    // Supports the open(), fillStack(), and close() methods
    public Object call() throws IOException {
        switch (action) {
            case "OPEN":
                action = "";
                in = new DataInputStream(new BufferedInputStream(new FileInputStream(rndSrc)));
                break;
            case "FILLSTACK":
                action = "";
                while (!stack.full()) {
                    byte[] foldedBytes = null;
                    for (int i = 0; i < NUMFOLDS; i++) {
                        byte[] bytes = new byte[STREAMREADSIZE];
                        int offset = 0;
                        int readCount = 0;
                        while ((readCount != -1) && (offset < STREAMREADSIZE)) {
                            readCount = in.read(bytes, offset, STREAMREADSIZE - offset);
                            offset += readCount;
                        }
                        if (readCount == -1)
                            throw new IOException("IOException: EOF has been reached! The \"" + rndSrc + "\" stream is inconsistent or too slow.");

                        if (foldedBytes == null) {
                            foldedBytes = bytes;
                        } else {
                            for (int j = 0; j < STREAMREADSIZE; j++) {
                                foldedBytes[j] ^= bytes[j];
                            }
                        }
                    }

                    stack.lock.lock();
                    stack.pushBytes(foldedBytes);
                    stack.lock.unlock();
                }
                break;
            case "CLOSE":
                action = "";
                in.close();
                break;
        }

        return new Object();
    }

    // RandomInterface Implementation
    public byte getByte() {
        if(stack.remaining() == 0) fillStack();

        stack.lock.lock();
        byte b = stack.pop();
        stack.lock.unlock();

        return b;
    }
    public String getName() {
        return rndSrc;
    }

    // Stack class used to hold the random data read and folded (XOR'ed) from the external RNG source or .dat file
    private static class Stack {
        private final byte[] stackArray;
        private int position;

        public ReentrantLock lock;

        public Stack(int size) {
            stackArray = new byte[size];
            position = 0;

            lock = new ReentrantLock();
        }

        public byte pop() {
            if(position == 0) throw new EmptyStackException();

            position--;
            return stackArray[position];
        }

        public void push(byte b) {
            if(position != stackArray.length) {
                stackArray[position] = b;
                position++;
            }
        }

        public void pushBytes(byte[] bytes) {
            for (byte aByte : bytes)
                push(aByte);
        }

        public int remaining() {
            return position;
        }

        public boolean full() {
            return (position == stackArray.length);
        }
    }
}