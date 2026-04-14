package com.hmoob.doc.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 访问类型枚举
 */
@Getter
@AllArgsConstructor
public enum VisitTypeEnum {

    UPLOAD(1, "上传"),
    PREVIEW(2, "预览"),
    DOWNLOAD(3, "下载"),
    DELETE(4, "删除"),
    EDIT(5, "编辑"),
    SHARE(6, "分享");

    private final Integer code;
    private final String desc;

    public static VisitTypeEnum getByCode(Integer code) {
        for (VisitTypeEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }
}