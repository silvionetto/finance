#!/usr/bin/env bash

set -euo pipefail

write_output() {
	local key="$1"
	local value="$2"

	if [[ -n "${GITHUB_OUTPUT:-}" ]]; then
		printf '%s=%s\n' "$key" "$value" >> "$GITHUB_OUTPUT"
	else
		printf '%s=%s\n' "$key" "$value"
	fi
}

append_summary() {
	local message="$1"

	if [[ -n "${GITHUB_STEP_SUMMARY:-}" ]]; then
		printf '%s\n' "$message" >> "$GITHUB_STEP_SUMMARY"
	fi
}

fail() {
	local message="$1"
	echo "$message" >&2
	append_summary "$message"
	exit 1
}

read_base_version_from_gradle() {
	local version

	version="$(sed -nE "s/^version = .*'([0-9]+\.[0-9]+\.[0-9]+)(-SNAPSHOT)?'.*/\1/p" build.gradle | head -n 1)"
	if [[ -z "$version" ]]; then
		fail "Unable to determine a semantic base version from build.gradle."
	fi

	printf '%s' "$version"
}

increment_version() {
	local base_version="$1"
	local bump="$2"
	local major
	local minor
	local patch

	IFS='.' read -r major minor patch <<< "$base_version"

	case "$bump" in
		major)
			major=$((major + 1))
			minor=0
			patch=0
			;;
		minor)
			minor=$((minor + 1))
			patch=0
			;;
		patch)
			patch=$((patch + 1))
			;;
		*)
			fail "Unsupported bump type: $bump"
			;;
	esac

	printf '%s.%s.%s' "$major" "$minor" "$patch"
}

latest_tag="$(git describe --tags --abbrev=0 --match 'v*' 2>/dev/null || true)"
target_sha="${GITHUB_SHA:-HEAD}"
commit_log_args=()

if [[ -n "$latest_tag" ]]; then
	base_version="${latest_tag#v}"
	commit_log_args=("${latest_tag}..${target_sha}")
else
	base_version="$(read_base_version_from_gradle)"
	before_sha="${BEFORE_SHA:-}"
	if [[ -n "$before_sha" && "$before_sha" != "0000000000000000000000000000000000000000" ]] && git cat-file -e "${before_sha}^{commit}" 2>/dev/null; then
		commit_log_args=("${before_sha}..${target_sha}")
	else
		commit_log_args=(-1 "${target_sha}")
	fi
fi

if [[ ! "$base_version" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
	fail "Base version '$base_version' is not valid semantic versioning."
fi

commit_messages="$(git log --format='%s%n%b%n----END-COMMIT----' "${commit_log_args[@]}")"

if [[ -z "${commit_messages//[$'\t\r\n ']}" ]]; then
	write_output "should_release" "false"
	write_output "reason" "No commits found to evaluate for a release."
	append_summary "No commits were found to evaluate for a release."
	exit 0
fi

bump="none"
if grep -Eq '^[^:]+(\([^)]+\))?!:' <<< "$commit_messages" || grep -Eq '(^|[[:space:]])BREAKING CHANGE:' <<< "$commit_messages"; then
	bump="major"
elif grep -Eq '^feat(\([^)]+\))?:' <<< "$commit_messages"; then
	bump="minor"
elif grep -Eq '^fix(\([^)]+\))?:' <<< "$commit_messages"; then
	bump="patch"
fi

if [[ "$bump" == "none" ]]; then
	write_output "should_release" "false"
	write_output "reason" "No feat/fix/breaking change commits were found in the evaluated range."
	append_summary "Skipped release creation because no \`feat:\`, \`fix:\`, or breaking change commits were found."
	exit 0
fi

next_version="$(increment_version "$base_version" "$bump")"
tag="v${next_version}"

write_output "should_release" "true"
write_output "bump" "$bump"
write_output "version" "$next_version"
write_output "tag" "$tag"

append_summary "Preparing release \`${tag}\` from base version \`${base_version}\` with a \`${bump}\` bump."
