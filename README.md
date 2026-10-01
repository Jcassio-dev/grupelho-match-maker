# grupelho

Bot de Discord privado, sem fins lucrativos, para organizar partidas personalizadas in-house de League of Legends (3v3, 4v4 e ARAM) entre um grupo fechado de amigos.

O grupelho cadastra os jogadores, gera as salas de jogo usando códigos de torneio da Riot, coleta os resultados das partidas e mantém uma liga de torneios interna (tournament standings) só para ajudar a equilibrar os times. Não é um substituto da ladder oficial, não calcula MMR nem ELO, não envolve aposta e não tem qualquer forma de monetização.

## O que é (enquadramento)

- Uso privado: apenas para o grupo fechado de amigos que participa do servidor de Discord.
- Sem fins lucrativos: nenhuma cobrança, assinatura ou venda de qualquer item.
- Sem aposta: os resultados servem só para estatística e organização dos times.
- Liga de torneios interna: os números gerados são "tournament standings" (classificação de torneio), nunca MMR ou ELO.
- Os dados ficam só dentro do Discord e do banco local do bot (SQLite), sem compartilhamento externo.

## Fluxo do usuário

1. **Cadastro**: cada jogador roda `/registrar riot_id:Nome#TAG` no Discord. O bot resolve o PUUID pela Account-V1 da Riot, confirma a conta pela Summoner-V4 e salva o jogador.
2. **Criação da sala**: alguém roda `/criar modo:3v3` (ou `4v4`/`aram`). O bot gera um código de torneio novo com o preset do modo escolhido e posta no canal.
3. **Jogo**: o grupo entra na sala customizada do cliente do LoL usando o código gerado.
4. **Resultado**: ao fim da partida, alguém roda `/resultado partida:<id> vencedor:<A|B>`. Um jogador do time que perdeu precisa confirmar o resultado antes de valer (fallback manual, usado enquanto o bot não tem production key da Riot).
5. **Ranking**: qualquer um pode rodar `/rank modo:3v3` (ou `4v4`/`aram`) para ver os standings daquele modo.

> Espaço reservado para prints do fluxo acima (cadastro, criação de sala, resultado e ranking).

## Como rodar

### Pré-requisitos

- Java 21 (LTS)
- Maven 3.9+
- Uma aplicação de bot criada no [Discord Developer Portal](https://discord.com/developers/applications), com o token do bot
- Uma chave de desenvolvimento da Riot API (ou production key, quando aprovada)

### Configuração

1. Copie `.env.example` para `.env`:
   ```bash
   cp .env.example .env
   ```
2. Preencha as variáveis no `.env`:
   - `RIOT_API_KEY`: sua chave da Riot API.
   - `RIOT_REGION`: região (padrão `BR`).
   - `USE_STUB`: `true` para usar o Tournament-Stub-V5 (não precisa de production key); `false` para usar o Tournament-V5 real.
   - `CALLBACK_URL`: URL pública HTTPS que recebe o callback da Riot (só funciona com production key; precisa ser um gTLD antigo como `.com`/`.net`/`.org`, `.gg` e `.xyz` não servem).
   - `DISCORD_TOKEN`: token do bot do Discord.
   - `DISCORD_GUILD_ID`: ID do servidor (guild) onde os slash commands serão registrados.
   - `CALLBACK_PORT`: porta do servidor web local (Javalin) que recebe o callback.

### Rodando

```bash
mvn -q -DskipTests package
java -jar target/grupelho.jar
```

## Decisões de implementação

- O ranking (`rating/`) usa um sistema de Elo por time, plugável atrás da interface `RatingSystem`, considerando só vitória/derrota. KDA e outras estatísticas ficam separados, só para exibição.
- Os standings são separados por modo (3v3, 4v4 e ARAM); cada modo tem sua própria classificação.
- Enquanto a production key da Riot não existe, o callback web (`web/`) fica no código mas inativo na prática: o Tournament-Stub-V5 não dispara callbacks, então o fluxo depende do `/resultado` manual com confirmação do time perdedor.
- A camada `store/` usa uma interface `Repository` com implementação SQLite, para permitir trocar por Postgres no futuro sem mexer no resto do bot.
- A troca entre o Tournament-Stub-V5 (teste) e o Tournament-V5 real (produção) é feita só pela flag `USE_STUB`, que troca a base do caminho da API e nada mais no código.

## Status das fases

- [x] Fase 1: scaffold Maven, dependências, configuração e README inicial.
- [ ] Fase 2: camada `riot/` e runner CLI de teste (modo stub).
- [ ] Fase 3: `store/` SQLite e `/registrar` ponta a ponta.
- [ ] Fase 4: bot Discord com `/criar`, `/resultado` e `/rank`, com rating Elo por time.
- [ ] Fase 5: servidor de callback Javalin e integração com Match-V5.

## Legal notice

grupelho isn't endorsed by Riot Games and doesn't reflect the views or opinions of Riot Games or anyone officially involved in producing or managing Riot Games properties. Riot Games and all associated properties are trademarks or registered trademarks of Riot Games, Inc.

## Product Description (for Riot application)

grupelho is a private, non-commercial Discord bot used by a single closed group of friends to organize in-house custom League of Legends matches (3v3, 4v4 and ARAM). Players register their Riot ID through a Discord slash command, which resolves their PUUID through Account-V1 and confirms the account through Summoner-V4. To start a match, a member of the group generates a tournament code through the bot, which calls Tournament-V5 (or Tournament-Stub-V5 during development) with a preset appropriate for the chosen mode, and allowed participants restricted to the registered PUUIDs of the group. The group joins the custom lobby in the League client using that code. After the match, the result is recorded either automatically through the Riot tournament callback and Match-V5 (once a production key is available) or, as a fallback, manually through a Discord command that requires confirmation from a member of the losing team before it counts. The bot keeps an internal tournament standings table per game mode, used only to help balance future teams. There is no betting, no monetization, no public access and no data shared outside the group's own Discord server. The bot operates only in the BR region.
