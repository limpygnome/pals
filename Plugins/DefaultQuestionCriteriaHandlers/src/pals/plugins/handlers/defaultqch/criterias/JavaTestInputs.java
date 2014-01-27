package pals.plugins.handlers.defaultqch.criterias;

import java.util.HashMap;
import pals.base.NodeCore;
import pals.base.Storage;
import pals.base.UUID;
import pals.base.assessment.InstanceAssignment;
import pals.base.assessment.InstanceAssignmentCriteria;
import pals.base.assessment.InstanceAssignmentQuestion;
import pals.base.assessment.QuestionCriteria;
import pals.base.database.Connector;
import pals.base.web.RemoteRequest;
import pals.base.web.WebRequestData;
import pals.base.web.security.CSRF;
import pals.plugins.handlers.defaultqch.data.TestInputs_Criteria;
import pals.plugins.handlers.defaultqch.java.CompilerResult;
import pals.plugins.handlers.defaultqch.java.Utils;

/**
 * Handles text inputs criteria marking.
 */
public class JavaTestInputs
{
    // Constants ***************************************************************
    public static final UUID UUID_CTYPE = UUID.parse("49d5b5fa-e0b9-427a-8171-04e7ae33fe64");
    // Methods *****************************************************************
    public static boolean pageCriteriaEdit(WebRequestData data, QuestionCriteria qc)
    {
        // Load criteria data
        TestInputs_Criteria cdata = (TestInputs_Criteria)qc.getData();
        if(cdata == null)
            cdata = new TestInputs_Criteria();
        // Check for postback
        RemoteRequest req = data.getRequestData();
        String critTitle = req.getField("crit_title");
        String critWeight = req.getField("crit_weight");
        String critClassName = req.getField("crit_class_name");
        String critMethod = req.getField("crit_method");
        String critTestCode = req.getField("crit_test_code");
        String critInputTypes = req.getField("crit_input_types");
        String critInputs = req.getField("crit_inputs");
        String critForceCompile = req.getField("crit_force_compile");
        if(critTitle != null && critWeight != null && critClassName != null && critMethod != null && critTestCode != null && critInputTypes != null && critInputs != null)
        {
            boolean compile = !critTestCode.equals(cdata.getTestCode()) || (critForceCompile != null && critForceCompile.equals("1"));
            if(compile)
            {
                String className = Utils.parseFullClassName(critTestCode);
                if(className == null)
                    data.setTemplateData("error", "Cannot compile test-code, unable to determine the full class-name.");
                else if(!className.equals(critClassName))
                    data.setTemplateData("error", "Class-name of test-code ('"+className+"') does not match the provided class-name ('"+critClassName+"'); these must be the same.");
                else
                {
                    HashMap<String,String> codeMap = new HashMap<>();
                    codeMap.put(className, critTestCode);
                    // Attempt to compile the code
                    CompilerResult cr = Utils.compile(data.getCore(), Storage.getPath_tempQC(data.getCore().getPathShared(), qc), codeMap);
                    CompilerResult.CompileStatus cs = cr.getStatus();
                    switch(cs)
                    {
                        case Unknown:
                            data.setTemplateData("warning", cs.getText());
                            break;
                        case Failed_CompilerNotFound:
                        case Failed_TempDirectory:
                        case Failed:
                            data.setTemplateData("error", cs.getText());
                            data.setTemplateData("error_messages", cr.getCodeErrors());
                            break;
                        case Success:
                            data.setTemplateData("info", cs.getText());
                            break;
                    }
                }
            }
            // Update data model
            cdata.setClassName(critClassName);
            cdata.setMethod(critMethod);
            cdata.setTestCode(critTestCode);
            cdata.setInputTypes(critInputTypes);
            cdata.setInputs(critInputs);
            // Handle entire process
            CriteriaHelper.handle_criteriaEditPostback(data, qc, critTitle, critWeight, cdata);
        }
        // Setup the page
        data.setTemplateData("pals_title", "Admin - Questions - Edit Criteria");
        data.setTemplateData("pals_content", "defaultqch/criteria/javatestinputs_edit");
        Utils.pageHookCodeMirror_Java(data);
        // -- Fields
        data.setTemplateData("criteria", qc);
        data.setTemplateData("question", qc.getQuestion());
        data.setTemplateData("csrf", CSRF.set(data));
        data.setTemplateData("crit_title", critTitle != null ? critTitle : qc.getTitle());
        data.setTemplateData("crit_weight", critWeight != null ? critWeight : qc.getWeight());
        data.setTemplateData("crit_class_name", critClassName != null ? critClassName : cdata.getClassName());
        data.setTemplateData("crit_method", critMethod != null ? critMethod : cdata.getMethod());
        data.setTemplateData("crit_test_code", critTestCode != null ? critTestCode : cdata.getTestCode());
        data.setTemplateData("crit_input_types", critInputTypes != null ? critInputTypes : cdata.getInputTypesWeb());
        data.setTemplateData("crit_inputs", critInputs != null ? critInputs : cdata.getInputsWeb());
        return true;
    }
    public static boolean criteriaMarking(Connector conn, NodeCore core, InstanceAssignmentCriteria iac)
    {
        return false;
    }
    public static boolean criteriaDisplay(WebRequestData data, InstanceAssignment ia, InstanceAssignmentQuestion iaq, InstanceAssignmentCriteria iac, StringBuilder html)
    {
        return false;
    }
}
