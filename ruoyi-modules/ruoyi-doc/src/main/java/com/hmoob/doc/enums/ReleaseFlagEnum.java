package com.hmoob.doc.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 发布标志枚举
 */
@Getter
@AllArgsConstructor
public enum ReleaseFlagEnum {

    UNPUBLISHED(0, "未发布"),
    PUBLISHED(1, "已发布");

    private final Integer code;
    private final String desc;

    public static ReleaseFlagEnum getByCode(Integer code) {
        for (ReleaseFlagEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }
}
