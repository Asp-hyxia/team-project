# CSC207 Team Project - Team Work Split

> **Status:** Draft for team approval.  
> **Last revised:** November 25, 2026.
>
> **Work split:** Rosalind Zhou leads M1 and UC3. Chris Li leads M2 and UC2. Yue Tang leads M4 and UC1, Sophia Yang leads UC4, Ran Yan leads UC5, and Anqi Feng leads UC6. Remaining module owners are TBD.

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
| UC1 - Manage Local To-Do         | **Yue Tang (`@happy-soup`)**        | Manage and persist tasks. A deadline sent from UC6 creates one task with its date and source. |
| UC2 - Find and Ask About Canvas Resources | **Chris Li (`@chrislxn`)** | Ask about a Page or PDF and show cited answers. Provide the reusable AI analysis entry point for UC5. |
| UC3 - Browse and Read Canvas Course Content | **Rosalind Zhou (`@RosalindZhou`)** | Open a course, follow linked Pages within the HTML preview, and demonstrate native Modules expansion and browser opening in a Modules-based course. |
| UC4 - Save and Reopen Course Resources | **Sophia Yang (`@sophiaysh16-spec`)** | Save a source from search results or an answer citation, then reopen or remove it after a restart |
| UC5 - Summarize Announcements    | **Ran Yan (`@Asp-hyxia`)**          | Summarize one announcement in its course group and publish its sourced deadlines to the shared repository. |
| UC6 - Generate Deadline Briefing | **Anqi Feng (`@fbgfaqzxm1003`)**    | Display an ordered timeline from assignments and announcements, flag conflicts, and add one selected item to UC1. |

**Course expectation:** Each member must take the lead on at least one Use Case. The lead should implement most of its relevant Interactor, boundaries, Controller, Presenter, and UI integration, and coordinate any shared dependencies. Module-only contributions do **not** replace this requirement.

## 3. Shared Interface Decisions

Use [Shared Entities and Interfaces](docs/SHARED_ENTITIES_AND_INTERFACES.md) to check the current Java signatures and planned handoffs before implementing a shared adapter.

| Interface / contract                | Responsible module     | Consumers                  | Decision status                                    |
|-------------------------------------|------------------------|----------------------------|----------------------------------------------------|
| `AgentGateway`                      | **M1 (Rosalind Zhou)** | Original single-Page Gemini flow | Existing final-answer model port |
| Shared AI operations | **UC2 (Chris Li)** | Any use case that needs AI | `QuestionModelGateway` supports final text and optional increments. See the [shared AI interface](docs/SHARED_ENTITIES_AND_INTERFACES.md#shared-ai-calls-and-streaming). |
| Announcement analysis | **Chris Li** with **UC5 (Ran Yan)** | UC5 | Planned typed final result with a summary and deadline candidates |
| `McpToolGateway`                    | **M2 (Chris Li)** | UC2, UC3 and academic features | Existing Java transport, new catalog and item-read tools planned |
| `listContent` / `readContent`     | **UC3 (Rosalind Zhou)** supplies Canvas data, **M2 (Chris Li)** integrates the client | Swing catalog and UC2 agent | See [shared content contract](PROJECT_BLUEPRINT.md#shared-course-content-contract) |
| `DocumentGateway`                   | M3                     | UC2, M1                    | Signature TBD                                      |
| `DeadlineRepository` | **UC6 (Anqi Feng)** with **UC5 (Ran Yan)** | UC5 and UC6 | One local store for assignment and announcement evidence |
| UC1 create-task boundary | **UC1 (Yue Tang)** | UC1 and UC6 | Receive a deadline ID, due value, title, and source from UC6 |
| `TaskRepository`                    | **M4 (Yue Tang)**      | UC1 | Persist personal tasks and identify imports by deadline ID |
| Shared Swing UI / ViewModel contracts | M5 | UC2, UC3, UC5 and UC6 | Agree on catalog selection, loading, and result states |
| AppBuilder composition              | M6                     | All modules                | Wiring plan TBD                                    |

**Integration contract:** Agree on DTOs, error types, and async execution boundaries before each person implements a concrete adapter. Use fake interfaces to avoid waiting for the real services.

## 4. Use Case Handoffs

- **Rosalind, UC3.** Own course setup, the browsing UI, the Canvas REST adapter, and MCP tool handlers. Build the shared catalog, preview Pages as HTML, and use native Modules with browser opens for module resources. Verify access on each content request. See the [UC3 browsing and navigation design](docs/UC3_CANVAS_BROWSING.md).
- **Chris, UC2.** Own search and Page/PDF question answering in Swing and the shared AI module. Use the shared catalog through M2, decide which entries the agent can request, and display streamed answers and citations.
- **Rosalind, M1.** Maintain the Gemini requests, tool-call handling, and model errors for the original Page flow. Chris defines source selection and reading limits for the revised UC2.
- **Integration.** Use the course ID, shared catalog, configured defaults, and selected reference IDs for questions in the same Swing window. Develop against a fixture catalog and typed read results while the live Canvas adapter is in progress.

- **Sophia, UC4.** Receive a title and source link from the course catalog or an answer citation. Own the Saved view and local link persistence.
- **UC5 → UC6.** Ran owns course-grouped announcement cards, invokes Chris's shared AI analysis interface, and presents the result in UC5. UC5 publishes sourced deadline evidence. UC6 reconciles it with Canvas assignment dates and owns the timeline UI.
- **UC6 → UC1.** The deadline row sends a create-task request when the student clicks **Add to To-Do**. Yue owns task creation, persistence, and the To-Do view.

See [academic workflows](docs/ACADEMIC_WORKFLOWS.md) for handoff fields and acceptance examples.

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
| Agree on announcement analysis output and the shared deadline evidence contract | Chris, Ran, and the UC6 lead |
| Agree on the deadline-to-task input and test repeated clicks and restart | Yue and the UC6 lead |

See [Project Blueprint](PROJECT_BLUEPRINT.md) for product scope, Use Cases, architecture and milestone requirements.
