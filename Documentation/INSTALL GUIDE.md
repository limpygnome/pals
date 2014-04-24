Installation Guide
==================
Basic Setup
-----------
To setup a new system, you will first need to fulfill the requirements for the target
platform, by reading either of the following in the current documentation directory:

- **REQUIREMENTS - LINUX.md**
- **REQUIREMENTS - WINDOWS.md**

Next you should navigate to /Builds. You will need a base installation for a node.
This will be a sub-directory. However, if no sub-directory exists, you can run
either **build_linux.sh** or **build_windows.bat** (depending on your current
platform) to build a new base installation.

Now copy the files inside the build sub-directory to the desired destination directory
of where you want your node to reside.

You will need to configure the node, by modifying the file *node.config* in the *_config*
directory. Configure any required settings, such as: database, e-mail, UUID for node
and/or node title.

Once the node has been configured, you can launch PALS by running the following from
the destination directory:

- **launch_linux_headless.sh**
- **launch_windows.bat**

Running an instance of a node will automatically install the database. If you have the
embedded Jetty server plugin, included by default, you can now navigate to the
following URL to interact with PALS:

*http://localhost:8084*

The default login is **admin* for both the username and password.

Changing the Default Java Keystore (CRITICAL)
-----------------------------------------
The Java Keystore (JKS) holds the certificates required for SSL connections between nodes and
the website. By default, a JKS is provided, but it is critical this is changed, since
this JKS is public and kept for testing purposes. FAILING TO CHANGE THE JKS COULD RESULT IN
MAN-IN-THE-MIDDLE ATTACKS AND THE ENTIRE SYSTEM BECOMING COMPROMISED.

You will first need to generate a new JKS, by navigating to your Java Development Kit
installation, navigating to the **bin** directory and running the following:

**keytool -genkey -v -keyalg RSA -keystore pals.jks**

This will generate the file **pals.jks** within the directory, which is the JKS file. This will
need to be copied to the following relative locations of your node installation:

- **/web/WEB-INF/pals.jks**
- **pals.jks**

Website - Apache Tomcat
-----------------------
To run the website from an Apache Tomcat web server, you will first need to copy the web
files. In builds of PALS, or within the directory of a node, you will find the directory
*web*. This directory contains all the files required for operating a website.

This directory will need to be copied to the **webapps** directory of your Apache Tomcat
installation, and configured accordingly. If the website is located on a different machine
to the localhost, or the RMI port changes, you will need to change this information. This
can be changed by modifying the configuration file, in the **web** directory, at:

**WEB-INF/web.config**

Adding and Removing Nodes
-------------------------
Nodes can be easily added by copying the same files, of an existing node, or using similar
settings. Only the UUID of the node (**node/uuid**) needs changing, but it is also recommended
you change the title (**node/title**), by modifying the node configuration file at:

*/_config/node.config*

When the node is started, it will automatically add its details to the database, which will
allow other nodes to communicate with it.

However, both nodes must have access to the same shared storage. This can be achieved by setting
up a shared directory, using something e.g. a Windows file share or Samba. The path of the
shared directory must then be specified, in the **storage/path** key of the node configuration
file. If you are on Linux and using Samba, this must be the path of the local system directory,
with the file share mounted at the directory.

The plugins directory can also be shared, which can be configured by the key **plugins/path**
in the node configuration file. Plugins are copied from the specified directory each time a node
starts, which will avoid expensive I/O calls and file-lock issues.

A node can later be removed by visiting PALS, visiting the **admin** area and going to the
**nodes** section.

