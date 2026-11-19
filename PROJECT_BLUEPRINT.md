# CSC207 Team Project - Project Blueprint

> **Status:** Draft for team review  
> **Last revised:** November 18, 2026  
> **Branch for proposed changes:** `test`  
> **Ownership:** See [Team Work Split](TEAM_WORK_SPLIT.md). M1 / UC3 are claimed by Rosalind Zhou (`@RosalindZhou`); M2 / UC2 are owned by Chris Li (`@chrislxn`). Remaining owners are `TBD`.

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
| P0 | AI tool-calling loop | Real LLM requests tools, Java dispatches tool calls and returns results |
| P0 | Canvas access through MCP | Reuse and integrate an existing Python Canvas MCP server where feasible |
| P0 | Resource discovery and Canvas Page question answering | First end-to-end demo target |
| P0 | Local To-Do with persistence | Owned by one member as a complete feature |
| P0 | Canvas PDF question answering | Start with one permitted PDF; model/file-input integration |
| P1 | Announcement summaries | Add once core tool calls are stable |
| P1 | Upcoming-deadline briefings | Canvas assignments; optional inclusion of local tasks |
| Out | Outlook / Microsoft To Do synchronization | Local To-Do only |
| Out | Canvas write operations, autonomous submissions, or message posting | Read-only MCP tool allowlist |
| Out | Background system notifications, full LMS mirroring, OCR and vector database | Not required for MVP |

**Scope rule:** P1 features may move into or out of the final MVP after a team decision. The number of final Use Cases must be consistent with the actual team size, and every team member must lead at least one substantial Use Case.

## 3. Proposed User Stories and Use Cases

These are **proposed refinements** of the User Stories in the current `test` branch README, not already approved team assignments.

| ID | User Story | Use Case | Priority | Lead |
| --- | --- | --- | --- | --- |
| UC1 | As a student, I want to manage my personal study tasks without switching between apps, so I can keep track of my work. | **Manage Local To-Do** | P0 | TBD |
| UC2 | As a student, I want to find relevant course Pages and files through the assistant, so I can locate resources without browsing every course. | **Find Canvas Resources** | P0 | **Chris Li (`@chrislxn`)** |
| UC3 | As a student, I want to ask questions about a Canvas Page, so I can understand course instructions quickly. | **Ask About Canvas Pages** | P0 | **Rosalind Zhou (`@RosalindZhou`)** |
| UC4 | As a student, I want to ask questions about PDFs on Canvas, so I can work with course materials without manually downloading and uploading them. | **Ask About Canvas PDFs** | P0 | TBD |
| UC5 | As a student, I want a summary of recent course announcements, so I can identify important updates. | **Summarize Announcements** | P1 | TBD |
| UC6 | As a student, I want a briefing of approaching deadlines, so I can prioritize my academic work. | **Generate Deadline Briefing** | P1 | TBD |

### UC1 - Manage Local To-Do

- **Input:** task title, optional due date, and a requested action (create, view, complete, reopen, or delete).
- **Success:** the action updates the local task list and persists between application launches.
- **Failure:** blank/invalid title, invalid date, or missing task results in a clear error.
- **Boundary:** no Microsoft Graph, Outlook, or third-party To-Do synchronization.

### UC2 - Find Canvas Resources (Lead: Chris Li)

- **Input:** a query or course selection from the Swing interface.
- **Success:** the application obtains relevant Canvas Page/file metadata using authorized MCP tools and displays resource titles, types, and source links.
- **Failure:** no matches, permission denial, disconnected MCP server, or unavailable Canvas is explained to the user.
- **Boundary:** discovery is read-only and does not require downloading all course resources.
- **Independent acceptance:** with a real connected account, the user can search for a known course resource and open its Canvas source link. A fake MCP/agent adapter allows unit tests without network access.

### UC3 - Ask About Canvas Pages (Lead: Rosalind Zhou)

- **Input:** a natural-language question about course information.
- **Success:** the AI chooses approved MCP tools, retrieves relevant Page content, and answers with the Canvas source link(s).
- **Failure:** inaccessible Page or unavailable provider is shown as a recoverable error.
- **Boundary:** the model should not invent a source link or treat course text as instructions to execute unapproved tools.

### UC4 - Ask About Canvas PDFs

- **Input:** a question concerning a PDF available in a course.
- **Success:** the agent discovers the file using MCP, the permitted PDF is downloaded or retrieved on demand, and the Java model adapter supplies it using the selected provider's file-input API. The response cites the original Canvas file.
- **Failure:** unsupported type, size limit, inaccessible file, or model limitations are surfaced clearly.
- **Boundary:** MVP targets accessible PDFs; OCR and full document search/indexing are excluded.

### UC5 - Summarize Announcements

- **Input:** selected course(s) and a recent time window.
- **Success:** the agent retrieves permitted announcements and presents a concise, dated summary with source links and actionable items.
- **Boundary:** do not claim an announcement is current without checking its retrieval time.

### UC6 - Generate Deadline Briefing

- **Input:** requested period (for example, the next seven days).
- **Success:** the program reads assignments and due dates via MCP, orders them by actual deadline, and displays an informative briefing. A local task list may be included if time permits.
- **Boundary:** suggestions are not official course deadlines; ordering and filtering should be independently testable without an LLM.

## 4. Domain Model (Initial)

| Entity / value object | Suggested fields | Purpose |
| --- | --- | --- |
| `Task` | `id`, `title`, `dueAt`, `completed` | Locally persisted personal task and its completion behavior |
| `Course` | `id`, `name`, `courseCode` | A course returned by Canvas |
| `CourseResource` | `id`, `courseId`, `title`, `type`, `sourceUrl` | Metadata for Canvas Pages and files |
| `AcademicDeadline` | `id`, `courseId`, `title`, `dueAt`, `sourceUrl` | Due dates for briefing and presentation |
| `SourceReference` | `title`, `url` | Value object attached to AI answers |

These are application models, **not** the raw JSON structures returned by Canvas or MCP. Do not add fields and classes until a Use Case requires them. We should document class attributes and relevant methods in the README or an accompanying UML class diagram before the final presentation.

## 5. Architecture and Modules

```text
Java Swing Views: Chat / Resources / To-Do / Briefing
           |
     Controllers, Presenters, ViewModels
           |
      Java Use Case Interactors
           |
           +---- AgentGateway (interface)
           |          |
           |          +---- LLM Agent Adapter (tool-calling loop)
           |                         |
           |                    McpToolGateway (interface)
           |                         |
           |                    Java MCP Adapter
           |                         |
           |                 Existing Python Canvas MCP Server
           |                         |
           |                    Canvas REST API
           |
           +---- TaskRepository (interface)
           |          |
           |     Local File DAO
           |
           +---- Clock (interface, where useful for deadline tests)
```

### Modules and interfaces

| Module | Description | Main interface / integration point | Owner |
| --- | --- | --- | --- |
| **M1 - AI Agent Core** | LLM API, tool selection, multi-step tool-calling loop, failure handling | `AgentGateway` | **Rosalind Zhou (`@RosalindZhou`)** |
| **M2 - MCP & Canvas Integration** | Java MCP adapter, Python server connection, read-only Canvas tool contracts and resource discovery | `McpToolGateway` / resource gateway | **Chris Li (`@chrislxn`)** |
| **M3 - Document Handling** | File retrieval, optional on-demand PDF cache, model-compatible PDF input | `DocumentGateway` | TBD |
| **M4 - Local Storage & To-Do** | Task Entity, local persistence, task operations | `TaskRepository` | TBD |
| **M5 - Swing Frontend** | Chat, resource, task and results views; ViewModels and application UI conventions | Controllers / Presenters / Views | TBD |
| **M6 - Academic Features & Integration** | Announcement/deadline workflows, shared composition / AppBuilder, demo integration | Use Case boundaries and AppBuilder | TBD |

**Ownership model:** Every person should have a primary shared module responsibility **and** lead a complete user-facing Use Case. Technical module ownership is not a substitute for CSC207's individual Use Case leadership requirement. Feature teams may contribute changes across modules, subject to review.

### Clean Architecture rules

1. Interactors depend on interfaces, not on concrete LLM SDKs, Python, HTTP, or Swing classes.
2. Controllers translate user input to Input Data; Presenters translate Output Data to ViewModels.
3. The shared agent and MCP connection are implemented once and reused, not rewritten for each AI feature.
4. External API DTOs and parsing logic belong in gateway adapters, not Entities.
5. Network and model calls must not block Swing's Event Dispatch Thread.
6. Views can share UI components, while Use Cases retain clear inputs, success/failure paths, and tests.

### Shared contracts to finalize before parallel implementation

- `AgentGateway`: `answer(question, allowedTools, context)` returns text, source references, and a status (conceptual signature only).
- `McpToolGateway`: `listTools()` and `callTool(name, args)` provide controlled access to approved MCP functions.
- `TaskRepository`: save, find, list and delete local tasks.
- `DocumentGateway`: retrieve a permitted Canvas document and return a model-compatible representation.
- `Clock`: current date/time for deterministic deadline logic, if needed.

**MCP tool names, arguments, return payloads and transport are not yet verified** against the existing Python server. M2 documents the actual contract before other members depend on it. First verify a small set such as course listing, Page listing and Page reading; PDF and announcement tools can follow.

## 6. API Integration, Persistence and Security

**Canvas:** Official developer documentation: <https://developerdocs.instructure.com/services/canvas>. The Python MCP server should expose a minimal, read-only subset; Java talks to it via an agreed MCP transport (stdio or Streamable HTTP, TBD).

**LLM provider:** Gemini; model selection and live API verification are pending. It must support tool calling for the core Agent. PDF Use Case additionally requires a supported PDF/file-input mechanism; file transport via MCP and upload/input to the LLM are separate integration steps.

**Local persistence:** Personal To-Do data can be stored in JSON or another simple local format behind a DAO. A Canvas cache is optional and should be on demand, associated with source and fetch timestamps; stale content must be distinguishable from freshly retrieved information.

**Security constraints:** Never commit Canvas or LLM tokens, `.env` files containing secrets, downloaded private course material, or cached credentials. Keep Canvas tools read-only. Use an allowlist of permitted tool calls. Treat LMS text and PDF content as untrusted data; it must not override program safety rules or authorize extra operations.

## 7. First End-to-End Demonstration

**Goal:** A student asks about the CSC207 Project Timeline and receives an answer citing the Canvas Page, without copying the Page manually into the chat.

1. User enters a question in the Swing Chat View.
2. Controller produces `AskPageInputData`.
3. Interactor invokes `AgentGateway`.
4. AI requests an authorized MCP Page discovery/read tool.
5. Java MCP Adapter calls the Python Canvas MCP Server; server fetches Page content through Canvas.
6. Agent receives returned content and generates an answer with source reference(s).
7. Presenter updates the ViewModel; Swing shows the answer and source URL.

**Minimum success criterion by Nov 20:** A real external tool call and at least one demonstrated full Use Case. The team should also show independent runnable contributions from individual members. Use a fake gateway for unit tests and a live smoke test for integration.

## 8. Testing and Course Timeline

| Date | Milestone | Evidence |
| --- | --- | --- |
| **Nov 20, 2026** | Individual progress and working prototype | Each member: substantial PR, review of at least one teammate PR, runnable assigned work; team: a working external API call and at least one complete Use Case |
| **Nov 27, 2026** | Near-complete MVP | MVP integrated, at least one unit test per Interactor |
| **Dec 4, 2026** | Final polish | 100% line coverage for Entity and Use Case layers; resolved bugs; presentation practice |
| **Dec 7-8, 2026** | Final presentations | Demonstrate MVP; all members explain their own work and design decisions |

Course sources: [Project Timeline](https://q.utoronto.ca/courses/444184/pages/project-timeline-2), [Project README and MVP guidance](https://q.utoronto.ca/courses/444184/pages/project-readme-and-minimum-viable-product-mvp), [Project Expectations](https://q.utoronto.ca/courses/444184/pages/project-expectations).

## 9. Decisions Needed from the Team

- [ ] Confirm the actual team size and the final number of MVP Use Cases.
- [ ] Approve the P0/P1 feature scope.
- [ ] Assign every remaining Module Owner and Use Case Lead (`TBD` entries).
- [ ] Confirm the existing Python MCP server's supported tool names, transport and setup instructions.
- [ ] Choose a compatible Java runtime/MCP library and an LLM provider.
- [ ] Agree on the shared Java interface signatures and error response types.
- [ ] Decide the local To-Do storage format and whether optional Canvas caching belongs in the MVP.

**Related document:** [Team Work Split](TEAM_WORK_SPLIT.md).
