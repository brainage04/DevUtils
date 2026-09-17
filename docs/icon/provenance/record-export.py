import hashlib
import json
import os
import re
import shutil
import subprocess
import sys
from pathlib import Path
from PIL import Image, ImageGrab
ROOT = Path(__file__).resolve().parents[1]
name, size = sys.argv[1], int(sys.argv[2])
dest = ROOT / 'exports' / name
dest.mkdir(exist_ok=True)
for ext in ('png','txt','css'):
    shutil.copy2(ROOT / 'runtime' / ('atlas.'+ext), dest / ('atlas.'+ext))
im = Image.open(dest / 'atlas.png').convert('RGBA')
entries = []
for line in (dest/'atlas.txt').read_text().splitlines():
    match = re.match(r'(.+): \[(\d+), (\d+)\]',line)
    label, x, y = match.group(1), int(match.group(2)), int(match.group(3))
    box = (x*size,y*size,(x+1)*size,(y+1)*size)
    cell = im.crop(box)
    entries.append(dict(label=label,x=x,y=y,box=box,alphaBounds=cell.getchannel('A').getbbox(),pixelsSha256=hashlib.sha256(cell.tobytes()).hexdigest()))
env = dict(os.environ,DISPLAY=':223')
window = subprocess.run(['xdotool','search','--name','Minecraft 1.8.9'],env=env,capture_output=True,text=True,check=True,timeout=5).stdout.strip().splitlines()[-1]
geometry = subprocess.run(['xdotool','getwindowgeometry','--shell',window],env=env,capture_output=True,text=True,check=True,timeout=5).stdout
options = (ROOT/'runtime/options.txt').read_text()
proof = dict(command='/atlas '+str(size),size=im.size,perItem=size,windowGeometry=geometry,options=options,sha256={p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in dest.glob('atlas.*')},mappedCells=len(entries),emptyMappedCells=[e for e in entries if e['alphaBounds'] is None],alphaBounds=im.getchannel('A').getbbox(),entries=entries)
(dest/'proof.json').write_text(json.dumps(proof,indent=2))
shutil.copy2(ROOT/'runtime/logs/latest.log',dest/'client.log')
ImageGrab.grab(xdisplay=':223').save(dest/'command-screen.png')
im.thumbnail((1024,1024))
im.save(dest/'preview.png')
print(json.dumps({k:v for k,v in proof.items() if k not in ('entries','options')},indent=2))
