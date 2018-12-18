package com.wso2telco.dep.usermaskservice.service;

import com.wso2telco.dep.validator.handler.utils.HandlerEncriptionUtils;

public class UserMaskService {

    public String getUserMask(String userId) {
        return HandlerEncriptionUtils.getUserMask(userId);
    }

    public String getUserId(String mask) {
        return HandlerEncriptionUtils.getUserId(mask);
    }
}
