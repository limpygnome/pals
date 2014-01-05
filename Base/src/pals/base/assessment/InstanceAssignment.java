package pals.base.assessment;

import pals.base.auth.User;
import pals.base.database.Connector;
import pals.base.database.DatabaseException;
import pals.base.database.Result;

/**
 * A model which represents the instance of an assignment for a user.
 */
public class InstanceAssignment
{
    // Enums *******************************************************************
    public enum PersistStatus
    {
        Success,
        Failed,
        Invalid_Assignment,
        Invalid_User,
        Invalid_Mark
    }
    // Fields ******************************************************************
    private int         aiid;       // The identifier of the assignment instance.
    private User        user;       // The user who is answering this instance of the assignment.
    private Assignment  ass;        // The assignment instantiated.
    private boolean     marked;     // Indicates if the assignment has been marked.
    private double      mark;       // The mark of the instance.
    // Methods - Constructors **************************************************
    /**
     * Creates a new instance of an unpersisted instance of an assignment.
     */
    public InstanceAssignment()
    {
        this(null, null, false, 0);
    }
    /**
     * Creates a new instance of an unpersisted instance of an assignment.
     * 
     * @param user The user who is answering this instance of the assignment.
     * @param ass The assignment instantiated.
     * @param marked Indicates if the assignment has been marked.
     * @param mark The mark of the instance.
     */
    public InstanceAssignment(User user, Assignment ass, boolean marked,  int mark)
    {
        this.aiid = -1;
        this.user = user;
        this.ass = ass;
        this.marked = marked;
        this.mark = mark;
    }
    // Methods - Persistence ***************************************************
    /**
     * Loads a persisted model.
     * 
     * @param conn Database connector.
     * @param assignment The assignment of the model; can be null. This is for
     * when the assignment is known and avoids reloading multiple times.
     * Automatically loaded if null.
     * @param user The user of the model; can be null. This is when the user
     * is known and avoids reloading multiple times. Automatically loaded if
     * null.
     * @param aiid The identifier of the assignment instance model.
     * @return An instance of the model or null.
     */
    public static InstanceAssignment load(Connector conn, Assignment assignment, User user, int aiid)
    {
        try
        {
            Result res;
            if(assignment == null)
                res = conn.read("SELECT * FROM pals_assignment_instance WHERE aiid=?;", aiid);
            else
                res = conn.read("SELECT * FROM pals_assignment_instance WHERE aiid=? AND assid=?;", aiid, assignment.getAssID());
            return res.next() ? load(conn, assignment, user, res) : null;
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
     * @param ass The assignment of the model; can be null. This is for
     * when the assignment is known and avoids reloading multiple times.
     * @param user The user of the model; can be null. This is when the user
     * is known and avoids reloading multiple times. Automatically loaded if
     * null.
     * @param res The result from a query; next() should be pre-invoked.
     * @return An instance of the model or null.
     */
    public static InstanceAssignment load(Connector conn, Assignment ass, User user, Result res)
    {
        try
        {
            // Load assignment, if null
            if(ass == null)
            {
                ass = Assignment.load(conn, null, (int)res.get("assid"));
                if(ass == null)
                    return null;
            }
            // Load user, if null
            if(user == null)
            {
                user = User.load(conn, (int)res.get("userid"));
                if(user == null)
                    return null;
            }
            // Setup instance and return
            InstanceAssignment ia = new InstanceAssignment(user, ass, ((String)res.get("marked")).equals("1"), (int)res.get("mark"));
            ia.aiid = (int)res.get("aiid");
            return ia;
        }
        catch(DatabaseException ex)
        {
            return null;
        }
    }
    /**
     * Persists the model to the database; if this model is unpersisted, its
     * identifier is automatically assigned.
     * 
     * @param conn Database connector.
     * @return The status of the operation.
     */
    public PersistStatus persist(Connector conn)
    {
        // Validate data
        if(ass == null || !ass.isPersisted())
            return PersistStatus.Invalid_Assignment;
        else if(user == null || !user.isPersisted())
            return PersistStatus.Invalid_User;
        else if(mark < 0 || mark > 100)
            return PersistStatus.Invalid_Mark;
        // Attempt to persist data
        try
        {
            if(aiid == -1)
            {
                aiid = (int)conn.executeScalar("INSERT INTO pals_assignment_instance (userid, assid, marked, mark) VALUES(?,?,?,?) RETURNING aiid;",
                        user.getUserID(),
                        ass.getAssID(),
                        marked ? "1" : "0",
                        mark
                        );
            }
            else
            {
                conn.execute("UPDATE pals_assignment_instance SET userid=?, assid=?, marked=?, mark=? WHERE aiid=?;",
                        user.getUserID(),
                        ass.getAssID(),
                        marked ? "1" : "0",
                        mark,
                        aiid
                        );
            }
            return PersistStatus.Success;
        }
        catch(DatabaseException ex)
        {
            return PersistStatus.Failed;
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
        if(aiid == -1)
            return false;
        try
        {
            conn.execute("DELETE FROM pals_assignment_instance WHERE aiid=?;", aiid);
            return true;
        }
        catch(DatabaseException ex)
        {
            return false;
        }
    }
    // Methods - Mutators ******************************************************
    /**
     * @param user Sets the user who is answering this instance of the
     * assignment.
     */
    public void setUser(User user)
    {
        this.user = user;
    }
    /**
     * @param ass Sets the current assignment being instantiated.
     */
    public void setAss(Assignment ass)
    {
        this.ass = ass;
    }
    /**
     * @param marked Sets if this assignment has been marked.
     */
    public void setMarked(boolean marked)
    {
        this.marked = marked;
    }
    /**
     * @param mark Sets the mark of this assignment.
     */
    public void setMark(double mark)
    {
        this.mark = mark;
    }
    // Methods - Accessors *****************************************************
    /**
     * @return Indicates if the model has been persisted.
     */
    public boolean isPersisted()
    {
        return aiid != -1;
    }
    /**
     * @return The identifier of this model.
     */
    public int getAIID()
    {
        return aiid;
    }
    /**
     * @return The user who is answering this instance of the assignment.
     */
    public User getUser()
    {
        return user;
    }
    /**
     * @return The assignment instantiated.
     */
    public Assignment getAss()
    {
        return ass;
    }
    /**
     * @return Indicates if the assignment has been marked.
     */
    public boolean isMarked()
    {
        return marked;
    }
    /**
     * @return The mark of the instance.
     */
    public double getMark()
    {
        return mark;
    }
}
