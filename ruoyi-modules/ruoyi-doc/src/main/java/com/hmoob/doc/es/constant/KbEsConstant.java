package com.hmoob.doc.es.constant;

/**
 * KB ES索引常量定义
 */
public class KbEsConstant {

    // ========== 文档索引 ==========
    public static final String DOC_INDEX_NAME = "kb_doc";
    public static final String DOC_INDEX_ALIAS = "kb_doc_alias";

    // ========== 搜索日志索引 ==========
    public static final String SEARCH_LOG_INDEX_NAME = "kb_search_log";
    public static final String SEARCH_LOG_INDEX_ALIAS = "kb_search_log_alias";

    // ========== 访问日志索引 ==========
    public static final String VISIT_LOG_INDEX_NAME = "kb_visit_log";
    public static final String VISIT_LOG_INDEX_ALIAS = "kb_visit_log_alias";

    // ========== ES配置 ==========
    public static final int SEARCH_TIMEOUT_SECONDS = 30;
    public static final int SAVE_TIMEOUT_HOURS = 1;
    public static final int MAX_RESULT_WINDOW = 1000;
    public static final int REFRESH_INTERVAL_SECONDS = 10;

    // ========== 搜索权重配置 ==========
    public static final float TITLE_SEARCH_BOOST = 2.0f;
    public static final float CONTENT_SEARCH_BOOST = 1.0f;
    public static final float KEYWORD_SEARCH_BOOST = 3.0f;

    // ========== 分词器 ==========
    public static final String ANALYZER_IK_MAX_WORD = "ik_max_word";
    public static final String ANALYZER_IK_SMART = "ik_smart";
}