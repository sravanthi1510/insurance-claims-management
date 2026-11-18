CREATE DATABASE IF NOT EXISTS customer_db;
CREATE DATABASE IF NOT EXISTS agent_db;

GRANT ALL PRIVILEGES ON customer_db.* TO 'claims_app'@'%';
GRANT ALL PRIVILEGES ON agent_db.* TO 'claims_app'@'%';
