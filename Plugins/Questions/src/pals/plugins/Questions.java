package pals.plugins;

import pals.base.NodeCore;
import pals.base.Plugin;
import pals.base.Settings;
import pals.base.TemplateManager;
import pals.base.UUID;
import pals.base.WebManager;
import pals.base.assessment.Question;
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
    
    // Fields ******************************************************************
    
    // Methods - Constructors **************************************************
    public Questions(NodeCore core, UUID uuid, JarIO jario, Settings settings, String jarPath)
    {
        super(core, uuid, jario, settings, jarPath);
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
                                    if((temp2 = mup.getPart(3)) == null)
                                        // View a question
                                        return pageAdminQuestions_view(data, temp);
                                    else
                                    {
                                        switch(temp2)
                                        {
                                            // Delete a question
                                            case "delete":
                                                return pageAdminQuestions_delete(data, temp);
                                            // Modify question-type properties
                                            case "edit":
                                                ;
                                            // Add a criteria to the question
                                            case "add_criteria":
                                                ;
                                        }
                                    }
                            }
                        }
                    break;
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
    public boolean pageAdminQuestions_viewAll(WebRequestData data)
    {
        final int QUESTIONS_PER_PAGE = 10;
        // Check permissions
        if(data.getUser() == null || !data.getUser().getGroup().isAdminModules())
            return false;
        // Fetch the page of questions being viewed
        int page = 1;
        try
        {
            if((page = Integer.parseInt(data.getRequestData().getField("page"))) < 0)
                page = 1;
        }
        catch(NumberFormatException ex)
        {
        }
        // Fetch questions
        Question[] questions = Question.load(data.getConnector(), QUESTIONS_PER_PAGE+1, (page-1)*QUESTIONS_PER_PAGE);
        // Setup the page
        data.setTemplateData("pals_title", "Admin - Questions");
        data.setTemplateData("pals_content", "questions/admin_questions");
        // -- Fields
        data.setTemplateData("questions", questions);
        data.setTemplateData("questions_page", page);
        if(page > 1)
            data.setTemplateData("questions_prev", page-1);
        if(page < Integer.MAX_VALUE && questions.length == QUESTIONS_PER_PAGE+1)
            data.setTemplateData("questions_next", page+1);
        return true;
    }
    public boolean pageAdminQuestions_create(WebRequestData data)
    {
        // Check permissions
        if(data.getUser() == null || !data.getUser().getGroup().isAdminModules())
            return false;
        // Fetch the available question-types
        TypeQuestion[] types = TypeQuestion.loadAll(data.getConnector());
        // Check postback
        RemoteRequest req = data.getRequestData();
        String questionTitle = req.getField("question_title");
        String questionType = req.getField("question_type");
        String csrf = req.getField("csrf");
        if(questionTitle != null && questionType != null)
        {
            if(!CSRF.isSecure(data, csrf))
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
                        data.getResponseData().setRedirectUrl("/admin/questions/"+q.getQID());
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
    public boolean pageAdminQuestions_delete(WebRequestData data, String rawQid)
    {
        // Check permissions
        if(data.getUser() == null || !data.getUser().getGroup().isAdminModules())
            return false;
        // Load the model being deleted
        Question q;
        try
        {
            q = Question.load(data.getConnector(), Integer.parseInt(rawQid));
            if(q == null)
                return false;
        }
        catch(NumberFormatException ex)
        {
            return false;
        }
        // Check postback
        RemoteRequest req = data.getRequestData();
        String questionDelete = req.getField("question_delete");
        String csrf = req.getField("csrf");
        if(questionDelete != null && questionDelete.equals("1"))
        {
            // Verify security
            if(!CSRF.isSecure(data, csrf))
                data.setTemplateData("error", "Invalid request; please try again or contact an administrator!");
            else if(!Captcha.isCaptchaCorrect(data))
                data.setTemplateData("error", "Invalid captcha verification code!");
            else
            {
                // Delete the question
                if(q.remove(data.getConnector()))
                    data.getResponseData().setRedirectUrl("/admin/questions");
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
    public boolean pageAdminQuestions_view(WebRequestData data, String rawQid)
    {
        // Check permissions
        if(data.getUser() == null || !data.getUser().getGroup().isAdminModules())
            return false;
        // Fetch criterias
        return true;
    }
    public boolean pageAdminQuestions_edit(WebRequestData data, String rawQid)
    {
        // Check permissions
        if(data.getUser() == null || !data.getUser().getGroup().isAdminModules())
            return false;
        // Load question model
        // Find the question-type plugin responsible for rendering the page
        // Invoke handler to handle request
        return true;
    }
}
