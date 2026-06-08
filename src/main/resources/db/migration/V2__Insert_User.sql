-- =========================================
-- INSERT PERMISSIONS
-- =========================================

INSERT INTO PERMISSION (
    STATUS,
    CREATED_BY,
    UPDATED_BY,
    DESCRIPTION
) VALUES
(1, 'SYSTEM', 'SYSTEM', 'ROLE_ADMIN'),
(1, 'SYSTEM', 'SYSTEM', 'ROLE_USER'),
(1, 'SYSTEM', 'SYSTEM', 'ROLE_MANAGER');


-- =========================================
-- INSERT USER ADMIN
-- PASSWORD = admin123
-- BCrypt:
-- $2a$10$DowJonesIndexExampleHash123456789
-- =========================================

INSERT INTO USERS (
    STATUS,
    CREATED_BY,
    UPDATED_BY,
    USER_NAME,
    FULL_NAME,
    PASSWORD,
    ACCOUNT_NON_EXPIRED,
    ACCOUNT_NON_LOCKED,
    CREDENTIALS_NON_EXPIRED,
    ENABLED
) VALUES (
    1,
    'SYSTEM',
    'SYSTEM',
    'admin',
    'Administrador do Sistema',
    '{pbkdf2}d1655d5ac31b92342b58152d794f19adf7e9965a3b9a270ee90d7e2c009302d4b41c3b3ceeee88cb',
    b'1',
    b'1',
    b'1',
    b'1'
);


-- =========================================
-- RELACIONAR USER COM PERMISSÕES
-- =========================================

INSERT INTO USER_PERMISSION (
    ID_USER,
    STATUS,
    CREATED_BY,
    UPDATED_BY,
    ID_PERMISSION
)
SELECT
    u.ID,
    1,
    'SYSTEM',
    'SYSTEM',
    p.ID
FROM USERS u
JOIN PERMISSION p
WHERE u.USER_NAME = 'admin'
AND p.DESCRIPTION IN ('ROLE_ADMIN', 'ROLE_USER');