#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# TigerPaw — Build & Publish Script
#
# Usage:
#   ./release.sh [--bump major|minor|patch] [--notes "Release notes"]
#
# Examples:
#   ./release.sh                          # build current version, publish
#   ./release.sh --bump patch             # 0.1.0 → 0.1.1, build, publish
#   ./release.sh --bump minor             # 0.1.0 → 0.2.0, build, publish
#   ./release.sh --bump major             # 0.1.0 → 1.0.0, build, publish
#   ./release.sh --bump patch --notes "Fix crash on launch"
# ─────────────────────────────────────────────────────────────────────────────
set -euo pipefail

GRADLE_FILE="app/build.gradle.kts"

# ── Helpers ───────────────────────────────────────────────────────────────────
log()  { echo -e "\033[1;36m▶ $*\033[0m"; }
ok()   { echo -e "\033[1;32m✔ $*\033[0m"; }
die()  { echo -e "\033[1;31m✘ $*\033[0m" >&2; exit 1; }

require() { command -v "$1" &>/dev/null || die "Required tool not found: $1"; }

# ── Argument parsing ──────────────────────────────────────────────────────────
BUMP=""
NOTES=""
while [[ $# -gt 0 ]]; do
    case "$1" in
        --bump)  BUMP="$2";  shift 2 ;;
        --notes) NOTES="$2"; shift 2 ;;
        *) die "Unknown argument: $1" ;;
    esac
done

# ── Prereq checks ─────────────────────────────────────────────────────────────
require gh
require git
require java

# ── Read current version ──────────────────────────────────────────────────────
CURRENT_NAME=$(grep 'versionName\s*=' "$GRADLE_FILE" | grep -oP '"\K[^"]+')
CURRENT_CODE=$(grep 'versionCode\s*=' "$GRADLE_FILE" | grep -oP '\d+')

log "Current version: $CURRENT_NAME (code $CURRENT_CODE)"

# ── Bump version ──────────────────────────────────────────────────────────────
IFS='.' read -r VMAJOR VMINOR VPATCH <<< "$CURRENT_NAME"

case "$BUMP" in
    major) VMAJOR=$((VMAJOR + 1)); VMINOR=0; VPATCH=0 ;;
    minor) VMINOR=$((VMINOR + 1)); VPATCH=0 ;;
    patch) VPATCH=$((VPATCH + 1)) ;;
    "")    ;;  # no bump, use current version
    *)     die "Invalid --bump value: $BUMP (must be major, minor, or patch)" ;;
esac

NEW_NAME="${VMAJOR}.${VMINOR}.${VPATCH}"
NEW_CODE=$((CURRENT_CODE + 1))

if [[ -n "$BUMP" ]]; then
    log "Bumping version: $CURRENT_NAME → $NEW_NAME (code $NEW_CODE)"

    # Update app/build.gradle.kts
    sed -i "s/versionCode\s*=\s*${CURRENT_CODE}/versionCode = ${NEW_CODE}/" "$GRADLE_FILE"
    sed -i "s/versionName\s*=\s*\"${CURRENT_NAME}\"/versionName = \"${NEW_NAME}\"/" "$GRADLE_FILE"

    ok "Updated $GRADLE_FILE"
fi

VERSION="$NEW_NAME"
TAG="v${VERSION}"

# ── Ensure working tree is clean (after any version bump) ─────────────────────
if [[ -n "$BUMP" ]]; then
    git add "$GRADLE_FILE"
    git commit -m "chore: bump version to $TAG"
    git push origin HEAD
    ok "Committed and pushed version bump"
fi

if [[ -n $(git status --porcelain) ]]; then
    die "Working tree is dirty. Commit or stash changes before releasing."
fi

# ── Build release APK ─────────────────────────────────────────────────────────
log "Building release APK…"
./gradlew :app:assembleRelease

APK_SRC="app/build/outputs/apk/release/app-release.apk"
[[ -f "$APK_SRC" ]] || die "APK not found at $APK_SRC"

ARTIFACT_DIR="dist"
mkdir -p "$ARTIFACT_DIR"

ARTIFACT_NAME="TigerPaw-${VERSION}.apk"
ARTIFACT_PATH="${ARTIFACT_DIR}/${ARTIFACT_NAME}"

cp "$APK_SRC" "$ARTIFACT_PATH"
ok "Artifact: $ARTIFACT_PATH"

# ── Git tag ───────────────────────────────────────────────────────────────────
if git rev-parse "$TAG" &>/dev/null; then
    log "Tag $TAG already exists, skipping tag creation"
else
    git tag -a "$TAG" -m "Release $TAG"
    git push origin "$TAG"
    ok "Tagged and pushed $TAG"
fi

# ── GitHub Release ────────────────────────────────────────────────────────────
log "Creating GitHub release $TAG…"

RELEASE_NOTES="${NOTES:-"Release $TAG"}"

gh release create "$TAG" \
    "$ARTIFACT_PATH#TigerPaw $VERSION (APK)" \
    --title "TigerPaw $VERSION" \
    --notes "$RELEASE_NOTES"

ok "Published: https://github.com/$(gh repo view --json nameWithOwner -q .nameWithOwner)/releases/tag/$TAG"
