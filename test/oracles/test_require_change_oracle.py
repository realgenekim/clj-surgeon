import unittest
from require_change_oracle import grade_bytes

SEED = b'''(ns a
  (:require
   [clojure.data.json :as json] ; trailing stays
   ;; attached to z
   [z :as z]))
(def body "unchanged")
'''
ADDED = b'   [marvin-voice-remote.json :as mjson]\n'
EXPECTED = SEED.replace(b'   ;; attached to z', ADDED + b'   ;; attached to z')
GOLDEN = SEED.replace(b'   [clojure.data.json', ADDED + b'   [clojure.data.json')


class RequireOracleTest(unittest.TestCase):
    # @spec REQUIRE-CHANGE-014
    def test_exact_candidate_and_historical_position(self):
        self.assertEqual([], grade_bytes(SEED, EXPECTED, GOLDEN))

    # @spec REQUIRE-CHANGE-014
    def test_independent_mutant_matrix(self):
        mutants = [
            EXPECTED.replace(b'; trailing stays', b'; changed'),
            EXPECTED.replace(b'   ;; attached to z\n', b''),
            SEED.replace(b'   [z', ADDED + b'   [z'),
            EXPECTED.replace(b'unchanged', b'changed'),
            EXPECTED.replace(ADDED, b'\n' + ADDED),
            EXPECTED.replace(ADDED, b' ' + ADDED),
            EXPECTED.replace(b':as mjson', b':as json'),
            EXPECTED.replace(ADDED, ADDED + ADDED),
            EXPECTED.replace(b'[clojure.data.json :as json]', b'[wrong :as json]'),
            SEED,
        ]
        for candidate in mutants:
            with self.subTest(candidate=candidate):
                self.assertTrue(grade_bytes(SEED, candidate, GOLDEN))

    # @spec REQUIRE-CHANGE-014
    def test_historical_hanging_target_is_explicit(self):
        golden = b'(ns a\n  (:require [marvin-voice-remote.json :as mjson]\n            [clojure.data.json :as json]\n            [z :as z]))\n'
        seed = b'(ns a\n  (:require\n            [clojure.data.json :as json]\n            [z :as z]))\n'
        candidate = seed.replace(b'            [z', b'            [marvin-voice-remote.json :as mjson]\n            [z')
        self.assertEqual([], grade_bytes(seed, candidate, golden))

    # @spec REQUIRE-CHANGE-014
    def test_natural_codex_append_conflicts_with_whole_line_preservation(self):
        from pathlib import Path
        fixture = Path(__file__).resolve().parents[2] / 'test-fixtures/require-change'
        seed = (fixture/'seed/codex_app_server.clj').read_bytes()
        golden = (fixture/'golden/codex_app_server.clj').read_bytes()
        moved_closer = seed.replace(b'[clojure.string :as str])',
                                   b'[clojure.string :as str]\n   [marvin-voice-remote.json :as mjson])')
        outside_clause = seed.replace(b'[clojure.string :as str])\n',
                                      b'[clojure.string :as str])\n' + ADDED)
        for candidate in [golden, moved_closer, outside_clause]:
            with self.subTest(candidate=candidate):
                self.assertTrue(grade_bytes(seed, candidate, golden))
