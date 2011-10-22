package org.linkedin.contest.ants.impl;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class MessageCodec {

	private SecretKeySpec key = null;
	private IvParameterSpec ivSpec = null;
	private Cipher cipher = null;
	
	public MessageCodec()
	{
		this("SOME_KEY","SOME_IV8");
	}
	
	public MessageCodec(String key, String iv)
	{
		this(key.getBytes(), iv.getBytes());
	}
	
    public MessageCodec(byte[] keyBytes, byte[] ivBytes)
    {
    	// wrap key data in Key/IV specs to pass to cipher
    	key = new SecretKeySpec(keyBytes, "DES");
    	ivSpec = new IvParameterSpec(ivBytes);
    	try
    	{
	    	// create the cipher with the algorithm you choose
	    	// see javadoc for Cipher class for more info, e.g.
	    	cipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
    	}
    	catch (Exception e)
    	{
    		e.printStackTrace();
    	}
    	
    }

    public String encrypt(String inputStr) 
    {
    	byte[] input = inputStr.getBytes();
    	try
    	{
	    	cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);
	    	byte[] encrypted = new byte[cipher.getOutputSize(input.length)];
	    	int enc_len = cipher.update(input, 0, input.length, encrypted, 0);
	    	enc_len += cipher.doFinal(encrypted, enc_len);
	    	
	    	return new String(encrypted);
    	}
    	catch (Exception e)
    	{
    		e.printStackTrace();
    		return null; // bury the exception
    	}
    }

    public String decrypt(String inputStr)
    {
    	byte[] input = inputStr.getBytes();
    	try
    	{
	    	cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);
	    	byte[] decrypted = new byte[cipher.getOutputSize(input.length)];
	    	int dec_len = cipher.update(input, 0, input.length, decrypted, 0);
	    	dec_len += cipher.doFinal(decrypted, dec_len);
	    	
			return new String(decrypted).trim();
    	}
    	catch (Exception e)
    	{
    		e.printStackTrace();
    		return null; // bury the exception
    	}
    }
    
    
    public static void main(String[] args)
    {
    	String[] testers = { "TEST", "BLASDFASDFASDFADSF", "JAMIE_M", "SOME_OTHER_STRING", "2323refsf9r293f9f" };
    	try
    	{
    		MessageCodec codec = new MessageCodec();
    		for ( String tester : testers )
    		{
	    		System.out.println(tester);
	    		String encoded = codec.encrypt(tester);
	    		System.out.println(encoded);
	    		String decoded = codec.decrypt(encoded);
	    		System.out.println(decoded);
	    		if ( !tester.equals(decoded) )
	    		{
	    			throw new Exception("failure during codec, tester=" + tester + ", encoded=" + encoded + ", decoded=" + decoded);
	    		}
    		}
    	} 
    	catch (Exception e)
    	{
    		System.out.println("Noooo!");
    		e.printStackTrace();
    	}
    }
}
