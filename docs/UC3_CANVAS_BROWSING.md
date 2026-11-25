# UC3 — Browse and Read Canvas Course Content

**Owner** Rosalind Zhou. **Status** Planned design. The current prototype lists Pages and files and reads a Page by slug.

UC3 lets a student add or open a Canvas course, browse its available material, and open a source either in the app or in Canvas. It also supplies the metadata catalog and item reader used by UC2. The course view, reference sidebar, and question panel share one Swing window. UC3 handles navigation and reading. UC2 handles questions and answers.

## 1. Add or open a course

1. In **Add course**, accept a Canvas course URL such as `https://q.utoronto.ca/courses/123` and a Canvas API token for that connection. Extract the Canvas origin and numeric course ID from the link. An existing connection for that Canvas origin can be reused.
2. The Canvas adapter requests `GET /api/v1/courses/{courseId}` with the token to verify access and obtain the course name and `default_view`. If the URL is invalid or Canvas denies access, show that outcome in the add-course form.
3. Open the course in the upper part of the main window. Load the appropriate home view, then make other accessible resources available in the shared catalog. Keep the question panel and selected references visible below or alongside the browser.
4. When reopening a configured course, reuse its course ID and connection. On a course switch, load the new course's catalog and active reference selection.

The Canvas adapter owns credentials and authenticated requests. The catalog, Swing ViewModel, MCP payloads, and model inputs carry resource metadata and reading results. Store connection credentials separately from the course metadata and omit them from URLs and logs.

## 2. Choose the course home view

| Canvas home type | First request after course metadata | Swing display |
| --- | --- | --- |
| `wiki` · custom home Page, as in the CSC207 example | `GET /api/v1/courses/{courseId}/front_page` | Read-only HTML preview with **Open original in Canvas** |
| `modules` · Modules home, as in the MAT235 lecture example | `GET /api/v1/courses/{courseId}/modules` | Native collapsible list in Canvas module order |
| Other home type, missing home, or empty home | Discover available Pages, Files, and Modules | Standard course catalog with the course's original Canvas link. A home that contains links remains useful even if it has little prose. |

Use the actual `default_view` returned by Canvas. The course names above are demonstration cases. An inaccessible home can coexist with independently accessible folders, pages, or modules. Report the unavailable section and show the resources that were successfully discovered.

[Canvas Courses API](https://developerdocs.instructure.com/services/canvas/resources/courses) · [Canvas Pages API](https://developerdocs.instructure.com/services/canvas/resources/pages) · [Canvas Modules API](https://developerdocs.instructure.com/services/canvas/resources/modules)

### Custom home and ordinary Pages

- Read the home Page's HTML `body` from Canvas. When a link from this preview leads to another supported Canvas Page, fetch it through the existing Page-by-slug path, `GET /api/v1/courses/{courseId}/pages/{slug}`, and continue in the same read-only `JEditorPane` with `text/html`.
- Render sanitized HTML, preserving supported headings, paragraphs, lists, tables, and links in their original order. Use a trusted page URL to resolve relative links. Exclude active elements such as frames and forms and prevent automatic loading of external content in the preview.
- Keep **Open original in Canvas** visible in every Page preview. Swing's HTML support is limited. Layouts that depend on complex CSS, formulas, or embeds are best read in the browser. The original-page button opens the canonical browser URL and leaves authentication to the browser session.
- Handle clicks through the UC3 controller. Read Canvas link metadata when present, with a fallback to the resolved `href`. For example, classify **Weekly Modules** by its target because it may lead to an index Page or to native Modules. Validate the scheme, origin, course, and route before mapping the target to a Canvas request. A Modules target switches to the native module list. File links and Pages without a usable preview open in the system browser.
- Extract recognized course resource links for the catalog. A CSS card, heading, or label with no link remains part of the preview. Opening a linked Page can discover more links, which are added to the catalog while preserving their location.

[Swing HTML rendering](https://docs.oracle.com/en/java/javase/11/docs/api/java.desktop/javax/swing/text/html/HTMLEditorKit.html) · [Swing hyperlink events](https://docs.oracle.com/en/java/javase/11/docs/api/java.desktop/javax/swing/JEditorPane.html) · [Canvas link metadata](https://developerdocs.instructure.com/services/canvas/basics/file.endpoint_attributes)

### Native Modules

- Create a Swing group for each module. Preserve Canvas module order and the item order and indentation within each group.
- On expansion, request `GET /api/v1/courses/{courseId}/modules/{moduleId}/items` if the entries are uncached. Load the item list in pages when needed. Use each item's title and type. Keep a `SubHeader` as a label and use `page_url` for a Page or `content_id` for a file to identify its target.
- Clicking a Page or file row opens the Canvas link in the system browser. Expanding the module and opening a row are separate actions. Reuse the existing [Open source action](../src/main/java/view/ResourcesView.java), which uses `Desktop.browse`. The browser uses the student's Canvas session.
- Eligible Page and PDF rows remain selectable references in the catalog. UC2 can request their contents through the item reader when answering a question.

[Canvas Modules API](https://developerdocs.instructure.com/services/canvas/resources/modules) · [Canvas Files API](https://developerdocs.instructure.com/services/canvas/resources/files) · [Canvas pagination](https://developerdocs.instructure.com/services/canvas/basics/file.pagination)

## 3. Share the catalog with the UI and UC2

Keep course navigation and resource identity in separate fields. The browse view can show a PDF under both **Home → Project resources** and **Modules → Team Project**. The catalog has one entry for that PDF, with both locations. Start from `CourseResource` and `ResourceListing` and extend them as needed.

| Field | Meaning |
| --- | --- |
| `courseId`, `contentId` | Stable course and item identifiers for selection and reads |
| `title`, `type` | Display label and kind, with Page and PDF supported for MVP question answering |
| `sourceUrl` | Canonical Canvas browser link for opening the source and citing it |
| `locations` | Home, directory, or module locations where the item was discovered |

Opening a course builds an initial catalog from accessible source metadata. Add entries as more branches are loaded and follow pagination for supported sections. Return coverage information so the UI and agent can distinguish a partial catalog, an unexplored branch, and a successfully loaded empty list.

### Operations and callers

| Operation | Caller and result |
| --- | --- |
| Open or add course | UC3 Controller initializes the course connection and publishes the initial home view. |
| `listContent(courseId, parentId?, cursor?)` | Swing can request entries in an expanded branch with a `parentId`. The reference sidebar and UC2 can list the deduplicated course catalog with the parent omitted. Return metadata, a continuation cursor when available, and coverage warnings. |
| `readContent(courseId, contentId)` | An in-app Page preview or a UC2 tool request resolves the ID in the selected course and checks access before retrieving that one item. Return Page content or an authorized PDF with its title and canonical source URL. Browser opens use the Canvas link from the catalog. |

Keep Canvas locators, access checks, and authenticated reads in the UC3 adapter and server handlers. The M2 client passes course and item IDs. The adapter constructs the corresponding API request for the selected course.

### UC2 handoff in the same window

1. The sidebar lists browseable entries that can serve as references. The checkbox selects a whole Page or PDF by ID. Opening a resource and selecting it as a reference are separate actions.
2. On **Send**, UC2 uses the question and current course along with the shared metadata catalog, any configured defaults, and the IDs explicitly selected in the sidebar. Default and selected reference IDs are combined and deduplicated before reading.
3. When a question needs a source, UC2 can request a whole catalog item by ID. With no selections or default references, it can start with the catalog and pick the first item itself. It can use the same catalog to choose an additional item as needed within the UC2 read budget.
4. UC2 cites the original links for the sources used. Page sections, selected text, and PDF page ranges are outside the reference selection scope.

Rosalind owns the Canvas adapter, UC3 tool handlers, and browse UI. Chris owns the MCP client integration and UC2's reference and question flow. M1 supplies the shared Gemini client.

## 4. Loading, errors, and acceptance

Run Canvas requests off the Swing event-dispatch thread, then publish the result through the Presenter and ViewModel. Show loading and retry at the level being fetched. Keep the rest of the course visible when one branch fails. An invalid connection, a locked item, an empty list, and a partial catalog should produce different messages.

| Scenario | Acceptance evidence |
| --- | --- |
| Add and reopen a course | A valid course link and token open the course. Reopening reuses its connection. An invalid link or rejected token produces a clear message. |
| Custom HTML home | A CSC207-style fixture displays readable content and clickable links. A linked Page replaces the preview in the same pane. A Modules link switches to the native list. A file link opens in the browser, and the original Canvas link is available from each Page preview. |
| Modules home | A MAT235-style fixture shows modules in order. Expanding `Start Here` loads only the item list. Clicking a Page or the syllabus PDF opens its Canvas URL in the browser. |
| Empty or restricted section | A course with an empty home can still show its accessible Modules, Pages, or Files. A denied directory is marked separately from one that returned zero entries. |
| Shared identity and reads | The same PDF discovered under Home and Modules keeps both locations and one selectable ID. A UC2 request reads the PDF by that ID, independently of the browser action. |
| Questions from the catalog | With fixture data, start a question with selected references or with no references selected. Check that UC2 can choose an additional item by ID and that the answer cites the items it used. |
