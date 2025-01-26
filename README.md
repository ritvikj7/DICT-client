# Dictionary Client

## Overview

This project implements a dictionary client that communicates with a DICT server using Java socket-related classes, as described in RFC 2229. The client provides basic dictionary functionality through a graphical interface and supports a subset of the DICT protocol.

The application establishes a connection to the DICT server, sends and receives commands, and displays the results in the user interface. The goal of this assignment is to implement key features that allow users to interact with various dictionaries and search for word definitions.

## Features

- **Connection Establishment:** The client establishes a connection to the DICT server, receiving an initial welcome message.
- **Database Listing:** Request, receive, and display a list of available databases (dictionaries) from the server.
- **Database Information:** Request and display detailed information about a specific dictionary.
- **Matching Strategies:** Retrieve and display a list of supported matching strategies for querying the dictionaries (e.g., prefix match, regular expression).
- **Word Suggestions:** As users type a word, the client suggests matching entries from a selected database using a chosen matching strategy.
- **Word Definitions:** Retrieve and display the definitions of a word from the selected database.

## Requirements

- **Java Version:** Java 8 or higher.
- **Libraries/Tools:** 
  - Java socket programming for communication.
  - A graphical user interface (GUI) framework (Swing, JavaFX, etc.) for user interaction.

## Getting Started

1. **Download and Extract the Assignment Files:**
   - Clone the repo
   - Import the `Dictionary` directory into your favorite IDE (e.g., IntelliJ IDEA, Eclipse).


2. **Graphical Interface:**
   - Implement the user interface to allow users to:
     - Select a dictionary from the list.
     - Choose a matching strategy.
     - Enter a word to search for suggestions and definitions.
     - View the results in the interface.

## Usage

1. Run the client application.
2. The graphical interface will appear, allowing you to:
   - Choose a database.
   - Select a matching strategy.
   - Type a word to see matching suggestions and definitions.
3. Interact with the DICT server through the interface as you type words or search for definitions.



