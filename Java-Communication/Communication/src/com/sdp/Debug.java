package com.sdp;

public class Debug {
	private static String previousOut = "";

	public static void out(String out, Object param) {
		if (!out.equals(previousOut)) {
			System.out.println(out + " " + param);
			previousOut = out;
		}
	}
}
