package org.linkedin.contest.ants.zoran;

import org.linkedin.contest.ants.api.*;

import java.io.ByteArrayOutputStream;
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

	public final static String uncompressed(String data) {
		Inflater inflator = new Inflater();
		try {
//			int i = data.indexOf(' ');
//			if (i<0 || i > 10) return null;
//			String sn = data.substring(0, i);
//			if (!Constants.isNumber(sn)) return null;
//			int adler = Integer.parseInt(sn);
//			data = data.substring(i + 1);
//			inflator.setDictionary(b);
			byte[] input = decode64(data);
//			byte[] input = data.getBytes(compressionEncoding);
			inflator.setInput(input);
			ByteArrayOutputStream bos = new ByteArrayOutputStream(input.length);
			byte[] buf = new byte[1024];
			while (true) {
				int count = inflator.inflate(buf);
				if (count > 0) {
					bos.write(buf, 0, count);
				} else if (count == 0 && inflator.finished()) {
					break;
				} else {
					System.out.print("bad zip data, size:" + input.length);
					return null;
				}
			}
			byte[] result = bos.toByteArray();
			return new String(result, 0, result.length, compressionEncoding);
		} catch(java.io.UnsupportedEncodingException ex) {
			System.out.print(ex);
		} catch (DataFormatException ex) {
			System.out.print(ex);
		} finally {
			inflator.end();
		}
		return null;
	}

	// Compressed representation of string
	public static String compressed(String inputString) {
		try {
			byte[] input = inputString.getBytes(compressionEncoding);
			byte[] output = new byte[input.length];
			Deflater compresser = new Deflater();
			compresser.setInput(input);
			compresser.finish();
			int compressedDataLength = compresser.deflate(output);
			assert compressedDataLength > 0;
			String s = encode64(output, compressedDataLength);
			return s;
		} catch(java.io.UnsupportedEncodingException ex) {
			System.out.print(ex);
		}
		return null;
	}

    private final static char[] ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray();

    private static int[]  toInt   = new int[128];

    static {
        for(int i=0; i< ALPHABET.length; i++){
            toInt[ALPHABET[i]]= i;
        }
    }

    /**
     * Translates the specified byte array into Base64 string.
     *
     * @param buf the byte array (not null)
     * @return the translated Base64 string (not null)
     */
    public static String encode64(byte[] buf, int size){
        char[] ar = new char[((size + 2) / 3) * 4];
        int a = 0;
        int i=0;
        while(i < size){
            byte b0 = buf[i++];
            byte b1 = (i < size) ? buf[i++] : 0;
            byte b2 = (i < size) ? buf[i++] : 0;

            int mask = 0x3F;
            ar[a++] = ALPHABET[(b0 >> 2) & mask];
            ar[a++] = ALPHABET[((b0 << 4) | ((b1 & 0xFF) >> 4)) & mask];
            ar[a++] = ALPHABET[((b1 << 2) | ((b2 & 0xFF) >> 6)) & mask];
            ar[a++] = ALPHABET[b2 & mask];
        }
        switch(size % 3){
            case 1: ar[--a]  = '=';		//$FALL-THROUGH$
            case 2: ar[--a]  = '=';
        }
        return new String(ar);
    }

    /**
     * Translates the specified Base64 string into a byte array.
     *
     * @param s the Base64 string (not null)
     * @return the byte array (not null)
     */
    public static byte[] decode64(String s){
        int delta = s.endsWith( "==" ) ? 2 : s.endsWith( "=" ) ? 1 : 0;
        byte[] buffer = new byte[s.length()*3/4 - delta];
        int mask = 0xFF;
        int index = 0;
        for(int i=0; i< s.length(); i+=4){
            int c0 = toInt[s.charAt( i )];
            int c1 = toInt[s.charAt( i + 1)];
            buffer[index++]= (byte)(((c0 << 2) | (c1 >> 4)) & mask);
            if(index >= buffer.length){
                return buffer;
            }
            int c2 = toInt[s.charAt( i + 2)];
            buffer[index++]= (byte)(((c1 << 4) | (c2 >> 2)) & mask);
            if(index >= buffer.length){
                return buffer;
            }
            int c3 = toInt[s.charAt( i + 3 )];
            buffer[index++]= (byte)(((c2 << 6) | c3) & mask);
        }
        return buffer;
    } 

}
