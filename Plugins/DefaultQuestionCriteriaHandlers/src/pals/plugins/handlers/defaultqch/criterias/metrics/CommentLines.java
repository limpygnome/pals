package pals.plugins.handlers.defaultqch.criterias.metrics;

import pals.base.NodeCore;
import pals.base.assessment.InstanceAssignmentCriteria;
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
    public void dispose(CodeJava_Instance idata, JavaCodeMetrics_Criteria cdata)
    {
        // No need to do anything...
    }
}
