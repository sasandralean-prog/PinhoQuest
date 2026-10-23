#!/usr/bin/env bash
set +e
chmod +x ./gradlew
test_exit=0
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.pinhoquest.flora.FlowerPixelArtProcessorInstrumentedTest || test_exit=$?
out=app/build/outputs/flower-pixel-art-evidence
mkdir -p "$out"
evidence_exit=0
echo "Inspecting app-specific evidence roots"
for package_id in com.pinhoquest com.pinhoquest.test; do
  echo "--- package $package_id external files ---"
  adb shell ls -la "/sdcard/Android/data/$package_id/files" 2>&1 || true
done
for name in camellia-original.jpg camellia-sprite.png camellia-original-vs-sprite.jpg camellia-garden-collection-screen.jpg; do
  pulled=0
  for package_id in com.pinhoquest com.pinhoquest.test; do
    external_file="/sdcard/Android/data/$package_id/files/pinhoquest-flower-evidence/$name"
    if adb pull "$external_file" "$out/$name"; then
      pulled=1
      break
    fi
    private_file="/data/user/0/$package_id/cache/pinhoquest-flower-evidence/$name"
    if adb shell run-as "$package_id" test -f "$private_file"; then
      if adb exec-out run-as "$package_id" cat "$private_file" > "$out/$name"; then
        pulled=1
        break
      fi
    fi
  done
  if [ "$pulled" -ne 1 ]; then
    echo "Missing floral evidence after checking app and test packages: $name"
    evidence_exit=1
  fi
done
python tools/flower_evidence/verify_images.py "$out" || evidence_exit=$?
echo "Verified floral evidence output:"
ls -lah "$out"
if [ "$test_exit" -ne 0 ]; then exit "$test_exit"; else exit "$evidence_exit"; fi
