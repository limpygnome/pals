package pals.plugins;

import pals.base.assessment.Module;
import pals.base.database.Connector;
import pals.base.database.DatabaseException;
import pals.base.database.Result;

/**
 * A model for displaying a module on the modules list page.
 */
public class ModulesViewModel
{
    // Fields ******************************************************************
    private Module      module;
    private int         assignmentsTotal,
                        assignmentsUnanswered,
                        assignmentsAnswered,
                        assignmentsIncomplete;
    // Methods - Constructors **************************************************
    public ModulesViewModel(Connector conn, Module module)
    {
        this.module = module;
        try
        {
            Result res = conn.read("");
            res.next();
            this.assignmentsTotal = (int)res.get("total");
            this.assignmentsUnanswered = (int)res.get("unanswered");
            this.assignmentsAnswered = (int)res.get("answered");
            this.assignmentsIncomplete = (int)res.get("incomplete");
        }
        catch(DatabaseException ex)
        {
            this.assignmentsTotal = this.assignmentsUnanswered = this.assignmentsAnswered = this.assignmentsIncomplete = 0;
        }
    }
    // Methods - Accessors *****************************************************
}
