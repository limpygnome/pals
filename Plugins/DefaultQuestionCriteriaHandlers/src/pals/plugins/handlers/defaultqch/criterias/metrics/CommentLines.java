package pals.plugins.handlers.defaultqch.criterias.metrics;

import java.util.HashMap;
import pals.base.NodeCore;
import pals.base.assessment.InstanceAssignment;
import pals.base.assessment.InstanceAssignmentCriteria;
import pals.base.assessment.InstanceAssignmentQuestion;
import pals.base.web.WebRequestData;
import pals.plugins.handlers.defaultqch.data.CodeJava_Instance;
import pals.plugins.handlers.defaultqch.data.JavaCodeMetrics_Criteria;

/**
 * A metric for comment lines.
 */
public class CommentLines implements Metric
{
    // Fields ******************************************************************
    private int sumLines, sumComments;
    // Methods - Overrides *****************************************************
    @Override
    public void init(NodeCore core, InstanceAssignmentCriteria iac, CodeJava_Instance idata, JavaCodeMetrics_Criteria cdata)
    {
        sumLines = sumComments = 0;
    }
    @Override
    public void process(CodeJava_Instance idata, JavaCodeMetrics_Criteria cdata, String className)
    {
        String code = idata.codeGet(className);
        if(code == null)
            return;
        
        boolean insideCommentBlock = false;
        String[] lines = code.split("\n");
        String line;
        for(int i = 0; i < lines.length; i++)
        {
            line = lines[i].trim();
            if(line.length() != 0)
            {
                if(line.contains("/*") && !insideCommentBlock)
                {
                    insideCommentBlock = true;
                    sumComments++;
                }
                else if(insideCommentBlock && line.contains("*/"))
                {
                    insideCommentBlock = false;
                    sumComments++;
                }
                else if(insideCommentBlock || line.startsWith("//"))
                    sumComments++;
            }
            else if(insideCommentBlock)
                sumComments++;
            sumLines++;
        }
    }
    @Override
    public double metricComputeValue(CodeJava_Instance idata, JavaCodeMetrics_Criteria cdata, InstanceAssignmentCriteria iac)
    {
        double value = cdata.isRatio() ? (sumComments > 0 ? (double)sumComments/(double)sumLines : 0.0 ) : sumComments;
        iac.setData(new double[]{sumLines, sumComments, value});
        return value;
    }
    @Override
    public void metricDisplay(WebRequestData data, InstanceAssignment ia, InstanceAssignmentQuestion iaq, InstanceAssignmentCriteria iac, StringBuilder html, JavaCodeMetrics_Criteria cdata)
    {
        if(iac.getData() != null)
        {
            double[] rdata = (double[])iac.getData();
            HashMap<String,Object> kvs = new HashMap<>();
            kvs.put("info", String.format("%.0f of comment lines - %.2f%% of total lines.", rdata[1], rdata[1] > 0.0 && rdata[0] > 0.0 ? (rdata[1]/rdata[0])*100 : 0.0));
            if(rdata[2] <= cdata.getLo())
                kvs.put("error", "Too few lines of comments.");
            else if(rdata[2] > cdata.getLo() && rdata[2] < cdata.getLotol())
                kvs.put("warning", "The lines of commenting is just about too short.");
            else if(rdata[2] > cdata.getHitol() && rdata[2] < cdata.getHi())
                kvs.put("warning", "The lines of commenting is just about too long.");
            else if(rdata[2] >= cdata.getHi())
                kvs.put("error", "Too many lines of comments.");
            else
                kvs.put("success", "Acceptable amount of commenting.");
            
            html.append(data.getCore().getTemplates().render(data, kvs, "defaultqch/criteria/feedback_display"));
        }
    }
    @Override
    public void dispose(CodeJava_Instance idata, JavaCodeMetrics_Criteria cdata)
    {
        // No need to do anything...
    }
}
