/*
    The MIT License (MIT)

    Copyright (c) 2014 Marcus Craske <limpygnome@gmail.com>

    Permission is hereby granted, free of charge, to any person obtaining a copy
    of this software and associated documentation files (the "Software"), to deal
    in the Software without restriction, including without limitation the rights
    to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
    copies of the Software, and to permit persons to whom the Software is
    furnished to do so, subject to the following conditions:

    The above copyright notice and this permission notice shall be included in
    all copies or substantial portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
    THE SOFTWARE.
    ----------------------------------------------------------------------------
    Version:    1.0
    Authors:    Marcus Craske           <limpygnome@gmail.com>
    ----------------------------------------------------------------------------
*/
package pals.base;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.EnumSet;
import org.joda.time.DateTime;

/**
 * Used to log the runtime of PALS.
 * 
 * Messages/entries can span across multiple lines, with \EOE\ at the end of an
 * entry.
 * 
 * Thread-safe.
 */
public class Logging 
{
    // Enums *******************************************************************
    /**
     * The type of log entry being made; allows different priorities of
     * messages regarding the system.
     */
    public enum EntryType
    {
        /**
         * A log entry which is general information.
         */
        Info,
        /**
         * A log entry which is a warning.
         */
        Warning,
        /**
         * A log entry about a critical error.
         */
        Error;
        /**
         * Allows a comma-separated list of types to be parsed into an enum-set
         * for this type.
         * 
         * @param data Comma-separated list; can be empty or null. Can be value
         * "All" for all types.
         * @return Representation of that list as an EnumSet; returns null if
         * parsing error.
         */
        public static EnumSet<EntryType> getSet(String data)
        {
            // Check if to specify all or none types
            if(data == null || data.length() == 0)
                return EnumSet.noneOf(EntryType.class);
            else if(data.toLowerCase().equals("all"))
                return EnumSet.allOf(EntryType.class);
            // Parse the types
            EnumSet<EntryType> types = EnumSet.noneOf(EntryType.class);
            String[] parts = data.split(",");
            for(String part : parts)
            {
                if(part.length() > 0)
                {
                    try
                    {
                        types.add(Enum.valueOf(EntryType.class, part));
                    }
                    catch(IllegalArgumentException ex)
                    {
                        return null;
                    }
                }
            }
            return types;
        }
    }
    // Fields - Constants ******************************************************
    /**
     * Maximum length of a logging alias, used to identify the source of a
     * logging entry.
     */
    private final static int    ALIAS_MAX_LENGTH    = 16;
    // Fields ******************************************************************
    private NodeCore            core;           // The current instance of the core.
    private final String        alias;          // The name for the current log-file.
    private PrintWriter         pw;             // The stream to the current log-file.
    private DateTime            logDt;          // The current day of the current log-file, used for deciding if to switch logs when the day changes.
    private boolean             stackTraces;    // Indicates if to log stack-traces of exceptions.
    private EnumSet<EntryType>  typesLogged;    // The types of errors logged.
    // Methods - Constructors **************************************************
    /**
     * Creates a new instance for logging system events.
     * 
     * @param core The current instance of the core.
     * @param alias The alias/name of the file.
     * @param stackTraces Indicates if to log stack-traces.
     * @param typesLogged The type of events to log; use bit-wise OR for multiple
     * types.
     */
    private Logging(NodeCore core, String alias, boolean stackTraces, EnumSet<EntryType> typesLogged)
    {
        this.core = core;
        this.alias = alias;
        this.pw = null;
        this.logDt = DateTime.now();
        this.stackTraces = stackTraces;
        this.typesLogged = typesLogged;
    }
    // Methods *****************************************************************
    /**
     * Logs a new exception.
     * 
     * @param alias The name of the component producing the message.
     * @param ex The exception which has occurred.
     * @param et The log entry type.
     */
    public synchronized void logEx(String alias, Throwable ex, EntryType et)
    {
        logEx(alias, null, ex, et);
    }
    /**
     * Logs a new exception.
     * 
     * @param alias The name of the component producing the message.
     * @param message The message to append with the exception, can be null.
     * @param ex The exception which has occurred.
     * @param et The log entry type.
     */
    public synchronized void logEx(String alias, String message, Throwable ex, EntryType et)
    {
        Throwable cause = ex.getCause();
        StringBuilder sb = new StringBuilder();
        // Append message
        if(message != null)
            sb.append(message).append(" - ");
        // Append exception
        sb.append("Exception: '").append(ex.getMessage()).append("'; cause: ");
        if(cause != null)
            sb.append("'").append(cause.getMessage()).append("'");
        else
            sb.append("[unknown].");
        // Append stack-trace (if enabled)
        if(stackTraces)
        {
            StringWriter s = new StringWriter();
            PrintWriter p = new PrintWriter(s);
            ex.printStackTrace(p);
            sb.append(" Stack-trace: '").append(s.toString()).append("'.");
        }
        // Log the message
        log(alias, sb.toString(), et);
    }
    /**
     * Logs a message.
     * 
     * @param alias The name of the component producing the message.
     * @param message The message to be logged.
     * @param et The log entry type.
     */
    public synchronized void log(String alias, String message, EntryType et)
    {
        // Check the alias is valid
        if(alias == null || alias.length() == 0)
            return;
        // Check we log the type of event
        if(!typesLogged.contains(et))
            return;
        // Verify the message is not null, else ignore...
        else if(message == null)
            return;
        // Check if the day has changed
        DateTime dt = DateTime.now();
        if(pw == null || (dt.getYear() != logDt.getYear() || dt.getMonthOfYear() != logDt.getMonthOfYear() || dt.getDayOfMonth()!= logDt.getDayOfMonth()))
        {
            this.logDt = dt;
            switchLogFile();
        }
        String logEntry;
        String logPrint;
        {
            StringBuilder sb = new StringBuilder();
            // Write the type of incident
            switch(et)
            {
                case Error:
                    sb.append("ERROR\t"); break;
                case Info:
                    sb.append("INFO\t"); break;
                case Warning:
                    sb.append("WARNING\t"); break;
            }
            // Write the date
            sb.append(String.format("%04d-%02d-%02d %02d:%02d:%02d", dt.getYear(), dt.getMonthOfYear(), dt.getDayOfMonth(), dt.getHourOfDay(), dt.getMinuteOfHour(), dt.getSecondOfMinute()));
            sb.append("\t");
            // Write the alias
            sb.append(padRestrictAlias(alias)).append("\t");
            // Write the message -- escape EOR end tag too!
            sb.append(message.replace("\\EOE\\", "\\//EOE\\//"));
            logPrint = sb.toString();
            sb.append("\\EOE\\");
            logEntry = sb.toString();
        }
        // Output to console and the log-file
        if(et == EntryType.Error)
            System.err.println(logPrint);
        else
            System.out.println(logPrint);
        pw.println(logEntry);
        pw.flush();
    }
    private static String padRestrictAlias(String alias)
    {
        // Ensure the alias is not too long
        if(alias.length() > ALIAS_MAX_LENGTH)
            return alias.substring(0, ALIAS_MAX_LENGTH);
        else
        {
            // Padd empty-space with spaces
            return String.format("%1$-"+ALIAS_MAX_LENGTH+"s", alias);
        }
    }
    private synchronized boolean switchLogFile()
    {
        // Dispose the current log-file
        if(pw != null)
        {
            pw.flush();
            pw.close();
        }
        try
        {
            String folder = Storage.getPath_logs(core.getPathShared()) + "/" + core.getNodeUUID().getHexHyphens();
            // Check the logs folder exists, else create it
            File dir = new File(folder);
            if(!dir.exists())
                dir.mkdir();
            // Build the log path
            // Note: %0x = x padding of digit
            String path = String.format("%s/%s_%04d_%02d_%02d.log", folder, alias, logDt.getYear(), logDt.getMonthOfYear(), logDt.getDayOfMonth());
            // Open new log-file - set to append data too!
            pw = new PrintWriter(new BufferedWriter(new FileWriter(new File(path), true)));
            return true;
        }
        catch(FileNotFoundException ex)
        {
            System.err.println("Failed to create logging instance ~ file exception ~ " + ex.getMessage() + "!");
        }
        catch(IOException ex)   
        {
            System.err.println("Failed to create logging instance ~ IO exception ~ " + ex.getMessage() + "!");
        }
        return false;
    }
    /**
     * Disposes this instance of logging; important to release the file used for
     * logging.
     */
    public synchronized void dispose()
    {
        if(pw != null)
        {
            pw.flush();
            pw.close();
            pw = null;
        }
    }
    // Methods - Static ********************************************************
    /**
     * Creates a new instance of the logger for PALS.
     * 
     * WARNING: only one instance should exist for the specified name, else
     * another logging instance will not be able to access the file.
     * 
     * @param core The current instance of the core.
     * @param alias The name of the logging file; the date of the current day
     * is also appended.
     * @param stackTraces Indicates if to log stack-traces.
     * @param typesLogged The type of events logged.
     * 
     * @return Instance or null if an error occurred.
     */
    public static Logging createInstance(NodeCore core, String alias, boolean stackTraces, EnumSet<EntryType> typesLogged)
    {
        Logging l = new Logging(core, alias, stackTraces, typesLogged);
        if(!l.switchLogFile())
        {
            l.dispose();
            return null;
        }
        else
            return l;
    }
    // Methods - Mutators ******************************************************
    /**
     * @param enabled Sets if stack-traces are logged with exceptions.
     */
    public void setStackTraces(boolean enabled)
    {
        this.stackTraces = enabled;
    }
}
