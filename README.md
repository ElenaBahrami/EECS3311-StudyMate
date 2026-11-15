# StudyMate: AI Study and Learning Agent

**EECS3311 Software Design — Stage 2 Project**

StudyMate is a Java-based study application that helps students study using their own course materials.

The application supports both a **Swing graphical user interface (GUI)** and a **command-line interface (CLI)**.

StudyMate combines AI-assisted learning features with deterministic Java logic for operations such as quiz grading, progress tracking, study-plan validation, persistence, confirmation, and undo.

## Features

StudyMate implements the following ten features:

- **F01 — Import Course Materials**  
  Import PDF or TXT course materials, extract the text, divide it into chunks, and save the material locally.

- **F02 — Summarise Materials**  
  Generate a grounded summary using evidence retrieved from imported course materials.

- **F03 — Extract Key Concepts**  
  Identify important concepts and definitions from the imported materials.

- **F04 — Answer Course Questions**  
  Ask questions about the imported course material and receive grounded answers with source references.

- **F05 — Generate Study Plan**  
  Generate a study schedule using exam dates, availability, estimated study effort, and selectable scheduling strategies.

- **F06 — Generate Flashcards**  
  Generate source-grounded flashcards for a selected topic with reveal/hide answer functionality.

- **F07 — Generate Quiz**  
  Generate grounded multiple-choice quizzes with topic, difficulty, and question-count controls.

- **F08 — Grade Quiz and Explain Mistakes**  
  Grade quizzes deterministically using the stored answer key and optionally generate AI explanations for incorrect answers.

- **F09 — Track Progress and Weak Topics**  
  Calculate topic accuracy from committed quiz attempts and identify weak topics.

- **F10 — Adapt, Confirm, Reject, and Undo Study Plans**  
  Preview study-plan revisions, explicitly confirm or reject them, and undo confirmed revisions using plan versioning.

## Design Patterns

StudyMate demonstrates five software design patterns:

### Adapter
Classes:
- `LLMClient`
- `OpenAIAdapter`
- `LearningAgent`

`OpenAIAdapter` isolates provider-specific API communication from the rest of the application.

### Strategy
Classes:
- `SchedulingStrategy`
- `DeadlineStrategy`
- `WeakTopicStrategy`
- `StudyPlanner`

Different study-scheduling algorithms can be selected without rewriting the main planner.

### Observer
Classes:
- `ProgressListener`
- `ProgressTracker`

Progress listeners can be notified after committed quiz attempts update student progress.

### Command
Classes:
- `PlanCommand`
- `RevisePlanCommand`
- `AgentController`

Study-plan revisions are represented as commands that support confirmation and undo.

### Facade
Class:
- `AgentController`

`AgentController` provides a simplified application-level interface for study-plan revision operations.

## Technologies

- Java 21
- Maven
- Java Swing
- SQLite
- Apache PDFBox
- OpenAI API

## Requirements

Before running StudyMate, install:

- Java 21 or later
- Maven
- An OpenAI API key

## OpenAI API Key

StudyMate reads the OpenAI API key from the environment variable:

```text
OPENAI_API_KEY
```

Do **not** place a real API key directly in the source code or commit it to GitHub.

Example:

```text
OPENAI_API_KEY=YOUR_API_KEY_HERE
```

A safe `.env.example` file can be used to show the required variable without exposing credentials.

## Build the Project

From the project directory, run:

```bash
mvn clean compile
```

## Run the GUI

The Swing GUI entry point is:

```text
studymate.gui.StudyGUI
```

In Eclipse:

1. Open `StudyGUI.java`.
2. Right-click the file.
3. Select **Run As → Java Application**.
4. Make sure `OPENAI_API_KEY` is configured in the run configuration environment.

## Run the CLI

The command-line entry point is:

```text
studymate.Main
```

In Eclipse:

1. Open `Main.java`.
2. Right-click the file.
3. Select **Run As → Java Application**.
4. Make sure `OPENAI_API_KEY` is configured in the run configuration environment.

The CLI provides access to operations including material import, summarisation, concept extraction, question answering, study planning, flashcards, quizzes, grading, progress tracking, and study-plan revision.

## Persistence

StudyMate stores application data locally using SQLite.

The local database file is not committed to the public repository.

## Demo Video

Full Stage 2 demonstration:

https://youtu.be/Z6pgRGJNu5s

The demonstration includes:

- F01–F10
- GUI operation
- CLI operation
- Adapter pattern
- Strategy pattern
- Observer pattern
- Command pattern
- Facade pattern

## Reports

The repository contains:

- Stage 1 design report
- Stage 2 implementation report

## Repository

GitHub:

https://github.com/ElenaBahrami/EECS3311-StudyMate

## Author

**Elena Bahrami Hezaveh**  
Student Number:221487616
EECS3311 Software Design  
York University
