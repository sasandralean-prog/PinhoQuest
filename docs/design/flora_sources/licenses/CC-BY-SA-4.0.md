# CC BY-SA 4.0 — working rights notes for PinhoQuest

This is an internal implementation note, not legal advice and not a substitute for the canonical license text.

Canonical license URL: https://creativecommons.org/licenses/by-sa/4.0/
Legal code: https://creativecommons.org/licenses/by-sa/4.0/legalcode
Human-readable deed: https://creativecommons.org/licenses/by-sa/4.0/deed.en

The Wikimedia Commons file page for `Flower photograph.jpg` declares CC BY-SA 4.0 and lists Anandha abirami Govindan as author/copyright holder:
https://commons.wikimedia.org/wiki/File:Flower_photograph.jpg

## Release checklist for this source

- Give appropriate credit, retain the creator's identity and the source URI, link the license, and clearly indicate that background removal/pixelization modified the file if those transformations are eventually used.
- If distributing an adapted sprite, determine and comply with the ShareAlike license obligation for the adapted material. Do not apply additional legal/technical restrictions that contradict the license.
- Do not imply that the creator endorses PinhoQuest.
- Confirm that all underlying photo bytes correspond exactly to the recorded provider revision before hashing/processing.
- Record a project-computed SHA-256 of the exact original bytes before modification. The Commons page currently exposes SHA-1 `34f1bf5a9bf54d8000a01cec5b248a2e044129d5`; that is provider metadata, not a local SHA-256.
- Verify taxonomic identity independently. The page categories suggest Mimosa/Fabaceae, but categories and a generic caption do not conclusively identify the species pictured.
- Keep `releaseAllowed=false` until source bytes, local SHA-256, identity, processed-output SHA-256, attribution, and derivative licensing have all been reviewed.

Official CC BY-SA 4.0 explains that sharing/adaptation, including commercial uses, is permitted subject to attribution, ShareAlike and no-additional-restrictions terms. See the official deed above. Other rights or facts may require separate verification.
