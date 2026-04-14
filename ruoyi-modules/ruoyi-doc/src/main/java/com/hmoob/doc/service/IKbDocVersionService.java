package com.hmoob.doc.service;

import com.hmoob.doc.domain.bo.KbDocVersionBo;
import com.hmoob.doc.domain.vo.KbDocVersionVo;

import java.util.List;

/**
 * KB文档版本 服务层
 *
 * @author hmoob
 */
public interface IKbDocVersionService {

    /**
     * 根据文档ID查询版本列表
     *
     * @param docId 文档ID
     * @return 版本列表(按版本号降序)
     */
    List<KbDocVersionVo> selectVersionList(Long docId);

    /**
     * 根据文档ID和版本号查询版本信息
     *
     * @param docId    文档ID
     * @param versionNo 版本号
     * @return 版本信息
     */
    KbDocVersionVo selectVersionByVersionNo(Long docId, Integer versionNo);

    /**
     * 根据版本ID查询版本信息
     *
     * @param versionId 版本ID
     * @return 版本信息
     */
    KbDocVersionVo selectVersionById(Long versionId);

    /**
     * 创建新版本
     *
     * @param bo 版本业务对象
     * @return 影响行数
     */
    int createNewVersion(KbDocVersionBo bo);

    /**
     * 回滚到指定版本
     * 将文档恢复到指定版本状态
     *
     * @param docId    文档ID
     * @param versionNo 目标版本号
     * @return 影响行数
     */
    int rollbackToVersion(Long docId, Integer versionNo);

    /**
     * 删除版本
     *
     * @param versionId 版本ID
     * @return 影响行数
     */
    int deleteVersion(Long versionId);

    /**
     * 获取文档的最新版本号
     *
     * @param docId 文档ID
     * @return 最新版本号, 无版本时返回0
     */
    Integer getLatestVersionNo(Long docId);

}