# Titan

Titan is a complete Minestom-based Minecraft lobby server that provides various quality-of-life features to enhance player experience. It contains everything needed to run a fully functional Minestom server.

## Features

- **Sitting System**: Allows players to sit on specific blocks like stairs
- **Tickle Mechanic**: Players can tickle each other using feathers, with cooldown periods
- **Elytra Boost**: Provides boost functionality for players using elytra
- **Height Teleportation**: Automatically teleports players when they exceed certain height limits

## Requirements

- Java 24 or higher

## Installation

1. Download the latest release from the releases page
2. Run the server using: `java -jar titan-x.x.x.jar`
3. The server will generate default configuration files on first run

## Running the Server

Once installed, you can:

- Start the server with additional memory: `java -Xmx2G -jar titan-x.x.x.jar`
- Use the console to manage the server while it's running
- Stop the server safely by typing `stop` in the console

### Server Properties

You can configure server properties like port, MOTD, and more in the generated configuration files.

## Configuration

Configuration is done through the `app.json` file. Here are the available options:

```json
{
  "tickleDuration": 4000,
  "sitOffset": {
    "x": 0.5,
    "y": 0.25,
    "z": 0.5
  },
  "allowedSitBlocks": [
    {
      "domain": "minecraft",
      "path": "spruce_stairs"
    }
  ],
  "simulationDistance": 2,
  "fireworkBoostSlot": 45,
  "elytraBoostMultiplier": 35.0,
  "updateRateAgones": 2000,
  "maxHeightBeforeTeleport": 310,
  "minHeightBeforeTeleport": -64
}
```

### Configuration Options Explained

- `tickleDuration`: Duration of tickle cooldown in milliseconds
- `sitOffset`: Offset for sitting position (x, y, z coordinates)
- `allowedSitBlocks`: List of blocks that players can sit on
- `simulationDistance`: Simulation distance for entities
- `fireworkBoostSlot`: Inventory slot for firework boost
- `elytraBoostMultiplier`: Multiplier for elytra boost
- `updateRateAgones`: Update rate for Agones in milliseconds
- `maxHeightBeforeTeleport`: Maximum height before player teleportation
- `minHeightBeforeTeleport`: Minimum height before player teleportation

## Development

### Building from Source

1. Clone the repository
2. Build using Gradle:
   ```
   ./gradlew clean build
   ```

### Testing

Run tests using:
```
./gradlew test
```

Code coverage reports are generated using JaCoCo and can be found in `build/reports/jacoco/`.

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## Credits

Developed by OneLiteFeather Network.
