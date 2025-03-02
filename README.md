# Kibana Opener Plugin for IntelliJ

[![Version](https://img.shields.io/badge/version-0.0.1-blue.svg)](https://plugins.jetbrains.com/plugin/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

A plugin for IntelliJ IDEA that provides an easy way to open Kibana UI directly from your IDE for viewing logs related to your code.

## Features

- Open Kibana logs directly from your code
- Automatically extract context information from your code (logger name, log level, URI, HTTP method)
- Configure multiple Kibana environments
- Customize query parameters for each environment
- Works with Java and Kotlin files

## Installation

### From JetBrains Marketplace

1. In IntelliJ IDEA, go to **Settings/Preferences** → **Plugins** → **Marketplace**
2. Search for "Kibana Opener"
3. Click **Install**
4. Restart IntelliJ IDEA when prompted

### Manual Installation

1. Download the plugin JAR file from [releases](https://github.com/yahito/kibana-plugin/releases)
2. In IntelliJ IDEA, go to **Settings/Preferences** → **Plugins** → ⚙️ → **Install Plugin from Disk**
3. Select the downloaded JAR file
4. Restart IntelliJ IDEA when prompted

## Configuration

1. Go to **Settings/Preferences** → **Build, Execution, Deployment** → **Kibana Opener**
2. Add one or more Kibana environments by clicking the "+" button
3. For each environment, configure:
   - Name: A display name for this environment
   - URL: The base URL of your Kibana instance (e.g., `http://localhost:5601`)
   - Index: The Elasticsearch index to search in
   - Parameters: Key-value pairs for filtering logs

### Default Parameters

The plugin comes with default parameters to help you get started:

- `logger`: `%logger` (automatically filled with the current class name)
- `level`: `%level` (filled with log level when navigating from a log statement)
- `requestUri`: `%uri` (filled with URI when navigating from an endpoint)
- `requestMethod`: `%httpMethod` (filled with HTTP method when navigating from an endpoint)
- `interval`: `24h` (time range to display logs)

You can modify or add additional parameters as needed.

## Usage

### Opening Kibana from a Log Statement

1. Place your cursor on a logger method call (e.g., `logger.info("Message")`)
2. Press `Alt+Insert` (Windows/Linux) or `⌘N` (macOS) to open the Generate menu
3. Select **Kibana** and choose your configured environment

### Opening Kibana from a REST Controller

1. Place your cursor within a Spring REST controller method with annotations like `@GetMapping`, `@PostMapping`, etc.
2. Press `Alt+Insert` (Windows/Linux) or `⌘N` (macOS) to open the Generate menu
3. Select **Kibana** and choose your configured environment

The plugin will automatically extract:
- The URI path from the controller's `@RequestMapping` and method's mapping annotation
- The HTTP method from the mapping annotation

## Supported IDEs

- IntelliJ IDEA 2020.1+
- Supports both Community and Ultimate editions

## Spring Framework Integration

The plugin has special handling for Spring Web annotations:
- `@GetMapping`
- `@PostMapping`
- `@DeleteMapping`
- `@PatchMapping`
- `@RequestMapping`

## Building from Source

```bash
# Clone the repository
git clone https://github.com/yahito/kibana-plugin.git
cd kibana-plugin

# Build the plugin
./gradlew buildPlugin

# The plugin JAR will be in build/libs/
```

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.
