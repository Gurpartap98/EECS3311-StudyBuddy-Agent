#!/usr/bin/env bash
# Opens ONE interactive CLI. Type the commands below (copy-paste).
# After each OK result: screenshot the terminal, then continue.
#
#   cd ~/EECS3311-StudyBuddy-Agent
#   git pull
#   mvn -q -DskipTests package
#   bash examples/cli-screenshot-walkthrough.sh

set -euo pipefail
cd "$(dirname "$0")/.."

if [[ ! -f target/Agent.jar ]]; then
  mvn -q -DskipTests package
fi

cat <<'EOF'
============================================================
CLI SCREENSHOT WALKTHROUGH (interactive — one session)
============================================================
1. A CLI will start.
2. Copy-paste EACH block below.
3. When you see the OK / result text → SCREENSHOT that.
4. Then paste the next block.

Save images as:
  stage2/screenshots/F01-cli.png ... F12-cli.png
  (or into ~/3311-screenshots/ then copy later)

AI steps need Groq in .env. Without a key, F03/F04/F05/F07/F12 may FAIL.

----- after CLI starts, paste these one at a time -----

# F01 — screenshot: Created course + courses list
course EECS3311|3311|FW26
courses

# F02 — screenshot: Uploaded ... + materials list
# (replace COURSE_ID with the id printed by courses)
upload COURSE_ID data/samples/os-scheduling.txt
materials COURSE_ID

# Tip: or use the batch file instead (placeholders auto-fill):
#   java -jar target/Agent.jar -LLMAPI -input examples/task-all-features.txt
# then scroll up and screenshot each section.

Starting interactive CLI now...
============================================================
EOF

export STUDYBUDDY_DATA_DIR="${STUDYBUDDY_DATA_DIR:-./data/screenshot-run}"
mkdir -p "$STUDYBUDDY_DATA_DIR"
exec java -jar target/Agent.jar cli
