#!/usr/bin/env bash
set +e
chmod +x ./gradlew
out=app/build/outputs/flower-pixel-art-evidence
remote_dir=/sdcard/Download/pinhoquest-flower-evidence
mkdir -p "$out"
rm -f "$out"/camellia-original.jpg "$out"/camellia-sprite.png "$out"/camellia-original-vs-sprite.jpg "$out"/camellia-garden-collection-screen.jpg "$out"/manifest.json
echo "Resetting shared floral evidence directory: $remote_dir"
adb shell "mkdir -p $remote_dir && rm -f $remote_dir/camellia-original.jpg $remote_dir/camellia-sprite.png $remote_dir/camellia-original-vs-sprite.jpg $remote_dir/camellia-garden-collection-screen.jpg" || true
test_exit=0
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.pinhoquest.flora.FlowerPixelArtProcessorInstrumentedTest || test_exit=$?
evidence_exit=0
for name in camellia-original.jpg camellia-sprite.png camellia-original-vs-sprite.jpg camellia-garden-collection-screen.jpg; do
  if adb pull "$remote_dir/$name" "$out/$name" >/dev/null 2>&1; then
    echo "Collected evidence: $name"
  else
    echo "No exported evidence found: $name"
    evidence_exit=1
  fi
done
count=$(find "$out" -maxdepth 1 -type f \( -name "camellia-original.jpg" -o -name "camellia-sprite.png" -o -name "camellia-original-vs-sprite.jpg" -o -name "camellia-garden-collection-screen.jpg" \) | wc -l)
if [ "$count" -eq 4 ]; then
  python tools/flower_evidence/verify_images.py "$out" || evidence_exit=$?
else
  echo "Evidence validation deferred: only $count of 4 expected images were exported; keep the partial evidence for debugging."
fi
echo "Floral evidence output:"
ls -lah "$out"
if [ "$test_exit" -ne 0 ]; then exit "$test_exit"; else exit "$evidence_exit"; fi
