from pyvirtualdisplay import Display
import subprocess
from IPython.display import Image, display
import shutil
import hashlib
import inspect
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

    def mcv_command(self, func):
        code = inspect.getsource(func)
        lines = code.strip("\n").splitlines()
        body_lines = lines[2:]
        dedented = textwrap.dedent("\n".join(body_lines))
        self.add_command_block(dedented)
        self.execute_buffer()
        return func
