package com.hmoob.doc.service;

import com.hmoob.common.mybatis.core.page.PageQuery;
import com.hmoob.common.mybatis.core.page.TableDataInfo;
import com.hmoob.doc.domain.bo.KbDocBo;
import com.hmoob.doc.domain.vo.KbDocVo;

import java.util.List;

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

}