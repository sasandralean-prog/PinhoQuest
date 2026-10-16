"""Generate FunctionGemma SFT fixtures from canonical Kotlin contracts.

The Kotlin MicroQuestToolContract defines the native function schema, while
MicroQuestCategorySeedCatalog defines the same approved category seeds used at
runtime. Python derives both contracts instead of maintaining duplicate values.
"""

from __future__ import annotations

import json
import random
import re
from pathlib import Path
from tempfile import TemporaryDirectory


DEFAULT_CONTRACT = (
    Path(__file__).resolve().parents[2]
    / "quest-core/src/main/kotlin/com/pinhoquest/core/inference/micro/MicroQuestToolContract.kt"
)

DEFAULT_SEED_CATALOG = (
    Path(__file__).resolve().parents[2]
    / "quest-core/src/main/kotlin/com/pinhoquest/core/inference/micro/MicroQuestCategorySeedCatalog.kt"
)
TRAINING_CATEGORIES = ("CODING", "GAMING", "EXPLORATION", "LEARNING", "RANDOM", "CREATIVE")


class ContractError(ValueError):
    pass


def _read_constants(source: str) -> dict[str, str]:
    constants: dict[str, str] = {}
    for name, value in re.findall(r'const val (\w+) = "([^"]*)"', source):
        constants[name] = value

    integer_constants: dict[str, int] = {}
    for name, value in re.findall(r"const val (\w+) = (\d+)", source):
        integer_constants[name] = int(value)

    return {**constants, **integer_constants}


def _required_arguments(source: str, constants: dict[str, object]) -> list[dict[str, object]]:
    block = re.search(r"val ARGUMENTS: List<Argument> = listOf\((.*?)\n    \)", source, re.S)
    if not block:
        raise ContractError("ARGUMENTS block not found")

    entries = re.findall(r"Argument\((.*?)\n        \),", block.group(1), re.S)
    arguments: list[dict[str, object]] = []

    for entry in entries:
        refs = re.findall(r"^\s*(\w+) = ([A-Z_]+|true|false|ArgumentType\.\w+),?$", entry, re.M)
        values = {key: value for key, value in refs}
        name_ref = values.get("name")
        description_ref = values.get("description")
        type_ref = values.get("type")
        if not name_ref or not type_ref:
            raise ContractError(f"Could not parse argument block: {entry!r}")

        if name_ref not in constants:
            raise ContractError(f"Unknown contract constant: {name_ref}")
        if description_ref not in constants:
            raise ContractError(f"Unknown contract description constant: {description_ref}")

        arg_type = type_ref.removeprefix("ArgumentType.")
        argument = {
            "name": constants[name_ref],
            "description": constants[description_ref],
            "type": arg_type,
            "required": values.get("required") == "true",
        }

        for field in (
            "minLength",
            "maxLength",
            "minItems",
            "maxItems",
            "itemMinLength",
            "itemMaxLength",
        ):
            ref = values.get(field)
            if ref and ref in constants:
                argument[field] = constants[ref]

        arguments.append(argument)

    if not arguments:
        raise ContractError("No canonical arguments parsed")
    return arguments


def load_contract(path: Path = DEFAULT_CONTRACT) -> dict[str, object]:
    source = path.read_text(encoding="utf-8")
    constants = _read_constants(source)
    arguments = _required_arguments(source, constants)

    required = [arg["name"] for arg in arguments if arg["required"]]
    if not required:
        raise ContractError("Canonical contract has no required arguments")

    return {
        "name": constants["NAME"],
        "version": constants["VERSION"],
        "description": constants["DESCRIPTION"],
        "arguments": arguments,
        "required": required,
    }


def load_category_seeds(path: Path = DEFAULT_SEED_CATALOG) -> dict[str, dict[str, str]]:
    source = path.read_text(encoding="utf-8")
    constants = dict(re.findall(r'const val (\w+) = "([^"]*)"', source))
    fields = ("facts_pt_br", "objective_pt_br", "facts_en_us", "objective_en_us")
    suffixes = {
        "facts_pt_br": "FACTS_PT_BR",
        "objective_pt_br": "OBJECTIVE_PT_BR",
        "facts_en_us": "FACTS_EN_US",
        "objective_en_us": "OBJECTIVE_EN_US",
    }
    seeds: dict[str, dict[str, str]] = {}
    for category in TRAINING_CATEGORIES:
        category_seed: dict[str, str] = {}
        for field in fields:
            constant = f"{category}_{suffixes[field]}"
            if constant not in constants:
                raise ContractError(f"Missing Kotlin category seed constant: {constant}")
            category_seed[field] = constants[constant]
        seeds[category] = category_seed
    return seeds


def _escape(value: str) -> str:
    return f"<escape>{value}<escape>"


def _schema_type(argument_type: str) -> str:
    if argument_type == "STRING":
        return "STRING"
    if argument_type == "STRING_LIST":
        return "ARRAY"
    raise ContractError(f"Unsupported canonical argument type: {argument_type}")


def build_function_declaration(contract: dict[str, object]) -> str:
    properties: list[str] = []
    for argument in contract["arguments"]:
        properties.append(
            f"{argument['name']}:{_escape(str(argument['description']))},"
            f"type:{_escape(_schema_type(argument['type']))}"
        )

    required = ",".join(_escape(name) for name in contract["required"])
    return (
        f"declaration:{contract['name']}{{"
        f"description:{_escape(str(contract['description']))},"
        f"parameters:{{properties:{{{','.join(properties)}}},"
        f"required:[{required}],type:<escape>OBJECT<escape>}}}}"
        f"<end_function_declaration>"
    )


def build_developer_turn(contract: dict[str, object]) -> str:
    declaration = build_function_declaration(contract)
    return (
        "<bos><start_of_turn>developer\n"
        "You compose micro-quest text from deterministic facts. "
        f"Call exactly {contract['name']} once. Do not invent facts, XP, "
        "rewards, rules, or metadata. Fill the canonical tool arguments."
        "<start_function_declaration>"
        + declaration
        + "<end_of_turn>\n<start_of_turn>user\n"
    )


def validate_completion(completion: str, contract: dict[str, object]) -> None:
    call_start = f"<start_function_call>call:{contract['name']}{{"
    if completion.count(call_start) != 1:
        raise ContractError("completion must contain exactly one canonical function call")
    if completion.count("<end_function_call>") != 1:
        raise ContractError("completion must contain exactly one function-call terminator")
    if "<start_function_response>" in completion:
        raise ContractError("completion must stop at the native function-call boundary")
    if not completion.endswith("<end_function_call>"):
        raise ContractError("completion must end at <end_function_call>")


def validate_prompt(prompt: str, contract: dict[str, object]) -> None:
    marker = "<start_function_declaration>"
    if prompt.count(marker) != 1:
        raise ContractError("prompt must contain exactly one declaration-start marker")
    if prompt.count("<end_function_declaration>") != 1:
        raise ContractError("prompt must contain exactly one declaration-end marker")
    if f"declaration:{contract['name']}{{" not in prompt:
        raise ContractError("prompt must contain the canonical tool declaration")


def build_rows(
    contract: dict[str, object],
    category_seeds: dict[str, dict[str, str]] | None = None,
) -> list[dict[str, str]]:
    if category_seeds is None:
        category_seeds = load_category_seeds()
    fn = str(contract["name"])
    developer = build_developer_turn(contract)
    categories = [
        (
            category,
            category_seeds[category]["facts_pt_br"],
            category_seeds[category]["objective_pt_br"],
        )
        for category in TRAINING_CATEGORIES
    ]
    envs = ["HOME", "DESKTOP", "OUTDOOR", "PUBLIC", "ANY"]
    diffs = ["EASY", "MEDIUM", "HARD"]
    langs = ["pt-BR", "pt-BR", "pt-BR", "en-US"]
    titles_pt = {
        "CODING": "Frankenstein Digital",
        "GAMING": "Jogue de outro jeito",
        "EXPLORATION": "Caca a detalhes",
        "LEARNING": "Curiosidade de bolso",
        "RANDOM": "Pequeno caos controlado",
        "CREATIVE": "Tres coisas viram uma",
    }
    titles_en = {
        "CODING": "Digital Frankenstein",
        "GAMING": "Play It Differently",
        "EXPLORATION": "Detail Hunt",
        "LEARNING": "Pocket Curiosity",
        "RANDOM": "Controlled Chaos",
        "CREATIVE": "Three Become One",
    }
    rows: list[dict[str, str]] = []

    for cat, desc, obj in categories:
        seed = category_seeds[cat]
        for env in envs:
            for diff in diffs:
                for lang in langs:
                    time = "10-20" if diff == "EASY" else "15-30" if diff == "MEDIUM" else "20-40"
                    title = titles_en[cat] if lang == "en-US" else titles_pt[cat]
                    output_description = seed["facts_en_us"] if lang == "en-US" else desc
                    output_objective = seed["objective_en_us"] if lang == "en-US" else obj
                    user = (
                        f"Categoria={cat}; Ambiente={env}; Dificuldade={diff}; "
                        f"Tempo={time}; Idioma={lang}. Fatos aprovados: {desc} "
                        f"Objetivo aprovado: {obj}"
                    )
                    args = (
                        f"title:{_escape(title)},"
                        f"description:{_escape(output_description)},"
                        f"objectives:[{_escape(output_objective)}]"
                    )
                    completion = f"<start_function_call>call:{fn}{{{args}}}<end_function_call>"
                    prompt = developer + user + "<end_of_turn>\n<start_of_turn>model\n"
                    validate_prompt(prompt, contract)
                    validate_completion(completion, contract)
                    rows.append({"prompt": prompt, "completion": completion, "fn": fn, "user": user})

    return rows


def generate(output_dir: Path, seed: int = 42) -> tuple[int, int, int]:
    contract = load_contract()
    rows = build_rows(contract)
    random.Random(seed).shuffle(rows)
    cut = int(len(rows) * 0.8)
    output_dir.mkdir(parents=True, exist_ok=True)

    for name, data in (("train", rows[:cut]), ("val", rows[cut:])):
        with (output_dir / f"{name}.jsonl").open("w", encoding="utf-8") as handle:
            for row in data:
                handle.write(json.dumps(row, ensure_ascii=False) + "\n")

    return len(rows), cut, len(rows) - cut


if __name__ == "__main__":
    destination = Path(r"D:\AI\HuggingFacesLLM\p3_sft")
    total, train, val = generate(destination)
    print(f"rows {total} train {train} val {val}")
