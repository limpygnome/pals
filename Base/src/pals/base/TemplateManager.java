/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package pals.base;

import java.util.HashMap;

/**
 *
 * @author limpygnome
 */
public class TemplateManager
{
    private HashMap<String, TemplateFunction> functions;
    private HashMap<String, Template> templates;
    // ability to configure so:
    // -- list of possible templates are registered with file path, relative path
    // -- ability to either load x templates, x size overall/bytes or all templates
    // -- -- load templates in and out like a page file cache type of thing?
    
    // issue: templates inside jars
    // -- cannot store a path to them...unless we're intelligent and state the
    // -- jar and use a class-loader
    
    // maybe templates come with jar and self-extract on install
    // -- empties config file etc into real-dir
    
    // -- or optionally install/load from path
    
    // method to render
}
