package org.linkedin.contest.ants.zoran;

import org.linkedin.contest.ants.api.*;
import java.util.Stack;
import java.util.zip.*;

public class TransmitMessage extends Operation {

	protected String message;
	private Stack<String> stack;

	TransmitMessage(Role role) {
		super(role);
	}

	// Maximum data size to send in one 'Say' action (should be low enough so that the little header we send + this size < 255)
	private static final int maxSize = 248;

	public void setMessage(String message) {
		assert !isActive();
		this.message = message;
		if (message != null && message.length() > 0) {
			stack = new Stack<String>();
			String remaining = compressed(message);
			String current;
			while (remaining != null) {
				if (remaining.length() > maxSize) {
					current = remaining.substring(0, maxSize);
					remaining = remaining.substring(maxSize);
				} else {
					current = remaining;
					remaining = null;
				}
				stack.add(current);
			}
			activate(true);
		} else {
			stack = null;
			activate(false);
		}
	}

	@Override
	Action effectiveAct() {
		if (stack != null) {
			assert stack.size() > 0;
			int page = stack.size();
			String s = stack.pop();
			if (stack.isEmpty()) {
				activate(false);
				stack = null;
			}
			s = String.format("%d %d %s", ant.id, page, s);
			assert s.length() < 255;
			return new Say(s, Direction.here);
		}
		return null;
	}

	private final static String compressionEncoding = "US-ASCII";
	private final static int compressionBufferSize = 50000000;

	// Uncompressed data (as made by 'compress')
	public static String uncompressed(String data) {
		 try {
		     // Decompress the bytes
		     byte[] output = data.getBytes(compressionEncoding);
		     Inflater decompresser = new Inflater();
		     decompresser.setInput(output, 0, data.length());
		     byte[] result = new byte[compressionBufferSize];
		     int resultLength = decompresser.inflate(result);
		     if (resultLength == 0 && decompresser.needsDictionary()) {
		    	 decompresser.getAdler();
		    	 decompresser.setDictionary(b);
		     }
		     decompresser.end();
		     // Decode the bytes into a String
		     return new String(result, 0, resultLength, compressionEncoding);
		 } catch(java.io.UnsupportedEncodingException ex) {
			 System.out.print(ex);
		 } catch (java.util.zip.DataFormatException ex) {
			 System.out.print(ex);
		 }
		 return null;
	}

	// Compressed representation of string
	public static String compressed(String inputString) {
		 try {
		     // Encode a String into bytes
		     byte[] input = inputString.getBytes(compressionEncoding);
		     // Compress the bytes
		     byte[] output = new byte[compressionBufferSize];
		     Deflater compresser = new Deflater();
		     compresser.setInput(input);
		     compresser.finish();
		     int compressedDataLength = compresser.deflate(output);
		     return new String(output, 0, compressedDataLength, compressionEncoding);
		 } catch(java.io.UnsupportedEncodingException ex) {
			 System.out.print(ex);
		 }
		 return null;
	}
	
}
