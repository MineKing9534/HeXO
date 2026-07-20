<h1>HeXO Renderer</h1>
HeXO Renderer is a small Discord bot written in Kotlin for rendering rectilinear <a href="https://hexo.did.science">HeXO</a> notation within Discord.

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
    * [Other](#other)
  * [Features](#features)
    * [Command `hexo`](#command-hexo)
    * [Command `render`](#command-render)
    * [Message command](#message-command)
    * [Command `game`](#command-game)
  * [Contributing](#contributing)
  * [Build](#build)
    * [Backend](#backend)
    * [Web](#web)
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
The length can be specified directly after. If no length is specified, it will default to `4`.

`.(>4)xx/.o(q3o)/(>4o)oo`

![example highlight line](assets/example_highlight_line.png)

#### Custom Labels
Cells can also be labeled. The label will be rendered as text inside the labeled cell.

Labels are define for the previous cell in square brackets.

`.o.[a].[b].[c].[d]/oxxxx.[e]/.[f].[g].[h]x.[i]/...[j].[k]`

![example label](assets/example_label.png)

### BKE Notation
The bot can also render a variation of BKE notation. This is especially useful if you want turn numbers to be displayed on the rendered tiles.

The basic idea of BKE notation is dividing the board in rings (identified by letters starting from 'A') around the origin and addressing cells using a ring and offset. For this to work, a zero offset line is required.
Even though the zero offset line is not required to identify a formation on an empty board, it is relevant to know in which orientation the formation should be rendered. 
Also, when applying BKE on a non-empty board, the origin and zero offset line become vital to avoid ambiguity.

To encode the zero offset line, the actual BKE notation can be prefixed with one of these indicators: `>`, `q`, `p`, `<`, `b`, `d` representing one of the right, bottom right, bottom left, left, top left or top right zero offset lines.
If no direction is specified, `d` is used implicitly.
It is also optionally possible to specify the direction (chirality) in which to step from that zero offset line by adding `CCW` or `CW`, for counterclockwise and clockwise respectively, behind the direction prefix.
If no chirality is specified, the value will default to clockwise.

For example, `p CCW o A0 A1 x A2 B3` renders as follows:

![example bke](assets/example_bke.png)

To avoid long offset values, you can optionally use sector addressing. 
For this, the board is split into 6 sectors divided by the possible zero offset lines. Using `sector.offset` you can use a sector-relative offset.
For example, `p CCW o A0 A1 x A2 B1.1` would be equivalent to the example before.

### Combined

It is also possible to combine rectilinear notation with BKE notation. This is useful if you want to encode an initial state and the moves made from that point on.
For that you just write `<rectilinear>, <bke>` with each part following the corresponding rules stated above. The BKE origin will be the top left cell by default. 
You can change this by adding a `@(q, r)` before the actual BKE notation, where q and r define the axial coordinates of the new origin relative to the top left corner.

The following are equivalent: 
- `.x/xx, b @(1,0) o A0 A1 x B3.1 B3.2`
- `.x/xx, d o A0 B1 x B2.0 B2.1`

![example combined](assets/example_combined.png)

### Other
In addition to traditional HeXO notation, sandbox position links (like https://hexo.did.science/sandbox/i6z4ur1) are also considered valid "notation".
You can optionally prefix sandbox position links with `#`, in which case the turn numbers will be rendered as well.

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
Another feature is reviewing games from https://hexo.did.science in Discord. Simply use the `game` slash command and provide a game id (or link) to the game you want to review:

![example review](assets/example_review.png)

## Contributing
Contributions are very welcome.
If you have a small or medium improvement, feel free to open a PR directly.
For larger changes, please open an issue first so we can align on scope and approach.

## Build

### Backend

To build the backend manually, run:

```shell
./gradlew :launcher:shadowJar
```

This creates `launcher/build/libs/launcher-[version]-all.jar`. You can run it with:

```shell
java -jar launcher/build/libs/launcher-[version]-all.jar
```

> [!NOTE]
> You need a JDK 21 (or higher) installed to build the jar. To run it, a JRE is sufficient.

Configuration is done using environment variables at runtime. The following environment variables are used by the backend:
```dotenv
bot.token=              # Discord bot token, required

# The oauth2 block is optional
oauth2.clientId=        # Discord client id for linked roles
oauth2.clientSecret=    # Discord client secret for linked roles
oauth2.encryptionKey=   # Encryption key used for encrypting discord tokens

# The database block is optional
database.url=           # JDBC url for persisting data

# The server block is optional
server.port=            # The port for the API to listen on
server.url=             # The public url of the server
```

### Web

To export the web module as a static site manually, run:

```shell
./gradlew :web:kobwebExport \
    -PkobwebExportLayout=STATIC \
    -Pweb.apiProxy=http://localhost:3001 \  # The HDS API proxy, e.g. https://hexo.mineking.dev/proxy
    -Pweb.toolsApi=http://localhost:1234    # The mineking hexo tools API, used for watch parties etc., e.g. https://hexo.mineking.dev
```

The generated site is written to `web/.kobweb/site`. The two URLs are embedded into the web application at build time.

### Docker Compose

As an alternative to building the modules manually, Docker Compose can build both the backend and frontend images. Set the required values in a `.env` file in the project root:

```dotenv
BOT_TOKEN=              # Discord bot token, required

# The oauth2 block is optional
OAUTH2_CLIENT_ID=       # Discord client id for linked roles
OAUTH2_CLIENT_SECRET=   # Discord client secret for linked roles
OAUTH2_ENCRYPTION_KEY=  # Encryption key used for encrypting discord tokens

SERVER_URL=             # The public server url

WEB_API_PROXY=          # The HDS API proxy, e.g. https://hexo.mineking.dev/proxy
```

To deploy both backend and frontend, run:
```shell
docker compose up
```
