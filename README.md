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
| US5 | Summarize an announcement and identify the deadlines it mentions | Act on updates posted in individual announcements and weekly briefings |
| US6 | View upcoming deadlines from assignments and announcements and add them to my To-Do list | Plan and track my work from one timeline |

## Use Cases and MVP

All use cases have a student as the actor and begin with an action in the application. Canvas features require authorized course access; AI features also require a configured model service. On failure, the application displays an error and allows the student to retry.

| Use case                         | Trigger / input                    | Main flow and successful outcome                                          | Alternative / failure                                        | Scope | Lead                            |
|----------------------------------|------------------------------------|---------------------------------------------------------------------------|--------------------------------------------------------------|-------|---------------------------------|
| UC1 - Manage To-Do | Choose a task action in To-Do or click **Add to To-Do** in UC6 | Manage and persist local tasks. A deadline adds one task with its date and source. | Invalid task input or storage failure | MVP | Yue Tang (`@happy-soup`) |
| UC2 - Find and Ask About Canvas Resources | Search a course or ask a question with optional Page or PDF references | Search the shared catalog. Use configured defaults and selected sources, and let the agent choose other relevant items when needed. Show the answer with links to the sources used. | No matches, inaccessible source, insufficient evidence, or model failure | MVP | Chris Li (`@chrislxn`) |
| UC3 - Browse and Read Canvas Course Content | Add or open a course, then browse its home and resources | Preview a custom HTML home or show native Modules. Expand modules on demand. Continue supported Page links in the preview and open files or module resources in Canvas. | Empty or partial catalog, inaccessible item, or Canvas failure | MVP | Rosalind Zhou (`@RosalindZhou`) |
| UC4 - Save and Reopen Course Resources | Save a UC3 catalog item or a UC2 answer citation | Store the title and Canvas link. Reopen or remove the saved link after restart. | Invalid link or storage failure | MVP | Sophia Yang (`@sophiaysh16-spec`) |
| UC5 - Summarize Announcements | Click **Summarize** on one announcement in a course group | Show the summary on that row and add extracted deadline evidence to the shared repository. | No announcements, analysis failure, or deadline save failure | MVP | Ran Yan (`@Asp-hyxia`) |
| UC6 - Generate Deadline Briefing | View the deadline timeline and choose a date range | Merge assignment due dates and UC5 evidence, show sources and date conflicts, and let the student add a dated item to UC1. | No deadlines, partial retrieval, or conflicting dates | MVP | TBD |

UC3 supplies the shared course catalog and item reader used by Swing and the UC2 agent. The MVP catalogs Pages and PDFs from the course home, directories, and Modules. Students can select whole resources as references from the same window. See the [UC3 browsing and navigation design](docs/UC3_CANVAS_BROWSING.md) and [shared content contract](PROJECT_BLUEPRINT.md#shared-course-content-contract).

The Dashboard groups announcements by course and shows a shared deadline timeline from Canvas assignments and processed announcements. UC1 saves tasks selected from the timeline. See the [announcement, deadline, saved-source, and To-Do workflows](docs/ACADEMIC_WORKFLOWS.md).

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

### UC2 offline Page question demo

Run `app.Main` in IntelliJ with `--uc2-demo` in **Program arguments**. Enter **When is the project demo?** and select the Project Timeline or use the course default, then click **Send**. Swing displays the simulated answer followed by the title and example Canvas URL of the Page it read.

The UC2 Interactor depends on two Java interfaces. `CourseContentGateway` lists resources and reads a whole Page by course and content ID. `QuestionModelGateway` accepts an `AnswerPrompt` and returns the answer. The demo supplies both implementations locally and needs no credentials. The demo citation points to an example Canvas domain.

### Live prototype

For a live demo, import `pom.xml` in IntelliJ and run `app.Main`. Add these environment variables to its run configuration.

| Variable         | Value                                            |
|------------------|--------------------------------------------------|
| `CANVAS_ORIGIN`  | Your Canvas origin, e.g. `https://q.utoronto.ca` |
| `CANVAS_TOKEN`   | Your local Canvas access token                   |
| `GEMINI_API_KEY` | Your local Gemini API key                        |
| `GEMINI_MODEL`   | Optional; defaults to `gemini-3.1-flash-lite`    |

In the current prototype, select a course, search resources, select a Page, then choose **Ask about selected page**. If the directory is unavailable, enter a known Page slug (e.g. `project-timeline-2`) and choose **Use page slug**. Ask a question and check the answer and Canvas source link. Credentials belong in local configuration.
