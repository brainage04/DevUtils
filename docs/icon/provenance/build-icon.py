import hashlib
import json
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / 'exports/b-1600x1000-gui2-256'
proof = json.loads((SOURCE / 'proof.json').read_text())
atlas = Image.open(SOURCE / 'atlas.png').convert('RGBA')
blocks = ['GRASS', 'DIRT', 'STONE', 'COBBLESTONE',
          'PLANKS', 'LOG', 'LEAVES', 'GLASS',
          'SAND', 'GRAVEL', 'BRICK_BLOCK', 'BOOKSHELF',
          'CRAFTING_TABLE', 'FURNACE', 'CHEST', 'OBSIDIAN']
icon = Image.new('RGBA', (1024,1024))
selections=[]
for i, block in enumerate(blocks):
    entry = next(e for e in proof['entries'] if e['label'].endswith('('+block+')'))
    cell = atlas.crop(entry['box'])
    assert cell.size == (256,256)
    box = (i%4*256,i//4*256,i%4*256+256,i//4*256+256)
    icon.paste(cell, box[:2])  # No mask: retain all exported RGBA bytes exactly.
    assert icon.crop(box).tobytes() == cell.tobytes()
    selections.append(dict(block=block,label=entry['label'],sourceBox=entry['box'],iconBox=box,pixelsSha256=hashlib.sha256(cell.tobytes()).hexdigest(),scaleFactor=1.0))
icon.save(ROOT/'devutils-legacy-blocks-1024.png')
check=Image.open(ROOT/'devutils-legacy-blocks-1024.png').convert('RGBA')
for selection in selections:
    assert check.crop(selection['iconBox']).tobytes() == atlas.crop(selection['sourceBox']).tobytes()
result=dict(source=str(SOURCE.relative_to(ROOT)/'atlas.png'),sourceSha256=proof['sha256']['atlas.png'],icon='devutils-legacy-blocks-1024.png',iconSha256=hashlib.sha256((ROOT/'devutils-legacy-blocks-1024.png').read_bytes()).hexdigest(),size=check.size,layout='4x4',cellSize=[256,256],scaleFactor=1.0,resampling='none',compositing='unmasked RGBA paste, every saved cell byte-identical to original export cell',selections=selections)
(ROOT/'icon-proof.json').write_text(json.dumps(result,indent=2))
print(json.dumps({k:v for k,v in result.items() if k!='selections'},indent=2))
