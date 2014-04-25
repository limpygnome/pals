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
 * Tests {@link QuestionCriteria}.
 * 
 * @version 1.0
 */
public class QuestionCriteriaTest extends TestWithCore
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
        
        // Create test data
        TypeCriteria tc = new TypeCriteria(UUID.generateVersion4(), core.getPlugins().getPlugins()[0].getUUID(), "ctype qct", "qc test");
        assertEquals(TypeCriteria.PersistStatus.Success, tc.persist(conn));
        
        // Setup initial model
        Question q = new Question();
        Object o = new Object();
        
        QuestionCriteria qc = new QuestionCriteria(q, tc, "title", o, 100);
        
        assertEquals(-1, qc.getQCID());
        
        // Test constructor
        assertEquals(q, qc.getQuestion());
        assertEquals(tc, qc.getCriteria());
        assertEquals("title", qc.getTitle());
        assertEquals(o, qc.getData());
        assertEquals(100, qc.getWeight());
        
        // Test mutators
        qc.setQuestion(null);
        assertNull(qc.getQuestion());
        qc.setQuestion(q);
        assertEquals(q, qc.getQuestion());
        
        qc.setCriteria(null);
        assertNull(qc.getCriteria());
        qc.setCriteria(tc);
        assertEquals(tc, qc.getCriteria());
        
        qc.setTitle("abc");
        assertEquals("abc", qc.getTitle());
        
        qc.setData(null);
        assertNull(qc.getData());
        
        qc.setWeight(123);
        assertEquals(123, qc.getWeight());
        
        // Dispose test data
        assertTrue(tc.delete(conn));
        
        conn.disconnect();
    }
    /**
     * Tests creating, loading and deleting question criterias.
     * 
     * @since 1.0
     */
    @Test
    public void testCreateLoadDelete()
    {
        Connector conn = core.createConnector();
        
        // Create test data
        TypeQuestion tq = new TypeQuestion(UUID.generateVersion4(), core.getPlugins().getPlugins()[0].getUUID(), "title qct", "qc test");
        assertEquals(TypeQuestion.PersistStatus.Success, tq.persist(conn));
        Question q = new Question(tq, "unit test q", "desc", null);
        assertEquals(Question.PersistStatus.Success, q.persist(conn));
        TypeCriteria tc = new TypeCriteria(UUID.generateVersion4(), core.getPlugins().getPlugins()[0].getUUID(), "tc title", "desc");
        assertEquals(TypeCriteria.PersistStatus.Success, tc.persist(conn));
        
        // Create
        QuestionCriteria qc = new QuestionCriteria(q, tc, "title qc", null, 100);
        
        qc.setQuestion(null);
        assertEquals(QuestionCriteria.PersistStatus.Invalid_Question, qc.persist(conn));
        qc.setQuestion(q);
        
        qc.setCriteria(null);
        assertEquals(QuestionCriteria.PersistStatus.Invalid_Criteria, qc.persist(conn));
        qc.setCriteria(tc);
        
        qc.setTitle(null);
        assertEquals(QuestionCriteria.PersistStatus.Invalid_Title, qc.persist(conn));
        qc.setTitle("title qc");
        
        qc.setWeight(0);
        assertEquals(QuestionCriteria.PersistStatus.Invalid_Weight, qc.persist(conn));
        qc.setWeight(100);
        
        assertEquals(QuestionCriteria.PersistStatus.Success, qc.persist(conn));
        
        // Load
        int qcid = qc.getQCID();
        qc = QuestionCriteria.load(core, conn, q, qcid);
        
        assertNotNull(qc);
        assertTrue(qc.getQCID()>=0);
        assertEquals(qcid, qc.getQCID());
        
        assertEquals(tc, qc.getCriteria());
        assertEquals(q, qc.getQuestion());
        assertEquals("title qc", qc.getTitle());
        assertEquals(100, qc.getWeight());
        
        // Delete
        assertTrue(qc.delete(conn));
        
        // Reload
        qc = QuestionCriteria.load(core, conn, q, qcid);
        assertNull(qc);
        
        // Dispose test data
        assertTrue(tc.delete(conn));
        assertTrue(q.delete(conn));
        assertTrue(tq.delete(conn));
        
        conn.disconnect();
    }
}
