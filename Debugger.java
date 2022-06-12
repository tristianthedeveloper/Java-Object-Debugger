
import java.util.stream.*;

import java.util.*;
import java.lang.reflect.*;

public class Debugger {

	protected int indent = 0;
	protected int spacesPerIndent = 5;
	protected StringBuilder buffer = new StringBuilder();



	private final List<Object> visited = new ArrayList<>();
	
	// write a class and all its fields.
	protected synchronized void writeObject(Object object) {
		if (visited.contains(object)) return;
		visited.add(object);
		write(object.getClass().getSimpleName());
		writeChar(' ');
		writeChar('{');
		Map<String, Object> map = convertObject(object);
		Iterator<Map.Entry<String, Object>> iterator = map.entrySet().iterator();

		Map.Entry<String, Object> entry;
		if ((entry = iterator.next()) != null) {
			this.indent();
			this.lineFeed();
			this.write(entry.getValue().getClass().getSimpleName());
			this.writeChar(' ');
			this.write(entry.getKey());
			this.writeChar(' ');
			this.writeChar('=');
			this.writeChar(' ');
			this.writeValue(entry.getValue());
		} else {
			this.writeChar('}');
			return;
		}
		if (iterator.hasNext()) {
			iterator.forEachRemaining(e -> {
				this.writeChar(',');
				this.lineFeed();
				this.write(e.getValue().getClass().getSimpleName());
				this.writeChar(' ');
				this.write(e.getKey());
				this.writeChar(' ');
				this.writeChar('=');
				this.writeChar(' ');
				this.writeValue(e.getValue());
			});
		}
		this.dedent();
		this.lineFeed();
		this.writeChar('}');
	}

	protected synchronized void writeValue(Object object) {
		switch (object.getClass().getName()) {

			case "java.lang.String": {
				writeString((String) object);
				return;
			}
			// handle data types (thanks java)
			case "java.lang.Double":
			case "java.lang.Float":
			case "java.lang.Short":
			case "java.lang.Integer": {
				writeNumber(object);
				return;
			}
			case "java.lang.Boolean": {
				write(Boolean.toString((boolean) object));
				return;
			}
			default: { // ignored }
			}

				if (object instanceof AbstractMap map) {

					if (indent > 1) {
						write(object.getClass().getSimpleName());

						writeChar(' ');
					}
					writeChar('{');

					Iterator<Map.Entry<?, ?>> iterator = map.entrySet().iterator();

					Map.Entry<?, ?> entry;
					if ((entry = iterator.next()) != null) {

						this.indent();
						this.lineFeed();
						writeValue(entry.getKey());
						writeChar(' ');
						writeChar('=');
						writeChar(' ');
						writeValue(entry.getValue());

					} else {
						// all done.
						this.writeChar('}');
						return;

					}
					iterator.forEachRemaining(e -> {
						this.writeChar(',');
						this.lineFeed();
						writeValue(e.getKey());
						writeChar(' ');
						writeChar('=');
						writeChar(' ');
						writeValue(e.getValue());
					});

					this.dedent();
					this.lineFeed();
					this.writeChar('}');

					return;

				}

				if (object instanceof Collection || object.getClass().isArray()) { // aww yee (// TODO any better way /
																					// edge cases we missed?)

					if (indent > 1) {
						write(object.getClass().getSimpleName());
						// to prevent writing the same type twice. (in the first indent) and to allow
						// for extra type information inside of multi-dimensional lists and arrays.
						writeChar(' ');
					}
					writeChar('{');

					Iterator<?> iterator;
					if (object instanceof Collection) // if its just a collection, we use the Collection#iterator method
						iterator = ((Collection<?>) object).iterator();
					else // otherwise, we get to do this the hard way.
						iterator = getIterator(object);
					Object entry;
					if ((entry = iterator.next()) != null) {

						this.indent();
						this.lineFeed();
						writeValue(entry);

					} else {
						// all done.
						this.writeChar('}');
						return;

					}
					iterator.forEachRemaining(e -> {
						this.writeChar(',');
						this.lineFeed();
						writeValue(e);
					});

					this.dedent();
					this.lineFeed();
					this.writeChar('}');

					return;
				}
				writeObject(object);
		}
	}

	protected synchronized void dedent() {
		this.indent--;
	}

	protected synchronized void indent() {
		this.indent++;
	}

	protected synchronized void lineFeed() {
		this.buffer.append("\n");
		// indent.
		for (int i = 0; i < this.indent * this.spacesPerIndent; i++) {
			buffer.append(" ");
		}
	}

	protected synchronized void write(String string) {
		buffer.append(string);
	}

	protected synchronized void writeString(String string) {
		writeChar('"');
		write(string); // make sure its wrapped in "" quotes.
		writeChar('"');
	}

	protected synchronized void writeChar(char c) {
		buffer.append(c);
	}

	protected synchronized void writeNumber(Object num) {
		// Object type to encompass all numbers.
		// TODO Number exists but not sure if it works.
		buffer.append(String.valueOf(num));
	}

	protected static synchronized Iterator getIterator(Object array) {

		String className = array.getClass().getName();
		switch (className) {
			case "[I": { // ints!
				return Arrays.stream((int[]) array).boxed().iterator();
			}
			case "[J": { // longs!
				return Arrays.stream((long[]) array).boxed().iterator();
			}
			case "[S": { // shorts!

				List<Short> shorts = new ArrayList<>();
				for (short s : (short[]) array) {
					shorts.add(s);
				}

				return shorts.iterator();
			}
			case "[F": { // floats!
				List<Float> floats = new ArrayList<>();
				// AHHHHHHHHHHHHHHHHHHHHHH
				for (float s : (float[]) array) {
					floats.add(s);
				}

				return floats.iterator();
			}
			case "[D": { // doubles!
				List<Double> doubles = new ArrayList<>();
				for (double s : (double[]) array) {
					doubles.add(s);
				}

				return doubles.iterator();
			}
			case "[Z": { // booleans!
				List<Boolean> booleans = new ArrayList<>();
				for (boolean s : (boolean[]) array) {
					booleans.add(s);
				}

				return booleans.iterator();

			}
			case "java/lang/String": {

				return Arrays.stream((String[]) array).iterator();
			}
			default: {
				return Arrays.stream((Object[]) array).iterator();
			}
		}
	}

	private static synchronized Map<String, Object> convertObject(Object object) {

		LinkedHashMap<String, Object> fieldMap = new LinkedHashMap<>();

		Comparator<Field> comparator = Comparator.comparing(o -> o.getType()
				.getSimpleName());
		for (Field f : Arrays.stream(object.getClass()
				.getDeclaredFields())
				// .sorted(comparator) // sort by type name
				.collect(Collectors.toList())) {
			try {
				f.setAccessible(true);
				fieldMap.put(f.getName(), f.get(object));

			} catch (Exception e) {
				fieldMap.put(f.getName(), "[UNACCESSABLE].");
				// this can happen if its static or something, i'm lazy so im not going to add
				// handling.

			}
		}

		return fieldMap;

	}

	/*
	 * wrapped in <> too
	 * // FIXME (broken for List<List<String>> )
	 * private static String genericTypeOrBlank(Field f) {
	 * try {
	 * ParameterizedType type = (ParameterizedType) f.getGenericType();
	 * System.out.println(type);
	 * System.out.println(Arrays.toString(type.getActualTypeArguments()));
	 * Class<?> clazz = (Class<?>)type.getActualTypeArguments()[0];
	 * String[] split = clazz.getName().split("\\.");
	 * System.out.println(Arrays.toString(split));
	 * return String.format("<%s>",
	 * Arrays.stream(type.getActualTypeArguments()).map(e ->
	 * e.toString()).collect(Collectors.joining(",")));
	 * }catch(Exception e) {
	 * // e.printStackTrace();
	 * return "";
	 * }
	 * }
	 */

	private static synchronized String implode(Object object) {

		Debugger d = new Debugger();
		if (isWrapperOrPrimitive(object)
				|| object.getClass().isArray()
				|| object instanceof Collection || object instanceof AbstractMap) { // we handle these cases by
																					// ourselves. because i am lazy.
			d.write(object.getClass().getSimpleName());
			d.writeChar(' ');
			d.writeValue(object);

		} else
			d.writeObject(object);

		return d.create();
	}

	private synchronized String create() {
		return buffer.toString();
	}

	public static String toString(Object object) { return implode(object); }

	private static final String FORMAT_STR = "[%s:%s] ";

	/**
	 * Blow up the object and then print it, while also letting you know the file
	 * and line number that the method is called with.
	 * i.e
	 * [Main.java:19] Main {
	 * ArrayList array = {
	 * "Test",
	 * "test2",
	 * "test3",
	 * "test4"
	 * },
	 * Integer i = 4,
	 * Integer b = 4,
	 * Integer c = 4
	 * }
	 */
	public static synchronized void debug(Object object) {

		StackTraceElement trace = new Throwable().getStackTrace()[1];

		System.out.printf(FORMAT_STR, trace.getFileName(), trace.getLineNumber());

		if (object == null) {
			System.out.println("null");
			return;
		}

		System.out.println(implode(object));

	}

	private static final List<String> primitives = Arrays.asList("Integer", "Double", "Float", "Short", "Boolean",
			"String");

	private static boolean isWrapperOrPrimitive(Object o) {

		Class<?> c;

		if ((c = o.getClass()).isPrimitive())
			return true;

		String simpleName = c.getSimpleName();

		// why
		return primitives.contains(simpleName);

	}
}

