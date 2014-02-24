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
package pals.base.assessment;

import java.sql.Timestamp;
import java.util.ArrayList;
import org.joda.time.DateTime;
import org.joda.time.Period;
import pals.base.Logging;
import pals.base.NodeCore;
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
        Active(0, "Active"),
        Submitted(1, "Pending Marking"),
        Marking(2, "Computing Grade"),
        Marked(3, "Marked");
        
        private final int status;
        private final String text;
        private Status(int status, String text)
        {
            this.status = status;
            this.text = text;
        }
        /**
         * @return The status of the assignment.
         */
        public int getStatus()
        {
            return status;
        }
        /**
         * @return The text representation of the status.
         */
        public String getText()
        {
            return text;
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
    private DateTime    timeStart;  // The time at which the assignment started.
    private DateTime    timeEnd;    // The time at which the assignment was submitted.
    private double      mark;       // The mark of the instance.
    // Methods - Constructors **************************************************
    /**
     * Creates a new instance of an unpersisted instance of an assignment.
     */
    public InstanceAssignment()
    {
        this(null, null, Status.Active, DateTime.now(), null, 0);
    }
    /**
     * Creates a new instance of an unpersisted instance of an assignment.
     * 
     * @param user The user who is answering this instance of the assignment.
     * @param ass The assignment instantiated.
     * @param status The status of the instance.
     * @param timeStart The time at which the assignment started.
     * @param timeEnd The time at which the assignment ended.
     * @param mark The mark of the instance.
     */
    public InstanceAssignment(User user, Assignment ass, Status status, DateTime timeStart, DateTime timeEnd, double mark)
    {
        this.aiid = -1;
        this.user = user;
        this.ass = ass;
        this.status = status;
        this.timeStart = timeStart;
        this.timeEnd = timeEnd;
        this.mark = mark;
    }
    // Methods - Persistence ***************************************************
    /**
     * Loads multiple persisted models; ordered by identifier descending.
     * 
     * @param conn Database connector.
     * @param ass  The assignment; can be null for unfiltered.
     * @param user  The user; can be null for unfiltered.
     * @param amount Number of models to retrieve.
     * @param offset Offset of rows.
     * @return Array of models; can be empty.
     */
    public static InstanceAssignment[] load(Connector conn, Assignment ass, User user, int amount, int offset)
    {
        try
        {
            Result res;
            if(ass != null && user != null)
                res = conn.read("SELECT * FROM pals_assignment_instance WHERE assid=? AND userid=? ORDER BY aiid DESC LIMIT ? OFFSET ?;", ass.getAssID(), user.getUserID(), amount, offset);
            else if(ass != null && user == null)
                res = conn.read("SELECT * FROM pals_assignment_instance WHERE assid=? ORDER BY aiid DESC LIMIT ? OFFSET ?;", ass.getAssID(), amount, offset);
            else if(ass == null && user != null)
                res = conn.read("SELECT * FROM pals_assignment_instance WHERE userid=? ORDER BY aiid DESC LIMIT ? OFFSET ?;", user.getUserID(), amount, offset);
            else
                res = conn.read("SELECT * FROM pals_assignment_instance ORDER BY aiid DESC LIMIT ? OFFSET ?;", amount, offset);
            ArrayList<InstanceAssignment> buffer = new ArrayList<>();
            InstanceAssignment ia;
            while(res.next())
            {
                if((ia = load(conn, ass, null, res)) != null)
                    buffer.add(ia);
            }
            return buffer.toArray(new InstanceAssignment[buffer.size()]);
        }
        catch(DatabaseException ex)
        {
            NodeCore core;
            if((core = NodeCore.getInstance())!=null)
                core.getLogging().logEx("Base", ex, Logging.EntryType.Warning);
            return new InstanceAssignment[0];
        }
    }
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
            Object  ts = res.get("time_start"),
                    te = res.get("time_end");
            InstanceAssignment ia = new InstanceAssignment(user, ass, Status.parse((int)res.get("status")), ts == null ? null : new DateTime(ts), te == null ? null : new DateTime(te), (double)res.get("mark"));
            ia.aiid = (int)res.get("aiid");
            return ia;
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
                aiid = (int)conn.executeScalar("INSERT INTO pals_assignment_instance (userid, assid, status, time_start, time_end, mark) VALUES(?,?,?,?,?,?) RETURNING aiid;",
                        user.getUserID(),
                        ass.getAssID(),
                        status.getStatus(),
                        timeStart != null ? new Timestamp(timeStart.toDate().getTime()) : null,
                        timeEnd != null ? new Timestamp(timeEnd.toDate().getTime()) : null,
                        mark
                        );
            }
            else
            {
                conn.execute("UPDATE pals_assignment_instance SET userid=?, assid=?, status=?, time_start=?, time_end=?, mark=? WHERE aiid=?;",
                        user.getUserID(),
                        ass.getAssID(),
                        status.getStatus(),
                        timeStart != null ? new Timestamp(timeStart.toDate().getTime()) : null,
                        timeEnd != null ? new Timestamp(timeEnd.toDate().getTime()) : null,
                        mark,
                        aiid
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
            NodeCore core;
            if((core = NodeCore.getInstance())!=null)
                core.getLogging().logEx("Base", ex, Logging.EntryType.Warning);
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
            NodeCore core;
            if((core = NodeCore.getInstance())!=null)
                core.getLogging().logEx("Base", ex, Logging.EntryType.Warning);
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
    /**
     * @param timeStart The time this instance started.
     */
    public void setTimeStart(DateTime timeStart)
    {
        this.timeStart = timeStart;
    }
    /**
     * @param timeEnd The time this instance was submitted.
     */
    public void setTimeEnd(DateTime timeEnd)
    {
        this.timeEnd = timeEnd;
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
     * @return The time at which the assignment started; can be null.
     */
    public DateTime getTimeStart()
    {
        return timeStart;
    }
    /**
     * @return The time at which the assignment was submitted; can be null.
     */
    public DateTime getTimeEnd()
    {
        return timeEnd;
    }
    /**
     * @return The method getTimeDuration, represented as a string; can be null
     * if the conditions of getTimeDuration meet null.
     */
    public String getTimeDurationStr()
    {
        Period p = timeStart != null && timeEnd != null ? new Period(timeStart, timeEnd) : null;
        return p == null ? null : p.getDays()+"d "+p.getHours()+"h "+p.getMinutes()+"m "+p.getSeconds()+"s";
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
            NodeCore core;
            if((core = NodeCore.getInstance())!=null)
                core.getLogging().logEx("Base", ex, Logging.EntryType.Warning);
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
            NodeCore core;
            if((core = NodeCore.getInstance())!=null)
                core.getLogging().logEx("Base", ex, Logging.EntryType.Warning);
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
            NodeCore core;
            if((core = NodeCore.getInstance())!=null)
                core.getLogging().logEx("Base", ex, Logging.EntryType.Warning);
            return -1;
        }
    }
}
