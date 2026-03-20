package com.ecommerce.project.exceptions;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class APIException extends RuntimeException{
    private static final long serialVersionUID=1L;

    public APIException(String message){
        super(message);
    }
}
