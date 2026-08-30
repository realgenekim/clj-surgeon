import unittest

from bench.consumption_gap.classify import classify_episode


def episode(**overrides):
    value = {
        "episode_id": "e1",
        "caller_model": "sol",
        "reread": True,
        "refusal": {
            "owner_names_complete": True,
            "owner_names": ["alpha", "beta", "target"],
            "location_rows": [
                {"owner": "alpha", "locator": "f:1"},
                {"owner": "beta", "locator": "f:2"},
            ],
            "location_rows_capped": True,
            "required_selector": "target",
            "answer_token": "target",
            "answer_unique": True,
        },
        "recovery_read": {
            "evidence_complete": True,
            "owner_names": ["alpha", "beta", "target"],
            "location_rows": [],
            "duplicate_groups": [],
            "resolved_duplicate": False,
            "semantic_kinds": ["owner-name"],
        },
    }
    for key, replacement in overrides.items():
        value[key] = replacement
    return value


class ClassifierTest(unittest.TestCase):
    def test_same_names_only_with_verbatim_answer_supports_habit(self):
        result = classify_episode(episode())
        self.assertEqual("same-owner-names-only", result["classification"])
        self.assertEqual("yes", result["verbatim_consumable"])

    def test_new_row_for_owner_omitted_by_cap_supports_location(self):
        value = episode()
        value["recovery_read"]["semantic_kinds"] = ["owner-name", "owner-location"]
        value["recovery_read"]["location_rows"] = [
            {"owner": "target", "locator": "f:30"}
        ]
        self.assertEqual("location-beyond-cap", classify_episode(value)["classification"])

    def test_duplicate_disambiguation_has_precedence(self):
        value = episode()
        value["recovery_read"]["duplicate_groups"] = [
            {"name": "target", "candidate_locators": ["f:30", "g:8"]}
        ]
        self.assertEqual(
            "duplicate-name-disambiguation", classify_episode(value)["classification"]
        )

    def test_source_body_is_other_not_habit(self):
        value = episode()
        value["recovery_read"]["semantic_kinds"] = ["owner-name", "source-body"]
        self.assertEqual("other", classify_episode(value)["classification"])

    def test_missing_result_evidence_is_retained_as_unclassifiable(self):
        value = episode()
        value["recovery_read"]["evidence_complete"] = False
        self.assertEqual("unclassifiable", classify_episode(value)["classification"])

    def test_nonunique_answer_is_not_verbatim_consumable(self):
        value = episode()
        value["refusal"]["answer_unique"] = False
        self.assertEqual("no", classify_episode(value)["verbatim_consumable"])


if __name__ == "__main__":
    unittest.main()
