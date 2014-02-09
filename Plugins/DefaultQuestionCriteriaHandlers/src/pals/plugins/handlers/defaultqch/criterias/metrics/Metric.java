package pals.plugins.handlers.defaultqch.criterias.metrics;

import pals.base.NodeCore;
import pals.base.assessment.InstanceAssignment;
import pals.base.assessment.InstanceAssignmentCriteria;
import pals.base.assessment.InstanceAssignmentQuestion;
import pals.base.web.WebRequestData;
import pals.plugins.handlers.defaultqch.data.CodeJava_Instance;
import pals.plugins.handlers.defaultqch.data.JavaCodeMetrics_Criteria;

/**
 * The interface for a metric.
 */
public interface Metric
{
    /**
     * Invoked before a metric is applied to code.
     * 
     * @param core The current instance of the core.
     * @param iac The current instance of the criteria.
     * @param idata Instance data.
     * @param cdata Criteria data.
     */
    public void init(NodeCore core, InstanceAssignmentCriteria iac, CodeJava_Instance idata, JavaCodeMetrics_Criteria cdata);
    /**
     * Processes a code file.
     * 
     * @param idata Instance data.
     * @param cdata Criteria data.
     * @param className The full class-name of the class to be processed.
     */
    public void process(CodeJava_Instance idata, JavaCodeMetrics_Criteria cdata, String className);
    /**
     * Computes the value for this metric.
     * 
     * @param idata Instance data.
     * @param cdata Criteria data.
     * @param iac The instance of the criteria; used for the metric to set
     * data to later be displayed.
     * @return The value for the current metric.
     */
    public double metricComputeValue(CodeJava_Instance idata, JavaCodeMetrics_Criteria cdata, InstanceAssignmentCriteria iac);
    /**
     * Invoked to display a metric, for the results page. The method {@link #init(pals.base.NodeCore, pals.base.assessment.InstanceAssignmentCriteria, pals.plugins.handlers.defaultqch.data.CodeJava_Instance, pals.plugins.handlers.defaultqch.data.JavaCodeMetrics_Criteria) }
     * is not invoked for this method.
     * 
     * @param data Data for the current web-request.
     * @param ia Current instance of an assignment.
     * @param iaq The instance of the assignment-question being rendered.
     * @param iac The criteria this method will be rendering.
     * @param html HTML for the criteria is outputted to this builder.
     */
    public void metricDisplay(WebRequestData data, InstanceAssignment ia, InstanceAssignmentQuestion iaq, InstanceAssignmentCriteria iac, StringBuilder html, JavaCodeMetrics_Criteria cdata);
    /**
     * Invoked when the metric should be disposed.
     * 
     * @param idata Instance data.
     * @param cdata Criteria data.
     */
    public void dispose(CodeJava_Instance idata, JavaCodeMetrics_Criteria cdata);
}
