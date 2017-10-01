package org.ak;

import java.lang.reflect.Field;

public class Utils {
	public static <T, V> V getValueFromObject(T object, String fieldName) {
		try {
			Class cls = object.getClass();
			Field field = cls.getDeclaredField(fieldName);
			field.setAccessible(true);
			Object output = field.get(object);
			return (V) output;
		} catch (Exception e) {
			return null;
		}
	}

	public static void sleep(long l) {
		try {
			Thread.sleep(5);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
