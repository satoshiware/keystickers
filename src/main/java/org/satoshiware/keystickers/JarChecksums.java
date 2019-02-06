/*
 *      This class checks the integrity of all the external jar dependencies.
 *      Each jar file is read, checksum (SHA-256) calculated, and then compared
 *      with the hardcoded values within this class (jarFiles() routine).
 *
 *******************************************************************************
 * Revision History:
 *      2018-11-09      Satoshiware
 *          Initial Release
 *
 *******************************************************************************
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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class JarChecksums {
    public static int FILESCHECKED = 21;

    public static boolean run(String filepath) {
        if(filepath == null) return false;

        System.out.println("\nVerifying the checksum (SHA-256) of each external library in the directory \"" + filepath + "\"\n");
        try {
            if ((new File(filepath)).list().length <= FILESCHECKED || (new File(filepath)).list().length > FILESCHECKED + 1) { // Increment FILESCHECKED by 1 to include the Keysticker jar file
                System.out.println("ERROR! Make sure to run Keystickers from the directory containing the jar files.");
                System.out.println("The directory being verified must only contain the " + Integer.toString(FILESCHECKED + 1) + " jar files required for this application.");
                return false;
            }
        } catch (NullPointerException e) {
            System.out.println("ERROR! " + "Directory \"" + filepath + "\" is not accessible!");
            return false;
        }

        boolean valid = true;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            for(int i = 1; i <= FILESCHECKED; i++) {
                if(jarFiles(i, "file") == null) {
                    System.out.println("ERROR! Incomplete information on the jar files!");
                    return false;
                }

                String chksm = checksum(filepath + System.getProperty("file.separator") + jarFiles(i, "file"), md);
                System.out.println(jarFiles(i, "file") + jarFiles(i, "spacing") + chksm + (chksm.equals(jarFiles(i, "checksum")) ? "\tValid    \t" : "\tNOT Valid\t") + jarFiles(i, "description"));
                valid &= chksm.equals(jarFiles(i, "checksum"));
            }
        }catch (IOException e) {
            System.out.println("\nERROR! IO exception accessing and reading the libraries!\n" + e.getMessage());
            return false;
        }catch (NoSuchAlgorithmException e) {
            System.out.println("ERROR! SHA-256 routine is not available!");
            return false;
        }

        if(valid) {
            return true;
        }else {
            System.out.println("\nERROR! Not all checksums match!");
            return false;
        }
    }

    private static String jarFiles(int index, String item) {
        String file;
        int spacing = 8;
        String checksum;
        String description;

        switch (index) {
            case 1:     file = "bitcoinj-core-0.14.7.jar";  spacing -= 4;   checksum = "e4f4962df63919194c58c628dec7464f626cd5776157f1e64a7441408eb507d8";  description = "org.bitcoinj.core 0.14.7 (Bitcoin implementation in java)"; break;
            case 2:     file = "slf4j-api-1.7.20.jar";      spacing += 0;   checksum = "2967c337180f6dca88a8a6140495b9f0b8a85b8527d02b0089bdbf9cdb34d40b";  description = "    -> BitcoinJ support file (1/10)"; break;
            case 3:     file = "scrypt-1.4.0.jar";          spacing += 4;   checksum = "9a82d218099fb14c10c0e86e7eefeebd8c104de920acdc47b8b4b7a686fb73b4";  description = "    -> BitcoinJ support file (2/10)"; break;
            case 4:     file = "protobuf-java-2.6.1.jar";   spacing -= 3;   checksum = "55aa554843983f431df5616112cf688d38aa17c132357afd1c109435bfdac4e6";  description = "    -> BitcoinJ support file (3/10)"; break;
            case 5:     file = "orchid-1.2.1.jar";          spacing += 4;   checksum = "f836325cfa0466a011cb755c9b0fee6368487a2352eb45f4306ad9e4c18de080";  description = "    -> BitcoinJ support file (4/10)"; break;
            case 6:     file = "okio-1.6.0.jar";            spacing += 6;   checksum = "114bdc1f47338a68bcbc95abf2f5cdc72beeec91812f2fcd7b521c1937876266";  description = "    -> BitcoinJ support file (5/10)"; break;
            case 7:     file = "okhttp-2.7.2.jar";          spacing += 4;   checksum = "b4c943138fcef2bcc9d2006b2250c4aabbedeafc5947ed7c0af7fd103ceb2707";  description = "    -> BitcoinJ support file (6/10)"; break;
            case 8:     file = "jsr305-2.0.1.jar";          spacing += 4;   checksum = "1e7f53fa5b8b5c807e986ba335665da03f18d660802d8bf061823089d1bee468";  description = "    -> BitcoinJ support file (7/10)"; break;
            case 9:     file = "jcip-annotations-1.0.jar";  spacing -= 4;   checksum = "be5805392060c71474bf6c9a67a099471274d30b83eef84bfc4e0889a4f1dcc0";  description = "    -> BitcoinJ support file (8/10)"; break;
            case 10:    file = "guava-18.0.jar";            spacing += 6;   checksum = "d664fbfc03d2e5ce9cab2a44fb01f1d0bf9dfebeccc1a473b1f9ea31f79f6f99";  description = "    -> BitcoinJ support file (9/10)"; break;
            case 11:    file = "core-1.51.0.0.jar";         spacing += 3;   checksum = "8d6240b974b0aca4d3da9c7dd44d42339d8a374358aca5fc98e50a995764511f";  description = "    -> BitcoinJ support file (10/10)"; break;
            case 12:    file = "core-3.3.3.jar";            spacing += 6;   checksum = "5820f81e943e4bce0329306621e2d6255d2930b0a6ce934c5c23c0d6d3f20599";  description = "com.google.zxing 3.3.3 (QR encoding)"; break;
            case 13:    file = "bccontrib-1.0.jar";         spacing += 3;   checksum = "cd5514068306262ce35c25122d75ddcfc406a672a0b41a8ef267201d94784cc3";  description = "org.jitsi.bccontrib 1.0 (Fortuna CSPRNG)"; break;
            case 14:    file = "bcprov-jdk15on-1.48.jar";   spacing -= 3;   checksum = "804b7e2e3b9ac771dfd3b43de16666ac6008f8600f48f28ddc94e39a114e2288";  description = "    -> Fortuna support file (1/1)"; break;
            case 15:    file = "slf4j-jdk14-1.7.12.jar";    spacing -= 2;   checksum = "a13362d9865740511ab35e4181e6821c4876d92fa770bfcc37e96086d924f75c";  description = "Simple Logging Facade for Java (SLF4J.org) JDK"; break;
            case 16:    file = "forms_rt-7.0.3.jar";        spacing -= 2;   checksum = "51d100f7f19e85bce8fd8fda551e433188174994f470d6e9f51c7f7b2227d9e3";  description = ""; break;
            case 17:    file = "asm-commons-3.0.jar";       spacing -= 2;   checksum = "32b516ca5738b87077afac83d08bae7e3f3cea4ce6ff6d7a18506b38c3729640";  description = ""; break;
            case 18:    file = "asm-tree-3.0.jar";          spacing -= 2;   checksum = "ad82cad09bb2f7752d4ddb8ebb2aaf893ecc4a801bcbe813c75027830f496255";  description = ""; break;
            case 19:    file = "asm-3.0.jar";               spacing -= 2;   checksum = "bfcb35900ec1d5a1346fb0dc91dd9d772cf93e3165cb8701aec3ed277b5a53ba";  description = ""; break;
            case 20:    file = "forms-1.1-preview.jar";     spacing -= 2;   checksum = "26b0fc745ea051b57462be22a150c7600dbac6716b24cc60a5ecc0e8085c41a0";  description = ""; break;
            case 21:    file = "jdom-1.0.jar";              spacing -= 2;   checksum = "3b23bc3979aec14a952a12aafc483010dc57579775f2ffcacef5256a90eeda02";  description = ""; break;
            default:    file = null;                        spacing = -1;   checksum = null;                                                                description = null; break;
        }

        if(item.equals("file")) {
            return file;
        }else if(item.equals("checksum")) {
            return checksum;
        }else if(item.equals("spacing")) {
            if (spacing == -1) return null;

            String s = "";
            for(int i = 0; i < spacing; i++){
                s += " ";
            }

            return s;
        }else if(item.equals("description")) {
            return description;
        }else {
            return null;
        }
    }

    private static String checksum(String filepath, MessageDigest md) throws IOException {
        // file hashing with DigestInputStream
        try (DigestInputStream dis = new DigestInputStream(new FileInputStream(filepath), md)) {
            while (dis.read() != -1) ; //empty loop to clear the data
            md = dis.getMessageDigest();
        }

        // bytes to hex
        StringBuilder result = new StringBuilder();
        for (byte b : md.digest()) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}