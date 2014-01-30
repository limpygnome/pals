/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package javaapplication7;

/**
 *
 * @author limpygnome
 */
public class JavaApplication7 {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws ClassNotFoundException
    {
        Class c = Class.forName("java.lang.int");
        System.out.println(c.getName());
    }
    
}
