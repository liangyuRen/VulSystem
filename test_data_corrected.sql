-- 漏洞管理系统测试数据插入脚本（修正版）
-- 注意：所有数据归属于现有的 company_name = 'test' 的公司
-- 确保表间关联关系正确，JSON格式字段符合系统要求

-- ==========================================
-- 重要说明：JSON字段格式
-- ==========================================
-- company.white_list: [{"name":"组件名","pojectid":"项目ID","language":"语言"}]
-- company.projectid: ["项目ID1","项目ID2"]
-- 注意：代码中使用的是 "pojectid"（拼写错误但系统在用）

-- ==========================================
-- 1. 更新现有test公司的配置
-- ==========================================

-- 假设test公司的ID为1，更新其配置
UPDATE company SET
    white_list = '[
        {"name":"spring-boot-starter-web","pojectid":"1","language":"java"},
        {"name":"mysql-connector-java","pojectid":"1","language":"java"},
        {"name":"fastjson","pojectid":"1","language":"java"},
        {"name":"log4j-core","pojectid":"2","language":"java"},
        {"name":"jackson-databind","pojectid":"2","language":"java"},
        {"name":"org.springframework","pojectid":"3","language":"java"},
        {"name":"com.alibaba.fastjson","pojectid":"3","language":"java"},
        {"name":"openssl","pojectid":"4","language":"c"},
        {"name":"curl","pojectid":"4","language":"c"},
        {"name":"redis","pojectid":"5","language":"java"},
        {"name":"nginx","pojectid":"6","language":"c"},
        {"name":"apache-commons","pojectid":"5","language":"java"},
        {"name":"tensorflow","pojectid":"7","language":"java"},
        {"name":"pytorch","pojectid":"7","language":"java"},
        {"name":"opencv","pojectid":"8","language":"c"}
    ]',
    projectid = '["1","2","3","4","5","6","7","8"]',
    detect_strategy = 'comprehensive',
    similarity_threshold = 0.8,
    max_detect_num = 15
WHERE name = 'test';

-- ==========================================
-- 2. 清空现有测试数据（可选）
-- ==========================================
-- 如果需要清空现有数据，取消下面的注释
-- DELETE FROM project_vulnerability WHERE id > 0;
-- DELETE FROM vulnerability_report_vulnerability WHERE id > 0;
-- DELETE FROM vulnerability WHERE id > 0;
-- DELETE FROM vulnerability_report WHERE id > 0;
-- DELETE FROM project WHERE id > 0 AND id != (SELECT id FROM company WHERE name = 'test');

-- ==========================================
-- 3. 插入项目数据
-- ==========================================

-- 注意：这些项目都归属于test公司
-- 项目ID需要与company.white_list中的pojectid对应

INSERT INTO project (id, name, create_time, description, language, file, roadmap_file, risk_threshold, isdelete) VALUES
(1, '电商平台后端系统', '2024-01-15 10:30:00', '基于Spring Boot的大型电商平台后端服务，包含用户管理、订单处理、支付系统等核心功能，使用Spring Boot、MySQL、FastJSON等技术栈', 'java', '/uploads/projects/ecommerce-backend.zip', '/roadmaps/ecommerce-roadmap.md', 3, 0),
(2, '企业日志分析系统', '2024-02-01 14:20:00', '企业级日志收集和分析系统，支持实时监控和异常告警，使用Log4j、Jackson等组件', 'java', '/uploads/projects/log-analysis.zip', '/roadmaps/log-analysis-roadmap.md', 5, 0),
(3, '社交媒体API服务', '2024-02-10 09:15:00', '高并发社交媒体平台API服务，支持用户动态、消息推送等功能，基于Spring Framework和FastJSON', 'java', '/uploads/projects/social-media-api.zip', '/roadmaps/social-media-roadmap.md', 2, 0),
(4, '网络代理服务', '2024-02-20 16:45:00', '基于C语言开发的高性能网络代理服务，使用OpenSSL和cURL库，支持HTTP/HTTPS协议转发', 'c', '/uploads/projects/network-proxy.zip', '/roadmaps/network-proxy-roadmap.md', 4, 0),
(5, '分布式缓存系统', '2024-03-01 11:00:00', '分布式缓存服务系统，提供高速数据存储和检索功能，集成Redis和Apache Commons组件', 'java', '/uploads/projects/cache-service.zip', '/roadmaps/cache-service-roadmap.md', 3, 0),
(6, '高性能Web服务器', '2024-03-10 13:30:00', '轻量级高性能Web服务器，基于Nginx核心，支持静态文件服务和反向代理', 'c', '/uploads/projects/web-server.zip', '/roadmaps/web-server-roadmap.md', 2, 0),
(7, 'AI智能推荐引擎', '2024-03-15 15:20:00', '基于机器学习的智能推荐系统，使用TensorFlow和PyTorch框架，支持个性化内容推荐', 'java', '/uploads/projects/ai-recommendation.zip', '/roadmaps/ai-recommendation-roadmap.md', 1, 0),
(8, '计算机视觉处理库', '2024-03-20 12:10:00', '高效的图像处理和计算机视觉库，基于OpenCV，支持多种图像格式和滤镜算法', 'c', '/uploads/projects/image-processing.zip', '/roadmaps/image-processing-roadmap.md', 3, 0);

-- ==========================================
-- 4. 插入漏洞报告数据
-- ==========================================

INSERT INTO vulnerability_report (cve_id, description, vulnerability_name, disclosure_time, riskLevel, referenceLink, affects_whitelist, isDelete) VALUES
('CVE-2024-1001', 'Spring Boot框架中存在远程代码执行漏洞，攻击者可通过特制请求执行任意代码，影响使用该框架的Web应用程序', 'spring-boot-starter-web;org.springframework', '2024-01-20', 'HIGH', 'https://nvd.nist.gov/vuln/detail/CVE-2024-1001', 1, 0),
('CVE-2024-1002', 'MySQL连接器中的SQL注入漏洞，当应用程序未正确处理用户输入时，可能导致数据库信息泄露或数据被篡改', 'mysql-connector-java', '2024-01-25', 'MEDIUM', 'https://nvd.nist.gov/vuln/detail/CVE-2024-1002', 1, 0),
('CVE-2024-1003', 'FastJSON反序列化漏洞，攻击者可构造恶意JSON数据执行任意代码，这是一个严重的安全漏洞', 'fastjson;com.alibaba.fastjson', '2024-02-01', 'CRITICAL', 'https://nvd.nist.gov/vuln/detail/CVE-2024-1003', 1, 0),
('CVE-2024-1004', 'Log4j核心组件存在LDAP注入漏洞，攻击者可通过日志消息触发远程代码执行', 'log4j-core', '2024-02-05', 'HIGH', 'https://nvd.nist.gov/vuln/detail/CVE-2024-1004', 1, 0),
('CVE-2024-1005', 'Jackson数据绑定库反序列化漏洞，在处理不可信数据时可能导致远程代码执行', 'jackson-databind', '2024-02-10', 'HIGH', 'https://nvd.nist.gov/vuln/detail/CVE-2024-1005', 1, 0),
('CVE-2024-1006', 'OpenSSL中的缓冲区溢出漏洞，在处理特定SSL/TLS握手时可能导致服务崩溃或代码执行', 'openssl', '2024-02-15', 'MEDIUM', 'https://nvd.nist.gov/vuln/detail/CVE-2024-1006', 1, 0),
('CVE-2024-1007', 'cURL库中的内存泄漏问题，长时间运行的应用程序可能因内存耗尽而崩溃', 'curl', '2024-02-20', 'LOW', 'https://nvd.nist.gov/vuln/detail/CVE-2024-1007', 1, 0),
('CVE-2024-1008', 'Redis服务器认证绕过漏洞，攻击者可在未经授权的情况下访问Redis实例', 'redis', '2024-02-25', 'HIGH', 'https://nvd.nist.gov/vuln/detail/CVE-2024-1008', 1, 0),
('CVE-2024-1009', 'Nginx服务器HTTP请求走私漏洞，可能导致缓存污染和安全策略绕过', 'nginx', '2024-03-01', 'MEDIUM', 'https://nvd.nist.gov/vuln/detail/CVE-2024-1009', 1, 0),
('CVE-2024-1010', 'Apache Commons Collections反序列化漏洞，攻击者可利用该漏洞执行任意代码', 'apache-commons', '2024-03-05', 'HIGH', 'https://nvd.nist.gov/vuln/detail/CVE-2024-1010', 1, 0),
('CVE-2024-1011', 'TensorFlow模型加载时的内存损坏漏洞，可能导致应用程序崩溃或数据泄露', 'tensorflow', '2024-03-10', 'MEDIUM', 'https://nvd.nist.gov/vuln/detail/CVE-2024-1011', 1, 0),
('CVE-2024-1012', 'PyTorch张量操作中的整数溢出，在处理大型数据集时可能引发异常行为', 'pytorch', '2024-03-12', 'LOW', 'https://nvd.nist.gov/vuln/detail/CVE-2024-1012', 1, 0),
('CVE-2024-1013', 'OpenCV图像处理函数中的堆溢出漏洞，处理恶意图像文件时可能导致代码执行', 'opencv', '2024-03-15', 'HIGH', 'https://nvd.nist.gov/vuln/detail/CVE-2024-1013', 1, 0);

-- ==========================================
-- 5. 插入具体漏洞数据（包含AI建议）
-- ==========================================

INSERT INTO vulnerability (name, description, language, time, riskLevel, isaccept, isdelete) VALUES
-- 项目1（电商平台）的漏洞
('spring-boot-starter-web', 'Spring Boot Web启动器存在远程代码执行漏洞，建议立即升级到3.2.0以上版本。检查所有@RequestMapping端点，确保输入验证和输出编码。实施Web应用防火墙(WAF)规则，定期进行安全代码审查。', 'java', '2024-01-20', 'HIGH', 0, 0),
('mysql-connector-java', 'MySQL JDBC驱动存在SQL注入风险，必须使用参数化查询(PreparedStatement)替换字符串拼接SQL。升级到mysql-connector-java 8.0.33+，启用SQL审计日志监控异常查询，对数据库用户权限进行最小化配置。', 'java', '2024-01-25', 'MEDIUM', 0, 0),
('fastjson', 'FastJSON存在严重反序列化漏洞，强烈建议立即停止使用，迁移到Jackson 2.15+或Gson 2.10+。如无法立即迁移，设置AutoType白名单，禁用AutoType功能，对所有反序列化入口点进行严格输入验证。', 'java', '2024-02-01', 'CRITICAL', 0, 0),

-- 项目2（日志分析）的漏洞
('log4j-core', 'Log4j核心组件存在LDAP注入漏洞，必须紧急升级到2.17.2+版本。设置系统属性log4j2.formatMsgNoLookups=true，移除JndiLookup类，实施日志注入检测机制，避免记录用户可控的数据。', 'java', '2024-02-05', 'HIGH', 0, 0),
('jackson-databind', 'Jackson数据绑定存在反序列化漏洞，建议升级到2.15.3+版本。配置ObjectMapper禁用FAIL_ON_UNKNOWN_PROPERTIES，使用@JsonTypeInfo注解控制多态反序列化，实施严格的输入白名单验证。', 'java', '2024-02-10', 'HIGH', 0, 0),

-- 项目3（社交媒体API）的漏洞
('org.springframework', 'Spring Framework核心组件存在安全漏洞，建议升级到6.0.13+版本。检查所有SpEL表达式使用场景，实施严格的输入验证，配置Spring Security防止CSRF和XSS攻击，定期更新依赖。', 'java', '2024-02-12', 'HIGH', 0, 0),
('com.alibaba.fastjson', 'FastJSON版本过旧存在多个安全漏洞，建议完全移除依赖，替换为Jackson或Gson。检查所有JSON解析点确保类型安全，实施API输入验证中间件，定期扫描依赖漏洞。', 'java', '2024-02-01', 'CRITICAL', 0, 0),

-- 项目4（网络代理）的漏洞
('openssl', 'OpenSSL库存在缓冲区溢出漏洞，建议升级到3.0.12+版本。检查SSL/TLS配置，禁用弱加密套件，实施证书固定，定期更新CA证书，监控SSL连接异常，确保正确处理证书验证。', 'c', '2024-02-15', 'MEDIUM', 0, 0),
('curl', 'cURL库存在内存泄漏问题，可能导致长时间运行的服务内存耗尽，建议升级到8.4.0+版本。设置适当的超时和重试机制，实施内存使用监控，考虑使用内存安全的HTTP客户端库。', 'c', '2024-02-20', 'LOW', 0, 0),

-- 项目5（缓存系统）的漏洞
('redis', 'Redis服务器存在认证绕过漏洞，建议升级到7.2.3+版本。启用AUTH认证和ACL权限控制，配置防火墙限制访问来源，禁用危险命令如FLUSHALL，启用持久化和备份机制。', 'java', '2024-02-25', 'HIGH', 0, 0),
('apache-commons', 'Apache Commons Collections存在反序列化漏洞，建议升级到4.4+版本。避免反序列化不可信数据，使用安全的序列化框架，实施反序列化白名单，定期审查序列化使用场景。', 'java', '2024-03-05', 'HIGH', 0, 0),

-- 项目6（Web服务器）的漏洞
('nginx', 'Nginx存在HTTP请求走私漏洞，建议升级到1.24.0+稳定版。检查代理配置，设置适当的缓冲区限制，启用HTTP/2协议，配置请求限速和大小限制，监控异常请求模式。', 'c', '2024-03-01', 'MEDIUM', 0, 0),

-- 项目7（AI推荐引擎）的漏洞
('tensorflow', 'TensorFlow模型加载存在内存损坏风险，建议升级到2.14.0+版本。验证模型文件来源和完整性，实施模型加载沙箱，监控内存使用，使用模型签名验证，避免加载不可信模型。', 'java', '2024-03-10', 'MEDIUM', 0, 0),
('pytorch', 'PyTorch张量操作存在整数溢出风险，建议升级到2.1.0+版本。添加张量大小和类型验证，使用安全的数据加载器，实施内存限制和监控，定期检查模型输入边界，验证数据集完整性。', 'java', '2024-03-12', 'LOW', 0, 0),

-- 项目8（图像处理库）的漏洞
('opencv', 'OpenCV图像处理函数存在堆溢出漏洞，建议升级到4.8.1+版本。验证图像文件格式和大小，实施图像处理沙箱，限制处理文件类型，添加内存使用监控和限制，检查图像元数据。', 'c', '2024-03-15', 'HIGH', 0, 0);

-- ==========================================
-- 6. 插入项目-漏洞关联关系
-- ==========================================
-- 这些关联关系必须与company.white_list中的pojectid对应

INSERT INTO project_vulnerability (vulnerability_id, project_id, isDelete) VALUES
-- 项目1（电商平台后端）的漏洞
(1, 1, 0),  -- spring-boot-starter-web
(2, 1, 0),  -- mysql-connector-java
(3, 1, 0),  -- fastjson

-- 项目2（日志分析系统）的漏洞
(4, 2, 0),  -- log4j-core
(5, 2, 0),  -- jackson-databind

-- 项目3（社交媒体API）的漏洞
(6, 3, 0),  -- org.springframework
(7, 3, 0),  -- com.alibaba.fastjson

-- 项目4（网络代理服务）的漏洞
(8, 4, 0),  -- openssl
(9, 4, 0),  -- curl

-- 项目5（缓存服务系统）的漏洞
(10, 5, 0), -- redis
(11, 5, 0), -- apache-commons

-- 项目6（Web服务器）的漏洞
(12, 6, 0), -- nginx

-- 项目7（AI推荐引擎）的漏洞
(13, 7, 0), -- tensorflow
(14, 7, 0), -- pytorch

-- 项目8（图像处理库）的漏洞
(15, 8, 0); -- opencv

-- ==========================================
-- 7. 插入漏洞报告-漏洞关联关系
-- ==========================================

INSERT INTO vulnerability_report_vulnerability (vulnerability_report_id, vulnerability_id, isDelete) VALUES
(1, 1, 0),   -- CVE-2024-1001 -> spring-boot-starter-web
(1, 6, 0),   -- CVE-2024-1001 -> org.springframework (同一CVE影响多个组件)
(2, 2, 0),   -- CVE-2024-1002 -> mysql-connector-java
(3, 3, 0),   -- CVE-2024-1003 -> fastjson
(3, 7, 0),   -- CVE-2024-1003 -> com.alibaba.fastjson (同一CVE)
(4, 4, 0),   -- CVE-2024-1004 -> log4j-core
(5, 5, 0),   -- CVE-2024-1005 -> jackson-databind
(6, 8, 0),   -- CVE-2024-1006 -> openssl
(7, 9, 0),   -- CVE-2024-1007 -> curl
(8, 10, 0),  -- CVE-2024-1008 -> redis
(9, 12, 0),  -- CVE-2024-1009 -> nginx
(10, 11, 0), -- CVE-2024-1010 -> apache-commons
(11, 13, 0), -- CVE-2024-1011 -> tensorflow
(12, 14, 0), -- CVE-2024-1012 -> pytorch
(13, 15, 0); -- CVE-2024-1013 -> opencv

-- ==========================================
-- 8. 更新白名单表数据（与company.white_list保持一致）
-- ==========================================

INSERT INTO white_list (name, file_path, description, language, isdelete) VALUES
('spring-boot-starter-web', '/libs/spring-boot-starter-web-2.6.0.jar', 'Spring Boot Web启动器，用于构建Web应用程序', 'java', 0),
('mysql-connector-java', '/libs/mysql-connector-java-8.0.28.jar', 'MySQL数据库JDBC驱动程序', 'java', 0),
('fastjson', '/libs/fastjson-1.2.76.jar', '阿里巴巴开源的JSON解析库（存在安全风险）', 'java', 0),
('log4j-core', '/libs/log4j-core-2.14.1.jar', 'Apache Log4j日志记录核心组件', 'java', 0),
('jackson-databind', '/libs/jackson-databind-2.12.3.jar', 'Jackson数据绑定库，用于JSON和对象转换', 'java', 0),
('org.springframework', '/libs/spring-framework-5.3.8/', 'Spring框架核心库', 'java', 0),
('com.alibaba.fastjson', '/libs/fastjson-1.2.73.jar', 'FastJSON JSON处理库（旧版本，存在严重漏洞）', 'java', 0),
('openssl', '/usr/lib/libssl.so.1.1', 'OpenSSL加密库', 'c', 0),
('curl', '/usr/lib/libcurl.so.4', 'cURL网络传输库', 'c', 0),
('redis', '/libs/jedis-3.6.0.jar', 'Redis Java客户端库', 'java', 0),
('nginx', '/usr/sbin/nginx', 'Nginx Web服务器', 'c', 0),
('apache-commons', '/libs/commons-collections-3.2.2.jar', 'Apache Commons集合工具库', 'java', 0),
('tensorflow', '/libs/tensorflow-java-0.3.1.jar', 'TensorFlow机器学习库Java版', 'java', 0),
('pytorch', '/libs/pytorch-java-1.9.0.jar', 'PyTorch深度学习库Java绑定', 'java', 0),
('opencv', '/usr/lib/libopencv_core.so.4.5', 'OpenCV计算机视觉库', 'c', 0);

-- ==========================================
-- 9. 数据验证查询
-- ==========================================

-- 验证test公司的配置
SELECT 'Test Company Configuration:' as info;
SELECT name, white_list, projectid, detect_strategy, similarity_threshold, max_detect_num
FROM company WHERE name = 'test';

-- 验证数据插入统计
SELECT 'Data Statistics:' as info;
SELECT 'Projects:', COUNT(*) FROM project;
SELECT 'Vulnerability Reports:', COUNT(*) FROM vulnerability_report;
SELECT 'Vulnerabilities:', COUNT(*) FROM vulnerability;
SELECT 'Project-Vulnerability Relations:', COUNT(*) FROM project_vulnerability;
SELECT 'Report-Vulnerability Relations:', COUNT(*) FROM vulnerability_report_vulnerability;
SELECT 'White List Items:', COUNT(*) FROM white_list;

-- 验证项目漏洞分布
SELECT 'Project Vulnerability Distribution:' as info;
SELECT
    p.id as project_id,
    p.name as project_name,
    p.language,
    COUNT(pv.vulnerability_id) as vulnerability_count,
    GROUP_CONCAT(v.riskLevel) as risk_levels
FROM project p
LEFT JOIN project_vulnerability pv ON p.id = pv.project_id AND pv.isDelete = 0
LEFT JOIN vulnerability v ON pv.vulnerability_id = v.id AND v.isdelete = 0
GROUP BY p.id, p.name, p.language
ORDER BY p.id;

-- 验证风险级别分布
SELECT 'Risk Level Distribution:' as info;
SELECT
    riskLevel,
    COUNT(*) as count
FROM vulnerability
WHERE isdelete = 0
GROUP BY riskLevel
ORDER BY
    CASE riskLevel
        WHEN 'CRITICAL' THEN 1
        WHEN 'HIGH' THEN 2
        WHEN 'MEDIUM' THEN 3
        WHEN 'LOW' THEN 4
    END;