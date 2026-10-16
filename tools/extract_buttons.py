from PIL import Image
from pathlib import Path
import numpy as np
im=Image.open(r'C:\Users\rafae\AppData\Local\Temp\codex-file-preview-kiAxYg\buttons.png').convert('RGBA')
a=np.array(im.getchannel('A'))
# Manually classified rectangles from the supplied canonical contact sheet, checked against alpha.
boxes={
'BtnContinue.png':(25,20,300,150),
'BtnSeeQuest.png':(360,20,742,150),
'BtnBack.png':(925,40,1017,150),
'BtnSortQuest.png':(295,152,812,289),
'BtnQuestRandom.png':(297,314,542,444),
'BtnQuestGame.png':(554,314,808,444),
'BtnTagMusica.png':(300,464,539,621),
'BtnTagFotografia.png':(555,464,800,621),
'BtnTagNatureza.png':(816,464,1038,621),
'BtnTagTecnologia.png':(299,643,542,796),
'BtnTagAnimais.png':(555,643,800,796),
'BtnTagAventuras.png':(816,643,1038,796),
'BtnTagRelaxar.png':(299,822,542,979),
'BtnTagCriar.png':(555,822,800,979),
'BtnTagFantasia.png':(816,822,1038,979),
'BtnUnknown.png':(20,151,286,518),
'BtnUnknownSmall.png':(21,540,285,910),
}
out=Path(r'C:\Dev\PinhoQuest\.worktrees\p6-total-ui-refactor\docs\design\Button\composed')
out.mkdir(parents=True,exist_ok=True)
for name,box in boxes.items():
    crop=im.crop(box)
    alpha=crop.getchannel('A')
    bb=alpha.getbbox()
    if bb: crop=crop.crop(bb)
    crop.save(out/name)
    print(name, box, '->', crop.size, 'alpha', alpha.getbbox())
