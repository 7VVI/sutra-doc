package com.hmoob.doc.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 文档状态枚举
 */
@Getter
@AllArgsConstructor
public enum DocStatusEnum {

    PENDING(0, "待处理"),
    PUBLISHED(1, "已发布"),
    WITHDRAWN(2, "已撤回"),
    ARCHIVED(3, "已归档"),
    FAILED(4, "处理失败");

    private final Integer code;
    private final String desc;

    public static DocStatusEnum getByCode(Integer code) {
        for (DocStatusEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }
}