#!/usr/bin/env bash
set +e
chmod +x ./gradlew
test_exit=0
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.pinhoquest.flora.FlowerPixelArtProcessorInstrumentedTest || test_exit=$?
out=app/build/outputs/flower-pixel-art-evidence
mkdir -p "$out"
evidence_exit=0
evidence_dir="/data/user/0/com.pinhoquest/cache/pinhoquest-flower-evidence"
echo "Checking flower evidence at $evidence_dir"
adb shell run-as com.pinhoquest sh -c 'pwd; ls -ld /data/user/0/com.pinhoquest/cache /data/user/0/com.pinhoquest/cache/pinhoquest-flower-evidence 2>&1'
for name in camellia-original.jpg camellia-sprite.png camellia-original-vs-sprite.jpg camellia-garden-collection-screen.jpg; do
  if adb shell run-as com.pinhoquest test -f "$evidence_dir/$name"; then
    adb exec-out run-as com.pinhoquest cat "$evidence_dir/$name" > "$out/$name" || { echo "Failed to pull $name from app cache"; evidence_exit=1; }
  else
    echo "Missing app-cache evidence: $name"
    evidence_exit=1
  fi
done
python tools/flower_evidence/verify_images.py "$out" || evidence_exit=$?
echo "Verified floral evidence output:"
ls -lah "$out"
if [ "$test_exit" -ne 0 ]; then exit "$test_exit"; else exit "$evidence_exit"; fi
