#!/bin/sh
# DevUtils geometry verification: isolated display :223 and devutils3capture null sink.
# Adapted from atlas2/tools/launch-devutils-client.sh; original left unchanged.
set -e
ATLAS2_DIR=/home/thomas/01_TM/Coding/Websites/brainage04.github.io/.local-icon-variants/round3/atlas3/devutils
FORTNITE_DIR=/home/thomas/01_TM/Coding/Websites/brainage04.github.io/.local-icon-variants/round3/captures-fortnite

export JAVA_HOME=/nix/store/18311cxwsdjsc52dhas54wfqqa212q5m-openjdk-21.0.12+8/lib/openjdk
export DISPLAY="${ATLAS_DISPLAY:-:223}"
export LD_LIBRARY_PATH="$(cat "$FORTNITE_DIR/libpath.txt")"
export LIBGL_DRIVERS_PATH="/nix/store/6q9zxz6km0z4dmlxi6yrdp8rccbh49m1-mesa-26.1.8/lib/dri"
export ATLAS_DISPLAY="$DISPLAY"
export ATLAS_SINK="${ATLAS_SINK:-devutils3capture}"
export ATLAS_DEVUTILS_SERVER="${ATLAS_DEVUTILS_SERVER:-127.0.0.1}"
export ATLAS_DEVUTILS_PORT="${ATLAS_DEVUTILS_PORT:-25963}"

cd /home/thomas/01_TM/Coding/Minecraft/DevUtils
exec ./gradlew --no-daemon --console=plain runClient \
    --init-script "$ATLAS2_DIR/tools/devutils-client.init.gradle" \
    -Dorg.gradle.java.installations.paths=/nix/store/rrpczn74jq86pdc6zb8lv2j3jzmzvlkj-openjdk-8u504-b01/lib/openjdk -Dorg.gradle.java.installations.auto-detect=false \
    "$@"
