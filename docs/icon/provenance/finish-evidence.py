import hashlib
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
configs = ['a-1280x720-gui1','b-1600x1000-gui2','c-1920x1080-gui3']
result = {'method':'Independent client launches; real client-side /atlas commands typed with xdotool on isolated Xvfb :223. Stock Minecraft 1.8.9 resources; no resource pack, export postprocessing, image re-encoding, or hash substitution.', 'configurations':[], 'groups':[]}
for size in [64,256]:
    hashes=[]
    for config in configs:
        folder=ROOT/'exports'/(config+'-'+str(size))
        proof=json.loads((folder/'proof.json').read_text())
        actual={name:hashlib.sha256((folder/name).read_bytes()).hexdigest() for name in ('atlas.png','atlas.txt','atlas.css')}
        assert actual == proof['sha256']
        assert proof['mappedCells']==610 and not proof['emptyMappedCells']
        assert proof['size']==[32*size,20*size]
        gui = [line for line in proof['options'].splitlines() if line.startswith('guiScale:')][0]
        result['configurations'].append(dict(configuration=config,command=proof['command'],guiScale=gui,windowGeometry=proof['windowGeometry'],files=actual,dimensions=proof['size'],mappedCells=610,emptyMappedCells=0,evidence=str(folder.relative_to(ROOT))))
        hashes.append(actual)
    assert hashes[0] == hashes[1] == hashes[2]
    result['groups'].append(dict(perItemPixels=size,identicalBytes=True,sha256=hashes[0],configurations=3))
result['geometry']={'oldPitchPx':34.13333333333333,'fixedPitchPx':64,'oldCanvas':[2048,1280],'oldAlphaBounds':[0,0,1092,1214],'fixedCanvas':[2048,1280],'fixedAlphaBounds':[0,0,2048,1268],'all610MappedCellsPopulated':True,'fullPageCells':576,'secondPageMappedCells':34,'unusedFinalRowCells':30,'note':'Transparent margins within natural item silhouettes and the final 30 unmapped cells are intentional, not the former canvas gap. All 610 mappings are populated, including every cell of the first 32x18 page.'}
maximum = json.loads((ROOT/'exports/c-1920x1080-gui3-512/proof.json').read_text())
assert maximum['mappedCells'] == 610 and not maximum['emptyMappedCells']
assert maximum['size'] == [16384,10240]
assert hashlib.sha256((ROOT/'exports/c-1920x1080-gui3-512/atlas.png').read_bytes()).hexdigest() == maximum['sha256']['atlas.png']
result['maximumResolution'] = dict(perItemPixels=512,glMaximumTextureSize=16384,command='/atlas 512',dimensions=maximum['size'],sha256=maximum['sha256'],evidence='exports/c-1920x1080-gui3-512',note='512 = 16384 / 32 is the mod/hardware maximum; /atlas 513 was rejected. The icon uses a separate native /atlas 256 export for exact 1:1 cells, not a downsample of this maximum export.')
(ROOT/'independence-proof.json').write_text(json.dumps(result,indent=2))
manifest=[dict(project='DevUtils',label='DevUtils — fixed native atlas, 16 foundational legacy blocks',path='atlas3/devutils/devutils-legacy-blocks-1024.png',method='Real /atlas 256 export from locally fixed DevUtils; 16 full 256x256 RGBA cells pasted into a 4x4 1024x1024 grid at exactly 1:1. No upscaling, resampling, retouching, or model replacement.',source='atlas3/devutils/exports/b-1600x1000-gui2-256/atlas.png',notes='GUI/resolution independence proven for /atlas 64 and /atlas 256 across 1280x720/gui1, 1600x1000/gui2, 1920x1080/gui3. All 610 mapped cells populated. Native icon scale factor: 1.0. See independence-proof.json and icon-proof.json.')]
(ROOT/'manifest.json').write_text(json.dumps(dict(round=3,iteration=4,project_dir='atlas3/devutils',entries=manifest),indent=2))
(ROOT/'blockers.json').write_text('[]\n')
print(json.dumps(result['groups'],indent=2))
