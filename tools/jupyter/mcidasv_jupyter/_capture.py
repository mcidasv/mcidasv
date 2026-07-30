"""Turn a Python function into a McIDAS-V script string.

Used by :meth:`McIDASV.command`.  The function's *body* becomes the script, and
any Python literal values it references from the enclosing scope (closure cells
and module globals) are emitted as assignments at the top so parameters can be
shared between Python and Jython.  Non-literal references -- modules, functions,
classes -- are skipped, since they cannot be serialised into a standalone script.
"""

from __future__ import annotations

import ast
import datetime
import inspect
import textwrap
import types
from pathlib import Path
from typing import Set

__all__ = ["build_script_from_function"]


def _safe_literal(value) -> str:
    """Return a ``repr`` of *value* if it is a serialisable literal, else raise."""
    if value is None or isinstance(value, (bool, int, float, str)):
        return repr(value)
    if isinstance(value, (list, tuple, set)):
        return repr(value)
    if isinstance(value, dict):
        return repr(value)
    if isinstance(value, Path):
        return repr(str(value))
    if isinstance(value, (datetime.datetime, datetime.date)):
        return repr(value.isoformat())

    # numpy / pandas are optional; only touch them if the value quacks right.
    if hasattr(value, "tolist") and callable(value.tolist):
        try:
            return repr(value.tolist())
        except Exception:  # pragma: no cover - defensive
            pass
    raise ValueError("unsupported type: {}".format(type(value).__name__))


def _names_loaded_in_body(source: str) -> Set[str]:
    """Names that are *read* in *source* without being locally bound first."""
    tree = ast.parse(source)
    loaded: Set[str] = set()
    bound: Set[str] = set()

    class Finder(ast.NodeVisitor):
        def visit_Name(self, node):
            if isinstance(node.ctx, ast.Load):
                loaded.add(node.id)
            else:  # Store / Del
                bound.add(node.id)
            self.generic_visit(node)

        def visit_arg(self, node):
            bound.add(node.arg)

        def visit_Import(self, node):
            for alias in node.names:
                bound.add(alias.asname or alias.name.split(".")[0])

        def visit_ImportFrom(self, node):
            for alias in node.names:
                bound.add(alias.asname or alias.name)

        def visit_For(self, node):
            self._bind_target(node.target)
            self.generic_visit(node)

        def visit_With(self, node):
            for item in node.items:
                if item.optional_vars is not None:
                    self._bind_target(item.optional_vars)
            self.generic_visit(node)

        def _bind_target(self, target):
            if isinstance(target, ast.Name):
                bound.add(target.id)
            elif isinstance(target, (ast.Tuple, ast.List)):
                for elt in target.elts:
                    self._bind_target(elt)

    Finder().visit(tree)
    return loaded - bound


def _function_body_source(func) -> str:
    """Return the dedented body of *func* (everything after the ``def`` line)."""
    source = inspect.getsource(func)
    lines = source.strip("\n").splitlines()
    first_def = next(
        (i for i, line in enumerate(lines) if line.lstrip().startswith("def ")),
        None)
    if first_def is None:
        raise ValueError("could not find a function definition to convert")
    body = "\n".join(lines[first_def + 1:])
    return textwrap.dedent(body)


def build_script_from_function(func) -> str:
    """Build a McIDAS-V script string from *func*'s body and captured literals."""
    body = _function_body_source(func)
    used = _names_loaded_in_body(body)

    assignments = {}

    # Closure variables first...
    if func.__closure__:
        for name, cell in zip(func.__code__.co_freevars, func.__closure__):
            if name in used:
                try:
                    assignments[name] = _safe_literal(cell.cell_contents)
                except (ValueError, Exception):
                    pass

    # ...then module globals for anything still unresolved.
    for name in sorted(used):
        if name in assignments:
            continue
        if name in func.__globals__:
            value = func.__globals__[name]
            if isinstance(value, (types.ModuleType, types.FunctionType, type)):
                continue
            try:
                assignments[name] = _safe_literal(value)
            except (ValueError, Exception):
                pass

    header = "\n".join("{} = {}".format(name, literal)
                       for name, literal in assignments.items())
    return "{}\n{}".format(header, body) if header else body
