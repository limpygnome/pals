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
package pals.plugins.handlers.defaultqch.data;

import java.io.Serializable;
import pals.base.utils.Misc;
import pals.plugins.handlers.defaultqch.criterias.JavaCodeMetrics;

/**
 * Used for storing the criteria settings for Java code-metrics. Refer to
 * {@link JavaCodeMetrics} for further documentation.
 */
public class JavaCodeMetrics_Criteria implements Serializable
{
    static final long serialVersionUID = 1L;
    // Enums *******************************************************************
    public enum MetricType
    {
        // -- Ratio-based
        LinesOfCode                         (0, false,  "Lines of Code"),
        LinesOfCode_Ratio                   (1, true,   "Lines of Code - Ratio"),
        BlankLines                          (2, false,  "Blank Lines"),
        BlankLines_Ratio                    (3, true,   "Blank Lines - Ratio"),
        CommentLines                        (4, false,  "Comment Lines"),
        CommentLines_Ratio                  (5, true,   "Comment Lines - Ratio"),
        // -- Specific values
        AverageLengthIdentifiersClasses     (6, false,  "Avergae Identifier Length - Classes"),
        AverageLengthIdentifiersMethods     (7, false,  "Avergae Identifier Length - Methods"),
        AverageLengthIdentifiersFields      (8, false,  "Avergae Identifier Length - Fields")
        ;
        
        private boolean ratio;
        private int     formValue;
        private String  title;
        private MetricType(int formValue, boolean ratio, String title)
        {
            this.formValue = formValue;
            this.ratio = ratio;
            this.title = title;
        }
        public int getFormValue()
        {
            return formValue;
        }
        /**
         * @param formValue The form value of the metric selected.
         * @return The enum, parsed from the form-value.
         */
        public static MetricType parse(int formValue)
        {
            switch(formValue)
            {
                case 0:
                    return LinesOfCode;
                case 1:
                    return LinesOfCode_Ratio;
                case 2:
                    return BlankLines;
                case 3:
                    return BlankLines_Ratio;
                case 4:
                    return CommentLines;
                case 5:
                    return CommentLines_Ratio;
                case 6:
                    return AverageLengthIdentifiersClasses;
                case 7:
                    return AverageLengthIdentifiersMethods;
                case 8:
                    return AverageLengthIdentifiersFields;
                default:
                    return null;
            }
        }
        /**
         * @return Double-dimension array; each row is a metric-type,
         * with column 0 as the form-value and column 1 as the title for
         * the metric.
         */
        public static Object[][] getModels()
        {
            MetricType[] m = MetricType.values();
            Object[][] buffer = new Object[m.length][];
            for(int i = 0; i < m.length; i++)
                buffer[i] = new Object[]{ m[i].formValue, m[i].title };
            return buffer;
        }
    }
    public enum UpdateThresholds
    {
        InvalidMustBeRatio,
        InvalidOrder,
        Success
    }
    // Fields ******************************************************************
    private MetricType  type;       // The type of metric.
    private String[]    classes;    // The classes to which the metric is applied.
    private double      lo,         // Min-value before 0 marks.
                        lotol,      // The min-value for full marks.
                        hitol,      // The the max-value for full marks. 
                        hi;         // Max-value before 0 marks.
    // Methods - Constructors **************************************************
    public JavaCodeMetrics_Criteria()
    {
        classes = null;
        type = MetricType.LinesOfCode;
        lo = 0.0;
        lotol = 0.25;
        hitol = 0.75;
        hi = 1.0;
    }
    // Methods - Mutators ******************************************************
    /**
     * @param type The type of criteria.
     */
    public void setType(MetricType type)
    {
        this.type = type;
    }
    /**
     * @param type The type of criteria, form-value.
     * @return True = valid value, false = invalid value.
     */
    public boolean setType(String type)
    {
        try
        {
            MetricType t = MetricType.parse(Integer.parseInt(type));
            if(t == null)
                return false;
            this.type = t;
            return true;
        }
        catch(NumberFormatException ex)
        {
            return false;
        }
    }
    /**
     * @param classes Sets the classes to which the metric is applied; can be
     * empty or null for all classes.
     */
    public void setClasses(String classes)
    {
        this.classes = Misc.arrayStringUnique(classes.replace("\r", "").split("\n"));
        if(this.classes.length == 0)
            this.classes = null;
    }
    /**
     * Updates the thresholds for marking.
     * 
     * @param lo The minimum value for zero marks.
     * @param lotol The minimum value for maximum marks.
     * @param hitol The maximum value for maximum marks.
     * @param hi The maximum value for zero marks.
     * @return The status from attempting to update the thresholds.
     */
    public UpdateThresholds setThresholds(double lo, double lotol, double hitol, double hi)
    {
        // Check 0.0 to 1.0 for ratio-type
        if(type.ratio && (lo < 0.0 || lo > 1.0 || lotol < 0.0 || lotol > 1.0 || hitol < 0.0 || hitol > 1.0 || hi < 0.0 || hi > 1.0))
            return UpdateThresholds.InvalidMustBeRatio;
        // Check order of values
        if(hi <= hitol || hitol <= lotol || lotol <= lo)
            return UpdateThresholds.InvalidOrder;
        // Update values
        this.lo = lo;
        this.lotol = lotol;
        this.hitol = hitol;
        this.hi = hi;
        return UpdateThresholds.Success;
    }
    // Methods - Accessors *****************************************************
    /**
     * @return The type of criteria.
     */
    public MetricType getType()
    {
        return type;
    }
    /**
     * @return The classes to which the metric is applied; can be an empty list
     * for all classes.
     */
    public String[] getClasses()
    {
        return classes;
    }
    
    public String getClassesWeb()
    {
        if(classes == null)
            return "";
        StringBuilder buffer = new StringBuilder();
        for(String c : classes)
            buffer.append(c).append('\n');
        if(buffer.length() > 0)
            buffer.deleteCharAt(buffer.length()-1);
        return buffer.toString();
    }
    /**
     * @return Indicates if the type is ratio-based.
     */
    public boolean isRatio()
    {
        return type.ratio;
    }
    /**
     * @return The minimum value for marks.
     */
    public double getLo()
    {
        return lo;
    }
    /**
     * @return The minimum value for maximum marks.
     */
    public double getLotol()
    {
        return lotol;
    }
    /**
     * @return The maximum value for maximum marks.
     */
    public double getHitol()
    {
        return hitol;
    }
    /**
     * @return The maximum value for zero marks.
     */
    public double getHi()
    {
        return hi;
    }
}
