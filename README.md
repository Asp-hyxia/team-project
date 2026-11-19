# Canvas Academic Assistant

## Project Domain

Education / Productivity. A Java Swing application that helps students find Canvas resources, ask questions about course materials with source links, and manage a persistent local To-Do list.

## User Stories

| ID | As a student, I want to... | So that I can... |
| --- | --- | --- |
| US1 | Manage personal study tasks | Keep track of my work |
| US2 | Find Canvas Pages and files | Locate course resources quickly |
| US3 | Ask questions about a Canvas Page | Understand course instructions |
| US4 | Ask questions about a Canvas PDF | Understand course materials without manually uploading them |
| US5 | Get summaries of recent announcements | Identify important course updates |
| US6 | View upcoming deadlines in date order | Prioritize my work |

## Use Cases and MVP

All use cases have a student as the actor and begin with an action in the application. Canvas features require authorized course access; AI features also require a configured model service. On failure, the application displays an error and allows the student to retry.

| Use case | Trigger / input | Main flow and successful outcome | Alternative / failure | Scope | Lead |
| --- | --- | --- | --- | --- | --- |
| UC1 - Manage To-Do | Choose a task action | Create, list, complete, reopen or delete tasks; save changes locally | Invalid task input or storage failure | MVP | TBD |
| UC2 - Find Canvas Resources | Select a course and enter a search | Retrieve and filter metadata; display matching resources and source links | No matches or access/service failure | MVP | Chris Li (`@chrislxn`) |
| UC3 - Ask About Canvas Pages | Ask a question about a Page | Retrieve Page content; generate an answer and display its source | Inaccessible Page, insufficient information or model failure | MVP | Rosalind Zhou (`@RosalindZhou`) |
| UC4 - Ask About Canvas PDFs | Ask a question about a course PDF | Retrieve an accessible PDF; generate an answer and display its source | Unsupported/inaccessible file or model failure | MVP | TBD |
| UC5 - Summarize Announcements | Select courses and a time window | Retrieve announcements; display a dated summary with source links | No announcements or access/service failure | MVP | TBD |
| UC6 - Generate Deadline Briefing | Select a date range | Retrieve assignments; display deadlines in date order with sources | No deadlines or access/service failure | MVP | TBD |

Each of the six members must lead at least one MVP use case. UC1–UC6 are in scope. Remaining leads need team confirmation. See [team work split](TEAM_WORK_SPLIT.md).

## Domain Entities

| Entity | Instance variables | Relevant methods |
| --- | --- | --- |
| `Course` | `id`, `name`, `code` | `getId()`, `getName()`, `getCode()` |
| `CourseResource` | `id`, `courseId`, `title`, `type` (Page/File), `sourceUrl` | `getId()`, `getCourseId()`, `getTitle()`, `getType()`, `getSourceUrl()` |

## API Usage

| API | Purpose | Successful live call? |
| --- | --- | --- |
| [Canvas REST API](https://developerdocs.instructure.com/services/canvas) | Read authorized course resources through MCP | Yes - course listing and Page reading verified |
| [Gemini API](https://ai.google.dev/api/generate-content) | Answer questions using retrieved course content | Yes - verified with `gemini-3.1-flash-lite` |


## Run and Test

Requires JDK 11+ and Maven. Run `mvn test` for offline tests; API keys are unnecessary for these tests.

For a live demo, import `pom.xml` in IntelliJ and run `app.Main`. Add these environment variables to its run configuration.

| Variable | Value |
| --- | --- |
| `CANVAS_ORIGIN` | Your Canvas origin, e.g. `https://q.utoronto.ca` |
| `CANVAS_TOKEN` | Your local Canvas access token |
| `GEMINI_API_KEY` | Your local Gemini API key |
| `GEMINI_MODEL` | Optional; defaults to `gemini-3.1-flash-lite` |

Select a course, search resources, select a Page, then choose **Ask about selected page**. If the directory is unavailable, enter a known Page slug (e.g. `project-timeline-2`) and choose **Use page slug**. Ask a question and check the answer and Canvas source link. Credentials belong in local configuration.
