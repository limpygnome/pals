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
    private String[]    output;
    private boolean[]   correct;
    // Methods - Constructors **************************************************
    public JavaTestInputs_InstanceCriteria(int numberOfTests)
    {
        input = new String[numberOfTests];
        output = new String[numberOfTests];
        correct = new boolean[numberOfTests];
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
     * @param value The value to set for the output of the test.
     */
    public void setOutput(int index, String value)
    {
        output[index] = value;
    }
    /**
     * @param index The index of the test.
     * @param correct Sets if the test is correct (true) or not (false).
     */
    public void setCorrect(int index, boolean correct)
    {
        this.correct[index] = correct;
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
     * @return The output from the test.
     */
    public String getOutput(int index)
    {
        return output[index];
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
        return correct[index];
    }
}