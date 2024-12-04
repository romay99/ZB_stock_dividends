package com.dayone.exception.impl;

import com.dayone.exception.AbstractException;

public class UserNotFoundException extends AbstractException {
    @Override
    public int getStatusCode() {
        return 401;
    }

    @Override
    public String getMessage() {
        return "존재하지 않는 사용자입니다.";
    }
}
