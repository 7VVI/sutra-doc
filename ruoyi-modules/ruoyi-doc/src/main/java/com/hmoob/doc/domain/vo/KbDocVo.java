package com.hmoob.doc.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import com.hmoob.doc.domain.KbDoc;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * KB文档视图对象 kb_doc
 *
 * @author hmoob
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = KbDoc.class)
public class KbDocVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 文档ID
     */
    @ExcelProperty(value = "文档ID")
    private Long docId;

    /**
     * 所属目录ID(0表示根目录/未分类)
     */
    @ExcelProperty(value = "所属目录ID")
    private Long folderId;

    /**
     * 关联文件ID
     */
    private Long fileId;

    /**
     * 预览文件ID(PDF转换后)
     */
    private Long previewFileId;

    /**
     * 原始预览文件ID
     */
    private Long originalPreviewFileId;

    /**
     * 全文检索索引ID
     */
    private String indexId;

    /**
     * 组织编码
     */
    private String orgCode;

    /**
     * 部门ID
     */
    private Long depId;

    /**
     * 文档名称
     */
    @ExcelProperty(value = "文档名称")
    private String docName;

    /**
     * 文档标题
     */
    @ExcelProperty(value = "文档标题")
    private String docTitle;

    /**
     * 文档编号(业务唯一标识)
     */
    @ExcelProperty(value = "文档编号")
    private String serialNumber;

    /**
     * 文件类型
     */
    @ExcelProperty(value = "文件类型")
    private String fileType;

    /**
     * 文件大小,单位:字节
     */
    @ExcelProperty(value = "文件大小")
    private Long fileSize;

    /**
     * 文档分类(字典:kb_dict_category)
     */
    @ExcelProperty(value = "文档分类")
    private String category;

    /**
     * 关键词(逗号分隔)
     */
    private String keywords;

    /**
     * 状态: 0-待处理, 1-已发布, 2-已撤回, 3-已归档, 4-处理失败
     */
    @ExcelProperty(value = "状态")
    private Integer status;

    /**
     * 全文检索标志: 0-否, 1-是
     */
    private Integer ftiFlag;

    /**
     * 转换标志: 0-未转换, 1-转换成功, 2-转换失败, 3-转换中
     */
    private Integer convertFlag;

    /**
     * 发布标志: 0-未发布, 1-已发布
     */
    private Integer releaseFlag;

    /**
     * 公开备注标志: 0-不公开, 1-公开
     */
    private Integer publicRemark;

    /**
     * 当前版本号
     */
    @ExcelProperty(value = "当前版本号")
    private Integer currentVersion;

    /**
     * 下载次数
     */
    @ExcelProperty(value = "下载次数")
    private Long downloadCount;

    /**
     * 评论次数
     */
    @ExcelProperty(value = "评论次数")
    private Long commentCount;

    /**
     * 浏览次数
     */
    @ExcelProperty(value = "浏览次数")
    private Long viewCount;

    /**
     * 收藏次数
     */
    @ExcelProperty(value = "收藏次数")
    private Long favouriteCount;

    /**
     * 处理消息(错误信息等)
     */
    private String processMsg;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    @ExcelProperty(value = "创建时间")
    private Date createTime;

}