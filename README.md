# Canvas Academic Assistant

## Project Domain

Education / Productivity. A Java Swing application that helps students find Canvas resources, ask questions about course materials with source links, and manage a persistent local To-Do list.

## User Stories

| ID  | As a student, I want to...            | So that I can...                                            |
|-----|---------------------------------------|-------------------------------------------------------------|
| US1 | Manage personal study tasks           | Keep track of my work                                       |
| US2 | Search Canvas resources and ask about Pages or PDFs | Get answers with links to the course material used |
| US3 | Browse a course and open its content | Find materials across the course home, Pages, Modules, and Files |
| US4 | Save and reopen useful course resources | Return to course sources without repeating a search |
| US5 | Get summaries of recent announcements | Identify important course updates                           |
| US6 | View upcoming deadlines in date order | Prioritize my work                                          |

## Use Cases and MVP

All use cases have a student as the actor and begin with an action in the application. Canvas features require authorized course access; AI features also require a configured model service. On failure, the application displays an error and allows the student to retry.

| Use case                         | Trigger / input                    | Main flow and successful outcome                                          | Alternative / failure                                        | Scope | Lead                            |
|----------------------------------|------------------------------------|---------------------------------------------------------------------------|--------------------------------------------------------------|-------|---------------------------------|
| UC1 - Manage To-Do               | Choose a task action               | Create, list, complete, reopen or delete tasks; save changes locally      | Invalid task input or storage failure                        | MVP   | Yue Tang (`@happy-soup`)        |
| UC2 - Find and Ask About Canvas Resources | Search a course or ask about a selected Page or PDF | Search the shared course catalog. The agent reads the selected source and can use the catalog to choose additional sources. Show the answer with links to the sources used. | No matches, inaccessible source, insufficient evidence, or model failure | MVP | Chris Li (`@chrislxn`) |
| UC3 - Browse and Read Canvas Course Content | Select a course, then a catalog item | Show a clickable catalog with titles, types, module locations, and source links. Retrieve and display or open the selected source on demand. | Empty or partial catalog, inaccessible item, or Canvas failure | MVP | Rosalind Zhou (`@RosalindZhou`) |
| UC4 - Save and Reopen Course Resources | Save a source from a search result or answer citation | List saved links and let students reopen or remove them after a restart | Invalid link or storage failure | MVP | TBD |
| UC5 - Summarize Announcements    | Select courses and a time window   | Retrieve announcements; display a dated summary with source links         | No announcements or access/service failure                   | MVP   | Ran Yan (@Asp-hyxia)            |
| UC6 - Generate Deadline Briefing | Select a date range                | Retrieve assignments; display deadlines in date order with sources        | No deadlines or access/service failure                       | MVP   | TBD                             |

UC3 provides the shared course catalog and item reader used by Swing and the UC2 agent. The MVP catalogs Pages and PDFs from the course home, directories, and Modules. The catalog contains metadata, and the app reads content by item ID when a student clicks or the agent requests it. See the [shared content contract](PROJECT_BLUEPRINT.md#shared-course-content-contract).

Each of the six members must lead at least one MVP use case. UC1–UC6 are in scope. Remaining leads need team confirmation. See [team work split](TEAM_WORK_SPLIT.md).

## Domain Entities

| Entity           | Instance variables                                         | Relevant methods                                                        |
|------------------|------------------------------------------------------------|-------------------------------------------------------------------------|
| `Course`         | `id`, `name`, `code`                                       | `getId()`, `getName()`, `getCode()`                                     |
| `CourseResource` | `id`, `courseId`, `title`, `type` (Page/File), `sourceUrl` | `getId()`, `getCourseId()`, `getTitle()`, `getType()`, `getSourceUrl()` |

## API Usage

| API                                                                      | Purpose                                         | Successful live call?                          |
|--------------------------------------------------------------------------|-------------------------------------------------|------------------------------------------------|
| [Canvas REST API](https://developerdocs.instructure.com/services/canvas) | Read authorized course resources through MCP    | Yes - course listing and Page reading verified |
| [Gemini API](https://ai.google.dev/api/generate-content)                 | Answer questions using retrieved course content | Yes - verified with `gemini-3.1-flash-lite`    |


## Run and Test

Requires JDK 11+ and Maven. Run `mvn test` for offline tests; API keys are unnecessary for these tests.

For a live demo, import `pom.xml` in IntelliJ and run `app.Main`. Add these environment variables to its run configuration.

| Variable         | Value                                            |
|------------------|--------------------------------------------------|
| `CANVAS_ORIGIN`  | Your Canvas origin, e.g. `https://q.utoronto.ca` |
| `CANVAS_TOKEN`   | Your local Canvas access token                   |
| `GEMINI_API_KEY` | Your local Gemini API key                        |
| `GEMINI_MODEL`   | Optional; defaults to `gemini-3.1-flash-lite`    |

In the current prototype, select a course, search resources, select a Page, then choose **Ask about selected page**. If the directory is unavailable, enter a known Page slug (e.g. `project-timeline-2`) and choose **Use page slug**. Ask a question and check the answer and Canvas source link. Credentials belong in local configuration.
