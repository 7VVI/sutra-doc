# CLAUDE.md - 项目开发规范

## 项目信息

- **框架**: RuoYi-Vue-Plus 5.6.0 (Spring Boot 3.5.12)
- **包名**: `com.hmoob` (非 org.dromara)
- **Java**: 17
- **构建**: Maven 多模块

## 核心编码规范
 必须使用 spring-boot-dev skill来开发
### 分层架构（严格遵循）

```
Controller → Service → Mapper
```

- Controller 只负责接收请求和返回响应，不包含业务逻辑
- Service 封装所有业务逻辑，是事务边界
- Mapper 仅负责数据访问

### 依赖注入

- 使用 `@RequiredArgsConstructor` + `private final` 构造器注入
- **禁止** `@Autowired` 字段注入
- **Controller 禁止注入 Mapper**，只能注入 Service
- **Service 禁止跨模块注入 Mapper**，通过 Service 调用

### 事务管理

- **必须使用编程式事务** `TransactionTemplate`
- **禁止使用 `@Transactional` 注解**

```java
// 正确
return transactionTemplate.execute(status -> {
    // 业务逻辑
});

// 错误
@Transactional
public void doSomething() { }
```

### 对象转换

- 使用 `MapstructUtils.convert()` 进行 BO/Entity/VO 转换
- 禁止大量手动 setter/getter 赋值

### 工具类

- Hutool: `ObjectUtil`, `CollUtil`, `StrUtil`
- Apache Commons Lang: `StringUtils`
- 不混用多个工具库

### 查询参数

- 分页查询使用专用 `XxxQuery extends PageQuery`
- **禁止** Controller 中使用 `Bo + PageQuery`

### 枚举

- 布尔类型统一使用 `YesOrNoEnum`
- 状态枚举提供 `of()` 方法和静态 Map 缓存
- **禁止魔法值**（硬编码字符串/数字）

### 性能

- **禁止在循环中查询数据库**（N+1 问题）
- 先收集 ID，批量查询，再填充数据

### 注释

- 所有公共方法必须有 JavaDoc 注释（中文）
- 包含参数说明和返回值说明

## 数据库

- 数据库: `sutra-doc` (MySQL, 43.138.216.65:40001)
- 缓存版本前缀: `v2:` (CacheNames)
- 全局Redis键前缀: `global_v2:` (GlobalConstants)

## KB模块

- ES索引名: `kb_doc`
- 文件存储: 支持 OSS 和本地存储，通过 `kb.upload.mode` 配置
- 上传路径: `{localPath}/{yyyy-MM-dd}/{uuid}.{ext}`
- 文件访问: `GET /kb/file/content/{fileId}`
