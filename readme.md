# genre-police

## about

genre-police is a discord bot, that posts the genres to if a spotify link is posted without any context.

## usage

you need to supply the bot with the appropriate tokes to access both the spotify and the discord api, so you need to
register as a developer on both platforms, if you want to run this bot.

at the moment the tokens can only be passed in as key/value pairs via the program arguments.

keys:

- DiscordToken
- SpotifyClientID
- SpotifyClientSecret

example:

```shell
java -jar genre-police-1.0.1.jar DiscordToken=token SpotifyClientId=id SpotifyClientSecret=secret
``` 

### docker/container

example:
```shell
docker run -d --name gp_1.0.1 registry.gitlab.com/rbbl/genre-police:latest DiscordToken=token SpotifyClientId=id SpotifyClientSecret=secret
```

### build from source

requirements :

- jdk11
- maven example:

```shell
mvn install
java -jar target/genre-police-1.0.1.jar DiscordToken=token SpotifyClientId=id SpotifyClientSecret=secret
```