-- 查询数据库中的所有用户信息
-- 使用: mysql -u root -p kulin < query_users.sql

SELECT
    id,
    user_name as '用户名',
    email as '邮箱',
    phone as '电话',
    role as '角色',
    company_name as '公司',
    isvip as 'VIP',
    isvalid as '已激活',
    isdelete as '已删除'
FROM user
WHERE isdelete = 0
ORDER BY id DESC;

-- 显示用户总数
SELECT CONCAT('总用户数: ', COUNT(*)) FROM user WHERE isdelete = 0;

-- 显示密码哈希值（用于调试）
-- 注意: 这显示的是 BCrypt 哈希值，不是明文密码
SELECT
    user_name as '用户名',
    email as '邮箱',
    LEFT(password, 20) as '密码哈希前20字符...'
FROM user
WHERE isdelete = 0
ORDER BY id DESC;
