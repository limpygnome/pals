package pals.plugins.handlers.defaultqch.logging;

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
    private String  className;
    private boolean runtime;
    // Enums *******************************************************************
    public enum LoadRemoveFilter
    {
        None,
        FilterCompileTime,
        FilterRuntime
    }
    // Methods - Constructors **************************************************
    private ModelExceptionClass(int ecid, long frequency, String className, boolean runtime)
    {
        this.ecid = ecid;
        this.frequency = frequency;
        this.className = className;
        this.runtime = runtime;
    }
    // Methods - Persistence - Loading *****************************************
    public static ModelExceptionClass[] load(Connector conn, LoadRemoveFilter lf)
    {
        try
        {
            if(lf == LoadRemoveFilter.None)
                return load(conn, conn.read("SELECT ec.ecid, ec.class_name, ec.runtime, COUNT(e.ecid) AS freq FROM pals_exception_classes AS ec, pals_exceptions AS e WHERE ec.ecid=e.ecid GROUP BY ec.ecid ORDER BY freq DESC;"));
            else
                return load(conn, conn.read("SELECT ec.ecid, ec.class_name, ec.runtime, COUNT(e.ecid) AS freq FROM pals_exception_classes AS ec, pals_exceptions AS e WHERE ec.ecid=e.ecid AND ec.runtime=? GROUP BY ec.ecid ORDER BY freq DESC;", lf == LoadRemoveFilter.FilterRuntime ? "1" : "0"));
        }
        catch(DatabaseException ex)
        {
            return new ModelExceptionClass[0];
        }
    }
    /**
     * @param conn Database connector.
     * @param module The module of errors, and their frequencies, to fetch.
     * @return Array-list of models; can be empty.
     */
    public static ModelExceptionClass[] load(Connector conn, Module module, LoadRemoveFilter lf)
    {
        try
        {
            if(lf == LoadRemoveFilter.None)
                return load(conn, conn.read("SELECT ec.ecid, ec.class_name, ec.runtime, COUNT(e.ecid) AS freq FROM pals_exception_classes AS ec, pals_exceptions AS e, pals_assignment_instance AS ai, pals_assignment AS a WHERE e.ecid=ec.ecid AND ai.aiid=e.aiid AND a.assid=ai.assid AND a.moduleid=? GROUP BY ec.ecid ORDER BY freq DESC;", module.getModuleID()));
            else
                return load(conn, conn.read("SELECT ec.ecid, ec.class_name, ec.runtime, COUNT(e.ecid) AS freq FROM pals_exception_classes AS ec, pals_exceptions AS e, pals_assignment_instance AS ai, pals_assignment AS a WHERE e.ecid=ec.ecid AND ec.runtime=? AND ai.aiid=e.aiid AND a.assid=ai.assid AND a.moduleid=? GROUP BY ec.ecid ORDER BY freq DESC;", lf == LoadRemoveFilter.FilterRuntime ? "1" : "0", module.getModuleID()));
        }
        catch(DatabaseException ex)
        {
            return new ModelExceptionClass[0];
        }
    }
    /**
     * @param conn Database connector.
     * @param ass The assignment of errors, and their frequencies, to fetch.
     * @return Array-list of models; can be empty.
     */
    public static ModelExceptionClass[] load(Connector conn, Assignment ass, LoadRemoveFilter lf)
    {
        try
        {
            if(lf == LoadRemoveFilter.None)
                return load(conn, conn.read("SELECT ec.ecid, ec.class_name, ec.runtime, COUNT(e.ecid) AS freq FROM pals_exception_classes AS ec, pals_exceptions AS e, pals_assignment_instance AS ai WHERE e.ecid=ec.ecid AND ai.aiid=e.aiid AND ai.assid=? GROUP BY ec.ecid ORDER BY freq DESC;", ass.getAssID()));
            else
                return load(conn, conn.read("SELECT ec.ecid, ec.class_name, ec.runtime, COUNT(e.ecid) AS freq FROM pals_exception_classes AS ec, pals_exceptions AS e, pals_assignment_instance AS ai WHERE e.ecid=ec.ecid AND ec.runtime=? AND ai.aiid=e.aiid AND ai.assid=? GROUP BY ec.ecid ORDER BY freq DESC;", lf == LoadRemoveFilter.FilterRuntime ? "1" : "0", ass.getAssID()));
        }
        catch(DatabaseException ex)
        {
            return new ModelExceptionClass[0];
        }
    }
    /**
     * @param conn Database connector.
     * @param q The question of errors, and their frequencies, to fetch.
     * @return Array-list of models; can be empty.
     */
    public static ModelExceptionClass[] load(Connector conn, Question q, LoadRemoveFilter lf)
    {
        try
        {
            if(lf == LoadRemoveFilter.None)
                return load(conn, conn.read("SELECT ec.ecid, ec.class_name, ec.runtime, COUNT(e.ecid) AS freq FROM pals_exception_classes AS ec, pals_exceptions AS e, pals_assignment_questions AS aq WHERE e.ecid=ec.ecid AND aq.aqid=e.aqid AND aq.qid=? GROUP BY ec.ecid ORDER BY freq DESC;", q.getQID()));
            else
                return load(conn, conn.read("SELECT ec.ecid, ec.class_name, ec.runtime, COUNT(e.ecid) AS freq FROM pals_exception_classes AS ec, pals_exceptions AS e, pals_assignment_questions AS aq WHERE e.ecid=ec.ecid AND ec.runtime=? AND aq.aqid=e.aqid AND aq.qid=? GROUP BY ec.ecid ORDER BY freq DESC;", lf == LoadRemoveFilter.FilterRuntime ? "1" : "0", q.getQID()));
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
     * @param res Result from a query; next() should be invoked.
     * @return Instance of model or null.
     */
    public static ModelExceptionClass loadSingle(Connector conn, Result res)
    {
        try
        {
            return new ModelExceptionClass((int)res.get("ecid"), (long)res.get("freq"), (String)res.get("class_name"), ((String)res.get("runtime")).equals("1"));
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
     * @return Indicates if this is a run-time exception.
     */
    public  boolean isRuntime()
    {
        return runtime;
    }
}
