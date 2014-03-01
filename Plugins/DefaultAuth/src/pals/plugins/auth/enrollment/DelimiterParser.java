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
package pals.plugins.auth.enrollment;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import pals.base.NodeCore;
import pals.base.Storage;
import pals.base.assessment.Module;
import pals.base.auth.UserGroup;
import pals.base.database.Connector;
import pals.base.database.DatabaseException;
import pals.base.web.UploadedFile;
import pals.base.web.WebRequestData;
import pals.plugins.auth.DefaultAuth;

/**
 * A delimiter parser implementation. Iterates each line and splits column
 * values using the provided delimiter.
 */
public class DelimiterParser extends Parser
{
    // Fields ******************************************************************
    private String delimiter;
    // Methods - Constructors **************************************************
    public DelimiterParser(String delimiter, NodeCore core, DefaultAuth auth, Module module, UserGroup group)
    {
        super(core, auth, module, group);
        this.delimiter = delimiter;
    }
    // Methods - Implementation ************************************************
    @Override
    public Result parse(Action action, WebRequestData data, UploadedFile file)
    {
        try
        {
            BufferedReader br = new BufferedReader(new FileReader(Storage.getPath_tempWebFile(core.getPathShared(), file)));
            String[] cols;
            String line;
            int colUsername = -1, colEmail = -1, colPassword = -1;
            // Read first-line for header data
            {
                line = br.readLine();
                if(line == null)
                    return Result.Invalid_Data;
                cols = line.split(delimiter);
                for(int i = 0; i < cols.length; i++)
                {
                    line = cols[i].trim();
                    switch(line)
                    {
                        case "username":
                            colUsername = i;
                            break;
                        case "email":
                            colEmail = i;
                            break;
                        case "password":
                            colPassword = i;
                            break;
                    }
                }
            }
            if(colUsername == -1)
                return Result.Header_Missing_Username;
            else if(colEmail == -1)
                return Result.Header_Missing_Email;
            // Read line-by-line
            String username, email, password;
            int currLine = 0;
            while((line = br.readLine()) != null)
            {
                currLine++;
                // Parse data
                cols = line.trim().split(delimiter);
                if(cols.length > 1)
                {
                    // Parse fields
                    if(colUsername < cols.length && colEmail < cols.length)
                    {
                        username = cols[colUsername];
                        email = cols[colEmail];
                        password = colPassword != -1 && colPassword < cols.length ? cols[colPassword] : null;
                        // Apply action
                        applyAction(action, data, username, email, password);
                    }
                    else
                        errors.add("Line "+currLine+" cannot be parsed.");
                }
            }
            return Result.Success;
        }
        catch(IOException ex)
        {
            return Result.Error;
        }
    }
    @Override
    public String construct(Connector conn, int moduleid, int groupid)
    {
        StringBuilder sb = new StringBuilder();
        // Fetch data
        pals.base.database.Result res = constructFetchData(conn, moduleid, groupid);
        if(res == null)
            return "";
        // Construct into parsable output
        // -- Headers
        sb.append("username,email\n");
        // -- Data
        try
        {
            while(res.next())
                sb.append((String)res.get("username")).append(delimiter).append((String)res.get("email")).append("\n");
        }
        catch(DatabaseException ex)
        {
        }
        return sb.toString();
    }
}
