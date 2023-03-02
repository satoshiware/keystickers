This free and open source software was created to facilitate the <ins>production</ins> of Keystickers (and [Satoshi Coins](https://github.com/Satoshiware/satoshicoins)). This software is offered “as-is”, without warranty, and disclaiming liability for any damage resulting from its use.

## Keystickers: Bitcoin Wallets that Stick
Keystickers are single use Bitcoin wallets that adhere to the back of your business cards. Once your sticker sheets are printed, the private keys are covered with security holographic scratch-off stickers.

Sticker sheets can be ordered from [Online Labels](https://www.onlinelabels.com/products/ol4143GF) (Gold: [OL4143GF](https://www.onlinelabels.com/products/ol4143GF), White: [OL4143WX](https://www.onlinelabels.com/products/ol4143WX))

Security holographic scratch-off stickers can be purchased from [Integraf](https://www.integraf.com/) (Product#: 7931-SC)  

<img src="media/print.jpg" width="400"> <img src="media/white.jpg" width="412">

<img src="media/holo.jpg" width="456"> <img src="media/scratch.jpg" width="400">

For a more professional appeal and better security, use the GOLD stickers. Light is impenetrable from underneath!

<img src="media/gold.jpg" width="400">

## Specifications
The security holographic scratch-off stickers are 40.6 mm in diameter and 30 um thick. They have a tough plastic film that protects and separates the private key from the scratch surface. The [PDF specification](specs/Holographic.pdf) for Satoshiware's security holographic scratch-off stickers is part of this repository as well as a [video](media/video.mp4) of the master proof.

The Keysticker sheets are 8 <sup>1</sup>/<sub>2</sub>" x 11" and will fit any standard printer. The technical drawing is available within this repository ([PDF](specs/Keystickers.dwg.pdf) or [DWG](specs/Keystickers.dwg)). A simple wired monochrome laser printer (e.g. HP LaserJet Pro M402n) is highly recommended for the job. These sheets are thicker compared to standard paper (especially the gold sheets); adjust the printer accordingly to avoid the toner from flaking off. Also, minimize the toner thickness as much as possible; otherwise, the private key may be slightly discernible underneath the scratch-off sticker. Note: The Keysticker software can control the private key grayscale color to further mitigate this problem; just make sure the QR is still easily scannable.

Note: [OpenJDK](http://openjdk.java.net/) is recommended; it is the open source version of Java.

## Development
[IntelliJ IDEA Community](https://www.jetbrains.com/idea/) (2022.3.2) was used for the development of this project.

**Getting started with IntelliJ**

* File -> Open
   * Find and select the Keysticker's project from its root directory
 
* File -> Project Structure -> Project Settings -> Project
   * Select the appropriate "Project SDK" (e.g. openjdk-19.02)
   * Set "Language level" to 11
    
* Create Multiple Run configurations for Main.main()
   * Run -> Edit Configurations -> Add New Configuration ('+' Sign) -> Application
      * Name (examples): “Verification”, “Keystickers”, “Satoshi Coins”, or "Without Parameter"
      * Main class: "org.satoshiware.keystickers.Main"
      * Program arguments (matching their respective name): “-v”, “-k”, “-s”, or blank
         * Note: To run, click Run -> Run (Alt+Shift+F10) and then select the desired run configuration. Also, the external libraries must be added to the "Keysticker Project Directory"\lib in order for verifications ("-v") to work from within the IntelliJ IDE.

## Creating Jar File
Keystickers is compiled with Maven. Visit [Maven's web site](https://maven.apache.org/) for additional information. IntelliJ IDE recognizes the Maven pom.xml file located in the project directory and will adjust its settings accordingly when opening the project; however, in order to build with Maven from within IntelliJ, delegate the IDE build to Maven as follows:

   * File -> Settings -> Build, Execution, Deployment -> Built Tools -> Maven -> Runner
      * Enable "Delegate IDE build/run actions to Maven"

After the next build, the jar file and its dependencies (/lib directory) can be found in the project folder.
   
## Running
After downloading the latest release, extract the contents of the file “keystickers-$REV.(zip|tar.gz)”. From the shell (or command prompt), move to the new Keysticker directory and execute “java -jar keystickers-$REV.jar”. You will be prompted to run the command again with one of the following parameters: -v, -k, or -s. (e.g. “java -jar keystickers-$REV.jar -k”). As soon as the printing starts, watch for a GUI Print Dialog Box (provided by the java.awt.print class) that may sometimes appear in the background. There were no print classes native to Java that would work without a GUI; therefore, make sure this is running on an OS with a GUI.

## External Dependencies
External libraries are not compiled into the main program. Use the "-v" switch at run time to calculate and cross-check their SHA256 checksums. This will ensure they have not been compromised. This will also do a quick sanity check on the RNGs (Random Number Generators). At the end of this verification, SHA-256 checksums for the Open JDK compressed install files are displayed. They can be used to verify those files that were used to install Java.

## Proof of Existence
[Proof of Existence](https://www.ProofOfExistence.com) (POEX) protocol is used to store the keystickers-$REV.jar checksum for each new Keysticker [release](https://github.com/satoshiware/keystickers/releases). This may prove to be a quicker and simpler way for many to verify the legitimacy of their precompiled download after it has become sufficiently battle tested by competent and trusted users. Look for the ProofOfExistence.txt text file, located in either the zip or tar.gz compressed files, for the POEX information.
