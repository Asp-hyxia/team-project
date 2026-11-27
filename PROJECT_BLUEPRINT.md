# CSC207 Team Project - Project Blueprint

> **Status:** Draft for team review  
> **Last revised:** November 25, 2026
>
> **Branch for proposed changes:** `test`  
> **Ownership:** Rosalind Zhou (`@RosalindZhou`) leads M1 and UC3. Chris Li (`@chrislxn`) leads M2 and UC2. Yue Tang leads M4 and UC1, Sophia Yang leads UC4, Ran Yan leads UC5, and Anqi Feng leads UC6. See [Team Work Split](TEAM_WORK_SPLIT.md).

## 1. Project Overview

**Working title:** MCP-first Academic Assistant (final name TBD)  
**Domain:** Education / Academic Productivity  
**Platform:** Java desktop application with a Swing GUI

The project helps university students find and understand information dispersed across Canvas LMS courses. Instead of requiring students to browse many course Pages, PDFs, assignments, and announcements, the application provides an AI assistant that uses Model Context Protocol (MCP) tools to retrieve relevant course information **on demand**. A small, local-only To-Do list lets students manage their own tasks without integrating Outlook or Microsoft To Do.

The MVP is intended to demonstrate meaningful software design, teamwork, Java/Swing implementation, external API integration, and data persistence. It is **not** a production-ready general-purpose LMS client.

### Core design principle: MCP-first

The AI agent decides which approved read-only tool to call in response to a user's question. Course information is fetched as needed through an MCP interface backed by Canvas. The application does not require all LMS information to be downloaded or added to every prompt before the user can ask a question.

## 2. MVP Scope

| Priority | Feature | Notes |
| --- | --- | --- |
| P0 | Java Swing application and Clean Architecture | Mandatory course design direction |
| P0 | AI tool-calling loop | The model chooses relevant catalog items, and Java dispatches approved item reads |
| P0 | Canvas access through MCP | UC3 provides a shared course catalog and on-demand reads through the Java Canvas MCP server |
| P0 | Browse Canvas course content in UC3 | Preview custom HTML or show native Modules, and open sources through the appropriate view |
| P0 | Local To-Do with persistence | Create personal tasks directly or add a deadline from UC6 |
| P0 | Search and ask about Canvas resources in UC2 | Answer from Pages and PDFs and cite the sources actually read |
| P0 | Saved course resources in UC4 | Save, reopen, and remove source links |
| P0 | Announcement summaries | Summarize one announcement at a time and record sourced deadline evidence |
| P0 | Upcoming-deadline briefings | Merge Canvas assignment due dates and UC5 evidence, then offer **Add to To-Do** |
| Out | Outlook / Microsoft To Do synchronization | Local To-Do only |
| Out | Canvas write operations, autonomous submissions, or message posting | Read-only MCP tool allowlist |
| Out | Background system notifications, full LMS mirroring, OCR and vector database | Not required for MVP |

**Scope rule:** UC1–UC6 form the MVP. Each of the six members leads at least one use case.

## 3. Proposed User Stories and Use Cases

UC3 lets students browse and open Canvas content. UC2 searches the same catalog and uses it for Page and PDF questions. UC5 and UC6 share a deadline repository, and UC6 can send a selected deadline to UC1.

| ID | User Story | Use Case | Priority | Lead |
| --- | --- | --- | --- | --- |
| UC1 | As a student, I want to manage my personal study tasks without switching between apps, so I can keep track of my work. | **Manage Local To-Do** | P0 | **Yue Tang (`@happy-soup`)** |
| UC2 | As a student, I want to search course resources and ask about Pages or PDFs, so I can get answers that cite the relevant material. | **Find and Ask About Canvas Resources** | P0 | **Chris Li (`@chrislxn`)** |
| UC3 | As a student, I want to browse a course's available resources and open an item, so I can find content across different Canvas layouts. | **Browse and Read Canvas Course Content** | P0 | **Rosalind Zhou (`@RosalindZhou`)** |
| UC4 | As a student, I want to save useful course sources, so I can reopen them without repeating a search. | **Save and Reopen Course Resources** | P0 | **Sophia Yang (`@sophiaysh16-spec`)** |
| UC5 | As a student, I want to summarize an announcement and capture dates it mentions, so I can act on updates posted by my instructors. | **Summarize Announcements** | P0 | **Ran Yan (`@Asp-hyxia`)** |
| UC6 | As a student, I want one timeline of upcoming deadlines with an option to add a task, so I can organize my work across courses. | **Generate Deadline Briefing** | P0 | **Anqi Feng (`@fbgfaqzxm1003`)** |

### UC1 - Manage Local To-Do

- **Input:** a task action in To-Do, or a UC6 request with a deadline ID, title, due value, and source link.
- **Success:** the action updates the local task list and persists between application launches.
- **Failure:** blank/invalid title, invalid date, or missing task results in a clear error.
- **Integration:** UC1 owns local task persistence. Its create boundary returns an existing task when UC6 submits the same deadline again.

### UC2 - Find and Ask About Canvas Resources (Lead: Chris Li)

- **Input:** a selected course and a search query or question, with optional configured defaults and selected Page or PDF IDs.
- **Success:** search returns matching catalog entries. For questions, the agent uses configured and selected references, reads whole items by ID, and can choose relevant additional entries from the shared catalog. Swing displays text as it arrives when the model supports streaming, then shows original links for the sources used.
- **Failure:** report empty search results, inaccessible sources, unsupported files or size limits, insufficient evidence, and model or service errors.
- **Integration:** Chris owns UC2's UI, prompts, source selection, reading limits, and the shared AI operations used across the project. The UC2 branch streams Page answers through DeepSeek using a sample course. Live course reads will use UC3's content contract over MCP. The planned UC5 analysis operation returns an announcement summary and deadline candidates.
- **Acceptance:** ask about a selected Page or PDF. With a fixture question that requires another item, verify the agent finds it in the catalog and cites both sources. A question answered by the initial item should finish with one read.

### UC3 - Browse and Read Canvas Course Content (Lead: Rosalind Zhou)

- **Input:** add a course by URL and Canvas connection, or open a configured course. Then follow a Page link, expand a module, or open a resource.
- **Success:** Swing renders a custom home as HTML or shows Modules in a native list. Links to supported Pages from the HTML preview continue in that pane. Files and Page or file rows in Modules open in the browser. The catalog combines Page and PDF metadata from course discovery routes.
- **Failure:** distinguish an empty catalog from a partial catalog or access failure. Report an inaccessible item when selected and keep the other available entries visible.
- **Integration:** Rosalind owns UC3's browsing UI, Canvas REST adapter, and MCP tool handlers for authorized reads. UC5 and UC6 use the shared Canvas connection for announcements and assignments.
- **Acceptance:** follow a Page link inside a custom HTML home, then expand Modules and open a module resource in the browser. Verify that a UC2 read of the same resource succeeds by catalog ID.

See the [UC3 browsing and navigation design](docs/UC3_CANVAS_BROWSING.md) for course setup, rendering, link routing, and detailed acceptance scenarios.

### UC4 - Save and Reopen Course Resources

- **Input:** save a title and link from a UC3 catalog entry or a UC2 answer citation, or select a saved source to open or remove.
- **Success:** saved titles and links survive a restart. Opening a saved link takes the student to the original Canvas source.
- **Failure:** report an invalid link or local persistence error.
- **Boundary:** store source metadata locally. Canvas checks access when the link is opened.

### UC5 - Summarize Announcements

- **Input:** click **Summarize** on one announcement in a course group on the Dashboard.
- **Success:** retrieve the selected announcement, call the shared AI analysis interface, and display the summary through the UC5 Presenter. Save deadline evidence grounded in the announcement to the shared repository.
- **Failure:** identify the affected course or announcement and let the student retry a failed read or analysis.
- **Integration:** UC5 owns the course groups, announcement actions, evidence validation, and its own Presenter. A weekly briefing can yield multiple deadline candidates.

### UC6 - Generate Deadline Briefing (Lead: Anqi Feng)

- **Input:** choose a date range in **Upcoming deadlines**, or select a row to add it to To-Do.
- **Success:** combine Canvas assignment due dates with UC5's announcement evidence in one ordered timeline. Display their sources and flag ambiguous or conflicting dates for review.
- **Integration:** UC6 owns deadline reconciliation, filtering, and the timeline UI. **Add to To-Do** calls the UC1 create-task boundary with the deadline's title, due value, stable ID, and source link.

See [academic workflows](docs/ACADEMIC_WORKFLOWS.md) for UC1, UC4, UC5, and UC6 acceptance cases and shared interfaces.

## 4. Domain Model (Initial)

See [Shared Entities and Interfaces](docs/SHARED_ENTITIES_AND_INTERFACES.md) for how each type is used across UC1–UC6 and which Java definitions are ready.

| Entity / value object | Suggested fields                                | Purpose                                                     |
|-----------------------|-------------------------------------------------|-------------------------------------------------------------|
| `Task`                | `id`, `title`, optional due date or time, `completed`, optional deadline ID and source link | Locally persisted personal task and its completion behavior |
| `Course`              | `id`, `name`, `courseCode`                      | A course returned by Canvas                                 |
| `CourseResource`      | `id`, `courseId`, `title`, `type`, `sourceUrl`  | Catalog metadata for a Canvas Page or file                   |
| `Announcement`       | `id`, `courseId`, title, posted time, body, source URL | Source for UC5 summaries and deadline discovery |
| `AcademicDeadline`    | `id`, `courseId`, `title`, due date or time when known, source evidence | Shared deadline record for UC5 and UC6 |
| `DeadlineEvidence`    | Source kind, source ID or URL, reported date or time, supporting wording when applicable | Evidence for merging and checking deadline dates |
| `SourceReference`     | `title`, `url`                                  | Value object attached to AI answers                         |

These are application models, **not** the raw JSON structures returned by Canvas or MCP. Do not add fields and classes until a Use Case requires them. We should document class attributes and relevant methods in the README or an accompanying UML class diagram before the final presentation.

## 5. Architecture and Modules

```mermaid
flowchart LR
    U3["UC3 · Browse and open in Swing"] -->|"User selects an item"| Content["Shared catalog and item reader"]
    U2["UC2 · Search and ask in Swing"] -->|"Agent inspects catalog and requests item IDs"| Content
    U2 -->|"Question and retrieved sources"| AI["UC2 · Shared AI operations"]
    U5["UC5 · Announcement summaries"] -.->|"Planned analysis of one announcement"| AI
    AI -->|"Text increments and final result"| U2
    Content --> Client["M2 · Java MCP client"]
    Client --> Server["UC3 · Canvas MCP server and REST adapter"]
    Server --> Canvas["Canvas REST API"]
```

Each Swing flow goes through its Controller, Interactor, Presenter, and ViewModel. UC2 and UC3 share the content contract and run through separate input boundaries.

### Modules and interfaces

| Module                                   | Description                                                                                        | Main interface / integration point  | Owner                               |
|------------------------------------------|----------------------------------------------------------------------------------------------------|-------------------------------------|-------------------------------------|
| **M1 - AI Agent Core**                   | Gemini requests, tool-call protocol, and provider error handling for the original Page flow | `AgentGateway` | **Rosalind Zhou (`@RosalindZhou`)** |
| **UC2 - Shared AI operations**          | Text generation with final or streamed output, and typed operations for other use cases as needed | `QuestionModelGateway` for Page questions. Announcement analysis is planned. | **Chris Li (`@chrislxn`)** |
| **M2 - MCP Client Integration**          | Java MCP client and integration with UC3's catalog and read contracts | `McpToolGateway` | **Chris Li (`@chrislxn`)** |
| **M3 - Document Handling**               | PDF validation and document handoff | `DocumentGateway` (planned) | TBD |
| **M4 - Local Storage & To-Do**           | Task Entity, local persistence, task operations                                                    | `TaskRepository`                    | **Yue Tang (`@happy-soup`)**        |
| **M5 - Swing Frontend**                  | Chat, resource, task and results views; ViewModels and application UI conventions                  | Controllers / Presenters / Views    | TBD                                 |
| **M6 - Academic Features & Integration** | Announcement/deadline workflows, shared composition / AppBuilder, demo integration                 | Use Case boundaries and AppBuilder  | TBD                                 |

**Ownership model:** Every person should have a primary shared module responsibility **and** lead a complete user-facing Use Case. Technical module ownership is not a substitute for CSC207's individual Use Case leadership requirement. Feature teams may contribute changes across modules, subject to review.

### Clean Architecture rules

1. Interactors depend on interfaces, not on concrete LLM SDKs, Python, HTTP, or Swing classes.
2. Controllers translate user input to Input Data; Presenters translate Output Data to ViewModels.
3. The shared agent and MCP connection are implemented once and reused, not rewritten for each AI feature.
4. External API DTOs and parsing logic belong in gateway adapters, not Entities.
5. Network and model calls must not block Swing's Event Dispatch Thread.
6. Views can share UI components, while Use Cases retain clear inputs, success/failure paths, and tests.

### Shared course content contract

The following operations describe the planned application interface. UC3 supplies the catalog and reads, and M2 maps them to the Java MCP client.

| Operation | Input | Result |
| --- | --- | --- |
| `listContent` | `courseId`, optional `parentId`, and optional pagination cursor | The shared catalog or the requested branch. Include `contentId`, title, type, canonical source URL, locations, and any continuation cursor or coverage warnings. |
| `readContent` | `courseId` and `contentId` | Authorized Page content or PDF with its title and canonical source URL. Return a typed error for denied access, an unavailable item, unsupported content, or a service failure. |

Start from the existing `CourseResource` and `ResourceListing` metadata. Deduplicate items across discovery routes by course, resource type, and resource ID, retaining module locations. An empty home leaves the Pages, Files, and Modules discovery routes available. Modules organize Page and PDF entries. Announcement and assignment entries can be added through the existing UC5 and UC6 retrieval paths if the catalog expands.

The catalog holds metadata. In-app Page previews and the agent read item contents on demand. Browser actions use the Canvas source links. The server validates course scope and item permissions for every read. For the MVP, UC2 searches one selected course and reads up to three distinct items per question. Apply that limit to the combined default, selected, and agent-chosen references. Show the limit before submission if configured and selected references already exceed it. UC2 assembles citations from the items actually read.

### Other shared contracts

- `QuestionModelGateway`: UC2's provider-neutral text port. The same call can expose text increments and return the final answer. See the [shared AI interface](docs/SHARED_ENTITIES_AND_INTERFACES.md#shared-ai-calls-and-streaming).
- `AgentGateway`: M1's model port for the original single-Page Gemini flow.
- `McpToolGateway`: `listTools()` and `callTool(name, args)` provide controlled access to approved MCP functions.
- `DeadlineRepository`: store announcement evidence from UC5 and assignment evidence from UC6, and return the merged timeline for UC6.
- UC1 create-task boundary: accept a deadline reference from UC6. UC1 uses its `TaskRepository` to save, find, list, and delete local tasks.
- `DocumentGateway`: planned PDF handoff between authorized retrieval and model input.
- `Clock`: current date/time for deterministic deadline logic, if needed.

The current Java prototype exposes `list_courses`, `list_resources`, and `read_page`. The unified catalog, PDF reads, and model-selected additional reads are planned extensions.

### Streaming in Swing

Any use case that needs AI can call UC2's shared AI module using a Java interface injected by the composition root. The caller keeps its own Interactor and Presenter. Each AI workflow can show a loading state and wait for the final result, or opt in to progressive text. For example, UC2 displays answer text as it arrives, while UC5 can wait for a complete summary and validated deadline candidates.

Run the model request and read incremental text on a worker thread. The Presenter buffers text, and the view appends batches on Swing's Event Dispatch Thread. In UC2 a timer refreshes about every 50 ms. The final result supplies citations or other validated data, and a failure ends the preview and restores the controls. See the [shared callback and completion contract](docs/SHARED_ENTITIES_AND_INTERFACES.md#shared-ai-calls-and-streaming).

## 6. API Integration, Persistence and Security

**Canvas.** UC3 owns the read-only Canvas REST adapter and Java MCP tool handlers. M2 integrates the Java client. See the [Canvas developer documentation](https://developerdocs.instructure.com/services/canvas).

**LLM providers.** The original Page prototype uses M1's Gemini client. The UC2 branch supports streaming Page answers through its DeepSeek adapter. PDF input and model-requested reads of additional catalog items are planned.

**Local persistence.** Personal tasks, saved links, and shared deadline records can use JSON or another simple local format behind repositories. Canvas cache entries should show their fetch timestamps and freshness.

**Security constraints:** Never commit Canvas or LLM tokens, `.env` files containing secrets, downloaded private course material, or cached credentials. Keep Canvas tools read-only. Use an allowlist of permitted tool calls. Treat LMS text and PDF content as untrusted data; it must not override program safety rules or authorize extra operations.

## 7. UC2 and UC3 Demonstration

1. In UC3, open the course and follow a Page link in the custom HTML home. Swing shows the Page preview and its original Canvas link. In a Modules-based course, expand a module and open a resource in the browser.
2. Select the Project Timeline as a reference and ask a question in the UC2 panel.
3. UC2 reads the selected source along with any configured default references. If the question needs information about evaluation, the agent can choose Project Expectations from the same catalog.
4. Swing shows the answer and links to the retrieved sources. Repeat with a permitted PDF to verify the document path.

Use fixture catalogs to verify browsing, access errors, and the choice to read another item. Verify the Canvas integration and run a live DeepSeek streaming smoke test.

## 8. Testing and Course Timeline

| Date              | Milestone                                 | Evidence                                                                                                                                                      |
|-------------------|-------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Nov 20, 2026**  | Individual progress and working prototype | Each member: substantial PR, review of at least one teammate PR, runnable assigned work; team: a working external API call and at least one complete Use Case |
| **Nov 27, 2026**  | Near-complete MVP                         | MVP integrated, at least one unit test per Interactor                                                                                                         |
| **Dec 4, 2026**   | Final polish                              | 100% line coverage for Entity and Use Case layers; resolved bugs; presentation practice                                                                       |
| **Dec 7-8, 2026** | Final presentations                       | Demonstrate MVP; all members explain their own work and design decisions                                                                                      |

Course sources: [Project Timeline](https://q.utoronto.ca/courses/444184/pages/project-timeline-2), [Project README and MVP guidance](https://q.utoronto.ca/courses/444184/pages/project-readme-and-minimum-viable-product-mvp), [Project Expectations](https://q.utoronto.ca/courses/444184/pages/project-expectations).

## 9. Decisions Needed from the Team

- [ ] Confirm the actual team size and the final number of MVP Use Cases.
- [ ] Review the acceptance criteria for the six MVP use cases.
- [ ] Assign every remaining Module Owner and Use Case Lead (`TBD` entries).
- [ ] Agree on MCP payloads for the shared catalog and item reader, including pagination and source permissions.
- [ ] Confirm PDF input and model-driven reads against the shared content contract.
- [ ] Agree on the shared Java interface signatures and error response types.
- [ ] Decide the local To-Do storage format and whether optional Canvas caching belongs in the MVP.

**Related document:** [Team Work Split](TEAM_WORK_SPLIT.md).
