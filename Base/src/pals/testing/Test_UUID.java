package pals.testing;

import pals.base.UUID;

public class Test_UUID
{
    public static void main(String[] args)
    {
        UUID uuid;
        // Test 1 - valid UUID, hyphens
        uuid = UUID.parse("a76d8ed8-b491-4f39-a39f-dd4a2b855a60");
        if(uuid == null)
            System.err.println("Test 1: failed to parse.");
        else
            System.out.println("Test 1: parsed as ~ '" + uuid.getHex() + "' and with hyphens '" + uuid.getHexHyphens() + "'.");
        // Test 2 - valid UUID, without hyphens
        uuid = UUID.parse("a76d8ed8b4914f39a39fdd4a2b855a60");
        if(uuid == null)
            System.err.println("Test 2: failed to parse.");
        else
            System.out.println("Test 2: parsed as ~ '" + uuid.getHex() + "' and with hyphens '" + uuid.getHexHyphens() + "'.");
        // Test 3 - invalid UUID
        uuid = UUID.parse("ZZZZZZZZ-ZZZZ-ZZZZ-ZZZZ-ZZZZZZZZZZZZ");
        if(uuid != null)
            System.err.println("Test 3: parsed invalid UUID.");
        else
            System.out.println("Test 3: success! Could not parse invalid UUID.");
        // Test 4 - test bytes of UUID by converting to bytes and re-parsing
        String testBytesUUID = "9328891f-5b6b-497a-b34a-b5777929c1a2";
        uuid = UUID.parse(testBytesUUID);
        if(uuid == null)
            System.err.println("Test 4: failed to parse.");
        else
        {
            System.out.println("Test 4: parsed as ~ '" + uuid.getHex() + "' and with hyphens '" + uuid.getHexHyphens() + "'.");
            byte[] data = uuid.getBytes();
            uuid = UUID.parse(data);
            if(uuid == null)
                System.err.println("Test 4: failed to parse UUID from bytes.");
            else if(uuid.getHexHyphens().equals(testBytesUUID.toUpperCase()))
                System.out.println("Test 4: parsed bytes ~ '" + uuid.getHex() + "' and with hyphens '" + uuid.getHexHyphens() + "'.");
            else
                System.err.println("Test 4: conversion from bytes came back with a different UUID! '" + uuid.getHex() + "' vs original '" + testBytesUUID.toUpperCase() + "'");
        }
        // Test 5 - version four UUID
        uuid = UUID.generateVersion4();
        String hex = null;
        if(uuid == null || ((hex = uuid.getHex()) == null) || hex.charAt(12) != '4' || (hex.charAt(16) != '8' && hex.charAt(16) != '9' && hex.charAt(16) != 'A' && hex.charAt(16) != 'B'))
        {
            if(uuid != null && hex != null)
                System.err.println("Test 5 - failed to create version 4 UUID:  '" + uuid.getHex() + "' and with hyphens '" + uuid.getHexHyphens() + "'");
            else
                System.err.println("Test 5 - failed to create version 4 UUID!");
        }
        else
            System.out.println("Test 5 - version 4 UUID created: '" + uuid.getHex() + "' and with hyphens '" + uuid.getHexHyphens() + "'.");
    }
}
