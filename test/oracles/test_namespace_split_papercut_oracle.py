"""Regression for oracle baseline attribution, including increased multiplicity."""
import unittest
from namespace_split_papercut_oracle import partition_baseline


# INTENT-TEST: NS-SPLIT-031
class BaselineAttributionTest(unittest.TestCase):
    def test_only_matching_baseline_multiplicity_is_advisory(self):
        old = dict(file="server.clj", type="unused-namespace", message="unused a", line=1)
        moved = dict(old, line=20)
        new = dict(old, message="unused b")
        self.assertEqual(([moved, new], [moved]),
                         partition_baseline([moved, moved, new], [old]))
        self.assertEqual(([dict(old, file="destination.clj")], []),
                         partition_baseline([dict(old, file="destination.clj")], [old]))


if __name__ == "__main__":
    unittest.main()
