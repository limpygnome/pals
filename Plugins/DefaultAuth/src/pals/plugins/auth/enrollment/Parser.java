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

import java.util.ArrayList;
import java.util.HashMap;
import org.joda.time.DateTime;
import pals.base.NodeCore;
import pals.base.assessment.Module;
import pals.base.auth.User;
import pals.base.auth.UserGroup;
import pals.base.database.Connector;
import pals.base.database.DatabaseException;
import pals.base.utils.Misc;
import pals.base.web.Email;
import pals.base.web.UploadedFile;
import pals.base.web.WebRequestData;
import pals.plugins.auth.DefaultAuth;

/**
 * An abstract class for mass-enrollment parsers.
 */
public abstract class Parser
{
    // Enums *******************************************************************
    public enum Result
    {
        Error,
        Invalid_Data,
        Header_Missing_Username,
        Header_Missing_Email,
        Success
    }
    public enum Action
    {
        AddUsers(1),
        DisenrollFromModule(2),
        RemoveUsers(3);
        private final int formval;
        private Action(int formval)
        {
            this.formval = formval;
        }
        public int getFormVal()
        {
            return formval;
        }
        /**
         * Parses the type of action based on input from a web-source.
         * 
         * @param data The data representing a parsing action to perform.
         * @return The parsing action or null.
         */
        public static Action parse(String data)
        {
            if(data == null || data.length() == 0)
                return null;
            int val;
            try
            {
                val = Integer.parseInt(data);
            }
            catch(NumberFormatException ex)
            {
                return null;
            }
            switch(val)
            {
                case 1:     return AddUsers;
                case 2:     return DisenrollFromModule;
                case 3:     return RemoveUsers;
                default:    return null;
            }
        }
    }
    // Fields ******************************************************************
    protected NodeCore              core;
    protected DefaultAuth           auth;
    protected Module                module;
    protected UserGroup             group;
    protected int                   usersAffected;
    protected ArrayList<String>     errors;
    protected ArrayList<String>     messages;
    // Methods - Constructors **************************************************
    public Parser(NodeCore core, DefaultAuth auth, Module module, UserGroup group)
    {
        this.core = core;
        this.auth = auth;
        this.module = module;
        this.group = group;
        this.errors = new ArrayList<>();
        this.messages = new ArrayList<>();
        this.usersAffected = 0;
    }
    // Methods - Abstract ******************************************************
    /**
     * Parses a file to apply an action to a set of users.
     * 
     * @param action The action to be applied.
     * @param data The data for the current web-request.
     * @param file The file of user data.
     * @return Indicates the general success of the operation.
     */
    public abstract Result parse(Action action, WebRequestData data, UploadedFile file);
    /**
     * Constructs a file, which can be later parsed.
     * 
     * @param conn Database connector.
     * @return The string data constructed.
     */
    public abstract String construct(Connector conn, int moduleid, int groupid);
    protected pals.base.database.Result constructFetchData(Connector conn, int moduleid, int groupid)
    {
        try
        {
            if(moduleid < 0 && groupid < 0)
                return conn.read("SELECT u.username, u.email FROM pals_users AS u ORDER BY u.username ASC;");
            else if(moduleid >= 0)
                return conn.read("SELECT u.username, u.email FROM pals_users AS u WHERE u.userid IN (SELECT userid FROM pals_modules_enrollment WHERE moduleid=?) ORDER BY u.username ASC;", moduleid);
            else
                return conn.read("SELECT u.username, u.email FROM pals_users AS u WHERE u.groupid=? ORDER BY u.username ASC;", groupid);
        }
        catch(DatabaseException ex)
        {
            return null;
        }
    }
    // Methods *****************************************************************
    protected void applyAction(Action action, WebRequestData data, String username, String email, String password)
    {
        switch(action)
        {
            case AddUsers:
                userAdd(data, username, email, password);
                break;
            case DisenrollFromModule:
                userDisenroll(data, username, email, password);
                break;
            case RemoveUsers:
                userDelete(data, username, email, password);
                break;
        }
    }
    protected void userAdd(WebRequestData data, String username, String email, String password)
    {
        // Clean input
        username = username.trim();
        email = email.trim();
        if(password != null)
            password = password.trim();
        // Check if the user exists
        User u = User.load(data.getConnector(), username);
        boolean needsPersisting, needsEmail = false;
        if(u == null)
        {
            // Create user
            u = new User();
            // Set account details
            u.setUsername(username);
            u.setEmail(email);
            u.setGroup(group);
            // Set/generate password
            if(password == null || password.length() == 0)
            {
                int len = u.getPasswordMin()+((u.getPasswordMax()-u.getPasswordMin())/2);
                password = Misc.randomText(core, len);
            }
            // Generate salt and hash password
            String salt = auth.getNewSalt();
            u.setPasswordSalt(salt);
            u.setPassword(auth.hash(password, salt));
            needsPersisting = true;
            needsEmail = true;
        }
        else if(!u.getEmail().equals(email))
        {
            // Update e-mail
            u.setEmail(email);
            needsPersisting = true;
        }
        else
            needsPersisting = false;
        // Persist user model changes
        if(needsPersisting)
        {
            User.PersistStatus_User ps = u.persist(core, data.getConnector());
            switch(ps)
            {
                default:
                    errors.add("Failed to persist user '"+u.getUsername()+"' ("+u.getUserID()+"): "+ps.name());
                    break;
                case Failed:
                    errors.add("Failed to persist user '"+u.getUsername()+"' ("+u.getUserID()+") due to error.");
                    break;
                case InvalidPassword_length:
                    errors.add("Failed to create user '"+u.getUsername()+"' - password must be "+u.getPasswordMin()+" to "+u.getPasswordMax()+" characters in length.");
                    break;
                case InvalidEmail_exists:
                    User dup = User.loadByEmail(data.getConnector(), email);
                    errors.add("Failed to create user '"+u.getUsername()+"' - e-mail ('"+u.getEmail()+"') already in-use by '"+(dup != null ? dup.getUsername() : "(could not load model)")+"'.");
                    break;
                case Success:
                    messages.add("Created/modified user '"+u.getUsername()+"'.");
                    break;
            }
            if(ps != User.PersistStatus_User.Success)
                needsEmail = false;
        }
        // Check if to deploy an e-mail with the user's password
        if(needsEmail)
        {
            // Format e-mail message
            HashMap<String,Object> kvs = new HashMap<>();
            kvs.put("user", u);
            kvs.put("datetime", DateTime.now());
            kvs.put("password", password);
            kvs.put("login_url", data.getCore().getSettings().getStr("web/base_url")+"/account/login");
            String content = core.getTemplates().render(data, kvs, "default_auth/email_creation");
            // Add e-mail to queue
            String inst = core.getSettings().getStr("templates/institution");
            Email m = new Email("PALS - "+(inst != null ? inst + " - " : "")+"Account Creation", content, email);
            m.persist(data.getConnector());
            // Wake e-mail service
            core.getRMI().nodesGlobalEvent("base.web.email.wake", new Object[]{});
        }
        // Check if to enroll the user on a module
        if(module != null)
        {
            if(module.usersAdd(data.getConnector(), u))
                messages.add("Enrolled '"+u.getUsername()+"' on module.");
        }
        usersAffected++;
    }
    protected void userDisenroll(WebRequestData data, String username, String email, String password)
    {
        if(module == null)
            return;
        // Clean input
        username = username.trim();
        // Load user model
        User u = User.load(data.getConnector(), username);
        if(u != null)
        {
            // Remove the user from the module
            if(module.isEnrolled(data.getConnector(), u))
            {
                if(module.usersRemove(data.getConnector(), u))
                {
                    messages.add("Disenrolled '"+u.getUsername()+"' from module.");
                    usersAffected++;
                }
                else
                    messages.add("Failed to disenroll '"+u.getUsername()+"'.");
            }
        }
    }
    protected void userDelete(WebRequestData data, String username, String email, String password)
    {
        // Clean input
        username = username.trim();
        // Load the user
        User u = User.load(data.getConnector(), username);
        if(u != null)
        {
            // Remove them from the system...
            u.remove(data.getConnector());
            messages.add("Deleted user '"+u.getUsername()+"'.");
        }
    }
    // Methods - Accessors *****************************************************
    /**
     * @return The module for affected users; can be null.
     */
    public Module getModule()
    {
        return module;
    }
    /**
     * @return The user-group for new affected users.
     */
    public UserGroup getUserGroup()
    {
        return group;
    }
    /**
     * @return The number of affected users.
     */
    public int getUsersAffected()
    {
        return usersAffected;
    }
    /**
     * @return Array of error messages; can be empty. This may only indicate
     * operations have failed for individual users.
     */
    public String[] getErrors()
    {
        return errors.toArray(new String[errors.size()]);
    }
    /**
     * @return Array of information messages; can be empty. Indicates general
     * operations.
     */
    public String[] getMessages()
    {
        return messages.toArray(new String[messages.size()]);
    }
}
