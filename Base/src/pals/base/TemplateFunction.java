package pals.base;

import pals.base.web.WebRequestData;

/**
 * The interface used by anonymous classes for template functions.
 */
public interface TemplateFunction
{
    public void render(WebRequestData data, String content, String[] args);
}
