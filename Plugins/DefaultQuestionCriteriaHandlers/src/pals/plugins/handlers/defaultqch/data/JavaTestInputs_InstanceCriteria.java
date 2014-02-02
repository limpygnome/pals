package pals.plugins.handlers.defaultqch.data;

import java.io.Serializable;

/**
 * Stores the results from a Java test-inputs criteria.
 */
public class JavaTestInputs_InstanceCriteria implements Serializable
{
    static final long serialVersionUID = 1L;
    // Fields ******************************************************************
    private String[]    input;
    private String[]    outputCorrect;
    private String[]    outputStudent;
    // Methods - Constructors **************************************************
    public JavaTestInputs_InstanceCriteria(int numberOfTests)
    {
        input = new String[numberOfTests];
        outputCorrect = outputStudent = new String[numberOfTests];
    }
    // Methods - Mutators ******************************************************
    /**
     * @param index The index of the test.
     * @param value The value to set for the input of the test.
     */
    public void setInput(int index, String value)
    {
        input[index] = value;
    }
    /**
     * @param index The index of the test.
     * @param value The output from the student.
     */
    public void setOutputStudent(int index, String value)
    {
        outputStudent[index] = value;
    }
    /**
     * @param index The The index of the test.
     * @param value The output from the correct solution.
     */
    public void setOutputCorrect(int index, String value)
    {
        outputCorrect[index] = value;
    }
    // Methods - Accessors *****************************************************
    /**
     * @param index The index of the test.
     * @return The input used for the test.
     */
    public String getInput(int index)
    {
        return input[index];
    }
    /**
     * @param index The index of the test.
     * @return The output from the student for the test.
     */
    public String getOutputStudent(int index)
    {
        return outputStudent[index];
    }
    /**
     * @param index The index of the test.
     * @return The output from solution for the test.
     */
    public String getOutputCorrect(int index)
    {
        return outputCorrect[index];
    }
    /**
     * @return The total number of tests.
     */
    public int getTests()
    {
        return input.length;
    }
    /**
     * @param index The index of the test.
     * @return Indicates if the test was correct.
     */
    public boolean isCorrect(int index)
    {
        return outputCorrect[index].equals(outputStudent[index]);
    }
}
