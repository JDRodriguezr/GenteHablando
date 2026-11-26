import unittest

from gitflow_policy import validate_pull_request


def event(source="feature/chat-errors", target="develop", fork=False):
    return {
        "repository": {"id": 1},
        "pull_request": {
            "head": {"ref": source, "repo": {"id": 2 if fork else 1}},
            "base": {"ref": target, "repo": {"id": 1}},
        },
    }


class GitFlowPolicyTest(unittest.TestCase):
    def test_accepted_pairings(self):
        for source, target, fork in [
            ("feature/chat-errors", "develop", False),
            ("feature/chat-errors", "develop", True),
            ("feature/123", "develop", True),
            ("release/0.1.0", "main", False),
            ("release/12.10.3", "main", False),
            ("main", "develop", False),
        ]:
            with self.subTest(source=source, target=target, fork=fork):
                validate_pull_request(event(source, target, fork))

    def test_rejected_pairings(self):
        for source, target, fork in [
            ("feature/chat-errors", "main", False),
            ("release/0.1.0", "develop", False),
            ("release/0.1.0", "main", True),
            ("main", "develop", True),
            ("main", "main", False),
            ("develop", "main", False),
            ("develop", "develop", False),
            ("hotfix/chat-errors", "main", False),
            ("hotfix/chat-errors", "develop", False),
            ("feature/chat-errors", "preview", False),
        ]:
            with self.subTest(source=source, target=target, fork=fork):
                with self.assertRaises(ValueError):
                    validate_pull_request(event(source, target, fork))

    def test_invalid_feature_names(self):
        for source in [
            "future/chat-errors", "fix/chat-errors", "nataliacor-chat-errors",
            "feature/", "feature/Chat", "feature/user/chat", "feature/-chat",
            "feature/chat-", "feature/chat--errors", "feature/chat_errors",
            "feature/chat.errors", "feature/chat errors", "feature/chat\n",
            "feature/chat;echo unsafe", "feature/$(echo unsafe)", "feature/caf\u00e9",
        ]:
            with self.subTest(source=source):
                with self.assertRaises(ValueError):
                    validate_pull_request(event(source))

    def test_invalid_release_names(self):
        for source in [
            "release/", "release/1", "release/1.0", "release/1.0.0.0",
            "release/01.0.0", "release/1.00.0", "release/1.0.00",
            "release/-1.0.0", "release/1.0.0-alpha", "release/1.0.0\n",
            "release/v1.0.0", "release/1.0.0/extra",
        ]:
            with self.subTest(source=source):
                with self.assertRaises(ValueError):
                    validate_pull_request(event(source, "main"))

    def test_missing_or_malformed_event(self):
        for malformed in [
            None, [], "pull_request", {}, {"pull_request": None},
            {"pull_request": []}, {"pull_request": {}},
        ]:
            with self.subTest(metadata=malformed):
                with self.assertRaises(ValueError):
                    validate_pull_request(malformed)

    def test_missing_or_malformed_branch_metadata(self):
        for side in ("head", "base"):
            for malformed in (None, [], "main", {}, {"repo": {"id": 1}}):
                with self.subTest(side=side, metadata=malformed):
                    data = event()
                    data["pull_request"][side] = malformed
                    with self.assertRaises(ValueError):
                        validate_pull_request(data)
            for ref in (None, 1, True, [], {}, ""):
                with self.subTest(side=side, ref=ref):
                    data = event()
                    data["pull_request"][side]["ref"] = ref
                    with self.assertRaises(ValueError):
                        validate_pull_request(data)
            data = event()
            del data["pull_request"][side]
            with self.subTest(side=side, missing=True):
                with self.assertRaises(ValueError):
                    validate_pull_request(data)

    def test_missing_or_malformed_repository_metadata(self):
        repositories = [None, [], "owner/repo", {}, {"id": None}, {"id": True},
                        {"id": 1.0}, {"id": "1"}, {"id": 0}, {"id": -1}]
        for location in ("event", "head", "base"):
            for repository in repositories:
                with self.subTest(location=location, repository=repository):
                    data = event()
                    if location == "event":
                        data["repository"] = repository
                    else:
                        data["pull_request"][location]["repo"] = repository
                    with self.assertRaises(ValueError):
                        validate_pull_request(data)
            data = event()
            if location == "event":
                del data["repository"]
            else:
                del data["pull_request"][location]["repo"]
            with self.subTest(location=location, missing=True):
                with self.assertRaises(ValueError):
                    validate_pull_request(data)

    def test_event_repository_must_match_pr_target(self):
        data = event()
        data["repository"]["id"] = 3
        with self.assertRaises(ValueError):
            validate_pull_request(data)


if __name__ == "__main__":
    unittest.main()
