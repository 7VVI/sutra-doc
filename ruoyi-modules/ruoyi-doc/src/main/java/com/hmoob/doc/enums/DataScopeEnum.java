package com.hmoob.doc.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 数据范围枚举
 */
@Getter
@AllArgsConstructor
public enum DataScopeEnum {

    SELF(1, "仅本人"),
    DEPT(2, "本部门"),
    DEPT_AND_CHILDREN(3, "本部门及下级"),
    CUSTOM(4, "自定义范围"),
    ALL(5, "全部");

    private final Integer code;
    private final String desc;

    public static DataScopeEnum getByCode(Integer code) {
        for (DataScopeEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }
}