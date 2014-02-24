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
package pals.plugins.handlers.defaultqch.criterias;

import java.io.Serializable;
import pals.base.assessment.QuestionCriteria;
import pals.base.web.WebRequestData;
import pals.base.web.security.CSRF;

/**
 * A helper class for criteria classes, intended to reduce repetitive code.
 */
public abstract class CriteriaHelper
{
    protected static <T extends Serializable> void handle_criteriaEditPostback(WebRequestData data, QuestionCriteria qc, String critTitle, String critWeight, T cdata)
    {
        // Validate the request
        if(!CSRF.isSecure(data))
            data.setTemplateData("error", "Invalid request; please try again or contact an administrator!");
        else
        {
            // Parse weight
            int weight;
            try
            {
                weight = Integer.parseInt(critWeight);
            }
            catch(NumberFormatException ex)
            {
                weight = -1;
            }
            // Update model
            qc.setData(cdata);
            qc.setTitle(critTitle);
            qc.setWeight(weight);
            // Persist QC model
            QuestionCriteria.PersistStatus qcps = qc.persist(data.getConnector());
            switch(qcps)
            {
                default:
                    data.setTemplateData("error", qcps.getText(qc));
                    break;
                case Success:
                    data.setTemplateData("success", qcps.getText(qc));
                    break;
            }
        }
    }
}
