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
package pals.plugins.auth;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import pals.base.Logging;
import pals.base.NodeCore;
import pals.base.Plugin;
import pals.base.PluginManager;
import pals.base.Settings;
import pals.base.TemplateManager;
import pals.base.UUID;
import pals.base.Version;
import pals.base.WebManager;
import pals.base.assessment.Module;
import pals.base.auth.User;
import pals.base.auth.UserGroup;
import pals.base.database.Connector;
import pals.base.database.DatabaseException;
import pals.base.database.Result;
import pals.base.utils.JarIO;
import pals.base.utils.Misc;
import pals.base.web.MultipartUrlParser;
import pals.base.web.RemoteRequest;
import pals.base.web.RemoteResponse;
import pals.base.web.UploadedFile;
import pals.base.web.WebRequestData;
import pals.base.web.security.CSRF;
import pals.base.web.security.Escaping;
import pals.plugins.auth.enrollment.DelimiterParser;
import pals.plugins.auth.enrollment.Parser;
import pals.plugins.auth.models.ModelRecovery;
import pals.plugins.auth.models.ModelUser;
import pals.plugins.web.Captcha;

/**
 * Default authentication handler for PALS.
 */
public class DefaultAuth extends Plugin
{
    // Constants ***************************************************************
    private static final String SESSION_KEY__USERID = "defaultauth_userid";
    private static final String LOGGING_ALIAS = "DefaultAuth";
    // Methods - Constructors **************************************************
    public DefaultAuth(NodeCore core, UUID uuid, JarIO jario, Version version, Settings settings, String jarPath)
    {
        super(core, uuid, jario, version, settings, jarPath);
    }
    // Methods - Event Handlers ************************************************
    @Override
    public boolean eventHandler_pluginInstall(NodeCore core, Connector conn)
    {
        // Create a default user with 'admin','admin' (u/p) (if admin does not exist)
        User user = User.load(conn, "admin");
        if(user == null)
        {
            // Fetch the default user-group for admins
            UserGroup ug = UserGroup.load(conn, settings.getInt("auth/default_group_admin"));
            if(ug == null)
            {
                core.getLogging().log(LOGGING_ALIAS, "Could not load default user-group for admin during installation; cannot create default admin user.", Logging.EntryType.Error);
                return false;
            }
            // Attempt to create a new admin user
            String passwordSalt = getNewSalt();
            user = new User("admin", hash("admin", passwordSalt), passwordSalt, "admin@localhost.com", ug);
            User.PersistStatus_User ps = user.persist(core, conn);
            if(ps != User.PersistStatus_User.Success)
            {
                core.getLogging().log(LOGGING_ALIAS, "Failed to create default admin user during installation (persist-status: '"+ps.name()+"').", Logging.EntryType.Error);
                return false;
            }
            core.getLogging().log(LOGGING_ALIAS, "Created default admin with username 'admin' and password 'admin during installation.", Logging.EntryType.Info);
        }
        else
            core.getLogging().log(LOGGING_ALIAS, "User 'admin' already exists, skipped creation during installation.", Logging.EntryType.Info);

        return true;
    }
    @Override
    public boolean eventHandler_pluginUninstall(NodeCore core, Connector conn)
    {
        return true;
    }
    @Override
    public boolean eventHandler_pluginLoad(NodeCore core)
    {
        return true;
    }
    @Override
    public void eventHandler_pluginUnload(NodeCore core)
    {
        // Unregister templates
        core.getTemplates().remove(this);
        // Unregister URLs
        core.getWebManager().urlsUnregister(this);
        // Unregister hooks
        core.getPlugins().globalHookUnregister(this);
    }
    @Override
    public boolean eventHandler_registerTemplates(NodeCore core, TemplateManager manager)
    {
        if(!manager.load(this, "templates"))
            return false;
        return true;
    }
    @Override
    public boolean eventHandler_registerUrls(NodeCore core, WebManager web)
    {
        if(!web.urlsRegister(this, new String[]
        {
            "account/settings",
            "account/register",
            "account/login",
            "account/logout",
            "account/recover",
            "admin/users",
            "admin/groups",
            "admin/mass_enrollment"
        }))
            return false;
        return true;
    }
    @Override
    public boolean eventHandler_registerHooks(NodeCore core, PluginManager plugins)
    {
        if(!plugins.globalHookRegister(this, "base.web.request_start"))
            return false;
        return true;
    }
    @Override
    public boolean eventHandler_handleHook(String event, Object[] args)
    {
        switch(event)
        {
            case "base.web.request_start":
                if(args.length != 1 || !(args[0] instanceof WebRequestData))
                    return false;
                WebRequestData data = (WebRequestData)args[0];
                // Load the current user for the request
                if(data.getSession().contains(SESSION_KEY__USERID))
                {
                    User user = User.load(data.getConnector(), (int)data.getSession().getAttribute(SESSION_KEY__USERID));
                    if(user != null)
                    {
                        data.setUser(user);
                    }
                    else
                    {
                        // Invalid user - invalidate the session!
                        data.getSession().removeAttribute(SESSION_KEY__USERID);
                    }
                }
                return true;
        }
        return false;
    }
    @Override
    public boolean eventHandler_webRequest(WebRequestData data)
    {
        MultipartUrlParser mup = new MultipartUrlParser(data);
        switch(mup.getPart(0))
        {
            case "account":
                switch(data.getRequestData().getRelativeUrl())
                {
                    case "account/settings":
                        return pageAccount_settings(data);
                    case "account/register":
                        return pageAccount_register(data);
                    case "account/login":
                        return pageAccount_login(data);
                    case "account/logout":
                        return pageAccount_logout(data);
                    case "account/recover":
                        return pageAccount_recover(data);
                    default:
                        return false;
                }
            case "admin":
                // Check the user is allowed to modify the user system
                if(data.getUser() == null || !data.getUser().getGroup().isAdminUsers())
                    return false;
                // Continue to handle request...
                String page = mup.getPart(1);
                if(page != null)
                {
                    switch(page)
                    {
                        case "mass_enrollment":
                            return pageAdmin_massEnrollment(data);
                        case "users":
                            page = mup.getPart(2);
                            if(page == null)
                                return pageAdminUsers_groupUserView(data, null);
                            else if(page.equals("create"))
                                return pageAdminUsers_userCreate(data);
                            else
                            {
                                page = mup.getPart(3);
                                if(page == null)
                                    return false;
                                // Load user-model
                                User user;
                                if((user = User.load(data.getConnector(), mup.parseInt(2, -1))) == null)
                                    return false;
                                // Delegate
                                switch(page)
                                {
                                    case "edit":
                                        return pageAdminUsers_userEdit(data, user);
                                    case "delete":
                                        return pageAdminUsers_userDelete(data, user);
                                    default:
                                        return false;
                                }
                            }
                        case "groups":
                            page = mup.getPart(2);
                            if(page == null)
                                return pageAdminUsers_groupBrowse(data);
                            else if(page.equals("create"))
                                return pageAdminUsers_groupCreate(data);
                            else
                            {
                                page = mup.getPart(3);
                                // Load group model
                                UserGroup ug;
                                if((ug = UserGroup.load(data.getConnector(), mup.parseInt(2, -1))) == null)
                                    return false;
                                // Delegate
                                if(page == null)
                                    return pageAdminUsers_groupUserView(data, ug);
                                else
                                {
                                    switch(page)
                                    {
                                        case "edit":
                                            return pageAdminUsers_groupEdit(data, ug);
                                        case "delete":
                                            return pageAdminUsers_groupDelete(data, ug);
                                    }
                                }
                            }
                        default:
                            return false;
                    }
                }
            default:
                return false;
        }
    }
    @Override
    public String getTitle()
    {
        return "PALS: Default Authentication";
    }
    // Methods - Pages *********************************************************
    private boolean pageAccount_settings(WebRequestData data)
    {
        // Fetch the current user
        User user = data.getUser();
        if(user == null)
            return false;
        // Check form data
        RemoteRequest request = data.getRequestData();
        String currentPassword = request.getField("current_password");
        String email = request.getField("email");
        String newPassword = request.getField("newpassword");
        String newPasswordConfirm = request.getField("newpassword_confirm");
        if(email != null || newPassword != null || newPasswordConfirm != null)
        {
            // Validate request
            if(!CSRF.isSecure(data))
                data.setTemplateData("error", "Invalid request; please try again or contact an administrator!");
            // Validate correct current-password
            else if(currentPassword == null || !hash(currentPassword, user.getPasswordSalt()).equals(user.getPassword()))
                data.setTemplateData("error", "Incorrect current password!");
            // Validate input data
            else if(newPassword != null && newPassword.length() > 0 && (newPassword.length() < user.getPasswordMin() || newPassword.length() > user.getPasswordMax()))
                data.setTemplateData("error", "New password must be "+user.getPasswordMin()+" to "+user.getPasswordMax()+" characters in length!");
            else if(newPassword != null && newPassword.length() > 0 && (newPasswordConfirm == null || !newPasswordConfirm.equals(newPassword)))
                data.setTemplateData("error", "New passwords do not match!");
            else
            {
                // Update user model
                // -- Password
                if(newPassword != null && newPassword.length() > 0)
                {
                    String salt = getNewSalt();
                    user.setPassword(hash(newPassword, salt));
                }
                // -- E-mail
                if(email != null)
                {
                    user.setEmail(email);
                }
                // Attempt to persist
                User.PersistStatus_User ps = user.persist(getCore(), data.getConnector());
                switch(ps)
                {
                    case InvalidEmail_exists:
                        data.setTemplateData("error", "E-mail address in-use by another user!");
                        break;
                    case InvalidEmail_format:
                        data.setTemplateData("error", "Invalid e-mail address!");
                        break;
                    case InvalidPassword_length:
                        data.setTemplateData("error", "New password must be "+user.getPasswordMin()+" to "+user.getPasswordMax()+" characters in length!");
                        break;
                    case Success:
                        data.setTemplateData("success", "Successfully updated account settings!");
                        break;
                    default:
                        data.setTemplateData("error", "An unknown issue occurred ("+ps.name()+"); please try again or contact an administrator!");
                        break;
                }
            }
        }
        // Setup the page
        data.setTemplateData("pals_title", "Account - Settings");
        data.setTemplateData("pals_content", "default_auth/page_settings");
        // -- Fields
        data.setTemplateData("csrf", CSRF.set(data));
        if(email != null)
            data.setTemplateData("email", email);
        return true;
    }
    private boolean pageAccount_register(WebRequestData data)
    {
        // Check user is not logged-in
        if(data.getUser() != null)
            return false;
        // Check recovery is enabled
        if(!getSettings().getBool("feature/register", false))
            return false;
        // Check form data
        RemoteRequest request = data.getRequestData();
        String username = request.getField("username");
        String password = request.getField("password");
        String passwordConfirm = request.getField("password_confirm");
        String email = request.getField("email");
        String emailConfirm = request.getField("email_confirm");
        
        if(username != null && password != null && passwordConfirm != null && email != null && emailConfirm != null)
        {
            // Check security
            if(!CSRF.isSecure(data))
                data.setTemplateData("error", "Invalid request, please try again or contact an administrator!");
            else if(!Captcha.isCaptchaCorrect(data))
                data.setTemplateData("error", "Incorrect captcha verification code!");
            else
            {
                String salt = getNewSalt();
                UserGroup ug = UserGroup.load(data.getConnector(), (int)settings.getInt("auth/default_group"));
                if(ug == null)
                    data.setTemplateData("error", "An error occurred (invalid default group); please try again or contact an administrator!");
                else
                {
                    User user = new User();
                    // Validate data reliant on user model
                    if(password.length() < user.getPasswordMin() || password.length() > user.getPasswordMax())
                        data.setTemplateData("error", "Password must be "+user.getPasswordMin()+" to "+user.getPasswordMax()+" characters in length!");
                    else if(!password.equals(passwordConfirm))
                        data.setTemplateData("error", "Passwords do not match!");
                    else if(email.length() < user.getEmailMin() || email.length() > user.getEmailMax())
                        data.setTemplateData("error", "Invalid e-mail.");
                    else if(!email.equals(emailConfirm))
                        data.setTemplateData("error", "E-mails do not match!");
                    else
                    {
                        // Setup a new user
                        user.setUsername(username);
                        user.setPassword(hash(password, salt));
                        user.setPasswordSalt(salt);
                        user.setEmail(email);
                        user.setGroup(ug);
                        // Attempt to persist
                        User.PersistStatus_User ps = user.persist(getCore(), data.getConnector());
                        // Handle persist status
                        switch(ps)
                        {
                            default:
                            case InvalidGroup:
                            case Failed:
                                data.setTemplateData("error", "An error occurred ("+ps.toString()+"); please try again or contact an administrator!");
                                break;
                            case InvalidEmail_exists:
                                data.setTemplateData("error", "E-mail address already in-use.");
                                break;
                            case InvalidEmail_format:
                                data.setTemplateData("error", "Invalid e-mail.");
                                break;
                            case InvalidPassword_length:
                                data.setTemplateData("error", "Password must be "+user.getPasswordMin()+" to "+user.getPasswordMax()+" characters in length!");
                                break;
                            case InvalidUsername_exists:
                                data.setTemplateData("error", "Username already in-use.");
                                break;
                            case InvalidUsername_format:
                                data.setTemplateData("error", "Invalid username; must contain only alpha-numeric characters.");
                                break;
                            case InvalidUsername_length:
                                data.setTemplateData("error", "Username must be "+user.getUsernameMin()+" to "+user.getUsernameMax()+" characters in length.");
                                break;
                            case Success:
                                data.getResponseData().setRedirectUrl("/account/login");
                                break;
                        }
                    }
                }
            }
        }
        // Setup the page
        data.setTemplateData("pals_title", "Account - Register");
        data.setTemplateData("pals_content", "default_auth/page_register");
        // -- Fields
        data.setTemplateData("register_username", Escaping.htmlEncode(username));
        data.setTemplateData("register_password", Escaping.htmlEncode(password));
        data.setTemplateData("register_password_confirm", Escaping.htmlEncode(passwordConfirm));
        data.setTemplateData("register_email", Escaping.htmlEncode(email));
        data.setTemplateData("register_email_confirm", Escaping.htmlEncode(emailConfirm));
        data.setTemplateData("csrf", CSRF.set(data));
        return true;
    }
    private boolean pageAccount_login(WebRequestData data)
    {
        // Check user is not logged-in
        if(data.getUser() != null)
            return false;
        // Check form data
        RemoteRequest req = data.getRequestData();
        String username = req.getField("username");
        String password = req.getField("password");
        String sessPrivate = req.getField("sess_private");
        boolean sPrivate = sessPrivate != null && sessPrivate.equals("1");
        if(username != null && password != null)
        {
            User user;
            // Validate security
            if(!CSRF.isSecure(data))
                data.setTemplateData("error", "Invalid request; please try again or contact an administrator!");
            else if(!Captcha.isCaptchaCorrect(data))
                data.setTemplateData("error", "Incorrect captcha verification code!");
            // Check the postback
            else if(username.length() == 0 || password.length() == 0)
                data.setTemplateData("error", "Invalid username/password!");
            else if((user = User.load(data.getConnector(), username)) == null)
                data.setTemplateData("error", "Invalid username/password!");
            else if(!user.getGroup().isUserLogin())
                data.setTemplateData("error", "The specified account has been disabled from logging-on.");
            else
            {
                // Generate a hash using the user's salt, compare passwords
                String passHashed = hash(password, user.getPasswordSalt());
                if(passHashed.equals(user.getPassword()))
                {
                    // Correct! Setup the user and redirect to home
                    setAuth(data, user, sPrivate);
                    data.getResponseData().setRedirectUrl("/");
                }
                else
                    data.setTemplateData("error", "Invalid username/password!");
            }
        }
        // Setup the page
        data.setTemplateData("pals_title", "Account - Login");
        data.setTemplateData("pals_content", "default_auth/page_login");
        // -- Set fields
        data.setTemplateData("login_username", username);
        data.setTemplateData("sess_private", sPrivate);
        data.setTemplateData("csrf", CSRF.set(data));
        return true;
    }
    private boolean pageAccount_logout(WebRequestData data)
    {
        // Fetch the current user
        User user = data.getUser();
        if(user == null)
            return false;
        // Destroy session
        data.getSession().removeAttribute(SESSION_KEY__USERID);
        // Update user
        data.setUser(null);
        // Setup confirmation page
        data.setTemplateData("pals_title", "Account - Logout");
        data.setTemplateData("pals_content", "default_auth/page_logout");
        return true;
    }
    private boolean pageAccount_recover(WebRequestData data)
    {
        // Check user is not logged-in
        if(data.getUser() != null)
            return false;
        // Check recovery is enabled
        if(!getSettings().getBool("feature/recover", false))
            return false;
        RemoteRequest req = data.getRequestData();
        // Check if the user is recovering an account using a code
        String email = req.getField("email");
        String code = req.getField("code");
        String newPassword = req.getField("password");
        String newPasswordConfirm = req.getField("password_confirm");
        
        if(code != null && email != null)
        {
            // Fetch the recovery model
            ModelRecovery m = ModelRecovery.load(data.getConnector(), code, email);
            if(m == null)
                data.setTemplateData("recover_mode", 3);
            else
            {
                // Check if to change the password yet
                if(newPassword != null && newPasswordConfirm != null)
                {
                    User u = m.getUser();

                    if(!CSRF.isSecure(data))
                        data.setTemplateData("error", "Invalid request; please try again or contact an administrator.");
                    else if(!newPassword.equals(newPasswordConfirm))
                        data.setTemplateData("error", "New passwords do not match.");
                    else if(newPassword.length() > 0 && (newPassword.length() < u.getPasswordMin() || newPassword.length() > u.getPasswordMax()))
                        data.setTemplateData("error", "New password must be "+u.getPasswordMin()+" to "+u.getPasswordMax()+" characters in length!");
                    else
                    {
                        String salt = getNewSalt();
                        String passwordHash = hash(newPassword, salt);
                        u.setPassword(passwordHash);
                        u.setPasswordSalt(salt);
                        User.PersistStatus_User ps = u.persist(data.getCore(), data.getConnector());
                        switch(ps)
                        {
                            default:
                                data.setTemplateData("error", "An unknown error ('"+ps.name()+"') occurred changing your password; please try again or contact an administrator.");
                                break;
                            case Success:
                                // Dispose model
                                m.delete(data.getConnector());
                                // Authenticate the user
                                setAuth(data, u, false);
                                // Redirect to home
                                data.getResponseData().setRedirectUrl("/account/settings");
                                break;
                        }
                    }
                }
                // Set the page mode
                data.setTemplateData("recover_mode", 4);
                data.setTemplateData("email", email);
                data.setTemplateData("code", code);
            }
        }
        else if(email != null)
        {
            // Attempt to deploy recovery e-mail
            ModelRecovery.DeployEmail de = ModelRecovery.deployEmail(data, data.getConnector(), email);
            switch(de)
            {
                case EmailNotExist:
                    data.setTemplateData("error", "E-mail does not exist.");
                    data.setTemplateData("recover_mode", 1);
                    break;
                case Failed:
                    data.setTemplateData("error", "Failed due to an unknown reason; please try again or contact an administrator.");
                    data.setTemplateData("recover_mode", 1);
                    break;
                case RecentlyDeployed:
                    data.setTemplateData("error", "A recovery e-mail has already been recently sent, please check your inbox or try again later.");
                    data.setTemplateData("recover_mode", 1);
                    break;
                case Success:
                    data.setTemplateData("recover_mode", 2);
                    // Inform all nodes an e-mail is available
                    data.getCore().getRMI().nodesGlobalEventAll("base.web.email.wake", new Object[]{});
                    break;
            }
        }
        else
        {
            // Display normal form
            data.setTemplateData("recover_mode", 1);
        }
        // Setup the page
        data.setTemplateData("pals_title", "Account - Recover");
        data.setTemplateData("pals_content", "default_auth/page_recover");
        data.setTemplateData("csrf", CSRF.set(data));
        data.setTemplateData("email", email);
        return true;
    }
    // Methods - Pages - Admin *************************************************
    private boolean pageAdmin_massEnrollment(WebRequestData data)
    {
        RemoteRequest req = data.getRequestData();
        // -- Shared
        UploadedFile file = req.getFile("enroll_data");
        String rawEnrollAType = req.getField("enroll_atype");
        String rawEnrollFormat = req.getField("enroll_format");
        String rawEnrollModule = req.getField("enroll_module");
        String rawEnrollGroup = req.getField("enroll_group");
        // -- Enrollment
        String rawEnrollAction = req.getField("enroll_action");
        // -- Download
        String rawEnrollSrc = req.getField("enroll_src");
        // Check for postback
        if(rawEnrollAType != null)
        {
            // Parse delimiter
            int enrollFormat = Misc.parseInt(rawEnrollFormat, -1);
            String delimiter, contentType, extension;
            switch(enrollFormat)
            {
                case 1:
                    delimiter = ",";
                    contentType = "text/csv";
                    extension = "csv";
                    break;
                case 2:
                    delimiter = "\t";
                    contentType = "text/tab-separated-values"; // http://www.rfc-editor.org/rfc/rfc4180.txt
                    extension = "txt";
                    break;
                default:
                    return false;
            }
            // Handle type
            if(rawEnrollAType.equals("1"))
            {
                // Parse data
                int     enrollModule = Misc.parseInt(rawEnrollModule, -1),
                        enrollGroup = Misc.parseInt(rawEnrollGroup, -1);
                Parser.Action enrollAction = Parser.Action.parse(rawEnrollAction);
                // Validate data
                if(!CSRF.isSecure(data))
                    data.setTemplateData("error", "Invalid request; please try again or contact an administrator.");
                else if(!Captcha.isCaptchaCorrect(data))
                    data.setTemplateData("error", "Incorrect capcha verification code.");
                else if(file == null || file.getSize() <= 0)
                    data.setTemplateData("error", "No file uploaded.");
                else if(enrollFormat < 1 || enrollFormat > 2)
                    data.setTemplateData("error", "Invalid format.");
                else if(enrollAction == null)
                    data.setTemplateData("error", "Invalid action.");
                else if(enrollGroup == -1)
                    data.setTemplateData("error", "Invalid user-group specified.");
                else
                {
                    Module m = null;
                    UserGroup ug;
                    // Load required models
                    if(enrollModule != -1 && (m = Module.load(data.getConnector(), enrollModule)) == null)
                        data.setTemplateData("error", "Invalid module; could not be found or loaded.");
                    else if((ug = UserGroup.load(data.getConnector(), enrollGroup)) == null)
                        data.setTemplateData("error", "Invalid user-group; could not be found or loaded.");
                    else
                    {
                        // Create and parse data
                        Parser p = new DelimiterParser(delimiter, data.getCore(), this, m, ug);
                        Parser.Result res = p.parse(enrollAction, data, file);
                        switch(res)
                        {
                            case Error:
                                data.setTemplateData("error", "An error occurred parsing the data; please try again or contact an administrator.");
                                break;
                            case Header_Missing_Email:
                                data.setTemplateData("error", "Missing e-mail header.");
                                break;
                            case Header_Missing_Username:
                                data.setTemplateData("error", "Missing username header.");
                                break;
                            case Invalid_Data:
                                data.setTemplateData("error", "File is invalid/malformed and cannot be parsed.");
                                break;
                            case Success:
                                data.setTemplateData("success", "Successfully parsed file and applied action.");
                                break;
                        }
                        data.setTemplateData("errors", p.getErrors());
                        data.setTemplateData("messages", p.getMessages());
                    }
                }
                data.setTemplateData("enroll_format", enrollFormat);
                data.setTemplateData("enroll_group", enrollGroup);
                data.setTemplateData("enroll_module", enrollModule);
                if(enrollAction != null)
                    data.setTemplateData("enroll_action", enrollAction.getFormVal());
            }
            else if(rawEnrollAType.equals("2"))
            {
                // Download data
                Result res = null;
                if(rawEnrollSrc != null)
                {
                    int     src = Misc.parseInt(rawEnrollSrc, -1),
                            module = src == 2 ? Misc.parseInt(rawEnrollModule, -1) : -1,
                            group = src == 3 ? Misc.parseInt(rawEnrollGroup, -1) : -1;
                    // Construct data
                    Parser p = new DelimiterParser(delimiter, data.getCore(), this, null, null);
                    String output = p.construct(data.getConnector(), module, group);
                    // Setup the page
                    RemoteResponse resp = data.getResponseData();
                    resp.setBuffer(output);
                    resp.setResponseType(contentType);
                    resp.setHeader("Content-Disposition", "attachment; filename=download."+extension);
                    return true;
                }
                if(res == null)
                    return false;
            }
        }
        // Fetch modules
        Module[] modules = Module.loadAll(data.getConnector());
        // Fetch groups
        UserGroup[] groups = UserGroup.load(data.getConnector());
        // Setup the page
        data.setTemplateData("pals_title", "Admin - Mass-Enrollment");
        data.setTemplateData("pals_content", "default_auth/page_admin_massenrollment");
        data.setTemplateData("modules", modules);
        data.setTemplateData("groups", groups);
        data.setTemplateData("csrf", CSRF.set(data));
        return true;
    }
    // Methods - Pages - Admin - Users *****************************************
    private boolean pageAdminUsers_userCreate(WebRequestData data)
    {
        RemoteRequest req = data.getRequestData();
        // Check for postback
        String username = req.getField("username");
        String email = req.getField("email");
        String password = req.getField("password");
        String passwordConfirm = req.getField("password_confirm");
        String group = req.getField("group");
        int groupid = -1;
        boolean createdUser = false;
        if(username != null && email != null && password != null && passwordConfirm != null && group != null)
        {
            // Parse group ID
            try
            {
                groupid = Integer.parseInt(group);
            }
            catch(NumberFormatException ex)
            {
                groupid = -1;
            }
            // Check the session is valid and group loads
            UserGroup ug;
            if(!CSRF.isSecure(data))
                data.setTemplateData("error", "Invalid request; please try again.");
            else if((ug = UserGroup.load(data.getConnector(), groupid)) == null)
                data.setTemplateData("error", "Invalid user-group.");
            else
            {
                User user = new User();
                // Validate data reliant on user model
                if(password.length() < user.getPasswordMin() || password.length() > user.getPasswordMax())
                    data.setTemplateData("error", "Password must be "+user.getPasswordMin()+" to "+user.getPasswordMax()+" characters in length!");
                else if(!password.equals(passwordConfirm))
                    data.setTemplateData("error", "Passwords do not match!");
                else if(email.length() < user.getEmailMin() || email.length() > user.getEmailMax())
                    data.setTemplateData("error", "Invalid e-mail.");
                else
                {
                    String salt = getNewSalt();
                    // Setup a new user
                    user.setUsername(username);
                    user.setPassword(hash(password, salt));
                    user.setPasswordSalt(salt);
                    user.setEmail(email);
                    user.setGroup(ug);
                    // Attempt to persist
                    User.PersistStatus_User ps = user.persist(getCore(), data.getConnector());
                    // Handle persist status
                    switch(ps)
                    {
                        default:
                        case InvalidGroup:
                        case Failed:
                            data.setTemplateData("error", "An error occurred ("+ps.toString()+"); please try again or contact an administrator!");
                            break;
                        case InvalidEmail_exists:
                            data.setTemplateData("error", "E-mail address already in-use.");
                            break;
                        case InvalidEmail_format:
                            data.setTemplateData("error", "Invalid e-mail.");
                            break;
                        case InvalidPassword_length:
                            data.setTemplateData("error", "Password must be "+user.getPasswordMin()+" to "+user.getPasswordMax()+" characters in length!");
                            break;
                        case InvalidUsername_exists:
                            data.setTemplateData("error", "Username already in-use.");
                            break;
                        case InvalidUsername_format:
                            data.setTemplateData("error", "Invalid username; must contain only alpha-numeric characters.");
                            break;
                        case InvalidUsername_length:
                            data.setTemplateData("error", "Username must be "+user.getUsernameMin()+" to "+user.getUsernameMax()+" characters in length.");
                            break;
                        case Success:
                            createdUser = true;
                            data.setTemplateData("success", "Created user '"+username+"'.");
                            break;
                    }
                }
            }
        }
        // Setup the page
        data.setTemplateData("pals_title", "Admin - Users - Create");
        data.setTemplateData("pals_content", "default_auth/page_admin_user_create");
        // -- Fields
        data.setTemplateData("csrf", CSRF.set(data));
        data.setTemplateData("username", !createdUser ? username : null);
        data.setTemplateData("email", !createdUser ? email : null);
        data.setTemplateData("groups", UserGroup.load(data.getConnector()));
        data.setTemplateData("groupid", groupid);
        return true;
    }
    private boolean pageAdminUsers_userEdit(WebRequestData data, User user)
    {
        RemoteRequest request = data.getRequestData();
        // Check for postback
        String username = request.getField("username");
        String password = request.getField("password");
        String passwordConfirm = request.getField("password_confirm");
        String group = request.getField("group");
        String email = request.getField("email");
        int groupid = -1;
        if(username != null && password != null && passwordConfirm != null && group != null && email != null)
        {
            // Parse groupid
            try
            {
                groupid = Integer.parseInt(group);
            }
            catch(NumberFormatException ex)
            {
                groupid = -1;
            }
            // Check the request is valid
            UserGroup ug;
            if(!CSRF.isSecure(data))
                data.setTemplateData("error", "Invalid request; please try again.");
            else if((ug = UserGroup.load(data.getConnector(), groupid)) == null)
                data.setTemplateData("error", "Invalid user-group.");
            else if(password.length() != 0 && (password.length() < user.getPasswordMin() || password.length() > user.getPasswordMax()))
                data.setTemplateData("error", "Password must be "+user.getPasswordMin()+" to "+user.getPasswordMax()+" characters in length!");
            else if(password.length() != 0 && !password.equals(passwordConfirm))
                data.setTemplateData("error", "Passwords do not match!");
            else if(email.length() < user.getEmailMin() || email.length() > user.getEmailMax())
                data.setTemplateData("error", "Invalid e-mail.");
            else
            {
                // Update the user's account
                user.setUsername(username);
                if(password.length() > 0)
                {
                    String salt = getNewSalt();
                    user.setPassword(hash(password, salt));
                    user.setPasswordSalt(salt);
                }
                user.setEmail(email);
                user.setGroup(ug);
                // Attempt to persist
                User.PersistStatus_User ps = user.persist(getCore(), data.getConnector());
                // Handle persist status
                switch(ps)
                {
                    default:
                    case InvalidGroup:
                    case Failed:
                        data.setTemplateData("error", "An error occurred ("+ps.toString()+"); please try again or contact an administrator!");
                        break;
                    case InvalidEmail_exists:
                        data.setTemplateData("error", "E-mail address already in-use.");
                        break;
                    case InvalidEmail_format:
                        data.setTemplateData("error", "Invalid e-mail.");
                        break;
                    case InvalidPassword_length:
                        data.setTemplateData("error", "Password must be "+user.getPasswordMin()+" to "+user.getPasswordMax()+" characters in length!");
                        break;
                    case InvalidUsername_exists:
                        data.setTemplateData("error", "Username already in-use.");
                        break;
                    case InvalidUsername_format:
                        data.setTemplateData("error", "Invalid username; must contain only alpha-numeric characters.");
                        break;
                    case InvalidUsername_length:
                        data.setTemplateData("error", "Username must be "+user.getUsernameMin()+" to "+user.getUsernameMax()+" characters in length.");
                        break;
                    case Success:
                        data.setTemplateData("success", "Updated account settings.");
                        break;
                }
            }
        }
        // Setup the page
        data.setTemplateData("pals_title", "Admin - Users - Edit");
        data.setTemplateData("pals_content", "default_auth/page_admin_user_edit");
        // -- Fields
        data.setTemplateData("csrf", CSRF.set(data));
        data.setTemplateData("edit_user", user);
        data.setTemplateData("username", username != null ? username : user.getUsername());
        data.setTemplateData("email", email != null ? email : user.getEmail());
        data.setTemplateData("groups", UserGroup.load(data.getConnector()));
        data.setTemplateData("groupid", group != null ? groupid : user.getGroup().getGroupID());
        return true;
    }
    private boolean pageAdminUsers_userDelete(WebRequestData data, User user)
    {
        RemoteRequest req = data.getRequestData();
        // Check postback for confirmation
        String confirm = req.getField("confirm");
        if(confirm != null && confirm.equals("1"))
        {
            if(!CSRF.isSecure(data))
                data.setTemplateData("error", "Invalid request; please try again.");
            else if(!Captcha.isCaptchaCorrect(data))
                data.setTemplateData("error", "Invalid captcha verification code.");
            else
            {
                // Unpersist the user
                user.delete(data.getConnector());
                data.getResponseData().setRedirectUrl("/admin/users");
            }
        }
        // Setup the page
        data.setTemplateData("pals_title", "Admin - Users - Delete");
        data.setTemplateData("pals_content", "default_auth/page_admin_user_delete");
        // -- Fields
        data.setTemplateData("csrf", CSRF.set(data));
        data.setTemplateData("edit_user", user);
        return true;
    }
    // Methods - Pages - Admin - Groups ****************************************
    private boolean pageAdminUsers_groupCreate(WebRequestData data)
    {
        RemoteRequest req = data.getRequestData();
        // Check postback
        String title = req.getField("group_title");
        if(title != null)
        {
            UserGroup ug = new UserGroup(title, false, false, false, false, false, false);
            if(!CSRF.isSecure(data))
                data.setTemplateData("error", "Invalid request; please try again.");
            else
            {
                // Attempt to persist new group
                UserGroup.PersistStatus_UserGroup ugps = ug.persist(data.getConnector());
                // Handle persist status
                switch(ugps)
                {
                    case Failed:
                        data.setTemplateData("error", "Failed to create new group; please try again.");
                        break;
                    case Title_Length:
                        data.setTemplateData("error", "Title must be x to x characters in length.");
                        break;
                    case Success:
                        data.getResponseData().setRedirectUrl("/admin/groups/"+ug.getGroupID()+"/edit");
                        break;
                }
            }
        }
        // Setup the page
        data.setTemplateData("pals_title", "Admin - Groups - Create");
        data.setTemplateData("pals_content", "default_auth/page_admin_group_create");
        data.setTemplateData("csrf", CSRF.set(data));
        return true;
    }
    private boolean pageAdminUsers_groupBrowse(WebRequestData data)
    {
        // Fetch groups
        UserGroup[] groups = UserGroup.load(data.getConnector());
        // Setup the page
        data.setTemplateData("pals_title", "Admin - Groups");
        data.setTemplateData("pals_content", "default_auth/page_admin_groups");
        data.setTemplateData("groups", groups);
        return true;
    }
    // Methods - Pages - Shared ************************************************
    private boolean pageAdminUsers_groupUserView(WebRequestData data, UserGroup group)
    {
        final int USERS_PER_PAGE = 20;
        RemoteRequest req = data.getRequestData();
        // Parse page
        int page;
        try
        {
            page = Integer.parseInt(req.getField("p"));
            if(page < 1)
                page = 1;
        }
        catch(NumberFormatException ex)
        {
            page = 1;
        }
        String filter = req.getField("filter");
        // Fetch user models
        ModelUser[] models;
        if(group == null)
            models = ModelUser.load(data.getConnector(), filter, USERS_PER_PAGE+1, (page*USERS_PER_PAGE)-USERS_PER_PAGE);
        else
            models = ModelUser.loadGroup(group, data.getConnector(), USERS_PER_PAGE+1, (page*USERS_PER_PAGE)-USERS_PER_PAGE);
        // Check if we have +1 models, to indicate a next page
        boolean nextPage = models.length > USERS_PER_PAGE;
        if(nextPage)
            models = Arrays.copyOf(models, USERS_PER_PAGE);
        // Setup the page
        if(group != null)
        {
            data.setTemplateData("pals_title", "Admin - Groups - Create");
            data.setTemplateData("pals_content", "default_auth/page_admin_group_view");
            data.setTemplateData("group", group);
        }
        else
        {
            data.setTemplateData("pals_title", "Admin - Users");
            data.setTemplateData("pals_content", "default_auth/page_admin_users");
            data.setTemplateData("filter", filter);
        }
        data.setTemplateData("models", models);
        data.setTemplateData("users_per_page", USERS_PER_PAGE);
        data.setTemplateData("users_page", page);
        if(page > 1)
            data.setTemplateData("users_page_prev", page-1);
        if(page < Integer.MAX_VALUE && nextPage)
            data.setTemplateData("users_page_next", page+1);
        return true;
    }
    private boolean pageAdminUsers_groupEdit(WebRequestData data, UserGroup group)
    {
        RemoteRequest req = data.getRequestData();
        // Check postback
        String groupTitle = req.getField("group_title");
        // -- Optional
        String userLogin   = req.getField("group_user_login");
        
        String markerGeneral    = req.getField("group_marker_general");
        
        String adminModules     = req.getField("group_admin_modules");
        String adminQuestions   = req.getField("group_admin_questions");
        String adminUsers       = req.getField("group_admin_users");
        String adminSystem      = req.getField("group_admin_system");
        if(groupTitle != null)
        {
            if(!CSRF.isSecure(data))
                data.setTemplateData("error", "Invalid request; please try again.");
            else
            {
                // Update model
                group.setUserLogin(userLogin != null && userLogin.equals("1"));
                
                group.setMarkerGeneral(markerGeneral != null && markerGeneral.equals("1"));
                
                group.setAdminModules(adminModules != null && adminModules.equals("1"));
                group.setAdminQuestions(adminQuestions != null && adminQuestions.equals("1"));
                group.setAdminUsers(adminUsers != null && adminUsers.equals("1"));
                group.setAdminSystem(adminSystem != null && adminSystem.equals("1"));
                
                // Attempt to persist
                UserGroup.PersistStatus_UserGroup ugps = group.persist(data.getConnector());
                // Handle persist status
                switch(ugps)
                {
                    default:
                        data.setTemplateData("error", "Failed to update group; please try again.");
                        break;
                    case Success:
                        data.setTemplateData("success", "Updated group.");
                        break;
                }
            }
        }
        // Setup the page
        data.setTemplateData("pals_title", "Admin - Groups - Create");
        data.setTemplateData("pals_content", "default_auth/page_admin_group_edit");
        // -- Fields
        data.setTemplateData("csrf", CSRF.set(data));
        data.setTemplateData("group", group);
        data.setTemplateData("group_title", groupTitle != null ? groupTitle : group.getTitle());
        // -- -- Permissions
        data.setTemplateData("group_user_login", userLogin != null || (groupTitle == null && group.isUserLogin()));
        
        data.setTemplateData("group_marker_general", markerGeneral != null || (groupTitle == null && group.isMarkerGeneral()));
        
        data.setTemplateData("group_admin_modules", adminModules != null || (groupTitle == null && group.isAdminModules()));
        data.setTemplateData("group_admin_questions", adminQuestions != null || (groupTitle == null && group.isAdminQuestions()));
        data.setTemplateData("group_admin_users", adminUsers != null || (groupTitle == null && group.isAdminUsers()));
        data.setTemplateData("group_admin_system", adminSystem != null || (groupTitle == null && group.isAdminSystem()));
        return true;
    }
    private boolean pageAdminUsers_groupDelete(WebRequestData data, UserGroup group)
    {
        RemoteRequest req = data.getRequestData();
        // Check postback
        String confirm = req.getField("confirm");
        if(confirm != null)
        {
            if(!CSRF.isSecure(data))
                data.setTemplateData("error", "Invalid request; please try again.");
            else if(!Captcha.isCaptchaCorrect(data))
                data.setTemplateData("error", "Incorrect captcha verification code.");
            else
            {
                // Unpersist the model
                if(!group.remove(data.getConnector()))
                    data.setTemplateData("error", "Could not remove group; please try again.");
                else
                    data.getResponseData().setRedirectUrl("/admin/groups");
            }
        }
        // Setup the page
        data.setTemplateData("pals_title", "Admin - Groups - Create");
        data.setTemplateData("pals_content", "default_auth/page_admin_group_delete");
        data.setTemplateData("csrf", CSRF.set(data));
        data.setTemplateData("group", group);
        return true;
    }
    // Methods *****************************************************************
    public String getNewSalt()
    {
        return Misc.randomText(getCore(), 32);
    }
    public String hash(String password, String salt)
    {
        try
        {
            byte[] data = password.getBytes();
            byte[] dataSalt = salt.getBytes();
            // Apply salt to data
            // -- Add (likely to overflow) each salt byte to each data byte
            for(int i = 0; i < data.length; i++)
            {
                for(int j = 0; j < dataSalt.length; j++)
                    data[i] += dataSalt[j];
            }
            // Hash the bytes
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            // Return as base64
            return org.apache.commons.codec.binary.Base64.encodeBase64String(md.digest(data));
        }
        catch(NoSuchAlgorithmException ex)
        {
            getCore().getLogging().logEx(LOGGING_ALIAS, "SHA-512 algorithm not found.", ex, Logging.EntryType.Error);
            return null;
        }
    }
    private void setAuth(WebRequestData data, User user, boolean isPrivate)
    {
        data.getSession().setAttribute(SESSION_KEY__USERID, user.getUserID());
        data.setUser(user);
        data.getSession().setIsPrivate(isPrivate);
    }
}
