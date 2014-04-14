PALS: Programming Assessment and Learning System
================================================
A third-generation programming assessment system, to provide the assessment and learning of programming languages through formative and summative exercises.

Features
--------
- Assessment of the Java programming language:
  * Testing parameters/output of methods using primitives, including arrays.
  * Testing standard input and output.
  * Code metrics (lines of code, blank lines, comment lines, average identifier length).
  * Testing the existence of classes, methods and fields.
  * Testing enum constants.
  * Testing inheritence and implemented interfaces.
  * Testing using regular expressions.
- Written and multiple choice/response questions (MCQ/MRQ).
- Multiple platforms:
  * Windows (tested on 7, 8 & 2008).
  * Linux (tested on Arch 2013+).
- Secure execution of dynamic analyzed programs, using sandboxing and a different user account.
  * Allows restriction of I/O to a temporary directory.
  * Disables advanced features of Java, such as sockets and reflection, to avoid exposing the host machine.
- A centralized pool of questions for reuse.
- Users enrolled on modules, which have assignments.
- Collection of statistics for exceptions during dynamic analysis and compilation.
  * Useful for assisting with general problems and altering course material.
- Mass enrollment and addition of users to the system.
- Clustering:
  * Ability to distribute work across multiple nodes.
  * Allows for higher throughput during heavy periods.
  * Allows the system to continue when a node fails.
- Plugin architecture:
  * Modular components of the system can be changed overtime.
  * Plugins can be added and reloaded during runtime, without the need to reboot.
  * Allows future support for other programming languages, and other features or forms of assessment.

License
-------
This project is distributed under a MIT License, with exception to */Third-Party Libraries*.

Refer to *LICENSE* for full license.

Documentation
-------------
Refer to */Documentation*, where you will find general documentation files and JavaDoc.

File Structure
--------------
- */Base* - the base framework for PALS, such as: creating instances of nodes, assessment models and helper classes.
- */Documentation* - contains documentation on PALS.
- */JavaSandbox* - the sandbox application used for executing Java programs within a restricted, sandboxed, environment.
- */Node* - a simple node application used to launch an instance of PALS, with support for plugin development.
- */Plugins* - contains all of the officially supported and maintained plugins.
- */Shared Storage* - the default shared storage configuration. If this is moved, you will need to copy the sub-files.
- */Third-Party Libraries* - any third-party libraries used for PALS.
- */Website* - the website used to forward web-requests to nodes. Actual web content is processed by nodes, the website simply acts as a bridge between the nodes and the user.
- */WindowsUserTool* - a simple tool, for Windows, to execute a program under a different account.

Unit Tests
----------
The base has been mostly unit tested, with the test files located at:
*/Node/test*

Third-Party Libraries
---------------------
- JavaMail 1.4.7 - used to send and receive e-mail messages.
- JUnit 4 - used for unit testing.
- Commons:
  * Codec 1.8 - used for base64 encoding.
  * Compress 1.7 - used for ZIP archive compression.
  * File Upload 1.3 - used to process file-uploads, as an alternative to the default Java EE library.
  * IO 2.4 - used for extended file I/O operations, such as copying directories.
- FreeMarker 2.3 - template engine, used as an alternative to JavaServer Pages. This allows template rendering for e-mails, as well as web.
- Java Servlets 3.0.0 - used by the portable Jetty webserver plugin.
- Jetty 9.1.1 - used for creating a portable webserver plugin.
- Joda 2.3 - a date-time alternative to the Java API.
- MySQL 5.1 - allows MySQL database connectivity. Currently not supported, but this has been partially tested.
- PostgreSQL 9.2 - used for database connectivity to a Postgres DBMS.

Authors
-------
Marcus Craske - limpygnome@gmail.com

