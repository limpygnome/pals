package pals.plugins.handlers.defaultqch.criterias.metrics;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import pals.base.NodeCore;
import pals.base.Storage;
import pals.base.assessment.InstanceAssignment;
import pals.base.assessment.InstanceAssignmentCriteria;
import pals.base.assessment.InstanceAssignmentQuestion;
import pals.base.web.WebRequestData;
import pals.plugins.handlers.defaultqch.data.CodeJava_Instance;
import pals.plugins.handlers.defaultqch.data.JavaCodeMetrics_Criteria;

/**
 * A metric for the average length of identifiers.
 */
public class AverageIdentifiers implements Metric
{
    // Fields ******************************************************************
    private int             totalChars, totalIdents;
    private URLClassLoader  cl;
    // Methods - Overrides *****************************************************
    @Override
    public void init(NodeCore core, InstanceAssignmentCriteria iac, CodeJava_Instance idata, JavaCodeMetrics_Criteria cdata)
    {
        totalChars = totalIdents = 0;
        // Fetch path for IAQ
        String compilePath = Storage.getPath_tempIAQ(core.getPathShared(), iac.getIAQ());
        // Setup class-loader
        try
        {
            cl = new URLClassLoader(new URL[]{
                new File(compilePath).toURI().toURL()
            });
        }
        catch(MalformedURLException ex)
        {
            cl = null;
        }
    }
    @Override
    public void process(CodeJava_Instance idata, JavaCodeMetrics_Criteria cdata, String className)
    {
        try
        {
            Class curr = cl.loadClass(className);
            if(curr == null)
                return;
            switch(cdata.getType())
            {
                case AverageLengthIdentifiersClasses:
                    // Current classes
                    totalChars += classLocalName(className).length();
                    totalIdents++;
                    // Classes inside current class
                    Class[] cs = curr.getDeclaredClasses();
                    for(Class c : cs)
                    {
                        totalChars += classLocalName(c.getName()).length();
                        totalIdents++;
                    }
                    break;
                case AverageLengthIdentifiersMethods:
                    // Fetch methods
                    Method[] ms = curr.getDeclaredMethods();
                    for(Method m : ms)
                    {
                        totalChars += m.getName().length();
                        totalIdents++;
                    }
                    break;
                case AverageLengthIdentifiersFields:
                    Field[] fs = curr.getDeclaredFields();
                    for(Field f : fs)
                    {
                        totalChars += f.getName().length();
                        totalIdents++;
                    }
                    break;
            }
        }
        catch(ClassNotFoundException ex)
        {
        }
    }
    private String classLocalName(String c)
    {
        int t = c.lastIndexOf('.');
        return t != -1 && t < c.length()-2 ? c.substring(t+1) : c;
    }
    @Override
    public double metricComputeValue(CodeJava_Instance idata, JavaCodeMetrics_Criteria cdata, InstanceAssignmentCriteria iac)
    {
        double avg = totalIdents > 0 && totalChars > 0 ? (double)totalChars / (double)totalIdents : 0.0;
        iac.setData(new double[]{totalChars, totalIdents, avg});
        return avg;
    }
    @Override
    public void metricDisplay(WebRequestData data, InstanceAssignment ia, InstanceAssignmentQuestion iaq, InstanceAssignmentCriteria iac, StringBuilder html, JavaCodeMetrics_Criteria cdata)
    {
        if(iac.getData() != null)
        {
            double[] rdata = (double[])iac.getData();
            HashMap<String,Object> kvs = new HashMap<>();
            String type = "unknown";
            switch(cdata.getType())
            {
                case AverageLengthIdentifiersClasses:
                    type = "class";
                    break;
                case AverageLengthIdentifiersMethods:
                    type = "method";
                    break;
                case AverageLengthIdentifiersFields:
                    type = "field";
                    break;
            }
            kvs.put("info", String.format("Average %s identifier length of %.0f - %.0f identifiers found.", type, rdata[2], rdata[1]));
            if(rdata[2] <= cdata.getLo())
                kvs.put("error", "Your "+type+" identifiers are too short in length.");
            else if(rdata[2] > cdata.getLo() && rdata[2] < cdata.getLotol())
                kvs.put("warning", "Your "+type+" identifiers are just a little too short.");
            else if(rdata[2] > cdata.getHitol() && rdata[2] < cdata.getHi())
                kvs.put("warning", "Your "+type+" identifiers are just a little too long.");
            else if(rdata[2] >= cdata.getHi())
                kvs.put("error", "Your "+type+" identifiers are too long.");
            else
                kvs.put("success", "Acceptable average identifier length.");
            
            html.append(data.getCore().getTemplates().render(data, kvs, "defaultqch/criteria/feedback_display"));
        }
    }
    @Override
    public void dispose(CodeJava_Instance idata, JavaCodeMetrics_Criteria cdata)
    {
        // Dispose class-loader
        try
        {
            cl.close();
            cl = null;
        }
        catch(IOException ex)
        {
        }
    }
}
