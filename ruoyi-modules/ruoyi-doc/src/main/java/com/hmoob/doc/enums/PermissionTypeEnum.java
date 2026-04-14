package com.hmoob.doc.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 权限类型枚举
 */
@Getter
@AllArgsConstructor
public enum PermissionTypeEnum {

    VIEW("view", "查看"),
    DOWNLOAD("download", "下载"),
    EDIT("edit", "编辑"),
    DELETE("delete", "删除"),
    UPLOAD("upload", "上传"),
    MANAGE("manage", "管理");

    private final String code;
    private final String desc;

    public static PermissionTypeEnum getByCode(String code) {
        for (PermissionTypeEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }
}