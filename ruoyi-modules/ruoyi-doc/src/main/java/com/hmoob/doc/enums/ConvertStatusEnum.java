package com.hmoob.doc.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 转换状态枚举
 */
@Getter
@AllArgsConstructor
public enum ConvertStatusEnum {

    NONE(0, "未转换"),
    SUCCESS(1, "转换成功"),
    FAILED(2, "转换失败"),
    PROCESSING(3, "转换中");

    private final Integer code;
    private final String desc;

    public static ConvertStatusEnum getByCode(Integer code) {
        for (ConvertStatusEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }
}