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
    Authors:    Marcus Craske           <limpygnome@gmail.com>
    ----------------------------------------------------------------------------
*/
package pals.plugins.handlers.defaultqch.data;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Stores the instance results for Java Custom criteria.
 * 
 * @version 1.0
 */
public class JavaCustom_InstanceCriteria implements Serializable
{
    static final long serialVersionUID = 1L;
    
    // Enums *******************************************************************
    /**
     * The type of feedback message.
     * 
     * @version 1.0
     */
    public enum FeedbackType
    {
        /**
         * General feedback.
         * 
         * @since 1.0
         */
        Info(1),
        /**
         * A warning message.
         * 
         * @since 1.0
         */
        Warning(2),
        /**
         * An error/critical message.
         * 
         * @since 1.0
         */
        Error(3),
        /**
         * A success message.
         * 
         * @since 1.0
         */
        Success(4);
        protected final int type;
        private FeedbackType(int type)
        {
            this.type = type;
        }
    }
    // Classes *****************************************************************
    /**
     * A feedback message, created by the custom code.
     * 
     * @version 1.0
     */
    public class FeedbackMessage implements Serializable
    {
        static final long serialVersionUID = 1L;
        
        // Fields ***************************************************************
        public FeedbackType     type;
        public String           message;
        // Methods - Constructors ***********************************************
        /**
         * Creates a new instance.
         * 
         * @param type The type.
         * @param message The feedback message.
         * @since 1.0
         */
        public FeedbackMessage(FeedbackType type, String message)
        {
            this.type = type;
            this.message = message;
        }
        /**
         * The type number, for template rendering.
         * 
         * @return Type number.
         * @since 1.0
         */
        public int getTypeNumber()
        {
            return type.type;
        }
        /**
         * The message stored for this feedback.
         * 
         * @return Text message.
         * @since 1.0
         */
        public String getMessage()
        {
            return message;
        }
    }
    // Fields ******************************************************************
    private ArrayList<FeedbackMessage> msgs;
    // Methods - Constructors **************************************************
    public JavaCustom_InstanceCriteria()
    {
        msgs = new ArrayList<>();
    }
    // Methods - Mutators ******************************************************
    /**
     * Adds a new feedback message.
     * 
     * @param type The type of message.
     * @param message The message to be added.
     * @since 1.0
     */
    public void add(FeedbackType type, String message)
    {
        msgs.add(new FeedbackMessage(type, message));
    }
    /**
     * Clears any feedback messages.
     * 
     * @since 1.0
     */
    public void clear()
    {
        msgs.clear();
    }
    // Methods - Accessors *****************************************************
    /**
     * Fetches the feedback messages, in-order of being added.
     * 
     * @return Array of feedback messages.
     * @since 1.0
     */
    public FeedbackMessage[] getMessages()
    {
        return msgs.toArray(new FeedbackMessage[msgs.size()]);
    }
}
