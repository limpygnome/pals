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
package pals.plugins.auth.models;

import java.util.HashMap;
import org.joda.time.DateTime;
import pals.base.auth.User;
import pals.base.database.Connector;
import pals.base.database.DatabaseException;
import pals.base.database.Result;
import pals.base.utils.Misc;
import pals.base.web.Email;
import pals.base.web.WebRequestData;

/**
 * A model for recovering user accounts via e-mail.
 */
public class ModelRecovery
{
    // Constants ***************************************************************
    private static final int DEPLOY_TIMEOUT_MS = 3600000;   // Timeout, in m/s, between new deployment e-mails.
    // Enums *******************************************************************
    public enum DeployEmail
    {
        RecentlyDeployed,
        EmailNotExist,
        Failed,
        Success
    }
    // Fields ******************************************************************
    private User    user;
    private String  code;
    // Methods - Constructors **************************************************
    private ModelRecovery(User user, String code)
    {
        this.user = user;
        this.code = code;
    }
    // Methods - Persistence ***************************************************
    /**
     * Fetches a recovery code.
     * 
     * @param conn Database connector.
     * @param code The unique recovery code.
     * @param email The e-mail of the account being recovered.
     * @return An instance of the recovery-code model or null.
     */
    public static ModelRecovery load(Connector conn, String code, String email)
    {
        // Dispose old models
        deleteOld(conn);
        // Fetch model
        try
        {
            Result res = conn.read("SELECT u.*, rc.code FROM pals_users AS u, pals_user_recovery_codes AS rc WHERE u.userid=rc.userid AND u.email=? AND rc.code=? AND rc.sent > current_timestamp-CAST(? AS INTERVAL);", email, code, DEPLOY_TIMEOUT_MS+" milliseconds");
            return res.next() ? new ModelRecovery(User.load(conn, res), (String)res.get("code")) : null;
        }
        catch(DatabaseException ex)
        {
            return null;
        }
    }
    /**
     * Deploys a recovery e-mail.
     * 
     * @param data The data for the current web request.
     * @param conn Database connector.
     * @param email The e-mail of the account to recover.
     * @return The status of the operation.
     */
    public static DeployEmail deployEmail(WebRequestData data, Connector conn, String email)
    {
        // Dispose old models
        deleteOld(conn);
        // Load the model of the user
        User user = User.loadByEmail(conn, email);
        if(user == null)
            return DeployEmail.EmailNotExist;
        // Check if an e-mail has already been sent recently
        try
        {
            if((long)conn.executeScalar("SELECT COUNT('') FROM pals_user_recovery_codes WHERE userid=? AND sent > current_timestamp-CAST(? AS INTERVAL);", user.getUserID(), DEPLOY_TIMEOUT_MS+" milliseconds") > 0)
                return DeployEmail.RecentlyDeployed;
        }
        catch(DatabaseException ex)
        {
            return DeployEmail.Failed;
        }
        // Generate recovery code
        String code = Misc.randomText(data.getCore(), 32);
        // Persist code
        try
        {
            conn.execute("DELETE FROM pals_user_recovery_codes WHERE userid=?; INSERT INTO pals_user_recovery_codes (code, userid, sent) VALUES(?,?,current_timestamp);", user.getUserID(), code, user.getUserID());
        }
        catch(DatabaseException ex)
        {
            return DeployEmail.Failed;
        }
        // Render content of e-mail
        HashMap<String, Object> kvs = new HashMap<>();
        kvs.put("data", data);
        kvs.put("user", user);
        kvs.put("code", code);
        kvs.put("datetime", DateTime.now());
        kvs.put("reset_url", data.getCore().getSettings().getStr("web/base_url")+"/account/recover?email="+email+"&amp;code="+code);
        String content = data.getCore().getTemplates().render(data, kvs, "default_auth/email_recovery");
        // Deploy new e-mail
        String inst = data.getCore().getSettings().getStr("templates/institution");
        Email m = new Email("PALS - "+(inst != null ? inst + " - " : "")+"Account Recovery", content, email);
        m.persist(conn);
        return DeployEmail.Success;
    }
    /**
     * Unpersists the current model.
     * 
     * @param conn Database connector.
     */
    public void delete(Connector conn)
    {
        try
        {
            conn.execute("DELETE FROM pals_user_recovery_codes WHERE code=?;", code);
        }
        catch(DatabaseException ex)
        {
        }
    }
    /**
     * Deletes any old recovery codes.
     * 
     * @param conn Database connector.
     */
    public static void deleteOld(Connector conn)
    {
        try
        {
            conn.execute("DELETE FROM pals_user_recovery_codes WHERE sent < current_timestamp-CAST(? AS INTERVAL);", DEPLOY_TIMEOUT_MS);
        }
        catch(DatabaseException ex)
        {
        }
    }
    // Methods - Accessors *****************************************************
    /**
     * @return The recovery code.
     */
    public String getCode()
    {
        return code;
    }
    /**
     * @return The user tied to the recovery code.
     */
    public User getUser()
    {
        return user;
    }
}
