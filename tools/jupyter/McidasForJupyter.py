from pyvirtualdisplay import Display
import subprocess
from IPython.display import Image, display
import shutil
import hashlib
import inspect
import numpy as np
import pandas as pd
import textwrap
import time
import os

class McIDAS: 
    
    def __init__(self, mcv_path): 
        self.mcv_path = mcv_path
        self.make_tmp_folder()

        self.script_buffer = []
        self.last_image = ""

    def make_tmp_folder(self):
        path = ".tmp"
        if os.path.exists(path): shutil.rmtree(path)
        os.makedirs(path, exist_ok=True)

    def add_command_single(self, cmd): 
        self.script_buffer.append(cmd)

    def add_command_block(self, cmd): 
        lines = cmd.split('\n')
        for line in lines: self.add_command_single(line)

    def purge_buffer(self):
        self.script_buffer = []

    def capture_display(self, type):
        abs_path = os.path.abspath(".tmp")

        current_time = str(time.time())
        hashed_time = hashlib.sha256(current_time.encode()).hexdigest()
        
        path_to_image = ""
        cmd = ""

        if type == "png":
            path_to_image = abs_path + "/" + hashed_time[:16] + ".png"
            cmd = "panel[0].captureImage(\'" + path_to_image +"\', width=800, height=600)\n"
        
        
        self.last_image = path_to_image
        self.add_command_single(cmd)    

    def execute_buffer(self, show_display = True):
        
        if show_display: self.capture_display("png")

        script = "\n".join(self.script_buffer + [""])
        
        current_time = str(time.time())
        hashed_time = hashlib.sha256(current_time.encode()).hexdigest()

        script_name = ".tmp/"+ f"{hashed_time[:16]}.py"

        with open(script_name, 'w') as file: file.write(script)
        
        abs_path = os.path.abspath(script_name)

        subprocess.run([self.mcv_path, "-script", abs_path])
        
        if show_display: display(Image(filename=self.last_image))

    def mcv_command_old(self, func):
        code = inspect.getsource(func)
        lines = code.strip("\n").splitlines()
        body_lines = lines[2:]
        dedented = textwrap.dedent("\n".join(body_lines))
        self.add_command_block(dedented)
        self.execute_buffer()
        return func


    def mcv_command(self, func):
        import ast, inspect, textwrap, types, datetime
        from pathlib import Path

        def _safe_literal(value):
            if value is None or isinstance(value, (bool, int, float, str)): return repr(value)
            if isinstance(value, (list, tuple)): return repr(type(value)(value))
            if isinstance(value, dict): return repr(value)
            if isinstance(value, Path): return repr(str(value))
            if isinstance(value, datetime.datetime): return repr(value.isoformat())
            if isinstance(value, datetime.date): return repr(value.isoformat())
            
            try:
                if isinstance(value, np.ndarray): return repr(value.tolist())
            except Exception: pass
    
            try:
                if isinstance(value, pd.Series): return repr(value.tolist())
                if isinstance(value, pd.DataFrame): return repr(value.to_dict(orient="list"))
            
            except Exception: pass

            if hasattr(value, "tolist") and callable(value.tolist):
                try: return repr(value.tolist())
                except Exception: pass

            raise ValueError("unsupported type: " + type(value).__name__)

        def _names_used_in_body(source):
            tree = ast.parse(source)
            names = set()
            assigned = set()
            params = set()
            class Finder(ast.NodeVisitor):
                def visit_Name(self, node):
                    if isinstance(node.ctx, ast.Load): names.add(node.id)
                    elif isinstance(node.ctx, (ast.Store, ast.Del)): assigned.add(node.id)
                    self.generic_visit(node)

                def visit_FunctionDef(self, node):
                    for arg in node.args.args: params.add(arg.arg)
                    if node.args.vararg: params.add(node.args.vararg.arg)
                    if node.args.kwarg: params.add(node.args.kwarg.arg)
                    
                    for dec in node.decorator_list: self.visit(dec)

                def visit_ClassDef(self, node):
                    for base in node.bases: self.visit(base)
                    for dec in node.decorator_list: self.visit(dec)

                def visit_Import(self, node):
                    for alias in node.names:
                        assigned.add(alias.asname or alias.name.split('.')[0])

                def visit_ImportFrom(self, node):
                    for alias in node.names:
                        assigned.add(alias.asname or alias.name)

                def visit_For(self, node):
                    if isinstance(node.target, ast.Name): assigned.add(node.target.id)
                    self.generic_visit(node)

                def visit_With(self, node):
                    for item in node.items:
                        if item.optional_vars and isinstance(item.optional_vars, ast.Name):
                            assigned.add(item.optional_vars.id)
                    self.generic_visit(node)

            Finder().visit(tree)
            return names - assigned - params
        

        code = inspect.getsource(func)
        lines = code.strip("\n").splitlines()
        
        first_def = None
        
        for i, line in enumerate(lines):
            if line.lstrip().startswith("def "):
                first_def = i
                break
        
        if first_def is None:
            raise RuntimeError("could not find function definition")
        
        body_lines = lines[first_def + 1:]
        dedented = textwrap.dedent("\n".join(body_lines))

        used_names = _names_used_in_body(dedented)

        literal_assignments = {}
        debug_msgs = []

        if func.__closure__:
            for name, cell in zip(func.__code__.co_freevars, func.__closure__):
                if name in used_names:
                    try:
                        literal_assignments[name] = _safe_literal(cell.cell_contents)
                        debug_msgs.append(f"captured closure {name}")
                    except Exception as e:
                        debug_msgs.append(f"skipped closure {name}: {e}")

        for name in sorted(used_names):
            if name in literal_assignments:
                continue
            if name in func.__globals__:
                val = func.__globals__[name]
                import types
                if isinstance(val, types.ModuleType): 
                    debug_msgs.append(f"skipping module {name}")
                    continue
                if isinstance(val, (types.FunctionType, type)):
                    debug_msgs.append(f"skipping func/class {name}")
                    continue
                try:
                    literal_assignments[name] = _safe_literal(val)
                    debug_msgs.append(f"captured global {name}")
                except Exception as e:
                    debug_msgs.append(f"skipped global {name}: {e}")

        header_lines = []
        for k, literal in literal_assignments.items():
            header_lines.append(f"{k} = {literal}")
        header = "\n".join(header_lines)
        for m in debug_msgs:
            header = f"# {m}\n" + header if header else f"# {m}"

        full_code = f"{header}\n{dedented}" if header else dedented
        self.add_command_block(full_code)
        self.execute_buffer()
        return func
