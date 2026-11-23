# Zoom / screen-recorder demo — GUI then CLI (Nov 24)

One short video. Do **not** show `.env` or API keys.

---

## Before you hit record

```bash
cd ~/EECS3311-StudyBuddy-Agent
git pull
mvn -q -DskipTests package
```

Open two windows side by side if you can: one for GUI later, one terminal ready.

---

## Say at the start (optional)
“Same StudyBuddy task: GUI first, then standalone CLI with `java -jar Agent.jar -input`.”

---

## PART A — GUI (~90 sec)

**Type exactly:**

```bash
cd ~/EECS3311-StudyBuddy-Agent
java -jar target/Agent.jar gui
```

**In the GUI, click this path:**

1. **New Course** → name `EECS3311` → create  
2. Select that course  
3. **Upload Material** → pick `data/samples/os-scheduling.txt` → upload  
4. **Demo Quiz** → answer the 3 questions (any letters) → submit  
5. Click **Weak Topics** once (show the list)  
6. Click **History** once (show the score line)  
7. Close the GUI window

---

## PART B — CLI (~60–90 sec)

**Type exactly (one line):**

```bash
java -jar target/Agent.jar -LLMAPI -input examples/task.txt
```

Let it finish. Scroll up a bit so the camera sees:

- `course …`
- `upload …`
- `demo-quiz` / `grade`
- `weak` / `history` / `plan`

That’s the whole CLI half. **Do not** type interactive commands unless the batch file fails.

---

## If batch fails — interactive backup

```bash
java -jar target/Agent.jar cli
```

Then paste these lines one at a time (replace IDs after `courses` / `upload`):

```text
course EECS3311|3311|FW26
courses
upload COURSE_ID data/samples/os-scheduling.txt
demo-quiz COURSE_ID
grade QUIZ_ID a,a,a
weak COURSE_ID
history COURSE_ID
plan COURSE_ID
quit
```

---

## After recording
1. Upload YouTube (Unlisted or Public)  
2. Send the link for `stage2/stage2-report.md` §7  
3. Keep taking feature screenshots separately if you still need them  

Done.
