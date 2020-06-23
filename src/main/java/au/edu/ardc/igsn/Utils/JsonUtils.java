package au.edu.ardc.igsn.Utils;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

public class JsonUtils {
	
	
	public static JSONObject convertToJSON(Exception e, String context) throws Exception {
	    JSONObject responseBody = new JSONObject();
	    JSONObject errorTag = new JSONObject();
	    responseBody.put("error", errorTag);

	    errorTag.put("code", 400);
	    errorTag.put("target", context);

	    JSONArray detailList = new JSONArray();
	    errorTag.put("details", detailList);

	    String lastMessage = "";
	    Throwable runner = e;
	    while (runner!=null) {
	        String className =  runner.getClass().getName();
	        String msg =  runner.toString();

	        runner = runner.getCause();

	        JSONObject detailObj = new JSONObject();
	        detailObj.put("message",msg);
	        int dotPos = className.lastIndexOf(".");
	        if (dotPos>0) {
	            className = className.substring(dotPos+1);
	        }
	        detailObj.put("code",className);
	        System.out.println("          ERR: "+msg);
	        detailList.put(detailObj);
	    }

	    JSONObject innerError = new JSONObject();
	    errorTag.put("innerError", innerError);

	    JSONArray stackList = new JSONArray();
	    runner = e;
	    while (runner != null) {
	        for (StackTraceElement ste : runner.getStackTrace()) {
	            String line = ste.getFileName() + ":" + ste.getMethodName() + ":" + ste.getLineNumber();
	            stackList.put(line);
	        }
	        stackList.put("----------------");
	        runner = runner.getCause();
	    }
	    errorTag.put("stack", stackList);
	    return responseBody;
	}
	

}
