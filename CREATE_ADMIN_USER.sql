-- 创建管理员账户的 SQL 脚本
-- 密码: admin (BCrypt 加密)
-- BCrypt 哈希值对应的明文密码是: admin
-- 生成时间: 2025-11-13

-- 查看是否已存在
SELECT * FROM user WHERE user_name = 'admin';

-- 插入新管理员账户
-- 注意: 密码 admin 的 BCrypt 哈希值为
-- $2a$10$slYQmyNdGzin7olVN3p5Be7DjH.TO4DNUf.Bsx1NTWLaIZ9RIK9zm
INSERT INTO user (
  user_name,
  email,
  phone,
  company_name,
  role,
  team,
  isvip,
  company_id,
  password,
  activation_time,
  isvalid,
  isdelete,
  confirm_code
) VALUES (
  'admin',
  'admin@vulsystem.local',
  '13800000000',
  'company',
  'admin',
  'admin',
  1,
  1,
  '$2a$10$slYQmyNdGzin7olVN3p5Be7DjH.TO4DNUf.Bsx1NTWLaIZ9RIK9zm',
  NOW(),
  1,
  0,
  'admin'
);

-- 验证插入成功
SELECT id, user_name, email, phone, role, isvip FROM user WHERE user_name = 'admin';
