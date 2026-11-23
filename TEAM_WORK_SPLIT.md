# CSC207 Team Project - Team Work Split

> **Status:** Draft for team approval.  
> **Last revised:** November 18, 2026.  
> **Rule:** M1 / UC3 are claimed by Rosalind Zhou; M2 / UC2 are owned by Chris Li; M4 / UC1 are claimed by Yue Tang. All remaining assignments are TBD until accepted by the team.
## 1. Module Ownership

| Module                               | Owner                               | Responsibilities                                                                                                         | First Deliverable                                                   |
|--------------------------------------|-------------------------------------|--------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------|
| M1 - AI Agent Core                   | **Rosalind Zhou (`@RosalindZhou`)** | LLM API adapter, prompt/tool loop, approved tool selection, provider errors                                              | Fake-tool call works from Java, then connect to MCP                 |
| M2 - MCP & Canvas Integration        | **Chris Li (`@chrislxn`)**          | Java MCP Client, integration with existing Python Canvas MCP Server, Canvas read-only tools and typed resource discovery | Java calls a live Canvas tool via MCP and returns a resource result |
| M3 - Document Handling               | **TBD**                             | Canvas file retrieval, optional on-demand cache, PDF input to selected model                                             | One accessible PDF can be passed to the model                       |
| M4 - Local Storage & To-Do           | **Yue Tang (`@happy-soup`)**        | Task Entity, task operations, file-based DAO, task tests                                                                 | Tasks can be created/completed and survive restart                  |
| M5 - Swing Frontend                  | **TBD**                             | Shared Swing screens and UI conventions; ViewModels, Controllers/Presenters coordination                                 | Chat view with working submit/status/result flow                    |
| M6 - Academic Features & Integration | **TBD**                             | Announcements/deadlines business rules, app composition and integration support                                          | A runnable deadline/announcement workflow and AppBuilder wiring     |

A **module owner** is responsible for its shared contracts, integration readiness and helping other members use it. The owner does not necessarily write every file in the module. All significant changes should pass through PR review.

## 2. MVP Use Case Leadership

| Use Case                         | Lead                                | Acceptance Evidence                                               |
|----------------------------------|-------------------------------------|-------------------------------------------------------------------|
| UC1 - Manage Local To-Do         | **Yue Tang (`@happy-soup`)**        | Create/list/complete/delete and persisted state                   |
| UC2 - Find Canvas Resources      | **Chris Li (`@chrislxn`)**          | Resource discovery through MCP; display titles/types/source links |
| UC3 - Ask About Canvas Pages     | **Rosalind Zhou (`@RosalindZhou`)** | Agent reads a Page and answers with a source link                 |
| UC4 - Ask About Canvas PDFs      | **TBD**                             | Agent obtains a permitted PDF and answers with its source         |
| UC5 - Summarize Announcements    | **TBD**                             | Dated summary with Canvas source links                            |
| UC6 - Generate Deadline Briefing | **TBD**                             | Ordered upcoming deadlines and an informative briefing            |

**Course expectation:** Each member must take the lead on at least one Use Case. The lead should implement most of its relevant Interactor, boundaries, Controller, Presenter, and UI integration, and coordinate any shared dependencies. Module-only contributions do **not** replace this requirement. Assign the remaining leads across the six MVP use cases.

## 3. Shared Interface Decisions

| Interface / contract                | Responsible module     | Consumers                  | Decision status                                    |
|-------------------------------------|------------------------|----------------------------|----------------------------------------------------|
| `AgentGateway`                      | **M1 (Rosalind Zhou)** | UC2-UC6 where AI is needed | Signature TBD                                      |
| `McpToolGateway`                    | **M2 (Chris Li)**      | M1, M3, academic features  | Actual Python server tools/transport to be audited |
| `DocumentGateway`                   | M3                     | UC4, M1                    | Signature TBD                                      |
| `TaskRepository`                    | **M4 (Yue Tang)**      | UC1, optionally UC6        | Signature TBD                                      |
| Shared Chat UI / ViewModel contract | M5                     | UC2-UC6                    | Signature TBD                                      |
| AppBuilder composition              | M6                     | All modules                | Wiring plan TBD                                    |

**Integration contract:** Agree on DTOs, error types, and async execution boundaries before each person implements a concrete adapter. Use fake interfaces to avoid waiting for the real services.

## 4. Module 2: Confirmed Owner's First PR

**Owner:** Chris Li (`@chrislxn`)  
**Module:** MCP & Canvas Integration  
**Use Case Lead:** UC2 - Find Canvas Resources

### Deliverables

- [ ] Document the Python Canvas MCP Server's actual transport, commands, tool names and required arguments (without secrets).
- [ ] Implement a Java-facing MCP gateway and concrete adapter; avoid exposing MCP SDK types in Use Case interfaces.
- [ ] Demonstrate that the Java application can invoke at least one live, read-only Canvas MCP tool.
- [ ] Provide resource-discovery output with title, resource type, course and source URL where available.
- [ ] Implement or coordinate UC2's Controller, Interactor, Presenter and Swing integration.
- [ ] Include tests for happy path, empty results and connection/tool failures using fakes.
- [ ] Submit a substantial code PR and review one teammate's PR by **November 20**.

**Existing Python server note:** Previously developed code may be reused as infrastructure, but the team's fresh Java integration, tests, documentation, review, and demonstrable Use Case contributions should be visible in Git history. Confirm with the TA if any course-specific attribution questions arise.

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

| Deadline | Team action                                                                                     | Owner                             |
|----------|-------------------------------------------------------------------------------------------------|-----------------------------------|
| Nov 17   | Confirm scope, actual team size and remaining ownership                                         | **TBD**                           |
| Nov 18   | Freeze minimal shared interface contracts and validate tool access                              | **M2: Chris Li; others TBD**      |
| Nov 19   | Integrate the first Swing-to-Canvas AI vertical slice                                           | **TBD**                           |
| Nov 20   | Demonstrate one end-to-end Use Case, external API calls, individual substantial PRs and reviews | All members (specific owners TBD) |

See [Project Blueprint](PROJECT_BLUEPRINT.md) for product scope, Use Cases, architecture and milestone requirements.
