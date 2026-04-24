package com.hmoob.doc.service;

import com.hmoob.common.mybatis.core.page.PageQuery;
import com.hmoob.common.mybatis.core.page.TableDataInfo;
import com.hmoob.doc.domain.KbDoc;
import com.hmoob.doc.domain.bo.KbDocBo;
import com.hmoob.doc.domain.dto.KbDocUploadDto;
import com.hmoob.doc.domain.vo.KbDocVo;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * KB文档管理 服务层
 *
 * @author hmoob
 */
public interface IKbDocService {

    /**
     * 分页查询文档管理数据
     *
     * @param bo        文档信息
     * @param pageQuery 分页对象
     * @return 文档信息集合
     */
    TableDataInfo<KbDocVo> selectPageDocList(KbDocBo bo, PageQuery pageQuery);

    /**
     * 查询文档管理数据
     *
     * @param bo 文档信息
     * @return 文档信息集合
     */
    List<KbDocVo> selectDocList(KbDocBo bo);

    /**
     * 根据文档ID查询信息
     *
     * @param docId 文档ID
     * @return 文档信息
     */
    KbDocVo selectDocById(Long docId);

    /**
     * 根据文档编号查询信息
     *
     * @param serialNumber 文档编号
     * @return 文档信息
     */
    KbDocVo selectDocBySerialNumber(String serialNumber);

    /**
     * 上传文档
     * 包含文件上传、文档保存、异步解析和ES索引
     *
     * @param file 上传文件
     * @param dto  上传参数
     * @return 文档信息
     */
    KbDocVo uploadDoc(MultipartFile file, KbDocUploadDto dto);

    /**
     * 异步处理文档（解析内容、索引ES）
     *
     * @param docId 文档ID
     */
    void asyncProcessDoc(Long docId);

    /**
     * 解析文档内容并索引到ES
     *
     * @param docId 文档ID
     */
    void parseAndIndexDoc(Long docId);

    /**
     * 发布文档
     *
     * @param docId 文档ID
     * @return 结果
     */
    int publishDoc(Long docId);

    /**
     * 撤回文档
     *
     * @param docId 文档ID
     * @return 结果
     */
    int withdrawDoc(Long docId);

    /**
     * 增加浏览次数
     *
     * @param docId 文档ID
     * @return 结果
     */
    int incrementViewCount(Long docId);

    /**
     * 增加下载次数
     *
     * @param docId 文档ID
     * @return 结果
     */
    int incrementDownloadCount(Long docId);

    /**
     * 校验文档名称是否唯一
     *
     * @param bo 文档信息
     * @return 结果
     */
    boolean checkDocNameUnique(KbDocBo bo);

    /**
     * 新增保存文档信息
     *
     * @param bo 文档信息
     * @return 结果
     */
    int insertDoc(KbDocBo bo);

    /**
     * 修改保存文档信息
     *
     * @param bo 文档信息
     * @return 结果
     */
    int updateDoc(KbDocBo bo);

    /**
     * 删除文档管理信息
     *
     * @param docId 文档ID
     * @return 结果
     */
    int deleteDocById(Long docId);

    /**
     * 检查文件类型是否支持
     *
     * @param fileType 文件扩展名
     * @return 是否支持
     */
    boolean isFileTypeSupported(String fileType);

    /**
     * 更新ES索引中的文档信息
     * 当文档的ES相关字段变化时调用(如发布/撤回、标题/关键词变更等)
     *
     * @param docId 文档ID
     */
    void updateEsIndex(Long docId);

    /**
     * 批量获取文档实体信息（用于ES搜索结果补充DB信息）
     *
     * @param docIds 文档ID列表
     * @return 文档ID到实体的映射
     */
    Map<Long, KbDoc> selectDocEntityMap(List<Long> docIds);

}