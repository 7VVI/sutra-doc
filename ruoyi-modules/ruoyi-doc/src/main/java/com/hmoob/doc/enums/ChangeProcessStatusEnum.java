package com.hmoob.doc.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 变化处理状态枚举
 *
 * @author hmoob
 */
@Getter
@AllArgsConstructor
public enum ChangeProcessStatusEnum {

    PENDING(0, "待处理"),
    SUCCESS(1, "处理成功"),
    FAILED(2, "处理失败"),
    IGNORED(3, "忽略");

    private final Integer code;
    private final String desc;

    public static ChangeProcessStatusEnum getByCode(Integer code) {
        for (ChangeProcessStatusEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }
}
