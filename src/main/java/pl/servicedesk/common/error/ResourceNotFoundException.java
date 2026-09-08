package pl.servicedesk.common.error;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends ApplicationException {

    public ResourceNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "resource-not-found", message);
    }

    public static ResourceNotFoundException of(String resource, Object identifier) {
        return new ResourceNotFoundException("%s %s was not found".formatted(resource, identifier));
    }
}
