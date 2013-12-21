package pals.plugins.web;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Random;
import javax.imageio.ImageIO;
import pals.base.NodeCore;
import pals.base.Plugin;
import pals.base.Settings;
import pals.base.TemplateManager;
import pals.base.UUID;
import pals.base.WebManager;
import pals.base.utils.JarIO;
import pals.base.utils.Misc;
import pals.base.web.WebRequestData;

/**
 * A plugin for featuring captcha on web-pages.
 */
public class Captcha extends Plugin
{
    // Constants ***************************************************************
    private static final String SESSION_KEY__CAPTCHA = "captcha";
    // Methods - Constructors **************************************************
    public Captcha(NodeCore core, UUID uuid, JarIO jario, Settings settings, String jarPath)
    {
        super(core, uuid, jario, settings, jarPath);
    }
    // Methods - Event Handlers ************************************************
    @Override
    public boolean eventHandler_pluginLoad(NodeCore core)
    {
        return true;
    }
    @Override
    public void eventHandler_pluginUnload(NodeCore core)
    {
        // Unload URLs
        core.getWebManager().urlsUnregister(this);
        // Unload templates
        core.getTemplates().remove(this);
    }
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
    public boolean eventHandler_registerUrls(NodeCore core, WebManager web)
    {
        if(!web.urlsRegister(this, new String[]{
            "captcha"
        }))
            return false;
        return true;
    }
    @Override
    public boolean eventHandler_registerTemplates(NodeCore core, TemplateManager manager)
    {
        if(!manager.load(this, "templates"))
            return false;
        return true;
    }
    @Override
    public boolean eventHandler_webRequest(WebRequestData data)
    {
        switch(data.getRequestData().getRelativeUrl())
        {
            case "captcha":
                // Output captcha image as response data
                data.getResponseData().setResponseType("image/png");
                data.getResponseData().setBuffer(captchaBuild(data));
                return true;
        }
        return false;
    }

    @Override
    public String getTitle()
    {
        return "PALS: Captcha Verification";
    }
    // Methods *****************************************************************
    /**
     * Generates a captcha image.
     * 
     * @param data The data for the current web request.
     * @return Byte-array of image data.
     */
    public byte[] captchaBuild(WebRequestData data)
    {
        // Constants
        final int   captchaWidth = 240,
                    captchaHeight = 90,
                    charsMin = 4,
                    charsMax = 8,
                    fontSizeMin = 16,
                    fontSizeMax = 20;
        // Fetch RNG
        Random rand = data.getCore().getRNG();
        // Generate random string and place into session data
        String text = Misc.randomText(data.getCore(), charsMin+rand.nextInt(charsMax-charsMin)).toLowerCase();
        data.getSession().setAttribute(SESSION_KEY__CAPTCHA, text);
        // Create image for rendering
        BufferedImage img = new BufferedImage(captchaWidth, captchaHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics g = img.createGraphics();
        // Load font for rendering chars
        Font f = new Font("Arial", Font.BOLD | Font.ITALIC, fontSizeMin+rand.nextInt(fontSizeMax-fontSizeMin));
        FontMetrics fm = g.getFontMetrics(f);
        g.setFont(f);
        // Render noise lines
        noise(rand, g, captchaWidth, captchaHeight, 8);
        // Render text
        int offX = 15 + rand.nextInt(25), offY = fm.getHeight() + rand.nextInt(35);
        for(char c : text.toCharArray())
        {
            randomColour(rand, g);
            g.drawString(String.valueOf(c), offX, offY);
            offX += fm.charWidth(c) + rand.nextInt(20);
            offY += -5 + rand.nextInt(5);
            if(offY <= fm.getHeight())
                offY = fm.getHeight();
            else if(offY >= captchaHeight+fm.getHeight())
                offY = captchaHeight;
        }
        // Render more noise lines
        noise(rand, g, captchaWidth, captchaHeight, 8);
        // Output the image
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try
        {
            ImageIO.write(img, "png", baos);
            baos.flush();
            return baos.toByteArray();
        }
        catch(IOException ex)
        {
            return null;
        }
    }
    private void noise(Random rand, Graphics g, int width, int height, int total)
    {
        for(int i = 0; i < total; i++)
        {
            randomColour(rand, g);
            g.drawLine(rand.nextInt(width), rand.nextInt(height), rand.nextInt(width), rand.nextInt(height));
        }
    }
    private void randomColour(Random rand, Graphics g)
    {
        g.setColor(new Color(rand.nextInt(255), rand.nextInt(255), rand.nextInt(255)));
    }
    /**
     * Indicates if a captcha value is correct.
     * 
     * @param data The data for the current web request.
     * @param input The captcha value provided by the user.
     * @return True = correct, false = incorrect.
     */
    public static boolean isCaptchaCorrect(WebRequestData data, String input)
    {
        if(input == null || input.length() == 0)
            return false;
        String original = data.getSession().getAttribute(SESSION_KEY__CAPTCHA);
        return original != null && original.equals(input);
    }
    /**
     * Indicates if a captcha value is correct.
     * 
     * @param data The data for the current web request.
     * @return True = correct, false = incorrect.
     */
    public static boolean isCaptchaCorrect(WebRequestData data)
    {
        return isCaptchaCorrect(data, data.getRequestData().getField("captcha"));
    }
}
