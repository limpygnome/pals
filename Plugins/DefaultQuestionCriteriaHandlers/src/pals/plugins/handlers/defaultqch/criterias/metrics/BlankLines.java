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
 * A metric for blank-lines.
 */
public class BlankLines implements Metric
{
    // Fields ******************************************************************
    private int sumLines, sumBlank;
    // Methods - Overrides *****************************************************
    @Override
    public void init(NodeCore core, InstanceAssignmentCriteria iac, CodeJava_Instance idata, JavaCodeMetrics_Criteria cdata)
    {
        sumLines = sumBlank = 0;
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
                    insideCommentBlock = true;
                else if(insideCommentBlock && line.contains("*/"))
                    insideCommentBlock = false;
            }
            else if(!insideCommentBlock)
                sumBlank++;
            sumLines++;
        }
    }
    @Override
    public double metricComputeValue(CodeJava_Instance idata, JavaCodeMetrics_Criteria cdata, InstanceAssignmentCriteria iac)
    {
        double value = cdata.isRatio() ? (sumBlank > 0 ? (double)sumBlank/(double)sumLines : 0.0 ) : sumBlank;
        iac.setData(new double[]{sumLines, sumBlank, value});
        return value;
    }
    @Override
    public void metricDisplay(WebRequestData data, InstanceAssignment ia, InstanceAssignmentQuestion iaq, InstanceAssignmentCriteria iac, StringBuilder html, JavaCodeMetrics_Criteria cdata)
    {
        if(iac.getData() != null)
        {
            double[] rdata = (double[])iac.getData();
            HashMap<String,Object> kvs = new HashMap<>();
            kvs.put("info", String.format("%.0f blank lines - %.2f%% of total lines.", rdata[1], rdata[1] > 0.0 && rdata[0] > 0.0 ? (rdata[1]/rdata[0])*100 : 0.0));
            if(rdata[2] <= cdata.getLo())
                kvs.put("error", "Too few blank lines.");
            else if(rdata[2] > cdata.getLo() && rdata[2] < cdata.getLotol())
                kvs.put("warning", "Just a too few blank lines.");
            else if(rdata[2] > cdata.getHitol() && rdata[2] < cdata.getHi())
                kvs.put("warning", "The number of blank lines is just a few too many.");
            else if(rdata[2] >= cdata.getHi())
                kvs.put("error", "Too many blank lines.");
            else
                kvs.put("success", "Acceptable amount of blank lines.");
            
            html.append(data.getCore().getTemplates().render(data, kvs, "defaultqch/criteria/feedback_display"));
        }
    }
    @Override
    public void dispose(CodeJava_Instance idata, JavaCodeMetrics_Criteria cdata)
    {
        // No need to do anything...
    }
}
