package au.edu.ardc.registry.igsn.event;

import au.edu.ardc.registry.common.entity.Identifier;
import au.edu.ardc.registry.common.entity.Request;

public class RequestExceptionEvent {

    private String message;

    private Request request;

    public RequestExceptionEvent(String message, Request request) {
        this.message = message;
        this.request = request;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Request getRequest() {
        return request;
    }

    public void setRequest(Request request) {
        this.request = request;
    }
}
