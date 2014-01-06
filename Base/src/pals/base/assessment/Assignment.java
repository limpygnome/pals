package pals.base.assessment;

import java.util.ArrayList;
import java.util.HashMap;
import pals.base.NodeCore;
import pals.base.database.Connector;
import pals.base.database.DatabaseException;
import pals.base.database.Result;

/**
 * A model which represents an assignment.
 */
public class Assignment
{
    // Enums *******************************************************************
    public enum PersistStatus
    {
        Success,
        Failed,
        Invalid_Module,
        Invalid_Title,
        Invalid_Weight,
    }
    public enum QuestionsPersistStatus
    {
        Success,
        Failed
    }
    // Fields ******************************************************************
    private int                                             assid;
    private Module                                          module;
    private String                                          title;
    private int                                             weight;
    private boolean                                         active;
    private HashMap<Integer,ArrayList<AssignmentQuestion>>  questions;  // page,list<question>
    // Methods - Constructors **************************************************
    /**
     * Creates a new unpersisted assignment.
     */
    public Assignment()
    {
        this(null, null, 0, false);
    }
    /**
     * Creates a new unpersisted assignment.
     * 
     * @param module The module to which the assignment belongs.
     * @param title The title of the assignment.
     * @param weight The weight of the assignment.
     * @param active Indicates if the assignment is active.
     */
    public Assignment(Module module, String title, int weight, boolean active)
    {
        this.assid = -1;
        this.module = module;
        this.title = title;
        this.weight = weight;
        this.active = active;
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
            return new Assignment[0];
        }
    }
    /**
     * Loads the persisted assignments for a module.
     * 
     * @param conn Database connector.
     * @param module The module.
     * @return Array of assignments for the specified module; possibly empty.
     */
    public static Assignment[] load(Connector conn, Module module)
    {
        if(module == null)
            return new Assignment[0];
        try
        {
            ArrayList<Assignment> buffer = new ArrayList<>();
            Assignment ass;
            Result res = conn.read("SELECT * FROM pals_assignment WHERE moduleid=?;", module.getModuleID());
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
            Assignment ass = new Assignment(module, (String)result.get("title"), (int)result.get("weight"), ((String)result.get("active")).equals("1"));
            ass.assid = result.get("assid");
            return ass;
        }
        catch(DatabaseException ex)
        {
            return null;
        }
    }
    /**
     * Persists the model to the database; if the model is unpersisted, it is
     * assigned a new assid.
     * 
     * @param conn Database connector.
     * @return The status of the operation.
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
        else
        {
            try
            {
                // Attempt to persist
                if(assid == -1)
                {
                    assid = (int)conn.executeScalar("INSERT INTO pals_assignment (moduleid, title, weight, active) VALUES(?,?,?,?) RETURNING assid;",
                            module.getModuleID(),
                            title,
                            weight,
                            active ? "1" : "0"
                            );
                }
                else
                {
                    conn.execute("UPDATE pals_assignment SET moduleid=?, title=?, weight=?, active=? WHERE assid=?;",
                            module.getModuleID(),
                            title,
                            weight,
                            active ? "1" : "0",
                            assid
                            );
                }
                return PersistStatus.Success;
            }
            catch(DatabaseException ex)
            {
                return PersistStatus.Failed;
            }
        }
    }
    /**
     * Unpersists the model from the database.
     * 
     * @param conn Database connector.
     * @return True = removed, false = failed.
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
            return false;
        }
    }
    // Methods - Mutators ******************************************************
    /**
     * @param module The module of which this assignment belongs.
     */
    public void setModule(Module module)
    {
        this.module = module;
    }
    /**
     * @param title The title of the assignment; cannot be null or empty.
     */
    public void setTitle(String title)
    {
        this.title = title;
    }
    /**
     * @param weight The weight of the assignment; must be greater than zero.
     */
    public void setWeight(int weight)
    {
        this.weight = weight;
    }
    /**
     * @param active Sets if the assignment can be taken by students.
     */
    public void setActive(boolean active)
    {
        this.active = active;
    }
    // Methods - Accessors *****************************************************
    /**
     * @return Indicates if the model has been persisted.
     */
    public boolean isPersisted()
    {
        return assid != -1;
    }
    /**
     * @return Indicates if the assignment can be taken by students.
     */
    public boolean isActive()
    {
        return active;
    }
    /**
     * @return The identifier of the assignment.
     */
    public int getAssID()
    {
        return assid;
    }
    /**
     * @return The module to which the assignment belongs.
     */
    public Module getModule()
    {
        return module;
    }
    /**
     * @return The title of the assignment.
     */
    public String getTitle()
    {
        return title;
    }
    /**
     * @return The weight of the assignment.
     */
    public int getWeight()
    {
        return weight;
    }
    // Methods - Accessors/Mutators - Questions ********************************
    /**
     * @return The map of the questions; the key is the page and the value is
     * the list of questions for that page, ordered by zindex.
     */
    public HashMap<Integer, ArrayList<AssignmentQuestion>> questionsMap()
    {
        return questions;
    }
    /**
     * @param page The page of questions to retrieve.
     * @return All of the questions for the specified page.
     */
    public AssignmentQuestion[] questions(int page)
    {
        ArrayList<AssignmentQuestion> buffer = questions.get(page);
        return buffer == null ? new AssignmentQuestion[0] : buffer.toArray(new AssignmentQuestion[buffer.size()]);
    }
    /**
     * @return Fetches an array of pages based on the questions loaded in the
     * collection; questionsPagesDb should be invoked if only a certain page
     * of questions have been loaded.
     */
    public Integer[] questionsPages()
    {
        return questions.keySet().toArray(new Integer[questions.size()]);
    }
    /**
     * @param conn Database connector.
     * @return Array of question pages available.
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
            return new Integer[0];
        }
    }
    /**
     * Removes all of the questions associated with this assignment.
     * 
     * @param conn Database connector.
     * @return True = all removed, false = failed.
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
            return false;
        }
    }
    private void questionsAdd(AssignmentQuestion question)
    {
        ArrayList<AssignmentQuestion> qs = questions.get(question.getPage());
        if(qs == null)
        {
            qs = (questions.put(question.getPage(), new ArrayList<AssignmentQuestion>()));
        }
        qs.add(question);
    }
    // Methods - Accessors - Limits ********************************************
    /**
     * @return The minimum length of an assignment title.
     */
    public int getTitleMin()
    {
        return 1;
    }
    /**
     * @return The maximum length of an assignment title.
     */
    public int getTitleMax()
    {
        return 64;
    }
}
