# initdb

O Compose cria o banco e o usuário `achernar_identity` via variáveis `POSTGRES_*`; o Liquibase aplica o esquema
na subida da API. Para ambientes com papéis separados (migração × aplicação), crie os papéis antes da primeira
subida conforme `deployment-operations.md` e aponte `ACHERNAR_DB_USER` para o papel de aplicação.
