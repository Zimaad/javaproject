# SmartInterview

An AI-powered mock interview desktop app for students, built with JavaFX. You pick a role, take timed MCQ, coding and HR rounds, and get a scored report with model solutions, exportable as a PDF.

Everything runs locally. Accounts, questions and results live in a SQLite file on your machine. The only network traffic is calls to the AI provider(s) you configure.

---

## Contents

1. [Features](#features)
2. [Requirements](#requirements)
3. [Quick start](#quick-start)
4. [Configuring AI](#configuring-ai)
5. [Accounts and first login](#accounts-and-first-login)
6. [How an interview works](#how-an-interview-works)
7. [Scoring](#scoring)
8. [PDF reports](#pdf-reports)
9. [Admin area](#admin-area)
10. [Project structure](#project-structure)
11. [Database](#database)
12. [Architecture notes](#architecture-notes)
13. [Troubleshooting](#troubleshooting)
14. [Known limitations](#known-limitations)
15. [Development notes](#development-notes)

---

## Features

**For students**
- Register, sign in, edit your profile, change your password, delete your account.
- Choose a role (Java Developer, Software Engineer, Data Analyst), then configure difficulty, duration, number of questions and which rounds to include (MCQ, Coding, HR).
- Timed rounds with a question navigator, answer persistence while you move between questions, and automatic submission when time runs out.
- A dashboard with your interview count, average and best score, questions answered, a score trend chart and recent interviews.
- Interview history with search and role filter, and a full report for each past interview.
- A searchable question bank (category, difficulty, type) with bookmarks that are saved per user.
- Saved preferences (default difficulty and duration) that pre-fill the interview setup screen.

**AI-powered**
- Questions are picked from the local bank; if the bank has too few at your chosen difficulty, the AI writes new ones and they are stored for future use.
- **Run** in the coding round asks the AI to trace your code against examples and edge cases.
- Coding and HR answers are graded by the AI, which also writes a model solution and a breakdown for every problem.
- Personalised strengths, weaknesses and next steps.

**Reports**
- A multi-page PDF with the overall result, round scores, a "questions to revisit" list, and a full review of every question in every round. See [PDF reports](#pdf-reports).

**For administrators**
- A dashboard with real counts, a seven-day interview chart and a live activity feed.
- Add, edit and delete questions, and generate batches of questions with the AI.

---

## Requirements

| Requirement | Notes |
|---|---|
| JDK 21 or newer | The project compiles with `--release 21`. JDK 25 is known to work. |
| Maven 3.9+ | `run.bat` expects Maven at `%USERPROFILE%\.m2\apache-maven-3.9.9`. Any Maven on your `PATH` also works. |
| Internet | Only needed the first time (to download dependencies) and for AI calls. |
| An AI API key | Optional but strongly recommended. Without one, only MCQ marking works. |

JavaFX, SQLite, Jackson and PDFBox are all pulled in by Maven; nothing else needs installing.

---

## Quick start

```bash
# 1. Give the app an AI key (see "Configuring AI" for all options)
set GROQ_API_KEY=your-key-here          # Windows cmd
$env:GROQ_API_KEY = "your-key-here"     # PowerShell

# 2. Run it from the project folder
mvn javafx:run
# or on Windows, just double-click run.bat
```

On first launch the app creates a `data/` folder containing `smartinterview.db`, seeds it with the starter question bank and creates the administrator account. The console prints `Database ready.` when it is up.

Run it **from the project folder**: `data/` and `config.properties` are looked up relative to the working directory.

---

## Configuring AI

The app talks to AI providers over HTTPS. You can configure several; it uses them in a fixed order and falls back to the next one if a call fails or returns something unparseable.

**Order tried:** `groq` → `openrouter` → `gemini` → `anthropic` → `openai`

Only providers that have a key are used.

### Environment variables

| Provider | Key variable | Default model | Override |
|---|---|---|---|
| gpt-oss-120b via Groq | `GROQ_API_KEY` | `openai/gpt-oss-120b` | `GROQ_MODEL` |
| gpt-oss-120b via OpenRouter | `OPENROUTER_API_KEY` | `openai/gpt-oss-120b` | `OPENROUTER_MODEL` |
| Gemini Flash | `GEMINI_API_KEY` | `gemini-3.8-flash` | `GEMINI_MODEL` |
| Anthropic | `ANTHROPIC_API_KEY` | `claude-sonnet-5-5` | `ANTHROPIC_MODEL` |
| OpenAI | `OPENAI_API_KEY` | `gpt-4o-mini` | `OPENAI_MODEL` |

Other variables:

| Variable | Purpose |
|---|---|
| `AI_PROVIDER` | Moves one provider to the front of the order, e.g. `gemini`. |
| `<PROVIDER>_BASE_URL` | Overrides a provider's endpoint, e.g. `OPENAI_BASE_URL=http://localhost:11434/v1` to use a local OpenAI-compatible server. |
| `ADMIN_PASSWORD` | Password for the seeded admin account (first run only). |

Environment variables must be set **before** the app starts. If you set them in a terminal, launch the app from that same terminal. If you set them system-wide, restart your terminal or IDE first.

### Using a config file instead

Copy `config.properties.example` to `config.properties` (same folder as `pom.xml`) and fill in keys. The same names work in lowercase with dots (`groq.api.key`, `gemini.model`, `ai.provider`). `config.properties` is git-ignored so keys are never committed. If both a file entry and an environment variable exist, the file wins.

### Which provider should I use?

- gpt-oss-120b and Gemini Flash are both reasoning-style models. The client doubles the response token limit for these providers so their thinking does not truncate the JSON they return.
- Coding grading with model solutions is the heaviest call (up to 6000 tokens requested). If it fails on one provider the app retries on the next.

### What happens with no key

| Feature | Without a key |
|---|---|
| MCQ round and scoring | Works fully (marked locally) |
| Question bank, history, dashboard | Works fully |
| Question generation | Disabled; the app uses what is in the bank and says so |
| Coding / HR grading, model solutions | Unavailable. These rounds are left out of the score; the report states why. |
| Run button | Shows a message asking for a key |

---

## Accounts and first login

**Student:** click **Register** on the login screen, fill in the form (password at least 8 characters), then sign in with the same email and password.

**Administrator:** a built-in account is created the first time the database is made:

| Email | Password |
|---|---|
| `admin@smartinterview.com` | `admin123` (or your `ADMIN_PASSWORD`) |

Enter these in the email and password fields, then click either **Sign in** or **Sign in as administrator**. Both open the admin dashboard.

- The admin button never works on a student account; it checks the account's role.
- The admin password is only read when the database is first created. To change it afterwards use **Settings → Change password**, or delete `data/` to start over (this erases all users and history).
- There is no email-based password reset, because the app is local-only and sends no email.

---

## How an interview works

1. **Select a role** on the Interview screen.
2. **Configure** difficulty, duration, question count and rounds. Defaults come from your Settings.
3. **Start.** The app prepares the questions in the background (the button shows "Preparing your questions…"). Questions come from the bank; if there are not enough at your difficulty and AI is configured, new ones are generated and saved.
4. **Rounds run in order: MCQ → Coding → HR**, skipping any you unticked.
   - **MCQ:** one timer for the whole round (1.5 minutes per question). Jump between questions with the navigator.
   - **Coding:** two problems (three for a 15-question interview). Use **Run** for an AI review before submitting. The language selector is passed to the grader.
   - **HR:** at least three behavioural questions, 2000 characters each. The timer is 2 minutes multiplied by the interview's question count.
5. **Evaluation:** the app shows four live stages (MCQ, coding, behavioural, feedback), reflecting what is actually running, not a fixed animation.
6. **Report:** view the score breakdown on screen and export the PDF.

Timers submit the round automatically at zero. Your answers are held in memory during the interview and written to the database only when evaluation finishes. Closing the app mid-interview discards that attempt.

### How questions are picked

- **MCQ** questions are limited to the role's topics (for example Java Developer: Java, OOP, Collections, JDBC, Multithreading).
- Questions at your chosen difficulty are preferred, then the nearest difficulty.
- If fewer than the requested number match your difficulty and an AI key exists, the AI generates the shortfall. Generated questions are validated (exactly four options, a valid correct index, no duplicates) before being stored; invalid ones are discarded.
- Any problem preparing questions is shown in the report's recommendations rather than silently ignored.

---

## Scoring

| Round | How it is scored |
|---|---|
| MCQ | Local. `correct / total × 100`. Unanswered counts as wrong. |
| Coding | AI. Each problem is scored 0–100 for correctness, edge cases, efficiency and quality; the round score is the average over **all** problems. |
| HR | AI. Each answer is scored 0–100 for structure (such as STAR), specificity, ownership, relevance and communication; averaged over all questions. |

**Overall score** = the average of the rounds that were actually scored.

Safeguards:
- **Skipped work is always 0.** An unchanged code template or an empty HR answer scores 0 regardless of what the AI says; this is enforced in code, not left to the model.
- **Nothing is invented.** If a round cannot be graded (no key, network failure), it is excluded from the overall score, shown as "could not be graded" in the report, and its answers are still kept.
- Score bands used for colour and verdicts: **75+** strong, **50–74** room to improve, **below 50** needs practice.

---

## PDF reports

Click **Export report** on any report screen (right after an interview, or from History → View report) and choose where to save the PDF.

### What is in it

1. **Summary page**
   - Role, date, duration, number of questions answered.
   - Overall score with a verdict.
   - A bar for each round.
   - Strengths, areas to improve, recommended next steps.
   - **Questions to revisit:** every question that was wrong, skipped, ungraded or scored below 60%.
2. **MCQ section** (one entry per question)
   - Category, difficulty and a status badge (correct / incorrect / skipped).
   - All options, with the correct one marked and your choice marked.
   - **Why this is the answer**: the explanation.
3. **Coding section** (one entry per problem)
   - The problem statement.
   - **Your solution** in a code block (or "not submitted").
   - **Feedback on your solution:** what was right, bugs, missed edge cases.
   - **Best solution** in the language you used.
   - **How to get there:** the approach in numbered steps.
   - **Complexity:** time and space, with reasons.
   - **Common mistakes** on that problem.
4. **HR section** (one entry per question)
   - Your answer and the AI's feedback.
   - **What your answer was missing.**
   - **What a strong answer looks like:** a model answer.
   - **How that answer is built:** its structure point by point.
   - The interviewer's guidance from the question bank.

Every page has a footer with the role, date and "Page X of Y".

### Notes

- The report is built from details saved with the interview, so it can be regenerated any time from History.
- **Interviews taken before detailed reports existed** have no per-question data. Their PDF contains the summary only and says why.
- If model solutions are missing from a coding problem, the AI call failed or no key was set when the interview was graded. The report says so rather than leaving a blank.
- The PDF uses the app's bundled fonts (Plus Jakarta Sans and JetBrains Mono). Characters those fonts lack (for example some emoji or CJK) are dropped from prose and shown as `?` inside code.

---

## Admin area

Sign in as the administrator (see [Accounts](#accounts-and-first-login)).

**Dashboard**
- Total students, total interviews, questions in the bank, global average score.
- A bar chart of interviews per day for the last seven days.
- An activity feed of the latest completed interviews and new sign-ups, with "time ago" stamps.

**Question management**
- Search and filter by category and type.
- **Add question:** a form that adapts to the type.
  - MCQ: four options and the correct answer, plus an explanation.
  - Coding: a starter code template.
  - HR: a sample answer or guidance.
- **Edit** (pencil) and **Delete** (bin, with confirmation) on each row.
- **Generate with AI:** choose type, topic, difficulty and count (3, 5 or 10). The new questions are validated, de-duplicated and added to the bank.

Deleting a question does not alter past reports, which keep their own copy of what was asked.

---

## Project structure

```
pom.xml                         Maven build (JavaFX, SQLite, Jackson, PDFBox, Ikonli)
run.bat                         Windows launcher
config.properties.example       Template for local settings (copy to config.properties)
.gitignore                      Ignores target/, data/, config.properties
data/                           Created on first run: smartinterview.db

src/main/java/
  module-info.java              Java module declaration
  com/smartinterview/
    App.java, Launcher.java     JavaFX entry point (opens the DB, then shows the landing page)
    navigation/                 NavigationManager: loads FXML screens, passes data between them
    controller/                 One controller per screen
    model/                      Plain data classes
      Interview, InterviewResult, Question, QuestionReview,
      User, UserSettings, PerformanceData, ActivityEntry
    service/
      AuthService               Accounts, sessions, profile, password, settings
      QuestionService           Question bank, bookmarks, picking, AI generation
      InterviewService          Roles, building a session, saving and querying results, admin stats
      InterviewSession          State of the interview in progress (questions + answers)
      EvaluationService         Marks MCQs, AI-grades coding/HR, builds per-question reviews, feedback
      ReportPdf, PdfCanvas      PDF report content and the layout engine
    ai/
      AiConfig                  Reads keys/models from config.properties or the environment
      AiClient                  HTTP client for Anthropic and OpenAI-compatible APIs, with fallback
      AiException
    db/
      Database                  Connections, schema, migrations, JSON helpers
      Seeder                    Starter questions and the admin account
      PasswordHasher            Salted PBKDF2 password hashing
    ui/
      Sidebar, AdminSidebar     Navigation bars
      Dialogs                   Styled confirm / change-password / generate dialogs
      QuestionEditorDialog      Add / edit question form

src/main/resources/com/smartinterview/
  view/*.fxml                   Screen layouts
  css/*.css                     Theme and component styles
  fonts/*.ttf                   Bundled fonts (also embedded in PDFs)
```

---

## Database

SQLite, one file: `data/smartinterview.db`. Connections are opened per operation and foreign keys are enforced.

| Table | Purpose | Key columns |
|---|---|---|
| `users` | Accounts | `id`, `full_name`, `email` (unique, case-insensitive), `password_hash`, `college`, `degree`, `year`, `role` (`student`/`admin`), `created_at` |
| `questions` | The question bank | `id`, `type` (`mcq`/`coding`/`hr`), `category`, `difficulty`, `text`, `options_json`, `correct_index`, `code_template`, `expected_output`, `sample_answer`, `explanation`, `source` (`seed`/`ai`/`admin`) |
| `interviews` | Finished interviews | `id`, `user_id`, `role_name`, `overall_score`, `time_taken_min`, `taken_at`, `questions_answered`, `round_scores_json`, `strengths_json`, `weaknesses_json`, `recommendations_json`, `details_json` |
| `bookmarks` | Saved questions per user | `user_id`, `question_id` |
| `settings` | Per-user preferences | `user_id`, `default_difficulty`, `default_duration`, `email_notifications`, `practice_reminders` |

- `details_json` holds the full per-question review used for the PDF. Databases created before it existed are upgraded automatically on startup.
- Deleting a user cascades to their interviews, bookmarks and settings.
- **Passwords** are never stored in plain text: PBKDF2-HMAC-SHA256, 120,000 iterations, a random 16-byte salt per password, compared in constant time.

### Resetting or inspecting

- **Reset everything:** close the app and delete the `data/` folder. It is recreated on next launch.
- **Inspect:** open `data/smartinterview.db` in any SQLite browser (for example DB Browser for SQLite).
- **Back up:** copy the file while the app is closed.

---

## Architecture notes

- **Singletons for services.** Screens call `AuthService`, `QuestionService`, `InterviewService` and `EvaluationService` directly.
- **Navigation with data.** `NavigationManager.navigateTo(view, data)` passes an object to controllers implementing `DataReceiver`; the interview itself travels this way, while questions and answers live in `InterviewSession`.
- **Background work.** Anything that can block (AI calls, preparing a session, grading, code review, question generation) runs in a JavaFX `Task` on a daemon thread. UI updates come back on the FX thread, so the window stays responsive.
- **AI client.** One small class, no vendor SDKs. Anthropic uses its Messages API; everything else uses the OpenAI-style `/chat/completions` format, which Gemini, Groq and OpenRouter all accept. JSON replies are extracted even if wrapped in markdown fences or surrounded by prose.
- **PDF engine.** `PdfCanvas` is a small flow-layout layer over Apache PDFBox: wrapped text, bullets, code panels that split across pages, score bars, badges and a footer pass at the end.
- **Java modules.** The app is a named module. Jackson needs reflective access to the `model` package (declared with `opens`); SQLite's driver needs `slf4j` on the module path, which is why `slf4j-api` and `slf4j-nop` are in the POM.

---

## Troubleshooting

| Symptom | Cause and fix |
|---|---|
| Report says coding/HR "could not be graded" | No key, a wrong key, or the provider is down. Check the key variable name and that it was set **before** launching. The report lists the exact error. |
| "No AI API key configured" | None of the key variables are visible to the app. Restart the terminal/IDE after setting system-wide variables, or use `config.properties`. |
| AI error `401` / `403` | The key was rejected. Re-copy it and check it belongs to the provider you named. |
| AI error `429` | Rate limit or quota. Wait, or add a second provider for fallback. |
| Model not found | Override the model, for example `GEMINI_MODEL=...` or `GROQ_MODEL=...`, with an ID your account can use. |
| "Incorrect email or password" | Check the email. Students must register first; there is no default student account. |
| "That account is not an administrator" | You used a student account with the admin button. Use the admin credentials. |
| Admin password does not match `ADMIN_PASSWORD` | That variable is only read when the database is first created. Use Settings → Change password, or delete `data/`. |
| `Module org.slf4j not found` on start | Run `mvn clean compile` so `slf4j-api` is resolved; do not remove it from the POM. |
| Window fails to start with a database error | `data/` may be read-only or on a locked cloud-synced path. Move the project to a normal folder or fix permissions. |
| PDF has `?` in code | The character is not in the bundled monospace font. The code itself is not changed in the app. |
| App seems to hang after "Start interview" | It is generating questions with the AI. This can take up to a minute on a slow connection; a failure shows an error on the setup screen. |

---

## Known limitations

- **Code is not executed.** Run and grading trace the code with an AI model; they do not compile or run it, so edge-case behaviour can be misjudged. Treat scores as strong guidance, not a compiler's verdict.
- **Single machine.** Data is a local file; there is no sync or multi-device login.
- **No email.** The notification and reminder toggles in Settings are saved but nothing sends email, and there is no email-based password reset.
- **Interviews are not resumable.** Closing the app mid-interview discards the attempt.
- **AI output varies.** Model solutions and feedback are generated text. Review them critically, particularly for harder problems.
- **Static role list.** The three roles are defined in code (`InterviewService.getAvailableRoles`); the questions beneath them are fully editable.
- **Language of the model solution** follows the language selector at submission time; the starter templates themselves are Java.

---

## Development notes

```bash
mvn compile              # build
mvn javafx:run           # run
mvn clean compile        # rebuild from scratch
```

Adding things:
- **A new AI provider:** if it speaks the OpenAI chat-completions format, add one `add(all, "name", false, baseUrl, defaultModel)` line in `AiConfig.endpoints()`. Its key is then `NAME_API_KEY`.
- **A new role:** add an `Interview` entry in `InterviewService.getAvailableRoles()` with topics that match question categories in the bank.
- **A new screen:** add an FXML and controller, register the view and its CSS in `NavigationManager`.
- **A new column or table:** add it to `Database.init()`; use `addColumnIfMissing` for columns so existing databases upgrade in place.

Never commit `config.properties` or `data/`; both are in `.gitignore`.
