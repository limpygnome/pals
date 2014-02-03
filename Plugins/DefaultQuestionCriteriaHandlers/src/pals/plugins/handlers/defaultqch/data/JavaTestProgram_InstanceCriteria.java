package pals.plugins.handlers.defaultqch.data;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Used for feedback.
 */
public class JavaTestProgram_InstanceCriteria implements Serializable
{
    static final long serialVersionUID = 1L;
    // Enums *******************************************************************
    public enum Status
    {
        Info(0),
        Input(1),
        Incorrect(2),
        Correct(4);
        private int value;
        private Status(int value)
        {
            this.value = value;
        }
        public int getValue()
        {
            return value;
        }
    }
    // Fields ******************************************************************
    private ArrayList<String>   lines;
    private ArrayList<Status>   status;
    // Methods - Constructors **************************************************
    public JavaTestProgram_InstanceCriteria()
    {
        this.lines = new ArrayList<>();
        this.status = new ArrayList<>();
    }
    // Methods - Mutators ******************************************************
    public void addLine(String line, Status status)
    {
        this.lines.add(line);
        this.status.add(status);
    }
    // Methods - Accessors *****************************************************
    /**
     * @param index The index of the line.
     * @return The line at the index.
     */
    public String getLine(int index)
    {
        return lines.get(index);
    }
    /**
     * @param index The index of the line.
     * @return The status of the line at the index.
     */
    public Status getStatus(int index)
    {
        return status.get(index);
    }
    /**
     * @return The total number of lines.
     */
    public int getLines()
    {
        return lines.size();
    }
    /**
     * @return The mark given to each correct line.
     */
    public double getCorrectMark()
    {
        return (1.0/(double)lines.size())*100.0;
    }
}
