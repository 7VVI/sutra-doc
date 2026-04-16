package com.hmoob.doc.service;

import com.hmoob.common.mybatis.core.page.PageQuery;
import com.hmoob.common.mybatis.core.page.TableDataInfo;
import com.hmoob.doc.domain.bo.KbSearchRecordBo;
import com.hmoob.doc.domain.vo.KbSearchRecordVo;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * KB搜索记录 服务层
 *
 * @author hmoob
 */
public interface IKbSearchRecordService {

    /**
     * 分页查询搜索记录数据
     *
     * @param bo        搜索记录信息
     * @param pageQuery 分页对象
     * @return 搜索记录信息集合
     */
    TableDataInfo<KbSearchRecordVo> selectPageSearchRecordList(KbSearchRecordBo bo, PageQuery pageQuery);

    /**
     * 查询搜索记录数据
     *
     * @param bo 搜索记录信息
     * @return 搜索记录信息集合
     */
    List<KbSearchRecordVo> selectSearchRecordList(KbSearchRecordBo bo);

    /**
     * 根据用户ID查询搜索记录列表
     *
     * @param userId 用户ID
     * @return 搜索记录列表
     */
    List<KbSearchRecordVo> selectSearchRecordByUserId(Long userId);

    /**
     * 获取用户搜索历史
     *
     * @param userId 用户ID
     * @param limit  数量限制
     * @return 搜索历史列表
     */
    List<KbSearchRecordVo> getUserSearchHistory(Long userId, Integer limit);

    /**
     * 记录搜索操作
     *
     * @param keywords    搜索关键词
     * @param userId      用户ID
     * @param resultCount 搜索结果数量
     * @return 搜索记录ID
     */
    Long recordSearch(String keywords, Long userId, Integer resultCount);

    /**
     * 记录搜索日志(完整参数)
     *
     * @param keywords      搜索关键词
     * @param resultCount   搜索结果数量
     * @param topicCodes    主题编码
     * @param businessTypes 业务类型
     * @param folderIds     文件夹ID
     * @param req           HTTP请求对象
     */
    void logSearch(String keywords, Long resultCount, String[] topicCodes,
                   String[] businessTypes, Long[] folderIds, HttpServletRequest req);

    /**
     * 获取热门搜索关键词
     *
     * @param limit 数量限制
     * @param days  时间范围(最近N天)
     * @return 门关键词列表
     */
    List<String> getHotKeywords(Integer limit, Integer days);

    /**
     * 清空用户搜索历史
     *
     * @param userId 用户ID
     * @return 结果
     */
    int clearUserSearchHistory(Long userId);

    /**
     * 根据搜索记录ID查询信息
     *
     * @param searchId 搜索记录ID
     * @return 搜索记录信息
     */
    KbSearchRecordVo selectSearchRecordById(Long searchId);

    /**
     * 新增保存搜索记录信息
     *
     * @param bo 搜索记录信息
     * @return 结果
     */
    int insertSearchRecord(KbSearchRecordBo bo);

    /**
     * 删除搜索记录信息
     *
     * @param searchId 搜索记录ID
     * @return 结果
     */
    int deleteSearchRecordById(Long searchId);

}