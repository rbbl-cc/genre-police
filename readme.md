# genre-police
## about
genre-police is a discord bot, that posts the genres to if a spotify link is posted without any context.
## usage
you need to supply the bot with the appropriate tokes to access both the spotify and the discord api,
so you need to register as a developer on both platforms, if you want to run this bot.

at the moment the tokens can only be passed in as key/value pairs via the program arguments.

keys:
- DiscordToken
- SpotifyClientID
- SpotifyClientSecret


example:
> java -jar genre-police-0.1.0.jar DiscordToken=token SpotifyClientID=id SpotifyClientSecret=secret
### docker/container
### build from source
