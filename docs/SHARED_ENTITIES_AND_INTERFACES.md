# Shared Entities and Interfaces

This reference covers UC1–UC6. The implementation status was checked against `main` at `7294260` and `UC2` at `76acd15` on November 26, 2026. **Main** means a Java definition is present on `main`. **UC2 branch** means the definition is available in the offline UC2 slice. **Planned** means the team has a design to agree on before writing a shared implementation.

## Shared data

Entities and value objects describe application data. The ports below exchange these objects or small request and response DTOs. Swing components, Canvas JSON, model payloads, and credentials stay in the adapters that handle them.

| Data | Fields and rules | Used by | Status |
| --- | --- | --- | --- |
| `Course` | `id`, `name`, `code`. The Canvas course ID is positive and scoped to the configured connection. | UC2, UC3, UC5, UC6 | Main |
| `CourseResource` | `id`, `courseId`, `title`, `type`, `sourceUrl`. Current types are `PAGE` and `FILE`. Module and homepage locations are planned catalog metadata. | UC2, UC3, UC4 | Main |
| `ResourceListing` | Resource metadata and warnings about partial coverage. | UC2, UC3 | Main, use-case output data |
| `PageContent` / `CourseContentGateway.Page` | An authorized Page and its text. The former carries course ID, page slug, title, text, and source. The latter pairs a `CourseResource` with text. | UC2, UC3 | `PageContent` in main, `Page` in UC2 branch |
| Authorized PDF content | Whole document payload with resource ID, media type, title, and canonical source. Confirm payload representation and limits before implementing the MCP or model adapters. | UC2, UC3 | Planned |
| `SourceReference` | Title and canonical Canvas URL. Reuse for answer citations and the save action. | UC2, UC3, UC4 | Planned shared value object. UC2 branch already presents equivalent citation fields. |
| `SavedResource` | Local ID, title, and canonical source URL. Repeated saves of the same URL keep one entry. | UC4 | Planned |
| `Announcement` | `id`, `courseId`, `courseName`, `time`, `title`, `content`, `sourceUrl`. `time` is the posting timestamp. | UC3's Canvas adapter, UC5, shared AI analysis | Main |
| `AcademicDeadline` | Current fields are `id`, `courseId`, `title`, `dueAt`, `sourceUrl`. The current class requires a precise `Instant` and one URL. The merged timeline design needs date-only values, review states, and evidence from multiple sources. | UC5, UC6 | Main for the current entity. Multi-source representation is planned. |
| `DeadlineEvidence` | Source kind, source ID and URL, reported due value, and supporting wording for an announcement. Retain each source when dates conflict. | UC5, UC6 | Planned |
| `Task` | Local ID, title, completion state, and optional due date or time. An imported deadline also carries its stable `deadlineId` and source link. | UC1, UC6 | Planned |

Use a stable resource ID within each course. When Pages and files can share a raw Canvas identifier, the catalog resolves them by type and ID. Retain multiple navigation locations on one catalog entry. Source links identify the original Canvas item and omit tokens and temporary download URLs.

## Who calls whom

| Use case | Inputs from other use cases or shared services | Output or handoff |
| --- | --- | --- |
| **UC1 · Local To-Do** | UC6 can submit a deadline to its create-task boundary. Direct task entry also works locally. | Creation or already-existing result for UC6. |
| **UC2 · Search and question answering** | Course catalog and whole resources from UC3. The model port receives the question and references. | Answer citations can go to UC4. The shared AI analysis entry point also serves UC5 independently of the question UI. |
| **UC3 · Canvas browsing** | Authenticated Canvas reads through its adapter. | Course catalog and item reads for UC2, source links for UC4, and the shared Canvas connection for UC5 and UC6. |
| **UC4 · Saved resources** | Title and original link from UC3 browsing or a UC2 citation. | Saved link and save result for its UI. |
| **UC5 · Announcement summaries** | Authorized announcement body via the Canvas adapter, and analysis from the shared AI entry point. | Validated announcement deadline evidence in the same repository UC6 reads. |
| **UC6 · Deadline briefing** | Canvas assignment due dates and announcement evidence from UC5. | Selected deadline sent to the UC1 create-task boundary. |

## Java interface contracts

### Canvas content and AI

| Contract | Request → result | Status and ownership |
| --- | --- | --- |
| `ResourceGateway` | `listCourses()` → `List<Course>`. `listResources(courseId)` → `ResourceListing`. | Main. Existing metadata discovery. |
| `PageGateway` | `readPage(courseId, pageSlug)` → authorized `PageContent`. | Main. Existing Page reader. |
| `McpToolGateway` | `listTools()` and `callTool(name, arguments)` exchange tool metadata and payloads. | Main. M2 integrates the Java client. The Canvas server currently exposes `list_courses`, `list_resources`, and `read_page`. |
| `CourseContentGateway` | `listContent(courseId)` → `ResourceListing`. `readContent(courseId, contentId)` → `Page`. | UC2 branch. Offline adapter currently implements the Page path. UC3 and M2 still need to agree on the live catalog and PDF payload. |
| Shared live content contract | `listContent(courseId, parentId?, cursor?)` returns resource metadata, coverage warnings, and any continuation information. `readContent(courseId, contentId)` reads one authorized whole Page or PDF. | Planned. UC3 supplies the server-side reads and M2 maps them to the client port. |
| `AgentGateway` | `answer(question, PageContent)` → answer text. | Main. Existing single-Page Gemini path. Rosalind leads M1. |
| `QuestionModelGateway` | `answer(AnswerPrompt)` → answer text. The prompt separates the system instruction, question, and reference text. | UC2 branch. The demo uses a simulated model. DeepSeek and model-requested follow-up reads are planned adapters and extensions. |

Host-specific request and response formats stay in their adapters. The UC2 Interactor owns input validation, selection, read budget, and citation assembly. The Canvas adapter verifies course scope and access for each content read.

### Announcements, deadlines, and local storage

| Contract | Request → result | Status and ownership |
| --- | --- | --- |
| `AnnouncementGateway` | `listAnnouncements(courseId, startDate, endDate)` → authorized `List<Announcement>` for an inclusive date range. | Main. UC5 port. The dashboard groups results by configured course. |
| `AnnouncementSummarizer` | `summarize(List<Announcement>)` → summary text. | Main. Current UC5 code contract. |
| Shared announcement analysis | Analyze one selected announcement → summary plus zero or more deadline candidates with original wording. UC5 validates the evidence and passes the summary to its own Presenter. | Planned. Chris supplies the AI entry point with Ran, who leads UC5. |
| `DeadlineGateway` | Read Canvas assignment due dates for the selected course or date range. | Empty Java interface on main. Its concrete signature is planned. |
| `DeadlineRepository` | Record announcement evidence from UC5 and assignment evidence from UC6, then return the merged timeline by course and date. Preserve stable deadline IDs and conflicts. | Planned. UC5 and UC6 share the store. |
| UC1 create-task boundary | Receive deadline ID, title, due value, and source link from UC6 → created or already existing. | Planned. UC1 validates the input and owns persistence. |
| `TaskRepository` | Save, find, list, and delete personal tasks. Look up imported tasks by deadline ID for repeated button clicks. | Planned. UC1 owns the repository. |
| UC4 saved-source boundary and repository | Save title and canonical URL → saved state. List or remove saved links. | Planned. UC4 owns the local store and Saved view. |

## Behavior to preserve at the boundaries

- Return an empty list for a successful empty lookup. Carry coverage warnings with partial results and return a typed failure for permission, service, or invalid-response errors.
- Give each UC its own input boundary and Presenter. A use case returns application data to its caller or output boundary. The caller's Presenter owns its UI updates.
- Test each handoff with fixtures. For example, UC3 can fake a complete Page for UC2. UC5 can fake an AI result containing two dated candidates. UC6 can use a fake task boundary to verify the deadline ID delivered to UC1.

See the [UC3 browsing design](UC3_CANVAS_BROWSING.md) for catalog navigation, and the [academic workflows](ACADEMIC_WORKFLOWS.md) for deadline evidence, conflict handling, and task creation.
