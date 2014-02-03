package pals.base.assessment;

import java.io.IOException;
import pals.base.Logging;
import pals.base.NodeCore;
import pals.base.database.DatabaseException;
import pals.base.database.Result;
import pals.base.utils.Misc;

/**
 * A helper class with common code shared between assessment classes.
 */
class Utils
{
    static Object loadData(NodeCore core, Result res, String column)
    {
        try
        {
            byte[] byteData = (byte[])res.get(column);
            return byteData != null ? Misc.bytesDeserialize(core, byteData) : null;
        }
        catch(IOException | ClassNotFoundException | NoClassDefFoundError | NullPointerException | DatabaseException ex)
        {
            core.getLogging().logEx("Base Assessment", ex, Logging.EntryType.Warning);
            return null;
        }
    }
}
