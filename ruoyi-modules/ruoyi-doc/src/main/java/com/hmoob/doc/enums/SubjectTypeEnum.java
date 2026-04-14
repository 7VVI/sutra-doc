package com.hmoob.doc.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 授权对象类型枚举
 */
@Getter
@AllArgsConstructor
public enum SubjectTypeEnum {

    USER(1, "用户"),
    ROLE(2, "角色");

    private final Integer code;
    private final String desc;

    public static SubjectTypeEnum getByCode(Integer code) {
        for (SubjectTypeEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }
}