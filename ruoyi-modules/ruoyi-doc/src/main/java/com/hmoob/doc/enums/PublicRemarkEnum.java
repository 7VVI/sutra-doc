package com.hmoob.doc.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 公开备注标志枚举
 */
@Getter
@AllArgsConstructor
public enum PublicRemarkEnum {

    PRIVATE(0, "不公开"),
    PUBLIC(1, "公开");

    private final Integer code;
    private final String desc;

    public static PublicRemarkEnum getByCode(Integer code) {
        for (PublicRemarkEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }
}
