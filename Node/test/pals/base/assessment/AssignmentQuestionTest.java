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
 * Tests {@link AssignmentQuestion}.
 * 
 * @version 1.0
 */
public class AssignmentQuestionTest extends TestWithCore
{
    /**
     * Tests the mutators and accessors.
     * 
     * @since 1.0
     */
    @Test
    public void testMutatorsAccessors()
    {
        Connector conn = core.createConnector();
        
        Module m = new Module("test");
        assertEquals(Module.PersistStatus.Success, m.persist(conn));
        Assignment ass = new Assignment(m, "title", 100, false, -1, null, true);
        assertEquals(Assignment.PersistStatus.Success, ass.persist(conn));
        
        Question q = new Question();
        
        AssignmentQuestion aq = new AssignmentQuestion(ass, q, 100, 1, 2);
        
        assertEquals(-1, aq.getAQID());
        
        // Test constructor
        assertEquals(ass, aq.getAssignment());
        assertEquals(q, aq.getQuestion());
        assertEquals(100, aq.getWeight());
        assertEquals(1, aq.getPage());
        assertEquals(2, aq.getPageOrder());
        
        // Test mutators
        aq.setAssignment(null);
        assertNull(aq.getAssignment());
        
        aq.setAssignment(ass);
        assertEquals(ass, aq.getAssignment());
        
        aq.setQuestion(null);
        assertNull(aq.getQuestion());
        
        aq.setQuestion(q);
        assertEquals(q, aq.getQuestion());
        
        aq.setWeight(9001);
        assertEquals(9001, aq.getWeight());
        
        aq.setPage(123);
        assertEquals(123, aq.getPage());
        
        aq.setPageOrder(987);
        assertEquals(987, aq.getPageOrder());
        
        assertTrue(ass.delete(conn));
        assertTrue(m.delete(conn));
        
        conn.disconnect();
    }
    /**
     * Tests creating/persisting, loading and deleting.
     * 
     * @since 1.0
     */
    @Test
    public void testCreateLoadDelete()
    {
        Connector conn = core.createConnector();
        
        // Create test data
        Module m = new Module("test");
        assertEquals(Module.PersistStatus.Success, m.persist(conn));
        
        Assignment ass = new Assignment(m, "unit test ass", 100, false, -1, null, true);
        assertEquals(Assignment.PersistStatus.Success, ass.persist(conn));
        
        TypeQuestion tq = new TypeQuestion(UUID.generateVersion4(), core.getPlugins().getPlugins()[0].getUUID(), "tq aq", "desc");
        assertEquals(TypeQuestion.PersistStatus.Success, tq.persist(conn));
        
        Question q = new Question(tq, "title", "desc", null);
        assertEquals(Question.PersistStatus.Success, q.persist(conn));
        
        TypeCriteria tc = new TypeCriteria(UUID.generateVersion4(), core.getPlugins().getPlugins()[0].getUUID(), "title aq", "desc");
        assertEquals(TypeCriteria.PersistStatus.Success, tc.persist(conn));
        
        QuestionCriteria qc = new QuestionCriteria(q, tc, "qc title", null, 100);
        assertEquals(QuestionCriteria.PersistStatus.Success, qc.persist(conn));
        
        // Create aq
        AssignmentQuestion aq = new AssignmentQuestion(ass, q, 100, 1, 1);
        
        aq.setAssignment(null);
        assertEquals(AssignmentQuestion.PersistStatus.Invalid_Assignment, aq.persist(conn));
        aq.setAssignment(ass);
        
        aq.setQuestion(null);
        assertEquals(AssignmentQuestion.PersistStatus.Invalid_Question, aq.persist(conn));
        aq.setQuestion(q);
        
        aq.setWeight(-1);
        assertEquals(AssignmentQuestion.PersistStatus.Invalid_Weight, aq.persist(conn));
        aq.setWeight(100);
        
        aq.setPage(0);
        assertEquals(AssignmentQuestion.PersistStatus.Invalid_Page, aq.persist(conn));
        aq.setPage(1);
        
        aq.setPageOrder(0);
        assertEquals(AssignmentQuestion.PersistStatus.Invalid_PageOrder, aq.persist(conn));
        aq.setPageOrder(2);
        
        assertEquals(AssignmentQuestion.PersistStatus.Success, aq.persist(conn));
        
        // Load aq
        int aqid = aq.getAQID();
        aq = AssignmentQuestion.load(core, conn, ass, aqid);
        
        assertEquals(aqid, aq.getAQID());
        assertEquals(ass, aq.getAssignment());
        assertEquals(1, aq.getPage());
        assertEquals(2, aq.getPageOrder());
        assertEquals(q, aq.getQuestion());
        assertEquals(100, aq.getWeight());
        
        // Delete aq
        assertTrue(aq.delete(conn));
        
        // Attempt to reload aq
        aq = AssignmentQuestion.load(core, conn, ass, aqid);
        assertNull(aq);
        
        // Dispose data
        assertTrue(qc.delete(conn));
        assertTrue(tc.delete(conn));
        assertTrue(q.delete(conn));
        assertTrue(tq.delete(conn));
        assertTrue(ass.delete(conn));
        assertTrue(m.delete(conn));
        
        conn.disconnect();
    }
}
