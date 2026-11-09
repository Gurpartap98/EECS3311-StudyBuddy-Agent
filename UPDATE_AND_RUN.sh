#!/usr/bin/env bash
# Force-update StudyBuddy from GitHub and launch the GUI.
# Usage: bash UPDATE_AND_RUN.sh

set -euo pipefail

TARGET="${HOME}/EECS3311-StudyBuddy-Agent"

echo "==> Using folder: ${TARGET}"
if [[ ! -d "${TARGET}/.git" ]]; then
  echo "Repo not found. Cloning fresh..."
  git clone https://github.com/Gurpartap98/EECS3311-StudyBuddy-Agent.git "${TARGET}"
fi

cd "${TARGET}"
echo "==> Closing tip: make sure any old StudyBuddy window is already closed."

git remote set-url origin https://github.com/Gurpartap98/EECS3311-StudyBuddy-Agent.git
git fetch origin
git checkout main
git reset --hard origin/main

echo "==> Current commit (must be 9c9325d or newer):"
git log -1 --oneline

echo "==> Checking that the new GUI is present..."
if ! grep -q "View Material" src/main/java/studybuddy/ui/StudyGUI.java; then
  echo "ERROR: View Material not found — wrong folder or update failed."
  exit 1
fi
if ! grep -q 'courseModel.addElement(c.getName())' src/main/java/studybuddy/ui/StudyGUI.java; then
  echo "ERROR: friendly course names not found — update failed."
  exit 1
fi
if grep -q 'LLM unavailable' src/main/java/studybuddy/tools/StudyPlanTool.java; then
  echo "ERROR: old StudyPlanTool still present — update failed."
  exit 1
fi
echo "Checks OK."

echo "==> Clean rebuild..."
mvn -q clean package -DskipTests

echo "==> Starting GUI from THIS folder:"
pwd
mvn -q exec:java -Dexec.args="gui"
