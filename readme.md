# genre-police

## about

genre-police is a discord bot, that posts the genres to spotify links that are posted without any context.

## usage

you need to supply the bot with the appropriate tokes to access both the spotify and the discord api, so you need to
register as a developer on both platforms, if you want to run this bot.

at the moment the tokens can only be passed in as key/value pairs via the program arguments.

keys:

- DISCORD_TOKEN
- SPOTIFY_CLIENT_ID
- SPOTIFY_CLIENT_SECRET
- JDBC_URL
- DB_USER
- DB_PASSWORD

example:

```shell
java -jar genre-police-1.3.0.jar \
  DISCORD_TOKEN=token \
  SPOTIFY_CLIENT_ID=id \
  SPOTIFY_CLIENT_SECRET=secret \
  JDBC_URL=url \ 
  DB_USER=userName \
  DB_PASSWORD=password
``` 

### docker/container

example:
```shell
docker run -d --name gp_1.3.0 registry.gitlab.com/rbbl/genre-police:latest \
  DISCORD_TOKEN=token \
  SPOTIFY_CLIENT_ID=id \
  SPOTIFY_CLIENT_SECRET=secret \
  JDBC_URL=url \ 
  DB_USER=userName \
  DB_PASSWORD=password
```

### build from source

requirements :

- jdk11

example:

```shell
./gradlew build
java -jar target/genre-police-1.3.0.jar \
  DISCORD_TOKEN=token \
  SPOTIFY_CLIENT_ID=id \
  SPOTIFY_CLIENT_SECRET=secret \
  JDBC_URL=url \ 
  DB_USER=userName \
  DB_PASSWORD=password
```