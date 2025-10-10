# AI智能建议功能集成方案

## 概述
为了让大模型能够针对检测到的漏洞提供智能建议，我们需要在现有系统基础上增加AI建议功能。

## 实现方案

### 1. 数据库扩展

#### 1.1 为Vulnerability表添加AI建议字段
```sql
-- 添加AI建议相关字段
ALTER TABLE vulnerability ADD COLUMN ai_recommendation TEXT COMMENT 'AI生成的修复建议';
ALTER TABLE vulnerability ADD COLUMN severity_score DECIMAL(3,1) COMMENT 'AI评估的严重性评分(0-10)';
ALTER TABLE vulnerability ADD COLUMN fix_priority INT COMMENT '修复优先级(1-5，1最高)';
ALTER TABLE vulnerability ADD COLUMN estimated_fix_time VARCHAR(50) COMMENT '预估修复时间';
ALTER TABLE vulnerability ADD COLUMN ai_confidence DECIMAL(3,2) COMMENT 'AI建议置信度(0-1)';
```

#### 1.2 创建AI建议历史表
```sql
CREATE TABLE ai_recommendation_history (
    id INT AUTO_INCREMENT PRIMARY KEY,
    vulnerability_id INT NOT NULL,
    recommendation_text TEXT NOT NULL,
    confidence_score DECIMAL(3,2),
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    model_version VARCHAR(50),
    FOREIGN KEY (vulnerability_id) REFERENCES vulnerability(id)
);
```

### 2. 后端API扩展

#### 2.1 AI建议服务接口
```java
@Service
public class AIRecommendationService {

    @Autowired
    private VulnerabilityMapper vulnerabilityMapper;

    @Autowired
    private RestTemplate restTemplate;

    /**
     * 为漏洞生成AI建议
     */
    public void generateAIRecommendation(Integer vulnerabilityId) {
        Vulnerability vulnerability = vulnerabilityMapper.selectById(vulnerabilityId);
        if (vulnerability == null) return;

        // 调用AI服务生成建议
        AIRecommendationRequest request = new AIRecommendationRequest();
        request.setVulnerabilityName(vulnerability.getName());
        request.setDescription(vulnerability.getDescription());
        request.setLanguage(vulnerability.getLanguage());
        request.setRiskLevel(vulnerability.getRiskLevel());

        // 调用AI API（可以是本地部署的大模型或云端API）
        String aiApiUrl = "http://localhost:8080/ai/generate-recommendation";
        AIRecommendationResponse response = restTemplate.postForObject(
            aiApiUrl, request, AIRecommendationResponse.class);

        if (response != null && response.isSuccess()) {
            // 更新漏洞记录
            vulnerability.setAiRecommendation(response.getRecommendation());
            vulnerability.setSeverityScore(response.getSeverityScore());
            vulnerability.setFixPriority(response.getFixPriority());
            vulnerability.setEstimatedFixTime(response.getEstimatedFixTime());
            vulnerability.setAiConfidence(response.getConfidence());

            vulnerabilityMapper.updateById(vulnerability);
        }
    }

    /**
     * 批量生成AI建议
     */
    public void generateBatchRecommendations(List<Integer> vulnerabilityIds) {
        for (Integer id : vulnerabilityIds) {
            try {
                generateAIRecommendation(id);
                Thread.sleep(1000); // 避免API调用过于频繁
            } catch (Exception e) {
                log.error("生成AI建议失败，漏洞ID: {}", id, e);
            }
        }
    }
}
```

#### 2.2 控制器扩展
```java
@RestController
@RequestMapping("/vulnerability")
public class VulnerabilityController {

    @Autowired
    private AIRecommendationService aiRecommendationService;

    /**
     * 获取带AI建议的漏洞详情
     */
    @GetMapping("/detail/{id}")
    public RespBean getVulnerabilityWithAI(@PathVariable Integer id) {
        try {
            Vulnerability vulnerability = vulnerabilityService.getById(id);

            // 如果没有AI建议，则生成
            if (vulnerability.getAiRecommendation() == null) {
                aiRecommendationService.generateAIRecommendation(id);
                vulnerability = vulnerabilityService.getById(id); // 重新获取
            }

            return RespBean.success(vulnerability);
        } catch (Exception e) {
            return RespBean.error(RespBeanEnum.ERROR, e.getMessage());
        }
    }

    /**
     * 手动触发AI建议生成
     */
    @PostMapping("/generate-ai-recommendation")
    public RespBean generateAIRecommendation(@RequestParam Integer vulnerabilityId) {
        try {
            aiRecommendationService.generateAIRecommendation(vulnerabilityId);
            return RespBean.success("AI建议生成成功");
        } catch (Exception e) {
            return RespBean.error(RespBeanEnum.ERROR, e.getMessage());
        }
    }
}
```

### 3. AI建议提示词模板

#### 3.1 系统提示词
```
你是一个专业的网络安全专家，专门分析软件漏洞并提供修复建议。请根据提供的漏洞信息，给出详细的分析和修复建议。

输出格式要求：
1. 漏洞严重性评分（0-10分）
2. 修复优先级（1-5级，1为最高）
3. 预估修复时间
4. 详细修复建议
5. 预防措施
6. 相关参考资料

请确保建议具体、可操作，适合开发团队实施。
```

#### 3.2 用户提示词模板
```
漏洞信息：
- 漏洞名称：{vulnerability_name}
- 漏洞描述：{description}
- 编程语言：{language}
- 风险等级：{risk_level}
- 发现时间：{discovery_time}

请提供针对性的修复建议。
```

### 4. 示例AI建议数据

更新测试数据中的vulnerability表，添加AI建议：

```sql
-- 更新现有漏洞数据，添加AI建议
UPDATE vulnerability SET
    ai_recommendation = CASE id
        WHEN 1 THEN '建议立即升级Spring Boot到3.2.0以上版本。同时检查所有@RequestMapping端点，确保输入验证和输出编码。实施Web应用防火墙(WAF)规则。定期进行安全代码审查。'
        WHEN 2 THEN '使用参数化查询(PreparedStatement)替换字符串拼接SQL。升级到mysql-connector-java 8.0.33+。启用SQL审计日志监控异常查询。对数据库用户权限进行最小化配置。'
        WHEN 3 THEN '立即停止使用FastJSON，迁移到Jackson 2.15+或Gson 2.10+。如无法立即迁移，设置AutoType白名单，禁用AutoType功能。对所有反序列化入口点进行输入验证。'
        WHEN 4 THEN '紧急升级Log4j到2.17.2+版本。设置系统属性log4j2.formatMsgNoLookups=true。移除JndiLookup类。实施日志注入检测机制。'
        WHEN 5 THEN '升级Jackson-databind到2.15.3+。配置ObjectMapper禁用FAIL_ON_UNKNOWN_PROPERTIES。使用@JsonTypeInfo注解控制多态反序列化。实施输入白名单验证。'
        WHEN 6 THEN '升级Spring Framework到6.0.13+。检查所有SpEL表达式使用场景。实施严格的输入验证。配置Spring Security防止CSRF和XSS攻击。'
        WHEN 7 THEN '完全移除FastJSON依赖，替换为Jackson或Gson。检查所有JSON解析点，确保类型安全。实施API输入验证中间件。定期扫描依赖漏洞。'
        WHEN 8 THEN '升级OpenSSL到3.0.12+版本。检查SSL/TLS配置，禁用弱加密套件。实施证书固定。定期更新CA证书。监控SSL连接异常。'
        WHEN 9 THEN '升级libcurl到8.4.0+。设置适当的超时和重试机制。实施内存使用监控。使用内存安全的HTTP客户端库。定期检查内存泄漏。'
        WHEN 10 THEN '升级Redis到7.2.3+。启用AUTH认证和ACL权限控制。配置防火墙限制访问来源。禁用危险命令如FLUSHALL。启用持久化和备份。'
        WHEN 11 THEN '升级Apache Commons Collections到4.4+。避免反序列化不可信数据。使用安全的序列化框架。实施反序列化白名单。定期审查序列化使用场景。'
        WHEN 12 THEN '升级Nginx到1.24.0+稳定版。检查代理配置，设置适当的缓冲区限制。启用HTTP/2协议。配置请求限速和大小限制。'
        WHEN 13 THEN '升级TensorFlow到2.14.0+。验证模型文件来源和完整性。实施模型加载沙箱。监控内存使用。使用模型签名验证。'
        WHEN 14 THEN '升级PyTorch到2.1.0+。添加张量大小和类型验证。使用安全的数据加载器。实施内存限制和监控。定期检查模型输入边界。'
        WHEN 15 THEN '升级OpenCV到4.8.1+。验证图像文件格式和大小。实施图像处理沙箱。限制处理文件类型。添加内存使用监控和限制。'
    END,
    severity_score = CASE id
        WHEN 1 THEN 8.5
        WHEN 2 THEN 6.0
        WHEN 3 THEN 9.8
        WHEN 4 THEN 9.0
        WHEN 5 THEN 8.0
        WHEN 6 THEN 8.5
        WHEN 7 THEN 9.8
        WHEN 8 THEN 6.5
        WHEN 9 THEN 4.0
        WHEN 10 THEN 8.0
        WHEN 11 THEN 8.0
        WHEN 12 THEN 6.0
        WHEN 13 THEN 6.5
        WHEN 14 THEN 4.5
        WHEN 15 THEN 8.0
    END,
    fix_priority = CASE id
        WHEN 1 THEN 1
        WHEN 2 THEN 3
        WHEN 3 THEN 1
        WHEN 4 THEN 1
        WHEN 5 THEN 2
        WHEN 6 THEN 1
        WHEN 7 THEN 1
        WHEN 8 THEN 3
        WHEN 9 THEN 4
        WHEN 10 THEN 2
        WHEN 11 THEN 2
        WHEN 12 THEN 3
        WHEN 13 THEN 3
        WHEN 14 THEN 4
        WHEN 15 THEN 2
    END,
    estimated_fix_time = CASE id
        WHEN 1 THEN '2-4小时'
        WHEN 2 THEN '4-8小时'
        WHEN 3 THEN '8-16小时'
        WHEN 4 THEN '1-2小时'
        WHEN 5 THEN '2-4小时'
        WHEN 6 THEN '2-4小时'
        WHEN 7 THEN '8-16小时'
        WHEN 8 THEN '1-2小时'
        WHEN 9 THEN '2-4小时'
        WHEN 10 THEN '1-2小时'
        WHEN 11 THEN '4-8小时'
        WHEN 12 THEN '1-2小时'
        WHEN 13 THEN '2-4小时'
        WHEN 14 THEN '2-4小时'
        WHEN 15 THEN '1-2小时'
    END,
    ai_confidence = CASE id
        WHEN 1 THEN 0.95
        WHEN 2 THEN 0.90
        WHEN 3 THEN 0.98
        WHEN 4 THEN 0.97
        WHEN 5 THEN 0.92
        WHEN 6 THEN 0.94
        WHEN 7 THEN 0.98
        WHEN 8 THEN 0.88
        WHEN 9 THEN 0.85
        WHEN 10 THEN 0.93
        WHEN 11 THEN 0.91
        WHEN 12 THEN 0.87
        WHEN 13 THEN 0.89
        WHEN 14 THEN 0.86
        WHEN 15 THEN 0.92
    END
WHERE id BETWEEN 1 AND 15;
```

## 集成步骤

1. **执行数据库扩展SQL**
2. **运行测试数据插入脚本**
3. **实施后端API扩展**
4. **集成大模型API**
5. **前端展示优化**

## 效果展示

执行完成后，系统将能够：
- 自动检测项目中的安全漏洞
- 匹配公司白名单中的组件
- 提供AI生成的专业修复建议
- 按优先级和严重性排序漏洞
- 估算修复时间和资源投入

这样就实现了一个真正可用的漏洞检测和智能建议系统！