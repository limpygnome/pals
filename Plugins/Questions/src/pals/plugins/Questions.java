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
package pals.plugins;

import java.util.Arrays;
import pals.base.NodeCore;
import pals.base.Plugin;
import pals.base.Settings;
import pals.base.TemplateManager;
import pals.base.UUID;
import pals.base.Version;
import pals.base.WebManager;
import pals.base.assessment.Assignment;
import pals.base.assessment.Question;
import pals.base.assessment.QuestionCriteria;
import pals.base.assessment.TypeCriteria;
import pals.base.assessment.TypeQuestion;
import pals.base.database.Connector;
import pals.base.utils.JarIO;
import pals.base.web.MultipartUrlParser;
import pals.base.web.RemoteRequest;
import pals.base.web.WebRequestData;
import pals.base.web.security.CSRF;
import pals.plugins.web.Captcha;

/**
 * The default web-interface for questions
 */
public class Questions extends Plugin
{
    // Methods - Constructors **************************************************
    public Questions(NodeCore core, UUID uuid, JarIO jario, Version version, Settings settings, String jarPath)
    {
        super(core, uuid, jario, version, settings, jarPath);
    }
    // Methods - Event Handlers ************************************************
    @Override
    public boolean eventHandler_pluginInstall(NodeCore core, Connector conn)
    {
        return true;
    }
    @Override
    public boolean eventHandler_pluginUninstall(NodeCore core, Connector conn)
    {
        return true;
    }
    @Override
    public boolean eventHandler_registerTemplates(NodeCore core, TemplateManager manager)
    {
        if(!manager.load(this, "templates"))
            return false;
        return true;
    }
    @Override
    public boolean eventHandler_registerUrls(NodeCore core, WebManager web)
    {
        if(!web.urlsRegister(this, new String[]{
            "admin/questions"
        }))
            return false;
        return true;
    }
    @Override
    public boolean eventHandler_pluginLoad(NodeCore core)
    {
        return true;
    }
    @Override
    public void eventHandler_pluginUnload(NodeCore core)
    {
        // Unregister URLs
        core.getWebManager().urlsUnregister(this);
        // Unregister templates
        core.getTemplates().remove(this);
    }
    @Override
    public boolean eventHandler_webRequest(WebRequestData data)
    {
        MultipartUrlParser mup = new MultipartUrlParser(data);
        String temp, temp2;
        switch(mup.getPart(0))
        {
            case "admin":
                switch(mup.getPart(1))
                {
                    case "questions":
                    {
                        // Check the user is allowed access
                        if(data.getUser() == null || !data.getUser().getGroup().isAdminQuestions())
                            return false;
                        // Continue processing their request...
                        temp = mup.getPart(2);
                        if(temp == null)
                            // View all the questions
                            return pageAdminQuestions_viewAll(data);
                        else
                        {
                            switch(temp)
                            {
                                case "create":
                                    // Create a question
                                    return pageAdminQuestions_create(data);
                                default:
                                    // Assume it's a question - delegate to question handler
                                    return pageAdminQuestion(data, temp, mup);
                            }
                        }
                    }
                }
                break;
        }
        return false;
    }
    @Override
    public String getTitle()
    {
        return "PALS [WEB]: Questions";
    }
    // Methods - Pages *********************************************************
    private boolean pageAdminQuestions_viewAll(WebRequestData data)
    {
        final int QUESTIONS_PER_PAGE = 10;
        // Check if we're in browse-mode for modules - if so, load the assignment
        RemoteRequest req = data.getRequestData();
        String assid = req.getField("assid");
        Assignment ass = null;
        if(assid != null)
        {
            // Load the model
            try
            {
                ass = Assignment.load(data.getConnector(), null, Integer.parseInt(assid));
            }
            catch(NumberFormatException ex)
            {
            }
            // Check it loaded, else invalid request...
            if(ass == null)
                return false;
        }
        // Fetch the page of questions being viewed
        int page = 1;
        try
        {
            if((page = Integer.parseInt(req.getField("page"))) < 0)
                page = 1;
        }
        catch(NumberFormatException ex)
        {
        }
        // Fetch questions
        String filter = req.getField("filter");
        Question[] questions = Question.load(data.getCore(), data.getConnector(), filter, QUESTIONS_PER_PAGE+1, (page-1)*QUESTIONS_PER_PAGE);
        // Setup the page
        data.setTemplateData("pals_title", "Admin - Questions");
        data.setTemplateData("pals_content", "questions/admin_questions");
        // -- Fields
        data.setTemplateData("questions", questions.length > QUESTIONS_PER_PAGE ? Arrays.copyOf(questions, QUESTIONS_PER_PAGE) : questions);
        data.setTemplateData("questions_page", page);
        data.setTemplateData("filter", filter);
        if(ass != null)
            data.setTemplateData("assignment", ass);
        if(page > 1)
            data.setTemplateData("questions_prev", page-1);
        if(page < Integer.MAX_VALUE && questions.length == QUESTIONS_PER_PAGE+1)
            data.setTemplateData("questions_next", page+1);
        return true;
    }
    private boolean pageAdminQuestions_create(WebRequestData data)
    {
        // Fetch the available question-types
        TypeQuestion[] types = TypeQuestion.loadAll(data.getConnector());
        // Check postback
        RemoteRequest req = data.getRequestData();
        String questionTitle = req.getField("question_title");
        String questionType = req.getField("question_type");
        if(questionTitle != null && questionType != null)
        {
            if(!CSRF.isSecure(data))
                data.setTemplateData("error", "Invalid request; please try again or contact an administrator!");
            else
            {
                // Attempt to persist the data
                Question q = new Question(TypeQuestion.load(data.getConnector(), UUID.parse(questionType)), questionTitle, null);
                Question.PersistStatus ps = q.persist(data.getConnector());
                switch(ps)
                {
                    case Failed:
                    case Failed_Serialize:
                    case Invalid_QuestionType:
                        data.setTemplateData("error", "An unknown error occurred ('"+ps.name()+"'); please try again or contact an administrator!");
                        break;
                    case Invalid_Title:
                        data.setTemplateData("error", "Title must be "+q.getTitleMin()+" to "+q.getTitleMax()+" characters in length!");
                        break;
                    case Success:
                        data.getResponseData().setRedirectUrl("/admin/questions/"+q.getQID()+"/edit");
                        break;
                }
            }
        }
        // Setup the page
        data.setTemplateData("pals_title", "Admin - Questions - Create");
        data.setTemplateData("pals_content", "questions/admin_question_create");
        // -- Fields
        data.setTemplateData("question_types", types);
        data.setTemplateData("question_title", questionTitle);
        data.setTemplateData("question_type", questionType);
        data.setTemplateData("csrf", CSRF.set(data));
        return true;
    }
    private boolean pageAdminQuestion(WebRequestData data, String rawQid, MultipartUrlParser mup)
    {
        // Load question model
        Question q;
        try
        {
            q = Question.load(data.getCore(), data.getConnector(), Integer.parseInt(rawQid));
            if(q == null)
                return false;
        }
        catch(NumberFormatException ex)
        {
            return false;
        }
        // Delegate to the correct page
        String page = mup.getPart(3);
        if(page == null)
            return pageAdminQuestion_view(data, q);
        else
        {
            switch(page)
            {
                case "edit":
                    return pageAdminQuestion_edit(data, q);
                case "delete":
                    return pageAdminQuestion_delete(data, q);
                case "criteria":
                    page = mup.getPart(4);
                    switch(page)
                    {
                        case "add":
                            return pageAdminQuestion_criteriaAdd(data, q);
                        default:
                            // Assume part 4 is a qcid
                            switch(mup.getPart(5))
                            {
                                case "edit":
                                    return pageAdminQuestion_criteriaEdit(data, q, page);
                                case "delete":
                                    return pageAdminQuestion_criteriaDelete(data, q, page);
                            }
                    }
                    break;
            }
        }
        return false;
    }
    private boolean pageAdminQuestion_delete(WebRequestData data, Question q)
    {
        // Check postback
        RemoteRequest req = data.getRequestData();
        String questionDelete = req.getField("question_delete");
        if(questionDelete != null && questionDelete.equals("1"))
        {
            // Verify security
            if(!CSRF.isSecure(data))
                data.setTemplateData("error", "Invalid request; please try again or contact an administrator!");
            else if(!Captcha.isCaptchaCorrect(data))
                data.setTemplateData("error", "Invalid captcha verification code!");
            else
            {
                // Delete the question
                if(q.delete(data.getConnector()))
                {
                    // Inform nodes
                    data.getCore().getRMI().nodesGlobalEventAll("base.cleaner.wake", new Object[]{});
                    // Redirect
                    data.getResponseData().setRedirectUrl("/admin/questions");
                }
                else
                    data.setTemplateData("error", "Could not delete the question, an unknown error occurred!");
            }
        }
        // Setup the page
        data.setTemplateData("pals_title", "Admin - Questions - Delete");
        data.setTemplateData("pals_content", "questions/admin_question_delete");
        // -- Fields
        data.setTemplateData("question", q);
        data.setTemplateData("csrf", CSRF.set(data));
        return true;
    }
    private boolean pageAdminQuestion_view(WebRequestData data, Question q)
    {
        // Fetch criterias
        QuestionCriteria[] qc = QuestionCriteria.loadAll(data.getCore(), data.getConnector(), q);
        // Add the cumulative weight
        int total = 0;
        for(QuestionCriteria c : qc)
            total += c.getWeight();
        // Setup the page
        data.setTemplateData("pals_title", "Admin - Questions - View");
        data.setTemplateData("pals_content", "questions/admin_question_view");
        // -- Fields
        data.setTemplateData("question", q);
        data.setTemplateData("criterias", qc);
        data.setTemplateData("total_weight", total);
        return true;
    }
    private boolean pageAdminQuestion_edit(WebRequestData data, Question q)
    {
        // Find the question-type plugin responsible for rendering the page
        Plugin p = data.getCore().getPlugins().getPlugin(q.getQtype().getUuidPlugin());
        if(p != null && p.eventHandler_handleHook("question_type.web_edit", new Object[]{data, q}))
        {
            // Handled successfully...nothing else needs to be done from here.
            return true;
        }
        // Failed - display information
        data.setTemplateData("pals_title", "Admin - Questions - Edit");
        data.setTemplateData("pals_content", "questions/admin_question_editfail");
        // -- Fields
        data.setTemplateData("question", q);
        data.setTemplateData("plugin", p);
        return true;
    }
    private boolean pageAdminQuestion_criteriaAdd(WebRequestData data, Question q)
    {
        // Check for postback
        RemoteRequest req = data.getRequestData();
        String ctype = req.getField("ctype");
        String critWeight = req.getField("crit_weight");
        String critTitle = req.getField("crit_title");
        if(ctype != null && critWeight != null && critTitle != null)
        {
            try
            {
                int weight;
                if((weight = Integer.parseInt(critWeight)) <= 0)
                    data.setTemplateData("error", "The weight must be greater than zero.");
                else if(!CSRF.isSecure(data))
                    data.setTemplateData("error", "Invalid request; please try again or contact an administrator!");
                else
                {
                    // Load the criteria-type
                    TypeCriteria ct = TypeCriteria.load(data.getConnector(), UUID.parse(ctype));
                    if(ct == null)
                        data.setTemplateData("error", "Invalid criteria type, could not be loaded.");
                    // Check the criteria-type is allowed for the qtype
                    else if(!TypeCriteria.isCapable(data.getConnector(), q.getQtype(), ct))
                        data.setTemplateData("error", "The specified type of criteria is unable to serve this type of question!");
                    else
                    {
                        // Attempt to create a new question criteria
                        QuestionCriteria qc = new QuestionCriteria(q, ct, critTitle, null, weight);
                        QuestionCriteria.PersistStatus psqc = qc.persist(data.getConnector());
                        switch(psqc)
                        {
                            case Failed:
                            case Failed_Serialize:
                                data.setTemplateData("error", "Failed to add the criteria ('"+psqc.name()+"')...");
                                break;
                            case Invalid_Criteria:
                                data.setTemplateData("error", "Invalid criteria for this type of question.");
                                break;
                            case Invalid_Question:
                                data.setTemplateData("error", "Invalid question.");
                                break;
                            case Invalid_Weight:
                                data.setTemplateData("error", "Invalid weight; must be a numeric value greater than zero!");
                                break;
                            case Invalid_Title:
                                data.setTemplateData("error", "Invalid title; must be "+qc.getTitleMin()+" to "+qc.getTitleMax()+" characters in length!");
                                break;
                            case Success:
                                data.getResponseData().setRedirectUrl("/admin/questions/"+q.getQID()+"/criteria/"+qc.getQCID()+"/edit");
                                break;
                        }
                    }
                }
            }
            catch(NumberFormatException ex)
            {
                data.setTemplateData("error", "Invalid weight; must be a numeric value.");
            }
        }
        // Fetch available criteria for this question-type
        TypeCriteria[] criterias = TypeCriteria.loadAll(data.getConnector(), q.getQtype());
        // Setup the page
        data.setTemplateData("pals_title", "Admin - Questions - Criteria - Add");
        data.setTemplateData("pals_content", "questions/admin_question_criteria_add");
        // -- Fields
        data.setTemplateData("csrf", CSRF.set(data));
        data.setTemplateData("criterias", criterias);
        data.setTemplateData("question", q);
        data.setTemplateData("crit_title", critTitle);
        data.setTemplateData("crit_weight", critWeight);
        data.setTemplateData("ctype", ctype);
        return true;
    }
    private boolean pageAdminQuestion_criteriaEdit(WebRequestData data, Question q, String rawQcid)
    {
        // Load the criteria model
        int qcid;
        try
        {
            qcid = Integer.parseInt(rawQcid);
        }
        catch(NumberFormatException ex)
        {
            return false;
        }
        QuestionCriteria qc = QuestionCriteria.load(data.getCore(), data.getConnector(), null, qcid);
        if(qc == null || qc.getQuestion().getQID() != q.getQID()) // Check the criteria belongs to the current question too!
            return false;
        // Find the plugin responsible for handling the critera-type
        Plugin p = data.getCore().getPlugins().getPlugin(q.getQtype().getUuidPlugin());
        if(p != null && p.eventHandler_handleHook("criteria_type.web_edit", new Object[]{data, qc}))
        {
            // Handled successfully...nothing else needs to be done from here.
            return true;
        }
        // Failed - display information
        data.setTemplateData("pals_title", "Admin - Questions - Criteria - Edit");
        data.setTemplateData("pals_content", "questions/admin_question_criteria_editfail");
        // -- Fields
        data.setTemplateData("criteria", qc);
        data.setTemplateData("question", q);
        data.setTemplateData("plugin", p);
        return true;
    }
    private boolean pageAdminQuestion_criteriaDelete(WebRequestData data, Question q, String rawQcid)
    {
        // Load the criteria model
        int qcid;
        try
        {
            qcid = Integer.parseInt(rawQcid);
        }
        catch(NumberFormatException ex)
        {
            return false;
        }
        QuestionCriteria qc = QuestionCriteria.load(data.getCore(), data.getConnector(), null, qcid);
        if(qc == null || qc.getQuestion().getQID() != q.getQID()) // Check the criteria belongs to the current question too!
            return false;
        // Check confirmation of deletion
        RemoteRequest req = data.getRequestData();
        String delete = req.getField("delete");
        if(delete != null && delete.equals("1"))
        {
            // Validate security
            if(!CSRF.isSecure(data))
                data.setTemplateData("error", "Invalid request; please try again or contact an administrator!");
            else if(!Captcha.isCaptchaCorrect(data))
                data.setTemplateData("error", "Invalid captcha verification code!");
            // Delete the model
            else if(!qc.delete(data.getConnector()))
                data.setTemplateData("error", "Could not remove model, an unknown error occurred; if this continues, contact an administrator!");
            else
            {
                // Inform nodes
                data.getCore().getRMI().nodesGlobalEventAll("base.cleaner.wake", new Object[]{});
                // Redirect
                data.getResponseData().setRedirectUrl("/admin/questions/"+q.getQID());
            }
        }
        // Setup the page
        data.setTemplateData("pals_title", "Admin - Questions - Criteria - Delete");
        data.setTemplateData("pals_content", "questions/admin_question_criteria_delete");
        // -- Fields
        data.setTemplateData("csrf", CSRF.set(data));
        data.setTemplateData("question", q);
        data.setTemplateData("criteria", qc);
        return true;
    }
}
