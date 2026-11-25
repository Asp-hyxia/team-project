# CSC207 Team Project - Team Work Split

> **Status:** Draft for team approval.  
> **Last revised:** November 25, 2026.
>
> **Work split:** Rosalind Zhou leads M1 and UC3. Chris Li leads M2 and UC2. Yue Tang leads M4 and UC1, and Ran Yan leads UC5. Remaining owners are TBD.

## 1. Module Ownership

| Module                               | Owner                               | Responsibilities                                                                                                         | First Deliverable                                                   |
|--------------------------------------|-------------------------------------|--------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------|
| M1 - AI Agent Core                   | **Rosalind Zhou (`@RosalindZhou`)** | Shared Gemini client, tool-call protocol, and provider errors | A shared model request returns an answer or a typed provider error |
| M2 - MCP Client Integration          | **Chris Li (`@chrislxn`)** | Java MCP client integration with the UC3 catalog and item-reader contracts | Java consumes the catalog and reads one item through MCP |
| M3 - Document Handling               | **TBD** | PDF validation and document handoff | An authorized PDF can be passed to the model |
| M4 - Local Storage & To-Do           | **Yue Tang (`@happy-soup`)**        | Task Entity, task operations, file-based DAO, task tests                                                                 | Tasks can be created/completed and survive restart                  |
| M5 - Swing Frontend                  | **TBD**                             | Shared Swing screens and UI conventions; ViewModels, Controllers/Presenters coordination                                 | Chat view with working submit/status/result flow                    |
| M6 - Academic Features & Integration | **TBD**                             | Announcements/deadlines business rules, app composition and integration support                                          | A runnable deadline/announcement workflow and AppBuilder wiring     |

A **module owner** is responsible for its shared contracts, integration readiness and helping other members use it. The owner does not necessarily write every file in the module. All significant changes should pass through PR review.

## 2. MVP Use Case Leadership

| Use Case                         | Lead                                | Acceptance Evidence                                               |
|----------------------------------|-------------------------------------|-------------------------------------------------------------------|
| UC1 - Manage Local To-Do         | **Yue Tang (`@happy-soup`)**        | Create/list/complete/delete and persisted state                   |
| UC2 - Find and Ask About Canvas Resources | **Chris Li (`@chrislxn`)** | Ask about a Page or PDF, let the agent choose a relevant additional item when needed, and show the answer with citations |
| UC3 - Browse and Read Canvas Course Content | **Rosalind Zhou (`@RosalindZhou`)** | Select a course, browse its catalog in Swing, and click an item to load it. Demonstrate a course with an empty home and content in Modules. |
| UC4 - Save and Reopen Course Resources | **TBD** | Save a source from search results or an answer citation, then reopen or remove it after a restart |
| UC5 - Summarize Announcements    | **Ran Yan (`@Asp-hyxia`)**          | Dated summary with Canvas source links                            |
| UC6 - Generate Deadline Briefing | **TBD**                             | Ordered upcoming deadlines and an informative briefing            |

**Course expectation:** Each member must take the lead on at least one Use Case. The lead should implement most of its relevant Interactor, boundaries, Controller, Presenter, and UI integration, and coordinate any shared dependencies. Module-only contributions do **not** replace this requirement. Assign the remaining leads across the six MVP use cases.

## 3. Shared Interface Decisions

| Interface / contract                | Responsible module     | Consumers                  | Decision status                                    |
|-------------------------------------|------------------------|----------------------------|----------------------------------------------------|
| `AgentGateway`                      | **M1 (Rosalind Zhou)** | UC2, UC5 and UC6 where AI is needed | Agree on Page/PDF input and tool-call responses |
| `McpToolGateway`                    | **M2 (Chris Li)** | UC2, UC3 and academic features | Existing Java transport, new catalog and item-read tools planned |
| `listContent` / `readContent`     | **UC3 (Rosalind Zhou)** supplies Canvas data, **M2 (Chris Li)** integrates the client | Swing catalog and UC2 agent | See [shared content contract](PROJECT_BLUEPRINT.md#shared-course-content-contract) |
| `DocumentGateway`                   | M3                     | UC2, M1                    | Signature TBD                                      |
| `TaskRepository`                    | **M4 (Yue Tang)**      | UC1, optionally UC6        | Signature TBD                                      |
| Shared Swing UI / ViewModel contracts | M5 | UC2, UC3, UC5 and UC6 | Agree on catalog selection, loading, and result states |
| AppBuilder composition              | M6                     | All modules                | Wiring plan TBD                                    |

**Integration contract:** Agree on DTOs, error types, and async execution boundaries before each person implements a concrete adapter. Use fake interfaces to avoid waiting for the real services.

## 4. UC2 and UC3 Handoff

- **Rosalind, UC3.** Own the browsing UI, the Canvas REST adapter, and MCP tool handlers. Build a metadata catalog per selected course from the home, Pages, Files, and Modules. Verify access and retrieve one item's content when the UI or the agent requests it.
- **Chris, UC2.** Own search and Page/PDF question answering in Swing. Use the shared catalog through M2, decide which entries the agent can request, and display citations for retrieved sources.
- **Rosalind, M1.** Supply reusable Gemini requests, tool-call handling, and model errors. Chris defines source selection and reading limits for UC2.
- **Integration.** Pass the selected course and content ID when the student opens the question view from UC3. Develop against a fixture catalog and typed read results while the live Canvas adapter is in progress.

## 5. Development Workflow

1. Use small feature branches from the team's agreed base branch (`test` for current planning; final branch convention TBD).
2. Write down shared interface assumptions before parallel implementation.
3. Open PRs with a scope statement, run instructions, tests, screenshots/demo notes when useful, and any known limitations.
4. Request review from a different team member. Avoid committing directly to `main` for ordinary changes.
5. Update the README and Blueprint when design decisions change.
6. Integrate frequently; every member should be able to run the project from a clean checkout.

### PR checklist

- [ ] Clear Use Case or shared-module objective
- [ ] Relevant Java implementation and/or tests
- [ ] No credentials or private Canvas course files committed
- [ ] Code compiles and tests run
- [ ] UI and API failures are handled
- [ ] Reviewed by at least one teammate
- [ ] Relevant Blueprint/README changes documented

## 6. Immediate Action Items

| Team action | Owner |
| --- | --- |
| Agree on catalog fields, content IDs, pagination, and read errors | Chris and Rosalind |
| Verify catalog browsing and one-item reads in courses with different layouts | Rosalind |
| Test when the UC2 agent should request an additional item and which sources appear in the answer | Chris |

See [Project Blueprint](PROJECT_BLUEPRINT.md) for product scope, Use Cases, architecture and milestone requirements.
