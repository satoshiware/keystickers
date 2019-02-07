# **Keystickers – Bitcoin Wallets That Stick**
The open source software presented here facilitates the production of Keystickers. More information can be found here: [Satoshiware.org/Keystickers](http://www.Satoshiware.org/Keystickers). This software is offered “as-is”, without warranty, and disclaiming liability for any damage resulting from its use.

It is highly recommended that each user audit and compile the source code for themselves. The goal should be to eliminate as many _trust vectors_ as possible.

## Word of Caution
The manufacture and distribution of Bitcoin wallets can be very rewarding when done properly. Your recipients will be empowered to control the keys to their own bitcoins; however, their dependence upon your integrity and your competence requires serious consideration. Failure to follow security precautions may lead to undesirable consequences. The more time and prudence invested into your own Keysticker setup will set the tone for the service you provide. Please visit the [wiki](https://github.com/Satoshiware/Keystickers/wiki/Secure-Setup) for additional information regarding secure setups.

## Built With
-   [OpenJDK](http://openjdk.java.net/) is the open source version of Java. Open source software promotes innovation and security while discouraging foul play. See [Ad](https://adoptopenjdk.net/)[opt](https://adoptopenjdk.net/)[O](https://adoptopenjdk.net/)[pen](https://adoptopenjdk.net/)[JDK](https://adoptopenjdk.net/)[.net](https://adoptopenjdk.net/) for install instructions.

-   Compiled with [Maven](https://maven.apache.org/) for a simplified build process that's easy to duplicate.

-   Java GUI programs are fairly dependent on the IDE used to create them. Keystickers uses [IntelliJ IDEA Community](https://www.jetbrains.com/idea/). It is free and open source.

## Running the Software
After a successful OpenJDK install, download the [latest Keystickers' release](https://github.com/Satoshiware/Keystickers/releases) from GitHub and verify the SHA256 checksum. Cross-check the checksum value from a reliable source. Unzip|Extract the download and then unzip|extract the file "RunKeystickers-_revision_.(zip|tar.gz)". The unpacked RunKeystickers-_revision_ folder contains everything needed to run the software; move it to a desired location.

From the command shell/prompt, change the directory to the RunKeystickers-_revision_ folder and execute the command **java -jar keystickers-**_**revision**_**.jar -v**  Note: the "-s" switch can be used instead of "-v" to skip the verification at startup of the external jar files (not recommended for production).

For more information on using the software, vist the [Software Overview](https://github.com/Satoshiware/Keystickers/wiki/Software-Overview) in the wiki. The wiki also includes a section for setting up [Hardware RNGs](https://github.com/Satoshiware/Keystickers/wiki/Hardware-RNGs) for additional entropy and redundancy.

## Compiling
Keystickers is compiled with Maven. This makes the process of compilation identical to other maven projects. The pom.xml file contains all the details unique to this project for a successful automated build. Visit [Maven's web site](https://maven.apache.org/) for additional information on downloading, installing, and running Maven.
  
## Development
Any Java IDE is sufficient for over viewing and improving the code; however, for GUI specific enhancements, the IntelliJ IDE is required. Java GUIs are fairly dependent on their respective IDEs. In this repository, there is a directory named ".idea" that contains IntelliJ settings for successfully loading the Keystickers' source code. Visit the wiki, [IntelliJ IDE Setup](https://github.com/Satoshiware/Keystickers/wiki/IntelliJ-IDE-Setup), for initial opening and configuring the source code with IntelliJ.

## Auditing
The more eyes on this project, the better it will be for everyone. If you have a GitHub account, be sure to [watch](https://help.github.com/articles/watching-and-unwatching-repositories/) this repository. If you have the capacity to audit Java code, consider becoming a contributor and/or reviewer today. A good audit begins with a sound understanding of how all the pieces of code interoperate. Visit the [Code Overview](https://github.com/Satoshiware/Keystickers/wiki/Code-Overview) in the wiki for a quick jump start into the details of the Keystickers' source code.

## External Dependencies
Listed below are the external libraries used for this software. These external libraries are not compiled into the main program. Use the "-v" program argument at run time to calculate and cross-check their SHA256 checksums. This will ensure they have not been compromised.

**org.bitcoinj:bitcoinj-core**
_bitcoinj_ is the Java library for working with the _Bitcoin_ protocol. It is used to generate public keys  from
the private keys and to encode them (private & public) in the proper format.
-   Link: [https://bitcoinj.github.io](https://bitcoinj.github.io/)
-   Source: [https://github.com/bitcoinj/bitcoinj](https://github.com/bitcoinj/bitcoinj)
-   JAR: [https://mvnrepository.com/artifact/org.bitcoinj/bitcoinj-core](https://mvnrepository.com/artifact/org.bitcoinj/bitcoinj-core)

**org.jitsi:bccontrib**
Java implementation of the Fortuna random number generator. Used as an additional source of randomness to increase robustness for generating private keys.
-   Link: [https://jitsi.org](https://jitsi.org/)
-   Source: [https://github.com/jitsi/bccontrib](https://github.com/jitsi/bccontrib)
-   JAR: [https://mvnrepository.com/artifact/org.jitsi/bccontrib](https://mvnrepository.com/artifact/org.jitsi/bccontrib)

**com.google.zxing:core**
Zxing is a barcode image processing library implemented in Java. It is used to encode the public and private keys as QR codes.
-   Link: [https://opensource.google.com/projects/zxing](https://opensource.google.com/projects/zxing)
-   Source: [https://github.com/zxing/zxing](https://github.com/zxing/zxing)
-   JAR: [https://mvnrepository.com/artifact/com.google.zxing/core](https://mvnrepository.com/artifact/com.google.zxing/core)

**com.intellij:forms_rt**
Jetbrains UiDesigner. This library is included in the pom.xml for a successful GUI build using Maven. It is supplied by default when compiling/running from the IntelliJ IDE.
-   Link: [https://www.jetbrains.com/idea](https://www.jetbrains.com/idea/)
-   Source: [https://github.com/JetBrains/intellij-community/tree/master/platform/forms_rt](https://github.com/JetBrains/intellij-community/tree/master/platform/forms_rt)
-   JAR: [https://mvnrepository.com/artifact/com.intellij/forms_rt](https://mvnrepository.com/artifact/com.intellij/forms_rt)

**org.slf4j:slf4j-jdk14**
The Simple Logging Facade for Java (SLF4J) serves as an abstraction for various logging frameworks.
-   Link: [https://www.slf4j.org](https://www.slf4j.org/)
-   Source: [https://github.com/qos-ch/slf4j](https://github.com/qos-ch/slf4j)
-   JAR: [https://mvnrepository.com/artifact/org.slf4j/slf4j-jdk14](https://mvnrepository.com/artifact/org.slf4j/slf4j-jdk14)

## Contributing
This project could sure use better documentation, more testing, and multi-lingual support. If you find value in this project and have some skills available, please consider helping out. See [CONTRIBUTING.md](https://github.com/Satoshiware/Keystickers/blob/master/CONTRIBUTING.md) for details on our code of conduct and the process for submitting pull requests.

## Versioning
We use [SemVer](https://semver.org/) for versioning. For the versions available, see the [tags on this repository](https://github.com/Satoshiware/Keystickers/tags).

## Authors
See list of [contributors](https://github.com/Satoshiware/Keystickers/graphs/contributors) who participated in this project.

## License
This project is licensed under version 3 of the GNU General Public License - see [LICENSE](https://github.com/Satoshiware/Keystickers/blob/master/LICENSE) for details.

## Acknowledgments
-   BitcoinJ Team ([BitcoinJ.GitHub.io](https://bitcoinj.github.io/))
-   Mike Caldwell ([Casascius Coin Creator](https://en.bitcoin.it/wiki/Casascius_physical_bitcoins))
