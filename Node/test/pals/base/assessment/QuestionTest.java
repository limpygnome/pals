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
import pals.base.database.Connector;

/**
 * Tests {@link Question}.
 * 
 * @version 1.0
 */
public class QuestionTest extends TestWithCore
{
    /**
     * Tests mutators and accessors.
     * 
     * @since 1.0
     */
    @Test
    public void testMutatorsAccessors()
    {
        TypeQuestion tq = new TypeQuestion(UUID.generateVersion4(), core.getPlugins().getPlugins()[0].getUUID(), "unit test question", "question-test");        
        Question q = new Question(tq, "test question", "description", null);
        
        assertEquals(-1, q.getQID());
        
        // Test constructor
        assertEquals(tq, q.getQtype());
        assertEquals(tq, tq);
        
        // Test mutators
        q.setQtype(null);
        assertNull(q.getQtype());
        
        q.setQtype(tq);
        assertEquals(tq, q.getQtype());
        
        q.setTitle("title");
        assertEquals("title", q.getTitle());
    }
    /**
     * Tests creating, loading and deleting. It also tests dependent
     * assignments.
     * 
     * @since 1.0
     */
    @Test
    public void testCreateLoadDeleteDependentAssignments()
    {
        Connector conn = core.createConnector();
        
        TypeQuestion tq = new TypeQuestion(UUID.generateVersion4(), core.getPlugins().getPlugins()[0].getUUID(), "unit test question", "question-test");
        assertEquals(TypeQuestion.PersistStatus.Success, tq.persist(conn));
        
        // Create
        Question q = new Question(tq, "test question", "description", null);
        
        q.setTitle(null);
        assertEquals(Question.PersistStatus.Invalid_Title, q.persist(conn));
        q.setTitle("test");
        
        q.setQtype(null);
        assertEquals(Question.PersistStatus.Invalid_QuestionType, q.persist(conn));
        q.setQtype(new TypeQuestion()); // Test unpersisted qtype
        assertEquals(Question.PersistStatus.Invalid_QuestionType, q.persist(conn));
        q.setQtype(tq);
        
        assertEquals(Question.PersistStatus.Success, q.persist(conn));
        
        // Load
        int qid = q.getQID();
        q = Question.load(core, conn, qid);
        assertNotNull(q);
        assertEquals("test", q.getTitle());
        assertEquals(tq, q.getQtype());
        assertEquals(qid, q.getQID());
        assertTrue(q.getQID() >= 0);
        
        // Test dependent assignments
        assertEquals(0, q.getDependentAssignments(conn));
        
        Module m = new Module("test");
        assertEquals(Module.PersistStatus.Success, m.persist(conn));
        Assignment ass = new Assignment(m, "title", 100, false, -1, null, true);
        assertEquals(Assignment.PersistStatus.Success, ass.persist(conn));
        TypeCriteria ctype = new TypeCriteria(UUID.generateVersion4(), core.getPlugins().getPlugins()[0].getUUID(), "test crit", "test desc");
        assertEquals(TypeCriteria.PersistStatus.Success, ctype.persist(conn));
        QuestionCriteria qc = new QuestionCriteria(q, ctype, "title", null, 100);
        assertEquals(QuestionCriteria.PersistStatus.Success, qc.persist(conn));
        AssignmentQuestion aq = new AssignmentQuestion(ass, q, 100, 1, 1);
        assertEquals(AssignmentQuestion.PersistStatus.Success, aq.persist(conn));
        
        assertEquals(1, q.getDependentAssignments(conn));
        
        assertTrue(aq.delete(conn));
        assertEquals(0, q.getDependentAssignments(conn));
        
        assertTrue(ass.delete(conn));
        assertEquals(0, q.getDependentAssignments(conn));
        
        assertTrue(qc.delete(conn));
        assertTrue(ctype.delete(conn));
        assertTrue(m.delete(conn));
        
        // Remove
        assertTrue(q.delete(conn));
        
        // Attempt reload
        q = Question.load(core, conn, qid);
        assertNull(q);
        
        conn.disconnect();
    }
}
