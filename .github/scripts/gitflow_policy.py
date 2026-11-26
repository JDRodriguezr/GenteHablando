"""Validate GitFlow PR metadata without executing branch names or PR content."""

import json
import os
import re
import sys


FEATURE = re.compile(r"feature/[a-z0-9]+(?:-[a-z0-9]+)*")
RELEASE = re.compile(r"release/(?:0|[1-9][0-9]*)\.(?:0|[1-9][0-9]*)\.(?:0|[1-9][0-9]*)")
ALLOWED = (
    "Allowed PRs: feature/<slug> -> develop; same-repository "
    "release/<major.minor.patch> -> main; same-repository main -> develop."
)


def repository_id(repository):
    if not isinstance(repository, dict):
        raise ValueError("Missing or invalid repository metadata.")
    value = repository.get("id")
    if type(value) is not int or value <= 0:
        raise ValueError("Repository IDs must be positive integers.")
    return value


def branch_metadata(pull_request, side):
    branch = pull_request.get(side)
    if not isinstance(branch, dict) or not isinstance(branch.get("ref"), str):
        raise ValueError("Missing or invalid branch metadata.")
    return branch["ref"], repository_id(branch.get("repo"))


def validate_pull_request(event):
    if not isinstance(event, dict) or not isinstance(event.get("pull_request"), dict):
        raise ValueError("Missing or invalid pull request metadata.")
    source, source_repo = branch_metadata(event["pull_request"], "head")
    target, target_repo = branch_metadata(event["pull_request"], "base")
    if target_repo != repository_id(event.get("repository")):
        raise ValueError("The PR target does not match the event repository.")

    same_repository = source_repo == target_repo
    if target == "develop" and FEATURE.fullmatch(source):
        return
    if same_repository and target == "main" and RELEASE.fullmatch(source):
        return
    if same_repository and source == "main" and target == "develop":
        return
    raise ValueError(ALLOWED)


def main():
    try:
        with open(os.environ["GITHUB_EVENT_PATH"], encoding="utf-8") as event_file:
            validate_pull_request(json.load(event_file))
    except (KeyError, OSError, ValueError) as error:
        print(f"GitFlow policy failed: {error}", file=sys.stderr)
        return 1
    print("GitFlow policy passed.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
