package com.hmoob.doc.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 全文检索标志枚举
 */
@Getter
@AllArgsConstructor
public enum FtiFlagEnum {

    NO(0, "否"),
    YES(1, "是"),
    FAILED(2, "失败"),
    PROCESSING(3, "处理中");

    private final Integer code;
    private final String desc;

    public static FtiFlagEnum getByCode(Integer code) {
        for (FtiFlagEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }
}
