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
package pals.base.assessment;

import java.io.IOException;
import pals.base.Logging;
import pals.base.NodeCore;
import pals.base.database.DatabaseException;
import pals.base.database.Result;
import pals.base.utils.Misc;

/**
 * A helper class with common code shared between assessment classes.
 * 
 * @version 1.0
 */
class Utils
{
    /**
     * Loads serialized data from a result's column.
     * 
     * @param core Current instance of core.
     * @param res Result.
     * @param column The column name.
     * @return The object loaded; can be null, especially if it cannot be
     * loaded.
     * @since 1.0
     */
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
