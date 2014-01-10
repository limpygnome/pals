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
    public enum Status
    {
        Active(0),
        Submitted(1),
        Marking(2),
        Marked(3);
        
        private final int status;
        private Status(int status)
        {
            this.status = status;
        }
        /**
         * @return The status of the assignment.
         */
        public int getStatus()
        {
            return status;
        }
        /**
         * @param value The value to parse.
         * @return Parses an integer value, returning the Status enum-type;
         * if a type is not matched, 'BeingTaken' is returned.
         */
        public static Status parse(int value)
        {
            switch(value)
            {
                default:
                case 0:
                    return Active;
                case 1:
                    return Submitted;
                case 2:
                    return Marking;
                case 3:
                    return Marked;
            }
        }
    }
    // Fields ******************************************************************
    private int         aiid;       // The identifier of the assignment instance.
    private User        user;       // The user who is answering this instance of the assignment.
    private Assignment  ass;        // The assignment instantiated.
    private Status      status;     // The status of this instance.
    private double      mark;       // The mark of the instance.
    // Methods - Constructors **************************************************
    /**
     * Creates a new instance of an unpersisted instance of an assignment.
     */
    public InstanceAssignment()
    {
        this(null, null, Status.Active, 0);
    }
    /**
     * Creates a new instance of an unpersisted instance of an assignment.
     * 
     * @param user The user who is answering this instance of the assignment.
     * @param ass The assignment instantiated.
     * @param status The status of the instance.
     * @param mark The mark of the instance.
     */
    public InstanceAssignment(User user, Assignment ass, Status status,  double mark)
    {
        this.aiid = -1;
        this.user = user;
        this.ass = ass;
        this.status = status;
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
            Result res = conn.read("SELECT * FROM pals_assignment_instance WHERE aiid=?;", aiid);
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
            else if(ass.getAssID() != (int)res.get("assid"))
                return null;
            // Load user, if null
            if(user == null)
            {
                user = User.load(conn, (int)res.get("userid"));
                if(user == null)
                    return null;
            }
            else if(user.getUserID() != (int)res.get("userid"))
                return null;
            // Setup instance and return
            InstanceAssignment ia = new InstanceAssignment(user, ass, Status.parse((int)res.get("status")), (double)res.get("mark"));
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
                aiid = (int)conn.executeScalar("INSERT INTO pals_assignment_instance (userid, assid, status, mark) VALUES(?,?,?,?) RETURNING aiid;",
                        user.getUserID(),
                        ass.getAssID(),
                        status.getStatus(),
                        mark
                        );
            }
            else
            {
                conn.execute("UPDATE pals_assignment_instance SET userid=?, assid=?, status=?, mark=? WHERE aiid=?;",
                        user.getUserID(),
                        ass.getAssID(),
                        status.getStatus(),
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
    /**
     * Computes the overall mark of the assignment, as well as the instances
     * of the questions.
     * 
     * @param conn Database connector.
     * @return True = successfully marked, false = failed.
     */
    public boolean computeMark(Connector conn)
    {
        try
        {
            // Compute marks of each question answered
            Result res = conn.read(
                "SELECT aiq.aiqid, CAST(("
                + "("
                + "(SELECT SUM((aiqc.mark/100.0)*qc.weight) FROM pals_assignment_instance_question_criteria AS aiqc LEFT OUTER JOIN pals_question_criteria AS qc ON qc.qcid=aiqc.qcid WHERE aiqc.aiqid=aiq.aiqid)"
                + "/"
                + "(SELECT SUM(qc.weight) FROM pals_question_criteria AS qc WHERE qc.qid=aq.qid)"
                + ") * 100.0) AS double precision) AS mark "
                + "FROM pals_assignment_instance_question AS aiq LEFT OUTER JOIN pals_assignment_questions AS aq ON aq.aqid=aiq.aqid WHERE aiq.aiid=?;",
                    aiid
            );
            while(res.next())
                conn.execute("UPDATE pals_assignment_instance_question SET mark=? WHERE aiqid=?;", (double)res.get("mark"), (int)res.get("aiqid"));
            // Compute mark of assignment
            mark = (double)conn.executeScalar(
                "UPDATE pals_assignment_instance AS ai SET mark = "
                + "(SELECT ("
                + "(SELECT SUM((aiq.mark/100.0)*aq.weight) FROM pals_assignment_instance_question AS aiq LEFT OUTER JOIN pals_assignment_questions AS aq ON aq.aqid=aiq.aqid WHERE aiq.aiid=ai.aiid)"
                + "/"
                + "(SELECT SUM(aq.weight) FROM pals_assignment_questions AS aq WHERE aq.assid=ai.assid))"
                + "*100.0)"
                + "WHERE ai.aiid=? RETURNING CAST(mark AS double precision);",
                    aiid
            );
            // Update this model
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
     * @param status Sets the status of this instance.
     */
    public void setStatus(Status status)
    {
        this.status = status;
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
     * @return The status of this instance.
     */
    public Status getStatus()
    {
        return status;
    }
    /**
     * @return The mark of the instance.
     */
    public double getMark()
    {
        return mark;
    }
    /**
     * @param conn Database connector.
     * @return Indicates if the assignment requires marking; this also checks
     * the status of the assignment, since this model may be outdated.
     */
    public boolean isMarkComputationNeeded(Connector conn)
    {
        try
        {
            Result res = conn.read(
                    "SELECT (SELECT COUNT('') FROM pals_assignment_instance_question_criteria AS aiqc "
                    + "LEFT OUTER JOIN pals_assignment_instance_question AS aiq ON aiq.aiqid=aiqc.aiqid "
                    +"WHERE aiq.aiid=? AND NOT aiqc.status=?) AS unmarked, (SELECT status FROM pals_assignment_instance WHERE aiid=?) AS status;", aiid, InstanceAssignmentCriteria.Status.Marked.dbValue, aiid);
            return res.next() && (long)res.get("unmarked") == 0 && Status.parse((int)res.get("status")) == Status.Submitted;
        }
        catch(DatabaseException ex)
        {
            return false;
        }
    }
    // Methods - Accessors - Static ********************************************
    /**
     * @param conn Database connector.
     * @param assignment The assignment being taken; cannot be null.
     * @param user The user taking the assignment; cannot be null.
     * @return Instance of a model or null.
     */
    public static InstanceAssignment getLastAssignment(Connector conn, Assignment assignment, User user)
    {
        try
        {
            Result res = conn.read("SELECT * FROM pals_assignment_instance WHERE userid=? AND assid=? ORDER BY aiid DESC LIMIT 1;", user.getUserID(), assignment.getAssID());
            return res.next() ? load(conn, assignment, user, res) : null;
        }
        catch(DatabaseException ex)
        {
            return null;
        }
    }
    /**
     * @param conn Database connector.
     * @param ass The assignment.
     * @param user The user.
     * @return The number of times a user has attempted an assignment.
     */
    public static int getAttempts(Connector conn, Assignment ass, User user)
    {
        try
        {
            return (int)(long)conn.executeScalar("SELECT COUNT('') FROM pals_assignment_instance WHERE userid=? AND assid=?;", user.getUserID(), ass.getAssID());
        }
        catch(DatabaseException ex)
        {
            return -1;
        }
    }
}
