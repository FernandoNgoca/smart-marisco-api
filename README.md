# smart-marisco-api
Sistema de gestão de loja de vendas de mariscos.

## Configuração obrigatória

A aplicação lê as credenciais do ambiente. Não há senha de base de dados nem chave JWT padrão.
Configure no terminal, IDE ou gestor de segredos do servidor:

| Variável | Utilização |
| --- | --- |
| `DB_URL` | JDBC MySQL; padrão `jdbc:mysql://localhost:3306/sm_bd?serverTimezone=UTC` |
| `DB_USERNAME` | Conta dedicada à aplicação, sem usar `root` |
| `DB_PASSWORD` | Senha da conta de base de dados |
| `JWT_SECRET` | Base64 de pelo menos 32 bytes aleatórios; gerar com `openssl rand -base64 32` |
| `CORS_ALLOWED_ORIGINS` | Origens exatas separadas por vírgula; padrão `http://localhost:5173`; não aceita `*` |
| `BOOTSTRAP_ADMIN_PASSWORD` | Senha inicial de 12–128 caracteres, apenas para substituir a credencial original da migração V2 |
| `API_DOCS_ENABLED` | `false` por padrão; se ativado, Swagger e OpenAPI exigem administrador |

Para desenvolvimento local, a aplicação carrega `.env.local.properties` da pasta de trabalho.
Este ficheiro é ignorado pelo Git e pode conter `JWT_SECRET=<chave Base64>`.
Na IDE, use a raiz deste projeto como pasta de trabalho. Variáveis de ambiente têm prioridade.
O Spring Boot não carrega outros ficheiros `.env` automaticamente. Em produção, configure as variáveis no processo que inicia a aplicação.
Não publique segredos nem reutilize a chave dos testes. As credenciais anteriormente expostas em ficheiros
ou no histórico Git devem ser substituídas no servidor; removê-las do código não as revoga.

## Atualização e primeira execução

1. Faça backup da base e configure as variáveis obrigatórias.
2. A migração Flyway V7 adiciona a versão de sessão dos utilizadores e a tabela de refresh tokens.
   Desativa apenas a conta `admin` que ainda usa o hash original da V2; senhas já alteradas são preservadas.
3. Se a conta ainda usar essa credencial original, configure `BOOTSTRAP_ADMIN_PASSWORD` na primeira
   execução. A aplicação substitui a senha e reativa a conta. Sem essa variável, recusa iniciar com
   a credencial original. Remova a variável depois; uma senha personalizada nunca é reposta por este mecanismo.
4. Reinicie a aplicação. Todos os tokens do formato antigo são recusados; os utilizadores devem entrar novamente.

As migrações V1–V6 não foram editadas para preservar os checksums das instalações existentes.
O Flyway usa a conta da aplicação por padrão. Em produção, forneça também
`SPRING_FLYWAY_USER` e `SPRING_FLYWAY_PASSWORD` para uma conta de migrações com privilégios DDL apenas
na base da aplicação. A conta de execução precisa de SELECT, INSERT, UPDATE e DELETE, sem privilégios
administrativos globais. O provisionamento destas contas é feito no MySQL pelo operador.

## Permissões

| Operação | Perfis |
| --- | --- |
| Criar e listar utilizadores | ADMIN |
| Consultar `/api/**` | ADMIN, MANAGER, USER |
| Registar vendas e clientes | ADMIN, MANAGER, USER |
| Alterar catálogo, stock e clientes; desativar registos | ADMIN, MANAGER |
| Alterar imagem e senha | Apenas o próprio utilizador autenticado |

O cadastro não é público. Um administrador pode atribuir os perfis existentes; a omissão de perfis
atribui `ROLE_USER`. Novas rotas fora das regras explícitas são bloqueadas.

## Sessões e proteção

- Enviar o access token em `Authorization: Bearer ...`. Tem validade de 15 minutos.
- Renovar em `PUT /auth/refresh/{username}` com o **refresh token** no mesmo cabeçalho.
  O refresh tem validade de 3 horas e só pode ser usado uma vez. Guardar o novo par devolvido e
  coordenar renovações concorrentes no cliente. O formato de resposta existente é mantido.
- Alterar a senha exige o access token e a senha anterior. Invalida imediatamente os tokens anteriores
  e exige novo login. Contas bloqueadas, desativadas ou expiradas não podem usar nem renovar tokens.
- JWTs validam assinatura, emissor, destinatário, tipo, validade e versão da sessão.
- Respostas nunca incluem a senha ou o seu hash. Novas senhas usam PBKDF2-HMAC-SHA256 com 600.000
  iterações e salt de 16 bytes; hashes antigos continuam verificáveis até a senha ser alterada.
  Referência: [OWASP Password Storage](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html).
- Login, renovação e alteração de senha partilham um limite de 20 pedidos/minuto por IP, por instância.
  O limite usa o endereço da conexão, não confia em `X-Forwarded-For`, e devolve 429 com `Retry-After`.
  Atrás de proxy, os clientes podem partilhar o mesmo limite. Em produção com várias instâncias,
  aplicar também limites partilhados por IP/conta num gateway configurado para proxies confiáveis.
- CORS não usa cookies nem credenciais automáticas. O frontend deve enviar o cabeçalho Authorization.
  Servir a API por HTTPS no proxy/servidor de produção.

## Testes

```sh
bash mvnw test
```

Os testes usam H2 em memória com o perfil `test`; não alteram a base MySQL. Cobrem autenticação,
permissões, separação e rotação de tokens, revogação por senha, contas desativadas, isolamento de
perfil, ausência de hashes nas respostas, CORS e limitação de pedidos. A migração MySQL deve ser
aplicada num ambiente de homologação antes da publicação.
