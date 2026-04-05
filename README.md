## Configuração

1. Crie o banco de dados no MySQL:

CREATE DATABASE cinescore;


2. Configure as credenciais no arquivo src/main/resources/application.properties:

properties
spring.datasource.url=jdbc:mysql://localhost:3306/cinescore
spring.datasource.username=
spring.datasource.password=
**username e password da maquina local**

3. As tabelas serão criadas automaticamente pelo **Flyway** ao iniciar a aplicação.

## Como rodar

# Clone o repositório
git clone https://github.com/seu-usuario/CineScore-BackEnd.git

# Entre na pasta do projeto
cd CineScore-BackEnd/cinescore

# Execute a aplicação
./mvnw spring-boot:run


A API ficará disponível em: http://localhost:8080

## Banco de Dados

O projeto usa **Flyway** para gerenciar as migrações do banco. As tabelas são criadas automaticamente na ordem abaixo:

| Migration | Tabela | Descrição |
|---|---|---|
| V1 | users | Usuários da plataforma |
| V2 | reviews | Avaliações de filmes (nota de 1 a 5) |
| V3 | review_history | Histórico de alterações das reviews |
| V4 | comments | Comentários nas reviews |
| V5 | followers | Sistema de seguidores entre usuários |
| V6 | — | Inserção do usuário administrador padrão |

---

## Endpoints

### Usuários —  http://localhost:8080/api/users

| Método | Rota | Descrição |
|---|---|---|
| GET | /api/users | Lista todos os usuários |
| GET | /api/users/{id} | Busca um usuário por ID |
| POST | /api/users | Cria um novo usuário |
| PUT | /api/users/{id} | Atualiza um usuário |
| DELETE | /api/users/{id} | Remove um usuário |

No campo Header

Key: Content-Type Value:application/json

----

Exemplo:

GET http://localhost:8080/api/users

----

Exemplo:

POST http://localhost:8080/api/users

Body: 
{
  "name": "Bruno",
  "email": "bruno@cinescore.com",
  "password": "123456"
}

Response:

{
    "id": 1,
    "name": "Bruno",
    "email": "bruno@cinescore.com",
    "role": "USER",
    "createdAt": "2026-04-05T18:43:18.180695"
}

----

Exemplo:

PUT http://localhost:8080/api/users/1

Body:

{
  "name": "Bruno Messias",
  "email": "brunomessias@cinescore.com",
  "password": "123456"
}

Reponse:

{
    "id": 5,
    "name": "Bruno Messias",
    "email": "brunomessias@cinescore.com",
    "role": "USER",
    "createdAt": "2026-04-05T18:43:18"
}


### Reviews — /api/reviews

| Método | Rota | Descrição |
|---|---|---|
| GET | /api/reviews/{id} | Busca uma review por ID |
| GET | /api/reviews/movie/{imdbId} | Lista reviews de um filme pelo ID do IMDB |
| GET | /api/reviews/user/{userId} | Lista reviews de um usuário |
| POST | /api/reviews?userId={id} | Cria uma nova review |
| PUT | /api/reviews/{id}?userId={id} | Atualiza uma review existente |
| DELETE | /api/reviews/{id}?userId={id} | Remove uma review |

> **Regras de negócio:**
> - Cada usuário pode ter **apenas uma review por filme**
> - A nota deve ser um valor entre **1 e 5**
> - Toda alteração em uma review é registrada no **histórico**

No campo Header

Key: Content-Type Value:application/json

----

Exemplo:

GET http://localhost:8080/api/reviews/user/1

----

Exemplo:

POST http://localhost:8080/api/reviews?userId=1

body:

{
  "movieImdbId": "tt0111161",
  "rating": 5,
  "reviewText": "Obra-prima absoluta!"
}

response:

{
    "id": 1,
    "userId": 1,
    "userName": "Administrador",
    "movieImdbId": "tt0111161",
    "rating": 5,
    "reviewText": "Obra-prima absoluta!",
    "createdAt": "2026-04-05T18:49:41.955562",
    "updatedAt": "2026-04-05T18:49:41.955562"

}

-----
Exemplo:

PUT http://localhost:8080/api/reviews/1?userId=1

body:

{
  "movieImdbId": "tt0111161",
  "rating": 4,
  "reviewText": "Excelente, mas não perfeito"
}

response:

{
    "id": 1,
    "userId": 1,
    "userName": "Administrador",
    "movieImdbId": "tt0111161",
    "rating": 4,
    "reviewText": "Excelente, mas não perfeito",
    "createdAt": "2026-04-05T18:49:42",
    "updatedAt": "2026-04-05T18:55:55.668123"
}


----


## Estrutura do Projeto

cinescore/
└── src/
    └── main/
        ├── java/com/cinescore/
        │   ├── config/          # Configurações (Security, etc.)
        │   ├── controller/      # Controladores REST
        │   ├── dto/             # Objetos de transferência de dados
        │   ├── model/           # Entidades do banco de dados
        │   ├── repository/      # Interfaces JPA
        │   └── service/         # Regras de negócio
        └── resources/
            ├── application.properties
            └── db/migration/    # Scripts Flyway (V1 a V6)

---
