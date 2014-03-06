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
package pals.plugins.stats;

import java.util.ArrayList;
import pals.base.assessment.Assignment;
import pals.base.assessment.Module;
import pals.base.assessment.Question;
import pals.base.database.Connector;
import pals.base.database.DatabaseException;
import pals.base.database.Result;

/**
 * Fetches the class-name of classes and their frequencies; ordered by
 * frequency descending.
 */
public class ModelExceptionClass
{
    // Fields ******************************************************************
    private int     ecid;
    private long    frequency;
    private String  className,
                    hint;
    private boolean runtime;
    // Enums *******************************************************************
    public enum LoadRemoveFilter
    {
        None,
        FilterCompileTime,
        FilterRuntime;
        
        public static LoadRemoveFilter parse(String filter)
        {
            if(filter == null)
                return ModelExceptionClass.LoadRemoveFilter.None;
            switch(filter)
            {
                case "0":
                    return ModelExceptionClass.LoadRemoveFilter.FilterCompileTime;
                case "1":
                    return ModelExceptionClass.LoadRemoveFilter.FilterRuntime;
                default:
                    return ModelExceptionClass.LoadRemoveFilter.None;
            }
        }
    }
    // Methods - Constructors **************************************************
    /**
     * Used to create an empty model for static calls from template system.
     */
    public ModelExceptionClass()
    {
        this(-1, -1, null, null, false);
    }
    private ModelExceptionClass(int ecid, long frequency, String className, String hint, boolean runtime)
    {
        this.ecid = ecid;
        this.frequency = frequency;
        this.className = className;
        this.hint = hint;
        this.runtime = runtime;
    }
    // Methods - Persistence - Loading *****************************************
    /**
     * @param conn Database connector.
     * @param lf The load-filter applied.
     * @return Array-list of models; can be empty.
     */
    public static ModelExceptionClass[] load(Connector conn, LoadRemoveFilter lf)
    {
        try
        {
            if(lf == LoadRemoveFilter.None)
                return load(conn, conn.read("SELECT ec.ecid, ec.class_name, ec.hint, ec.runtime, COUNT(e.ecid) AS freq FROM pals_exception_classes AS ec, pals_exceptions AS e WHERE ec.ecid=e.ecid GROUP BY ec.ecid ORDER BY freq DESC;"));
            else
                return load(conn, conn.read("SELECT ec.ecid, ec.class_name, ec.hint, ec.runtime, COUNT(e.ecid) AS freq FROM pals_exception_classes AS ec, pals_exceptions AS e WHERE ec.ecid=e.ecid AND ec.runtime=? GROUP BY ec.ecid ORDER BY freq DESC;", lf == LoadRemoveFilter.FilterRuntime ? "1" : "0"));
        }
        catch(DatabaseException ex)
        {
            return new ModelExceptionClass[0];
        }
    }
    /**
     * @param conn Database connector.
     * @param module The module of errors, and their frequencies, to fetch.
     * @param lf The load-filter applied.
     * @return Array-list of models; can be empty.
     */
    public static ModelExceptionClass[] load(Connector conn, Module module, LoadRemoveFilter lf)
    {
        try
        {
            if(lf == LoadRemoveFilter.None)
                return load(conn, conn.read("SELECT ec.ecid, ec.class_name, ec.hint, ec.runtime, COUNT(e.ecid) AS freq FROM pals_exception_classes AS ec, pals_exceptions AS e, pals_assignment_instance AS ai, pals_assignment AS a WHERE e.ecid=ec.ecid AND ai.aiid=e.aiid AND a.assid=ai.assid AND a.moduleid=? GROUP BY ec.ecid ORDER BY freq DESC;", module.getModuleID()));
            else
                return load(conn, conn.read("SELECT ec.ecid, ec.class_name, ec.hint, ec.runtime, COUNT(e.ecid) AS freq FROM pals_exception_classes AS ec, pals_exceptions AS e, pals_assignment_instance AS ai, pals_assignment AS a WHERE e.ecid=ec.ecid AND ec.runtime=? AND ai.aiid=e.aiid AND a.assid=ai.assid AND a.moduleid=? GROUP BY ec.ecid ORDER BY freq DESC;", lf == LoadRemoveFilter.FilterRuntime ? "1" : "0", module.getModuleID()));
        }
        catch(DatabaseException ex)
        {
            return new ModelExceptionClass[0];
        }
    }
    /**
     * @param conn Database connector.
     * @param ass The assignment of errors, and their frequencies, to fetch.
     * @param lf The load-filter applied.
     * @return Array-list of models; can be empty.
     */
    public static ModelExceptionClass[] load(Connector conn, Assignment ass, LoadRemoveFilter lf)
    {
        try
        {
            if(lf == LoadRemoveFilter.None)
                return load(conn, conn.read("SELECT ec.ecid, ec.class_name, ec.hint, ec.runtime, COUNT(e.ecid) AS freq FROM pals_exception_classes AS ec, pals_exceptions AS e, pals_assignment_instance AS ai WHERE e.ecid=ec.ecid AND ai.aiid=e.aiid AND ai.assid=? GROUP BY ec.ecid ORDER BY freq DESC;", ass.getAssID()));
            else
                return load(conn, conn.read("SELECT ec.ecid, ec.class_name, ec.hint, ec.runtime, COUNT(e.ecid) AS freq FROM pals_exception_classes AS ec, pals_exceptions AS e, pals_assignment_instance AS ai WHERE e.ecid=ec.ecid AND ec.runtime=? AND ai.aiid=e.aiid AND ai.assid=? GROUP BY ec.ecid ORDER BY freq DESC;", lf == LoadRemoveFilter.FilterRuntime ? "1" : "0", ass.getAssID()));
        }
        catch(DatabaseException ex)
        {
            return new ModelExceptionClass[0];
        }
    }
    /**
     * @param conn Database connector.
     * @param q The question of errors, and their frequencies, to fetch.
     * @param lf The load-filter applied.
     * @return Array-list of models; can be empty.
     */
    public static ModelExceptionClass[] load(Connector conn, Question q, LoadRemoveFilter lf)
    {
        try
        {
            if(lf == LoadRemoveFilter.None)
                return load(conn, conn.read("SELECT ec.ecid, ec.class_name, ec.hint, ec.runtime, COUNT(e.ecid) AS freq FROM pals_exception_classes AS ec, pals_exceptions AS e, pals_assignment_questions AS aq WHERE e.ecid=ec.ecid AND aq.aqid=e.aqid AND aq.qid=? GROUP BY ec.ecid ORDER BY freq DESC;", q.getQID()));
            else
                return load(conn, conn.read("SELECT ec.ecid, ec.class_name, ec.hint, ec.runtime, COUNT(e.ecid) AS freq FROM pals_exception_classes AS ec, pals_exceptions AS e, pals_assignment_questions AS aq WHERE e.ecid=ec.ecid AND ec.runtime=? AND aq.aqid=e.aqid AND aq.qid=? GROUP BY ec.ecid ORDER BY freq DESC;", lf == LoadRemoveFilter.FilterRuntime ? "1" : "0", q.getQID()));
        }
        catch(DatabaseException ex)
        {
            return new ModelExceptionClass[0];
        }
    }
    /**
     * @param conn Database connector.
     * @param res Result from a query; next() should be invoked.
     * @return Array-list of models; can be empty.
     */
    public static ModelExceptionClass[] load(Connector conn, Result res)
    {
        try
        {
            ArrayList<ModelExceptionClass> buff = new ArrayList<>();
            ModelExceptionClass t;
            while(res.next())
            {
                if((t = loadSingle(conn, res)) != null)
                    buff.add(t);
                else
                    System.err.println("null");
            }
            return buff.toArray(new ModelExceptionClass[buff.size()]);
        }
        catch(DatabaseException ex)
        {
            return new ModelExceptionClass[0];
        }
    }
    /**
     * @param conn Database connector.
     * @param res Result from a query; next() should be invoked; a frequency,
     * to represent the number of items, can be optionally present in the query.
     * @return Instance of model or null.
     */
    public static ModelExceptionClass loadSingle(Connector conn, Result res)
    {
        try
        {
            boolean containsFreq = res.contains("freq");
            return new ModelExceptionClass((int)res.get("ecid"), containsFreq ? (long)res.get("freq") : 0, (String)res.get("class_name"), (String)res.get("hint"), ((String)res.get("runtime")).equals("1"));
        }
        catch(DatabaseException ex)
        {
            return null;
        }
    }
    /**
     * @param conn Database connector.
     * @param ecid The identifier of the exception class.
     * @return Instance of model or null; if an instance is returned,
     * the frequency field will always be zero.
     */
    public static ModelExceptionClass loadSingle(Connector conn, int ecid)
    {
        try
        {
            Result res = conn.read("SELECT ecid, class_name, hint, runtime FROM pals_exception_classes WHERE ecid=?;", ecid);
            return res.next() ? loadSingle(conn, res) : null;
        }
        catch(DatabaseException ex)
        {
            return null;
        }
    }
    /**
     * Persists a hint for an exception-class.
     * 
     * @param conn Database connector.
     * @param hint The hint data; can be null or empty.
     * @param ecid The identifier of the exception class.
     * @return True = successful, false = failed.
     */
    public static boolean persistHint(Connector conn, String hint, int ecid)
    {
        try
        {
            conn.execute("UPDATE pals_exception_classes SET hint=? WHERE ecid=?;", hint == null || hint.length() == 0 ? null : hint, ecid);
            return true;
        }
        catch(DatabaseException ex)
        {
            return false;
        }
    }
    /**
     * @param conn Database connector.
     * @param className The class-name of the exception.
     * @param runtime Indicates if the exception occurred at runtime.
     * @return The hint associated with the class-name.
     */
    public static String fetchHint(Connector conn, String className, boolean runtime)
    {
        try
        {
            return (String)conn.executeScalar("SELECT hint FROM pals_exception_classes WHERE class_name=? AND runtime=?;", className, runtime ? "1" : "0");
        }
        catch(DatabaseException ex)
        {
            return null;
        }
    }
    // Methods - Persistence - Deleting ****************************************
    /**
     * Removes all the exception data.
     * 
     * @param conn Database connector.
     * @param lf Deletion filer.
     */
    public static void delete(Connector conn, LoadRemoveFilter lf)
    {
        try
        {
            if(lf == LoadRemoveFilter.None)
                conn.execute("DELETE FROM pals_exceptions;");
            else
                conn.execute("DELETE FROM pals_exceptions WHERE exid IN (SELECT e.exid FROM pals_exceptions AS e,pals_exception_classes AS ec WHERE ec.ecid=e.ecid AND ec.runtime=?);", lf == LoadRemoveFilter.FilterRuntime ? "1" : "0");
        }
        catch(DatabaseException ex)
        {
        }
    }
    /**
     * Removes exception data for a module.
     * 
     * @param conn Database connector.
     * @param module The module.
     * @param lf Deletion filer.
     */
    public static void delete(Connector conn, Module module, LoadRemoveFilter lf)
    {
        try
        {
            if(lf == LoadRemoveFilter.None)
                conn.execute("DELETE FROM pals_exceptions WHERE exid IN (SELECT e.exid FROM pals_exception_classes AS ec, pals_exceptions AS e, pals_assignment_instance AS ai, pals_assignment AS a WHERE e.ecid=ec.ecid AND ai.aiid=e.aiid AND a.assid=ai.assid AND a.moduleid=?);", module.getModuleID());
            else
                conn.execute("DELETE FROM pals_exceptions WHERE exid IN (SELECT e.exid FROM pals_exception_classes AS ec, pals_exceptions AS e, pals_assignment_instance AS ai, pals_assignment AS a WHERE e.ecid=ec.ecid AND ec.runtime=? AND ai.aiid=e.aiid AND a.assid=ai.assid AND a.moduleid=?);", lf == LoadRemoveFilter.FilterRuntime ? "1" : "0", module.getModuleID());
        }
        catch(DatabaseException ex)
        {
        }
    }
    /**
     * Removes exception data for an assignment.
     * @param conn Database connector.
     * @param ass The assignment.
     * @param lf Deletion filer.
     */
    public static void delete(Connector conn, Assignment ass, LoadRemoveFilter lf)
    {
        try
        {
            if(lf == LoadRemoveFilter.None)
                conn.execute("DELETE FROM pals_exceptions WHERE exid IN (SELECT e.exid FROM pals_exception_classes AS ec, pals_exceptions AS e, pals_assignment_instance AS ai WHERE e.ecid=ec.ecid AND ai.aiid=e.aiid AND ai.assid=?);", ass.getAssID());
            else
                conn.execute("DELETE FROM pals_exceptions WHERE exid IN (SELECT e.exid FROM pals_exception_classes AS ec, pals_exceptions AS e, pals_assignment_instance AS ai WHERE e.ecid=ec.ecid AND ec.runtime=? AND ai.aiid=e.aiid AND ai.assid=?);", lf == LoadRemoveFilter.FilterRuntime ? "1" : "0", ass.getAssID());
        }
        catch(DatabaseException ex)
        {
        }
    }
    /**
     * Removes exception data for a question.
     * 
     * @param conn Database connector.
     * @param q The question.
     * @param lf Deletion filer.
     */
    public static void delete(Connector conn, Question q, LoadRemoveFilter lf)
    {
        try
        {
            if(lf == LoadRemoveFilter.None)
                conn.execute("DELETE FROM pals_exceptions WHERE exid IN (SELECT e.exid FROM pals_exception_classes AS ec, pals_exceptions AS e, pals_assignment_questions AS aq WHERE e.ecid=ec.ecid AND aq.aqid=e.aqid AND aq.qid=?);", q.getQID());
            else
                conn.execute("DELETE FROM pals_exceptions WHERE exid IN (SELECT e.exid FROM pals_exception_classes AS ec, pals_exceptions AS e, pals_assignment_questions AS aq WHERE e.ecid=ec.ecid AND ec.runtime=? AND aq.aqid=e.aqid AND aq.qid=?);", lf == LoadRemoveFilter.FilterRuntime ? "1" : "0", q.getQID());
        }
        catch(DatabaseException ex)
        {
        }
    }
    // Methods - Accessors *****************************************************
    /**
     * @return The identifier of the class-name.
     */
    public int getECID()
    {
        return ecid;
    }
    /**
     * @return The number of instances of errors for this class-name.
     */
    public long getFrequency()
    {
        return frequency;
    }
    /**
     * @return The class-name of this exception.
     */
    public String getClassName()
    {
        return className;
    }
    /**
     * @return The hint associated with this exception; can be null.
     */
    public String getHint()
    {
        return hint;
    }
    /**
     * @return Indicates if this is a run-time exception.
     */
    public  boolean isRuntime()
    {
        return runtime;
    }
}
