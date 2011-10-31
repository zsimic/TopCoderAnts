package org.linkedin.contest.ants.zoran;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;

public class Logger {

	private static boolean omitLogs = false;		// Set to 'false' to enable output

	private static HashMap<String, OutputStreamWriter> streamWriters = new HashMap<String, OutputStreamWriter>();
	static {
		if (!omitLogs) {
			// Clear previous run logs
			File logDir = new File("logs");
			if (logDir.exists() && logDir.isDirectory()) {
				for (File file : logDir.listFiles()) {
					if (!file.isDirectory()) {
						if (!file.delete()) {
							System.err.print(String.format("Can't delete %s\n", file.getName()));
						}
					}
				}
			}
		}
	}

	private static String turnTooLongMessage(long elapsedTimeMillis) {
		return String.format("Turn took %d ms\n", elapsedTimeMillis);
	}

//L	public static void logAverageRunTime(CommonAnt ant) {														// Logger.
//L		if (omitLogs) return;																					// Logger.
//L		String msg = String.format("Average run-time: %d", Math.round(ant.totalRunTime * 1000.0 / ant.turn));	// Logger.
//L		if (omitLogs) {																							// Logger.
//L			System.err.print(message(ant, msg));																// Logger.
//L		} else {																								// Logger.
//L			inform(ant, msg);																					// Logger.
//L		}																										// Logger.
//L	}																											// Logger.

	// Log how long a turn took to complete
	public static void logRunTime(CommonAnt ant, long elapsedTimeMillis) {
		if (elapsedTimeMillis > 10) {
			System.err.print(message(ant, turnTooLongMessage(elapsedTimeMillis)));
		}
	}

	private static String message(CommonAnt ant, String info) {
		String message = ant.toString() + " " + info;
		if (message.charAt(message.length() - 1) != '\n') message = message +"\n";
		return message;
	}

	// Trace progress of 'ant'
	public static void trace(CommonAnt ant, String info) {
		if (omitLogs) return;
		String fileName = getFileName("trace", ant.id);
		append(fileName, message(ant, info));
	}

	// Output some unusual information on 'ant'
	public static void inform(CommonAnt ant, String info) {
		if (omitLogs) return;
		String fileName = "info";
		append(fileName, message(ant, info));
	}

	// Output warning message for 'ant', need to check what happened here
	public static void warn(CommonAnt ant, String info) {
		if (omitLogs) return;
		String message = message(ant, info);
		append("info", "warning: " + message);
		append(getFileName("trace", ant.id), message);
		System.out.print(message);
	}

	// Output error message for 'ant', need to check what happened here
	public static void error(CommonAnt ant, String info) {
		if (omitLogs) return;
		String message = message(ant, info);
		append("info", "error: " + message);
		append(getFileName("trace", ant.id), message);
		System.err.print(message);
	}

	// Dump current board representation for ant, under log file board_ID_name.txt
	public static void dumpBoard(CommonAnt ant) {
		if (omitLogs) return;
		String representation = String.format("%s\n%s", ant.toString(), ant.board.representation(true));
		writeAndClose(getFileName("board", ant.id), representation);
	}

	// Standard file name
	private static String getFileName(String prefix, int id) {
		assert id > 0 && id <= 50;
		return String.format("%s_%02d", prefix, id);
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
