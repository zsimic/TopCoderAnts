package org.linkedin.contest.ants.zoran;

import org.linkedin.contest.ants.api.*;
import java.io.ByteArrayOutputStream;
import java.util.Stack;
import java.util.Arrays;
import java.util.zip.*;

public class TransmitMessage extends Operation {

	protected char messageType;
	protected String messageBody;
	private Direction direction;			// Direction in which to transmit message
	private int totalPages;
	private Stack<String> stack;

	TransmitMessage(Role role) {
		super(role);
	}

	// Maximum data size to send in one 'Say' action (should be low enough so that the little header we send + this size < 255)
	private static final int maxSize = 248;

	public void setBoardInfo(Direction dir) {
		setMessage(dir, Constants.messageBoard, ant.board.representation(false));
	}

	public void setMessage(Direction dir, char type, String msg) {
		assert !isActive();
		direction = dir;
		messageType = type;
		messageBody = msg;
		if (msg != null && msg.length() > 0) {
			assert msg.length() > 10;
			stack = new Stack<String>();
			String remaining = compressed(msg);
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
			totalPages = stack.size();
			activate(true);
		} else {
			clear();
		}
	}

	public void clear() {
		messageType = '\0';
		messageBody = null;
		totalPages = 0;
		stack = null;
		activate(false);
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
			char antIdChar = Constants.encodedCharInt(ant.id);
			char pageChar = Constants.encodedCharInt(page);
			char totalPagesChar = Constants.encodedCharInt(totalPages);
			s = String.format("%c%c%c%c%s", messageType, antIdChar, pageChar, totalPagesChar, s);
			assert s.length() < 255;
			return new Say(s, direction);
		}
		return null;
	}

	public final static String uncompressed(CommonAnt ant, String data) {
		Inflater inflator = new Inflater();
		try {
			byte[] input = decoded64(data);
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
					assert false;
				}
			}
			byte[] result = bos.toByteArray();
			return new String(result, 0, result.length, Constants.compressionEncoding);
		} catch(java.io.UnsupportedEncodingException ex) {
			Logger.error(ant, ex.toString());
		} catch (DataFormatException ex) {
			Logger.error(ant, ex.toString());
		} finally {
			inflator.end();
		}
		return null;
	}

	// Compressed representation of string
	public String compressed(String inputString) {
		try {
			byte[] input = inputString.getBytes(Constants.compressionEncoding);
			byte[] output = new byte[input.length];
			Deflater compresser = new Deflater();
			compresser.setInput(input);
			compresser.finish();
			int compressedDataLength = compresser.deflate(output);
			assert compressedDataLength > 0;
			String s = encoded64(output, compressedDataLength);
			return s;
		} catch(java.io.UnsupportedEncodingException ex) {
			Logger.error(ant, ex.toString());
		}
		return null;
	}

//	import sun.misc.BASE64Decoder;
//	import sun.misc.BASE64Encoder;
//	private static String encoded64sun(byte[] buf, int size){
//		BASE64Encoder encoder = new BASE64Encoder();
//		return encoder.encode(buf);
//	}
//	private static byte[] decoded64sun(String s) throws IOException{
//		BASE64Decoder decoder = new BASE64Decoder();
//		return decoder.decodeBuffer(s);
//	}

	private static final char[] CA = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray();
	private static final int[] IA = new int[256];
	static {
		Arrays.fill(IA, -1);
		for (int i = 0, iS = CA.length; i < iS; i++)
			IA[CA[i]] = i;
		IA['='] = 0;
	}

	/** Decodes a BASE64 encoded string that is known to be reasonably well formatted. The preconditions are:<br>
	 * + The array must have no line separators at all (one line).<br>
	 * + The array must not contain illegal characters within the encoded string<br>
	 * + The array CAN have illegal characters at the beginning and end, those will be dealt with appropriately.<br>
	 * @param s The source string must be non empty.
	 * @return The decoded array of bytes.
	 */
	public final static byte[] decoded64(String s) {
		assert s != null && s.length() > 0;
		int sLen = s.length();
		int sIx = 0, eIx = sLen - 1;		// Start and end index after trimming.
		// Trim illegal chars from start
		while (sIx < eIx && IA[s.charAt(sIx) & 0xff] < 0)
			sIx++;
		// Trim illegal chars from end
		while (eIx > 0 && IA[s.charAt(eIx) & 0xff] < 0)
			eIx--;
		// get the padding count (=) (0, 1 or 2)
		int pad = s.charAt(eIx) == '=' ? (s.charAt(eIx - 1) == '=' ? 2 : 1) : 0;  // Count '=' at end.
		int cCnt = eIx - sIx + 1;   // Content count including possible separators
		int sepCnt = sLen > 76 ? (s.charAt(76) == '\r' ? cCnt / 78 : 0) << 1 : 0;
		int len = ((cCnt - sepCnt) * 6 >> 3) - pad; // The number of decoded bytes
		byte[] dArr = new byte[len];       // Preallocate byte[] of exact length
		// Decode all but the last 0 - 2 bytes.
		int d = 0;
		for (int cc = 0, eLen = (len / 3) * 3; d < eLen;) {
			// Assemble three bytes into an int from four "valid" characters.
			int i = IA[s.charAt(sIx++)] << 18 | IA[s.charAt(sIx++)] << 12 | IA[s.charAt(sIx++)] << 6 | IA[s.charAt(sIx++)];
			// Add the bytes
			dArr[d++] = (byte) (i >> 16);
			dArr[d++] = (byte) (i >> 8);
			dArr[d++] = (byte) i;
			// If line separator, jump over it.
			if (sepCnt > 0 && ++cc == 19) {
				sIx += 2;
				cc = 0;
			}
		}
		if (d < len) {
			// Decode last 1-3 bytes (including '=') into 1-3 bytes
			int i = 0;
			for (int j = 0; sIx <= eIx - pad; j++)
				i |= IA[s.charAt(sIx++)] << (18 - j * 6);
			for (int r = 16; d < len; r -= 8)
				dArr[d++] = (byte) (i >> r);
		}
		return dArr;
	}

	/** Encodes a raw byte array into a BASE64 <code>String</code> representation i accordance with RFC 2045.
	 * @param sArr The bytes to convert. If <code>null</code> or length 0 an empty array will be returned.
	 * @param lineSep Optional "\r\n" after 76 characters, unless end of file.<br>
	 * No line separator will be in breach of RFC 2045 which specifies max 76 per line but will be a
	 * little faster.
	 * @return A BASE64 encoded array. Never <code>null</code>.
	 */
	private final static String encoded64(byte[] sArr, int sLen) {
		assert sArr != null && sLen > 0;
		int eLen = (sLen / 3) * 3;              // Length of even 24-bits.
		int dLen = ((sLen - 1) / 3 + 1) << 2; 	// Length of returned array (=returned character count)
		char[] dArr = new char[dLen];
		// Encode even 24-bits
		for (int s = 0, d = 0; s < eLen;) {
			// Copy next three bytes into lower 24 bits of int, paying attention to sign.
			int i = (sArr[s++] & 0xff) << 16 | (sArr[s++] & 0xff) << 8 | (sArr[s++] & 0xff);
			// Encode the int into four chars
			dArr[d++] = CA[(i >>> 18) & 0x3f];
			dArr[d++] = CA[(i >>> 12) & 0x3f];
			dArr[d++] = CA[(i >>> 6) & 0x3f];
			dArr[d++] = CA[i & 0x3f];
		}
		// Pad and encode last bits if source isn't even 24 bits.
		int left = sLen - eLen; // 0 - 2.
		if (left > 0) {
			// Prepare the int
			int i = ((sArr[eLen] & 0xff) << 10) | (left == 2 ? ((sArr[sLen - 1] & 0xff) << 2) : 0);
			// Set last four chars
			dArr[dLen - 4] = CA[i >> 12];
			dArr[dLen - 3] = CA[(i >>> 6) & 0x3f];
			dArr[dLen - 2] = left == 2 ? CA[i & 0x3f] : '=';
			dArr[dLen - 1] = '=';
		}
		return new String(dArr);
	}

}
