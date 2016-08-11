package selleck.test;

import java.io.IOException;

public class Test2 {

	public static void main(String[] args) throws IOException {
		java.lang.reflect.Method[] methods = Test2.class.getMethods();
		for (final java.lang.reflect.Method m : methods) {
			if (m.getName().startsWith("hahaha")) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							m.invoke(Test2.class.newInstance());
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}).run();
			}
		}
	}

	public void hahaha1() {
		for (int i = 0; i < 1000; i++) {
			System.out.println(1);
		}
	}

	public void hahaha2() {
		for (int i = 0; i < 1000; i++) {
			System.out.println(2);
		}
	}
}
  