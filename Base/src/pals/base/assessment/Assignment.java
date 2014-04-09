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
    Authors:    Marcus Craske           <limpygnome@gmail.com>
    ----------------------------------------------------------------------------
*/
package pals.base.assessment;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import org.joda.time.DateTime;
import pals.base.Logging;
import pals.base.NodeCore;
import pals.base.database.Connector;
import pals.base.database.DatabaseException;
import pals.base.database.Result;

/**
 * A model which represents an assignment.
 * 
 * @version 1.0
 */
public class Assignment
{
    // Enums *******************************************************************
    /**
     * The status of persisting the model.
     * 
     * @since 1.0
     */
    public enum PersistStatus
    {
        /**
         * Successfully persisted.
         * 
         * @since 1.0
         */
        Success,
        /**
         * Failed due to an unknown state or exception.
         * 
         * @since 1.0
         */
        Failed,
        /**
         * Invalid module.
         * 
         * @since 1.0
         */
        Invalid_Module,
        /**
         * Invalid title.
         * 
         * @since 1.0
         */
        Invalid_Title,
        /**
         * Invalid weight.
         * 
         * @since 1.0
         */
        Invalid_Weight,
        /**
         * Invalid max-attempts.
         * 
         * @since 1.0
         */
        Invalid_MaxAttempts,
        /**
         * Invalid due date-time.
         * 
         * @since 1.0
         */
        Invalid_Due
    }
    // Fields ******************************************************************
    private int                                             assid;
    private Module                                          module;
    private String                                          title;
    private int                                             weight;
    private boolean                                         active;
    private int                                             maxAttempts;
    private DateTime                                        due;
    private boolean                                         dueHandled;
    private HashMap<Integer,ArrayList<AssignmentQuestion>>  questions;  // page,list
    // Methods - Constructors **************************************************
    /**
     * Creates a new unpersisted assignment.
     * 
     * @since 1.0
     */
    public Assignment()
    {
        this(null, null, 0, false, -1, null, false);
    }
    /**
     * Creates a new unpersisted assignment.
     * 
     * @param module The module to which the assignment belongs.
     * @param title The title of the assignment.
     * @param weight The weight of the assignment.
     * @param active Indicates if the assignment is active.
     * @param maxAttempts The maximum attempts; can be -1 for unlimited.
     * @param due  The date at which the assignment is due.
     * @param dueHandled Indicates if the due-date, once surpassed, has been handled.
     * @since 1.0
     */
    public Assignment(Module module, String title, int weight, boolean active, int maxAttempts, DateTime due, boolean dueHandled)
    {
        this.assid = -1;
        this.module = module;
        this.title = title;
        this.weight = weight;
        this.active = active;
        this.maxAttempts = maxAttempts;
        this.due = due;
        this.dueHandled = dueHandled;
        this.questions = new HashMap<>();
    }
    // Methods - Persistence ***************************************************
    /**
     * Loads the persisted assignments for a module, which are active/inactive.
     * 
     * @param conn Database connector.
     * @param module The module.
     * @param active If true, the active assignments are returned, else the
     * inactive assignments are returned.
     * @return Array of assignments for the specified module; possibly empty.
     * @since 1.0
     */
    public static Assignment[] loadActive(Connector conn, Module module, boolean active)
    {
        if(module == null)
            return new Assignment[0];
        try
        {
            ArrayList<Assignment> buffer = new ArrayList<>();
            Assignment ass;
            Result res = conn.read("SELECT * FROM pals_assignment WHERE moduleid=? AND active=?;", module.getModuleID(), active ? "1" : "0");
            while(res.next())
            {
                ass = load(conn, module, res);
                if(ass != null)
                    buffer.add(ass);
            }
            return buffer.toArray(new Assignment[buffer.size()]);
        }
        catch(DatabaseException ex)
        {
            NodeCore core;
            if((core = NodeCore.getInstance())!=null)
                core.getLogging().logEx("Base", ex, Logging.EntryType.Warning);
            return new Assignment[0];
        }
    }
    /**
     * Loads the persisted assignments for a module.
     * 
     * @param conn Database connector.
     * @param module The module.
     * @param filterActive Indicates if to filter the assignments by those
     * which are active.
     * @return Array of assignments for the specified module; possibly empty.
     * @since 1.0
     */
    public static Assignment[] load(Connector conn, Module module, boolean filterActive)
    {
        if(module == null)
            return new Assignment[0];
        try
        {
            ArrayList<Assignment> buffer = new ArrayList<>();
            Assignment ass;
            Result res = conn.read("SELECT * FROM pals_assignment WHERE moduleid=?"+(filterActive ? " AND active='1'" : "")+";", module.getModuleID());
            while(res.next())
            {
                ass = load(conn, module, res);
                if(ass != null)
                    buffer.add(ass);
            }
            return buffer.toArray(new Assignment[buffer.size()]);
        }
        catch(DatabaseException ex)
        {
            NodeCore core;
            if((core = NodeCore.getInstance())!=null)
                core.getLogging().logEx("Base", ex, Logging.EntryType.Warning);
            return new Assignment[0];
        }
    }
    /**
     * Loads a persisted model.
     * 
     * @param conn Database connector.
     * @param module The module of the assignment; if this is null, this is
     * loaded (possibly expensive).
     * @param assid The identifier of the assignment.
     * @return An instance of the model or null.
     * @since 1.0
     */
    public static Assignment load(Connector conn, Module module, int assid)
    {
        try
        {
            Result res;
            if(module == null)
                res = conn.read("SELECT * FROM pals_assignment WHERE assid=?;", assid);
            else
                res = conn.read("SELECT * FROM pals_assignment WHERE assid=? AND moduleid=?;", assid, module.getModuleID());
            return res.next() ? load(conn, module, res) : null;
        }
        catch(DatabaseException ex)
        {
            NodeCore core;
            if((core = NodeCore.getInstance())!=null)
                core.getLogging().logEx("Base", ex, Logging.EntryType.Warning);
            return null;
        }
    }
    /**
     * Loads a persisted model.
     * 
     * @param conn Database connector.
     * @param module The module of the assignment; if this is null, this is
     * loaded (possibly expensive).
     * @param result The result of a query; next() should be pre-invoked.
     * @return An instance of the model or null.
     * @since 1.0
     */
    public static Assignment load(Connector conn, Module module, Result result)
    {
        try
        {
            // Load the module
            if(module == null)
            {
                module = Module.load(conn, (int)result.get("moduleid"));
                if(module == null)
                    return null;
            }
            // Create and return an instance
            Object due = result.get("due");
            Assignment ass = new Assignment(module, (String)result.get("title"), (int)result.get("weight"), ((String)result.get("active")).equals("1"), (int)result.get("max_attempts"), due != null ? new DateTime(((Date)result.get("due")).getTime()) : null, ((String)result.get("due_handled")).equals("1"));
            ass.assid = result.get("assid");
            return ass;
        }
        catch(DatabaseException ex)
        {
            NodeCore core;
            if((core = NodeCore.getInstance())!=null)
                core.getLogging().logEx("Base", ex, Logging.EntryType.Warning);
            return null;
        }
    }
    /**
     * Persists the model to the database; if the model is unpersisted, it is
     * assigned a new assid.
     * 
     * @param conn Database connector.
     * @return The status of the operation.
     * @since 1.0
     */
    public PersistStatus persist(Connector conn)
    {
        // Validate data
        if(module == null || !module.isPersisted())
            return PersistStatus.Invalid_Module;
        else if(title == null || title.length() < getTitleMin() || title.length() > getTitleMax())
            return PersistStatus.Invalid_Title;
        else if(weight <= 0)
            return PersistStatus.Invalid_Weight;
        else if(maxAttempts < -1 || maxAttempts == 0)
            return PersistStatus.Invalid_MaxAttempts;
        else if(isDueSurpassed())
            return PersistStatus.Invalid_Due;
        else
        {
            try
            {
                // Attempt to persist
                if(assid == -1)
                {
                    assid = (int)conn.executeScalar("INSERT INTO pals_assignment (moduleid, title, weight, active, max_attempts, due, due_handled) VALUES(?,?,?,?,?,?,?) RETURNING assid;",
                            module.getModuleID(),
                            title,
                            weight,
                            active ? "1" : "0",
                            maxAttempts,
                            (due != null ? new Timestamp(due.toDate().getTime()) : null),
                            dueHandled ? "1" : "0"
                            );
                }
                else
                {
                    conn.execute("UPDATE pals_assignment SET moduleid=?, title=?, weight=?, active=?, max_attempts=?, due=?, due_handled=? WHERE assid=?;",
                            module.getModuleID(),
                            title,
                            weight,
                            active ? "1" : "0",
                            maxAttempts,
                            (due != null ? new Timestamp(due.toDate().getTime()) : null),
                            dueHandled ? "1" : "0",
                            assid
                            );
                }
                return PersistStatus.Success;
            }
            catch(DatabaseException ex)
            {
                NodeCore core;
                if((core = NodeCore.getInstance())!=null)
                    core.getLogging().logEx("Base", ex, Logging.EntryType.Warning);
                return PersistStatus.Failed;
            }
        }
    }
    /**
     * Unpersists the model from the database.
     * 
     * @param conn Database connector.
     * @return True = removed, false = failed.
     * @since 1.0
     */
    public boolean delete(Connector conn)
    {
        if(assid == -1)
            return false;
        try
        {
            conn.execute("DELETE FROM pals_assignment WHERE assid=?;", assid);
            return true;
        }
        catch(DatabaseException ex)
        {
            NodeCore core;
            if((core = NodeCore.getInstance())!=null)
                core.getLogging().logEx("Base", ex, Logging.EntryType.Warning);
            return false;
        }
    }
    // Methods - Mutators ******************************************************
    /**
     * Sets the module, to which the assignment belongs.
     * 
     * @param module The module of which this assignment belongs.
     * @since 1.0
     */
    public void setModule(Module module)
    {
        this.module = module;
    }
    /**
     * Sets the title of the assignment.
     * 
     * @param title The title of the assignment; cannot be null or empty.
     * @since 1.0
     */
    public void setTitle(String title)
    {
        this.title = title;
    }
    /**
     * Sets the weight of the assignment, relative to the module.
     * 
     * @param weight The weight of the assignment; must be greater than zero.
     * @since 1.0
     */
    public void setWeight(int weight)
    {
        this.weight = weight;
    }
    /**
     * Sets if the assignment is active.
     * 
     * @param active Sets if the assignment can be taken by students.
     * @since 1.0
     */
    public void setActive(boolean active)
    {
        this.active = active;
    }
    /**
     * Sets the maximum attempts, which is the number of times this assignment
     * can be taken by a user.
     * 
     * @param maxAttempts Sets the maximum attempts for the assignment; can be
     * -1 for unlimited or greater than zero.
     * @since 1.0
     */
    public void setMaxAttempts(int maxAttempts)
    {
        this.maxAttempts = maxAttempts;
    }
    /**
     * Sets the due-date of the assignment.
     * 
     * @param due The date-time of when the assignment is due; can be null.
     * @since 1.0
     */
    public void setDue(DateTime due)
    {
        this.due = due;
    }
    /**
     * Sets if the due-date has been handled.
     * 
     * @param dueHandled Sets if the due-date has been handled.
     * @since 1.0
     */
    public void setDueHandled(boolean dueHandled)
    {
        this.dueHandled = dueHandled;
    }
    // Methods - Accessors *****************************************************
    /**
     * Indicates if this model has been persisted.
     * 
     * @return True = persisted, false = not persisted.
     * @since 1.0
     */
    public boolean isPersisted()
    {
        return assid != -1;
    }
    /**
     * The identifier of the assignment.
     * 
     * @return The identifier of the assignment.
     * @since 1.0
     */
    public int getAssID()
    {
        return assid;
    }
    /**
     * The module to which the assignment belongs.
     * 
     * @return The module to which the assignment belongs.
     * @since 1.0
     */
    public Module getModule()
    {
        return module;
    }
    /**
     * The title of the assignment.
     * 
     * @return The title of the assignment.
     * @since 1.0
     */
    public String getTitle()
    {
        return title;
    }
    /**
     * The weight of the assignment.
     * 
     * @return The weight of the assignment.
     * @since 1.0
     */
    public int getWeight()
    {
        return weight;
    }
    /**
     * Indicates if the assignment is active.
     * 
     * @return Indicates if the assignment can be taken by students.
     * @since 1.0
     */
    public boolean isActive()
    {
        return active;
    }
    /**
     * The maximum number of times this assignment can be taken by users.
     * 
     * @return The maximum number of attempts; -1 for unlimited.
     * @since 1.0
     */
    public int getMaxAttempts()
    {
        return maxAttempts;
    }
    /**
     * The date at which the assignment can no longer be taken.
     * 
     * @return The date-time of when the assignment is due; can be null.
     * @since 1.0
     */
    public DateTime getDue()
    {
        return due;
    }
    /**
     * A flag which is used to indicate if a node is handling a due-date being
     * surpassed. Once surpassed and false, all instances of this assignment
     * are set to true.
     * 
     * @return Indicates if the due-date, once surpassed, has been handled.
     * @since 1.0
     */
    public boolean isDueHandled()
    {
        return dueHandled;
    }
    /**
     * Indicates if the due-date has been surpassed.
     * 
     * @return Indicates if the due-date for the assignment has been surpassed
     * by the present time and it is now in the past.
     * @since 1.0
     */
    public boolean isDueSurpassed()
    {
        return due != null &&(DateTime.now().getMillis()-due.getMillis()) > 0;
    }
    // Methods - Accessors/Mutators - Questions ********************************
    /**
     * The underlying data-structure used to hold the assignment questions.
     * 
     * @return The map of the questions; the key is the page and the value is
     * the list of questions for that page, ordered by zindex.
     * @since 1.0
     */
    public HashMap<Integer, ArrayList<AssignmentQuestion>> questionsMap()
    {
        return questions;
    }
    /**
     * Fetches questions for a page.
     * 
     * @param page The page of questions to retrieve.
     * @return All of the questions for the specified page.
     * @since 1.0
     */
    public AssignmentQuestion[] questions(int page)
    {
        ArrayList<AssignmentQuestion> buffer = questions.get(page);
        return buffer == null ? new AssignmentQuestion[0] : buffer.toArray(new AssignmentQuestion[buffer.size()]);
    }
    /**
     * Fetches the numbers of all the available question pages for this
     * assignment.
     * 
     * @return Fetches an array of pages based on the questions loaded in the
     * collection; questionsPagesDb should be invoked if only a certain page
     * of questions have been loaded.
     * @since 1.0
     */
    public Integer[] questionsPages()
    {
        return questions.keySet().toArray(new Integer[questions.size()]);
    }
    /**
     * Fetches the numbers of all the available question pages for this
     * assignment. This is retrieved from the database, when the models
     * of questions have not been loaded.
     * 
     * @param conn Database connector.
     * @return Array of question pages available.
     * @since 1.0
     */
    public Integer[] questionsPagesDb(Connector conn)
    {
        try
        {
            ArrayList<Integer> buffer = new ArrayList<>();
            Result res = conn.read("SELECT DISTINCT page FROM pals_assignment_questions WHERE assid=?;", assid);
            while(res.next())
            {
                buffer.add((int)res.get("page"));
            }
            return buffer.toArray(new Integer[buffer.size()]);
        }
        catch(DatabaseException ex)
        {
            NodeCore core;
            if((core = NodeCore.getInstance())!=null)
                core.getLogging().logEx("Base", ex, Logging.EntryType.Warning);
            return new Integer[0];
        }
    }
    /**
     * Removes all of the questions associated with this assignment.
     * 
     * @param conn Database connector.
     * @return True = all removed, false = failed.
     * @since 1.0
     */
    public boolean questionsRemoveAll(Connector conn)
    {
        try
        {
            conn.execute("DELETE FROM pals_assignment WHERE assid=?;", assid);
            return true;
        }
        catch(DatabaseException ex)
        {
            NodeCore core;
            if((core = NodeCore.getInstance())!=null)
                core.getLogging().logEx("Base", ex, Logging.EntryType.Warning);
            return false;
        }
    }
    /**
     * Loads the persisted questions associated with this assignment.
     * 
     * @param core Current instance of the core.
     * @param conn Database connector.
     * @param page The page; if greater than zero, questions are filtered by
     * their page.
     * @return True = loaded, false = failed.
     * @since 1.0
     */
    public boolean questionsLoad(NodeCore core, Connector conn, int page)
    {
        try
        {
            // Clear previous questions
            questions.clear();
            // Query for existing data
            Result res;
            if(page >= 0)
                res = conn.read("SELECT * FROM pals_assignment_questions WHERE assid=? AND page=? ORDER BY page ASC, page_order ASC;", assid, page);
            else
                res = conn.read("SELECT * FROM pals_assignment_questions WHERE assid=? ORDER BY page ASC, page_order ASC;", assid);
            // Read results of query
            AssignmentQuestion question;
            while(res.next())
            {
                question = AssignmentQuestion.load(core, conn, this, res);
                if(question != null)
                {
                    questionsAdd(question);
                }
            }
            return true;
        }
        catch(DatabaseException ex)
        {
            core.getLogging().logEx("Base", ex, Logging.EntryType.Warning);
            return false;
        }
    }
    private void questionsAdd(AssignmentQuestion question)
    {
        if(!questions.containsKey(question.getPage()))
            questions.put(question.getPage(), new ArrayList<AssignmentQuestion>());
        ArrayList<AssignmentQuestion> qs = questions.get(question.getPage());
        qs.add(question);
    }
    // Methods - Accessors - Limits ********************************************
    /**
     * The minimum length of a title.
     * 
     * @return The minimum length of an assignment title.
     * @since 1.0
     */
    public int getTitleMin()
    {
        return 1;
    }
    /**
     * The maximum length of a title.
     * 
     * @return The maximum length of an assignment title.
     * @since 1.0
     */
    public int getTitleMax()
    {
        return 64;
    }
}
