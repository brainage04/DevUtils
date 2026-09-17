import os
import subprocess
import sys
import time
from pathlib import Path
from PIL import ImageGrab

ROOT = Path(__file__).resolve().parents[1]
ENV = dict(os.environ, DISPLAY=':223', PULSE_SINK='devutils3capture')

def run(*args):
    return subprocess.run(args, env=ENV, capture_output=True, text=True, check=True, timeout=5).stdout

if sys.argv[1] == 'capture':
    ImageGrab.grab(xdisplay=':223').save(ROOT / 'evidence' / sys.argv[2])
elif sys.argv[1] == 'command':
    window = run('xdotool', 'search', '--name', 'Minecraft 1.8.9').strip().splitlines()[-1]
    run('xdotool', 'windowfocus', window)
    run('xdotool', 'key', '--clearmodifiers', 't')
    time.sleep(0.4)
    run('xdotool', 'type', '--clearmodifiers', '--delay', '30', sys.argv[2])
    run('xdotool', 'key', '--clearmodifiers', 'Return')
    print('Typed', sys.argv[2], 'on display :223')
