package pals.plugins.auth;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import pals.base.Logging;
import pals.base.NodeCore;
import pals.base.Plugin;
import pals.base.PluginManager;
import pals.base.Settings;
import pals.base.TemplateManager;
import pals.base.UUID;
import pals.base.WebManager;
import pals.base.auth.User;
import pals.base.auth.UserGroup;
import pals.base.utils.JarIO;
import pals.base.utils.Misc;
import pals.base.web.RemoteRequest;
import pals.base.web.WebRequestData;
import pals.base.web.security.CSRF;
import pals.base.web.security.Escaping;
import pals.plugins.web.Captcha;

/**
 * Default authentication handler for PALS.
 */
public class DefaultAuth extends Plugin
{
    // Constants ***************************************************************
    private static final String SESSION_KEY__USERID = "defaultauth_userid";
    // Methods - Constructors **************************************************
    public DefaultAuth(NodeCore core, UUID uuid, JarIO jario, Settings settings, String jarPath)
    {
        super(core, uuid, jario, settings, jarPath);
    }
    // Methods - Event Handlers ************************************************
    @Override
    public boolean eventHandler_pluginInstall(NodeCore core)
    {
        return true;
    }
    @Override
    public boolean eventHandler_pluginUninstall(NodeCore core)
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
            "account/logout"
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
        switch(data.getRequestData().getRelativeUrl())
        {
            case "account/settings":
                return pageAccountSettings(data);
            case "account/register":
                return pageAccountRegister(data);
            case "account/login":
                return pageAccountLogin(data);
            case "account/logout":
                return pageAccountLogout(data);
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
    private boolean pageAccountSettings(WebRequestData data)
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
        String csrf = request.getField("csrf");
        if(email != null || newPassword != null || newPasswordConfirm != null)
        {
            // Validate request
            if(!CSRF.isSecure(data, csrf))
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
    private boolean pageAccountRegister(WebRequestData data)
    {
        if(data.getUser() != null)
            return false;
        // Check form data
        RemoteRequest request = data.getRequestData();
        String username = request.getField("username");
        String password = request.getField("password");
        String passwordConfirm = request.getField("password_confirm");
        String email = request.getField("email");
        String emailConfirm = request.getField("email_confirm");
        String csrf = request.getField("csrf");
        
        if(username != null && password != null && passwordConfirm != null && email != null && emailConfirm != null)
        {
            // Check security
            if(!CSRF.isSecure(data, csrf))
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
                    User user = User.create();
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
    private boolean pageAccountLogin(WebRequestData data)
    {
        if(data.getUser() != null)
            return false;
        // Check form data
        RemoteRequest request = data.getRequestData();
        String username = request.getField("username");
        String password = request.getField("password");
        String csrf = request.getField("csrf");
        if(username != null && password != null && csrf != null)
        {
            // Fetch the user's information
            User user;
            if(username.length() == 0 || password.length() == 0)
                data.setTemplateData("error", "Invalid username/password!");
            else if((user = User.load(data.getConnector(), username)) == null)
                data.setTemplateData("error", "Invalid username/password!");
            else
            {
                // Generate a hash using the user's salt, compare passwords
                String passHashed = hash(password, user.getPasswordSalt());
                if(passHashed.equals(user.getPassword()))
                {
                    // Correct! Setup the user and redirect to home
                    data.getSession().setAttribute(SESSION_KEY__USERID, user.getUserID());
                    data.setUser(user);
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
        data.setTemplateData("login_username", Escaping.htmlEncode(username));
        data.setTemplateData("csrf", CSRF.set(data));
        return true;
    }
    private boolean pageAccountLogout(WebRequestData data)
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
    // Methods *****************************************************************
    private String getNewSalt()
    {
        return Misc.randomText(getCore(), 32);
    }
    private String hash(String password, String salt)
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
            getCore().getLogging().log("SHA-512 algorithm not found.", ex, Logging.EntryType.Error);
            return null;
        }
    }
}
