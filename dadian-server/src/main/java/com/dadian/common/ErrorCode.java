package com.dadian.common;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class ErrorCode {
    public static final int RESOURCE_NOT_FOUND = 1001;
    public static final int VALIDATION_FAILED = 1002;
    public static final int AUTH_EXPIRED = 2001;
    public static final int AUTH_INVALID = 2002;
    public static final int FORBIDDEN = 2003;
    public static final int ACCOUNT_DEACTIVATED = 2004;
    public static final int PHONE_ALREADY_REGISTERED = 2005;
    public static final int BUSINESS_RULE_VIOLATION = 3001;
    public static final int UNLOCK_REQUIRED = 3002;
    public static final int AI_RATE_LIMITED = 4001;
    public static final int SMS_RATE_LIMITED = 4002;
}
