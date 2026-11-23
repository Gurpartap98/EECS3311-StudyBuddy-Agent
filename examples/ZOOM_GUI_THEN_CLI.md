# Zoom demo script (GUI then CLI) — due November 24

Record **one** short Zoom video (~2–3 minutes). Unlisted/Public YouTube.

## Say at the start
“This demo shows the same StudyBuddy task in the GUI, then in the standalone CLI.”

## Part A — GUI (~90 seconds)
```bash
cd ~/EECS3311-StudyBuddy-Agent
git pull
mvn -q -DskipTests package
java -jar target/Agent.jar gui
```

1. **New Course** → name `EECS3311`  
2. Select course → **Upload Material** → `data/samples/os-scheduling.txt`  
3. **Demo Quiz** → answer questions → submit  
4. Click **Weak Topics** and **History** briefly  

## Part B — CLI (~60–90 seconds)
In a terminal (same folder):

```bash
java -jar target/Agent.jar -LLMAPI -input examples/task.txt
```

Scroll so the TA sees commands run: course → upload → demo-quiz → grade → weak → history → plan.

## After recording
1. Upload to YouTube (Unlisted or Public)  
2. Send the link so it can be added to `stage2/stage2-report.md` §7  
3. Optional: pause each feature GUI video → screenshot → replace `stage2/screenshots/F0x-gui.png`  

Do **not** show your `.env` or API key on screen.
