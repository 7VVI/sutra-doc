package com.hmoob.media.service;

import com.hmoob.common.mybatis.core.page.PageQuery;
import com.hmoob.common.mybatis.core.page.TableDataInfo;
import com.hmoob.media.domain.bo.MediaVideoAuthBo;
import com.hmoob.media.domain.vo.MediaVideoAuthVo;
import java.util.List;

/**
 * 视频授权服务接口
 */
public interface IMediaVideoAuthService {
    /** 新增授权 */
    int insertAuth(MediaVideoAuthBo bo);
    /** 删除授权 */
    int deleteAuth(Long id);
    /** 查询视频授权列表(分页) */
    TableDataInfo<MediaVideoAuthVo> selectAuthPage(Long videoId, PageQuery pageQuery);
    /** 获取用户有权限观看的视频ID列表 */
    List<Long> getAuthorizedVideoIds(Long userId, Long deptId);
}
