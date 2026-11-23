# What to screenshot (the RESULT, not the empty window)

For each feature, capture the screen **after it succeeds** — the output that proves it worked.

Put GUI stills in: `stage2/screenshots/` (replace `F0x-gui.png` files)  
Put CLI stills in: `stage2/screenshots/` (e.g. `F0x-cli-….png`) or a folder like `~/3311-screenshots/` then copy in.

---

## GUI (from your old videos — pause + screenshot)

| Feature | Pause when you see… |
|---------|---------------------|
| F01 | Course name in the **Courses** list (after New/Rename) |
| F02 | File name under **Materials** + “Uploaded …” in Agent output |
| F03 | **Full summary text** in Agent output (not only “OK”) |
| F04 | **Listed concepts** in Agent output |
| F05 | **Answer** to your question in Agent output |
| F06 | **Study plan sessions** in Agent output |
| F07 | Flashcards listed / review window open |
| F08 demo | Demo quiz window with questions |
| F08 AI | Quiz (AI) window with generated questions |
| F09 | Grade/score after submit |
| F10 | Weak topics list in Agent output |
| F11 | History lines in Agent output |
| F12 | Multi-step plan output after `summarize then make flashcards` |

---

## CLI (run the walkthrough script below)

| Feature | Screenshot when terminal shows… |
|---------|----------------------------------|
| F01 | `OK: Created course …` and `courses` list |
| F02 | `OK: Uploaded "os-scheduling.txt"` and `materials` list |
| F03 | `OK:` + summary text (`summarize $MATERIAL`) |
| F04 | `OK:` + concept list (`concepts $MATERIAL`) |
| F05 | `OK:` + answer (`ask What is Round Robin?`) |
| F06 | `OK: Study plan ready…` + session lines |
| F07 | `OK:` + flashcards generated |
| F08 | `OK: Created demo quiz…` + `quizzes` showing questions |
| F09 | `OK: Score: …` after `grade` |
| F10 | Weak topic list (or “no weak topics” if 100%) |
| F11 | History line with score % |
| F12 | Plan steps for `nl summarize then make flashcards` |
| Batch | Full `java -jar … -input examples/task.txt` run |

**AI features (F03–F05, F07, F08-AI, F12)** need `.env` with a Groq key.  
Without a key they will FAIL — still screenshot the failure only if you must; better to set the key first.

---

## Quick commands

```bash
cd ~/EECS3311-StudyBuddy-Agent
git pull
mvn -q -DskipTests package
cp .env.example .env   # if needed, put Groq key in .env

# Step-by-step CLI with pauses for screenshots:
bash examples/cli-screenshot-walkthrough.sh
```
