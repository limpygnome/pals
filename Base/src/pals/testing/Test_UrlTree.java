package pals.testing;

import pals.base.UUID;
import pals.base.UrlTree;

public class Test_UrlTree
{
    public static void main(String[] args)
    {
        UrlTree tree = new UrlTree();
        
        UUID uuid1 = UUID.generateVersion4();
        UUID uuid2 = UUID.generateVersion4();
        
        System.out.println("UUID 1: " + uuid1.getHexHyphens());
        System.out.println("UUID 2: " + uuid2.getHexHyphens());
        
        System.out.println("url 1: " + tree.add(uuid1, "a"));
        System.out.println("url 2: " + tree.add(uuid1, "a/b"));
        System.out.println("url 3: " + tree.add(uuid1, "a/b/c"));
        System.out.println("url 3b: " + tree.add(uuid1, "a/b/c"));
        System.out.println("url 4: " + tree.add(uuid1, ""));
        System.out.println("url 5: " + tree.add(uuid2, "a/a"));
        System.out.println("url 6: " + tree.add(uuid2, "1/2/3/4/5/6/7/8/9/10"));
        
        System.out.println("url 7: " + tree.add(uuid1, "cat/dog/wolf"));
        System.out.println("url 7b: " + tree.add(uuid2, "cat/dog"));
        
        System.out.println("Test 1:");
        for(UUID uuid : tree.getUUIDs("a/b"))
        {
            System.out.println("- " + uuid.getHexHyphens());
        }
        
        System.out.println("Test 2:");
        for(UUID uuid : tree.getUUIDs("a"))
        {
            System.out.println("- " + uuid.getHexHyphens());
        }
        
        System.out.println("Test 3:");
        for(UUID uuid : tree.getUUIDs("a/b/c"))
        {
            System.out.println("- " + uuid.getHexHyphens());
        }
        
        System.out.println("Test 4:");
        for(UUID uuid : tree.getUUIDs("1/2/3/4/5/6/7/8/9/10/11/12/13/14/15/16"))
        {
            System.out.println("- " + uuid.getHexHyphens());
        }
        
        System.out.println("Test 5:");
        for(UUID uuid : tree.getUUIDs("cat/dog/wolf/fox/"))
        {
            System.out.println("- " + uuid.getHexHyphens());
        }
        
        System.out.println("URLs in collection:");
        for(String s : tree.getUrls())
        {
            System.out.println("'" + s + "'");
        }
        
        System.out.println("Test 6:");
        tree.remove(uuid2);
        for(UUID uuid : tree.getUUIDs("cat/dog/wolf/fox/"))
        {
            System.out.println("- " + uuid.getHexHyphens());
        }
        
        System.out.println("Test 7:");
        tree.remove(uuid1);
        for(UUID uuid : tree.getUUIDs("cat/dog/wolf/fox/"))
        {
            System.out.println("- " + uuid.getHexHyphens());
        }
        
        System.out.println("URLs in collection:");
        for(String s : tree.getUrls())
        {
            System.out.println("'" + s + "'");
        }
    }
}
