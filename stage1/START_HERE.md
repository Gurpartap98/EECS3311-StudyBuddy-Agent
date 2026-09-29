# START HERE — StudyBuddy Agent (Stage 1)

## Plagiarism vs required AI (admin slide)

**They are not the same thing.**

| Allowed / required | Not allowed (plagiarism) |
|--------------------|---------------------------|
| Using LLMs for **your** course project (design help, later coding) | Copying another student’s report or code |
| AI coding tools in Stage 2 (must document them) | Pasting web/slides/code **without saying where it came from** |
| Turning in work **you** understand and edited | Submitting someone else’s design as yours |

**You will not be flagged just for using AI on this project** — Song Wang said AI use is **required**.  
You **will** get in trouble if you copy peers or uncited sources, or submit work you can’t explain.

Safe habit: keep notes of what AI helped with (Stage 2 needs a collaboration log anyway).

---

## Deadline reality (today ≈ Sep 28, due Oct 5)

You also have midterms next week. Do **design only** this week — no full app.

| When | Time | Task |
|------|------|------|
| **Tonight** | 45–60 min | Read `Stage1_Design_Report.md`, put your name, skim features, create GitHub repo |
| **Tue** | 60–90 min | Draw **use-case diagram** + fill 4–5 use-case writeups |
| **Wed** | 60–90 min | Draw **class diagram** (mark 5 patterns) |
| **Thu** | midterm priority | Only 30 min if free: start 2 sequence diagrams |
| **Fri–Sat** | 2–3 hrs total | Finish sequences + traceability polish |
| **Sun Oct 4** | 60 min | Push to GitHub, check public link, submit URL |
| **Mon Oct 5** | buffer | Fix anything broken |

Midterm days: **do not** start coding the agent. Report first.

---

## Done vs not done

**Done for you already (in `Stage1_Design_Report.md`):**
- Project choice: AI Study / Learning Agent  
- Overview, LLM = Ollama local  
- 12 features in required format  
- 5+ patterns mapped  
- Class list, sequence list, traceability table, short realizations  

**You still must do:**
1. Put **your name / student number**  
2. Create **public GitHub** repo and paste URL  
3. **Draw the UML pictures** (diagrams.net) and drop PNGs in `diagrams/`  
4. Expand **full use-case descriptions** (template in the report)  
5. Skim everything so you can explain it if asked  
6. Submit repo URL on eClass  

---

## GitHub (do tonight)

```bash
cd ~/Documents/3311
# if not a git repo yet:
git init
# create repo on github.com (public), then:
git remote add origin https://github.com/YOUR_USER/EECS3311-StudyBuddy-Agent.git
git add stage1
git commit -m "Add Stage 1 design draft for StudyBuddy Agent"
git branch -M main
git push -u origin main
```

Keep using **this same repo** for Stages 2 and 3.

---

## Diagrams tool
https://app.diagrams.net/ → Export PNG → save under `stage1/diagrams/`

Suggested filenames:
- `usecase.png`
- `class.png`
- `seq_upload.png`
- `seq_summarize.png`
- `seq_qa.png`
- `seq_quiz.png`
- `seq_grade.png`
- `seq_nl_command.png`

---

## Next message to your AI helper
Say: **“let’s draw the use-case diagram — tell me exactly what boxes and lines”**  
or **“walk me through the class diagram class by class.”**
