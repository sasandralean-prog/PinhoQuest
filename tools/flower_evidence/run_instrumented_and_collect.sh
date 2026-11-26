#!/usr/bin/env bash
set +e
chmod +x ./gradlew
out=app/build/outputs/flower-pixel-art-evidence
mkdir -p "$out"
rm -f "$out"/camellia-original.jpg "$out"/camellia-sprite.png "$out"/camellia-original-vs-sprite.jpg "$out"/camellia-garden-collection-screen.jpg "$out"/manifest.json "$out"/flower-evidence.log
echo "Increasing logcat buffer for chunked floral evidence"
adb logcat -G 16M || echo "Warning: could not increase logcat buffer; evidence integrity checks will report any missing chunks."
adb logcat -c || true
test_exit=0
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.pinhoquest.flora.FlowerPixelArtProcessorInstrumentedTest || test_exit=$?
logcat_file="$out/flower-evidence.log"
adb logcat -d -v brief -s PqFlowerEvidence:I > "$logcat_file" || true
echo "Reconstructing images from numbered, checksummed logcat chunks"
python tools/flower_evidence/extract_logcat.py "$logcat_file" "$out"
extract_exit=$?
evidence_exit=0
count=$(find "$out" -maxdepth 1 -type f \( -name "camellia-original.jpg" -o -name "camellia-sprite.png" -o -name "camellia-original-vs-sprite.jpg" -o -name "camellia-garden-collection-screen.jpg" \) | wc -l)
if [ "$count" -eq 4 ]; then
  python tools/flower_evidence/verify_images.py "$out" || evidence_exit=$?
else
  echo "Evidence validation deferred: only $count of 4 expected images were reconstructed; keep partial evidence for debugging."
  evidence_exit=1
fi
echo "Floral evidence output:"
ls -lah "$out"
if [ "$test_exit" -ne 0 ]; then exit "$test_exit"; fi
if [ "$extract_exit" -ne 0 ]; then exit "$extract_exit"; fi
if [ "$evidence_exit" -ne 0 ]; then exit "$evidence_exit"; fi
exit 0
