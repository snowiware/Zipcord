# Zipcord
Zipcord responds to HTTP requests for info about the server.

# Paths
Subpaths to be called. Ex: `http://localhost:21960/world/time/ticks`

`Link` API uses SQLite for players in a database. You can set and get tags. This can be used to connect to external sources, such as Discord bots. This can be disabled in the `config.yml` file.

Arguments are sent in JSON body. `{'argumentName':'value'}`

## World
`/world/weather`: Returns the weather (clear, rainy, or stormy).

`/world/forecast`: Returns both the weather and time of day (ex: rainy night).
### Time
`/world/time/day-or-night`: Returns whether it is day or night in the current world.

`/world/time/ticks`: Returns how many ticks of the day have passed.
## Players
`/players/list`: Returns a list of the usernames of players online.

`/players/count`: Returns how many players are online.
## Link
`/link/find/`: Returns a list of player UUIDs with the matching tag(s). \[Args: tags]

`/link/find-username/`: Returns a list of player usernames with the matching tag(s). \[Args: tags]

`/link/get/`: Returns a list of tags stored under the player's UUID. \[Args: player]

`/link/add/` *POST*: Adds a tag/tags to the specified player. \[Args: player, tags]

`/link/set/` *POST*: Replaces all of the specified player's tags. \[Args: player, tags]

`/link/remove/` *POST*: Removes specified tags from a player. \[Args: player, tags]

`/link/create/` *POST*: Creates a new link that can be used with Zipcord's in-game `link` command. It applies the specified tags when a player sends the command. Permission: `zipcord.link`. The link can be set to auto-expire after a specified amount of time. Self-destruct makes the code remove itself on use. \[Args: tags, code? (auto-generate), time? (infinite), self-destruct? (true) ]

`/link/destroy/` *POST*: Manually removes the link specified (by name). \[Args: code]

# Discord bot example
Discord bot example is provided in Python. Likely not going to be updated.
