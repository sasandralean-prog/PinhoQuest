#!/usr/bin/env bash
set +e
chmod +x ./gradlew
test_exit=0
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.pinhoquest.flora.FlowerPixelArtProcessorInstrumentedTest || test_exit=$?
out=app/build/outputs/flower-pixel-art-evidence
mkdir -p "$out"
evidence_exit=0
external_dir="/sdcard/Android/data/com.pinhoquest/files/pinhoquest-flower-evidence"
private_dir="/data/user/0/com.pinhoquest/cache/pinhoquest-flower-evidence"
echo "Checking app-specific external evidence at $external_dir"
adb shell ls -la "$external_dir" 2>&1 || true
for name in camellia-original.jpg camellia-sprite.png camellia-original-vs-sprite.jpg camellia-garden-collection-screen.jpg; do
  pulled=0
  if adb pull "$external_dir/$name" "$out/$name" >/dev/null 2>&1; then
    pulled=1
  elif adb shell run-as com.pinhoquest test -f "$private_dir/$name"; then
    adb exec-out run-as com.pinhoquest cat "$private_dir/$name" > "$out/$name" && pulled=1
  fi
  if [ "$pulled" -ne 1 ]; then
    echo "Missing floral evidence: $name"
    evidence_exit=1
  fi
done
python tools/flower_evidence/verify_images.py "$out" || evidence_exit=$?
echo "Verified floral evidence output:"
ls -lah "$out"
if [ "$test_exit" -ne 0 ]; then exit "$test_exit"; else exit "$evidence_exit"; fi
