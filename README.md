<h1>HeXO Renderer</h1>
HeXO Renderer is a small Discord bot written in Kotlin for rendering <a href="https://hexo.did.science">HeXO</a> notation within Discord.

## Invite
Add HeXO Renderer to your server or user account: [Invite HeXO Renderer](https://discord.com/oauth2/authorize?client_id=1496214901713014894).

## Table of Contents
<!-- TOC -->
  * [Invite](#invite)
  * [Table of Contents](#table-of-contents)
  * [Notation](#notation)
    * [Rectilinear Notation](#rectilinear-notation)
      * [Basics](#basics)
      * [Highlighting](#highlighting)
      * [Custom Labels](#custom-labels)
    * [BKE Notation](#bke-notation)
    * [Combined](#combined)
    * [HTTTX Notation](#htttx-notation)
    * [Other](#other)
  * [Features](#features)
    * [Command `hexo`](#command-hexo)
    * [Command `render`](#command-render)
    * [Message command](#message-command)
    * [Command `game`](#command-game)
  * [Contributing](#contributing)
  * [Build](#build)
    * [Backend](#backend)
    * [Frontend](#frontend)
    * [Docker Compose](#docker-compose)
<!-- TOC -->

## Notation
### Rectilinear Notation
#### Basics
Rectilinear notation is a notation for encoding board states used by the community to quickly write down formations in text messages. 
However, it can become hard to reason about for more complex states. To solve this issue, this bot provides a way to render this notation as an image directly from within Discord.

The general syntax has the following characters:

| Character | Meaning                                               |
|-----------|-------------------------------------------------------|
| x         | Player 1 (Red / Yellow)                               |
| o         | Player 2 (Blue)                                       |
| .         | Empty cell                                            |
| -         | Two empty cells (equivalent to `..`)                  |
| /         | New row. A newline character can also be used instead |

It is also possible to use numbers to indicate the number of empty cells, so the following are equivalent: `x...x`, `x-.x`, `x3x`.

The following image is produced by the notation `x-x/o.o//x`:

![example 1](assets/example_1.png)

Or for a more complex example:
```
. . x
 . o o
  . x x x o
   x x . o .
```
![example 2](assets/example_2.png)

#### Highlighting
Cells can also be highlighted. To do so, you can put the color (player symbol, or `!` for a neutral highlight) of the highlight in parentheses after the cell to highlight.
`.(!)` Would be an empty neutral highlight, `o(x)` a blue cell with a yellow highlight.

> [!NOTE]
> There is also a shorter syntax for neutral cell highlights: To highlight a cell notated by `x`, `o` or `.` you can use `X`, `O` or `!` to highlight that cell respectively.
> For example `X` is equivalent to `x(!)`.

Additionally, winning rows (6 or more in a row) are highlighted automatically in white.

`....(!)/...x/oxxxxxx/.oox/ooox/.o.o`

![example highlight](assets/example_highlight.png)

You can also highlight lines. To do so, you have to prefix a direction and length of the line before the color in the parentheses.

The direction of the line is indicated by one of the following symbols: `>`, `q`, `p`, `<`, `b`, `d` representing one of the right, bottom right, bottom left, left, top left or top right.
The length can be specified directly after. If no length is specified, it will default to `6`.

`.(>4)xx/.o(q3o)/(>4o)oo`

![example highlight line](assets/example_highlight_line.png)

#### Custom Labels
Cells can also be labeled. The label will be rendered as text inside the labeled cell.

Labels are defined for the previous cell in square brackets.

`.o.[a].[b].[c].[d]/oxxxx.[e]/.[f].[g].[h]x.[i]/...[j].[k]`

![example label](assets/example_label.png)

### BKE Notation
The bot can also render a variation of BKE notation. This is especially useful if you want turn numbers to be displayed on the rendered tiles.

BKE notation divides the board into rings around an origin. Ring `A` is one cell away from the origin, `B` is two cells away, and so on. 
After `Z`, the labels continue with `AA`, `AB`, etc. A cell is addressed using its ring and a zero-based offset, such as `A0` or `B3`.
Each turn starts with `x` or `o`, followed by one or more moves. Consecutive turns must alternate players.

The zero offset line can be indicated by `>`, `q`, `p`, `<`, `b`, `d`, representing right, bottom right, bottom left, left, top left or top right. 
A direction prefix must be followed by `CW` or `CCW`, for clockwise or counterclockwise respectively.

There are two ways to specify the starting position:
- Without a direction and chirality prefix, `d CW` is used implicitly and an opening stone is added at the origin. Its player is the opposite of the first listed player. For example, `o A0 A1` includes an initial X stone. `0` represents just an initial X stone, with no later moves.
- With a prefix such as `p CCW`, no opening stone is added. Only the listed moves are placed. This is useful for partial turn lists and for moves on an existing board.

For example, `o A3 A2 x A1 B3` renders as follows:

![example bke](assets/example_bke.png)

To avoid long offset values, you can optionally use sector addressing.
For this, the board is split into 6 sectors numbered `0` through `5`. Using `sector.offset` you can specify a sector-relative offset, which must be smaller than the ring number (`0` for ring `A`, `0` or `1` for ring `B`, etc.).
For example, `o A3 A2 x A1 B1.1` is equivalent to the example above.

### Combined

It is also possible to combine rectilinear notation with BKE notation. This is useful if you want to encode an initial state and the moves made from that point on.
Write `<rectilinear> || <bke>`, with each part following the corresponding rules stated above. Mark exactly one cell in the initial state with `[*]` to select the BKE origin. This may be an occupied cell, such as `x[*]`, or an empty cell, such as `.[*]`. The origin marker is removed from the label in the combined position.

Include both a direction and chirality in the BKE part so only the listed moves are added. Without that prefix, BKE adds an opening stone at the marked origin; if that cell is already occupied, the notation is rejected as an overlap. Listed moves cannot overlap stones in the initial state or earlier turns either.

The following describe the same position using different origins, zero offset lines, and chiralities:
- `.x[*]/xx || b CW o A0 A1 x B7 B8`
- `.x/x[*]x || d CCW o B1 B0 x A4 A3`

![example combined](assets/example_combined.png)

### HTTTX Notation
See the [official HTTTX specification](https://github.com/hex-tic-tac-toe/htttx-notation) for the notation's syntax.

The bot supports a subset of versions 1 and 2, including version 2 highlights and labels from the last move only. HTTTX export currently uses version 1 and does not include highlights or labels.

### Other
In addition to traditional HeXO notation, sandbox position links (like https://hexo.did.science/sandbox/i6z4ur1) and game links from https://hexo.did.science are also considered valid "notation". Tyto analysis links starting with `https://hexo.tyto.cc/analysis#c=` are supported as well.
You can prefix HTTTX notation or a Tyto analysis link with `#` to display turn numbers. BKE turn lists and combined positions display turn numbers automatically.

## Features
### Command `hexo`
Accepts HeXO notation as parameter and renders it as image.
Example usage:

![example slash command](assets/example_slash_command.png)

### Command `render`
Opens a modal that allows you to specify more complex HeXO notation. It allows you to mix both notation and normal text to create a composite message.
Using the modal to send a complex message is equivalent to sending a message with the notation yourself and then using the `Render HeXO notation in message` context command.
See [Message command](#message-command) for more details.

### Message command
It is also possible to render notation in existing messages. To do so, right-click the message and select `Apps > HeXO Renderer > Render HeXO notation in message`:
Any valid HeXO notation inside code blocks (triple backticks: `` ``` ``) will be rendered as image that will be put in the resulting message instead of the code block.
You can also use inline code segments (single backticks: `` ` ``) with HeXO notation in parts of your message. These segments will not be removed and the rendered image is put after the current paragraph.

![example message command](assets/example_message_command.png)

### Command `game`
Another feature is reviewing games from https://hexo.did.science in Discord. Simply use the `game` slash command and provide a game ID (or link) to the game you want to review:

![example review](assets/example_review.png)

## Contributing
Contributions are very welcome.
If you have a small or medium improvement, feel free to open a PR directly.
For larger changes, please open an issue first so we can align on scope and approach.

## Build

### Backend

The backend is split into two launchers:

- `launcher:api` serves the HTTP API and OAuth2 callbacks.
- `launcher:discord` runs the Discord bot.

To build both launchers manually, run:

```shell
./gradlew :launcher:api:shadowJar :launcher:discord:shadowJar
```

This creates the following executable jars:

- `launcher/api/build/libs/launcher-api-[version]-all.jar`
- `launcher/discord/build/libs/launcher-discord-[version]-all.jar`

Run each launcher in a separate process:

```shell
java -jar launcher/api/build/libs/launcher-api-[version]-all.jar
java -jar launcher/discord/build/libs/launcher-discord-[version]-all.jar
```

> [!NOTE]
> You need JDK 21 (or higher) installed to build the JAR. To run it, a JRE is sufficient.

Configuration is done using environment variables at runtime. Each launcher column indicates whether the variable is required, optional, or unused (`—`).

| Environment variable   | API      | Discord  | Description                                            |
|------------------------|:--------:|:--------:|--------------------------------------------------------|
| `bot.token`            | —        | Required | Discord bot token                                      |
| `oauth2.clientId`      | Optional | Optional | Discord client ID for linked roles and OAuth2          |
| `oauth2.clientSecret`  | Optional | Optional | Discord client secret for linked roles and OAuth2      |
| `oauth2.encryptionKey` | Optional | Optional | Key used to encrypt Discord tokens                     |
| `database.url`         | Optional | Optional | R2DBC URL for persistent storage                       |
| `server.port`          | Optional | —        | Port on which the API listens                          |
| `server.webUrl`        | Required | Optional | Public URL of the web application                     |
| `server.apiUrl`        | Required | Optional | Externally visible URL of the API                      |
| `auth.secret`          | Optional | —        | Base64-encoded secret used to sign authentication JWTs |
| `auth.accessTokenTtl`  | Optional | —        | Access-token lifetime, for example `15m`               |
| `auth.refreshTokenTtl` | Optional | —        | Refresh-token lifetime, for example `90d`              |

The three `auth.*` variables must be configured together to enable profiles and web login. Generate a 256-bit signing secret with:

```shell
openssl rand -base64 32
```

Keep this value private and stable. Replacing it invalidates all existing login sessions.

### Frontend

The frontend is served by the dedicated `launcher:web` Ktor application. Building the launcher automatically exports the Kobweb site with the static layout and copies it into the launcher's resources. The two API URLs are embedded into the frontend at build time:

```shell
./gradlew :launcher:web:shadowJar \
    -Pweb.hdsApiUrl=http://localhost:3001 \
    -Pweb.hmdApiUrl=http://localhost:1234
```

- `web.hdsApiUrl` is the HDS API proxy, for example `https://hexo.mineking.dev/proxy/api`.
- `web.hmdApiUrl` is the HeXO API used for watch parties and rendering, for example `https://hexo.mineking.dev/api`.

For frontend development, run Kobweb's development server from the `web` directory. It watches the frontend sources and reloads the browser when they change:

```shell
kobweb run --gradle="-Pweb.hdsApiUrl=http://localhost:3001 -Pweb.hmdApiUrl=http://localhost:1234" -p web
```

The development server can also be started through its Gradle task from the repository root:

```shell
./gradlew :web:kobwebStart \
    -Pweb.hdsApiUrl=http://localhost:3001 \
    -Pweb.hmdApiUrl=http://localhost:1234
```

Both commands serve the frontend on the port configured in `web/.kobweb/conf.yaml`, which defaults to `8080`. The dedicated Ktor launcher is used to verify the exported site and its dynamic OpenGraph handling.

This creates `launcher/web/build/libs/launcher-web-[version]-all.jar`. Set the listen port through the `server.port` environment variable when running it:

```shell
env 'server.port=8080' java -jar launcher/web/build/libs/launcher-web-[version]-all.jar
```

For local development, the launcher can be built and started directly with Gradle:

```shell
env 'server.port=8080' ./gradlew :launcher:web:run \
    -Pweb.hdsApiUrl=http://localhost:3001 \
    -Pweb.hmdApiUrl=http://localhost:1234
```

### Docker Compose

As an alternative to building the modules manually, Docker Compose builds separate API, Discord bot, and web services. The API and Discord images use `launcher/Dockerfile`. The web service uses its dedicated `launcher/web/Dockerfile`, which builds the web launcher and embeds the exported frontend in its executable jar. Set the required values in a `.env` file in the project root:

```dotenv
BOT_TOKEN=              # Discord bot token, required

OAUTH2_CLIENT_ID=       # Discord client ID for linked roles
OAUTH2_CLIENT_SECRET=   # Discord client secret for linked roles
OAUTH2_ENCRYPTION_KEY=  # Encryption key used for encrypting Discord tokens

# Generate with: openssl rand -base64 32
AUTH_SECRET=            # JWT signing secret; keep this private and stable

WEB_URL=                # Public web URL, e.g. https://hexo.mineking.dev
API_URL=                # Public API URL, e.g. https://hexo.mineking.dev/api

HDS_API_URL=            # The HDS API URL, e.g. https://hexo.mineking.dev/proxy/api
HMD_API_URL=            # The HMD API URL, e.g. https://hexo.mineking.dev/api
```

To deploy all services, run:
```shell
docker compose up
```
