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

import static org.junit.Assert.*;
import org.junit.Test;
import pals.TestWithCore;
import pals.base.UUID;
import pals.base.auth.User;
import pals.base.auth.UserGroup;
import pals.base.database.Connector;

/**
 * Tests {@link InstanceAssignmentQuestion} and
 * {@link InstanceAssignmentCriteria}.
 * 
 * @version 1.0
 */
public class InstanceAssignmentQuestionCriteriaTest extends TestWithCore
{
    /**
     * Tests the mutators, accessors, creation/persisting, loading and
     * deletion of IAQ and IAC models. This has been combined into one
     * large test because they rely on each other and this is quite a
     * complicated area of testing.
     * 
     * @since 1.0
     */
    @Test
    public void testMutatorsAccessorsCreateLoadDelete()
    {
        Connector conn = core.createConnector();
        
        // Create test data
        User u = new User("user1", null, null, "user1@user1.com", UserGroup.load(conn, 1));
        assertEquals(User.PersistStatus_User.Success, u.persist(core, conn));
        Module m = new Module("test module");
        assertEquals(Module.PersistStatus.Success, m.persist(conn));
        Assignment ass = new Assignment(m, "title", 100, false, -1, null, true);
        assertEquals(Assignment.PersistStatus.Success, ass.persist(conn));
        InstanceAssignment ia = new InstanceAssignment(u, ass, InstanceAssignment.Status.Active, null, null, 100.0);
        assertEquals(InstanceAssignment.PersistStatus.Success, ia.persist(conn));
        TypeQuestion tq = new TypeQuestion(UUID.generateVersion4(), core.getPlugins().getPlugins()[0].getUUID(), "tq aq", "desc");
        assertEquals(TypeQuestion.PersistStatus.Success, tq.persist(conn));
        Question q = new Question(tq, "title", "desc", null);
        assertEquals(Question.PersistStatus.Success, q.persist(conn));
        TypeCriteria tc = new TypeCriteria(UUID.generateVersion4(), core.getPlugins().getPlugins()[0].getUUID(), "title aq", "desc");
        assertEquals(TypeCriteria.PersistStatus.Success, tc.persist(conn));
        QuestionCriteria qc = new QuestionCriteria(q, tc, "qc title", null, 100);
        assertEquals(QuestionCriteria.PersistStatus.Success, qc.persist(conn));
        AssignmentQuestion aq = new AssignmentQuestion(ass, q, 100, 1, 1);
        assertEquals(AssignmentQuestion.PersistStatus.Success, aq.persist(conn));
        // Create IAQ
        InstanceAssignmentQuestion iaq = new InstanceAssignmentQuestion(aq, ia, null, false, 15.0);
        
        assertEquals(false, iaq.isPersisted());
        assertEquals(-1, iaq.getAIQID());
        
        // -- Test constructor
        assertEquals(aq, iaq.getAssignmentQuestion());
        assertEquals(ia, iaq.getInstanceAssignment());
        assertFalse(iaq.isAnswered());
        assertEquals(15.0, iaq.getMark(), 0.0);
        
        // -- Test mutators
        iaq.setAssignmentQuestion(null);
        assertNull(iaq.getAssignmentQuestion());
        iaq.setAssignmentQuestion(aq);
        assertEquals(aq, iaq.getAssignmentQuestion());
        
        iaq.setInstanceAssignment(null);
        assertNull(iaq.getInstanceAssignment());
        iaq.setInstanceAssignment(ia);
        assertEquals(ia, iaq.getInstanceAssignment());
        
        iaq.setAnswered(false);
        assertFalse(iaq.isAnswered());
        iaq.setAnswered(true);
        assertTrue(iaq.isAnswered());
        
        iaq.setMark(10);
        assertEquals(10, iaq.getMark(), 0.0);
        
        // Persist IAQ
        assertEquals(InstanceAssignmentQuestion.PersistStatus.Success, iaq.persist(conn));
        
        // Load IAQ
        int aiqid = iaq.getAIQID();
        iaq = InstanceAssignmentQuestion.load(core, conn, ia, aiqid);
        assertNotNull(iaq);
        
        // Create IAC
        InstanceAssignmentCriteria iac = new InstanceAssignmentCriteria(iaq, qc, InstanceAssignmentCriteria.Status.Marked, 100, null);
        
        assertFalse(iac.isPersisted());
        
        // -- Test constructor
        assertEquals(iaq, iac.getIAQ());
        assertEquals(qc, iac.getQC());
        assertEquals(InstanceAssignmentCriteria.Status.Marked, iac.getStatus());
        assertEquals(100, iac.getMark());
        
        // -- Test mutators
        iac.setIAQ(null);
        assertNull(iac.getIAQ());
        iac.setIAQ(iaq);
        
        iac.setQC(null);
        assertNull(iac.getQC());
        iac.setQC(qc);
        
        iac.setStatus(InstanceAssignmentCriteria.Status.BeingAnswered);
        assertEquals(InstanceAssignmentCriteria.Status.BeingAnswered, iac.getStatus());
        
        iac.setMark(50);
        assertEquals(50, iac.getMark());
        
        // Persist IAC
        assertEquals(InstanceAssignmentCriteria.PersistStatus.Success, iac.persist(conn));
        
        // Load IAC
        iac = InstanceAssignmentCriteria.load(core, conn, iaq, qc);
        assertNotNull(iac);
        
        // Delete IAC
        assertTrue(iac.delete(conn));
        
        // Reload IAC
        iac = InstanceAssignmentCriteria.load(core, conn, iaq, qc);
        assertNull(iac);
        
        // Delete IAQ
        assertTrue(iaq.delete(conn));
        
        // Reload IAQ
        iaq = InstanceAssignmentQuestion.load(core, conn, ia, aiqid);
        assertNull(iaq);
        
        // Dispose test data
        assertTrue(aq.delete(conn));
        assertTrue(qc.delete(conn));
        assertTrue(tc.delete(conn));
        assertTrue(q.delete(conn));
        assertTrue(tq.delete(conn));
        
        assertTrue(ia.delete(conn));
        assertTrue(ass.delete(conn));
        assertTrue(m.delete(conn));
        assertTrue(u.delete(conn));
        
        conn.disconnect();
    }
}
