# Flower pixel-art test fixtures

## Camellia granthamiana with foliage

- Source: [Wikimedia Commons file page](https://commons.wikimedia.org/wiki/File:%E5%A4%A7%E8%8B%9E%E5%B1%B1%E8%8C%B6(%E8%91%9B%E9%87%8F%E6%B4%AA%E8%8C%B6)_Camellia_granthamiana_-%E9%A6%99%E6%B8%AF%E5%8B%95%E6%A4%8D%E7%89%A9%E5%85%AC%E5%9C%92_Hong_Kong_Botanical_Garden-_(9229858104).jpg)
- Photographer: 阿橋 HQ
- License: [Creative Commons Attribution-ShareAlike 2.0 Generic](https://creativecommons.org/licenses/by-sa/2.0/)
- Changes to fixture: none; the image file is stored as downloaded for a reproducible pixel-art regression.
- Test purpose: verify that a real flower photo with green leaves and a dark leafy background produces a transparent sprite that retains plant-green pixels. Instrumentation also emits a side-by-side comparison and a screenshot of the actual garden collection card; these are collected as a CI artifact and are not bundled into the production APK.
