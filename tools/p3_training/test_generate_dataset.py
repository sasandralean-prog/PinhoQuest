from pathlib import Path
import tempfile
import unittest

from generate_dataset import (
    ContractError,
    build_developer_turn,
    build_rows,
    load_contract,
    load_category_seeds,
    validate_completion,
    validate_prompt,
)


class CanonicalTrainingContractTest(unittest.TestCase):
    def setUp(self):
        self.contract = load_contract()

    def test_contract_is_derived_from_kotlin_authority(self):
        self.assertEqual("compose_quest_text", self.contract["name"])
        self.assertEqual(["title", "description", "objectives"], self.contract["required"])

    def test_declaration_marker_is_emitted_once(self):
        prompt = build_developer_turn(self.contract)
        self.assertEqual(1, prompt.count("<start_function_declaration>"))
        self.assertEqual(1, prompt.count("<end_function_declaration>"))

    def test_completion_stops_at_native_call_boundary(self):
        row = build_rows(self.contract)[0]
        validate_completion(row["completion"], self.contract)
        self.assertTrue(row["completion"].endswith("<end_function_call>"))
        self.assertNotIn("<start_function_response>", row["completion"])

    def test_all_rows_match_canonical_protocol(self):
        rows = build_rows(self.contract)
        self.assertEqual(6 * 5 * 3 * 4, len(rows))
        for row in rows:
            validate_prompt(row["prompt"], self.contract)
            validate_completion(row["completion"], self.contract)
            self.assertEqual(self.contract["name"], row["fn"])

    def test_training_uses_the_same_kotlin_seed_catalog_as_runtime(self):
        seeds = load_category_seeds()
        rows = build_rows(self.contract, seeds)

        self.assertEqual(
            {"CODING", "GAMING", "EXPLORATION", "LEARNING", "RANDOM", "CREATIVE"},
            set(seeds),
        )
        for category, seed in seeds.items():
            pt_row = next(
                row for row in rows
                if row["user"].startswith(f"Categoria={category};")
                and "Idioma=pt-BR." in row["user"]
            )
            en_row = next(
                row for row in rows
                if row["user"].startswith(f"Categoria={category};")
                and "Idioma=en-US." in row["user"]
            )
            self.assertIn(seed["facts_pt_br"], pt_row["user"])
            self.assertIn(seed["objective_pt_br"], pt_row["user"])
            self.assertIn(seed["facts_en_us"], en_row["completion"])
            self.assertIn(seed["objective_en_us"], en_row["completion"])

    def test_duplicate_declaration_is_rejected(self):
        row = build_rows(self.contract)[0]
        broken = row["prompt"].replace(
            "<start_function_declaration>",
            "<start_function_declaration><start_function_declaration>",
            1,
        )
        with self.assertRaises(ContractError):
            validate_prompt(broken, self.contract)

    def test_old_response_tail_is_rejected(self):
        row = build_rows(self.contract)[0]
        with self.assertRaises(ContractError):
            validate_completion(
                row["completion"] + "<start_function_response>",
                self.contract,
            )


if __name__ == "__main__":
    unittest.main()
