# Shared Entities and Interfaces

This reference covers UC1–UC6. The implementation status was checked against `main` at `7294260` and `UC2` at `4d44e7a` on November 26, 2026. **Main** and **UC2 branch** identify where a Java definition is available. **Planned** identifies designs that still need a shared implementation.

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
| **UC2 · Search and question answering** | Course catalog and whole resources from UC3. The model port receives the question and references. | Streams answer text and attaches citations at completion. Any use case that needs AI can call a shared AI operation. UC5 is the next planned consumer. |
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
| `QuestionModelGateway` | `answer(AnswerPrompt)` → final text. `answer(AnswerPrompt, Consumer<String>)` → optional text increments and the same final text. Both forms throw `CourseQuestionException` on failure. | UC2 branch. Chris owns this port and its DeepSeek adapter. The offline model implements the final-text path. |

Host-specific request and response formats stay in their adapters. The UC2 Interactor owns input validation, selection, read budget, and citation assembly. The Canvas adapter verifies course scope and access for each content read.

### Shared AI calls and streaming

Chris owns the shared AI module in UC2. Any use case that needs AI calls the appropriate Java operation through its own Interactor or adapter. The composition root injects the implementation. Each consumer owns its Controller, Presenter, and UI. The current text operation is grounded question answering through `QuestionModelGateway` and `AnswerPrompt`. Task-specific operations such as announcement analysis use typed inputs and results.

| Call mode | Contract | Use it when |
| --- | --- | --- |
| Final result | Call `answer(prompt)` on a worker. It returns the complete answer or throws a typed failure. The adapter chooses the provider transport. | A caller needs only the finished result or shows a loading state until validation completes. |
| Progressive text | Call `answer(prompt, onAnswerDelta)` on a worker. The callback receives new text only, in order and on the calling worker thread. Preserve spaces and line breaks. The method still returns the complete answer on success. An adapter can return the final result with zero callbacks. | A caller can show incremental text, such as the UC2 question panel. |

The callback has no completion signal. A normal return is the terminal result. A thrown failure ends the request even if some increments were already emitted. Use the returned text to reconcile the displayed preview and avoid appending the full answer twice. UC2 attaches its source links on successful completion. If generation fails after a preview, UC2 replaces it with a retryable error or an invalid-response message. Only a validated final result may be used for downstream handoffs or persistence.

Keep external calls and streaming callbacks off Swing's Event Dispatch Thread (EDT). Keep callbacks quick so they do not delay stream reads. In UC2, the Presenter buffers increments and a Swing timer appends the accumulated text about every 50 ms. Keep each EDT update limited to the new text. On success or failure, stop periodic updates, apply the terminal state, and restore the request controls. Other Swing views can use the same batching pattern.

DeepSeek stream framing and parsing stay in the adapter. It emits visible answer text, checks the provider's finish signal, and maps HTTP errors and timeouts to `MODEL_UNAVAILABLE` and malformed or truncated responses to `INVALID_RESPONSE`. Callers handle Java results and errors through their own boundaries. The legacy `AgentGateway` on main still provides final Gemini answers.

**UC5 integration.** The planned announcement analysis operation receives one announcement and returns a validated summary and zero or more structured deadline candidates. UC5 calls it once, displays the result through its own Presenter, and persists validated deadline evidence for UC6. Final-result mode is sufficient for this flow. If UC5 later displays a streamed preview, stream summary text only and process structured candidates from the final result.

### Announcements, deadlines, and local storage

| Contract | Request → result | Status and ownership |
| --- | --- | --- |
| `AnnouncementGateway` | `listAnnouncements(courseId, startDate, endDate)` → authorized `List<Announcement>` for an inclusive date range. | Main. UC5 port. The dashboard groups results by configured course. |
| `AnnouncementSummarizer` | `summarize(List<Announcement>)` → summary text. | Main. Current UC5 code contract. |
| Shared announcement analysis | Analyze one selected announcement → final summary plus zero or more deadline candidates with original wording. UC5 validates the evidence and passes the summary to its own Presenter. | Planned. Chris supplies the AI operation with Ran, who leads UC5. See [shared AI calls and streaming](#shared-ai-calls-and-streaming). |
| `DeadlineGateway` | Read Canvas assignment due dates for the selected course or date range. | Empty Java interface on main. Its concrete signature is planned. |
| `DeadlineRepository` | Record announcement evidence from UC5 and assignment evidence from UC6, then return the merged timeline by course and date. Preserve stable deadline IDs and conflicts. | Planned. UC5 and UC6 share the store. |
| UC1 create-task boundary | Receive deadline ID, title, due value, and source link from UC6 → created or already existing. | Planned. UC1 validates the input and owns persistence. |
| `TaskRepository` | Save, find, list, and delete personal tasks. Look up imported tasks by deadline ID for repeated button clicks. | Planned. UC1 owns the repository. |
| UC4 saved-source boundary and repository | Save title and canonical URL → saved state. List or remove saved links. | Planned. UC4 owns the local store and Saved view. |

## Behavior to preserve at the boundaries

- Return an empty list for a successful empty lookup. Carry coverage warnings with partial results and return a typed failure for permission, service, or invalid-response errors.
- Give each UC its own input boundary and Presenter. A use case returns application data to its caller or output boundary. The caller's Presenter owns its UI updates.
- Test each handoff with fixtures. For example, UC3 can fake a complete Page for UC2. UC5 can fake an AI result containing two dated candidates. UC6 can use a fake task boundary to verify the deadline ID delivered to UC1.
- For a streaming consumer, test ordered increments before completion, completion with zero increments, and failure after partial text. Verify that the UI finishes and restores its controls in each case.

See the [UC3 browsing design](UC3_CANVAS_BROWSING.md) for catalog navigation, and the [academic workflows](ACADEMIC_WORKFLOWS.md) for deadline evidence, conflict handling, and task creation.
