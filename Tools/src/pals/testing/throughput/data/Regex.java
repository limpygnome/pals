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
package pals.testing.throughput.data;

import pals.base.NodeCore;
import pals.base.assessment.Assignment;
import pals.base.assessment.AssignmentQuestion;
import pals.base.assessment.InstanceAssignment;
import pals.base.assessment.InstanceAssignmentCriteria;
import pals.base.assessment.InstanceAssignmentQuestion;
import pals.base.assessment.Module;
import pals.base.assessment.Question;
import pals.base.assessment.QuestionCriteria;
import pals.base.assessment.TypeCriteria;
import pals.base.assessment.TypeQuestion;
import pals.base.auth.User;
import pals.base.auth.UserGroup;
import pals.base.database.Connector;
import pals.plugins.handlers.defaultqch.criterias.RegexMatch;
import pals.plugins.handlers.defaultqch.data.Regex_Criteria;
import pals.plugins.handlers.defaultqch.data.Written_Question;
import pals.plugins.handlers.defaultqch.questions.WrittenResponse;
import pals.testing.throughput.TestData;

/**
 * Creates test written response data, to be marked by a regular expression
 * (regex) criteria.
 * 
 * @version 1.0
 */
public class Regex extends TestData
{
    private int userid;
    private int moduleid;
    private int qid;
    private int fakeInstances;
    
    public Regex(int fakeInstances)
    {
        this.fakeInstances = fakeInstances;
    }
    
    @Override
    public void create(NodeCore core)
    {
        Connector conn = core.createConnector();
        
        // Create fake user
        User u = new User();
        u.setEmail("test@throughput.com");
        u.setGroup(UserGroup.load(conn, 1)); // Default group - should exist
        u.setUsername("throughput");
        if(u.persist(core, conn) != User.PersistStatus_User.Success)
        {
            System.err.println("Failed to create user - "+u.persist(core, conn)+"!");
            return;
        }
        userid = u.getUserID();
        
        // Create a module
        Module m = new Module("Throughput Test Module");
        if(m.persist(conn) != Module.PersistStatus.Success)
        {
            System.err.println("Could not persist test module - "+m.persist(conn)+"!");
            return;
        }
        moduleid = m.getModuleID();
        
        // Load type question
        TypeQuestion tq = TypeQuestion.load(conn, WrittenResponse.UUID_QTYPE);
        if(tq == null)
        {
            System.err.println("Failed to load type-question.");
            return;
        }
        
        // Create question
        Written_Question qdata = new Written_Question();
        qdata.setText("Question test.");
        Question q = new Question(tq, "Throughput Regex Test", "", qdata);
        if(q.persist(conn) != Question.PersistStatus.Success)
        {
            System.err.println("Could not persist question - "+q.persist(conn)+"!");
            return;
        }
        qid = q.getQID();
        
        // Create and add criteria to question
        Regex_Criteria cdata = new Regex_Criteria();
        cdata.setRegexPattern(".+");
        QuestionCriteria qc = new QuestionCriteria(q, TypeCriteria.load(conn, RegexMatch.UUID_CTYPE), "Regex criteria", cdata, 100);
        if(qc.persist(conn) != QuestionCriteria.PersistStatus.Success)
        {
            System.err.println("Failed to create question criteria - "+qc.persist(conn)+"!");
            return;
        }
        
        // Create an assignment
        Assignment ass = new Assignment(m, "Test Assignment", 100, true, -1, null, false);
        if(ass.persist(conn) != Assignment.PersistStatus.Success)
        {
            System.err.println("Could not persist assignment - "+ass.persist(conn)+"!");
            return;
        }
        
        // Create assignment question
        AssignmentQuestion aq = new AssignmentQuestion(ass, q, 100, 1, 1);
        if(aq.persist(conn) != AssignmentQuestion.PersistStatus.Success)
        {
            System.err.println("Failed to create assignment question - "+aq.persist(conn)+"!");
            return;
        }
        
        // Create fake instances of work to mark
        InstanceAssignment ia;
        for(int i = 0; i < fakeInstances; i++)
        {
            ia = new InstanceAssignment(u, ass, InstanceAssignment.Status.Active, null, null, 0);
            if(ia.persist(conn) != InstanceAssignment.PersistStatus.Success)
                System.err.println("Could not persist IA #"+i+" - "+ia.persist(conn)+"!");
            else
            {
                // Create instance of question with text
                InstanceAssignmentQuestion iaq = new InstanceAssignmentQuestion(aq, ia, "test data to be matched", true, 0);
                if(iaq.persist(conn) != InstanceAssignmentQuestion.PersistStatus.Success)
                    System.err.println("Could not persist IAQ #"+i+" - "+iaq.persist(conn)+"!");
                else
                {
                    // Create instance of criteria
                    InstanceAssignmentCriteria iac = new InstanceAssignmentCriteria(iaq, qc, InstanceAssignmentCriteria.Status.AwaitingMarking, 0, null);
                    if(iac.persist(conn) != InstanceAssignmentCriteria.PersistStatus.Success)
                        System.err.println("Could not persist IAC #"+i+" - "+iac.persist(conn)+"!");
                }
            }
        }
        
        conn.disconnect();
    }

    @Override
    public void dispose(NodeCore core)
    {
        Connector conn = core.createConnector();
        // Dispose user
        User u = User.load(conn, userid);
        if(u == null || !u.delete(conn))
            System.err.println("Warning: could not dispose user!");
        // Dispose module - all other data will cascade
        Module m = Module.load(conn, moduleid);
        if(m == null || !m.delete(conn))
            System.err.println("Warning: could not dispose test module!");
        // Dispose question
        Question q = Question.load(core, conn, qid);
        if(q == null || !q.delete(conn))
            System.err.println("Warning: could not dispose old question!");
        conn.disconnect();
    }
}
