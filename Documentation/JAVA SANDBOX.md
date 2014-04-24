The Java Sandbox
================
Introduction
------------
You do not need to read this file unless you are a developer.

The Java Sandbox allows for the secure execution of Java class files by restricting the environment
and available classes and actions.

Arguments
---------
The arguments expected:
 - 0:	The directory of class-files; the program will be allowed I/O to this directory (only).
 - 1:	The entry-point class.
 - 2:	The method to invoke; this must be static.
 - 3:	List of white-listed classes, or 0 for no white-listing.
 - 4:	Output mode (1 or 0) - outputs the value from the method.
 - 5:	Timeout before self-terminating.
 - 6:	Indicates if this is a sub-process.
 - 7-n:	The arguments for the method; this is automatically parsed.

Each argument should be *type=value*.

Accepted types: refer to ParsedArgument class, in the Java Sandbox code base.
