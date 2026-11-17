# Java status protocol references

Research checked on 2026-11-10. These are community-maintained protocol references, not Mojang's official API documentation.

- [PrismarineJS Minecraft protocol version data](https://github.com/PrismarineJS/minecraft-data/blob/master/data/pc/common/protocolVersions.json) lists Java Edition 1.20.6 as protocol version 766.
- [PrismarineJS protocol data for Java 1.20.5](https://prismarinejs.github.io/minecraft-data/protocol/pc/1.20.5/) documents the handshake fields (`protocolVersion`, `serverHost`, `serverPort`, `nextState`) and the status request, status response, ping, and pong packet IDs and payloads. Java 1.20.5 and 1.20.6 share protocol version 766.
- [PrismarineJS protocol data format notes](https://github.com/PrismarineJS/minecraft-data/blob/master/doc/protocol.md) describe the generated protocol definitions and Java Edition's big-endian default encoding.

The implementation currently uses only the uncompressed server-list status state. It does not implement login, account/session authentication, encryption, compression, configuration, or gameplay packets.
