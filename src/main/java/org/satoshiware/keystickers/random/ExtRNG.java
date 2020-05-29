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
<<<<<<< Updated upstream:src/main/java/org/satoshiware/keystickers/random/ExtRNG.java

=======
>>>>>>> Stashed changes:src/main/java/org/satoshiware/keystickers/random/RPiHWRNG.java
package org.satoshiware.keystickers.random;

import javax.swing.*;
import java.io.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.EmptyStackException;

<<<<<<< Updated upstream:src/main/java/org/satoshiware/keystickers/random/ExtRNG.java
public class ExtRNG implements Callable<Object>, KSGenerator.RandomInterface {
    public static int STREAMREADSIZE = 1024; // How many bytes to read from stream
    public static int NUMFOLDS = 4; // Number of stream reads XOR'ed together
    public static int STACKSIZE = 131072; // (128K) Number of random bytes folded and stored in this instance
    public static TimeUnit TIMEUNIT = TimeUnit.MILLISECONDS; // Interpretation of time parameters.
    public static int OPENTIMEOUT = 2500; // Maximum amount of time to open a stream
    public static int CLOSETIMEOUT = 8000; // Maximum amount of time to close a stream
    public static int CLRBUFFERTIME = 3000; // Buffer clearing duration (for measuring baud rate).
    public static int MINIMUMBAUDRATE = 3500; // Minimum acceptable baud rate (bps)
    public static int MEASURETIME = 5000; // Sampling time (for measuring baud rate).
    public static int STACKTIMEOUT = 60000; // Maximum amount of time loading the stack with random numbers.
=======
public class RPiHWRNG implements Callable<Object>, KSGenerator.RandomInterface {
    public static final int STREAMREADSIZE = 1024; // How many bytes to read from stream
    public static final int NUMFOLDS = 4; // Number of stream reads XOR'ed together.
    public static final int STACKSIZE = 131072; // (128K) Number of random bytes folded and stored in this instance.
    public static final TimeUnit TIMEUNIT = TimeUnit.MILLISECONDS; // Interpretation of time parameters.
    public static final int OPENTIMEOUT = 2500; // Maximum amount of time to open a stream.
    public static final int CLOSETIMEOUT = 8000; // Maximum amount of time to close a stream.
    public static final int STACKTIMEOUT = 60000; // Maximum amount of time loading the stack with random numbers.
    public static final String rndSrc = "/dev/hwrng"; // Location or source of the Raspberry Pi's HWRNG
>>>>>>> Stashed changes:src/main/java/org/satoshiware/keystickers/random/RPiHWRNG.java

    private ExecutorService executor; // Used create threads for opening, closing, and reading from external sources
    private DataInputStream in; // The input stream to an external source (RNG or ".dat" file containing random numbers)

    private final Stack stack; // Holds the folded (XOR'ed) random data read from the source

    private String action; // What action is taken with call(): OPEN, FILLSTACK, CLOSE.

<<<<<<< Updated upstream:src/main/java/org/satoshiware/keystickers/random/ExtRNG.java
    public ExtRNG(File rndSrc){
        this.rndSrc = rndSrc;

        stack = new Stack(STACKSIZE);
        if(rndSrc.getName().toLowerCase().substring(rndSrc.getName().length() - 4).equals(".dat")) { // Is it a file (ends with ".dat")?
            isFile = true;
        }else {
            isFile = false;
        }
        baud = "NA";
=======
    public RPiHWRNG(){
        stack = new Stack(STACKSIZE);
>>>>>>> Stashed changes:src/main/java/org/satoshiware/keystickers/random/RPiHWRNG.java
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
<<<<<<< Updated upstream:src/main/java/org/satoshiware/keystickers/random/ExtRNG.java
    }

    // Measures the baud rate and returns true if successful
    public boolean baudrate() { // Measure and return baud rate (bps)
        int ARRAYSIZE = 1024; // Size of the byte array used to read from the stream

        if(in == null) {
            System.out.println("There is no available stream to measure baud rate.");
            return false;
        }

        System.out.println("Measuring \"" + rndSrc.getPath() + "\" baud rate:");
        System.out.print("\tClearing \"" + rndSrc.getPath() + "\" buffer");
        try {
            byte[] bytes = new byte[ARRAYSIZE];
            long startTime = System.nanoTime();
            long progressTime = startTime;
            while ((System.nanoTime() - startTime) < TIMEUNIT.toNanos(CLRBUFFERTIME)) { // Clear the buffer
                in.read(bytes);
                if (((System.nanoTime() - progressTime) > 250000000)) { // Add a dot to show progress every 250 mS
                    progressTime = System.nanoTime();
                    System.out.print(".");
                }
            }
            System.out.println(" Success!");


            System.out.print("\tSampling \"" + rndSrc + "\" data stream");
            long bytesRead = 0;
            startTime = System.nanoTime();
            while ((System.nanoTime() - startTime) < TIMEUNIT.toNanos(MEASURETIME)) {
                bytesRead += in.read(bytes);
                if (((System.nanoTime() - progressTime) > 250000000)) { // Add a dot to show progress every 250 mS
                    progressTime = System.nanoTime();
                    System.out.print(".");
                }
            }
            System.out.println(" Success!");

            long baudRate = (long) (8 * (bytesRead / ((double) (System.nanoTime() - startTime) / 1000000000))); // Calculate baud rate in bps.
            if (baudRate < 1000) {
                baud = Long.toString(baudRate) + " bps";
            } else if (baudRate < 1000000) {
                baud = Double.toString((double) Math.round((double) baudRate / 100) / 10) + " Kbps";
            } else {
                baud = Double.toString((double) Math.round((double) baudRate / 100000) / 10) + " Mbps"; // Two decimal places
            }
            System.out.println("\tMeasured Baud Rate: " + baud);

            if(baudRate >= MINIMUMBAUDRATE) {
                return true;
            }else {
                System.out.println("Baud rate is too slow; Discard stream.");
                return false;
            }
        } catch (IOException e) {
            return false;
        }
=======
>>>>>>> Stashed changes:src/main/java/org/satoshiware/keystickers/random/RPiHWRNG.java
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

    // Returns true if the selected source is a ".dat" file
    public boolean isFile() {
        return isFile;
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