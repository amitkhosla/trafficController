package com.samples;

public class RandomGenerator {
	public static Long getRandomLong(Long maxValue) {
		return (long) (Math.random() * maxValue);
	}
	
	public static String getRandomString(int maxValue) {
		int size = getRandomInteger(maxValue);
		StringBuilder sb = new StringBuilder();
		for (int i=0;i<size;i++) {
			int ch = getRandomInteger(25);
			char c = (char) (ch + 'a');
			sb.append(c);
		}
		return sb.toString();
	}

	public static int getRandomInteger(int maxValue) {
		return (int) (Math.random() * maxValue);
	}
	
	public static void main(String[] args) {
		System.out.println(getRandomString(20));
		System.out.println(getRandomLong(23L));
	}
}
