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

import java.util.HashMap;
import java.util.TreeMap;
import pals.base.NodeCore;
import pals.base.Storage;
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
import pals.plugins.handlers.defaultqch.criterias.JavaTestInputs;
import pals.plugins.handlers.defaultqch.data.CodeJava_Instance;
import pals.plugins.handlers.defaultqch.data.CodeJava_Question;
import pals.plugins.handlers.defaultqch.data.CodeJava_Shared;
import pals.plugins.handlers.defaultqch.data.JavaTestInputs_Criteria;
import pals.plugins.handlers.defaultqch.java.CompilerResult;
import pals.plugins.handlers.defaultqch.java.Utils;
import pals.plugins.handlers.defaultqch.questions.CodeJava;
import pals.testing.throughput.TestData;

/**
 * Creates test written response data, to be marked by a regular expression
 * (regex) criteria.
 * 
 * @version 1.0
 */
public class TestInputs extends TestData
{
    private int userid;
    private int moduleid;
    private int qid;
    private int fakeInstances;
    
    public TestInputs(int fakeInstances)
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
        TypeQuestion tq = TypeQuestion.load(conn, CodeJava.UUID_QTYPE);
        if(tq == null)
        {
            System.err.println("Failed to load type-question.");
            return;
        }
        
        // Create question
        CodeJava_Question qdata = new CodeJava_Question();
        qdata.setText("Question test.");
        Question q = new Question(tq, "Throughput Java Test", "", qdata);
        if(q.persist(conn) != Question.PersistStatus.Success)
        {
            System.err.println("Could not persist question - "+q.persist(conn)+"!");
            return;
        }
        qid = q.getQID();
        
        String code =
                "package com.example;\n"+
                "public class Test{\n"+
                "public static int sum(int a, int b){\n" +
                "return a+b; } \n" +
                "}\n\n";
        
        // Create and add criteria to question
        JavaTestInputs_Criteria cdata = new JavaTestInputs_Criteria();
        cdata.setClassName("com.example.Test");
        cdata.setHideSolution(false);
        cdata.setInputTypes("int,int");
        cdata.setInputs("1;1\n2;2\n3;3\n4;4\n5;5\n100;200\n10000;4324");
        cdata.setMethod("sum");
        cdata.setTestCode(code);
        
        QuestionCriteria qc = new QuestionCriteria(q, TypeCriteria.load(conn, JavaTestInputs.UUID_CTYPE), "Inputs criteria", cdata, 100);
        if(qc.persist(conn) != QuestionCriteria.PersistStatus.Success)
        {
            System.err.println("Failed to create question criteria - "+qc.persist(conn)+"!");
            return;
        }
        // Compile code for criteria
        HashMap<String,String> codeMap = new HashMap<>();
        codeMap.put("com.example.Test", code);
        // Attempt to compile the code
        CompilerResult cr = Utils.compile(core, Storage.getPath_tempQC(core.getPathShared(), qc), codeMap);
        CompilerResult.CompileStatus cs = cr.getStatus();
        if(cs != CompilerResult.CompileStatus.Success)
            System.err.println("Warning: failed to compile criteria.");
        
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
        CodeJava_Instance adata;
        for(int i = 0; i < fakeInstances; i++)
        {
            ia = new InstanceAssignment(u, ass, InstanceAssignment.Status.Active, null, null, 0);
            if(ia.persist(conn) != InstanceAssignment.PersistStatus.Success)
                System.err.println("Could not persist IA #"+i+" - "+ia.persist(conn)+"!");
            else
            {
                // Create initial instance of question with text
                InstanceAssignmentQuestion iaq = new InstanceAssignmentQuestion(aq, ia, null, true, 0);
                if(iaq.persist(conn) != InstanceAssignmentQuestion.PersistStatus.Success)
                    System.err.println("Could not persist IAQ #"+i+" - "+iaq.persist(conn)+"!");
                else
                {
                    // Create instance data for marking
                    adata = new CodeJava_Instance();
                    adata.setPrepared(false);
                    adata.codeAdd("com.example.Test", code);
                    // Copy question code, if available
                    TreeMap<String,String> compileCode = CodeJava_Shared.copyCode(qdata, adata);
                    // -- If fails, set answered to false.
                    cr = Utils.compile(core, Storage.getPath_tempIAQ(core.getPathShared(), iaq), compileCode);
                    // Update the model's status
                    adata.setCompileStatus(cr.getStatus());
                    if(cr.getStatus() != CompilerResult.CompileStatus.Success)
                        System.err.println("#"+i+" - could not compile code - "+cr.getStatus()+".");
                    // Update IAQ with data
                    iaq.setData(adata);
                    iaq.setAnswered(true);
                    // Re-persist data
                    if(iaq.persist(conn) != InstanceAssignmentQuestion.PersistStatus.Success)
                        System.err.println("Could not persist IAQ #"+i+" second-time - "+iaq.persist(conn)+"!");
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
