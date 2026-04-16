package com.hmoob.common.core.constant;

/**
 * 缓存组名称常量
 * <p>
 * key 格式为 cacheNames#ttl#maxIdleTime#maxSize#local
 * <p>
 * ttl 过期时间 如果设置为0则不过期 默认为0
 * maxIdleTime 最大空闲时间 根据LRU算法清理空闲数据 如果设置为0则不检测 默认为0
 * maxSize 组最大长度 根据LRU算法清理溢出数据 如果设置为0则无限长 默认为0
 * local 默认开启本地缓存为1 关闭本地缓存为0
 * <p>
 * 例子: test#60s、test#0#60s、test#0#1m#1000、test#1h#0#500、test#1h#0#500#0
 *
 * @author Lion Li
 */
public interface CacheNames {

    /**
     * 缓存版本号 - 用于清除旧缓存(包名变更后旧缓存反序列化失败)
     */
    String CACHE_VERSION = "v2:";

    /**
     * 演示案例
     */
    String DEMO_CACHE = CACHE_VERSION + "demo:cache#60s#10m#20";

    /**
     * 系统配置
     */
    String SYS_CONFIG = CACHE_VERSION + "sys_config";

    /**
     * 数据字典
     */
    String SYS_DICT = CACHE_VERSION + "sys_dict";

    /**
     * 数据字典类型
     */
    String SYS_DICT_TYPE = CACHE_VERSION + "sys_dict_type";

    /**
     * 租户
     */
    String SYS_TENANT = CACHE_VERSION + GlobalConstants.GLOBAL_REDIS_KEY + "sys_tenant#30d";

    /**
     * 客户端
     */
    String SYS_CLIENT = CACHE_VERSION + GlobalConstants.GLOBAL_REDIS_KEY + "sys_client#30d";

    /**
     * 用户账户
     */
    String SYS_USER_NAME = CACHE_VERSION + "sys_user_name#30d";

    /**
     * 用户昵称
     */
    String SYS_NICKNAME = CACHE_VERSION + "sys_nickname#30d";

    /**
     * 部门
     */
    String SYS_DEPT = CACHE_VERSION + "sys_dept#30d";

    /**
     * OSS内容
     */
    String SYS_OSS = CACHE_VERSION + "sys_oss#30d";

    /**
     * 角色自定义权限
     */
    String SYS_ROLE_CUSTOM = CACHE_VERSION + "sys_role_custom#30d";

    /**
     * 部门及以下权限
     */
    String SYS_DEPT_AND_CHILD = CACHE_VERSION + "sys_dept_and_child#30d";

    /**
     * OSS配置
     */
    String SYS_OSS_CONFIG = CACHE_VERSION + GlobalConstants.GLOBAL_REDIS_KEY + "sys_oss_config";

    /**
     * 在线用户
     */
    String ONLINE_TOKEN = CACHE_VERSION + "online_tokens";

}