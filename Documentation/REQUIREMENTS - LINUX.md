Requirements - Linux
====================
This has been tested on 32-bit and 64-bit distributions of Arch; if the system is not running within a GUI environment,
the node base will need to be launched with headless mode for graphics rendering (as used by the captcha plugin):
http://www.oracle.com/technetwork/articles/javase/headless-136834.html

In default builds, the file **launch_linux_headless.sh** can be used to launch an instance in headless mode.
Otherwise, simply append '-Djava.awt.headless=true' as a parameter when launching the JVM.

Packages
--------
The following packages are required:
- fontconfig
- xorg-server
- ttf-dejavu
- sshpass

You will also need both the JRE and JDK installed:
- jre-openjdk
- jdk-openjdk

Refer to your package manager for name specifics.

Account
-------
All execution occurs under a different account, which will need to be created. The account should be a very low-privledged
user, of which will be used to run student programs. By default, the user *test*, with the password *test*, is used.
Modify your node configuration file to change either of the credentials. The recommended way of creating new
users is the following:

- useradd test
- passwd test (enter password when asked)
- mkdir /home/test
- chown -R test /home/test
- chmod -R 777 /home/test

Since SSH is used to execute programs under a different account, failing to create the home directory correctly will be
outputted in any executed programs.
