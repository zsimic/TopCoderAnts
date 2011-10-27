package org.linkedin.contest.ants.zoran;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;

public class Logger {

	private static HashMap<String, OutputStreamWriter> streamWriters = new HashMap<String, OutputStreamWriter>();
	private static boolean isRunningWithoutAssertions = true;
	static {
		try {
			assert false;
		} catch (AssertionError ex) {
			isRunningWithoutAssertions = false;
		}
	}

	// Trace progress of 'ant'
	public static void trace(CommonAnt ant, String info) {
		if (isRunningWithoutAssertions) return;
		String fileName = getFileName("trace", ant.id);
		String message = ant.toString() + " " + info;
		append(fileName, message);
	}

	// Output some unusual information on 'ant'
	public static void inform(CommonAnt ant, String info) {
		if (isRunningWithoutAssertions) return;
		String fileName = getFileName("info", ant.id);
		String message = ant.toString() + " " + info;
		append(fileName, message);
	}

	// Output error message for 'ant', need to check what happened here
	public static void error(CommonAnt ant, String info) {
		if (isRunningWithoutAssertions) return;
		String fileName = getFileName("error", ant.id);
		String message = ant.toString() + " " + info;
		append(fileName, message);
		System.err.print(message);
	}

	// Dump current board representation for ant
	public static void dumpBoard(CommonAnt ant) {
		if (isRunningWithoutAssertions) return;
		String fileName = getFileName("board", ant.id, ant.turn);
		String representation = String.format("%s\n%s", ant.toString(), ant.board.representation(true));
		writeAndClose(fileName, representation);
	}

	// Standard file name
	private static String getFileName(String prefix, int id) {
		assert id > 0 && id <= 50;
		return String.format("%s%02d", prefix, id);
	}

	// Standard file name with turn number
	private static String getFileName(String prefix, int id, int turn) {
		assert id > 0 && id <= 50 && turn > 0;
		return String.format("%s%02d_%06d", prefix, id, turn);
	}

	// Append message to file with fileName
	private static void append(String fileName, String message) {
		try {
			assert message != null && message.length() > 0;
			OutputStreamWriter stream = getStream(fileName);
			stream.write(message);
			if (message.charAt(message.length() - 1) != '\n') stream.write("\n");
			stream.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Write message to file with fileName and close it
	private static void writeAndClose(String fileName, String message) {
		try {
			FileOutputStream outputStream = new FileOutputStream(fullPath(fileName));
			OutputStreamWriter streamWriter = new OutputStreamWriter(outputStream);
			streamWriter.write(message);
			streamWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Output stream corresponding to file with fileName
	private static OutputStreamWriter getStream(String fileName) {
		OutputStreamWriter streamWriter = streamWriters.get(fileName);
		if (streamWriter == null) {
			try {
				FileOutputStream outputStream = new FileOutputStream(fullPath(fileName));
				streamWriter = new OutputStreamWriter(outputStream);
				streamWriters.put(fileName, streamWriter);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		return streamWriter;
	}

	// Full path for file with fileName
	private static String fullPath(String fileName) {
		return String.format("logs/%s.txt",fileName);
	}
}
