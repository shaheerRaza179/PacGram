IF SUSER_ID(N'pacgram') IS NULL
BEGIN
    CREATE LOGIN [pacgram] WITH PASSWORD = N'pacsquare', CHECK_POLICY = OFF;
END;
GO

IF DB_ID(N'pacgram') IS NULL
BEGIN
    CREATE DATABASE pacgram;
END;
GO

USE pacgram;
GO

IF USER_ID(N'pacgram') IS NULL
BEGIN
    CREATE USER [pacgram] FOR LOGIN [pacgram];
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.database_role_members drm
    JOIN sys.database_principals role_principal ON role_principal.principal_id = drm.role_principal_id
    JOIN sys.database_principals member_principal ON member_principal.principal_id = drm.member_principal_id
    WHERE role_principal.name = N'db_datareader' AND member_principal.name = N'pacgram'
)
BEGIN
    ALTER ROLE [db_datareader] ADD MEMBER [pacgram];
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.database_role_members drm
    JOIN sys.database_principals role_principal ON role_principal.principal_id = drm.role_principal_id
    JOIN sys.database_principals member_principal ON member_principal.principal_id = drm.member_principal_id
    WHERE role_principal.name = N'db_datawriter' AND member_principal.name = N'pacgram'
)
BEGIN
    ALTER ROLE [db_datawriter] ADD MEMBER [pacgram];
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.database_role_members drm
    JOIN sys.database_principals role_principal ON role_principal.principal_id = drm.role_principal_id
    JOIN sys.database_principals member_principal ON member_principal.principal_id = drm.member_principal_id
    WHERE role_principal.name = N'db_ddladmin' AND member_principal.name = N'pacgram'
)
BEGIN
    ALTER ROLE [db_ddladmin] ADD MEMBER [pacgram];
END;
GO
