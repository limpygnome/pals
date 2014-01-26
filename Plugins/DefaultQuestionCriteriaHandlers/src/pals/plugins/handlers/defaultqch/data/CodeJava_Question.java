package pals.plugins.handlers.defaultqch.data;

import java.io.Serializable;
import pals.base.utils.Misc;

/**
 * Stores the data for a code-java question.
 */
public class CodeJava_Question implements Serializable
{
    static final long serialVersionUID = 1L;
    // Enums *******************************************************************
    public enum QuestionType
    {
        Fragment(0),
        Upload(1);
        private final int formValue;
        private QuestionType(int formValue)
        {
            this.formValue = formValue;
        }
        public int getFormValue()
        {
            return formValue;
        }
        public static QuestionType parse(String value)
        {
            if(value == null || value.length() == 0)
                return Fragment;
            try
            {
                switch(Integer.parseInt(value))
                {
                    case 0:
                        return Fragment;
                    case 1:
                        return Upload;
                }
            }
            catch(NumberFormatException ex) {}
            return Fragment;
        }
    }
    // Fields ******************************************************************
    private String          text;
    private String          skeleton;
    private String[]        whitelist;
    private QuestionType    type;
    // Methods - Constructors **************************************************
    public CodeJava_Question()
    {
        this.text = "Undefined question text...";
        this.skeleton = "";
        this.whitelist = new String[0];
        this.type = QuestionType.Fragment;
    }

    // Methods - Mutators ******************************************************
    /**
     * @param text Sets the question text.
     */
    public void setText(String text)
    {
        this.text = text;
    }
    /**
     * @param skeleton Sets the skeleton code.
     */
    public void setSkeleton(String skeleton)
    {
        this.skeleton = skeleton;
    }
    /**
     * @param whitelist Sets the array of classes white-listed; this includes
     * filtering.
     */
    public void setWhitelist(String[] whitelist)
    {
        this.whitelist = Misc.arrayStringUnique(whitelist);
    }
    /**
     * @param type Sets the type of question.
     */
    public void setType(QuestionType type)
    {
        this.type = type;
    }
    // Methods - Accessors *****************************************************
    /**
     * @return The question's text.
     */
    public String getText()
    {
        return text;
    }
    /**
     * @return The question's skeleton code.
     */
    public String getSkeleton()
    {
        return skeleton;
    }
    /**
     * @return Array of classes white-listed; can be empty for no restrictions.
     */
    public String[] getWhitelist()
    {
        return whitelist;
    }
    /**
     * @return Same as {@link #getWhitelist()}, except split by new-line character.
     */
    public String getWhitelistWeb()
    {
        StringBuilder sb = new StringBuilder();
        for(String s : whitelist)
            sb.append(s).append('\n');
        return sb.toString();
    }
    /**
     * @return The type of question.
     */
    public QuestionType getType()
    {
        return type;
    }
}
