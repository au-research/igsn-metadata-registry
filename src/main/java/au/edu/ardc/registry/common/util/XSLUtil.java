package au.edu.ardc.registry.common.util;

public class XSLUtil {

	public XSLUtil() {

	}

	public static String toUpperCase(String dirtyText) {
		return dirtyText.toUpperCase();
	}

	public static String toLowerCase(String dirtyText) {
		return dirtyText.toLowerCase();
	}

	/**
	 * https://stackoverflow.com/questions/18898773/java-escape-json-string
	 * @param raw input string that may contains unescaped string that is invalid JSON str
	 * @return an escaped string
	 */
	public static String escapeJsonString(String raw) {
		String escaped = raw;
		escaped = escaped.replace("\\", "\\\\");
		escaped = escaped.replace("\"", "\\\"");
		escaped = escaped.replace("\b", "\\b");
		escaped = escaped.replace("\f", "\\f");
		escaped = escaped.replace("\n", "\\n");
		escaped = escaped.replace("\r", "\\r");
		escaped = escaped.replace("\t", "\\t");
		// TODO: escape other non-printing characters using uXXXX notation
		return escaped;
	}

}
